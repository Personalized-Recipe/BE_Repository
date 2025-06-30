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
import java.util.Map;

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
        try {
            System.out.println("=== 레시피 조회 요청 ===");
            System.out.println("요청된 recipeId: " + recipeId);
            
            Recipe recipe = recipeService.getRecipeById(recipeId);
            
            System.out.println("조회된 레시피: " + (recipe != null ? recipe.getTitle() : "null"));
            System.out.println("=== 레시피 조회 완료 ===");
            
            return ResponseEntity.ok(recipe);
        } catch (Exception e) {
            System.err.println("레시피 조회 중 에러 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping("/detail") // 메뉴 클릭 시 특정 레시피 상세 조회
    public ResponseEntity<RecipeResponseDTO> getRecipeDetail(@RequestBody RecipeRequestDTO recipeRequest) {
        // 특정 레시피 요청으로 처리 (isSpecificRecipe: true)
        RecipeResponseDTO recipeResponse = recipeService.requestRecipe(
            recipeRequest.getUserId(),
            recipeRequest.getChatRoomId(),
            recipeRequest.getRequest(),
            recipeRequest.getUseRefrigerator(),
            true // isSpecificRecipe를 true로 설정
        );
        return ResponseEntity.ok(recipeResponse);
    }
    
    /**
     * 잘못된 메뉴명으로 저장된 레시피들을 조회 (관리자용)
     * @return 잘못된 메뉴명 목록
     */
    @GetMapping("/invalid-titles")
    public ResponseEntity<List<Map<String, Object>>> getInvalidRecipeTitles() {
        try {
            List<Map<String, Object>> invalidRecipes = recipeService.findInvalidRecipeTitles();
            return ResponseEntity.ok(invalidRecipes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 잘못된 메뉴명의 레시피를 삭제 (관리자용)
     * @param recipeId 레시피 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/cleanup/{recipeId}")
    public ResponseEntity<Map<String, String>> cleanupInvalidRecipe(@PathVariable Integer recipeId) {
        try {
            recipeService.deleteInvalidRecipe(recipeId);
            return ResponseEntity.ok(Map.of("message", "잘못된 레시피가 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}