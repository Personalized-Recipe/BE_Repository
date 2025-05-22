package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.ChatHistory;
import ac.su.kdt.prompttest.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.relational.core.sql.In;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatHistoryRepository chatHistoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CHAT_CACHE_KEY = "chat:session:";
    private static final long CHAT_CACHE_TTL = 24 * 60 * 60; // 24시간
    
    @Transactional
    public void saveChat(Integer userId, String message, boolean isUserMessage, Integer recipeId) {
        // 새로운 세션 ID 생성 또는 기존 세션 ID 사용
        String sessionId = getCurrentSessionId(userId);
        
        // 채팅 내역 저장
        ChatHistory chatHistory = ChatHistory.builder()
                .userId(userId)
                .message(message)
                .isUserMessage(isUserMessage)
                .sessionId(sessionId)
                .recipeId(recipeId)
                .build();
        
        // Redis 캐시에 저장
        String cacheKey = CHAT_CACHE_KEY + userId + ":" + sessionId;
        redisTemplate.opsForList().rightPush(cacheKey, chatHistory);
        redisTemplate.expire(cacheKey, CHAT_CACHE_TTL, java.util.concurrent.TimeUnit.SECONDS);
        
        // DB에도 저장
        chatHistoryRepository.save(chatHistory);
    }
    
    public List<ChatHistory> getRecentChats(Integer userId, String sessionId) {
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
    
    private String getCurrentSessionId(Integer userId) {
        String cacheKey = "user:session:" + userId;
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