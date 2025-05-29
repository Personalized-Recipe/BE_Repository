package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Integer> {
    List<ChatHistory> findByUserIdAndSessionId(Integer userId, String sessionId);
    List<ChatHistory> findByCreatedAtBefore(LocalDateTime dateTime);
    void deleteByCreatedAtBefore(LocalDateTime dateTime);

    List<ChatHistory> findByUserIdAndSessionIdOrderByCreatedAtDesc(Integer userId, String sessionId);
} 