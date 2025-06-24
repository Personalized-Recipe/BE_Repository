package ac.su.kdt.prompttest.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "Ingredient", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "creator_user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer ingredientId;
    
    @Column(nullable = false, length = 50)
    private String name;  // 재료 이름
    
    @Column(name = "required_amount")
    private Float requiredAmount;  // g 또는 ml단위
    
    @Column
    private Integer calories;  // 100g 당 칼로리

    @OneToMany(mappedBy = "ingredient")
    private List<UserIngredient> userIngredients;
    
    @Column(columnDefinition = "TEXT")
    private String nutritionInfo;  // 영양 정보 (단백질, 지방, 탄수화물 등)

    /**
     * 이 재료를 등록한 사용자의 userId
     * 이름+등록자 조합이 unique하도록 DB와 JPA에 제약을 둡니다.
     */
    @Column(name = "creator_user_id", nullable = false)
    private Integer creatorUserId;
} 