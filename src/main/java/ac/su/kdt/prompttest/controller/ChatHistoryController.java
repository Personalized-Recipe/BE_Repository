package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.ChatRequestDTO;
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
    
    @PostMapping("/recipes")
    public ResponseEntity<ChatHistory> requestRecipe(@RequestBody ChatRequestDTO request) {
        ChatHistory chatHistory = chatHistoryService.processRecipeRequest(
                request.getUserId(),
                request.getMessage()
        );
        return ResponseEntity.ok(chatHistory);
    }
    
    @GetMapping("/user/{userId}/session/{sessionId}")
    public ResponseEntity<List<ChatHistory>> getChatHistory(
            @PathVariable Integer userId,
            @PathVariable String sessionId) {
        List<ChatHistory> history = chatHistoryService.getUserChatHistory(userId, sessionId);
        return ResponseEntity.ok(history);
    }
} 