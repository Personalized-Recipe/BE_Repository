package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.RecipeResponseDTO;
import ac.su.kdt.prompttest.entity.ChatHistory;
import ac.su.kdt.prompttest.entity.Recipe;
import ac.su.kdt.prompttest.entity.UserRecipe;
import ac.su.kdt.prompttest.repository.RecipeRepository;
import ac.su.kdt.prompttest.repository.UserRecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RecipeService {
    
    private final RecipeRepository recipeRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final PerplexityService perplexityService;
    private final ChatService chatService;

    @Transactional
    public RecipeResponseDTO requestRecipe(Integer userId, String chatRoomId, String request, Boolean useRefrigerator, Boolean isSpecificRecipe) {
        try {
            // 채팅방 ID 파싱
            Integer roomId = null;
            if (chatRoomId != null && !chatRoomId.trim().isEmpty()) {
                try {
                    roomId = Integer.parseInt(chatRoomId);
                } catch (NumberFormatException e) {
                    // chatRoomId가 숫자가 아닌 경우 무시
                }
            }
            
            // 1. 사용자 메시지 저장 (한 번만)
            ChatHistory userMessage = chatService.saveChat(userId, request, true, null, roomId);
            
            // 2. Perplexity API를 통해 레시피 생성
            RecipeResponseDTO recipeResponse = perplexityService.getResponse(userId, request, useRefrigerator, isSpecificRecipe);
            
            // 3. AI 응답을 채팅 히스토리에 저장
            String responseMessage;
            Integer recipeId = null;
            
            if (recipeResponse.getType().equals("recipe-list")) {
                responseMessage = "메뉴 추천:\n";
                for (Recipe recipe : recipeResponse.getRecipes()) {
                    responseMessage += "- " + recipe.getTitle() + "\n";
                }
                // 메뉴 추천의 경우 recipe_id는 null (개별 레시피가 아니므로)
            } else {
                // 특정 레시피인 경우
                Recipe recipe = recipeResponse.getRecipe();
                responseMessage = recipe.getTitle() + "\n" + recipe.getDescription();
                recipeId = recipe.getRecipeId(); // 레시피 ID 저장
            }
            
            ChatHistory aiMessage = chatService.saveChat(userId, responseMessage, false, recipeId, roomId);
            
            return recipeResponse;
            
        } catch (Exception e) {
            // 에러 발생 시 로그 기록
            System.err.println("레시피 요청 처리 중 에러 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("레시피 요청 처리에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    public List<UserRecipe> getRecipeHistory(Integer userId) {
        return userRecipeRepository.findByUserId(userId);
    }

    @Transactional // 사용자가 레시피를 저장
    public UserRecipe saveUserRecipe(Integer userId, Integer recipeId) {
        // Check if recipe exists
        recipeRepository.findById(recipeId)
            .orElseThrow(() -> new RuntimeException("Recipe not found"));
            
        UserRecipe userRecipe = UserRecipe.builder()
            .userId(userId)
            .recipeId(recipeId)
            .build();
            
        return userRecipeRepository.save(userRecipe);
    }

    @Transactional // 사용자가 저장한 레시피 삭제
    public void deleteUserRecipe(Integer userId, Integer recipeId) {
        UserRecipe.UserRecipeId id = new UserRecipe.UserRecipeId(userId, recipeId);
        userRecipeRepository.deleteById(id);
    }

    public Recipe getRecipeById(Integer recipeId) { // 레시피 상세 조회
        return recipeRepository.findById(recipeId)
            .orElseThrow(() -> new RuntimeException("Recipe not found"));
    }

    // Helper methods
    private String extractTitle(String response) {
        // "1. 요리 이름: " 다음에 오는 텍스트를 추출
        Pattern pattern = Pattern.compile("1\\.\\s*요리 이름:\\s*([^\\n]+)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "레시피 제목"; // 기본값
    }

    private String determineCategory(String response) {
        // 한식, 중식, 일식, 양식, 분식, 퓨전 중 하나를 찾음
        Pattern pattern = Pattern.compile("(한식|중식|일식|양식|분식|퓨전)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "한식"; // 기본값
    }

    private Integer extractCookingTime(String response) {
        // "3. 조리 시간: " 다음에 오는 숫자를 추출
        Pattern pattern = Pattern.compile("3\\.\\s*조리 시간:\\s*(\\d+)분");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 30; // 기본값
    }

    private String determineDifficulty(String response) {
        // "4. 난이도: " 다음에 오는 상/중/하를 추출
        Pattern pattern = Pattern.compile("4\\.\\s*난이도:\\s*(상|중|하)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "중"; // 기본값
    }

    private String extractIngredients(String response) {
        // "2. 필요한 재료와 양:" 다음에 오는 텍스트를 추출
        Pattern pattern = Pattern.compile("2\\.\\s*필요한 재료와 양:\\s*([^3]+)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return ""; // 기본값
    }

    private String extractImageUrl(String response) {
        // 7. 이미지 URL: 패턴 시도
        Pattern pattern = Pattern.compile("7\\.\\s*이미지 URL\\s*:\\s*([^\\n]+)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // 5. 이미지 URL: 패턴 시도 (기존 형식)
        pattern = Pattern.compile("5\\.\\s*이미지 URL\\s*:\\s*([^\\n]+)");
        matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // 일반적인 이미지 URL 패턴 시도
        pattern = Pattern.compile("(https?://[^\\s]+\\.[^\\s]+(?:png|jpg|jpeg|gif|webp))");
        matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return null; // 이미지 URL을 찾을 수 없는 경우
    }
} 