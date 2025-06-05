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
    public ChatRoom createChatRoom(String roomName) {
        ChatRoom chatRoom = ChatRoom.builder()
                .roomName(roomName)
                .build();
        return chatRoomRepository.save(chatRoom);
    }
    
    @Transactional(readOnly = true)
    public List<ChatRoom> getAllChatRooms() {
        return chatRoomRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public ChatRoom getChatRoomById(Integer roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found with id: " + roomId));
    }
} 