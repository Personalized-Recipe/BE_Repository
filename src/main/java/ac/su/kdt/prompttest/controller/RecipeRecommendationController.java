package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.RecipeRecommendationRequestDTO;
import ac.su.kdt.prompttest.entity.Recipe;
import ac.su.kdt.prompttest.service.PerplexityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/recipe-recommendations")
@RequiredArgsConstructor
public class RecipeRecommendationController {

    private final PerplexityService perplexityService;

    /**
     * 냉장고 재료를 기반으로 레시피 추천
     */
    @PostMapping("/refrigerator")
    public ResponseEntity<Recipe> getRecipeRecommendation(
            @RequestParam Integer userId,
            @RequestBody RecipeRecommendationRequestDTO requestDTO) {
        
        log.info("Recipe recommendation request - userId: {}, refrigeratorId: {}", userId, requestDTO.getRefrigeratorId());
        
        try {
            Recipe recipe = perplexityService.getRecipeRecommendation(userId, requestDTO);
            return ResponseEntity.ok(recipe);
            
        } catch (Exception e) {
            log.error("Error generating recipe recommendation", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 간단한 텍스트 기반 레시피 추천 (기존 기능)
     */
    @PostMapping("/text")
    public ResponseEntity<Recipe> getTextBasedRecommendation(
            @RequestParam Integer userId,
            @RequestParam String prompt) {
        
        log.info("Text-based recipe recommendation request - userId: {}, prompt: {}", userId, prompt);
        
        try {
            Recipe recipe = perplexityService.getResponse(userId, prompt);
            return ResponseEntity.ok(recipe);
            
        } catch (Exception e) {
            log.error("Error generating text-based recipe recommendation", e);
            return ResponseEntity.badRequest().build();
        }
    }
} 