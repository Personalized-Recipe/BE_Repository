package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.TokenResponseDTO;
import ac.su.kdt.prompttest.service.SocialLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SocialLoginController {

    private final SocialLoginService socialLoginService;

    @PostMapping("/kakao")
    public ResponseEntity<TokenResponseDTO> kakaoLogin(@RequestBody String accessToken) {
        TokenResponseDTO response = socialLoginService.authenticateSocialLogin(accessToken, "KAKAO");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<TokenResponseDTO> googleLogin(@RequestBody String accessToken) {
        TokenResponseDTO response = socialLoginService.authenticateSocialLogin(accessToken, "GOOGLE");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/naver")
    public ResponseEntity<TokenResponseDTO> naverLogin(@RequestBody String accessToken) {
        TokenResponseDTO response = socialLoginService.authenticateSocialLogin(accessToken, "NAVER");
        return ResponseEntity.ok(response);
    }
}