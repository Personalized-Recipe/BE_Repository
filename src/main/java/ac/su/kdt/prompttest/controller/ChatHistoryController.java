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
import java.util.ArrayList;

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
        
        // 메뉴 추천 메시지인지 확인
        if (isMenuRecommendation(content)) {
            log.info("메뉴 추천 메시지로 판별됨");
            message.put("type", "recipe-list");
            List<Map<String, Object>> recipes = parseMenuRecommendation(content);
            message.put("recipes", recipes);
            return message;
        }
        
        // 일반 텍스트로 처리
        log.info("일반 텍스트 메시지로 처리됨");
        message.put("type", "text");
        message.put("content", content);
        return message;
    }
    
    /**
     * 메뉴 추천 메시지인지 판별
     * @param content 메시지 내용
     * @return 메뉴 추천 여부
     */
    private boolean isMenuRecommendation(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String lowerContent = content.toLowerCase();
        
        // 메뉴 추천 키워드 확인
        boolean hasRecommendationKeywords = lowerContent.contains("메뉴 추천") || 
                                          lowerContent.contains("추천 메뉴") ||
                                          lowerContent.contains("추천:") ||
                                          lowerContent.contains("다음과 같은") ||
                                          lowerContent.contains("추천드립니다");
        
        // 메뉴 목록 패턴 확인 (줄바꿈으로 구분된 항목들)
        String[] lines = content.split("\n");
        int menuItemCount = 0;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;
            
            // 메뉴 항목 패턴 확인
            if (trimmedLine.startsWith("- ") || 
                trimmedLine.startsWith("• ") ||
                trimmedLine.matches("^\\d+\\..*") ||
                (trimmedLine.length() > 1 && !trimmedLine.contains(":") && !trimmedLine.contains("재료") && !trimmedLine.contains("조리"))) {
                menuItemCount++;
            }
        }
        
        return hasRecommendationKeywords || menuItemCount >= 2;
    }
    
    /**
     * 메뉴 추천 메시지를 파싱해서 레시피 목록 생성
     * @param content 메뉴 추천 메시지
     * @return 레시피 목록
     */
    private List<Map<String, Object>> parseMenuRecommendation(String content) {
        List<Map<String, Object>> recipes = new ArrayList<>();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;
            
            // 메뉴명 추출
            String menuTitle = extractMenuTitle(trimmedLine);
            if (menuTitle != null && !menuTitle.isEmpty()) {
                Map<String, Object> recipe = new HashMap<>();
                
                // DB에서 해당 메뉴명의 상세 레시피 정보 조회
                try {
                    Recipe detailedRecipe = recipeService.getRecipeByTitle(menuTitle);
                    if (detailedRecipe != null) {
                        // DB에 저장된 상세 정보가 있는 경우
                        recipe.put("recipeId", detailedRecipe.getRecipeId());
                        recipe.put("title", detailedRecipe.getTitle());
                        recipe.put("description", detailedRecipe.getDescription());
                        recipe.put("category", detailedRecipe.getCategory());
                        recipe.put("imageUrl", detailedRecipe.getImageUrl());
                        recipe.put("cookingTime", detailedRecipe.getCookingTime());
                        recipe.put("difficulty", detailedRecipe.getDifficulty());
                        recipe.put("hasDetailedInfo", true);
                        log.info("메뉴 '{}'의 상세 레시피 정보를 DB에서 조회했습니다. ID: {}", menuTitle, detailedRecipe.getRecipeId());
                    } else {
                        // DB에 상세 정보가 없는 경우 (기본 정보만)
                        recipe.put("recipeId", null);
                        recipe.put("title", menuTitle);
                        recipe.put("description", "");
                        recipe.put("category", "기타");
                        recipe.put("imageUrl", "https://i.imgur.com/8tMUxoP.jpg");
                        recipe.put("cookingTime", "정보 없음");
                        recipe.put("difficulty", "정보 없음");
                        recipe.put("hasDetailedInfo", false);
                        log.info("메뉴 '{}'의 상세 레시피 정보가 DB에 없습니다.", menuTitle);
                    }
                } catch (Exception e) {
                    log.error("메뉴 '{}'의 상세 레시피 정보 조회 중 오류: {}", menuTitle, e.getMessage());
                    // 오류 발생 시 기본 정보로 설정
                    recipe.put("recipeId", null);
                    recipe.put("title", menuTitle);
                    recipe.put("description", "");
                    recipe.put("category", "기타");
                    recipe.put("imageUrl", "https://i.imgur.com/8tMUxoP.jpg");
                    recipe.put("cookingTime", "정보 없음");
                    recipe.put("difficulty", "정보 없음");
                    recipe.put("hasDetailedInfo", false);
                }
                
                recipes.add(recipe);
            }
        }
        
        return recipes;
    }
    
    /**
     * 메뉴명 추출
     * @param line 원본 라인
     * @return 추출된 메뉴명
     */
    private String extractMenuTitle(String line) {
        // 다양한 패턴으로 메뉴명 추출
        String[] patterns = {
            "^[-•]\\s*(.+)$",           // "- 메뉴명" 또는 "• 메뉴명"
            "^\\d+\\.\\s*(.+)$",        // "1. 메뉴명"
            "^([가-힣\\s]+)$",          // 한글만 있는 줄
            "^(.+?)(?:\\s*[-•]\\s*|$)"  // 일반적인 메뉴명 패턴
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(line);
            
            if (m.find() && m.group(1) != null) {
                String candidate = m.group(1).trim();
                
                // 메뉴명이 아닌 키워드 제외
                if (isValidMenuTitle(candidate)) {
                    return candidate;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 유효한 메뉴명인지 확인
     * @param title 메뉴명 후보
     * @return 유효성 여부
     */
    private boolean isValidMenuTitle(String title) {
        if (title == null || title.length() <= 1) {
            return false;
        }
        
        String lowerTitle = title.toLowerCase();
        
        // 제외할 키워드들
        String[] excludeKeywords = {
            "메뉴 추천", "추천 메뉴", "다음과 같은", "추천드립니다", 
            "레시피", "재료", "조리법", "조리 방법", "필요한 재료"
        };
        
        for (String keyword : excludeKeywords) {
            if (lowerTitle.contains(keyword)) {
                return false;
            }
        }
        
        return true;
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