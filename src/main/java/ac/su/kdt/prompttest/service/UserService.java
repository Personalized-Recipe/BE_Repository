package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.UserDTO;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
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
        if (userDTO.getProfileImage() != null) {
            user.setProfileImage(userDTO.getProfileImage());
        }
        return userRepository.save(user);
    }
    
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }
    
    @Transactional
    public User updateProfileImage(String profileImageUrl) {
        User user = getCurrentUser();
        user.setProfileImage(profileImageUrl);
        return userRepository.save(user);
    }

    public String generateToken(String userInfo) {
        // TODO: Implement JWT token generation
        // 임시로 하드코딩된 토큰 반환
        return "dummy-jwt-token";
    }

    @Transactional
    public User createOrUpdateSocialUser(String provider, String providerId, String email, String username) {
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> User.builder()
                        .provider(provider)
                        .providerId(providerId)
                        .username(username)
                        .email(email)
                        .build());
        
        return userRepository.save(user);
    }
} 