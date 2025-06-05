package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.entity.ChatRoom;
import ac.su.kdt.prompttest.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {
    
    private final ChatRoomService chatRoomService;
    
    @PostMapping
    public ResponseEntity<ChatRoom> createChatRoom(@RequestBody String roomName) {
        ChatRoom chatRoom = chatRoomService.createChatRoom(roomName);
        return ResponseEntity.ok(chatRoom);
    }
    
    @GetMapping
    public ResponseEntity<List<ChatRoom>> getAllChatRooms() {
        List<ChatRoom> chatRooms = chatRoomService.getAllChatRooms();
        return ResponseEntity.ok(chatRooms);
    }
    
    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoom> getChatRoomById(@PathVariable Integer roomId) {
        ChatRoom chatRoom = chatRoomService.getChatRoomById(roomId);
        return ResponseEntity.ok(chatRoom);
    }
} 