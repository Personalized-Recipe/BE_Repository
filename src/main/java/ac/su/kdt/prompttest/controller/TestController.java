package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.RecipeResponseDTO;
import ac.su.kdt.prompttest.entity.Recipe;
import ac.su.kdt.prompttest.service.PerplexityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import ac.su.kdt.prompttest.entity.ChatHistory;
import ac.su.kdt.prompttest.service.ChatHistoryService;
import ac.su.kdt.prompttest.service.RecipeService;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final PerplexityService perplexityService;
    private final ChatHistoryService chatHistoryService;
    private final RecipeService recipeService;

    @GetMapping("/perplexity")
    public ResponseEntity<String> testPerplexity() {
        String testPrompt = "간단한 김치찌개 레시피를 알려줘";
        RecipeResponseDTO recipeResponse = perplexityService.getResponse(1, testPrompt, false, true);
        
        String response;
        if (recipeResponse.getType().equals("recipe-detail")) {
            Recipe recipe = recipeResponse.getRecipe();
            response = recipe.getTitle() + "\n" + recipe.getDescription();
        } else {
            response = "메뉴 추천:\n";
            for (Recipe recipe : recipeResponse.getRecipes()) {
                response += "- " + recipe.getTitle() + "\n";
            }
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ai-content")
    public String getAIContent(@RequestBody Map<String, Object> req) {
        Integer userId = (Integer) req.get("userId");
        String request = (String) req.get("request");
        RecipeResponseDTO recipeResponse = perplexityService.getResponse(userId, request, false, false);
        
        String response;
        if (recipeResponse.getType().equals("recipe-detail")) {
            Recipe recipe = recipeResponse.getRecipe();
            response = recipe.getTitle() + "\n" + recipe.getDescription();
        } else {
            response = "메뉴 추천:\n";
            for (Recipe recipe : recipeResponse.getRecipes()) {
                response += "- " + recipe.getTitle() + "\n";
            }
        }
        
        return response;
    }

    /**
     * 특정 채팅방의 원본 메시지와 파싱된 메시지를 비교해서 보여주는 테스트 API
     */
    @GetMapping("/parse-test/{chatRoomId}")
    public ResponseEntity<Map<String, Object>> testParsing(@PathVariable Integer chatRoomId) {
        try {
            // 원본 메시지 조회
            List<ChatHistory> originalMessages = chatHistoryService.getChatRoomMessages(1, chatRoomId);
            
            // 파싱된 메시지 생성 (ChatHistoryController의 로직과 동일)
            List<Map<String, Object>> parsedMessages = originalMessages.stream()
                .map(this::parseChatMessage)
                .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> result = Map.of(
                "chatRoomId", chatRoomId,
                "originalMessages", originalMessages,
                "parsedMessages", parsedMessages,
                "messageCount", originalMessages.size()
            );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("파싱 테스트 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * ChatHistory를 프론트엔드용 형식으로 파싱 (ChatHistoryController와 동일한 로직)
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
     * AI 메시지 타입 판별 및 파싱 (ChatHistoryController와 동일한 로직)
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
            List<Map<String, Object>> recipes = recipeService.parseMenuRecommendation(content);
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
     * 메뉴 추천 메시지인지 판별 (ChatHistoryController와 동일한 로직)
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
     * 파싱 로직 테스트용 API (인증 불필요)
     */
    @GetMapping("/parse-demo")
    public ResponseEntity<Map<String, Object>> parseDemo() {
        // 예시 메시지들
        String[] sampleMessages = {
            // 사용자 메시지
            "간단한 아침 메뉴 추천해줘",
            
            // 메뉴 추천 메시지
            "메뉴 추천:\n1. 김치찌개\n2. 된장찌개\n3. 미역국\n4. 계란볶음밥\n5. 토스트",
            
            // 상세 레시피 메시지
            "1. 요리 이름: 김치찌개\n\n2. 필요한 재료와 양:\n- 김치 300g\n- 돼지고기 200g\n- 두부 1/2모\n- 양파 1/2개\n- 대파 1대\n\n3. 조리 시간: 20분\n\n4. 난이도: 중\n\n5. 조리 방법:\n1) 김치를 적당한 크기로 썰어주세요.\n2) 돼지고기를 넣고 볶아주세요.\n3) 물을 넣고 끓여주세요.\n4) 두부를 넣고 마무리해주세요."
        };
        
        List<Map<String, Object>> parsedResults = new ArrayList<>();
        
        for (int i = 0; i < sampleMessages.length; i++) {
            String message = sampleMessages[i];
            Map<String, Object> result = new HashMap<>();
            result.put("originalMessage", message);
            result.put("messageType", i == 0 ? "user" : "bot");
            
            if (i == 0) {
                // 사용자 메시지
                result.put("parsedType", "text");
                result.put("content", message);
            } else if (i == 1) {
                // 메뉴 추천 메시지
                result.put("parsedType", "recipe-list");
                List<Map<String, Object>> recipes = recipeService.parseMenuRecommendation(message);
                result.put("recipes", recipes);
            } else {
                // 상세 레시피 메시지
                result.put("parsedType", "recipe-detail");
                result.put("content", message);
            }
            
            parsedResults.add(result);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("demo", "파싱 로직 테스트");
        response.put("results", parsedResults);
        response.put("explanation", Map.of(
            "user_message", "사용자 메시지는 단순 텍스트로 처리됩니다.",
            "menu_recommendation", "메뉴 추천 메시지는 recipe-list 타입으로 파싱되어 각 메뉴가 버튼으로 표시됩니다.",
            "detailed_recipe", "상세 레시피 메시지는 recipe-detail 타입으로 파싱되어 레시피 카드로 표시됩니다."
        ));
        
        return ResponseEntity.ok(response);
    }

    /**
     * 데이터베이스에 저장된 실제 채팅 메시지들을 확인하는 API
     */
    @GetMapping("/db-messages")
    public ResponseEntity<Map<String, Object>> getDatabaseMessages() {
        try {
            // 특정 사용자의 채팅방 메시지들을 조회 (예시로 userId=1 사용)
            List<ChatHistory> messages = new ArrayList<>();
            
            // 여러 채팅방을 시도해보기
            for (int roomId = 1; roomId <= 5; roomId++) {
                try {
                    List<ChatHistory> roomMessages = chatHistoryService.getChatRoomMessages(1, roomId);
                    messages.addAll(roomMessages);
                } catch (Exception e) {
                    // 해당 채팅방이 없으면 무시
                }
            }
            
            List<Map<String, Object>> messageDetails = new ArrayList<>();
            
            for (ChatHistory message : messages) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("chatId", message.getChatId());
                detail.put("userId", message.getUserId());
                detail.put("chatRoomId", message.getChatRoomId());
                detail.put("isUserMessage", message.isUserMessage());
                detail.put("recipeId", message.getRecipeId());
                detail.put("message", message.getMessage());
                detail.put("createdAt", message.getCreatedAt());
                
                // AI 메시지인 경우 파싱 결과도 포함
                if (!message.isUserMessage()) {
                    try {
                        Map<String, Object> parsedResult = parseAIMessage(message.getMessage(), message.getRecipeId());
                        detail.put("parsedType", parsedResult.get("type"));
                        if (parsedResult.containsKey("recipes")) {
                            detail.put("parsedRecipes", parsedResult.get("recipes"));
                        }
                    } catch (Exception e) {
                        detail.put("parseError", e.getMessage());
                    }
                }
                
                messageDetails.add(detail);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalMessages", messages.size());
            response.put("messages", messageDetails);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("데이터베이스 메시지 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 