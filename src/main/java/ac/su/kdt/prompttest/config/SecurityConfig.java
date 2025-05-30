package ac.su.kdt.prompttest.config;

import ac.su.kdt.prompttest.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 클래스
 * 보안 관련 설정과 JWT 인증 필터를 구성
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // JWT 인증 필터
    private final JwtAuthenticationFilter jwtAuthFilter;
    // 인증 제공자
    private final AuthenticationProvider authenticationProvider;

    /**
     * Spring Security 필터 체인을 구성
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 보호 비활성화 (JWT를 사용하므로 세션 기반 CSRF 보호가 필요 없음)
            .csrf(csrf -> csrf.disable())
            // URL별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // 인증 관련 엔드포인트는 모두 허용
                .requestMatchers("/api/public/**").permitAll() // 공개 API는 모두 허용
                .anyRequest().authenticated()  // 나머지 모든 요청은 인증 필요
            )
            // 세션 관리 설정
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // 세션 사용하지 않음 (JWT 사용)
            )
            // 인증 제공자 설정
            .authenticationProvider(authenticationProvider)
            // JWT 인증 필터 추가
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
} 