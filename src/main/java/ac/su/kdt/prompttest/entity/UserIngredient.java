package ac.su.kdt.prompttest.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "User_Ingredient")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIngredient {
    @EmbeddedId
    private UserIngredientId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("ingredientId")
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "ingredient_name", length = 100)
    private String ingredientName;

    @Positive(message = "수량은 양수여야 합니다")
    @Column(name = "amount")
    private Float amount;

    @Column(name = "unit", length = 20)
    private String unit;

    // 편의 메서드
    public Integer getUserId() {
        return id != null ? id.getUserId() : null;
    }

    public Integer getIngredientId() {
        return id != null ? id.getIngredientId() : null;
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserIngredientId implements java.io.Serializable {
        @Column(name = "user_id")
        private Integer userId;

        @Column(name = "ingredient_id")
        private Integer ingredientId;
    }
}
