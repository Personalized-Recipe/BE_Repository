package ac.su.kdt.prompttest.security;

import ac.su.kdt.prompttest.service.JwtService;
import ac.su.kdt.prompttest.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 토큰을 검증하고 인증을 처리하는 필터
 * 모든 HTTP 요청에 대해 JWT 토큰의 유효성을 검사하고, 유효한 경우 사용자를 인증
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // JWT 토큰 처리를 위한 서비스
    private final JwtService jwtService;
    // 사용자 정보를 조회하기 위한 서비스
    private final UserService userService;

    /**
     * HTTP 요청을 가로채서 JWT 토큰을 검증하고 인증을 처리
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // Authorization 헤더에서 JWT 토큰 추출
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Authorization 헤더가 없거나 Bearer로 시작하지 않으면 다음 필터로 넘어감
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bearer 토큰 추출 (Bearer 접두어 제거)
        jwt = authHeader.substring(7);
        
        try {
            // JWT 토큰에서 username 추출
            username = jwtService.extractUsername(jwt);
            
            // username이 있고 현재 인증된 사용자가 없는 경우에만 인증 처리
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 사용자 정보 조회
                UserDetails userDetails = userService.loadUserByUsername(username);
                
                // 토큰이 유효한 경우 인증 처리
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // 인증 토큰 생성
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    // 요청 상세 정보 설정
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // SecurityContext에 인증 정보 저장
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // 토큰 검증 실패 시 로그 기록
            logger.error("JWT 토큰 검증 실패: " + e.getMessage());
        }
        
        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
} 