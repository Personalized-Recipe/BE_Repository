package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.UserPrompt;
import ac.su.kdt.prompttest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPromptRepository extends JpaRepository<UserPrompt, Integer> {
    UserPrompt findByUser(User user);
}