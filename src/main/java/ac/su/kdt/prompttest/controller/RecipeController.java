package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.RecipeRequestDTO;
import ac.su.kdt.prompttest.dto.RecipeResponseDTO;
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
    
    @PostMapping("/request") // 사용자의 요청을 받아 레시피 생성
    public ResponseEntity<RecipeResponseDTO> requestRecipe(@RequestBody RecipeRequestDTO recipeRequest) {
        RecipeResponseDTO recipeResponse = recipeService.requestRecipe(
            recipeRequest.getUserId(),
            recipeRequest.getChatRoomId(),
            recipeRequest.getRequest(),
            recipeRequest.getUseRefrigerator(),
            recipeRequest.getIsSpecificRecipe()
        );
        return ResponseEntity.ok(recipeResponse);
    }
    
    @GetMapping("/history/{userId}") // 사용자의 레시피 조회 이력 조회
    public ResponseEntity<List<UserRecipe>> getRecipeHistory(@PathVariable Integer userId) {
        List<UserRecipe> recipes = recipeService.getRecipeHistory(userId);
        return ResponseEntity.ok(recipes);
    }

    @PostMapping("/save/{userId}/{recipeId}") // 사용자가 레시피를 저장
    public ResponseEntity<UserRecipe> saveRecipe(
            @PathVariable Integer userId,
            @PathVariable Integer recipeId) {
        UserRecipe savedRecipe = recipeService.saveUserRecipe(userId, recipeId);
        return ResponseEntity.ok(savedRecipe);
    }

    @DeleteMapping("/delete/{userId}/{recipeId}") // 사용자가 저장한 레시피 삭제
    public ResponseEntity<Void> deleteRecipe(
            @PathVariable Integer userId,
            @PathVariable Integer recipeId) {
        recipeService.deleteUserRecipe(userId, recipeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{recipeId}") // 레시피 상세 정보 조회
    public ResponseEntity<Recipe> getRecipe(@PathVariable Integer recipeId) {
        Recipe recipe = recipeService.getRecipeById(recipeId);
        return ResponseEntity.ok(recipe);
    }
}