package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.ChatHistory;
import ac.su.kdt.prompttest.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {
    
    private final ChatHistoryRepository chatHistoryRepository;
    private final PerplexityService perplexityService;
    private final PromptService promptService;
    
    @Transactional
    public ChatHistory processRecipeRequest(Integer userId, String message) {
        // 세션 ID 생성
        String sessionId = UUID.randomUUID().toString();
        
        // 프롬프트 생성
        String prompt = promptService.generatePrompt(userId, message);
        
        // Perplexity API 호출
        String response = perplexityService.getResponseAsString(userId, message);
        
        // 채팅 기록 생성
        ChatHistory chatHistory = ChatHistory.builder()
                .userId(userId)
                .message(message)
                .isUserMessage(true)
                .sessionId(sessionId)
                .build();
        
        // 시스템 응답 저장
        ChatHistory systemResponse = ChatHistory.builder()
                .userId(userId)
                .message(response)
                .isUserMessage(false)
                .sessionId(sessionId)
                .build();
        
        // DB에 저장
        chatHistoryRepository.save(chatHistory);
        chatHistoryRepository.save(systemResponse);
        
        return systemResponse;
    }
    
    @Transactional(readOnly = true)
    public List<ChatHistory> getUserChatHistory(Integer userId, String sessionId) {
        return chatHistoryRepository.findByUserIdAndSessionIdOrderByCreatedAtDesc(userId, sessionId);
    }
    
    @Transactional
    public ChatHistory saveChatHistory(Integer userId, String message, String type) {
        String sessionId = UUID.randomUUID().toString();
        ChatHistory chatHistory = ChatHistory.builder()
                .userId(userId)
                .message(message)
                .isUserMessage(true)
                .sessionId(sessionId)
                .build();
        return chatHistoryRepository.save(chatHistory);
    }
    
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
    @Transactional
    public void archiveOldChats() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<ChatHistory> oldChats = chatHistoryRepository.findByCreatedAtBefore(thirtyDaysAgo);
        
        // 여기에 아카이빙 로직 구현
        // 예: S3나 다른 저장소로 이동
        
        chatHistoryRepository.deleteByCreatedAtBefore(thirtyDaysAgo);
    }
} 