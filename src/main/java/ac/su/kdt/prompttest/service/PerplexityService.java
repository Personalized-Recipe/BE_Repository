package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.RecipeResponseDTO;
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
    
    public RecipeResponseDTO getResponse(Integer userId, String userPrompt, Boolean useRefrigerator) {
        return getResponse(userId, userPrompt, useRefrigerator, false);
    }
    
    public RecipeResponseDTO getResponse(Integer userId, String userPrompt, Boolean useRefrigerator, Boolean isSpecificRecipe) {
        return getResponseWithRetry(userId, userPrompt, useRefrigerator, isSpecificRecipe, 0);
    }
    
    private RecipeResponseDTO getResponseWithRetry(Integer userId, String userPrompt, Boolean useRefrigerator, Boolean isSpecificRecipe, int retryCount) {
        log.info("Starting getResponse for userId: {}, prompt: {}, useRefrigerator: {}, isSpecificRecipe: {}, retry: {}",
                userId, userPrompt, useRefrigerator, isSpecificRecipe, retryCount);
        
        try {
            // 1. 유저 정보 조회
            User user = userService.getUserById(userId);
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            
            // 2. AI 응답 생성
            String systemPrompt;
            if (isSpecificRecipe) {
                systemPrompt = promptService.generatePrompt(userId, userPrompt, useRefrigerator, true);
            } else {
                systemPrompt = promptService.generatePrompt(userId, userPrompt, useRefrigerator, false);
            }
            
            // 3. Perplexity API 호출
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
            
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
            
            if (response != null && response.containsKey("choices") 
                && !((List)response.get("choices")).isEmpty()) {
                
                Map<String, Object> choice = (Map<String, Object>)((List)response.get("choices")).get(0);
                Map<String, Object> messageResponse = (Map<String, Object>)choice.get("message");
                String content = (String) messageResponse.get("content");
                
                log.info("=== AI 응답 파싱 완료 ===");
                
                // AI 응답을 파싱하여 Recipe 객체로 변환
                if (isSpecificRecipe) {
                Recipe recipe = parseRecipeResponse(content, userId);
                    return RecipeResponseDTO.createRecipeDetail(recipe);
                } else {
                    List<Recipe> recipes = parseMenuRecommendationResponse(content, userId);
                    return RecipeResponseDTO.createMenuRecommendation(recipes);
                }
            } else {
                log.error("No valid response received from Perplexity API");
                throw new RuntimeException("No response received from Perplexity API");
            }
        } catch (Exception e) {
            log.error("Error calling Perplexity API", e);
            throw new RuntimeException("Error calling Perplexity API: " + e.getMessage());
        }
    }

    
    public Recipe getResponse(Integer userId, String userPrompt) {
        RecipeResponseDTO response = getResponse(userId, userPrompt, false, true);
        if (response.getType().equals("recipe-detail")) {
            return response.getRecipe();
        } else {
            // 메뉴 추천인 경우 첫 번째 레시피 반환
            if (response.getRecipes() != null && !response.getRecipes().isEmpty()) {
                return response.getRecipes().get(0);
            } else {
                // 기본 레시피 반환
                Recipe defaultRecipe = new Recipe();
                defaultRecipe.setTitle("레시피 정보 없음");
                defaultRecipe.setDescription("레시피 정보를 찾을 수 없습니다.");
                defaultRecipe.setCookingTime(0);
                defaultRecipe.setDifficulty("중");
                return defaultRecipe;
            }
        }
    }

    public String getResponseAsString(Integer userId, String userPrompt) {
        Recipe recipe = getResponse(userId, userPrompt);
        return formatRecipeAsString(recipe);
    }

    private String formatRecipeAsString(Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        sb.append("1. 요리 이름: ").append(recipe.getTitle()).append("\n\n");
        // 2번(필요한 재료와 양) 항목 삭제
        // 조리 시간
        sb.append("2. 조리 시간: ").append(recipe.getCookingTime()).append("분\n\n");
        // 난이도
        sb.append("3. 난이도: ").append(recipe.getDifficulty()).append("\n\n");
        // 조리 방법과 팁
        sb.append("4. ").append(recipe.getDescription());
        return sb.toString();
    }
    

    private Recipe parseRecipeResponse(String content, Integer userId) {
        return parseRecipeResponse(content, userId, 0);
    }
    
    private Recipe parseRecipeResponse(String content, Integer userId, int retryCount) {
        log.info("Starting to parse recipe response (retry: {})", retryCount);
        log.debug("Content to parse: {}", content);
        
        // 최대 3번까지 재시도
        if (retryCount >= 3) {
            log.error("최대 재시도 횟수 초과. 기본 레시피를 반환합니다.");
            Recipe errorRecipe = new Recipe();
            errorRecipe.setTitle("레시피 정보 부족");
            errorRecipe.setDescription("조리 방법 정보를 찾을 수 없어 레시피를 제공할 수 없습니다. 다시 요청해주세요.");
            errorRecipe.setCookingTime(0);
            errorRecipe.setDifficulty("중");
            return errorRecipe;
        }
        
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
            // '필요한 재료' 섹션만 추출
            String ingredientsSection = null;
            Pattern ingredientsPattern = Pattern.compile("\\*\\*필요한 재료.*?\\*\\*(.+?)(?=\\*\\*|$)", Pattern.DOTALL);
            Matcher matcher = ingredientsPattern.matcher(content);
            if (matcher.find()) {
                ingredientsSection = matcher.group(1);
            }
            if (ingredientsSection != null) {
                String lowerIngredients = ingredientsSection.toLowerCase();
                for (String allergy : allergyKeywords) {
                    String allergyNorm = allergy.replaceAll("\\s+", "").toLowerCase();
                    if (lowerIngredients.contains(allergyNorm)) {
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
        
        // 카테고리 파싱 - AI 응답에서 직접 받기
        String category = parseCategoryFromResponse(content);
        recipe.setCategory(category);
        
        // 7. 이미지 URL 파싱
        String imageUrl = parseImageUrlFromResponse(content);
        if (imageUrl != null) {
            log.info("Found image URL: {}", imageUrl);
            recipe.setImageUrl(imageUrl);
        } else {
            // 기본 이미지 URL 설정 (Imgur)
            recipe.setImageUrl("https://i.imgur.com/8tMUxoP.jpg");
        }
        
        // 재료와 양 파싱 - 여러 패턴 시도
        String ingredientsText = null;
        
        // 패턴 1: "5. 필요한 재료와 양: ..." (수정된 형식)
        Pattern ingredientPattern1 = Pattern.compile("5\\.\\s*필요한 재료와 양\\s*:\\s*(.+?)(?=\\n\\d\\.|\\n조리 방법|$)", Pattern.DOTALL);
        Matcher ingredientMatcher1 = ingredientPattern1.matcher(content);
        if (ingredientMatcher1.find()) {
            ingredientsText = ingredientMatcher1.group(1).trim();
        }
        
        // 패턴 2: "4. 필요한 재료와 양: ..." (현재 AI 응답 형식)
        if (ingredientsText == null) {
            Pattern ingredientPattern2 = Pattern.compile("4\\.\\s*필요한 재료와 양\\s*:\\s*(.+?)(?=\\n\\d\\.|\\n조리 방법|$)", Pattern.DOTALL);
            Matcher ingredientMatcher2 = ingredientPattern2.matcher(content);
            if (ingredientMatcher2.find()) {
                ingredientsText = ingredientMatcher2.group(1).trim();
            }
        }
        
        // 패턴 3: "6. 필요한 재료와 양: ..." (기존 형식)
        if (ingredientsText == null) {
            Pattern ingredientPattern3 = Pattern.compile("6\\.\\s*필요한 재료와 양\\s*:\\s*(.+?)(?=\\n\\d\\.|\\n조리 방법|$)", Pattern.DOTALL);
            Matcher ingredientMatcher3 = ingredientPattern3.matcher(content);
            if (ingredientMatcher3.find()) {
                ingredientsText = ingredientMatcher3.group(1).trim();
            }
        }
        
        // 패턴 4: "2. 필요한 재료와 양: ..." (기존 형식)
        if (ingredientsText == null) {
            Pattern ingredientPattern4 = Pattern.compile("2\\.\\s*필요한 재료\\s*:\\s*(.+?)(?=\\n\\d\\.|$)");
            Matcher ingredientMatcher4 = ingredientPattern4.matcher(content);
            if (ingredientMatcher4.find()) {
                ingredientsText = ingredientMatcher4.group(1).trim();
            }
        }
        
        // 패턴 5: AI 응답 형식 "준비 재료(2인분 기준): ..."
        if (ingredientsText == null) {
            Pattern ingredientPattern5 = Pattern.compile("준비 재료\\([^)]+\\)\\s*:\\s*(.+?)(?=\\n-\\s*조리 방법|$)", Pattern.DOTALL);
            Matcher ingredientMatcher5 = ingredientPattern5.matcher(content);
            if (ingredientMatcher5.find()) {
                ingredientsText = ingredientMatcher5.group(1).trim();
            }
        }
        
        // 패턴 6: "**필요한 재료**" (마크다운 형식)
        if (ingredientsText == null) {
            int startIndex = content.indexOf("**필요한 재료**");
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
        
        // 패턴 7: "재료: ..."
        if (ingredientsText == null) {
            Pattern ingredientPattern7 = Pattern.compile("재료\\s*:\\s*(.+?)(?=\\n\\d\\.|$)");
            Matcher ingredientMatcher7 = ingredientPattern7.matcher(content);
            if (ingredientMatcher7.find()) {
                ingredientsText = ingredientMatcher7.group(1).trim();
            }
        }
        
        // 패턴 8: 조리 방법에서 재료 추출 (마지막 수단)
        if (ingredientsText == null) {
            ingredientsText = extractIngredientsFromCookingMethod(content);
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
        
        // 새로운 AI 응답 형식에 맞는 조리 방법 파싱 (7. 조리 방법:)
        String cookingMethod = parseCookingMethod(content);
        if (cookingMethod != null) {
            descriptionBuilder.append(cookingMethod);
            log.info("Successfully parsed cooking method");
        } else {
            log.warn("조리 방법을 찾을 수 없습니다");
                descriptionBuilder.append("조리 방법 정보를 찾을 수 없습니다.\n\n");
        }
        
        String description = descriptionBuilder.toString().trim();
        log.info("Final description: {}", description);
        
        // 조리 방법 정보가 없으면 null 반환 (상위에서 재시도)
        if (!description.contains("조리 방법:") || description.contains("조리 방법 정보를 찾을 수 없습니다")) {
            log.warn("조리 방법 정보가 없어서 null을 반환합니다.");
            return null;
        }
        
        recipe.setDescription(description);
        
        // 특정 레시피 요청의 경우에만 DB 저장 (캐싱 효과)
        try {
            // 제목으로 기존 레시피가 있는지 확인
            Recipe existingRecipe = recipeRepository.findByTitle(recipe.getTitle());
            if (existingRecipe != null) {
                log.info("Found existing recipe with same title: {}", recipe.getTitle());
                return existingRecipe;
            }
            
            // 새로운 레시피 저장
            log.info("Saving new recipe with title: {}", recipe.getTitle());
            recipe = recipeRepository.save(recipe);
            log.info("Saved recipe with ID: {}", recipe.getRecipeId());
            
            return recipe;
        } catch (Exception e) {
            log.error("Error saving recipe", e);
            // 저장 실패해도 레시피는 반환
            return recipe;
        }
    }

    private String parseCookingMethod(String content) {
        // 현재 AI 응답 형식 (4. 조리 방법:) 먼저 시도
        Pattern methodPattern = Pattern.compile("4\\.\\s*조리 방법\\s*:\\s*(.+?)(?=\\n\\d\\.|\\n요리 팁|$)", Pattern.DOTALL);
        Matcher methodMatcher = methodPattern.matcher(content);
        
        if (methodMatcher.find()) {
            String method = methodMatcher.group(1).trim();
            log.info("Found cooking method (Pattern 4): {}", method.substring(0, Math.min(100, method.length())));
            return "조리 방법:\n" + method + "\n\n";
        }
        
        // 새로운 형식 (7. 조리 방법:) 시도
        methodPattern = Pattern.compile("7\\.\\s*조리 방법\\s*:\\s*(.+?)(?=\\n\\d\\.|\\n요리 팁|$)", Pattern.DOTALL);
        methodMatcher = methodPattern.matcher(content);
        
        if (methodMatcher.find()) {
            String method = methodMatcher.group(1).trim();
            log.info("Found cooking method (Pattern 7): {}", method.substring(0, Math.min(100, method.length())));
            return "조리 방법:\n" + method + "\n\n";
        }
        
        // 현재 AI 응답 형식 (조리 방법:) 시도
        methodPattern = Pattern.compile("조리 방법\\s*:\\s*(.+?)(?=\\n\\d\\.|\\n요리 팁|$)", Pattern.DOTALL);
        methodMatcher = methodPattern.matcher(content);
        
        if (methodMatcher.find()) {
            String method = methodMatcher.group(1).trim();
            log.info("Found cooking method (Pattern cooking method): {}", method.substring(0, Math.min(100, method.length())));
            return "조리 방법:\n" + method + "\n\n";
        }
        
        // 새로운 형식 (6. 상세한 조리 방법과 팁) 시도
        methodPattern = Pattern.compile("6\\.\\s*상세한 조리 방법과 팁\\s*:\\s*(.+?)(?=\\n\\d\\.|$)", Pattern.DOTALL);
        methodMatcher = methodPattern.matcher(content);
        
        if (methodMatcher.find()) {
            String method = methodMatcher.group(1).trim();
            log.info("Found cooking method (Pattern 6): {}", method.substring(0, Math.min(100, method.length())));
            return "조리 방법:\n" + method + "\n\n";
        }
        
        // 기존 형식 (4. 상세한 조리 방법과 팁) 시도
        methodPattern = Pattern.compile("4\\.\\s*상세한 조리 방법과 팁\\s*\\n(.+?)(?=\\n\\d\\.|$)", Pattern.DOTALL);
        methodMatcher = methodPattern.matcher(content);
        
        if (methodMatcher.find()) {
            String method = methodMatcher.group(1).trim();
            log.info("Found cooking method (Pattern 4 detailed): {}", method.substring(0, Math.min(100, method.length())));
            return "조리 방법:\n" + method + "\n\n";
        }
        
        // 기존 형식 (5. 조리 방법) 시도
        methodPattern = Pattern.compile("5\\.\\s*조리 방법\\s*:\\s*(.+?)(?=\\n\\d\\.|$)", Pattern.DOTALL);
        methodMatcher = methodPattern.matcher(content);
        
        if (methodMatcher.find()) {
            String method = methodMatcher.group(1).trim();
            // 조리 방법에서 번호 제거
            method = method.replaceAll("\\d+\\)", "").trim();
            log.info("Found cooking method (Pattern 5): {}", method.substring(0, Math.min(100, method.length())));
            return "조리 방법:\n" + method + "\n\n";
        }
        
        // 마크다운 형식 조리 방법 파싱
        methodPattern = Pattern.compile("\\*\\*상세한 조리 방법\\*\\*\\s*\\n(.+?)(?=\\n\\*\\*|$)", Pattern.DOTALL);
        methodMatcher = methodPattern.matcher(content);
        
        if (methodMatcher.find()) {
            String method = methodMatcher.group(1).trim();
            log.info("Found cooking method (Markdown pattern): {}", method.substring(0, Math.min(100, method.length())));
            return "조리 방법:\n" + method + "\n\n";
        }
        
        // 유연한 패턴 시도 (마지막 수단)
        methodPattern = Pattern.compile("(조리|만드는|요리|요리법|레시피|방법|단계|순서).*?\\n(.+?)(?=\\n\\n|$)", Pattern.DOTALL);
        methodMatcher = methodPattern.matcher(content);
        
        if (methodMatcher.find()) {
            String method = methodMatcher.group(2).trim();
            log.info("Found cooking method (Flexible pattern): {}", method.substring(0, Math.min(100, method.length())));
            return "조리 방법:\n" + method + "\n\n";
        }
        
        log.warn("=== 전체 AI 응답 내용 ===");
        log.warn(content);
        log.warn("=== AI 응답 내용 끝 ===");
        
        return null;
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

            String systemPrompt = promptService.generatePrompt(userId, userPrompt, false, false);
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

    private String parseCategoryFromResponse(String content) {
        Pattern categoryPattern = Pattern.compile("2\\.\\s*카테고리\\s*:\\s*(.+?)(?=\\n|$)");
        Matcher categoryMatcher = categoryPattern.matcher(content);
        if (categoryMatcher.find()) {
            String category = categoryMatcher.group(1).trim();
            log.info("Found category from AI response: {}", category);
            return category;
        }
        return "한식"; // 기본값
    }

    private String parseImageUrlFromResponse(String content) {
        // 수정된 형식 (7. 이미지 URL:) 먼저 시도
        Pattern imageUrlPattern = Pattern.compile("7\\.\\s*이미지 URL\\s*:\\s*(.+?)(?=\\n|$)");
        Matcher imageUrlMatcher = imageUrlPattern.matcher(content);
        if (imageUrlMatcher.find()) {
            String imageUrl = imageUrlMatcher.group(1).trim();
            log.info("Found image URL from AI response (Pattern 7): {}", imageUrl);
            return validateAndCleanImageUrl(imageUrl);
        }
        
        // 기존 형식 (5. 이미지 URL:) 시도
        imageUrlPattern = Pattern.compile("5\\.\\s*이미지 URL\\s*:\\s*(.+?)(?=\\n|$)");
        imageUrlMatcher = imageUrlPattern.matcher(content);
        if (imageUrlMatcher.find()) {
            String imageUrl = imageUrlMatcher.group(1).trim();
            log.info("Found image URL from AI response (Pattern 5): {}", imageUrl);
            return validateAndCleanImageUrl(imageUrl);
        }
        
        // 일반적인 이미지 URL 패턴 시도
        imageUrlPattern = Pattern.compile("(https?://[^\\s]+\\.[^\\s]+(?:png|jpg|jpeg|gif|webp))");
        imageUrlMatcher = imageUrlPattern.matcher(content);
        if (imageUrlMatcher.find()) {
            String imageUrl = imageUrlMatcher.group(1).trim();
            log.info("Found image URL from general pattern: {}", imageUrl);
            return validateAndCleanImageUrl(imageUrl);
        }
        
        log.warn("No image URL found in AI response");
        return null;
    }
    
    private String validateAndCleanImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }
        
        // URL 정리
        String cleanedUrl = imageUrl.trim();
        
        // Imgur 도메인만 허용
        if (!cleanedUrl.contains("imgur.com") && !cleanedUrl.contains("i.imgur.com")) {
            log.warn("Image URL is not from Imgur: {}", cleanedUrl);
            return null;
        }
        
        // 일반적인 이미지 확장자 확인
        if (!cleanedUrl.matches(".*\\.(jpg|jpeg|png|gif|webp)(\\?.*)?$")) {
            log.warn("Invalid image URL format: {}", cleanedUrl);
            return null;
        }
        
        // Imgur URL 형식 검증
        if (!cleanedUrl.matches("https?://(?:i\\.)?imgur\\.com/[a-zA-Z0-9]+\\.[a-zA-Z]{3,4}(\\?.*)?$")) {
            log.warn("Invalid Imgur URL format: {}", cleanedUrl);
            return null;
        }
        
        log.info("Valid Imgur image URL found: {}", cleanedUrl);
        return cleanedUrl;
    }

    private String extractIngredientsFromCookingMethod(String content) {
        // 조리 방법 섹션 찾기
        Pattern methodPattern = Pattern.compile("4\\.\\s*조리 방법\\s*:\\s*(.+?)(?=\\n\\d\\.|\\n요리 팁|$)", Pattern.DOTALL);
        Matcher methodMatcher = methodPattern.matcher(content);
        
        if (!methodMatcher.find()) {
            return null;
        }
        
        String cookingMethod = methodMatcher.group(1);
        
        // 일반적인 요리 재료 패턴들
        Set<String> ingredients = new HashSet<>();
        
        // 돼지고기, 소고기, 닭고기 등 고기류
        Pattern meatPattern = Pattern.compile("(돼지고기|소고기|닭고기|돈까스|삼겹살|목살|등심|안심|갈비|갈비살|양고기|오리고기|돈육|소주|미림|간장|고추장|고춧가루|설탕|굴소스|후추|식용유|다진 마늘|양파|대파|채 썰어|팬|중불|볶아|향을 낸다|익을 때까지|양념장|고루 섞어|수분을 날리면서|국물이 거의 없어질 때까지|완성된|밥 위에|덮밥 형태)");
        Matcher meatMatcher = meatPattern.matcher(cookingMethod);
        while (meatMatcher.find()) {
            String ingredient = meatMatcher.group(1);
            if (ingredient.length() > 1) { // 한 글자 제외
                ingredients.add(ingredient);
            }
        }
        
        // 채소류
        Pattern vegPattern = Pattern.compile("(양파|대파|마늘|고추|당근|감자|고구마|버섯|상추|깻잎|쌈채소|김치|무|배추|시금치|부추|파|쪽파|청양고추|홍고추)");
        Matcher vegMatcher = vegPattern.matcher(cookingMethod);
        while (vegMatcher.find()) {
            ingredients.add(vegMatcher.group(1));
        }
        
        // 양념류
        Pattern seasoningPattern = Pattern.compile("(소금|후추|간장|고추장|고춧가루|설탕|굴소스|미림|소주|식용유|참기름|들기름|겨자|와사비|마요네즈|케찹|머스타드|올리브오일|식초|레몬즙|다진 마늘|다진 생강|다진 파|다진 양파)");
        Matcher seasoningMatcher = seasoningPattern.matcher(cookingMethod);
        while (seasoningMatcher.find()) {
            ingredients.add(seasoningMatcher.group(1));
        }
        
        // 기타 재료
        Pattern otherPattern = Pattern.compile("(밥|국수|라면|떡|만두|김치|된장|고추장|두부|계란|달걀|치즈|버터|우유|생크림|밀가루|부침가루|빵가루|깨|들깨|참깨|흑임자|견과류|아몬드|호두|땅콩)");
        Matcher otherMatcher = otherPattern.matcher(cookingMethod);
        while (otherMatcher.find()) {
            ingredients.add(otherMatcher.group(1));
        }
        
        if (ingredients.isEmpty()) {
            return null;
        }
        
        // 재료 목록 생성
        StringBuilder result = new StringBuilder();
        for (String ingredient : ingredients) {
            result.append("- ").append(ingredient).append("\n");
        }
        
        log.info("Extracted ingredients from cooking method: {}", ingredients);
        return result.toString();
    }

    private List<Recipe> parseMenuRecommendationResponse(String content, Integer userId) {
        log.info("Starting to parse menu recommendation response");
        log.debug("Content to parse: {}", content);
        
        List<Recipe> recipes = new ArrayList<>();
        
        // AI 응답에서 여러 메뉴 추천을 파싱
        // 예: "1. 김치찌개\n2. 된장찌개\n3. 미역국" 형태로 파싱
        String[] lines = content.split("\\n");
        int menuCount = 0;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 번호로 시작하는 메뉴 찾기 (1., 2., 3. 등)
            if (line.matches("^\\d+\\..*")) {
                String menuName = line.replaceAll("^\\d+\\.\\s*", "").trim();
                if (!menuName.isEmpty()) {
                    // 메뉴명에 해당하는 상세 레시피 정보를 파싱하여 DB에 저장
                    Recipe recipe = parseAndSaveDetailedRecipeFromMenu(content, menuName, userId);
                    if (recipe != null) {
                        recipes.add(recipe);
                        menuCount++;
                        
                        // 최대 5개 메뉴까지만 추천
                        if (menuCount >= 5) break;
                    }
                }
            }
        }
        
        // 메뉴를 찾지 못한 경우 기본 메뉴 추천
        if (recipes.isEmpty()) {
            Recipe defaultRecipe = new Recipe();
            defaultRecipe.setTitle("메뉴 추천");
            defaultRecipe.setCategory("기타");
            defaultRecipe.setCookingTime(0);
            defaultRecipe.setDifficulty("중");
            defaultRecipe.setDescription(content);
            defaultRecipe.setImageUrl("https://i.imgur.com/8tMUxoP.jpg");
            recipes.add(defaultRecipe);
        }
        
        log.info("Parsed {} menu recommendations", recipes.size());
        return recipes;
    }

    private Recipe parseAndSaveDetailedRecipeFromMenu(String recipeContent, String menuName, Integer userId) {
        try {
            log.info("Parsing detailed recipe for menu: {}", menuName);
            
            // 메뉴명에 해당하는 상세 레시피 정보를 파싱
            Recipe recipe = new Recipe();
            recipe.setTitle(menuName);
            
            // 카테고리 파싱
            String category = parseCategoryFromResponse(recipeContent);
            recipe.setCategory(category);
            
            // 이미지 URL 파싱
            String imageUrl = parseImageUrlFromResponse(recipeContent);
            if (imageUrl != null) {
                recipe.setImageUrl(imageUrl);
            } else {
                recipe.setImageUrl("https://i.imgur.com/8tMUxoP.jpg");
            }
            
            // 조리 시간 파싱
            Pattern timePattern = Pattern.compile("3\\.\\s*조리 시간\\s*:\\s*(\\d+)\\s*분");
            Matcher timeMatcher = timePattern.matcher(recipeContent);
            if (timeMatcher.find()) {
                recipe.setCookingTime(Integer.parseInt(timeMatcher.group(1)));
            } else {
                recipe.setCookingTime(30); // 기본값
            }
            
            // 난이도 파싱
            Pattern difficultyPattern = Pattern.compile("6\\.\\s*난이도\\s*:\\s*(.+?)(?=\\n|$)");
            Matcher difficultyMatcher = difficultyPattern.matcher(recipeContent);
            if (difficultyMatcher.find()) {
                String difficulty = difficultyMatcher.group(1).trim();
                recipe.setDifficulty(normalizeDifficulty(difficulty));
            } else {
                recipe.setDifficulty("중"); // 기본값
            }
            
            // 재료와 양 파싱
            String ingredientsText = null;
            Pattern ingredientPattern = Pattern.compile("5\\.\\s*필요한 재료와 양\\s*:\\s*(.+?)(?=\\n\\d\\.|\\n조리 방법|$)", Pattern.DOTALL);
            Matcher ingredientMatcher = ingredientPattern.matcher(recipeContent);
            if (ingredientMatcher.find()) {
                ingredientsText = ingredientMatcher.group(1).trim();
            }
            
            // 조리 방법 파싱
            String cookingMethod = parseCookingMethod(recipeContent);
            
            // description 구성
            StringBuilder descriptionBuilder = new StringBuilder();
            if (ingredientsText != null) {
                descriptionBuilder.append("필요한 재료와 양:\n").append(ingredientsText.trim()).append("\n\n");
            }
            if (cookingMethod != null) {
                descriptionBuilder.append(cookingMethod);
            } else {
                descriptionBuilder.append("조리 방법 정보를 찾을 수 없습니다.\n\n");
            }
            
            recipe.setDescription(descriptionBuilder.toString().trim());
            
            // 상세 레시피 정보가 충분한 경우에만 DB에 저장
            if (recipe.getDescription() != null && 
                !recipe.getDescription().contains("조리 방법 정보를 찾을 수 없습니다") &&
                recipe.getDescription().contains("조리 방법:")) {
                
                try {
                    // 제목으로 기존 레시피가 있는지 확인
                    Recipe existingRecipe = recipeRepository.findByTitle(recipe.getTitle());
                    if (existingRecipe != null) {
                        log.info("Found existing recipe with same title: {}", recipe.getTitle());
                        return existingRecipe;
                    }
                    
                    // 새로운 레시피 저장
                    log.info("Saving new detailed recipe for menu: {}", recipe.getTitle());
                    recipe = recipeRepository.save(recipe);
                    log.info("Saved detailed recipe with ID: {}", recipe.getRecipeId());
                    
                    return recipe;
                } catch (Exception e) {
                    log.error("Error saving detailed recipe for menu: {}", menuName, e);
                    // 저장 실패해도 레시피는 반환
                    return recipe;
                }
            } else {
                log.warn("Insufficient recipe information for menu: {}. Not saving to DB.", menuName);
                // 상세 정보가 부족한 경우 DB 저장하지 않고 반환
                return recipe;
            }
            
        } catch (Exception e) {
            log.error("Error parsing detailed recipe for menu: {}", menuName, e);
            return null;
        }
    }
}