package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.ChatHistory;
import ac.su.kdt.prompttest.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String CHAT_CACHE_KEY = "chat:session:";
    private static final String USER_SESSION_KEY = "user:session:";
    private static final long CHAT_CACHE_TTL = 24 * 60 * 60; // 24시간
    
    @Transactional
    public ChatHistory processRecipeRequest(Long userId, String message) {
        // 세션 ID 가져오기
        String sessionId = getCurrentSessionId(userId);
        
        // 프롬프트 생성
        String prompt = promptService.generatePrompt(userId, message);
        
        // Perplexity API 호출
        String response = perplexityService.getResponse(userId, message);
        
        // 채팅 기록 생성
        ChatHistory chatHistory = ChatHistory.builder()
                .userId(userId)
                .message(message)
                .isUserMessage(true)
                .sessionId(sessionId)
                .build();
        
        // Redis 캐시에 사용자 메시지 저장
        String cacheKey = CHAT_CACHE_KEY + userId + ":" + sessionId;
        redisTemplate.opsForList().rightPush(cacheKey, chatHistory);
        
        // 시스템 응답 저장
        ChatHistory systemResponse = ChatHistory.builder()
                .userId(userId)
                .message(response)
                .isUserMessage(false)
                .sessionId(sessionId)
                .build();
        
        // Redis 캐시에 시스템 응답 저장
        redisTemplate.opsForList().rightPush(cacheKey, systemResponse);
        redisTemplate.expire(cacheKey, CHAT_CACHE_TTL, java.util.concurrent.TimeUnit.SECONDS);
        
        // DB에 저장
        chatHistoryRepository.save(chatHistory);
        chatHistoryRepository.save(systemResponse);
        
        return systemResponse;
    }
    
    @Transactional(readOnly = true)
    public List<ChatHistory> getUserChatHistory(Long userId, String sessionId) {
        // 먼저 Redis 캐시에서 조회
        String cacheKey = CHAT_CACHE_KEY + userId + ":" + sessionId;
        List<Object> cachedChats = redisTemplate.opsForList().range(cacheKey, 0, -1);
        
        if (cachedChats != null && !cachedChats.isEmpty()) {
            return cachedChats.stream()
                    .map(chat -> (ChatHistory) chat)
                    .toList();
        }
        
        // 캐시에 없으면 DB에서 조회
        return chatHistoryRepository.findByUserIdAndSessionIdOrderByCreatedAtDesc(userId, sessionId);
    }
    
    private String getCurrentSessionId(Long userId) {
        String cacheKey = USER_SESSION_KEY + userId;
        String sessionId = (String) redisTemplate.opsForValue().get(cacheKey);
        
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(cacheKey, sessionId);
        }
        
        return sessionId;
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