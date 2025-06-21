package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.RefrigeratorIngredientDTO;
import ac.su.kdt.prompttest.dto.RefrigeratorIngredientRequestDTO;
import ac.su.kdt.prompttest.service.RefrigeratorIngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refrigerators/{refrigeratorId}/ingredients")
@RequiredArgsConstructor
public class RefrigeratorIngredientController {
    
    private final RefrigeratorIngredientService refrigeratorIngredientService;
    
    @GetMapping
    public ResponseEntity<List<RefrigeratorIngredientDTO>> getRefrigeratorIngredients(
            @PathVariable Integer refrigeratorId) {
        List<RefrigeratorIngredientDTO> ingredients = refrigeratorIngredientService.getRefrigeratorIngredients(refrigeratorId);
        return ResponseEntity.ok(ingredients);
    }
    
    @GetMapping("/{ingredientId}")
    public ResponseEntity<RefrigeratorIngredientDTO> getRefrigeratorIngredient(
            @PathVariable Integer refrigeratorId,
            @PathVariable Integer ingredientId) {
        RefrigeratorIngredientDTO ingredient = refrigeratorIngredientService.getRefrigeratorIngredient(ingredientId);
        return ResponseEntity.ok(ingredient);
    }
    
    @PostMapping
    public ResponseEntity<RefrigeratorIngredientDTO> addIngredient(
            @PathVariable Integer refrigeratorId,
            @RequestBody RefrigeratorIngredientRequestDTO requestDTO) {
        RefrigeratorIngredientDTO ingredient = refrigeratorIngredientService.addIngredient(refrigeratorId, requestDTO);
        return ResponseEntity.ok(ingredient);
    }
    
    @PutMapping("/{ingredientId}")
    public ResponseEntity<RefrigeratorIngredientDTO> updateIngredient(
            @PathVariable Integer refrigeratorId,
            @PathVariable Integer ingredientId,
            @RequestBody RefrigeratorIngredientRequestDTO requestDTO) {
        RefrigeratorIngredientDTO ingredient = refrigeratorIngredientService.updateIngredient(ingredientId, requestDTO);
        return ResponseEntity.ok(ingredient);
    }
    
    @DeleteMapping("/{ingredientId}")
    public ResponseEntity<Void> deleteIngredient(
            @PathVariable Integer refrigeratorId,
            @PathVariable Integer ingredientId) {
        refrigeratorIngredientService.deleteIngredient(ingredientId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/expiring")
    public ResponseEntity<List<RefrigeratorIngredientDTO>> getExpiringIngredients(
            @PathVariable Integer refrigeratorId) {
        List<RefrigeratorIngredientDTO> ingredients = refrigeratorIngredientService.getExpiringIngredients(refrigeratorId);
        return ResponseEntity.ok(ingredients);
    }
    
    @GetMapping("/expired")
    public ResponseEntity<List<RefrigeratorIngredientDTO>> getExpiredIngredients(
            @PathVariable Integer refrigeratorId) {
        List<RefrigeratorIngredientDTO> ingredients = refrigeratorIngredientService.getExpiredIngredients(refrigeratorId);
        return ResponseEntity.ok(ingredients);
    }
    
    @GetMapping("/location/{storageLocation}")
    public ResponseEntity<List<RefrigeratorIngredientDTO>> getIngredientsByStorageLocation(
            @PathVariable Integer refrigeratorId,
            @PathVariable String storageLocation) {
        List<RefrigeratorIngredientDTO> ingredients = refrigeratorIngredientService.getIngredientsByStorageLocation(refrigeratorId, storageLocation);
        return ResponseEntity.ok(ingredients);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<RefrigeratorIngredientDTO>> searchIngredients(
            @PathVariable Integer refrigeratorId,
            @RequestParam(value = "name", required = false) String ingredientName,
            @RequestParam(value = "q", required = false) String query) {
        // q 파라미터가 있으면 우선 사용, 없으면 name 파라미터 사용
        String searchTerm = query != null ? query : ingredientName;
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // 빈 검색어일 때는 빈 리스트 반환
            return ResponseEntity.ok(List.of());
        }
        List<RefrigeratorIngredientDTO> ingredients = refrigeratorIngredientService.searchIngredients(refrigeratorId, searchTerm.trim());
        return ResponseEntity.ok(ingredients);
    }
    
    @PostMapping("/search")
    public ResponseEntity<List<RefrigeratorIngredientDTO>> searchIngredientsPost(
            @PathVariable Integer refrigeratorId,
            @RequestBody SearchRequest searchRequest) {
        if (searchRequest.getQuery() == null || searchRequest.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<RefrigeratorIngredientDTO> ingredients = refrigeratorIngredientService.searchIngredients(refrigeratorId, searchRequest.getQuery().trim());
        return ResponseEntity.ok(ingredients);
    }
    
    // 검색 요청을 위한 내부 클래스
    public static class SearchRequest {
        private String query;
        
        public String getQuery() {
            return query;
        }
        
        public void setQuery(String query) {
            this.query = query;
        }
    }
} 