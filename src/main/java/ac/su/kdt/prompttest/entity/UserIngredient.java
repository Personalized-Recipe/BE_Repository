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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer ingredientId;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    @Positive(message = "무게는 양수여야 합니다")
    @Column(name = "weight_in_grams")
    private Float weightInGrams;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
} 