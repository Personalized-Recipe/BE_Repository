package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.entity.UserPrompt;
import ac.su.kdt.prompttest.service.UserPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
     * @return UserPrompt 객체
     */
    @GetMapping("/{userId}/prompts")
    public ResponseEntity<?> getUserPrompts(@PathVariable Integer userId) {
        try {
            List<UserPrompt> userPrompts = userPromptService.getUserPrompts(userId);
            return ResponseEntity.ok(userPrompts);
        } catch (RuntimeException e) {
            log.error("Error getting user prompt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 새로운 사용자 프롬프트 정보를 생성합니다.
     * @param userId 사용자 ID
     * @param userPrompt 생성할 프롬프트 정보
     * @return 생성된 UserPrompt 객체
     */
    @PostMapping("/{userId}/prompts")
    public ResponseEntity<?> createUserPrompt(
            @PathVariable Integer userId,
            @RequestBody UserPrompt userPrompt) {
        try {
            // 필수 필드 설정
            if (userPrompt.getName() == null || userPrompt.getName().trim().isEmpty()) {
                userPrompt.setName("기본프롬프트");
            }
            if (userPrompt.getAge() == null) {
                userPrompt.setAge(25);
            }
            if (userPrompt.getGender() == null) {
                userPrompt.setGender("M");
            }
            
            UserPrompt createdPrompt = userPromptService.createUserPrompt(userId, userPrompt);
            return ResponseEntity.ok(createdPrompt);
        } catch (RuntimeException e) {
            log.error("Error creating user prompt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 사용자의 프롬프트 정보를 업데이트합니다.
     * @param userId 사용자 ID
     * @param promptId 업데이트할 프롬프트 ID
     * @param userPrompt 업데이트할 프롬프트 정보
     * @return 업데이트된 UserPrompt 객체
     */
    @PutMapping("/{userId}/prompts/{promptId}")
    public ResponseEntity<?> updateUserPrompt(
            @PathVariable Integer userId,
            @PathVariable Integer promptId,
            @RequestBody UserPrompt userPrompt) {
        try {
            UserPrompt updatedPrompt = userPromptService.updateUserPrompt(userId, promptId, userPrompt);
            return ResponseEntity.ok(updatedPrompt);
        } catch (RuntimeException e) {
            log.error("Error updating user prompt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 사용자의 프롬프트 정보를 삭제합니다.
     * @param userId 사용자 ID
     * @param promptId 삭제할 프롬프트 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{userId}/prompts/{promptId}")
    public ResponseEntity<?> deleteUserPrompt(@PathVariable Integer userId, @PathVariable Integer promptId) {
        try {
            userPromptService.deleteUserPrompt(userId, promptId);
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