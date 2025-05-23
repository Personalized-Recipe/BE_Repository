package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.RecipeRequestDTO;
import ac.su.kdt.prompttest.entity.Recipe;
import ac.su.kdt.prompttest.entity.UserRecipe;
import ac.su.kdt.prompttest.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {
    
    private final RecipeService recipeService;
    
    @PostMapping
    public ResponseEntity<Recipe> requestRecipe(@RequestBody RecipeRequestDTO recipeRequest) {
        Recipe recipe = recipeService.requestRecipe(
                recipeRequest.getUserId(),
                recipeRequest.getRequest()
        );
        return ResponseEntity.ok(recipe);
    }
    
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<UserRecipe>> getRecipeHistory(@PathVariable Integer userId) {
        List<UserRecipe> recipes = recipeService.getRecipeHistory(userId);
        return ResponseEntity.ok(recipes);
    }
}