package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Integer> {
    Optional<PromptTemplate> findByIsActive(Boolean isActive);
} 