package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {
    Optional<Ingredient> findByName(String name);
    /**
     * 이름+등록자(userId) 기준으로 재료의 중복 등록을 막기 위한 메서드
     */
    Optional<Ingredient> findByNameAndCreatorUserId(String name, Integer creatorUserId);
} 