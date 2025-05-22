package ac.su.kdt.prompttest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "User_Recipe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserRecipe.UserRecipeId.class)
public class UserRecipe {
    @Id
    @Column(name = "user_id")
    private Integer userId;
    
    @Id
    @Column(name = "recipe_id")
    private Integer recipeId;
    
    // 복합 키 클래스
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRecipeId implements Serializable {
        private Integer userId;
        private Integer recipeId;
    }
} 