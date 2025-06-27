package ac.su.kdt.prompttest.dto;

import ac.su.kdt.prompttest.entity.Recipe;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponseDTO {
    private String type;
    private List<Recipe> recipes;  // 메뉴 추천용
    private Recipe recipe;         // 특정 레시피용
    
    // 메뉴 추천 응답 생성
    public static RecipeResponseDTO createMenuRecommendation(List<Recipe> recipes) {
        return RecipeResponseDTO.builder()
                .type("recipe-list")
                .recipes(recipes)
                .build();
    }
    
    // 특정 레시피 응답 생성
    public static RecipeResponseDTO createRecipeDetail(Recipe recipe) {
        return RecipeResponseDTO.builder()
                .type("recipe-detail")
                .recipe(recipe)
                .build();
    }
} 