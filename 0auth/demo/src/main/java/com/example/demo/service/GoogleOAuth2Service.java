package com.example.demo.service;    

import com.example.demo.dto.GoogleTokenResponse;
import com.example.demo.dto.GoogleUserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2Service {

    // application.properties에 설정된 값들을 주입받음
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    private final WebClient webClient; // 비동기 HTTP 통신을 위한 클라이언트

    // 1. 인증 코드로 Google에 액세스 토큰 요청
    public String getAccessToken(String code) {
        String tokenUri = "<https://oauth2.googleapis.com/token>";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        GoogleTokenResponse response = webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(params)
                .retrieve() // 응답을 받아옴
                .bodyToMono(GoogleTokenResponse.class) // 응답 본문을 GoogleTokenResponse 객체로 변환
                .block(); // 비동기 작업이 끝날 때까지 대기

        if (response == null) {
            throw new RuntimeException("Failed to get access token from Google");
        }
        return response.getAccessToken();
    }

    // 2. 액세스 토큰으로 Google에 사용자 정보 요청
    public GoogleUserInfoResponse getUserInfo(String accessToken) {
        String userInfoUri = "<https://www.googleapis.com/oauth2/v2/userinfo>";

        GoogleUserInfoResponse response = webClient.get()
                .uri(userInfoUri)
                .headers(headers -> headers.setBearerAuth(accessToken)) // 헤더에 Bearer 토큰 추가
                .retrieve()
                .bodyToMono(GoogleUserInfoResponse.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Failed to get user info from Google");
        }
        return response;
    }
}
