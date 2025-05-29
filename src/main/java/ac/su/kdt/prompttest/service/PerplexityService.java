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
        log.info("Starting getResponse for userId: {}, prompt: {}", userId, userPrompt);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        String systemPrompt = promptService.generatePrompt(userId, userPrompt);
        log.debug("System prompt: {}", systemPrompt);
        
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
        
        if (ingredientsText != null) {
            log.info("Found ingredients text: {}", ingredientsText);
            // 재료 라인별로 처리
            String[] ingredientLines = ingredientsText.split("\\n");
            log.info("Number of ingredient lines: {}", ingredientLines.length);
            
            for (String line : ingredientLines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("-")) {
                    log.debug("Skipping empty or bullet line: {}", line);
                    continue;
                }
                
                log.info("Processing ingredient line: {}", line);
                
                // 재료명과 양을 분리 (여러 패턴 시도)
                String ingredientName = null;
                String amount = null;
                
                // 패턴 1: "재료명: 양 단위"
                Pattern pattern1 = Pattern.compile("([^:]+):\\s*(.+)");
                Matcher matcher1 = pattern1.matcher(line);
                if (matcher1.find()) {
                    ingredientName = matcher1.group(1).trim();
                    amount = matcher1.group(2).trim();
                    log.info("Pattern 1 matched - Name: {}, Amount: {}", ingredientName, amount);
                }
                
                // 패턴 2: "재료명 양 단위"
                if (ingredientName == null) {
                    Pattern pattern2 = Pattern.compile("([가-힣a-zA-Z]+)\\s*(\\d+[가-힣a-zA-Z]+)");
                    Matcher matcher2 = pattern2.matcher(line);
                    if (matcher2.find()) {
                        ingredientName = matcher2.group(1).trim();
                        amount = matcher2.group(2).trim();
                        log.info("Pattern 2 matched - Name: {}, Amount: {}", ingredientName, amount);
                    }
                }
                
                if (ingredientName != null && amount != null) {
                    log.info("Found valid ingredient - Name: {}, Amount: {}", ingredientName, amount);
                    
                    try {
                        // 재료 찾기 또는 생성
                        String finalAmount = amount;
                        String finalIngredientName = ingredientName;
                        Ingredient ingredient = ingredientRepository.findByName(ingredientName)
                            .orElseGet(() -> {
                                log.info("Creating new ingredient: {}", finalIngredientName);
                                Ingredient newIngredient = new Ingredient();
                                newIngredient.setName(finalIngredientName);
                                
                                // 양과 단위 추출 및 변환
                                String[] amountParts = finalAmount.split("\\s+");
                                if (amountParts.length >= 2) {
                                    try {
                                        float value = Float.parseFloat(amountParts[0]);
                                        String unit = amountParts[1];
                                        // 단위를 g 또는 ml로 변환
                                        float convertedAmount = convertToStandardUnit(value, unit);
                                        newIngredient.setRequiredAmount(convertedAmount);
                                        log.info("Converted amount for {}: {} {} -> {} g/ml", 
                                            finalIngredientName, value, unit, convertedAmount);
                                    } catch (NumberFormatException e) {
                                        log.warn("Invalid amount format for ingredient {}: {}", 
                                            finalIngredientName, finalAmount);
                                    }
                                }
                                Ingredient savedIngredient = ingredientRepository.save(newIngredient);
                                log.info("Saved new ingredient with ID: {}", savedIngredient.getIngredientId());
                                return savedIngredient;
                            });
                        
                        log.info("Found existing ingredient with ID: {}", ingredient.getIngredientId());
                        
                        // RecipeIngredient 생성
                        RecipeIngredient recipeIngredient = new RecipeIngredient();
                        recipeIngredient.setRecipeId(recipe.getRecipeId());
                        recipeIngredient.setIngredientId(ingredient.getIngredientId());
                        recipeIngredients.add(recipeIngredient);
                        log.info("Created recipe ingredient relationship");
                    } catch (Exception e) {
                        log.error("Error processing ingredient: {}", ingredientName, e);
                    }
                } else {
                    log.warn("Failed to parse ingredient line: {}", line);
                }
            }
        } else {
            log.warn("No ingredients text found in content");
        }
        
        // 조리 시간 파싱
        Pattern timePattern = Pattern.compile("3\\.\\s*조리 시간\\s*:\\s*(\\d+)\\s*분");
        Matcher timeMatcher = timePattern.matcher(content);
        if (timeMatcher.find()) {
            recipe.setCookingTime(Integer.parseInt(timeMatcher.group(1)));
        } else {
            recipe.setCookingTime(30); // 기본값
        }
        
        // 난이도 파싱
        Pattern difficultyPattern = Pattern.compile("4\\.\\s*난이도\\s*:\\s*(.+?)(?=\\n|$)");
        Matcher difficultyMatcher = difficultyPattern.matcher(content);
        if (difficultyMatcher.find()) {
            recipe.setDifficulty(difficultyMatcher.group(1).trim());
        } else {
            recipe.setDifficulty("중"); // 기본값
        }
        
        // 조리 방법과 팁을 description에 통합
        StringBuilder descriptionBuilder = new StringBuilder();
        
        // 재료 정보 추가
        if (ingredientsText != null) {
            descriptionBuilder.append("필요한 재료와 양:\n").append(ingredientsText.trim()).append("\n\n");
        }
        
        // 조리 방법 파싱
        Pattern methodPattern = Pattern.compile("5\\.\\s*조리 방법\\s*:\\s*(.+?)(?=\\n\\d\\.|$)");
        Matcher methodMatcher = methodPattern.matcher(content);
        if (methodMatcher.find()) {
            String method = methodMatcher.group(1).trim();
            // 조리 방법에서 번호 제거
            method = method.replaceAll("\\d+\\)", "").trim();
            descriptionBuilder.append("조리 방법:\n").append(method).append("\n\n");
        }
        
        // 요리 팁과 주의사항 파싱
        Pattern tipsPattern = Pattern.compile("6\\.\\s*요리 팁과 주의사항\\s*:\\s*(.+?)(?=\\n\\d\\.|$)");
        Matcher tipsMatcher = tipsPattern.matcher(content);
        if (tipsMatcher.find()) {
            String tips = tipsMatcher.group(1).trim();
            descriptionBuilder.append("요리 팁과 주의사항:\n").append(tips);
        }
        
        String description = descriptionBuilder.toString().trim();
        log.info("Final description: {}", description);
        recipe.setDescription(description);
        
        try {
            // Recipe 저장
            log.info("Saving recipe with title: {}", recipe.getTitle());
            recipe = recipeRepository.save(recipe);
            log.info("Saved recipe with ID: {}", recipe.getRecipeId());
            
            // RecipeIngredient 저장
            for (RecipeIngredient ri : recipeIngredients) {
                ri.setRecipeId(recipe.getRecipeId());
                log.info("Saving recipe ingredient relationship - Recipe ID: {}, Ingredient ID: {}", 
                    ri.getRecipeId(), ri.getIngredientId());
                RecipeIngredient savedRI = recipeIngredientRepository.save(ri);
                log.info("Saved recipe ingredient relationship with ID: {}", savedRI.getRecipeId());
            }
            
            return recipe;
        } catch (Exception e) {
            log.error("Error saving recipe or recipe ingredients", e);
            throw new RuntimeException("Failed to save recipe: " + e.getMessage(), e);
        }
    }

    private float convertToStandardUnit(float value, String unit) {
        if (unit == null) return value;
        
        // 단위 변환 로직
        switch (unit.trim()) {
            case "g":
            case "ml":
                return value;
            case "kg":
                return value * 1000;
            case "L":
                return value * 1000;
            case "개":
            case "장":
            case "쪽":
                return value; // 기본 단위로 변환하지 않음
            default:
                return value;
        }
    }
}