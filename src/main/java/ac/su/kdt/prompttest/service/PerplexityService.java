package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.Recipe;
import ac.su.kdt.prompttest.entity.Ingredient;
import ac.su.kdt.prompttest.repository.RecipeRepository;
import ac.su.kdt.prompttest.repository.IngredientRepository;
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
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.service.UserService;

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
    private final UserService userService;
    
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
                log.info("AI 응답 원본:\n{}", content);
                
                // AI 응답을 파싱하여 Recipe 객체로 변환
                Recipe recipe = parseRecipeResponse(content, userId);
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
        try {
            // 재료 정보를 불러오는 로직을 주석처리 또는 간단한 안내 로그로 대체
            // List<RecipeIngredient> recipeIngredients = recipeIngredientRepository.findByRecipeId(recipe.getRecipeId());
            // for (RecipeIngredient ri : recipeIngredients) {
            //     Ingredient ingredient = ingredientRepository.findById(ri.getIngredientId()).orElse(null);
            //     if (ingredient != null) {
            //         sb.append("   - ").append(ingredient.getName()).append("\n");
            //     }
            // }
        } catch (Exception e) {
            log.warn("Error fetching recipe ingredients: {}", e.getMessage());
            sb.append("   - 재료 정보를 불러올 수 없습니다.\n");
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

    private Recipe parseRecipeResponse(String content, Integer userId) {
        log.info("Starting to parse recipe response");
        log.debug("Content to parse: {}", content);
        
        // 1. 유저 알러지 정보 조회
        String userAllergy = null;
        try {
            User user = userService.getUserById(userId);
            var userPromptOpt = userPromptRepository.findByUser(user);
            if (userPromptOpt != null && userPromptOpt.getAllergy() != null) {
                userAllergy = userPromptOpt.getAllergy();
            }
        } catch (Exception e) {
            log.warn("알러지 정보 조회 실패: {}", e.getMessage());
        }

        Recipe recipe = new Recipe();
        List<String> ingredientNamesForAllergyCheck = new ArrayList<>(); // 알러지 체크용
        
        // 2. 알러지 키워드가 재료명에 포함되어 있는지 검사 (content 전체가 아니라 재료명 기준)
        if (userAllergy != null && !userAllergy.isBlank()) {
            String[] allergyKeywords = userAllergy.split(",");
            for (String ingredientName : ingredientNamesForAllergyCheck) {
                String ingredientNorm = ingredientName.replaceAll("\\s+", "").toLowerCase();
                for (String allergy : allergyKeywords) {
                    String allergyNorm = allergy.replaceAll("\\s+", "").toLowerCase();
                    if (ingredientNorm.contains(allergyNorm)) {
                        log.warn("알러지 재료({})가 포함된 레시피. 안내문구만 반환.", allergy);
                        Recipe allergyRecipe = new Recipe();
                        allergyRecipe.setTitle("알러지 주의");
                        allergyRecipe.setDescription("알러지 재료가 포함된 요리는 추천할 수 없습니다. (" + allergy + ")");
                        return allergyRecipe;
                    }
                }
            }
        }

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
        
        // 마크다운 헤더(## 등) 제거
        if (title != null) {
            title = title.replaceAll("^#+\\s*", "").trim();
        }
        
        // 제목이 여전히 null이면 기본값 설정
        if (title == null || title.isEmpty()) {
            log.warn("Failed to parse recipe title from content: {}", content);
            title = "레시피 제목";
        }
        
        // title 파싱 후 255자 제한 보장
        if (title != null && title.length() > 255) {
            title = title.substring(0, 255);
        }
        recipe.setTitle(title);
        
        // 재료와 양 파싱 - 여러 패턴 시도
        String ingredientsText = null;
        
        // 패턴 1: "2. 필요한 재료와 양: ..."
        Pattern ingredientPattern1 = Pattern.compile("2\\.\\s*필요한 재료와 양\\s*:\\s*(.+?)(?=\\n\\d\\.|$)");
        Matcher ingredientMatcher1 = ingredientPattern1.matcher(content);
        if (ingredientMatcher1.find()) {
            ingredientsText = ingredientMatcher1.group(1).trim();
        }
        
        // 패턴 2: "**필요한 재료와 양**" (마크다운 형식) - 간단한 버전
        if (ingredientsText == null) {
            int startIndex = content.indexOf("**필요한 재료와 양**");
            if (startIndex != -1) {
                startIndex = content.indexOf("\n", startIndex);
                if (startIndex != -1) {
                    int endIndex = content.indexOf("**", startIndex);
                    if (endIndex != -1) {
                        ingredientsText = content.substring(startIndex, endIndex).trim();
                    }
                }
            }
        }
        
        // 패턴 3: "재료: ..."
        if (ingredientsText == null) {
            Pattern ingredientPattern3 = Pattern.compile("재료\\s*:\\s*(.+?)(?=\\n\\d\\.|$)");
            Matcher ingredientMatcher3 = ingredientPattern3.matcher(content);
            if (ingredientMatcher3.find()) {
                ingredientsText = ingredientMatcher3.group(1).trim();
            }
        }
        
        if (ingredientsText != null) {
            log.info("Found ingredients text: {}", ingredientsText);
            String[] ingredientLines = ingredientsText.split("\\n");
            log.info("Number of ingredient lines: {}", ingredientLines.length);
            for (String line : ingredientLines) {
                line = line.trim();
                if (line.isEmpty() || !line.startsWith("-")) {
                    log.debug("Skipping non-ingredient line: {}", line);
                    continue;
                }
                line = line.substring(2).trim();
                log.info("Processing ingredient line: {}", line);
                String ingredientName = null;
                String amount = null;
                Pattern pattern1 = Pattern.compile("([^:]+):\\s*(.+)");
                Matcher matcher1 = pattern1.matcher(line);
                if (matcher1.find()) {
                    ingredientName = matcher1.group(1).trim();
                    amount = matcher1.group(2).trim();
                    log.info("Pattern 1 matched - Name: {}, Amount: {}", ingredientName, amount);
                }
                if (ingredientName == null) {
                    Pattern pattern2 = Pattern.compile("([가-힣a-zA-Z]+)\\s*(\\d+[가-힣a-zA-Z]+)");
                    Matcher matcher2 = pattern2.matcher(line);
                    if (matcher2.find()) {
                        ingredientName = matcher2.group(1).trim();
                        amount = matcher2.group(2).trim();
                        log.info("Pattern 2 matched - Name: {}, Amount: {}", ingredientName, amount);
                    }
                }
                if (ingredientName != null) {
                    ingredientNamesForAllergyCheck.add(ingredientName);
                }
                if (ingredientName != null && amount != null) {
                    log.info("Found valid ingredient - Name: {}, Amount: {}", ingredientName, amount);
                    
                    try {
                        // 재료 찾기 또는 생성
                        String finalAmount = amount;
                        String finalIngredientName = ingredientName;
                        Optional<Ingredient> ingredientOpt = ingredientRepository.findByName(ingredientName);
                        if (ingredientOpt.isPresent()) {
                            Ingredient ingredient = ingredientOpt.get();
                            log.info("Found existing ingredient with ID: {}", ingredient.getIngredientId());
                        } else {
                            log.warn("Ingredient '{}' not found in DB. Skipping.", ingredientName);
                        }
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
            // 마크다운 형식 조리 시간 파싱
            Pattern timePattern2 = Pattern.compile("\\*\\*조리 시간\\*\\*\\s*\\n-\\s*(\\d+)\\s*분");
            Matcher timeMatcher2 = timePattern2.matcher(content);
            if (timeMatcher2.find()) {
                recipe.setCookingTime(Integer.parseInt(timeMatcher2.group(1)));
            } else {
                recipe.setCookingTime(30); // 기본값
            }
        }
        
        // 난이도 파싱
        Pattern difficultyPattern = Pattern.compile("4\\.\\s*난이도\\s*:\\s*(.+?)(?=\\n|$)");
        Matcher difficultyMatcher = difficultyPattern.matcher(content);
        if (difficultyMatcher.find()) {
            String difficulty = difficultyMatcher.group(1).trim();
            recipe.setDifficulty(normalizeDifficulty(difficulty));
        } else {
            // 마크다운 형식 난이도 파싱
            Pattern difficultyPattern2 = Pattern.compile("\\*\\*난이도\\*\\*\\s*\\n-\\s*(.+?)(?=\\n|$)");
            Matcher difficultyMatcher2 = difficultyPattern2.matcher(content);
            if (difficultyMatcher2.find()) {
                String difficulty = difficultyMatcher2.group(1).trim();
                recipe.setDifficulty(normalizeDifficulty(difficulty));
            } else {
                recipe.setDifficulty("중"); // 기본값
            }
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
        
        // 마크다운 형식 조리 방법 파싱
        if (descriptionBuilder.toString().contains("조리 방법:")) {
            // 이미 파싱됨
        } else {
            Pattern methodPattern2 = Pattern.compile("\\*\\*상세한 조리 방법\\*\\*\\s*\\n(.+?)(?=\\n\\*\\*|$)", Pattern.DOTALL);
            Matcher methodMatcher2 = methodPattern2.matcher(content);
            if (methodMatcher2.find()) {
                String method = methodMatcher2.group(1).trim();
                descriptionBuilder.append("조리 방법:\n").append(method).append("\n\n");
            } else {
                descriptionBuilder.append("조리 방법 정보를 찾을 수 없습니다.\n\n");
            }
        }
        
        // 요리 팁과 주의사항 파싱
        Pattern tipsPattern = Pattern.compile("6\\.\\s*요리 팁과 주의사항\\s*:\\s*(.+?)(?=\\n\\d\\.|$)");
        Matcher tipsMatcher = tipsPattern.matcher(content);
        if (tipsMatcher.find()) {
            String tips = tipsMatcher.group(1).trim();
            descriptionBuilder.append("요리 팁과 주의사항:\n").append(tips);
        }
        
        // 마크다운 형식 요리 팁 파싱
        if (descriptionBuilder.toString().contains("요리 팁과 주의사항:")) {
            // 이미 파싱됨
        } else {
            Pattern tipsPattern2 = Pattern.compile("\\*\\*요리 팁과 주의사항\\*\\*\\s*\\n(.+?)(?=\\n\\*\\*|$)");
            Matcher tipsMatcher2 = tipsPattern2.matcher(content);
            if (tipsMatcher2.find()) {
                String tips = tipsMatcher2.group(1).trim();
                descriptionBuilder.append("요리 팁과 주의사항:\n").append(tips);
            }
        }
        
        String description = descriptionBuilder.toString().trim();
        log.info("Final description: {}", description);
        recipe.setDescription(description);
        
        try {
            // Recipe 저장
            log.info("Saving recipe with title: {}", recipe.getTitle());
            recipe = recipeRepository.save(recipe);
            log.info("Saved recipe with ID: {}", recipe.getRecipeId());
            
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

    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null) return "중";
        
        switch (difficulty.toLowerCase().trim()) {
            case "쉬움":
            case "하":
            case "easy":
                return "하";
            case "중간":
            case "중":
            case "medium":
                return "중";
            case "어려움":
            case "상":
            case "hard":
                return "상";
            default:
                return "중"; // 기본값
        }
    }

    public String getAIContentRaw(Integer userId, String userPrompt) {
        int maxTries = 3;
        for (int i = 0; i < maxTries; i++) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            String systemPrompt = promptService.generatePrompt(userId, userPrompt);
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
                Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
                if (response != null && response.containsKey("choices")
                    && !((List)response.get("choices")).isEmpty()) {
                    Map<String, Object> choice = (Map<String, Object>)((List)response.get("choices")).get(0);
                    Map<String, Object> messageResponse = (Map<String, Object>)choice.get("message");
                    String content = (String) messageResponse.get("content");
                    if (content != null && content.contains("상세한 조리 방법")) {
                        return content;
                    }
                    // 아니면 재시도
                } else {
                    return "No valid response received from Perplexity API";
                }
            } catch (Exception e) {
                return "Error calling Perplexity API: " + e.getMessage();
            }
        }
        return "상세한 조리 방법 섹션이 포함된 답변을 받지 못했습니다.";
    }
}