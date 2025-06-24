package ac.su.kdt.prompttest.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 토큰의 생성, 검증, 파싱을 담당하는 서비스 클래스
 */
@Service
public class JwtService {

    // application.yml에서 설정된 JWT 시크릿 키를 주입받음
    @Value("${jwt.secret}")
    private String secretKey;

    // application.yml에서 설정된 JWT 만료 시간을 주입받음 (밀리초 단위)
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * JWT 토큰에서 사용자 이름을 추출
     * @param token JWT 토큰
     * @return 토큰에 포함된 사용자 이름
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * JWT 토큰에서 사용자 ID를 추출
     * @param token JWT 토큰
     * @return 토큰에 포함된 사용자 ID
     */
    public Integer extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Integer.class));
    }

    /**
     * JWT 토큰에서 특정 클레임을 추출
     * @param token JWT 토큰
     * @param claimsResolver 클레임 추출 함수
     * @return 추출된 클레임 값
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 사용자 ID와 사용자 이름으로 JWT 토큰을 생성
     * @param userId 사용자 ID
     * @param username 사용자 이름
     * @return 생성된 JWT 토큰
     */
    public String generateToken(Integer userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return createToken(claims, username);
    }

    /**
     * 클레임과 주체(사용자)로 JWT 토큰을 생성
     * @param claims 토큰에 포함할 클레임
     * @param subject 토큰의 주체(사용자 이름)
     * @return 생성된 JWT 토큰
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)  // 클레임 설정
                .setSubject(subject)  // 토큰 주체 설정
                .setIssuedAt(new Date(System.currentTimeMillis()))  // 토큰 발행 시간
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))  // 토큰 만료 시간
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // 서명 알고리즘과 키 설정
                .compact();  // 토큰 생성
    }

    /**
     * JWT 토큰의 유효성을 검증
     * @param token 검증할 JWT 토큰
     * @param userDetails 사용자 정보
     * @return 토큰이 유효하면 true, 아니면 false
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * JWT 토큰의 만료 여부를 확인
     * @param token 검증할 JWT 토큰
     * @return 토큰이 만료되었으면 true, 아니면 false
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * JWT 토큰에서 만료 시간을 추출
     * @param token JWT 토큰
     * @return 토큰의 만료 시간
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * JWT 토큰의 모든 클레임을 추출
     * @param token JWT 토큰
     * @return 토큰의 모든 클레임
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())  // 서명 키 설정
                .build()
                .parseClaimsJws(token)  // 토큰 파싱
                .getBody();  // 클레임 추출
    }

    /**
     * JWT 서명에 사용할 키를 생성
     * @return 서명 키
     */
    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
//