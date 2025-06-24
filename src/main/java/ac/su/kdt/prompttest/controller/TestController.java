package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final JwtService jwtService;

    /**
     * JWT 토큰에서 userId 추출 테스트
     * @param token JWT 토큰
     * @return 추출된 userId와 username
     */
    @PostMapping("/extract-token")
    public ResponseEntity<Map<String, Object>> extractTokenInfo(@RequestBody String token) {
        try {
            String username = jwtService.extractUsername(token);
            Integer userId = jwtService.extractUserId(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("userId", userId);
            response.put("success", true);
            
            log.info("토큰에서 추출된 정보: userId={}, username={}", userId, username);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("토큰 추출 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("success", false);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 