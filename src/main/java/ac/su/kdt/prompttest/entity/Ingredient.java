package ac.su.kdt.prompttest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String name;
    
    private Float requiredAmount;
    
    private Integer calories;
    
    @Column(columnDefinition = "TEXT")
    private String nutritionInfo;
} 