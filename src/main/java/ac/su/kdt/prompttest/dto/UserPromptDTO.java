package ac.su.kdt.prompttest.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPromptDTO {
    private String name;
    private Integer age;
    private String gender;
    private Boolean isPregnant;
    private String healthStatus;
    private String allergy;
    private String preference;
    private String nickname;
} 