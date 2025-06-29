package ac.su.kdt.prompttest.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserIngredientDTO {
    private Integer userId;
    private Integer ingredientId;
    private String ingredientName;
    private Float amount;
    private String unit;
    
    // 프론트엔드 요청 구조에 맞는 필드들
    private String name;        // 재료명 (ingredientName과 동일)
    
    // userId는 URL 경로에서 받으므로 DTO에서 제외
} 