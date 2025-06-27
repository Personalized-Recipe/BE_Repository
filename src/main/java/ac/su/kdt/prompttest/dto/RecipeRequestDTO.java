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
    private String chatRoomId;
    private String request;
    private Boolean useRefrigerator;
    private Boolean isSpecificRecipe;
} 