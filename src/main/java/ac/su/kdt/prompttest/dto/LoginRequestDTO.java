package ac.su.kdt.prompttest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDTO {
    private String socialToken;    // 소셜 로그인 토큰
    private String provider;       // 소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER 등)
} 