package ac.su.kdt.prompttest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String getSecretKey() {
        return secretKey;
    }

    public long getJwtExpiration() {
        return jwtExpiration;
    }
} 