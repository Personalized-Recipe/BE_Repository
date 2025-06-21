package ac.su.kdt.prompttest.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {
    private String authCode;
    private String provider;
} 