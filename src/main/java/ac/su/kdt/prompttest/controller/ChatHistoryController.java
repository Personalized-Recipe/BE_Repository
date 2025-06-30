package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.entity.ChatHistory;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.entity.Recipe;
import ac.su.kdt.prompttest.dto.RecipeResponseDTO;
import ac.su.kdt.prompttest.service.ChatHistoryService;
import ac.su.kdt.prompttest.service.ChatService;
import ac.su.kdt.prompttest.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryController {
    
    private final ChatHistoryService chatHistoryService;
    private final ChatService chatService;
    private final RecipeService recipeService;
    
    @GetMapping("/user/{userId}/session/{sessionId}")
    public ResponseEntity<List<ChatHistory>> getChatHistory(
            @PathVariable Integer userId,
            @PathVariable String sessionId) {
        List<ChatHistory> history = chatHistoryService.getUserChatHistory(userId, sessionId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * 특정 채팅방의 메시지 조회
     * @param chatRoomId 채팅방 ID
     * @return 채팅방의 메시지 목록
     */
    @GetMapping("/rooms/{chatRoomId}/messages")
    public ResponseEntity<List<Map<String, Object>>> getChatRoomMessages(@PathVariable Integer chatRoomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            List<ChatHistory> messages = chatHistoryService.getChatRoomMessages(user.getUserId(), chatRoomId);
            
            // ChatHistory를 프론트엔드용 형식으로 변환
            List<Map<String, Object>> responseMessages = messages.stream()
                .map(this::parseChatMessage)
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(responseMessages);
        } catch (Exception e) {
            log.error("채팅방 메시지 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * ChatHistory를 프론트엔드용 형식으로 파싱
     * @param chatHistory 원본 채팅 히스토리
     * @return 파싱된 메시지 맵
     */
    private Map<String, Object> parseChatMessage(ChatHistory chatHistory) {
        Map<String, Object> message = new HashMap<>();
        
        // 기본 필드 설정
        message.put("chatId", chatHistory.getChatId());
        message.put("userId", chatHistory.getUserId());
        message.put("message", chatHistory.getMessage());
        message.put("isUserMessage", chatHistory.isUserMessage());
        message.put("sessionId", chatHistory.getSessionId());
        message.put("chatRoomId", chatHistory.getChatRoomId());
        message.put("recipeId", chatHistory.getRecipeId());
        message.put("createdAt", chatHistory.getCreatedAt());
        
        // 사용자 메시지는 단순 텍스트로 처리
        if (chatHistory.isUserMessage()) {
            message.put("type", "text");
            message.put("role", "user");
            message.put("content", chatHistory.getMessage());
            return message;
        }
        
        // AI 메시지 타입 판별 및 파싱
        String content = chatHistory.getMessage();
        Map<String, Object> parsedMessage = parseAIMessage(content, chatHistory.getRecipeId());
        
        message.putAll(parsedMessage);
        message.put("role", "bot");
        
        return message;
    }
    
    /**
     * AI 메시지 타입 판별 및 파싱
     * @param content 메시지 내용
     * @param recipeId 레시피 ID (있는 경우)
     * @return 파싱된 메시지 맵
     */
    private Map<String, Object> parseAIMessage(String content, Integer recipeId) {
        Map<String, Object> message = new HashMap<>();
        
        log.info("=== AI 메시지 파싱 시작 ===");
        log.info("원본 메시지: {}", content);
        log.info("레시피 ID: {}", recipeId);
        
        // 레시피 ID가 있는 경우 상세 레시피로 처리
        if (recipeId != null) {
            log.info("레시피 ID {}로 상세 레시피 조회 시도", recipeId);
            try {
                Recipe recipe = recipeService.getRecipeById(recipeId);
                if (recipe != null) {
                    log.info("레시피 조회 성공: {}", recipe.getTitle());
                    message.put("type", "recipe-detail");
                    Map<String, Object> recipeMap = new HashMap<>();
                    recipeMap.put("recipeId", recipe.getRecipeId());
                    recipeMap.put("title", recipe.getTitle());
                    recipeMap.put("description", recipe.getDescription());
                    recipeMap.put("category", recipe.getCategory());
                    recipeMap.put("imageUrl", recipe.getImageUrl());
                    recipeMap.put("cookingTime", recipe.getCookingTime());
                    recipeMap.put("difficulty", recipe.getDifficulty());
                    message.put("recipe", recipeMap);
                    log.info("레시피 상세 정보 파싱 완료: {}", recipeMap);
                    return message;
                } else {
                    log.warn("레시피 ID {}로 조회했지만 결과가 null입니다", recipeId);
                }
            } catch (Exception e) {
                log.error("레시피 ID {} 조회 실패: {}", recipeId, e.getMessage(), e);
            }
        }
        
        // 메뉴 추천 메시지인지 판별 (강화된 버전)
        log.info("메뉴 추천 메시지 판별 시도...");
        if (isMenuRecommendation(content)) {
            log.info("메뉴 추천 메시지로 판별됨");
            message.put("type", "recipe-list");
            List<Map<String, Object>> recipes = recipeService.parseMenuRecommendation(content);
            message.put("recipes", recipes);
            log.info("메뉴 추천 파싱 완료. 총 {}개의 레시피 추출", recipes.size());
            return message;
        } else {
            log.info("메뉴 추천 메시지가 아님으로 판별됨");
        }
        
        // 일반 텍스트로 처리
        log.info("일반 텍스트 메시지로 처리됨");
        message.put("type", "text");
        message.put("content", content);
        return message;
    }
    
    /**
     * 메뉴 추천 메시지인지 판별 (강화된 버전)
     * @param content 메시지 내용
     * @return 메뉴 추천 여부
     */
    private boolean isMenuRecommendation(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String lowerContent = content.toLowerCase();
        
        // 상세 레시피 키워드가 포함된 경우 메뉴 추천이 아님
        String[] detailedRecipeKeywords = {
            "요리 이름:", "조리 시간:", "난이도:", "조리 방법:", "필요한 재료와 양:",
            "재료:", "양념:", "소스:", "조리 과정:", "만드는 방법:",
            "1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.", "10.",
            "불을 끄고", "완성합니다", "그릇에 담아", "졸아들면", "익으면",
            "볶으면", "끓으면", "굽으면", "튀기면", "찌면", "삶으면",
            "데치면", "무치면", "섞으면", "향을 내", "마무리", "마지막에"
        };
        
        for (String keyword : detailedRecipeKeywords) {
            if (lowerContent.contains(keyword)) {
                log.info("상세 레시피 키워드 '{}' 발견으로 메뉴 추천이 아님으로 판별", keyword);
                return false;
            }
        }
        
        // 메뉴 추천 키워드 확인
        boolean hasRecommendationKeywords = lowerContent.contains("메뉴 추천") || 
                                          lowerContent.contains("추천 메뉴") ||
                                          lowerContent.contains("추천:") ||
                                          lowerContent.contains("다음과 같은") ||
                                          lowerContent.contains("추천드립니다") ||
                                          lowerContent.contains("추천해드립니다");
        
        // 메뉴 목록 패턴 확인 (줄바꿈으로 구분된 항목들)
        String[] lines = content.split("\n");
        int menuItemCount = 0;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;
            
            // 메뉴 항목 패턴 확인 (더 엄격하게)
            if (trimmedLine.startsWith("- ") || 
                trimmedLine.startsWith("• ") ||
                (trimmedLine.matches("^\\d+\\..*") && !trimmedLine.contains(":"))) {
                menuItemCount++;
            }
        }
        
        // 메뉴 추천 키워드가 있거나, 메뉴 항목이 1개 이상이면 메뉴 추천으로 판별
        boolean isMenuList = hasRecommendationKeywords || menuItemCount >= 1;
        
        log.info("메뉴 추천 판별 결과: hasRecommendationKeywords={}, menuItemCount={}, isMenuList={}", 
                hasRecommendationKeywords, menuItemCount, isMenuList);
        
        return isMenuList;
    }
    
    /**
     * 사용자의 모든 세션 ID 조회
     * @param userId 사용자 ID
     * @return 세션 ID 목록
     */
    @GetMapping("/user/{userId}/sessions")
    public ResponseEntity<List<String>> getUserSessions(@PathVariable Integer userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            // 본인의 세션만 조회 가능
            if (!user.getUserId().equals(userId)) {
                return ResponseEntity.status(403).build();
            }
            
            List<String> sessionIds = chatService.getUserSessionIds(userId);
            return ResponseEntity.ok(sessionIds);
        } catch (Exception e) {
            log.error("사용자 세션 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 특정 세션의 대화 컨텍스트 조회 (AI 모델용)
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     * @param maxMessages 최대 메시지 수 (기본값: 10)
     * @return 대화 컨텍스트 문자열
     */
    @GetMapping("/user/{userId}/session/{sessionId}/context")
    public ResponseEntity<Map<String, String>> getConversationContext(
            @PathVariable Integer userId,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "10") int maxMessages) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            // 본인의 컨텍스트만 조회 가능
            if (!user.getUserId().equals(userId)) {
                return ResponseEntity.status(403).build();
            }
            
            String context = chatService.getConversationContext(userId, sessionId, maxMessages);
            return ResponseEntity.ok(Map.of("context", context));
        } catch (Exception e) {
            log.error("대화 컨텍스트 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 채팅방의 세션 ID 조회
     * @param chatRoomId 채팅방 ID
     * @return 세션 ID
     */
    @GetMapping("/rooms/{chatRoomId}/session")
    public ResponseEntity<Map<String, String>> getChatRoomSession(@PathVariable Integer chatRoomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            String sessionId = chatService.getSessionIdForChatRoom(user.getUserId(), chatRoomId);
            return ResponseEntity.ok(Map.of("sessionId", sessionId));
        } catch (Exception e) {
            log.error("채팅방 세션 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 채팅 메시지 전송 및 AI 응답 처리
     * @param chatRoomId 채팅방 ID
     * @param request 채팅 요청 데이터
     * @return AI 응답
     */
    @PostMapping("/{chatRoomId}")
    public ResponseEntity<Map<String, Object>> sendChatMessage(
            @PathVariable Integer chatRoomId,
            @RequestBody Map<String, Object> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            String message = (String) request.get("message");
            Boolean useRefrigerator = (Boolean) request.get("useRefrigerator");
            Boolean isSpecificRecipe = (Boolean) request.get("isSpecificRecipe");
            
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // RecipeService를 통해 메시지 처리
            RecipeResponseDTO recipeResponse = recipeService.requestRecipe(
                user.getUserId(), 
                chatRoomId.toString(), 
                message, 
                useRefrigerator != null ? useRefrigerator : false,
                isSpecificRecipe != null ? isSpecificRecipe : false
            );
            
            // RecipeResponseDTO를 Map으로 변환
            Map<String, Object> response = new HashMap<>();
            if (recipeResponse.getRecipe() != null) {
                response.put("type", "recipe-detail");
                response.put("recipe", recipeResponse.getRecipe());
            } else if (recipeResponse.getRecipes() != null && !recipeResponse.getRecipes().isEmpty()) {
                response.put("type", "recipe-list");
                response.put("recipes", recipeResponse.getRecipes());
            } else {
                response.put("type", "text");
                response.put("content", "레시피를 찾을 수 없습니다.");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("채팅 메시지 처리 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
} 