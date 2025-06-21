package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.ChatRequestDTO;
import ac.su.kdt.prompttest.dto.ChatHistoryDTO;
import ac.su.kdt.prompttest.entity.ChatHistory;
import ac.su.kdt.prompttest.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatHistoryController {
    
    private final ChatHistoryService chatHistoryService;
    
    @PostMapping("/history")
    public ResponseEntity<ChatHistoryDTO> sendMessage(@RequestBody ChatRequestDTO chatRequest) {
        ChatHistoryDTO response = chatHistoryService.processRecipeRequestDTO(
                chatRequest.getUserId(),
                chatRequest.getMessage(),
                chatRequest.getRoomId()
        );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/user/{userId}/room/{roomId}")
    public ResponseEntity<ChatHistoryDTO> startChat(
            @PathVariable Integer userId,
            @PathVariable Integer roomId,
            @RequestBody ChatRequestDTO chatRequest) {
        ChatHistoryDTO response = chatHistoryService.processRecipeRequestDTO(userId, chatRequest.getMessage(), roomId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/user/{userId}/session/new/room/{roomId}")
    public ResponseEntity<ChatHistoryDTO> startNewChat(
            @PathVariable Integer userId,
            @PathVariable Integer roomId,
            @RequestBody ChatRequestDTO chatRequest) {
        ChatHistoryDTO response = chatHistoryService.processRecipeRequestDTO(userId, chatRequest.getMessage(), roomId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}/session/{sessionId}")
    public ResponseEntity<List<ChatHistoryDTO>> getChatHistory(
            @PathVariable Integer userId,
            @PathVariable String sessionId) {
        List<ChatHistoryDTO> history = chatHistoryService.getUserChatHistoryDTO(userId, sessionId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<ChatHistoryDTO>> getAllUserChatHistory(
            @PathVariable Integer userId) {
        List<ChatHistoryDTO> history = chatHistoryService.getAllUserChatHistoryDTO(userId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/user/{userId}/room/{roomId}/history")
    public ResponseEntity<List<ChatHistoryDTO>> getRoomChatHistory(
            @PathVariable Integer userId,
            @PathVariable Integer roomId) {
        List<ChatHistoryDTO> history = chatHistoryService.getRoomChatHistoryDTO(userId, roomId);
        return ResponseEntity.ok(history);
    }
} 