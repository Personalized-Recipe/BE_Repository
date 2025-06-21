package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.UserPrompt;
import ac.su.kdt.prompttest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPromptRepository extends JpaRepository<UserPrompt, Integer> {
    UserPrompt findByUser(User user);
    List<UserPrompt> findByUserId(Integer userId);
}