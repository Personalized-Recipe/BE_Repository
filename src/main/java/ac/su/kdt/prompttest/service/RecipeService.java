package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.Recipe;
import ac.su.kdt.prompttest.entity.UserRecipe;
import ac.su.kdt.prompttest.repository.RecipeRepository;
import ac.su.kdt.prompttest.repository.UserRecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RecipeService {
    
    private final RecipeRepository recipeRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final PromptService promptService;
    private final PerplexityService perplexityService;

    @Transactional
    public Recipe requestRecipe(Integer userId, String request) {
        // 프롬프트 생성
        String prompt = promptService.generatePrompt(userId, request); // 이 프롬프트는 사용자의 프롬프트
        
        // Perplexity API를 통해 레시피 생성
        String response = perplexityService.getResponseAsString(userId, prompt);
        
        // 결과 저장
        Recipe recipe = Recipe.builder()
                .title(extractTitle(response))
                .description(response)
                .category(determineCategory(response))
                .cookingTime(extractCookingTime(response))
                .difficulty(determineDifficulty(response))
                .image(extractImage(response))
                .build();
        
        Recipe savedRecipe = recipeRepository.save(recipe);
        
        return savedRecipe;
    }
    
    public List<UserRecipe> getRecipeHistory(Integer userId) {
        return userRecipeRepository.findByUserId(userId);
    }

    @Transactional // 사용자가 레시피를 저장
    public UserRecipe saveUserRecipe(Integer userId, Integer recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
            .orElseThrow(() -> new RuntimeException("Recipe not found"));
            
        UserRecipe userRecipe = UserRecipe.builder()
            .userId(userId)
            .recipeId(recipeId)
            .build();
            
        return userRecipeRepository.save(userRecipe);
    }

    @Transactional // 사용자가 저장한 레시피 삭제
    public void deleteUserRecipe(Integer userId, Integer recipeId) {
        UserRecipe.UserRecipeId id = new UserRecipe.UserRecipeId(userId, recipeId);
        userRecipeRepository.deleteById(id);
    }

    public Recipe getRecipeById(Integer recipeId) { // 레시피 상세 조회
        return recipeRepository.findById(recipeId)
            .orElseThrow(() -> new RuntimeException("Recipe not found"));
    }

    // Helper methods
    private String extractTitle(String response) {
        // "1. 요리 이름: " 다음에 오는 텍스트를 추출
        Pattern pattern = Pattern.compile("1\\.\\s*요리 이름:\\s*([^\\n]+)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "레시피 제목"; // 기본값
    }

    private String determineCategory(String response) {
        // 한식, 중식, 일식, 양식, 분식, 퓨전 중 하나를 찾음
        Pattern pattern = Pattern.compile("(한식|중식|일식|양식|분식|퓨전)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "한식"; // 기본값
    }

    private Integer extractCookingTime(String response) {
        // "3. 조리 시간: " 다음에 오는 숫자를 추출
        Pattern pattern = Pattern.compile("3\\.\\s*조리 시간:\\s*(\\d+)분");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 30; // 기본값
    }

    private String determineDifficulty(String response) {
        // "4. 난이도: " 다음에 오는 상/중/하를 추출
        Pattern pattern = Pattern.compile("4\\.\\s*난이도:\\s*(상|중|하)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "중"; // 기본값
    }

    private String extractIngredients(String response) {
        // "2. 필요한 재료와 양:" 다음에 오는 텍스트를 추출
        Pattern pattern = Pattern.compile("2\\.\\s*필요한 재료와 양:\\s*([^3]+)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return ""; // 기본값
    }

    private byte[] extractImage(String response) {
        // TODO: 이미지 데이터 처리 로직 구현
        return null;
    }
} 