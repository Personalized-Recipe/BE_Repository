package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.entity.UserPrompt;
import ac.su.kdt.prompttest.service.UserPromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ac.su.kdt.prompttest.dto.UserPromptDTO;
import ac.su.kdt.prompttest.entity.User;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserPromptController {

    private final UserPromptService userPromptService;

    /**
     * 사용자의 프롬프트 정보를 조회합니다.
     * @param userId 사용자 ID
     * @return UserPromptDTO 객체
     */
    @GetMapping("/{userId}/prompt")
    public ResponseEntity<?> getUserPrompt(@PathVariable Integer userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            if (!user.getUserId().equals(userId)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Permission denied"));
            }

            UserPrompt userPrompt = userPromptService.getUserPrompt(userId);
            if (userPrompt == null) {
                return ResponseEntity.notFound().build();
            }
            UserPromptDTO dto = new UserPromptDTO();
            dto.setName(userPrompt.getName());
            dto.setAge(userPrompt.getAge());
            dto.setGender(userPrompt.getGender());
            dto.setIsPregnant(userPrompt.getIsPregnant());
            dto.setHealthStatus(userPrompt.getHealthStatus());
            dto.setAllergy(userPrompt.getAllergy());
            dto.setPreference(userPrompt.getPreference());
            dto.setNickname(userPrompt.getNickname());
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            log.error("Error getting user prompt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 새로운 사용자 프롬프트 정보를 생성합니다.
     * @param userId 사용자 ID
     * @param promptData 프롬프트 데이터 (Map 형태)
     * @return 생성된 UserPrompt 객체
     */
    @PostMapping("/{userId}/prompt")
    public ResponseEntity<?> createUserPrompt(
            @PathVariable Integer userId,
            @RequestBody Map<String, Object> promptData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            if (!user.getUserId().equals(userId)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Permission denied"));
            }

            log.info("프롬프트 생성 요청 - userId: {}, data: {}", userId, promptData);
            
            // Map을 UserPrompt 엔티티로 변환
            UserPrompt userPrompt = new UserPrompt();
            userPrompt.setName((String) promptData.get("name"));
            userPrompt.setNickname((String) promptData.get("nickname"));
            
            // age 필드 안전한 타입 변환
            Object ageObj = promptData.get("age");
            if (ageObj != null) {
                if (ageObj instanceof Integer) {
                    userPrompt.setAge((Integer) ageObj);
                } else if (ageObj instanceof String) {
                    try {
                        userPrompt.setAge(Integer.parseInt((String) ageObj));
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid age format: " + ageObj);
                    }
                } else {
                    throw new RuntimeException("Invalid age type: " + ageObj.getClass().getSimpleName());
                }
            }
            
            userPrompt.setGender((String) promptData.get("gender"));
            
            // isPregnant 필드 안전한 타입 변환
            Object isPregnantObj = promptData.get("isPregnant");
            if (isPregnantObj != null) {
                if (isPregnantObj instanceof Boolean) {
                    userPrompt.setIsPregnant((Boolean) isPregnantObj);
                } else if (isPregnantObj instanceof String) {
                    String isPregnantStr = (String) isPregnantObj;
                    if ("true".equalsIgnoreCase(isPregnantStr) || "yes".equalsIgnoreCase(isPregnantStr)) {
                        userPrompt.setIsPregnant(true);
                    } else if ("false".equalsIgnoreCase(isPregnantStr) || "no".equalsIgnoreCase(isPregnantStr) || "none".equalsIgnoreCase(isPregnantStr)) {
                        userPrompt.setIsPregnant(false);
                    } else {
                        userPrompt.setIsPregnant(false); // 기본값
                    }
                } else {
                    userPrompt.setIsPregnant(false); // 기본값
                }
            }
            
            userPrompt.setAllergy((String) promptData.get("allergy"));
            userPrompt.setHealthStatus((String) promptData.get("healthStatus"));
            userPrompt.setPreference((String) promptData.get("preference"));
            
            UserPrompt createdPrompt = userPromptService.createUserPrompt(userId, userPrompt);
            log.info("프롬프트 생성 성공 - promptId: {}", createdPrompt.getPromptId());
            return ResponseEntity.ok(createdPrompt);
        } catch (RuntimeException e) {
            log.error("Error creating user prompt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 사용자의 프롬프트 정보를 업데이트합니다.
     * @param userId 사용자 ID
     * @param userPrompt 업데이트할 프롬프트 정보
     * @return 업데이트된 UserPrompt 객체
     */
    @PutMapping("/{userId}/prompt")
    public ResponseEntity<?> updateUserPrompt(
            @PathVariable Integer userId,
            @RequestBody UserPrompt userPrompt) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            if (!user.getUserId().equals(userId)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Permission denied"));
            }

            UserPrompt updatedPrompt = userPromptService.updateUserPrompt(userId, userPrompt);
            return ResponseEntity.ok(updatedPrompt);
        } catch (RuntimeException e) {
            log.error("Error updating user prompt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 사용자의 프롬프트 정보를 삭제합니다.
     * @param userId 사용자 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{userId}/prompt")
    public ResponseEntity<?> deleteUserPrompt(@PathVariable Integer userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            if (!user.getUserId().equals(userId)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Permission denied"));
            }

            userPromptService.deleteUserPrompt(userId);
            return ResponseEntity.ok(createSuccessResponse("User prompt deleted successfully"));
        } catch (RuntimeException e) {
            log.error("Error deleting user prompt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 특정 필드만 업데이트합니다.
     * @param userId 사용자 ID
     * @param field 업데이트할 필드명
     * @param value 업데이트할 값
     * @return 업데이트된 UserPrompt 객체
     */
    @PatchMapping("/{userId}/fields/{field}")
    public ResponseEntity<?> updateUserPromptField(
            @PathVariable Integer userId,
            @PathVariable String field,
            @RequestBody Object value) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            if (!user.getUserId().equals(userId)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Permission denied"));
            }

            UserPrompt updatedPrompt = userPromptService.updateUserPromptField(userId, field, value);
            return ResponseEntity.ok(updatedPrompt);
        } catch (RuntimeException e) {
            log.error("Error updating user prompt field: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 에러 응답을 생성합니다.
     * @param message 에러 메시지
     * @return 에러 응답 맵
     */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }

    /**
     * 성공 응답을 생성합니다.
     * @param message 성공 메시지
     * @return 성공 응답 맵
     */
    private Map<String, String> createSuccessResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return response;
    }
} 