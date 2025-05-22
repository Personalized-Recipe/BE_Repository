package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.TokenResponseDTO;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.enums.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Service
@RequiredArgsConstructor
public class SocialLoginService {
    private final RestTemplate restTemplate;
    private final UserService userService;

    public TokenResponseDTO authenticateSocialLogin(String token, String provider) {
        SocialProvider socialProvider = SocialProvider.fromString(provider);
        String userInfo = validateAndGetUserInfo(token, socialProvider);
        
        // 소셜 로그인 사용자 정보 파싱 및 저장
        User user = parseAndSaveUserInfo(userInfo, provider);
        
        // JWT 토큰 생성
        String jwtToken = userService.generateToken(userInfo);
        
        return TokenResponseDTO.builder()
                .token(jwtToken)
                .type("Bearer")
                .build();
    }

    private String validateAndGetUserInfo(String token, SocialProvider provider) {
        String userInfoEndpoint = getProviderEndpoint(provider);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            userInfoEndpoint,
            HttpMethod.GET,
            entity,
            String.class
        );
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to validate social token");
        }
        
        return response.getBody();
    }

    private User parseAndSaveUserInfo(String userInfo, String provider) {
        // TODO: 각 제공자별 응답 파싱 로직 구현
        // 임시로 더미 데이터 사용
        return userService.createOrUpdateSocialUser(
            provider,
            "dummy-provider-id",
            "dummy@email.com",
            "dummy-username"
        );
    }

    private String getProviderEndpoint(SocialProvider provider) {
        return switch (provider) {
            case GOOGLE -> "https://www.googleapis.com/oauth2/v3/userinfo";
            case KAKAO -> "https://kapi.kakao.com/v2/user/me";
            case NAVER -> "https://openapi.naver.com/v1/nid/me";
        };
    }
} 