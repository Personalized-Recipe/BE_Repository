package ac.su.kdt.prompttest.config;

import ac.su.kdt.prompttest.security.JwtAuthenticationFilter;
import ac.su.kdt.prompttest.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 클래스
 * 보안 관련 설정과 JWT 인증 필터를 구성
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationFilter jwtAuthFilter;

    // 1. AuthenticationProvider 빈 생성 (필드 주입 제거)
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    // 2. SecurityFilterChain에서 AuthenticationProvider 직접 사용
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트 (인증 불필요)
                        .requestMatchers("/api/oauth/**", "/api/auth/**", "/api/v1/**", "/api/test/**").permitAll()
                        
                        // 레시피 관련 엔드포인트 (개발 중 임시 공개)
                        .requestMatchers("/api/recipes/**").permitAll() // 개발 중 임시 허용
                        
                        // 사용자 관련 엔드포인트 (인증 필요)
                        .requestMatchers("/api/users/me").permitAll() // 임시로 허용 (개발 중)
                        .requestMatchers("/api/users/{userId}/profile").authenticated()
                        .requestMatchers("/api/users/{userId}").authenticated()
                        .requestMatchers("/api/users/me/profile-image").authenticated()
                        
                        // 프롬프트 관련 엔드포인트 (인증 필요)
                        .requestMatchers("/api/users/{userId}/prompt").authenticated()
                        .requestMatchers("/api/users/{userId}/fields/{field}").authenticated()
                        
                        // 재료 관련 엔드포인트 (인증 필요)
                        .requestMatchers("/api/ingredients/user/{userId}").authenticated() // 재료 추가
                        .requestMatchers("/api/ingredients/{userId}").authenticated() // 재료 조회
                        .requestMatchers("/api/ingredients/{userId}/{ingredientName}").authenticated() // 재료 수정/삭제
                        .requestMatchers("/api/ingredients/all").authenticated() // 모든 재료 조회
                        .requestMatchers("/api/ingredients/search").authenticated() // 재료 검색
                        
                        // 기타 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
