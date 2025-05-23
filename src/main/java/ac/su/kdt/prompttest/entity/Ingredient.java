package ac.su.kdt.prompttest.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Ingredient")
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
    
    @Column(columnDefinition = "TEXT")
    private String nutritionInfo;  // 영양 정보 (단백질, 지방, 탄수화물 등)
} 