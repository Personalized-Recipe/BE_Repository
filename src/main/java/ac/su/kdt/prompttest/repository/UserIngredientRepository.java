package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.UserIngredient;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserIngredientRepository extends JpaRepository<UserIngredient, UserIngredient.UserIngredientId> {
    List<UserIngredient> findByUser(User user);
    List<UserIngredient> findByUser_UserId(Integer userId);
    Optional<UserIngredient> findByUserAndIngredient(User user, Ingredient ingredient);
} 