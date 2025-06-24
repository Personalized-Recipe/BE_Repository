package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.entity.Ingredient;
import ac.su.kdt.prompttest.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
public class IngredientController {
    private final IngredientRepository ingredientRepository;

    /**
     * 재료 추가 (이름+등록자 중복 방지)
     * - userId를 파라미터로 받아 creatorUserId에 저장
     * - 같은 사용자가 같은 이름의 재료를 중복 등록하지 못하게 함
     * - 다른 사용자는 같은 이름의 재료를 등록할 수 있음
     */
    @PostMapping
    public ResponseEntity<?> addIngredient(@RequestBody Ingredient ingredient, @RequestParam Integer userId) {
        // 이름+등록자(userId) 기준으로 이미 등록된 재료가 있는지 확인
        Optional<Ingredient> existing = ingredientRepository.findByNameAndCreatorUserId(ingredient.getName(), userId);
        if (existing.isPresent()) {
            // 이미 있으면 409(CONFLICT) 에러 반환
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 같은 이름의 재료가 등록되어 있습니다.");
        }
        // 등록자 정보 저장
        ingredient.setCreatorUserId(userId);
        return ResponseEntity.ok(ingredientRepository.save(ingredient));
    }

    /**
     * 모든 재료 조회
     */
    @GetMapping
    public ResponseEntity<List<Ingredient>> getAllIngredients() {
        List<Ingredient> ingredients = ingredientRepository.findAll();
        return ResponseEntity.ok(ingredients);
    }

    /**
     * ID로 재료 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getIngredientById(@PathVariable Integer id) {
        Optional<Ingredient> ingredient = ingredientRepository.findById(id);
        if (ingredient.isPresent()) {
            return ResponseEntity.ok(ingredient.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("재료를 찾을 수 없습니다.");
        }
    }

    /**
     * 이름으로 재료 검색
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchIngredientsByName(@RequestParam String name) {
        Optional<Ingredient> ingredient = ingredientRepository.findByName(name);
        if (ingredient.isPresent()) {
            return ResponseEntity.ok(ingredient.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 이름의 재료를 찾을 수 없습니다.");
        }
    }

    /**
     * 재료 삭제 (등록자만 삭제 가능)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteIngredient(@PathVariable Integer id, @RequestParam Integer userId) {
        Optional<Ingredient> ingredient = ingredientRepository.findById(id);
        if (ingredient.isPresent()) {
            Ingredient foundIngredient = ingredient.get();
            // 등록자만 삭제할 수 있도록 확인
            if (foundIngredient.getCreatorUserId().equals(userId)) {
                ingredientRepository.deleteById(id);
                return ResponseEntity.ok("재료가 성공적으로 삭제되었습니다.");
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("재료를 삭제할 권한이 없습니다.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("삭제할 재료를 찾을 수 없습니다.");
        }
    }
} 