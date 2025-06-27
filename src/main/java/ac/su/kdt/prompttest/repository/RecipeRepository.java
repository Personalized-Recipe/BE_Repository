package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.Recipe;
import ac.su.kdt.prompttest.entity.UserRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Integer> {
    // 기본 레시피 조회 메서드들
    
    // 제목으로 레시피 찾기
    Recipe findByTitle(String title);
} 