package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.ChatHistory;
import ac.su.kdt.prompttest.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ChatHistoryRepository chatHistoryRepository;
    
    @Transactional
    public ChatHistory saveChat(Integer userId, String message, boolean isUserMessage, Integer recipeId, Integer chatRoomId) {
        try {
            // 채팅방 ID가 있는 경우 해당 채팅방의 세션 ID 사용, 없으면 새로운 세션 생성
            String sessionId = getSessionIdForChatRoom(userId, chatRoomId);
            
            // 채팅 내역 저장
            ChatHistory chatHistory = ChatHistory.builder()
                    .userId(userId)
                    .message(message)
                    .isUserMessage(isUserMessage)
                    .sessionId(sessionId)
                    .recipeId(recipeId)
                    .chatRoomId(chatRoomId)
                    .build();
            
            // DB에 저장
            ChatHistory savedChat = chatHistoryRepository.save(chatHistory);
            
            log.info("채팅 메시지 저장 성공: userId={}, chatRoomId={}, sessionId={}, isUserMessage={}", 
                    userId, chatRoomId, sessionId, isUserMessage);
            return savedChat;
            
        } catch (Exception e) {
            log.error("채팅 메시지 저장 실패: userId={}, chatRoomId={}, message={}", userId, chatRoomId, message, e);
            throw new RuntimeException("채팅 메시지 저장에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 특정 세션의 최근 채팅 내역 조회
     */
    public List<ChatHistory> getRecentChats(Integer userId, String sessionId) {
        return chatHistoryRepository.findByUserIdAndSessionIdOrderByCreatedAtDesc(userId, sessionId);
    }
    
    /**
     * 특정 채팅방의 세션 ID 조회 (없으면 생성)
     */
    public String getSessionIdForChatRoom(Integer userId, Integer chatRoomId) {
        if (chatRoomId != null) {
            // 채팅방이 있는 경우 해당 채팅방의 기존 세션 ID 조회
            List<ChatHistory> existingChats = chatHistoryRepository.findByUserIdAndChatRoomIdOrderByCreatedAtAsc(userId, chatRoomId);
            
            if (!existingChats.isEmpty()) {
                // 기존 채팅이 있으면 첫 번째 메시지의 세션 ID 사용
                return existingChats.get(0).getSessionId();
            } else {
                // 새로운 채팅방이면 새로운 세션 ID 생성
                return UUID.randomUUID().toString();
            }
        } else {
            // 채팅방이 없는 경우 새로운 세션 ID 생성
            return UUID.randomUUID().toString();
        }
    }
    
    /**
     * 사용자의 모든 세션 ID 조회
     */
    public List<String> getUserSessionIds(Integer userId) {
        return chatHistoryRepository.findDistinctSessionIdsByUserId(userId);
    }
    
    /**
     * 특정 세션의 대화 컨텍스트 조회 (AI 모델용)
     */
    public String getConversationContext(Integer userId, String sessionId, int maxMessages) {
        List<ChatHistory> recentChats = chatHistoryRepository
                .findByUserIdAndSessionIdOrderByCreatedAtDesc(userId, sessionId);
        
        if (recentChats.isEmpty()) {
            return "";
        }
        
        // 최근 메시지들만 선택 (AI 컨텍스트 제한 고려)
        List<ChatHistory> contextMessages = recentChats.stream()
                .limit(maxMessages)
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt())) // 시간순 정렬
                .toList();
        
        StringBuilder context = new StringBuilder();
        for (ChatHistory chat : contextMessages) {
            String role = chat.isUserMessage() ? "사용자" : "AI";
            context.append(role).append(": ").append(chat.getMessage()).append("\n");
        }
        
        return context.toString();
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