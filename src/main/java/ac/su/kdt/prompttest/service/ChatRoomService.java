package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.ChatRoom;
import ac.su.kdt.prompttest.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    
    private final ChatRoomRepository chatRoomRepository;
    
    @Transactional
    public ChatRoom createChatRoom(Integer userId, String title) {
        ChatRoom chatRoom = ChatRoom.builder()
                .userId(userId)
                .title(title)
                .isActive(true)
                .messageCount(0)
                .build();
        
        return chatRoomRepository.save(chatRoom);
    }
    
    public List<ChatRoom> getUserChatRooms(Integer userId) {
        return chatRoomRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
    }
    
    public ChatRoom getChatRoom(Integer chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
    }
    
    @Transactional
    public ChatRoom updateChatRoom(Integer chatRoomId, String title) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        chatRoom.setTitle(title);
        return chatRoomRepository.save(chatRoom);
    }
    
    @Transactional
    public void deactivateChatRoom(Integer chatRoomId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        chatRoom.setIsActive(false);
        chatRoomRepository.save(chatRoom);
    }
    
    @Transactional
    public void incrementMessageCount(Integer chatRoomId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        chatRoom.setMessageCount(chatRoom.getMessageCount() + 1);
        chatRoomRepository.save(chatRoom);
    }
} 