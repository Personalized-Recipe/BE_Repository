package ac.su.kdt.prompttest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefrigeratorIngredientRequestDTO {
    private Integer ingredientId;
    private Float quantity;
    private String unit;
    private LocalDate expiryDate;
    private LocalDate purchaseDate;
    private String storageLocation;
    private String freshness;
    private String notes;
} 