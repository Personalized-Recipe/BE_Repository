package ac.su.kdt.prompttest.dto;

import ac.su.kdt.prompttest.entity.Refrigerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefrigeratorDTO {
    private Integer refrigeratorId;
    private Integer userId;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RefrigeratorIngredientDTO> ingredients;
    
    public static RefrigeratorDTO from(Refrigerator refrigerator) {
        return RefrigeratorDTO.builder()
                .refrigeratorId(refrigerator.getRefrigeratorId())
                .userId(refrigerator.getUser().getId())
                .name(refrigerator.getName())
                .description(refrigerator.getDescription())
                .createdAt(refrigerator.getCreatedAt())
                .updatedAt(refrigerator.getUpdatedAt())
                .ingredients(refrigerator.getRefrigeratorIngredients() != null ?
                    refrigerator.getRefrigeratorIngredients().stream()
                        .map(RefrigeratorIngredientDTO::from)
                        .collect(Collectors.toList()) : null)
                .build();
    }
} 