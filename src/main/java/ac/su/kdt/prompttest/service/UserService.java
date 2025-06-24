package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.UserDTO;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    /**
     * Spring Security의 UserDetailsService 인터페이스 구현
     * 사용자 이름으로 사용자 정보를 조회하여 UserDetails 객체로 반환
     * @param username 사용자 이름
     * @return UserDetails 객체
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }
    
    public User createUser(UserDTO userDTO) {
        User user = User.builder()
                .username(userDTO.getUsername())
                .email(userDTO.getEmail())
                .provider(userDTO.getProvider())
                .providerId(userDTO.getProviderId())
                .profileImage(userDTO.getProfileImage())
                .build();
        
        return userRepository.save(user);
    }
    
    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    @Transactional
    public User updateUser(Integer id, UserDTO userDTO) {
        User user = getUserById(id);
        // 필요한 필드만 업데이트
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail());
        }
        if (userDTO.getNickname() != null) {
            user.setNickname(userDTO.getNickname());
        }
        if (userDTO.getProfileImage() != null) {
            user.setProfileImage(userDTO.getProfileImage());
        }
        return userRepository.save(user);
    }
    
    public User getCurrentUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof User) {
                // JWT 필터에서 설정한 User 객체 반환
                return (User) principal;
            } else {
                // JWT 필터에서 User 객체가 설정되지 않은 경우
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                if (username == null || "anonymousUser".equals(username)) {
                    throw new RuntimeException("User not authenticated");
                }
                
                // JWT 토큰에서 userId를 추출하여 사용자 조회 (username 중복 문제 해결)
                try {
                    // 현재 요청에서 JWT 토큰 추출
                    String authHeader = getCurrentRequest().getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String jwt = authHeader.substring(7);
                        Integer userId = jwtService.extractUserId(jwt);
                        if (userId != null) {
                            User user = getUserById(userId);
                            log.info("JWT 토큰에서 userId로 사용자 조회 성공: userId={}, username={}", userId, user.getUsername());
                            return user;
                        }
                    }
                } catch (Exception e) {
                    log.warn("JWT 토큰에서 userId 추출 실패: {}", e.getMessage());
                }
                
                // username으로 조회는 중복 가능성이 있으므로 경고 로그와 함께 사용
                log.warn("username으로 사용자 조회 (중복 가능성 있음): {}", username);
                return userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("Current user not found"));
            }
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage());
            throw new RuntimeException("Current user not found: " + e.getMessage());
        }
    }
    
    /**
     * 현재 HTTP 요청을 가져오는 헬퍼 메서드
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception e) {
            log.warn("현재 요청을 가져올 수 없음: {}", e.getMessage());
            return null;
        }
    }
    
    @Transactional
    public User updateProfileImage(String profileImageUrl) {
        User user = getCurrentUser();
        user.setProfileImage(profileImageUrl);
        return userRepository.save(user);
    }

    /**
     * 사용자 정보로 JWT 토큰을 생성합니다.
     * @param user 토큰을 생성할 사용자 정보
     * @return 생성된 JWT 토큰
     */
    public String generateToken(User user) {
        return jwtService.generateToken(user.getUserId(), user.getUsername());
    }

    /**
     * 소셜 로그인 사용자 정보를 생성하거나 업데이트합니다.
     * @param provider 소셜 로그인 제공자
     * @param providerId 소셜 로그인 제공자의 사용자 ID
     * @param email 사용자 이메일
     * @param username 사용자 이름
     * @param profileImage 프로필 이미지 URL
     * @return 생성 또는 업데이트된 사용자 정보
     */
    @Transactional
    public User createOrUpdateSocialUser(String provider, String providerId, String email, String username, String profileImage) {
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElse(new User());

        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setEmail(email);
        user.setUsername(username);
        user.setProfileImage(profileImage);

        return userRepository.save(user);
    }
} 