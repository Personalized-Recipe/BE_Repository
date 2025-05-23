package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, RecipeIngredient.RecipeIngredientId> {
    List<RecipeIngredient> findByRecipeId(Integer recipeId);
    void deleteByRecipeId(Integer recipeId);
} 