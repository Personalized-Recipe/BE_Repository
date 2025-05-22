package ac.su.kdt.prompttest.dto;

import ac.su.kdt.prompttest.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Integer userId;
    private String username;
    private String email;
    private String provider;
    private String providerId;
    private String profileImage;
    
    /**
     * User 엔티티를 UserDTO로 변환하는 정적 메서드입니다.
     * 이 메서드는 User 엔티티의 데이터를 받아서 UserDTO 객체로 매핑합니다.
     * 비밀번호는 보안상의 이유로 DTO에 포함되지 않습니다.
     *
     * @param user 변환할 User 엔티티
     * @return 변환된 UserDTO 객체
     */
    public static UserDTO from(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .profileImage(user.getProfileImage())
                .build();
    }
} 