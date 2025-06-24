package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.UserIngredient;
import ac.su.kdt.prompttest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserIngredientRepository extends JpaRepository<UserIngredient, Integer> {
    List<UserIngredient> findByUser(User user);
    List<UserIngredient> findByUser_UserId(Integer userId);
} 