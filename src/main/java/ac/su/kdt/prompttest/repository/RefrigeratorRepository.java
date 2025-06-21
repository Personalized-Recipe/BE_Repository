package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.Refrigerator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefrigeratorRepository extends JpaRepository<Refrigerator, Integer> {
    List<Refrigerator> findByUserId(Integer userId);
    Optional<Refrigerator> findByUserIdAndName(Integer userId, String name);
    boolean existsByUserIdAndName(Integer userId, String name);
} 