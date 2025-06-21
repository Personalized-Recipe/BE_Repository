package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.LoginRequestDTO;
import ac.su.kdt.prompttest.dto.TokenResponseDTO;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final JwtService jwtService;
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    public String handleGoogleCallback(String code) {
        // Google 인가코드로 access token 요청
        String accessToken = getGoogleAccessToken(code);
        
        // access token으로 사용자 정보 조회 및 저장
        User user = getUserFromSocial(accessToken, "google");
        
        // JWT 토큰 생성
        return jwtService.generateToken(user.getUserId(), user.getUsername());
    }

    public String handleKakaoCallback(String code) {
        try {
            String accessToken = getKakaoAccessToken(code);
            User user = getUserFromSocial(accessToken, "kakao");
            String jwtToken = jwtService.generateToken(user.getUserId(), user.getUsername());
            return jwtToken;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 400) {
                return "Error: Authorization code is invalid or expired. Please try logging in again. Error: " + e.getMessage();
            }
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public TokenResponseDTO login(LoginRequestDTO loginRequest) {
        String authCode = loginRequest.getAuthCode();
        String provider = loginRequest.getProvider();
        
        String accessToken;
        if ("google".equals(provider)) {
            accessToken = getGoogleAccessToken(authCode);
        } else if ("kakao".equals(provider)) {
            accessToken = getKakaoAccessToken(authCode);
        } else {
            throw new IllegalArgumentException("Unsupported provider: " + provider);
        }

        // 소셜 토큰으로 사용자 정보 조회
        User user = getUserFromSocial(accessToken, provider);
        
        // JWT 토큰 생성
        String token = jwtService.generateToken(user.getUserId(), user.getUsername());
        
        return TokenResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getUserId())
                .username(user.getUsername())
                .build();
    }

    private String getGoogleAccessToken(String authCode) {
        String tokenUrl = "https://oauth2.googleapis.com/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("code", authCode);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", "http://localhost:3000/oauth/callback/google");

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(tokenUrl, request, JsonNode.class);
        return response.getBody().get("access_token").asText();
    }

    public String getKakaoAccessToken(String authCode) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", "http://localhost:3000/oauth/callback/kakao");
        params.add("code", authCode);

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(tokenUrl, request, JsonNode.class);
        return response.getBody().get("access_token").asText();
    }

    public User getUserFromKakaoAccessToken(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
            userInfoUrl,
            HttpMethod.GET,
            request,
            JsonNode.class
        );
        JsonNode userInfo = response.getBody();
        
        return userRepository.findByProviderAndProviderId("kakao", userInfo.get("id").asText())
            .orElseGet(() -> {
                JsonNode kakaoAccount = userInfo.get("kakao_account");
                String nickname = "kakao_user_" + userInfo.get("id").asText();
                
                if (kakaoAccount != null && kakaoAccount.get("profile") != null) {
                    JsonNode profile = kakaoAccount.get("profile");
                    if (profile.get("nickname") != null) {
                        nickname = profile.get("nickname").asText();
                    }
                }
                
                String profileImage = null;
                if (kakaoAccount != null && kakaoAccount.get("profile") != null) {
                    JsonNode profile = kakaoAccount.get("profile");
                    if (profile.get("profile_image_url") != null) {
                        profileImage = profile.get("profile_image_url").asText();
                    }
                }
                
                User newUser = User.builder()
                    .username(nickname)
                    .email("kakao_" + userInfo.get("id").asText() + "@kakao.com")
                    .password("OAUTH2_USER_NO_PASSWORD")
                    .provider("kakao")
                    .providerId(userInfo.get("id").asText())
                    .profileImage(profileImage)
                    .build();
                return userRepository.save(newUser);
            });
    }

    private User getUserFromSocial(String accessToken, String provider) {
        String userInfoUrl;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> request = new HttpEntity<>(headers);

        if ("google".equals(provider)) {
            userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                request,
                JsonNode.class
            );
            JsonNode userInfo = response.getBody();
            
            String providerId = userInfo.get("id").asText();
            System.out.println("=== 구글 사용자 정보 조회 ===");
            System.out.println("Provider ID: " + providerId);
            System.out.println("Provider: " + provider);
            
            // 기존 사용자 조회 또는 새 사용자 생성
            return userRepository.findByProviderAndProviderId(provider, providerId)
                .map(existingUser -> {
                    System.out.println("기존 사용자 발견: " + existingUser.getUsername());
                    return existingUser;
                })
                .orElseGet(() -> {
                    System.out.println("새 사용자 생성 시작...");
                    User newUser = User.builder()
                        .username(userInfo.get("email").asText())
                        .email(userInfo.get("email").asText())
                        .password("OAUTH2_USER_NO_PASSWORD")
                        .provider(provider)
                        .providerId(providerId)
                        .profileImage(userInfo.get("picture").asText())
                        .build();
                    
                    User savedUser = userRepository.save(newUser);
                    System.out.println("새 사용자 생성 완료: " + savedUser.getUsername());
                    return savedUser;
                });
        } else if ("kakao".equals(provider)) {
            userInfoUrl = "https://kapi.kakao.com/v2/user/me";
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                request,
                JsonNode.class
            );
            JsonNode userInfo = response.getBody();
            
            String providerId = userInfo.get("id").asText();
            System.out.println("=== 카카오 사용자 정보 조회 ===");
            System.out.println("Provider ID: " + providerId);
            System.out.println("Provider: " + provider);
            
            // 기존 사용자 조회 또는 새 사용자 생성
            return userRepository.findByProviderAndProviderId(provider, providerId)
                .map(existingUser -> {
                    System.out.println("기존 사용자 발견: " + existingUser.getUsername());
                    return existingUser;
                })
                .orElseGet(() -> {
                    System.out.println("새 사용자 생성 시작...");
                    JsonNode kakaoAccount = userInfo.get("kakao_account");
                    String nickname = "kakao_user_" + providerId;
                    
                    if (kakaoAccount != null && kakaoAccount.get("profile") != null) {
                        JsonNode profile = kakaoAccount.get("profile");
                        if (profile.get("nickname") != null) {
                            nickname = profile.get("nickname").asText();
                        }
                    }
                    
                    String profileImage = null;
                    if (kakaoAccount != null && kakaoAccount.get("profile") != null) {
                        JsonNode profile = kakaoAccount.get("profile");
                        if (profile.get("profile_image_url") != null) {
                            profileImage = profile.get("profile_image_url").asText();
                        }
                    }
                    
                    User newUser = User.builder()
                        .username(nickname)
                        .email("kakao_" + providerId + "@kakao.com")
                        .password("OAUTH2_USER_NO_PASSWORD")
                        .provider(provider)
                        .providerId(providerId)
                        .profileImage(profileImage)
                        .build();
                    
                    User savedUser = userRepository.save(newUser);
                    System.out.println("새 사용자 생성 완료: " + savedUser.getUsername());
                    return savedUser;
                });
        }
        throw new IllegalArgumentException("Unsupported provider: " + provider);
    }
}
