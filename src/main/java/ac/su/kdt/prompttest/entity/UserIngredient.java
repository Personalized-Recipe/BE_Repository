package ac.su.kdt.prompttest.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "User_Ingredient",
        uniqueConstraints = @UniqueConstraint( // 🔽 변경된 부분
                columnNames = {"user_id", "ingredient_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Positive(message = "무게는 양수여야 합니다")
    @Column(name = "weight_in_grams")
    private Float weightInGrams;
}
