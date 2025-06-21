package ac.su.kdt.prompttest.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import ac.su.kdt.prompttest.service.AuthService;
import ac.su.kdt.prompttest.service.JwtService;
import ac.su.kdt.prompttest.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    // OAuth2 URL을 반환하는 엔드포인트들 (프론트엔드에서 사용)
    @GetMapping("/api/oauth/kakao/url")
    public ResponseEntity<?> getKakaoOAuthUrl() {
        String reqUrl = "https://kauth.kakao.com/oauth/authorize?client_id=" + kakaoClientId 
        + "&redirect_uri=http://localhost:3000/oauth/callback/kakao"
        + "&response_type=code&scope=profile_nickname,profile_image";
        return ResponseEntity.ok(Map.of("oauth_url", reqUrl));
    }

    @GetMapping("/api/oauth/google/url")
    public ResponseEntity<?> getGoogleOAuthUrl() {
        String reqUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + googleClientId 
        + "&redirect_uri=http://localhost:3000/oauth/callback/google"
        + "&response_type=code&scope=email profile";
        return ResponseEntity.ok(Map.of("oauth_url", reqUrl));
    }

    // 프론트엔드에서 인가코드를 받아서 처리하는 엔드포인트들
    @PostMapping("/api/oauth/kakao/callback")
    public ResponseEntity<?> kakaoCallback(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            System.out.println("=== 카카오 콜백 요청 수신 ===");
            System.out.println("받은 인가코드: " + code);
            
            if (code == null || code.isEmpty()) {
                System.out.println("에러: 인가코드가 없음");
                return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
            }
            
            System.out.println("AuthService 호출 시작...");
            String result = authService.handleKakaoCallback(code);
            System.out.println("AuthService 결과: " + result);
            
            if (result.startsWith("Error: Authorization code is invalid or expired")) {
                System.out.println("에러: 인가코드 만료 또는 무효");
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Authorization code has already been used or expired",
                    "message", "Please try logging in again",
                    "action", "redirect_to_login"
                ));
            } else if (result.startsWith("Error:")) {
                System.out.println("에러: " + result);
                return ResponseEntity.badRequest().body(Map.of("error", result));
            } else {
                System.out.println("로그인 성공: JWT 토큰 생성 완료");
                return ResponseEntity.ok(Map.of(
                    "token", result, 
                    "message", "Kakao login successful",
                    "action", "redirect_to_dashboard"
                ));
            }
        } catch (Exception e) {
            System.out.println("예외 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/oauth/google/callback")
    public ResponseEntity<?> googleCallback(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            System.out.println("=== 구글 콜백 요청 수신 ===");
            System.out.println("받은 인가코드: " + code);
            
            if (code == null || code.isEmpty()) {
                System.out.println("에러: 인가코드가 없음");
                return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
            }
            
            System.out.println("AuthService 호출 시작...");
            String result = authService.handleGoogleCallback(code);
            System.out.println("AuthService 결과: " + result);
            
            if (result.startsWith("Error:")) {
                System.out.println("에러: " + result);
                return ResponseEntity.badRequest().body(Map.of("error", result));
            } else {
                System.out.println("로그인 성공: JWT 토큰 생성 완료");
                return ResponseEntity.ok(Map.of(
                    "token", result, 
                    "message", "Google login successful",
                    "action", "redirect_to_dashboard"
                ));
            }
        } catch (Exception e) {
            System.out.println("예외 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 기타 유틸리티 엔드포인트들
    @PostMapping("/api/auth/kakao/token")
    public ResponseEntity<?> getKakaoAccessToken(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            String accessToken = authService.getKakaoAccessToken(code);
            return ResponseEntity.ok(Map.of("access_token", accessToken));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/auth/kakao/userinfo")
    public ResponseEntity<?> getKakaoUserInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            String accessToken = authHeader.replace("Bearer ", "");
            User user = authService.getUserFromKakaoAccessToken(accessToken);
            String jwtToken = jwtService.generateToken(user.getUserId(), user.getUsername());
            return ResponseEntity.ok(Map.of(
                "token", jwtToken,
                "user", Map.of(
                    "userId", user.getUserId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "profileImage", user.getProfileImage()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

