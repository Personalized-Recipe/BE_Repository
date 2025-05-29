package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.UserRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRecipeRepository extends JpaRepository<UserRecipe, UserRecipe.UserRecipeId> {
    List<UserRecipe> findByUserId(Integer userId);
}
