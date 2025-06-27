package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.RecipeResponseDTO;
import ac.su.kdt.prompttest.entity.ChatHistory;
import ac.su.kdt.prompttest.entity.Recipe;
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
        
        // Perplexity API 호출 (메뉴 추천으로 처리)
        RecipeResponseDTO recipeResponse = perplexityService.getResponse(userId, message, false, false);
        
        // 응답을 문자열로 변환
        String response;
        if (recipeResponse.getType().equals("recipe-list")) {
            // 메뉴 추천인 경우
            response = "메뉴 추천:\n";
            for (Recipe recipe : recipeResponse.getRecipes()) {
                response += "- " + recipe.getTitle() + "\n";
            }
        } else {
            // 특정 레시피인 경우
            Recipe recipe = recipeResponse.getRecipe();
            response = recipe.getTitle() + "\n" + recipe.getDescription();
        }
        
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