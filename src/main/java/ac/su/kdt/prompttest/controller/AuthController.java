package ac.su.kdt.prompttest.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import ac.su.kdt.prompttest.service.AuthService;
import ac.su.kdt.prompttest.dto.TokenResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    // OAuth2 URL을 반환하는 엔드포인트들 (프론트엔드에서 사용)
    @GetMapping("/api/oauth/kakao/url")
    public ResponseEntity<?> getKakaoOAuthUrl() {
        String reqUrl = "https://kauth.kakao.com/oauth/authorize?client_id=" + kakaoClientId 
        + "&redirect_uri=http://localhost:5173/oauth/callback/kakao"
        + "&response_type=code&scope=profile_nickname,profile_image";
        return ResponseEntity.ok(Map.of("oauth_url", reqUrl));
    }

    @GetMapping("/api/oauth/google/url")
    public ResponseEntity<?> getGoogleOAuthUrl() {
        String reqUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + googleClientId 
        + "&redirect_uri=http://localhost:5173/oauth/callback/google"
        + "&response_type=code&scope=email profile";
        return ResponseEntity.ok(Map.of("oauth_url", reqUrl));
    }

    // 프론트엔드에서 인가코드를 받아서 처리하는 엔드포인트들
    @PostMapping("/api/oauth/kakao/callback")
    public ResponseEntity<?> kakaoCallback(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== 카카오 콜백 요청 수신 ===");
            System.out.println("요청 본문 전체: " + request);
            System.out.println("요청 본문 타입: " + request.getClass().getName());
            // 다양한 키로 코드 찾기
            String code = null;
            if (request.get("code") != null) {
                code = request.get("code").toString();
            } else if (request.get("authorization_code") != null) {
                code = request.get("authorization_code").toString();
            }
            System.out.println("추출된 인가코드: " + code);
            if (code == null || code.isEmpty()) {
                System.out.println("에러: 인가코드가 없음");
                System.out.println("사용 가능한 키들: " + request.keySet());
                return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
            }
            System.out.println("AuthService.handleKakaoCallback() 호출 시작...");
            TokenResponseDTO result = authService.handleKakaoCallback(code);
            System.out.println("AuthService.handleKakaoCallback() 호출 완료");
            System.out.println("AuthService 결과: " + result);
            System.out.println("로그인 성공: JWT 토큰 및 사용자 정보 생성 완료");
            
            // 프로필 이미지 정보 상세 로깅
            System.out.println("=== 프로필 이미지 정보 ===");
            System.out.println("result.getProfileImage(): " + result.getProfileImage());
            System.out.println("result.getUsername(): " + result.getUsername());
            System.out.println("result.getUserId(): " + result.getUserId());
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", result.getUserId());
            userInfo.put("username", result.getUsername());
            userInfo.put("profileImage", result.getProfileImage());
            userInfo.put("provider", result.getProvider());
            
            System.out.println("userInfo Map: " + userInfo);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", result.getToken());
            response.put("type", result.getType());
            response.put("user", userInfo);
            response.put("message", "Kakao login successful");
            response.put("action", "redirect_to_dashboard");
            
            System.out.println("최종 response: " + response);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.out.println("RuntimeException 발생: " + e.getMessage());
            e.printStackTrace();
            if (e.getMessage() != null && e.getMessage().contains("Authorization code is invalid or expired")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Authorization code has already been used or expired",
                    "message", "Please try logging in again",
                    "action", "redirect_to_login"
                ));
            }
            return ResponseEntity.status(500).body(Map.of("error", String.valueOf(e.getMessage()), "stack", Arrays.toString(e.getStackTrace())));
        } catch (Exception e) {
            System.out.println("Exception 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", String.valueOf(e.getMessage()), "stack", Arrays.toString(e.getStackTrace())));
        }
    }

    @PostMapping("/api/oauth/google/callback")
    public ResponseEntity<?> googleCallback(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== 구글 콜백 요청 수신 ===");
            System.out.println("요청 본문 전체: " + request);
            System.out.println("요청 본문 타입: " + request.getClass().getName());
            // 다양한 키로 코드 찾기
            String code = null;
            if (request.get("code") != null) {
                code = request.get("code").toString();
            } else if (request.get("authorization_code") != null) {
                code = request.get("authorization_code").toString();
            }
            System.out.println("추출된 인가코드: " + code);
            if (code == null || code.isEmpty()) {
                System.out.println("에러: 인가코드가 없음");
                System.out.println("사용 가능한 키들: " + request.keySet());
                return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
            }
            System.out.println("AuthService 호출 시작...");
            TokenResponseDTO result = authService.handleGoogleCallback(code);
            System.out.println("AuthService 결과: " + result);
            System.out.println("로그인 성공: JWT 토큰 및 사용자 정보 생성 완료");
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", result.getUserId());
            userInfo.put("username", result.getUsername());
            userInfo.put("profileImage", result.getProfileImage());
            userInfo.put("provider", result.getProvider());
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", result.getToken());
            response.put("type", result.getType());
            response.put("user", userInfo);
            response.put("message", "Google login successful");
            response.put("action", "redirect_to_dashboard");
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.out.println("에러: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", String.valueOf(e.getMessage()), "stack", Arrays.toString(e.getStackTrace())));
        } catch (Exception e) {
            System.out.println("예외 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", String.valueOf(e.getMessage()), "stack", Arrays.toString(e.getStackTrace())));
        }
    }
}

