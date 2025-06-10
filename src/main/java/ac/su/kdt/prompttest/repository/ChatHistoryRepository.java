package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    List<ChatHistory> findByUserIdAndSessionId(Integer userId, String sessionId);
    
    List<ChatHistory> findByUserId(Integer userId);
    
    @Modifying
    @Query("DELETE FROM ChatHistory ch WHERE ch.createdAt < :date")
    void deleteByCreatedAtBefore(LocalDateTime date);

    List<ChatHistory> findByCreatedAtBefore(LocalDateTime dateTime);

    List<ChatHistory> findByUserIdAndSessionIdOrderByCreatedAtDesc(Integer userId, String sessionId);

    List<ChatHistory> findByUserIdAndChatRoom_RoomIdOrderByCreatedAtDesc(Integer userId, Integer roomId);
} 