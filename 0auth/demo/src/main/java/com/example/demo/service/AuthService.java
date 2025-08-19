package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.GoogleUserInfoResponse;
import com.example.demo.model.User;
import com.example.demo.model.UserProfile;
import com.example.demo.enums.Provider;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleOAuth2Service googleOAuth2Service;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse signInWithGoogle(String code) {
        // 1. Google 사용자 정보 가져오기
        String accessToken = googleOAuth2Service.getAccessToken(code);
        GoogleUserInfoResponse userInfo = googleOAuth2Service.getUserInfo(accessToken);

        // 2. 사용자 정보 DB와 동기화
        boolean isNewUser = userRepository.findByEmail(userInfo.getEmail()).isEmpty();

        User user = userRepository.findByEmail(userInfo.getEmail())
               .map(existingUser -> {
                    // 기존 유저: 이름 업데이트
                    existingUser.updateName(userInfo.getName());
                    return userRepository.save(existingUser);
                })
               .orElseGet(() -> {
                    // 신규 유저: DB에 저장
                    return userRepository.save(User.builder()
                           .email(userInfo.getEmail())
                           .name(userInfo.getName())
                           .provider(Provider.GOOGLE)
                           .providerId(userInfo.getId())
                           .role("USER")
                           .build());
                });

        // 3. 신규 유저일 경우, Google 프로필 사진으로 프로필 자동 생성
        if (isNewUser) {
            createUserProfileFromGoogle(user, userInfo);
        }

        // 4. JWT 토큰 생성 및 응답
        String accessTokenJwt = jwtService.generateToken(user.getEmail(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new AuthResponse(
                accessTokenJwt, refreshToken, "Bearer", 86400L,
                user.getEmail(), user.getName(), user.getRole()
        );
    }

    // Google 프로필 정보로 사용자 프로필 생성
    private void createUserProfileFromGoogle(User user, GoogleUserInfoResponse userInfo) {
        UserProfile profile = UserProfile.builder()
                .user(user)
                .avatarUrl(userInfo.getPicture())
                .build();
        userProfileRepository.save(profile);
    }
}
