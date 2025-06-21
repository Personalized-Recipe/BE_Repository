package ac.su.kdt.prompttest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeRecommendationRequestDTO {
    private Integer refrigeratorId;
    private List<String> additionalIngredients; // 추가로 필요한 재료들
    private String cuisineType; // 요리 종류 (한식, 중식, 양식, 일식 등)
    private String difficulty; // 난이도 (초급, 중급, 고급)
    private Integer servingSize; // 인분 수
    private String dietaryRestrictions; // 식이 제한 (채식, 알레르기 등)
    private String cookingTime; // 조리 시간 (15분, 30분, 1시간 등)
    private String preference; // 선호도 (매운맛, 달콤한맛, 건강식 등)
} 