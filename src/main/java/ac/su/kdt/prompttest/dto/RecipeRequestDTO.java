package ac.su.kdt.prompttest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeRequestDTO {
    private Integer userId;
    private String preferences;
    private String healthConditions;
    private String allergies;
    private String prompt;
} 