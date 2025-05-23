package ac.su.kdt.prompttest.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Recipe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer recipeId; 
    
    @Column(nullable = false, length = 15)
    private String title; // 요리 이름
    
    @Column(columnDefinition = "TEXT")
    private String description; // 요리 설명
    
    @Pattern(regexp = "^(한식|중식|일식|양식|분식|퓨전)$", message = "카테고리는 '한식', '중식', '일식', '양식', '분식', '퓨전' 중 하나여야 합니다.")
    @Column(length = 50)
    private String category; // 요리 카테고리

    @Lob
    private byte[] image; // 요리 이미지
    
    private Integer cookingTime; // 조리 시간
    
    @Pattern(regexp = "^(상|중|하)$", message = "난이도는 '상', '중', '하' 중 하나여야 합니다.")
    @Column(length = 5)
    private String difficulty; // 난이도    
} 