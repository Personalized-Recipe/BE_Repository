package ac.su.kdt.prompttest.dto;

import ac.su.kdt.prompttest.entity.RefrigeratorIngredient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefrigeratorIngredientDTO {
    private Integer id;
    private Integer refrigeratorId;
    private Integer ingredientId;
    private String ingredientName;
    private Float quantity;
    private String unit;
    private LocalDate expiryDate;
    private LocalDate purchaseDate;
    private String storageLocation;
    private String freshnessStatus;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static RefrigeratorIngredientDTO from(RefrigeratorIngredient refrigeratorIngredient) {
        return RefrigeratorIngredientDTO.builder()
                .id(refrigeratorIngredient.getId())
                .refrigeratorId(refrigeratorIngredient.getRefrigerator().getRefrigeratorId())
                .ingredientId(refrigeratorIngredient.getIngredient().getIngredientId())
                .ingredientName(refrigeratorIngredient.getIngredient().getName())
                .quantity(refrigeratorIngredient.getQuantity())
                .unit(refrigeratorIngredient.getUnit())
                .expiryDate(refrigeratorIngredient.getExpiryDate())
                .purchaseDate(refrigeratorIngredient.getPurchaseDate())
                .storageLocation(refrigeratorIngredient.getStorageLocation() != null ? 
                    refrigeratorIngredient.getStorageLocation().getDescription() : null)
                .freshnessStatus(refrigeratorIngredient.getFreshnessStatus() != null ? 
                    refrigeratorIngredient.getFreshnessStatus().getDescription() : null)
                .notes(refrigeratorIngredient.getNotes())
                .createdAt(refrigeratorIngredient.getCreatedAt())
                .updatedAt(refrigeratorIngredient.getUpdatedAt())
                .build();
    }
} 