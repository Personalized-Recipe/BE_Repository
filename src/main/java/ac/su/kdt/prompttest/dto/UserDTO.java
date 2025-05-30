package ac.su.kdt.prompttest.dto;

import ac.su.kdt.prompttest.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserDTO {
    private Integer id;
    private String username;
    private String password;
    private String name;
    private Integer age;
    private String gender;
    private String preferences;
    private String healthConditions;
    private String allergies;
    
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
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .age(user.getAge())
                .gender(user.getGender())
                .preferences(user.getPreferences())
                .healthConditions(user.getHealthConditions())
                .allergies(user.getAllergies())
                .build();
    }
} 