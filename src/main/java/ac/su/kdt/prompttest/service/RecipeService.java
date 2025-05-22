package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.Recipe;
import ac.su.kdt.prompttest.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeService {
    
    private final RecipeRepository recipeRepository;
    private final PromptService promptService;
    private final PerplexityService perplexityService;
    
    @Transactional
    public Recipe requestRecipe(Long userId, String request) {
        // 프롬프트 생성
        String prompt = promptService.generatePrompt(userId, request);
        
        // Perplexity API를 통해 레시피 생성
        String response = perplexityService.getResponse(userId, prompt);
        
        // 결과 저장
        Recipe recipe = Recipe.builder()
                .title(extractTitle(response))
                .description(response)
                .category(determineCategory(response))
                .cookingTime(extractCookingTime(response))
                .difficulty(determineDifficulty(response))
                .build();
        
        return recipeRepository.save(recipe);
    }
    
    public List<Recipe> getRecipeHistory(Long userId) {
        return recipeRepository.findByUserId(userId);
    }

    // Helper methods
    private String extractTitle(String response) {
        // TODO: Implement title extraction logic from response
        return "레시피 제목";
    }

    private String determineCategory(String response) {
        // TODO: Implement category determination logic from response
        return "한식";
    }

    private Integer extractCookingTime(String response) {
        // TODO: Implement cooking time extraction logic from response
        return 30;
    }

    private String determineDifficulty(String response) {
        // TODO: Implement difficulty determination logic from response
        return "중";
    }
} 