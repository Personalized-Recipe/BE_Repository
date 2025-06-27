package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Integer> {
    
    // 사용자의 모든 채팅방 조회
    List<ChatRoom> findByUserIdOrderByUpdatedAtDesc(Integer userId);
    
    // 활성화된 채팅방만 조회
    List<ChatRoom> findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(Integer userId);
    
    // 특정 사용자의 채팅방 개수 조회
    long countByUserId(Integer userId);
} 