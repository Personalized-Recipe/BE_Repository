package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.ChatHistory;
import ac.su.kdt.prompttest.entity.ChatRoom;
import ac.su.kdt.prompttest.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import ac.su.kdt.prompttest.dto.ChatHistoryDTO;
import ac.su.kdt.prompttest.entity.Recipe;
import ac.su.kdt.prompttest.repository.RecipeRepository;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {
    
    private final ChatHistoryRepository chatHistoryRepository;
    private final PerplexityService perplexityService;
    private final PromptService promptService;
    private final ChatRoomService chatRoomService;
    private final RecipeRepository recipeRepository;
    
    @Transactional
    public ChatHistory processRecipeRequest(Integer userId, String message, Integer roomId) {
        // 세션 ID 생성
        String sessionId = UUID.randomUUID().toString();
        
        // 프롬프트 생성
        String prompt = promptService.generatePrompt(userId, message);
        
        // Perplexity API 호출하여 이미 저장된 Recipe 객체 반환
        Recipe recipe = perplexityService.getResponse(userId, message);
        
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomService.getChatRoomById(roomId);
        
        // 채팅 기록 생성
        ChatHistory chatHistory = ChatHistory.builder()
                .userId(userId)
                .message(message)
                .isUserMessage(true)
                .sessionId(sessionId)
                .chatRoom(chatRoom)
                .build();
        
        // 시스템 응답 저장
        ChatHistory systemResponse = ChatHistory.builder()
                .userId(userId)
                .message(perplexityService.formatRecipeAsString(recipe))
                .isUserMessage(false)
                .sessionId(sessionId)
                .chatRoom(chatRoom)
                .recipeId(recipe.getRecipeId())
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
    public ChatHistory saveChatHistory(Integer userId, String message, String type, Integer roomId) {
        String sessionId = UUID.randomUUID().toString();
        ChatRoom chatRoom = chatRoomService.getChatRoomById(roomId);
        ChatHistory chatHistory = ChatHistory.builder()
                .userId(userId)
                .message(message)
                .isUserMessage(true)
                .sessionId(sessionId)
                .chatRoom(chatRoom)
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

    @Transactional(readOnly = true)
    public List<ChatHistory> getAllUserChatHistory(Integer userId) {
        return chatHistoryRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ChatHistory> getRoomChatHistory(Integer userId, Integer roomId) {
        return chatHistoryRepository.findByUserIdAndChatRoom_RoomIdOrderByCreatedAtDesc(userId, roomId);
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryDTO> getRoomChatHistoryDTO(Integer userId, Integer roomId) {
        List<ChatHistory> historyList = chatHistoryRepository.findByUserIdAndChatRoom_RoomIdOrderByCreatedAtDesc(userId, roomId);
        return historyList.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryDTO> getAllUserChatHistoryDTO(Integer userId) {
        List<ChatHistory> historyList = chatHistoryRepository.findByUserId(userId);
        return historyList.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryDTO> getUserChatHistoryDTO(Integer userId, String sessionId) {
        List<ChatHistory> historyList = chatHistoryRepository.findByUserIdAndSessionIdOrderByCreatedAtDesc(userId, sessionId);
        return historyList.stream().map(this::toDTO).toList();
    }

    @Transactional
    public ChatHistoryDTO processRecipeRequestDTO(Integer userId, String message, Integer roomId) {
        ChatHistory systemResponse = processRecipeRequest(userId, message, roomId);
        return toDTO(systemResponse);
    }

    private ChatHistoryDTO toDTO(ChatHistory entity) {
        return ChatHistoryDTO.builder()
                .chatId(entity.getChatId())
                .userId(entity.getUserId())
                .message(entity.getMessage())
                .isUserMessage(entity.isUserMessage())
                .sessionId(entity.getSessionId())
                .recipeId(entity.getRecipeId())
                .createdAt(entity.getCreatedAt())
                .roomId(entity.getChatRoom() != null ? entity.getChatRoom().getRoomId() : null)
                .roomName(entity.getChatRoom() != null ? entity.getChatRoom().getRoomName() : null)
                .build();
    }
} 