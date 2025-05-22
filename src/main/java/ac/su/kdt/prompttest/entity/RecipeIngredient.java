package ac.su.kdt.prompttest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "Recipe_Ingredient")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(RecipeIngredient.RecipeIngredientId.class)
public class RecipeIngredient {
    @Id
    @Column(name = "recipe_id")
    private Integer recipeId;

    @Id
    @Column(name = "ingredient_id")
    private Integer ingredientId;
    
    // 복합 키 클래스
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeIngredientId implements Serializable {
        private Integer recipeId;
        private Integer ingredientId;
    }
} 