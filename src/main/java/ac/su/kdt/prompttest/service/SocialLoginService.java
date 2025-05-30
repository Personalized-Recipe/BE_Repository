package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.TokenResponseDTO;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.enums.SocialProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLoginService {
    private final RestTemplate restTemplate;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    /**
     * 소셜 로그인 인증을 처리합니다.
     * @param token 소셜 로그인 토큰
     * @param provider 소셜 로그인 제공자
     * @return JWT 토큰 응답
     */
    public TokenResponseDTO authenticateSocialLogin(String token, String provider) {
        SocialProvider socialProvider = SocialProvider.fromString(provider);
        String userInfo = validateAndGetUserInfo(token, socialProvider);
        
        // 소셜 로그인 사용자 정보 파싱 및 저장
        User user = parseAndSaveUserInfo(userInfo, provider);
        
        // JWT 토큰 생성 - User 객체를 직접 전달
        String jwtToken = userService.generateToken(user);
        
        return TokenResponseDTO.builder()
                .token(jwtToken)
                .type("Bearer")
                .userId(user.getUserId())
                .username(user.getUsername())
                .build();
    }

    /**
     * 소셜 로그인 토큰을 검증하고 사용자 정보를 가져옵니다.
     * @param token 소셜 로그인 토큰
     * @param provider 소셜 로그인 제공자
     * @return 사용자 정보 JSON 문자열
     */
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

    /**
     * 소셜 로그인 응답을 파싱하여 사용자 정보를 저장합니다.
     * @param userInfo 소셜 로그인 응답 JSON
     * @param provider 소셜 로그인 제공자
     * @return 저장된 사용자 정보
     */

     // userInfo를 파싱하여 사용자 정보를 User 객체로 저장 
    private User parseAndSaveUserInfo(String userInfo, String provider) {
        try {
            JsonNode jsonNode = objectMapper.readTree(userInfo);
            String providerId;
            String email;
            String username;
            String profileImage;  // 프로필 이미지 URL을 저장할 변수 추가

            switch (SocialProvider.fromString(provider)) {
                case GOOGLE:
                    providerId = jsonNode.get("sub").asText();
                    email = jsonNode.get("email").asText();
                    username = jsonNode.get("name").asText();
                    profileImage = jsonNode.get("picture").asText();  // 구글은 "picture" 필드에 프로필 이미지 URL이 있음
                    break;
                    
                case KAKAO:
                    JsonNode kakaoAccount = jsonNode.get("kakao_account");
                    providerId = jsonNode.get("id").asText();
                    email = kakaoAccount.get("email").asText();
                    username = kakaoAccount.get("profile").get("nickname").asText();
                    profileImage = kakaoAccount.get("profile").get("profile_image_url").asText();  // 카카오는 profile_image_url 필드에 있음
                    break;
                    
                case NAVER:
                    JsonNode naverResponse = jsonNode.get("response");
                    providerId = naverResponse.get("id").asText();
                    email = naverResponse.get("email").asText();
                    username = naverResponse.get("name").asText();
                    profileImage = naverResponse.get("profile_image").asText();  // 네이버는 profile_image 필드에 있음
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unsupported social provider: " + provider);
            }

            // User 객체 생성/업데이트 시 프로필 이미지도 포함
            return userService.createOrUpdateSocialUser(provider, providerId, email, username, profileImage);
            
        } catch (Exception e) {
            log.error("Error parsing social login response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse social login response", e);
        }
    }

    /**
     * 소셜 로그인 제공자별 사용자 정보 엔드포인트를 반환합니다.
     * @param provider 소셜 로그인 제공자
     * @return 사용자 정보 엔드포인트 URL
     */
    private String getProviderEndpoint(SocialProvider provider) {
        return switch (provider) {
            case GOOGLE -> "https://www.googleapis.com/oauth2/v3/userinfo";
            case KAKAO -> "https://kapi.kakao.com/v2/user/me";
            case NAVER -> "https://openapi.naver.com/v1/nid/me";
        };
    }
} 