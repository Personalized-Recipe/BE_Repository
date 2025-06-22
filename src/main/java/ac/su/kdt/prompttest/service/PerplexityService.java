package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.Recipe;
import ac.su.kdt.prompttest.entity.Ingredient;
import ac.su.kdt.prompttest.entity.RecipeIngredient;
import ac.su.kdt.prompttest.entity.UserPrompt;
import ac.su.kdt.prompttest.repository.RecipeRepository;
import ac.su.kdt.prompttest.repository.IngredientRepository;
import ac.su.kdt.prompttest.repository.RecipeIngredientRepository;
import ac.su.kdt.prompttest.repository.UserPromptRepository;
import ac.su.kdt.prompttest.dto.RecipeRecommendationRequestDTO;
import ac.su.kdt.prompttest.dto.RefrigeratorIngredientDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import ac.su.kdt.prompttest.dto.RecipeRequestDTO;

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
    private final RefrigeratorIngredientService refrigeratorIngredientService;
    
    @PostConstruct
    public void init() {
        log.info("API URL: {}", apiUrl);
        log.info("API Key length: {}", apiKey != null ? apiKey.length() : "null");
        log.info("API Key value: {}", apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) + "..." : "null");
        log.info("Full API Key: {}", apiKey);
        
        // API 키 유효성 검사
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("API Key is null or empty!");
        } else if (!apiKey.startsWith("pplx-")) {
            log.error("API Key format is invalid! Should start with 'pplx-' but got: {}", apiKey.substring(0, Math.min(10, apiKey.length())));
        } else {
            log.info("API Key format is valid");
        }
    }
    
    public Recipe getResponse(Integer userId, String userPrompt) {
        log.info("Getting response for userId: {}, prompt: {}", userId, userPrompt);
        
        // API 키 유효성 검사
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-actual-api-key-here")) {
            log.warn("Invalid or missing Perplexity API key. Returning test response.");
            return createTestRecipe(userPrompt);
        }
        
        // 시스템 프롬프트 생성
        String systemPrompt = promptService.generatePrompt(userId, userPrompt);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
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
            log.info("Sending request to Perplexity API");
            log.debug("Request headers: {}", headers);
            log.debug("Request body: {}", requestBody);
            
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
            log.info("Received response from Perplexity API");
            log.debug("Response: {}", response);
            
            if (response != null && response.containsKey("choices") 
                && !((List)response.get("choices")).isEmpty()) {
                Map<String, Object> choice = (Map<String, Object>)((List)response.get("choices")).get(0);
                Map<String, Object> messageResponse = (Map<String, Object>)choice.get("message");
                String content = (String) messageResponse.get("content");
                log.info("Parsing AI response content");
                log.debug("Content: {}", content);
                
                // AI 응답을 파싱하여 Recipe 객체로 변환
                Recipe recipe = parseRecipeResponse(content);
                log.info("Successfully parsed recipe with ID: {}", recipe.getRecipeId());
                return recipe;
            } else {
                log.error("No valid response received from Perplexity API");
                throw new RuntimeException("No response received from Perplexity API");
            }
        } catch (Exception e) {
            log.error("Error calling Perplexity API", e);
            // API 호출 실패 시 테스트 응답 반환
            log.warn("Returning test response due to API error");
            return createTestRecipe(userPrompt);
        }
    }

    /**
     * 테스트용 레시피 생성
     */
    private Recipe createTestRecipe(String userPrompt) {
        Recipe testRecipe = new Recipe();
        testRecipe.setTitle("테스트 레시피 - " + (userPrompt.contains("양파") ? "양파 요리" : "간단한 요리"));
        testRecipe.setDescription("이것은 테스트용 레시피입니다. 실제 API 키를 설정하면 더 정확한 레시피를 받을 수 있습니다.");
        testRecipe.setCookingTime(30);
        testRecipe.setDifficulty("중");
        testRecipe.setCategory("한식");
        
        // DB에 저장
        Recipe savedRecipe = recipeRepository.save(testRecipe);
        log.info("Created test recipe with ID: {}", savedRecipe.getRecipeId());
        
        return savedRecipe;
    }

    public String getResponseAsString(Integer userId, String userPrompt) {
        Recipe recipe = getResponse(userId, userPrompt);
        return formatRecipeAsString(recipe);
    }

    public String formatRecipeAsString(Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        
        // 요리 이름
        sb.append("1. 요리 이름: ").append(recipe.getTitle()).append("\n\n");
        
        // 재료와 양
        sb.append("2. 필요한 재료와 양:\n");
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository.findByRecipeId(recipe.getRecipeId());
        for (RecipeIngredient ri : recipeIngredients) {
            Ingredient ingredient = ingredientRepository.findById(ri.getIngredientId()).orElse(null);
            if (ingredient != null) {
                sb.append("- ").append(ingredient.getName()).append("\n");
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

    /**
     * 냉장고 재료를 기반으로 레시피를 추천받는 메서드
     */
    public Recipe getRecipeRecommendation(Integer userId, RecipeRecommendationRequestDTO requestDTO) {
        log.info("Starting recipe recommendation for userId: {}, refrigeratorId: {}", userId, requestDTO.getRefrigeratorId());
        
        // 냉장고 재료 목록 조회
        List<RefrigeratorIngredientDTO> refrigeratorIngredients = refrigeratorIngredientService.getRefrigeratorIngredients(requestDTO.getRefrigeratorId());
        
        if (refrigeratorIngredients.isEmpty()) {
            throw new RuntimeException("냉장고에 재료가 없습니다. 먼저 재료를 추가해주세요.");
        }
        
        // 냉장고 재료 정보를 포함한 프롬프트 생성
        String prompt = buildRefrigeratorBasedPrompt(refrigeratorIngredients, requestDTO);
        
        // Perplexity API 호출
        return getResponse(userId, prompt);
    }
    
    /**
     * 냉장고 재료 기반 레시피 추천 프롬프트 생성
     */
    public static String buildRefrigeratorBasedPrompt(List<RefrigeratorIngredientDTO> refrigeratorIngredients, RecipeRecommendationRequestDTO requestDTO) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("현재 냉장고에 있는 재료들을 기반으로 레시피를 추천해주세요.\n\n");
        
        // 냉장고 재료 목록
        prompt.append("【냉장고 재료 목록】\n");
        for (RefrigeratorIngredientDTO ingredient : refrigeratorIngredients) {
            prompt.append("- ").append(ingredient.getIngredientName())
                  .append(" ").append(ingredient.getQuantity())
                  .append(ingredient.getUnit())
                  .append(" (유통기한: ").append(ingredient.getExpiryDate()).append(")\n");
        }
        prompt.append("\n");
        
        // 추가 요구사항
        if (requestDTO.getCuisineType() != null && !requestDTO.getCuisineType().isEmpty()) {
            prompt.append("요리 종류: ").append(requestDTO.getCuisineType()).append("\n");
        }
        
        if (requestDTO.getDifficulty() != null && !requestDTO.getDifficulty().isEmpty()) {
            prompt.append("난이도: ").append(requestDTO.getDifficulty()).append("\n");
        }
        
        if (requestDTO.getServingSize() != null) {
            prompt.append("인분 수: ").append(requestDTO.getServingSize()).append("인분\n");
        }
        
        if (requestDTO.getCookingTime() != null && !requestDTO.getCookingTime().isEmpty()) {
            prompt.append("조리 시간: ").append(requestDTO.getCookingTime()).append("\n");
        }
        
        if (requestDTO.getPreference() != null && !requestDTO.getPreference().isEmpty()) {
            prompt.append("선호도: ").append(requestDTO.getPreference()).append("\n");
        }
        
        if (requestDTO.getDietaryRestrictions() != null && !requestDTO.getDietaryRestrictions().isEmpty()) {
            prompt.append("식이 제한: ").append(requestDTO.getDietaryRestrictions()).append("\n");
        }
        
        // 추가 재료가 있다면
        if (requestDTO.getAdditionalIngredients() != null && !requestDTO.getAdditionalIngredients().isEmpty()) {
            prompt.append("\n【추가로 필요한 재료】\n");
            for (String additionalIngredient : requestDTO.getAdditionalIngredients()) {
                prompt.append("- ").append(additionalIngredient).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("\n위의 냉장고 재료들을 최대한 활용하면서, 추가 요구사항에 맞는 맛있는 레시피를 추천해주세요. ");
        prompt.append("유통기한이 임박한 재료를 우선적으로 사용하는 방향으로 추천해주시고, ");
        prompt.append("재료의 양을 고려해서 적절한 인분 수의 레시피를 제안해주세요.\n\n");
        
        prompt.append("응답은 다음 형식으로 해주세요:\n");
        prompt.append("1. 요리 이름: [요리명]\n");
        prompt.append("2. 필요한 재료와 양:\n");
        prompt.append("- [재료명] [양]\n");
        prompt.append("3. 조리 시간: [시간]분\n");
        prompt.append("4. 난이도: [난이도]\n");
        prompt.append("5. 조리 방법과 팁: [상세한 조리 방법과 팁]\n");
        
        return prompt.toString();
    }

    private Recipe parseRecipeResponse(String content) {
        log.info("Starting to parse recipe response");
        log.debug("Content to parse: {}", content);
        
        Recipe recipe = new Recipe();
        List<RecipeIngredient> recipeIngredients = new ArrayList<>();
        
        // 요리 이름 파싱 - 여러 패턴 시도
        String title = null;
        
        // 패턴 1: "1. 요리 이름: [이름]"
        Pattern namePattern1 = Pattern.compile("1\\.\\s*요리 이름\\s*:\\s*(.+?)(?=\\n|$)");
        Matcher nameMatcher1 = namePattern1.matcher(content);
        if (nameMatcher1.find()) {
            title = nameMatcher1.group(1).trim();
        }
        
        // 패턴 2: "요리 이름: [이름]"
        if (title == null) {
            Pattern namePattern2 = Pattern.compile("요리 이름\\s*:\\s*(.+?)(?=\\n|$)");
            Matcher nameMatcher2 = namePattern2.matcher(content);
            if (nameMatcher2.find()) {
                title = nameMatcher2.group(1).trim();
            }
        }
        
        // 패턴 3: 첫 번째 줄이 제목인 경우
        if (title == null) {
            String[] lines = content.split("\\n");
            if (lines.length > 0) {
                title = lines[0].trim();
            }
        }
        
        // 제목이 여전히 null이면 기본값 설정
        if (title == null || title.isEmpty()) {
            log.warn("Failed to parse recipe title from content: {}", content);
            title = "레시피 제목";
        }
        
        log.info("Parsed recipe title: {}", title);
        recipe.setTitle(title);
        
        // 재료와 양 파싱 - 여러 패턴 시도
        String ingredientsText = null;
        
        // 패턴 1: "2. 필요한 재료와 양: ..."
        Pattern ingredientPattern1 = Pattern.compile("2\\.\\s*필요한 재료와 양\\s*:\\s*(.+?)(?=\\n\\d\\.|$)");
        Matcher ingredientMatcher1 = ingredientPattern1.matcher(content);
        if (ingredientMatcher1.find()) {
            ingredientsText = ingredientMatcher1.group(1).trim();
        }
        
        // 패턴 2: "재료: ..."
        if (ingredientsText == null) {
            Pattern ingredientPattern2 = Pattern.compile("재료\\s*:\\s*(.+?)(?=\\n\\d\\.|$)");
            Matcher ingredientMatcher2 = ingredientPattern2.matcher(content);
            if (ingredientMatcher2.find()) {
                ingredientsText = ingredientMatcher2.group(1).trim();
            }
        }
        
        // 재료 파싱 및 RecipeIngredient 생성
        if (ingredientsText != null) {
            String[] ingredientLines = ingredientsText.split("\\n");
            for (String line : ingredientLines) {
                line = line.trim();
                if (line.startsWith("-") || line.startsWith("•")) {
                    line = line.substring(1).trim();
                }
                
                if (!line.isEmpty()) {
                    // 재료명과 양 분리 (예: "양파 1개", "돼지고기 200g")
                    Pattern ingredientPattern = Pattern.compile("(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*(.+)?");
                    Matcher matcher = ingredientPattern.matcher(line);
                    
                    if (matcher.find()) {
                        String ingredientName = matcher.group(1).trim();
                        float quantity = Float.parseFloat(matcher.group(2));
                        String unit = matcher.group(3) != null ? matcher.group(3).trim() : "개";
                        
                        // 재료가 데이터베이스에 있는지 확인하고 없으면 생성
                        Ingredient ingredient = ingredientRepository.findByName(ingredientName)
                                .orElseGet(() -> {
                                    Ingredient newIngredient = new Ingredient();
                                    newIngredient.setName(ingredientName);
                                    newIngredient.setRequiredAmount(quantity);
                                    return ingredientRepository.save(newIngredient);
                                });
                        
                        RecipeIngredient recipeIngredient = new RecipeIngredient();
                        recipeIngredient.setRecipeId(recipe.getRecipeId());
                        recipeIngredient.setIngredientId(ingredient.getIngredientId());
                        recipeIngredients.add(recipeIngredient);
                    }
                }
            }
        }
        
        // 조리 시간 파싱
        Integer cookingTime = 30; // 기본값
        Pattern timePattern = Pattern.compile("3\\.\\s*조리 시간\\s*:\\s*(\\d+)\\s*분");
        Matcher timeMatcher = timePattern.matcher(content);
        if (timeMatcher.find()) {
            cookingTime = Integer.parseInt(timeMatcher.group(1));
        }
        recipe.setCookingTime(cookingTime);
        
        // 난이도 파싱
        String difficulty = "중"; // 기본값
        Pattern difficultyPattern = Pattern.compile("4\\.\\s*난이도\\s*:\\s*(.+)");
        Matcher difficultyMatcher = difficultyPattern.matcher(content);
        if (difficultyMatcher.find()) {
            String rawDifficulty = difficultyMatcher.group(1).trim();
            // AI 응답의 난이도를 엔티티 검증에 맞는 값으로 변환
            difficulty = mapDifficultyToEntityFormat(rawDifficulty);
        }
        recipe.setDifficulty(difficulty);
        
        // 조리 방법 파싱
        String description = "";
        Pattern descPattern = Pattern.compile("5\\.\\s*조리 방법과 팁\\s*:\\s*(.+)", Pattern.DOTALL);
        Matcher descMatcher = descPattern.matcher(content);
        if (descMatcher.find()) {
            description = descMatcher.group(1).trim();
        }
        recipe.setDescription(description);
        
        // Recipe 저장
        Recipe savedRecipe = recipeRepository.save(recipe);
        
        // RecipeIngredient 저장
        for (RecipeIngredient ri : recipeIngredients) {
            ri.setRecipeId(savedRecipe.getRecipeId());
            recipeIngredientRepository.save(ri);
        }
        
        log.info("Successfully parsed and saved recipe: {}", savedRecipe.getRecipeId());
        return savedRecipe;
    }

    private float convertToStandardUnit(float value, String unit) {
        // 단위 변환 로직 (필요시 구현)
        return value;
    }

    public Recipe generateRecipe(RecipeRequestDTO requestDTO) {
        String prompt = buildRecipePrompt(requestDTO);
        return getResponse(requestDTO.getUserId(), prompt);
    }

    private String buildRecipePrompt(RecipeRequestDTO requestDTO) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 요구사항에 맞는 레시피를 생성해주세요:\n\n");
        
        if (requestDTO.getPreferences() != null) {
            prompt.append("선호도: ").append(requestDTO.getPreferences()).append("\n");
        }
        if (requestDTO.getHealthConditions() != null) {
            prompt.append("건강 상태: ").append(requestDTO.getHealthConditions()).append("\n");
        }
        if (requestDTO.getAllergies() != null) {
            prompt.append("알레르기: ").append(requestDTO.getAllergies()).append("\n");
        }
        if (requestDTO.getPrompt() != null) {
            prompt.append("추가 요구사항: ").append(requestDTO.getPrompt()).append("\n");
        }
        
        prompt.append("\n위의 요구사항에 맞는 맛있는 레시피를 생성해주세요.\n\n");
        prompt.append("응답은 다음 형식으로 해주세요:\n");
        prompt.append("1. 요리 이름: [요리명]\n");
        prompt.append("2. 필요한 재료와 양:\n");
        prompt.append("- [재료명] [양]\n");
        prompt.append("3. 조리 시간: [시간]분\n");
        prompt.append("4. 난이도: [난이도]\n");
        prompt.append("5. 조리 방법과 팁: [상세한 조리 방법과 팁]\n");
        
        return prompt.toString();
    }

    private String mapDifficultyToEntityFormat(String rawDifficulty) {
        if (rawDifficulty == null || rawDifficulty.trim().isEmpty()) {
            return "중";
        }
        
        String normalized = rawDifficulty.toLowerCase().trim();
        
        // 초급/쉬움/간단 → 하
        if (normalized.contains("초급") || normalized.contains("쉬움") || 
            normalized.contains("간단") || normalized.contains("easy") ||
            normalized.contains("기본") || normalized.contains("입문")) {
            return "하";
        }
        
        // 고급/어려움/복잡 → 상
        if (normalized.contains("고급") || normalized.contains("어려움") || 
            normalized.contains("복잡") || normalized.contains("hard") ||
            normalized.contains("전문") || normalized.contains("고급자")) {
            return "상";
        }
        
        // 중급/보통/일반 → 중
        if (normalized.contains("중급") || normalized.contains("보통") || 
            normalized.contains("일반") || normalized.contains("medium") ||
            normalized.contains("중간")) {
            return "중";
        }
        
        // 기본값
        return "중";
    }
}