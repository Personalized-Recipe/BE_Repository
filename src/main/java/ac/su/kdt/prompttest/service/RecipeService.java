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
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

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
            Integer roomId = null;
            if (chatRoomId != null && !chatRoomId.trim().isEmpty()) {
                try {
                    roomId = Integer.parseInt(chatRoomId);
                } catch (NumberFormatException e) {
                    // chatRoomId가 숫자가 아닌 경우 무시
                }
            }
            
            // 1. 사용자 메시지 저장 (먼저 실행)
            ChatHistory userMessage = chatService.saveChat(userId, request, true, null, roomId);
            
            // 2. Perplexity API를 통해 레시피 생성 (사용자 메시지 저장 이후)
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

    public Recipe getRecipeByTitle(String title) { // 제목으로 레시피 조회
        if (title == null || title.trim().isEmpty()) {
            return null;
        }
        
        // 정확한 매칭 시도
        Recipe exactMatch = recipeRepository.findByTitle(title.trim());
        if (exactMatch != null) {
            System.out.println("정확한 매칭 발견: " + title);
            return exactMatch;
        }
        
        // 부분 매칭 시도 (제목에 포함되는 경우)
        List<Recipe> allRecipes = recipeRepository.findAll();
        for (Recipe recipe : allRecipes) {
            if (recipe.getTitle() != null && 
                (recipe.getTitle().contains(title.trim()) || title.trim().contains(recipe.getTitle()))) {
                System.out.println("부분 매칭 발견: " + title + " -> " + recipe.getTitle());
                return recipe;
            }
        }
        
        System.out.println("매칭되는 레시피 없음: " + title);
        return null;
    }

    /**
     * 메뉴명 추출 (실제 저장 형식에 맞게 수정)
     * @param line 원본 라인
     * @return 추출된 메뉴명 (- 메뉴명 형식)
     */
    public String extractMenuTitle(String line) {
        // '- 메뉴명' 형식 추출
        Pattern pattern = Pattern.compile("^-\\s*(.+)$");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find() && matcher.group(1) != null) {
            String extractedTitle = matcher.group(1).trim();
            
            // 재료명이나 부적절한 텍스트 필터링
            if (isValidMenuTitle(extractedTitle)) {
                return extractedTitle;
            }
        }
        return null;
    }
    
    /**
     * 추출된 텍스트가 유효한 메뉴명인지 검증
     * @param title 검증할 메뉴명
     * @return 유효한 메뉴명 여부
     */
    private boolean isValidMenuTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        
        String lowerTitle = title.toLowerCase().trim();
        
        // 너무 짧은 텍스트 (2글자 이하) 제외
        if (title.length() <= 2) {
            return false;
        }
        
        // 메뉴명에 적합한 키워드가 포함된 경우 우선 포함
        String[] menuKeywords = {
            "찌개", "국", "탕", "찜", "구이", "볶음", "튀김", "전", "부침", "무침",
            "나물", "김치", "반찬", "밥", "면", "국수", "라면", "떡", "만두",
            "스프", "샐러드", "스테이크", "파스타", "피자", "햄버거", "샌드위치",
            "케이크", "빵", "과자", "아이스크림", "주스", "차", "커피", "술",
            "요리", "메뉴", "음식", "식사", "아침", "점심", "저녁", "간식",
            "도시락", "비빔밥", "덮밥", "칼국수", "말이", "계란말이"
        };
        
        for (String keyword : menuKeywords) {
            if (lowerTitle.contains(keyword)) {
                return true;
            }
        }
        
        // 단순한 재료명만 필터링 (메뉴명에 포함된 경우는 허용)
        String[] simpleIngredientPatterns = {
            "재료", "양념", "소스", "장", "가루", "오일", "식용유", "참기름", "들기름",
            "간장", "고추장", "된장", "고춧가루", "설탕", "소금", "후추",
            "다진", "마늘", "양파", "대파", "고추", "당근", "감자", "고구마", "버섯",
            "상추", "깻잎", "무", "배추", "시금치", "부추", "파", "쪽파",
            "돈까스", "삼겹살", "목살", "등심", "안심", "갈비", "갈비살", "양고기", "오리고기", "돈육",
            "소주", "미림", "굴소스", "두부", "치즈", "버터", "우유", "생크림",
            "밀가루", "부침가루", "빵가루", "깨", "들깨", "참깨", "흑임자", "견과류", "아몬드", "호두", "땅콩"
        };
        
        // 단순한 재료명 패턴 체크 (메뉴명이 아닌 경우만)
        for (String pattern : simpleIngredientPatterns) {
            if (lowerTitle.equals(pattern)) { // 정확히 일치하는 경우만 제외
                return false;
            }
        }
        
        // 조리 관련 동사나 부사가 포함된 경우 제외
        String[] cookingVerbs = {
            "볶아", "끓여", "굽", "튀겨", "찌", "삶", "데쳐", "무쳐", "양념",
            "섞어", "고루", "골고루", "잘게", "썰어", "다져", "썰", "썰어서",
            "채", "채 썰어", "다진", "다져서", "잘게 썰어", "잘게 다져"
        };
        
        for (String verb : cookingVerbs) {
            if (lowerTitle.contains(verb)) {
                return false;
            }
        }
        
        // 기본적으로는 포함 (메뉴명에 재료가 포함되어도 허용)
        return true;
    }
    
    /**
     * 메뉴 추천 메시지를 파싱해서 레시피 목록 생성 (개선된 버전)
     * @param content 메뉴 추천 메시지
     * @return 레시피 목록
     */
    public List<Map<String, Object>> parseMenuRecommendation(String content) {
        List<Map<String, Object>> recipes = new ArrayList<>();
        String[] lines = content.split("\n");
        
        System.out.println("=== 메뉴 추천 메시지 파싱 시작 ===");
        System.out.println("원본 메시지: " + content);
        System.out.println("총 라인 수: " + lines.length);
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();
            System.out.println("라인 " + (i+1) + ": '" + trimmedLine + "'");
            
            if (trimmedLine.isEmpty()) {
                System.out.println("  -> 빈 라인, 건너뜀");
                continue;
            }
            
            // 메뉴명 추출 (개선된 검증 로직 적용)
            String menuTitle = extractMenuTitle(trimmedLine);
            if (menuTitle != null && !menuTitle.isEmpty()) {
                System.out.println("  -> 추출된 메뉴명: '" + menuTitle + "'");
                
                // 추가 검증: 메뉴명이 실제로 유효한지 확인
                if (isValidMenuTitleForDisplay(menuTitle)) {
                    System.out.println("  -> 유효한 메뉴명으로 판별됨");
                    Map<String, Object> recipe = new HashMap<>();
                    
                    // DB에서 해당 메뉴명의 상세 레시피 정보 조회
                    try {
                        Recipe detailedRecipe = getRecipeByTitle(menuTitle);
                        if (detailedRecipe != null) {
                            // DB에 저장된 상세 정보가 있는 경우
                            recipe.put("recipeId", detailedRecipe.getRecipeId());
                            recipe.put("title", detailedRecipe.getTitle());
                            recipe.put("description", detailedRecipe.getDescription());
                            recipe.put("category", detailedRecipe.getCategory());
                            recipe.put("imageUrl", detailedRecipe.getImageUrl());
                            recipe.put("cookingTime", detailedRecipe.getCookingTime());
                            recipe.put("difficulty", detailedRecipe.getDifficulty());
                            
                            // 상세 정보가 충분한 경우 hasDetailedInfo를 true로 설정
                            boolean hasDetailedInfo = detailedRecipe.getDescription() != null && 
                                                    detailedRecipe.getDescription().length() > 100;
                            recipe.put("hasDetailedInfo", hasDetailedInfo);
                            
                            System.out.println("  -> DB에서 상세 정보 조회 성공. ID: " + detailedRecipe.getRecipeId() + ", hasDetailedInfo: " + hasDetailedInfo);
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
                            System.out.println("  -> DB에 상세 정보 없음, 기본 정보로 설정");
                        }
                    } catch (Exception e) {
                        System.err.println("  -> 메뉴 '" + menuTitle + "'의 상세 레시피 정보 조회 중 오류: " + e.getMessage());
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
                    System.out.println("  -> 레시피 목록에 추가됨");
                } else {
                    System.out.println("  -> 유효하지 않은 메뉴명으로 판별되어 제외: '" + menuTitle + "'");
                }
            } else {
                System.out.println("  -> 메뉴명 추출 실패");
            }
        }
        
        System.out.println("=== 파싱 완료. 총 " + recipes.size() + "개의 메뉴를 추출했습니다. ===");
        for (int i = 0; i < recipes.size(); i++) {
            Map<String, Object> recipe = recipes.get(i);
            System.out.println("  " + (i+1) + ". " + recipe.get("title"));
        }
        return recipes;
    }
    
    /**
     * 추출된 메뉴명이 화면에 표시하기에 적합한지 추가 검증
     * @param menuTitle 검증할 메뉴명
     * @return 표시 가능 여부
     */
    private boolean isValidMenuTitleForDisplay(String menuTitle) {
        if (menuTitle == null || menuTitle.trim().isEmpty()) {
            return false;
        }
        
        String title = menuTitle.trim();
        
        // 너무 짧은 텍스트 (2글자 이하) 제외
        if (title.length() <= 2) {
            return false;
        }
        
        // 너무 긴 텍스트 (50글자 이상) 제외
        if (title.length() > 50) {
            return false;
        }
        
        // 특수문자나 기호가 과도하게 포함된 경우 제외
        int specialCharCount = 0;
        for (char c : title.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c) && c != '가' && c != '나' && c != '다' && c != '라' && c != '마' && c != '바' && c != '사' && c != '아' && c != '자' && c != '차' && c != '카' && c != '타' && c != '파' && c != '하') {
                specialCharCount++;
            }
        }
        if (specialCharCount > title.length() * 0.5) { // 50% 이상이 특수문자인 경우만 제외
            return false;
        }
        
        // 명확한 메뉴명 키워드가 포함된 경우 우선 포함
        String[] menuKeywords = {
            "찌개", "국", "탕", "찜", "구이", "볶음", "튀김", "전", "부침", "무침",
            "나물", "김치", "반찬", "밥", "면", "국수", "라면", "떡", "만두",
            "스프", "샐러드", "스테이크", "파스타", "피자", "햄버거", "샌드위치",
            "케이크", "빵", "과자", "아이스크림", "주스", "차", "커피", "술",
            "요리", "메뉴", "음식", "식사", "아침", "점심", "저녁", "간식"
        };
        
        String lowerTitle = title.toLowerCase();
        for (String keyword : menuKeywords) {
            if (lowerTitle.contains(keyword)) {
                return true;
            }
        }
        
        // 기본적으로는 포함하되, 명확한 재료명은 제외
        return true;
    }
    
    /**
     * 잘못된 메뉴명으로 저장된 레시피들을 조회 (관리자용)
     * @return 잘못된 메뉴명 목록
     */
    public List<Map<String, Object>> findInvalidRecipeTitles() {
        List<Map<String, Object>> invalidRecipes = new ArrayList<>();
        List<Recipe> allRecipes = recipeRepository.findAll();
        
        for (Recipe recipe : allRecipes) {
            if (!isValidMenuTitle(recipe.getTitle()) || !isValidMenuTitleForDisplay(recipe.getTitle())) {
                Map<String, Object> invalidRecipe = new HashMap<>();
                invalidRecipe.put("recipeId", recipe.getRecipeId());
                invalidRecipe.put("title", recipe.getTitle());
                invalidRecipe.put("category", recipe.getCategory());
                invalidRecipe.put("createdAt", recipe.getRecipeId()); // 임시로 recipeId 사용
                invalidRecipes.add(invalidRecipe);
            }
        }
        
        return invalidRecipes;
    }
    
    /**
     * 잘못된 메뉴명의 레시피를 삭제 (관리자용)
     * @param recipeId 레시피 ID
     */
    @Transactional
    public void deleteInvalidRecipe(Integer recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
            .orElseThrow(() -> new RuntimeException("Recipe not found with ID: " + recipeId));
        
        // UserRecipe에서도 삭제 (복합키로 삭제)
        List<UserRecipe> allUserRecipes = userRecipeRepository.findAll();
        for (UserRecipe userRecipe : allUserRecipes) {
            if (userRecipe.getRecipeId().equals(recipeId)) {
                userRecipeRepository.delete(userRecipe);
            }
        }
        
        // Recipe 삭제
        recipeRepository.delete(recipe);
    }
} 