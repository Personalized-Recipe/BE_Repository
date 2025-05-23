package ac.su.kdt.prompttest.service;

//import ac.su.kdt.prompttest.dto.LoginRequestDTO;
//import ac.su.kdt.prompttest.dto.TokenResponseDTO;
//import ac.su.kdt.prompttest.service.JwtService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;

/*
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final JwtService jwtService;
    
    public TokenResponseDTO login(LoginRequestDTO loginRequest) {
        // 하드코딩된 인증 로직
        if ("testuser".equals(loginRequest.getUsername()) && 
            "password123".equals(loginRequest.getPassword())) {
            
            String token = jwtService.generateToken(1, "testuser");
            
            return TokenResponseDTO.builder()
                    .token(token)
                    .type("Bearer")
                    .userId(1)
                    .username("testuser")
                    .build();
        }
        
        throw new RuntimeException("Invalid credentials");
    }
}
*/ 