package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.UserIngredientDTO;
import ac.su.kdt.prompttest.entity.Ingredient;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.entity.UserIngredient;
import ac.su.kdt.prompttest.repository.IngredientRepository;
import ac.su.kdt.prompttest.repository.UserIngredientRepository;
import ac.su.kdt.prompttest.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {
    private final IngredientRepository ingredientRepository;
    private final UserIngredientRepository userIngredientRepository;
    private final UserService userService;

    /**
     * 특정 사용자의 보유 재료 목록 조회
     * GET /api/ingredients/{userId}
     */
    @GetMapping("/{userId}")
    @PreAuthorize("#userId == authentication.principal.userId")
    public ResponseEntity<List<UserIngredientDTO>> getUserIngredients(@PathVariable Integer userId) {
        List<UserIngredient> userIngredients = userIngredientRepository.findByUser_UserId(userId);
        List<UserIngredientDTO> dtoList = userIngredients.stream().map(ui -> {
            UserIngredientDTO dto = new UserIngredientDTO();
            dto.setUserId(ui.getId().getUserId());
            dto.setIngredientId(ui.getId().getIngredientId());
            dto.setIngredientName(ui.getIngredientName());
            dto.setAmount(ui.getAmount());
            dto.setUnit(ui.getUnit());
            return dto;
        }).toList();
        return ResponseEntity.ok(dtoList);
    }

    /**
     * 사용자에게 보유 재료 추가 (재료명으로 추가)
     * POST /api/ingredients/user/{userId}
     * - 재료명이 존재하면 해당 재료를 사용자의 보유 재료로 추가
     * - 재료명이 존재하지 않으면 새로 생성 후 추가
     */
    @PostMapping("/user/{userId}")
    @PreAuthorize("#userId == authentication.principal.userId")
    public ResponseEntity<?> addUserIngredient(
            @PathVariable Integer userId,
            @RequestBody UserIngredientDTO userIngredientDTO) {
        
        try {
            // 사용자 존재 확인
            User user = userService.getUserById(userId);
            
            // 프론트엔드 요청 구조 처리
            String ingredientName = userIngredientDTO.getName() != null ? 
                userIngredientDTO.getName() : userIngredientDTO.getIngredientName();
            
            Float amount = userIngredientDTO.getAmount();
            String unit = userIngredientDTO.getUnit() != null ? 
                userIngredientDTO.getUnit().toLowerCase() : "g";
            
            if (ingredientName == null || amount == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("재료명과 수량은 필수입니다.");
            }
            
            // 재료명으로 재료 찾기 또는 생성
            Optional<Ingredient> ingredientOpt = ingredientRepository.findByName(ingredientName);
            Ingredient ingredient;
            
            if (ingredientOpt.isPresent()) {
                ingredient = ingredientOpt.get();
            } else {
                // 재료가 없으면 새로 생성
                ingredient = new Ingredient();
                ingredient.setName(ingredientName);
                ingredient.setCreatorUserId(userId);
                ingredient = ingredientRepository.save(ingredient);
            }
            
            // 이미 사용자가 해당 재료를 보유하고 있는지 확인
            Optional<UserIngredient> existingUserIngredient = userIngredientRepository
                    .findByUserAndIngredient(user, ingredient);
            
            if (existingUserIngredient.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("이미 보유하고 있는 재료입니다: " + ingredientName);
            }
            
            // 사용자의 보유 재료로 추가
            UserIngredient userIngredient = UserIngredient.builder()
                    .id(new UserIngredient.UserIngredientId(userId, ingredient.getIngredientId()))
                    .user(user)
                    .ingredient(ingredient)
                    .ingredientName(ingredientName)
                    .amount(amount)
                    .unit(unit)
                    .build();
            
            UserIngredient savedUserIngredient = userIngredientRepository.save(userIngredient);
            
            // 응답 생성 시 상세 로그 추가
            try {
                UserIngredientDTO responseDto = new UserIngredientDTO();
                responseDto.setUserId(savedUserIngredient.getId().getUserId());
                responseDto.setIngredientId(savedUserIngredient.getId().getIngredientId());
                responseDto.setIngredientName(savedUserIngredient.getIngredientName());
                responseDto.setAmount(savedUserIngredient.getAmount());
                responseDto.setUnit(savedUserIngredient.getUnit());
                
                System.out.println("응답 DTO 생성 성공: " + responseDto);
                return ResponseEntity.ok(responseDto);
            } catch (Exception e) {
                System.err.println("응답 생성 중 에러: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("응답 생성 중 오류: " + e.getMessage());
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("보유 재료 추가 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 사용자의 보유 재료 수정 (ID 기반)
     * PUT /api/ingredients/{userId}/{ingredientId}
     */
    @PutMapping("/{userId}/{ingredientId}")
    @PreAuthorize("@userIngredientRepository.findById(new ac.su.kdt.prompttest.entity.UserIngredient.UserIngredientId(#userId, #ingredientId)).orElse(null)?.user.userId == authentication.principal.userId")
    public ResponseEntity<?> updateUserIngredient(
            @PathVariable Integer userId,
            @PathVariable Integer ingredientId,
            @RequestBody UserIngredientDTO userIngredientDTO) {
        try {
            UserIngredient.UserIngredientId key = new UserIngredient.UserIngredientId(userId, ingredientId);
            Optional<UserIngredient> userIngredientOpt = userIngredientRepository.findById(key);
            if (!userIngredientOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("보유 재료를 찾을 수 없습니다. userId: " + userId + ", ingredientId: " + ingredientId);
            }
            UserIngredient userIngredient = userIngredientOpt.get();
            userIngredient.setAmount(userIngredientDTO.getAmount());
            userIngredient.setUnit(userIngredientDTO.getUnit());
            UserIngredient savedUserIngredient = userIngredientRepository.save(userIngredient);
            UserIngredientDTO responseDto = new UserIngredientDTO();
            responseDto.setUserId(savedUserIngredient.getId().getUserId());
            responseDto.setIngredientId(savedUserIngredient.getId().getIngredientId());
            responseDto.setIngredientName(savedUserIngredient.getIngredientName());
            responseDto.setAmount(savedUserIngredient.getAmount());
            responseDto.setUnit(savedUserIngredient.getUnit());
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("보유 재료 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 사용자의 보유 재료 삭제 (ID 기반)
     * DELETE /api/ingredients/{userId}/{ingredientId}
     */
    @DeleteMapping("/{userId}/{ingredientId}")
    @PreAuthorize("@userIngredientRepository.findById(new ac.su.kdt.prompttest.entity.UserIngredient.UserIngredientId(#userId, #ingredientId)).orElse(null)?.user.userId == authentication.principal.userId")
    public ResponseEntity<?> deleteUserIngredient(@PathVariable Integer userId, @PathVariable Integer ingredientId) {
        try {
            UserIngredient.UserIngredientId key = new UserIngredient.UserIngredientId(userId, ingredientId);
            Optional<UserIngredient> userIngredientOpt = userIngredientRepository.findById(key);
            if (!userIngredientOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("보유 재료를 찾을 수 없습니다. userId: " + userId + ", ingredientId: " + ingredientId);
            }
            userIngredientRepository.delete(userIngredientOpt.get());
            return ResponseEntity.ok("보유 재료가 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("보유 재료 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 모든 재료 목록 조회 (보유 재료 추가 시 선택용)
     */
    @GetMapping("/all")
    public ResponseEntity<List<Ingredient>> getAllIngredients() {
        List<Ingredient> ingredients = ingredientRepository.findAll();
        return ResponseEntity.ok(ingredients);
    }

    /**
     * 재료명으로 검색 (보유 재료 추가 시 검색용)
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchIngredientsByName(@RequestParam String name) {
        Optional<Ingredient> ingredient = ingredientRepository.findByName(name);
        if (ingredient.isPresent()) {
            return ResponseEntity.ok(ingredient.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("해당 이름의 재료를 찾을 수 없습니다.");
        }
    }
} 