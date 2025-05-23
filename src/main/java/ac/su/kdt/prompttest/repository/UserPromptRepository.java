package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.UserPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPromptRepository extends JpaRepository<UserPrompt, Integer> {
    UserPrompt findByUserId(Integer userId);
} 