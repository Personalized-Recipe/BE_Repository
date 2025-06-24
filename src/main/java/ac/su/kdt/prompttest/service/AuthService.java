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
import org.springframework.http.MediaType;

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

    public TokenResponseDTO handleGoogleCallback(String code) {
        // Google 인가코드로 access token 요청
        String accessToken = getGoogleAccessToken(code);
        
        // access token으로 사용자 정보 조회 및 저장
        User user = getUserFromSocial(accessToken, "google");
        
        // JWT 토큰 생성
        String token = jwtService.generateToken(user.getUserId(), user.getUsername());
        
        return TokenResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .provider(user.getProvider())
                .build();
    }

    public TokenResponseDTO handleKakaoCallback(String code) {
        try {
            System.out.println("=== handleKakaoCallback 시작 ===");
            System.out.println("인가코드: " + code);
            
            String accessToken = getKakaoAccessToken(code);
            System.out.println("카카오 액세스 토큰 획득 완료");
            
            User user = getUserFromSocial(accessToken, "kakao");
            System.out.println("카카오 사용자 정보 조회 완료: " + user.getUsername());
            System.out.println("사용자 프로필 이미지: " + user.getProfileImage());
            System.out.println("사용자 닉네임: " + user.getNickname());
            
            String jwtToken = jwtService.generateToken(user.getUserId(), user.getUsername());
            System.out.println("JWT 토큰 생성 완료");
            
            TokenResponseDTO result = TokenResponseDTO.builder()
                    .token(jwtToken)
                    .type("Bearer")
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .profileImage(user.getProfileImage())
                    .provider(user.getProvider())
                    .build();
            
            System.out.println("TokenResponseDTO 생성 완료:");
            System.out.println("- userId: " + result.getUserId());
            System.out.println("- username: " + result.getUsername());
            System.out.println("- profileImage: " + result.getProfileImage());
            System.out.println("- provider: " + result.getProvider());
            
            System.out.println("=== handleKakaoCallback 완료 ===");
            return result;
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException 발생: " + e.getMessage());
            if (e.getStatusCode().value() == 400) {
                throw new RuntimeException("Error: Authorization code is invalid or expired. Please try logging in again. Error: " + e.getMessage());
            }
            throw new RuntimeException("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Exception 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error: " + e.getMessage());
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
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .provider(user.getProvider())
                .build();
    }

    private String getGoogleAccessToken(String authCode) {
        String tokenUrl = "https://oauth2.googleapis.com/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("code", authCode);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", "http://localhost:5173/oauth/callback/google");

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
        params.add("redirect_uri", "http://localhost:5173/oauth/callback/kakao");
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
        System.out.println("=== getUserFromSocial 시작 ===");
        System.out.println("Provider: " + provider);
        System.out.println("Access Token: " + accessToken.substring(0, Math.min(20, accessToken.length())) + "...");
        
        String userInfoUrl;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> request = new HttpEntity<>(headers);

        if ("google".equals(provider)) {
            System.out.println("구글 로그인 경로로 이동");
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
                    
                    // 구글에서 최신 정보 가져오기
                    String nickname = existingUser.getUsername();
                    String profileImage = existingUser.getProfileImage();
                    boolean hasChanges = false;
                    
                    // 구글은 name과 picture 필드를 사용
                    if (userInfo.get("name") != null) {
                        nickname = userInfo.get("name").asText();
                        if (!nickname.equals(existingUser.getUsername())) {
                            System.out.println("구글 이름 업데이트: " + existingUser.getUsername() + " -> " + nickname);
                            existingUser.setUsername(nickname);
                            hasChanges = true;
                        }
                    }
                    
                    if (userInfo.get("picture") != null) {
                        profileImage = userInfo.get("picture").asText();
                        // null 안전 비교
                        if (existingUser.getProfileImage() == null || !profileImage.equals(existingUser.getProfileImage())) {
                            System.out.println("구글 프로필 이미지 업데이트: " + existingUser.getProfileImage() + " -> " + profileImage);
                            existingUser.setProfileImage(profileImage);
                            hasChanges = true;
                        }
                    }
                    
                    // 변경사항이 있으면 저장
                    if (hasChanges) {
                        return userRepository.save(existingUser);
                    }
                    
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
            System.out.println("카카오 로그인 경로로 이동");
            // 1. 먼저 사용자 정보 가져오기 (연결 상태 확인)
            userInfoUrl = "https://kapi.kakao.com/v2/user/me";
            System.out.println("=== 카카오 API 호출 시작 ===");
            System.out.println("API URL: " + userInfoUrl);
            System.out.println("Access Token: " + accessToken.substring(0, Math.min(20, accessToken.length())) + "...");
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                request,
                JsonNode.class
            );
            System.out.println("카카오 API 응답 상태: " + response.getStatusCode());
            JsonNode userInfo = response.getBody();
            System.out.println("카카오 API 응답 본문: " + userInfo.toString());
            
            // 모든 필드를 개별적으로 확인
            System.out.println("=== 카카오 API 응답 필드별 확인 ===");
            System.out.println("id: " + userInfo.get("id"));
            System.out.println("connected_at: " + userInfo.get("connected_at"));
            System.out.println("has_signed_up: " + userInfo.get("has_signed_up"));
            System.out.println("synched_at: " + userInfo.get("synched_at"));
            System.out.println("properties: " + userInfo.get("properties"));
            System.out.println("kakao_account: " + userInfo.get("kakao_account"));
            System.out.println("profile: " + userInfo.get("profile"));
            
            // properties 필드가 있다면 그 내용도 확인
            if (userInfo.get("properties") != null) {
                JsonNode properties = userInfo.get("properties");
                System.out.println("=== properties 필드 상세 ===");
                System.out.println("properties.nickname: " + properties.get("nickname"));
                System.out.println("properties.profile_image: " + properties.get("profile_image"));
                System.out.println("properties.thumbnail_image: " + properties.get("thumbnail_image"));
            }
            
            // kakao_account 필드가 있다면 그 내용도 확인
            if (userInfo.get("kakao_account") != null) {
                JsonNode kakaoAccount = userInfo.get("kakao_account");
                System.out.println("=== kakao_account 필드 상세 ===");
                System.out.println("kakao_account.profile: " + kakaoAccount.get("profile"));
                if (kakaoAccount.get("profile") != null) {
                    JsonNode profile = kakaoAccount.get("profile");
                    System.out.println("kakao_account.profile.nickname: " + profile.get("nickname"));
                    System.out.println("kakao_account.profile.profile_image_url: " + profile.get("profile_image_url"));
                }
            }
            
            String providerId = userInfo.get("id").asText();
            System.out.println("=== 카카오 사용자 정보 조회 ===");
            System.out.println("Provider ID: " + providerId);
            System.out.println("Provider: " + provider);
            System.out.println("전체 사용자 정보: " + userInfo.toString());
            System.out.println("사용자 정보 키들: " + userInfo.fieldNames());
            
            // 연결 상태 확인
            boolean isConnected = userInfo.get("has_signed_up") != null && userInfo.get("has_signed_up").asBoolean();
            System.out.println("사용자 연결 상태: " + (isConnected ? "연결됨" : "연결 대기"));
            
            // 추가 필드 확인
            System.out.println("=== 추가 필드 확인 ===");
            System.out.println("has_signed_up: " + userInfo.get("has_signed_up"));
            System.out.println("connected_at: " + userInfo.get("connected_at"));
            System.out.println("synched_at: " + userInfo.get("synched_at"));
            System.out.println("properties: " + userInfo.get("properties"));
            System.out.println("kakao_account: " + userInfo.get("kakao_account"));
            System.out.println("profile: " + userInfo.get("profile"));
            
            // 연결되지 않은 경우 연결하기 API 호출
            if (!isConnected) {
                System.out.println("사용자를 앱과 연결합니다...");
                try {
                    String signupUrl = "https://kapi.kakao.com/v1/user/signup";
                    HttpHeaders signupHeaders = new HttpHeaders();
                    signupHeaders.setBearerAuth(accessToken);
                    signupHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                    
                    HttpEntity<String> signupRequest = new HttpEntity<>(signupHeaders);
                    ResponseEntity<JsonNode> signupResponse = restTemplate.exchange(
                        signupUrl,
                        HttpMethod.POST,
                        signupRequest,
                        JsonNode.class
                    );
                    System.out.println("연결하기 API 응답: " + signupResponse.getBody().toString());
                    
                    // 연결 후 다시 사용자 정보 가져오기
                    response = restTemplate.exchange(
                        userInfoUrl,
                        HttpMethod.GET,
                        request,
                        JsonNode.class
                    );
                    userInfo = response.getBody();
                    System.out.println("연결 후 전체 사용자 정보: " + userInfo.toString());
                } catch (HttpClientErrorException e) {
                    System.out.println("연결하기 API 에러: " + e.getMessage());
                    if (e.getStatusCode().value() == 400) {
                        JsonNode errorBody = e.getResponseBodyAs(JsonNode.class);
                        if (errorBody != null && errorBody.get("code") != null && errorBody.get("code").asInt() == -102) {
                            System.out.println("이미 등록된 사용자입니다. 기존 정보로 진행합니다.");
                            // 이미 등록된 사용자이므로 기존 userInfo로 계속 진행
                        } else {
                            System.out.println("연결하기 API에서 다른 에러 발생: " + errorBody);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("연결하기 API 호출 중 예외 발생: " + e.getMessage());
                    // 에러가 발생해도 기존 userInfo로 계속 진행
                }
            }
            
            // 최종 사용자 정보를 final 변수로 저장
            final JsonNode finalUserInfo = userInfo;
            
            // 각 필드별로 확인
            System.out.println("kakao_account 필드 존재: " + (finalUserInfo.get("kakao_account") != null));
            System.out.println("profile 필드 존재: " + (finalUserInfo.get("profile") != null));
            System.out.println("properties 필드 존재: " + (finalUserInfo.get("properties") != null));
            
            // properties 필드 내용 확인
            if (finalUserInfo.get("properties") != null) {
                System.out.println("properties 내용: " + finalUserInfo.get("properties").toString());
            }
            
            // kakao_account가 없어도 profile 정보가 최상위에 있을 수 있음
            final JsonNode profile;
            if (finalUserInfo.get("kakao_account") != null) {
                profile = finalUserInfo.get("kakao_account").get("profile");
                System.out.println("kakao_account.profile 존재: " + (profile != null));
            } else if (finalUserInfo.get("profile") != null) {
                // 최상위 레벨에서 profile 정보 확인
                profile = finalUserInfo.get("profile");
                System.out.println("최상위 profile 존재: " + (profile != null));
            } else {
                profile = null;
                System.out.println("profile 정보를 찾을 수 없음");
            }
            
            // 기존 사용자 조회 또는 새 사용자 생성
            return userRepository.findByProviderAndProviderId(provider, providerId)
                .map(existingUser -> {
                    System.out.println("기존 사용자 발견: " + existingUser.getUsername());
                    System.out.println("기존 프로필 이미지: " + existingUser.getProfileImage());
                    
                    // 카카오에서 최신 정보 가져오기
                    String nickname = existingUser.getUsername();
                    boolean hasChanges = false;
                    
                    System.out.println("카카오 계정 정보 존재: " + (profile != null));
                    if (profile != null) {
                        System.out.println("프로필 정보: " + profile.toString());
                        
                        // 닉네임 업데이트
                        if (profile.get("nickname") != null) {
                            String newNickname = profile.get("nickname").asText();
                            System.out.println("카카오에서 받은 닉네임: " + newNickname);
                            
                            // username 중복 확인
                            if (!newNickname.equals(existingUser.getUsername())) {
                                // 다른 사용자가 같은 username을 사용하고 있는지 확인
                                boolean usernameExists = userRepository.findByUsername(newNickname)
                                    .map(otherUser -> !otherUser.getUserId().equals(existingUser.getUserId()))
                                    .orElse(false);
                                
                                if (usernameExists) {
                                    // 중복되는 경우 provider_id를 추가하여 고유하게 만듦
                                    newNickname = newNickname + "_" + existingUser.getProviderId();
                                    System.out.println("username 중복으로 인해 수정: " + newNickname);
                                }
                                
                                System.out.println("닉네임 업데이트: " + existingUser.getUsername() + " -> " + newNickname);
                                existingUser.setUsername(newNickname);
                                hasChanges = true;
                            }
                        }
                        
                        // 프로필 이미지 업데이트 - 강화된 로직
                        if (profile.get("profile_image_url") != null) {
                            String newProfileImage = profile.get("profile_image_url").asText();
                            System.out.println("카카오에서 받은 프로필 이미지: " + newProfileImage);
                            System.out.println("기존 프로필 이미지: " + existingUser.getProfileImage());
                            
                            // 카카오에서 받은 프로필 이미지가 있으면 항상 업데이트
                            boolean shouldUpdate = false;
                            
                            if (existingUser.getProfileImage() == null) {
                                System.out.println("기존 프로필 이미지가 null이므로 업데이트");
                                shouldUpdate = true;
                            } else if (!newProfileImage.equals(existingUser.getProfileImage())) {
                                System.out.println("프로필 이미지가 다르므로 업데이트");
                                shouldUpdate = true;
                            } else {
                                System.out.println("프로필 이미지가 동일하지만 최신 정보로 업데이트");
                                shouldUpdate = true; // 항상 최신 정보로 업데이트
                            }
                            
                            if (shouldUpdate) {
                                System.out.println("프로필 이미지 업데이트: " + existingUser.getProfileImage() + " -> " + newProfileImage);
                                existingUser.setProfileImage(newProfileImage);
                                hasChanges = true;
                            }
                        } else {
                            System.out.println("카카오에서 프로필 이미지 URL을 받지 못함");
                        }
                    } else {
                        System.out.println("카카오 계정에 프로필 정보가 없음");
                        
                        // properties 필드에서도 프로필 이미지 확인
                        if (finalUserInfo.get("properties") != null) {
                            JsonNode properties = finalUserInfo.get("properties");
                            System.out.println("properties에서 프로필 정보 확인: " + properties.toString());
                            
                            if (properties.get("profile_image") != null) {
                                String newProfileImage = properties.get("profile_image").asText();
                                System.out.println("properties에서 프로필 이미지 발견: " + newProfileImage);
                                
                                if (existingUser.getProfileImage() == null || !newProfileImage.equals(existingUser.getProfileImage())) {
                                    System.out.println("properties에서 프로필 이미지 업데이트: " + existingUser.getProfileImage() + " -> " + newProfileImage);
                                    existingUser.setProfileImage(newProfileImage);
                                    hasChanges = true;
                                }
                            }
                            
                            // properties에서 닉네임도 확인
                            if (properties.get("nickname") != null) {
                                String newNickname = properties.get("nickname").asText();
                                System.out.println("properties에서 닉네임 발견: " + newNickname);
                                
                                if (!newNickname.equals(existingUser.getUsername())) {
                                    System.out.println("properties에서 닉네임 업데이트: " + existingUser.getUsername() + " -> " + newNickname);
                                    existingUser.setUsername(newNickname);
                                    hasChanges = true;
                                }
                            }
                        }
                        
                        // 최상위 레벨에서도 프로필 정보 확인
                        if (finalUserInfo.get("profile") != null) {
                            JsonNode topLevelProfile = finalUserInfo.get("profile");
                            System.out.println("최상위 profile에서 프로필 정보 확인: " + topLevelProfile.toString());
                            
                            if (topLevelProfile.get("profile_image_url") != null) {
                                String newProfileImage = topLevelProfile.get("profile_image_url").asText();
                                System.out.println("최상위 profile에서 프로필 이미지 발견: " + newProfileImage);
                                
                                if (existingUser.getProfileImage() == null || !newProfileImage.equals(existingUser.getProfileImage())) {
                                    System.out.println("최상위 profile에서 프로필 이미지 업데이트: " + existingUser.getProfileImage() + " -> " + newProfileImage);
                                    existingUser.setProfileImage(newProfileImage);
                                    hasChanges = true;
                                }
                            }
                            
                            if (topLevelProfile.get("nickname") != null) {
                                String newNickname = topLevelProfile.get("nickname").asText();
                                System.out.println("최상위 profile에서 닉네임 발견: " + newNickname);
                                
                                if (!newNickname.equals(existingUser.getUsername())) {
                                    System.out.println("최상위 profile에서 닉네임 업데이트: " + existingUser.getUsername() + " -> " + newNickname);
                                    existingUser.setUsername(newNickname);
                                    hasChanges = true;
                                }
                            }
                        }
                    }
                    
                    // 변경사항이 있으면 저장
                    if (hasChanges) {
                        System.out.println("변경사항이 있어서 DB에 저장합니다.");
                        User savedUser = userRepository.save(existingUser);
                        System.out.println("저장 후 프로필 이미지: " + savedUser.getProfileImage());
                        return savedUser;
                    } else {
                        System.out.println("변경사항이 없어서 기존 정보를 사용합니다.");
                    }
                    
                    return existingUser;
                })
                .orElseGet(() -> {
                    System.out.println("새 사용자 생성 시작...");
                    String nickname = "kakao_user_" + providerId;
                    String profileImage = null;
                    
                    if (profile != null) {
                        System.out.println("프로필 정보: " + profile.toString());
                        
                        if (profile.get("nickname") != null) {
                            nickname = profile.get("nickname").asText();
                            System.out.println("닉네임 추출: " + nickname);
                        }
                        
                        if (profile.get("profile_image_url") != null) {
                            profileImage = profile.get("profile_image_url").asText();
                            System.out.println("프로필 이미지 URL 추출: " + profileImage);
                        } else {
                            System.out.println("프로필 이미지 URL이 없습니다.");
                        }
                    } else {
                        System.out.println("프로필 정보가 없습니다.");
                    }
                    
                    User newUser = User.builder()
                        .username(nickname)  // 카카오 닉네임을 username으로 사용
                        .email("kakao_" + providerId + "@kakao.com")
                        .password("OAUTH2_USER_NO_PASSWORD")
                        .provider(provider)
                        .providerId(providerId)
                        .profileImage(profileImage)
                        .build();
                    
                    User savedUser = userRepository.save(newUser);
                    System.out.println("새 사용자 생성 완료: " + savedUser.getUsername());
                    System.out.println("저장된 프로필 이미지: " + savedUser.getProfileImage());
                    return savedUser;
                });
        }
        throw new IllegalArgumentException("Unsupported provider: " + provider);
    }
}
