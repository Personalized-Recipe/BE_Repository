package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.RecipeResponseDTO;
import ac.su.kdt.prompttest.entity.Recipe;
import ac.su.kdt.prompttest.service.PerplexityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final PerplexityService perplexityService;

    @GetMapping("/perplexity")
    public ResponseEntity<String> testPerplexity() {
        String testPrompt = "간단한 김치찌개 레시피를 알려줘";
        RecipeResponseDTO recipeResponse = perplexityService.getResponse(1, testPrompt, false, true);
        
        String response;
        if (recipeResponse.getType().equals("recipe-detail")) {
            Recipe recipe = recipeResponse.getRecipe();
            response = recipe.getTitle() + "\n" + recipe.getDescription();
        } else {
            response = "메뉴 추천:\n";
            for (Recipe recipe : recipeResponse.getRecipes()) {
                response += "- " + recipe.getTitle() + "\n";
            }
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ai-content")
    public String getAIContent(@RequestBody Map<String, Object> req) {
        Integer userId = (Integer) req.get("userId");
        String request = (String) req.get("request");
        RecipeResponseDTO recipeResponse = perplexityService.getResponse(userId, request, false, false);
        
        String response;
        if (recipeResponse.getType().equals("recipe-detail")) {
            Recipe recipe = recipeResponse.getRecipe();
            response = recipe.getTitle() + "\n" + recipe.getDescription();
        } else {
            response = "메뉴 추천:\n";
            for (Recipe recipe : recipeResponse.getRecipes()) {
                response += "- " + recipe.getTitle() + "\n";
            }
        }
        
        return response;
    }
} 