package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.entity.UserIngredient;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.entity.Ingredient;
import ac.su.kdt.prompttest.repository.UserIngredientRepository;
import ac.su.kdt.prompttest.repository.UserRepository;
import ac.su.kdt.prompttest.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ac.su.kdt.prompttest.dto.UserIngredientDTO;

import java.util.List;

@RestController
@RequestMapping("/user-ingredients")
@RequiredArgsConstructor
public class UserIngredientController {
    private final UserIngredientRepository userIngredientRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;

    // 유저의 재료 추가 (DTO 없이)
    @PostMapping
    public ResponseEntity<UserIngredient> addUserIngredient(
            @RequestParam Integer userId,
            @RequestParam Integer ingredientId,
            @RequestParam Float weightInGrams) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("검색한 유저가 없습니다..."));
        Ingredient ingredient = ingredientRepository.findById(ingredientId).orElseThrow(() -> new RuntimeException("검색한 재료가 없습니다..."));
        UserIngredient userIngredient = UserIngredient.builder()
                .user(user)
                .ingredient(ingredient)
                .weightInGrams(weightInGrams)
                .build();
        return ResponseEntity.ok(userIngredientRepository.save(userIngredient));
    }
        /*UserIngredient.builder():
        빌더 패턴을 사용해 UserIngredient 객체를 생성합니다.
        .user(user):
        위에서 찾은 User 객체(어떤 유저가)
        .ingredient(ingredient):
        위에서 찾은 Ingredient 객체(어떤 재료를)
        .weightInGrams(weightInGrams):
        파라미터로 받은 보유량(몇 g을)
        .build():
        위 정보를 바탕으로 UserIngredient 객체를 완성합니다.*/

    // 유저의 재료 목록 조회
    @GetMapping("/{userId}")
    public ResponseEntity<List<UserIngredientDTO>> getUserIngredients(@PathVariable Integer userId) {
        List<UserIngredient> userIngredients = userIngredientRepository.findByUser_UserId(userId);
        List<UserIngredientDTO> dtoList = userIngredients.stream().map(ui -> {
            UserIngredientDTO dto = new UserIngredientDTO();
            dto.setIngredientName(ui.getIngredient().getName());
            dto.setWeightInGrams(ui.getWeightInGrams());
            return dto;
        }).toList();
        return ResponseEntity.ok(dtoList);
    }

    // JSON으로 보유재료 등록 (ingredientName만 입력하면 자동으로 id 할당)
    @PostMapping("/json")
    public ResponseEntity<UserIngredient> addUserIngredientJson(@RequestBody UserIngredientRequest request) {
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("검색한 유저가 없습니다..."));
        Ingredient ingredient = ingredientRepository.findByName(request.getIngredientName())
            .orElseGet(() -> {
                Ingredient newIngredient = Ingredient.builder()
                    .name(request.getIngredientName())
                    .creatorUserId(request.getUserId())
                    .build();
                return ingredientRepository.save(newIngredient);
            });
        UserIngredient userIngredient = UserIngredient.builder()
            .user(user)
            .ingredient(ingredient)
            .weightInGrams(request.getWeightInGrams())
            .build();
        return ResponseEntity.ok(userIngredientRepository.save(userIngredient));
    }

    // DTO 클래스 (내부 static)
    public static class UserIngredientRequest {
        private Integer userId;
        private String ingredientName;
        private Float weightInGrams;
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }
        public String getIngredientName() { return ingredientName; }
        public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
        public Float getWeightInGrams() { return weightInGrams; }
        public void setWeightInGrams(Float weightInGrams) { this.weightInGrams = weightInGrams; }
    }
} 