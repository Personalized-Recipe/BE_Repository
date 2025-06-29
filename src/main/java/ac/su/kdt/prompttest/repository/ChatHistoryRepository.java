package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Integer> {
    List<ChatHistory> findByUserIdAndSessionId(Integer userId, String sessionId);
    List<ChatHistory> findByCreatedAtBefore(LocalDateTime dateTime);
    void deleteByCreatedAtBefore(LocalDateTime dateTime);

    List<ChatHistory> findByUserIdAndSessionIdOrderByCreatedAtDesc(Integer userId, String sessionId);
    
    // 채팅방별 메시지 조회
    List<ChatHistory> findByChatRoomIdOrderByCreatedAtAsc(Integer chatRoomId);
    
    // 사용자의 특정 채팅방 메시지 조회
    List<ChatHistory> findByUserIdAndChatRoomIdOrderByCreatedAtAsc(Integer userId, Integer chatRoomId);
    
    // 사용자의 모든 세션 ID 조회
    @Query("SELECT DISTINCT ch.sessionId FROM ChatHistory ch WHERE ch.userId = :userId")
    List<String> findDistinctSessionIdsByUserId(@Param("userId") Integer userId);
} 