package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.LoginRequestDTO;
import ac.su.kdt.prompttest.dto.TokenResponseDTO;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    
    public TokenResponseDTO login(LoginRequestDTO loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));
        
        if (!Objects.equals(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        
        return TokenResponseDTO.builder()
                .token("dummy-token")  // 임시 토큰
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }
} 