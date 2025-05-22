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

    @GetMapping("/login/google")
    public ResponseEntity<String> googleLogin() {
        return ResponseEntity.ok("Google 로그인 페이지로 이동하세요: /oauth2/authorization/google");
    }

    @GetMapping("/login/success")
    public ResponseEntity<TokenResponseDTO> loginSuccess(@RequestParam("code") String code) {
        TokenResponseDTO response = socialLoginService.authenticateSocialLogin(code, "GOOGLE");
        return ResponseEntity.ok(response);
    }
} 