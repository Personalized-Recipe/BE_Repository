package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.Recipe;
import ac.su.kdt.prompttest.entity.Ingredient;
import ac.su.kdt.prompttest.entity.RecipeIngredient;
import ac.su.kdt.prompttest.entity.UserPrompt;
import ac.su.kdt.prompttest.repository.RecipeRepository;
import ac.su.kdt.prompttest.repository.IngredientRepository;
import ac.su.kdt.prompttest.repository.RecipeIngredientRepository;
import ac.su.kdt.prompttest.repository.UserPromptRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerplexityService {

    @Value("${perplexity.api.key}")
    private String apiKey;
    
    @Value("${perplexity.api.url}")
    private String apiUrl;
    
    private final RestTemplate restTemplate;
    private final PromptService promptService;
    private final UserPromptRepository userPromptRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    
    @PostConstruct
    public void init() {
        log.info("API URL: {}", apiUrl);
        log.info("API Key length: {}", apiKey != null ? apiKey.length() : "null");
    }
    
    public Recipe getResponse(Integer userId, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        String systemPrompt = buildSystemPrompt(userId);
        
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "sonar-pro");
        requestBody.put("messages", Arrays.asList(systemMessage, userMessage));
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            log.info("Sending request to Perplexity API with headers: {}", headers);
            log.info("Request body: {}", requestBody);
            
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
            
            if (response != null && response.containsKey("choices") 
                && !((List)response.get("choices")).isEmpty()) {
                Map<String, Object> choice = (Map<String, Object>)((List)response.get("choices")).get(0);
                Map<String, Object> messageResponse = (Map<String, Object>)choice.get("message");
                String content = (String) messageResponse.get("content");
                
                // AI 응답을 파싱하여 Recipe 객체로 변환
                return parseRecipeResponse(content);
            } else {
                throw new RuntimeException("No response received from Perplexity API");
            }
        } catch (Exception e) {
            log.error("Error calling Perplexity API", e);
            throw new RuntimeException("Error calling Perplexity API: " + e.getMessage());
        }
    }

    public String getResponseAsString(Integer userId, String userPrompt) {
        Recipe recipe = getResponse(userId, userPrompt);
        return formatRecipeAsString(recipe);
    }

    private String formatRecipeAsString(Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        sb.append("1. 요리 이름: ").append(recipe.getTitle()).append("\n\n");
        
        // 재료 정보 추가
        sb.append("2. 필요한 재료와 양:\n");
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository.findByRecipeId(recipe.getRecipeId());
        for (RecipeIngredient ri : recipeIngredients) {
            Ingredient ingredient = ingredientRepository.findById(ri.getIngredientId()).orElse(null);
            if (ingredient != null) {
                sb.append("   - ").append(ingredient.getName()).append("\n");
            }
        }
        sb.append("\n");
        
        // 조리 시간
        sb.append("3. 조리 시간: ").append(recipe.getCookingTime()).append("분\n\n");
        
        // 난이도
        sb.append("4. 난이도: ").append(recipe.getDifficulty()).append("\n\n");
        
        // 조리 방법과 팁
        sb.append("5. ").append(recipe.getDescription());
        
        return sb.toString();
    }

    private Recipe parseRecipeResponse(String content) {
        Recipe recipe = new Recipe();
        List<RecipeIngredient> recipeIngredients = new ArrayList<>();
        
        // 요리 이름 파싱
        Pattern namePattern = Pattern.compile("1\\.\\s*요리 이름\\s*:\\s*(.+?)(?=\\n|$)");
        Matcher nameMatcher = namePattern.matcher(content);
        if (nameMatcher.find()) {
            recipe.setTitle(nameMatcher.group(1).trim());
        }
        
        // 재료와 양 파싱
        Pattern ingredientPattern = Pattern.compile("2\\.\\s*필요한 재료와 양\\s*:\\s*(.+?)(?=\\n\\d\\.|$)");
        Matcher ingredientMatcher = ingredientPattern.matcher(content);
        if (ingredientMatcher.find()) {
            String[] ingredientLines = ingredientMatcher.group(1).split("\\n");
            for (String line : ingredientLines) {
                if (line.trim().isEmpty()) continue;
                
                // 재료명과 양을 분리
                String[] parts = line.split("\\s*:\\s*");
                if (parts.length >= 2) {
                    String ingredientName = parts[0].trim();
                    String amount = parts[1].trim();
                    
                    // 재료 찾기 또는 생성
                    Ingredient ingredient = ingredientRepository.findByName(ingredientName)
                        .orElseGet(() -> {
                            Ingredient newIngredient = new Ingredient();
                            newIngredient.setName(ingredientName);
                            // 단위 추출
                            String[] amountParts = amount.split("\\s+");
                            if (amountParts.length >= 2) {
                                newIngredient.setUnit(amountParts[1]);
                            }
                            return ingredientRepository.save(newIngredient);
                        });
                    
                    // RecipeIngredient 생성
                    RecipeIngredient recipeIngredient = new RecipeIngredient();
                    recipeIngredient.setRecipeId(recipe.getRecipeId());
                    recipeIngredient.setIngredientId(ingredient.getId());
                    recipeIngredients.add(recipeIngredient);
                }
            }
        }
        
        // 조리 시간 파싱
        Pattern timePattern = Pattern.compile("3\\.\\s*조리 시간\\s*:\\s*(\\d+)\\s*분");
        Matcher timeMatcher = timePattern.matcher(content);
        if (timeMatcher.find()) {
            recipe.setCookingTime(Integer.parseInt(timeMatcher.group(1)));
        }
        
        // 난이도 파싱
        Pattern difficultyPattern = Pattern.compile("4\\.\\s*난이도\\s*:\\s*(.+?)(?=\\n|$)");
        Matcher difficultyMatcher = difficultyPattern.matcher(content);
        if (difficultyMatcher.find()) {
            recipe.setDifficulty(difficultyMatcher.group(1).trim());
        }
        
        // 조리 방법과 팁을 description에 통합
        StringBuilder descriptionBuilder = new StringBuilder();
        
        // 조리 방법 파싱
        Pattern methodPattern = Pattern.compile("5\\.\\s*상세한 조리 방법\\s*:\\s*(.+?)(?=\\n\\s*-|$)");
        Matcher methodMatcher = methodPattern.matcher(content);
        if (methodMatcher.find()) {
            descriptionBuilder.append("조리 방법:\n").append(methodMatcher.group(1).trim()).append("\n\n");
        }
        
        // 요리 팁과 주의사항 파싱
        Pattern tipsPattern = Pattern.compile("요리 팁과 주의사항\\s*:\\s*(.+?)(?=\\n\\s*-|$)");
        Matcher tipsMatcher = tipsPattern.matcher(content);
        if (tipsMatcher.find()) {
            descriptionBuilder.append("요리 팁과 주의사항:\n").append(tipsMatcher.group(1).trim());
        }
        
        recipe.setDescription(descriptionBuilder.toString());
        
        // Recipe 저장
        recipe = recipeRepository.save(recipe);
        
        // RecipeIngredient 저장
        for (RecipeIngredient ri : recipeIngredients) {
            ri.setRecipeId(recipe.getRecipeId());
            recipeIngredientRepository.save(ri);
        }
        
        return recipe;
    }

    private String buildSystemPrompt(Integer userId) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("당신은 레시피 추천 전문가입니다. 다음과 같은 사용자의 선호도와 요구사항을 고려하여 답변해주세요:\n\n");

        UserPrompt userPrompt = userPromptRepository.findByUserId(userId);
        
        if (userPrompt != null) {
            if (userPrompt.getPreference() != null) {
                promptBuilder.append("선호도: ").append(userPrompt.getPreference()).append("\n");
            }
            if (userPrompt.getHealthStatus() != null) {
                promptBuilder.append("건강 상태: ").append(userPrompt.getHealthStatus()).append("\n");
            }
            if (userPrompt.getAllergy() != null) {
                promptBuilder.append("알레르기: ").append(userPrompt.getAllergy()).append("\n");
            }
            if (userPrompt.getIsPregnant() != null && userPrompt.getIsPregnant()) {
                promptBuilder.append("임신 중\n");
            }
        }

        promptBuilder.append("\n레시피는 다음 형식으로 제공합니다:\n");
        promptBuilder.append("1. 요리 이름: [요리 이름]\n");
        promptBuilder.append("2. 필요한 재료와 양:\n");
        promptBuilder.append("   - [재료1]: [양] [단위]\n");
        promptBuilder.append("   - [재료2]: [양] [단위]\n");
        promptBuilder.append("   ...\n");
        promptBuilder.append("3. 조리 시간: [시간]분\n");
        promptBuilder.append("4. 난이도: [상/중/하]\n");
        promptBuilder.append("5. 상세한 조리 방법: [단계별 조리 방법]\n\n");
        promptBuilder.append("요리 팁과 주의사항: [팁과 주의사항]\n");

        return promptBuilder.toString();
    }
}