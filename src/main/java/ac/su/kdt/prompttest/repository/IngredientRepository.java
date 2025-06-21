package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {
    Optional<Ingredient> findByName(String name);
    List<Ingredient> findByNameContaining(String name);
} 