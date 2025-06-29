package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.entity.UserPrompt;
import ac.su.kdt.prompttest.repository.UserPromptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptService {
    
    private final UserService userService;
    private final UserPromptRepository userPromptRepository;
    
    public String generatePrompt(Integer userId, String request, Boolean useRefrigerator) {
        return generatePrompt(userId, request, useRefrigerator, false); // 기본값은 메뉴 추천
    }
    
    public String generatePrompt(Integer userId, String request, Boolean useRefrigerator, Boolean isSpecificRecipe) {
        try {
            log.info("=== 프롬프트 생성 시작 ===");
            log.info("사용자 ID: {}", userId);
            log.info("사용자 요청: {}", request);
            log.info("냉장고 사용 여부: {}", useRefrigerator);
            log.info("특정 레시피 요청 여부: {}", isSpecificRecipe);
            
            User user = userService.getUserById(userId);
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            
            UserPrompt userPrompt = userPromptRepository.findByUser(user);
            
            // isSpecificRecipe 파라미터를 사용하여 요청 유형 결정
            log.info("요청 유형: {}", isSpecificRecipe ? "특정 레시피 요청" : "메뉴 추천 요청");
            
            String finalPrompt;
            if (isSpecificRecipe) {
                finalPrompt = generateSpecificRecipePrompt(userId, request, useRefrigerator, user, userPrompt);
            } else {
                finalPrompt = generateMenuRecommendationPrompt(userId, request, useRefrigerator, user, userPrompt);
            }
            
            log.info("=== 최종 생성된 프롬프트 ===");
            log.info(finalPrompt);
            log.info("=== 프롬프트 생성 완료 ===");
            
            return finalPrompt;
            
        } catch (Exception e) {
            log.error("Error generating prompt for user {}: {}", userId, e.getMessage());
            return getDefaultPrompt(request);
        }
    }
    
    private String getDefaultPrompt(String request) { // 비회원일 경우 개인화 서비스는 사용 불가
        return String.format("""
            당신은 레시피 추천 전문가입니다. 사용자의 요청에 맞는 레시피를 추천해주세요.

            ## 사용자 요청
            %s

            레시피는 다음 형식으로 제공해주세요:
            ## 요리 이름

            **필요한 재료**
            - 재료명
            - 재료명
            ...

            **조리 시간**
            - 시간분

            **난이도**
            - 난이도 (상/중/하 중 하나)

            **상세한 조리 방법**
            1. 첫 번째 단계
            2. 두 번째 단계
            ...

            **요리 팁과 주의사항**
            - 팁 또는 주의사항
            - 팁 또는 주의사항

            **완성된 요리의 이미지 URL**
            - 인터넷에서 해당 요리를 검색하여 실제 이미지 URL을 제공해주세요 (교육/개인 목적)

            주의사항:
            - 모든 재료의 양과 단위를 정확히 명시해주세요.
            - 조리 방법은 반드시 포함해야 하며, 순서대로 상세히 설명해주세요.
            - 요리 팁과 주의사항은 실제 조리 시 도움이 될 수 있는 내용을 포함해주세요.
            - 요리 이미지 URL은 인터넷 검색을 통해 실제 존재하는 이미지의 URL을 제공해주세요. (교육/개인 목적)
            - 난이도는 반드시 '상', '중', '하' 중 하나로만 표기해주세요.
            - 각 섹션은 정확히 위의 형식을 따라주세요.
            - 조리 방법 섹션을 절대 생략하지 마세요. 반드시 포함해주세요.
            - '상세한 조리 방법' 섹션이 누락되면 답변하지 마세요. 반드시 포함하세요.
            - 이미지 URL 섹션이 누락되면 답변하지 마세요. 반드시 포함하세요.
            - 아래 예시처럼 모든 섹션이 빠짐없이 포함된 답변만 작성하세요.
            """, request);
    }
    
    // 메뉴 추천용 프롬프트 생성 (간단한 요리명 목록)
    private String generateMenuRecommendationPrompt(Integer userId, String request, Boolean useRefrigerator, User user, UserPrompt userPrompt) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("당신은 메뉴 추천 전문가입니다. 사용자의 요청에 맞는 5개의 메뉴를 추천해주세요.\n\n");
        
        // 1. 보유 재료 우선 추천 (1순위 - useRefrigerator가 true일 때)
        if (useRefrigerator != null && useRefrigerator) {
            List<String> userIngredients = userService.getUserIngredientNames(userId);
            if (userIngredients != null && !userIngredients.isEmpty()) {
                promptBuilder.append("## 보유 재료 우선 추천 (1순위 - 가장 중요)\n");
                for (String ingredient : userIngredients) {
                    promptBuilder.append("- ").append(ingredient).append("\n");
                }
                promptBuilder.append("**중요**: 위 재료 중 1개 이상을 반드시 포함한 메뉴를 우선적으로 추천해주세요.\n\n");
            } else {
                promptBuilder.append("## 보유 재료\n");
                promptBuilder.append("등록된 보유 재료가 없습니다. 일반적인 재료로 메뉴를 추천해주세요.\n\n");
            }
        }
        
        // 2. 사용자 요청 (2순위)
        promptBuilder.append("## 사용자 요청 (2순위)\n");
        promptBuilder.append("요청: ").append(request).append("\n");
        promptBuilder.append("**중요**: 위 요청의 맥락(아침/점심/저녁, 간단한/정성스러운, 건강한/기름진 등)을 정확히 파악하여 적절한 메뉴를 추천해주세요.\n");
        promptBuilder.append("예시: '아침 메뉴' 요청 → 아침에 먹기 적절한 가벼운 메뉴 추천\n");
        promptBuilder.append("예시: '간단한 저녁' 요청 → 저녁에 간단하게 만들 수 있는 메뉴 추천\n\n");
        
        // 3. 개인화 정보 (3순위)
        if (userPrompt != null) {
            promptBuilder.append("## 개인화 정보 (3순위)\n");
            promptBuilder.append("- 나이: ").append(userPrompt.getAge()).append("세\n");
            promptBuilder.append("- 성별: ").append(userPrompt.getGender().equals("M") ? "남성" : "여성").append("\n");
            
            if (userPrompt.getHealthStatus() != null && !userPrompt.getHealthStatus().trim().isEmpty()) {
                promptBuilder.append("- 건강 상태: ").append(userPrompt.getHealthStatus()).append("\n");
            }
            
            if (userPrompt.getAllergy() != null && !userPrompt.getAllergy().trim().isEmpty()) {
                promptBuilder.append("- 알레르기: ").append(userPrompt.getAllergy()).append("\n");
                promptBuilder.append("**중요**: 위 알레르기 재료가 포함된 메뉴는 추천하지 마세요.\n");
            }
            
            promptBuilder.append("**고려사항**: 위 개인화 정보를 바탕으로 건강하고 안전한 메뉴를 추천해주세요.\n\n");
        }
        
        // 4. 사용자 선호도 (4순위 - 참고사항)
        if (userPrompt != null && userPrompt.getPreference() != null && !userPrompt.getPreference().trim().isEmpty()) {
            promptBuilder.append("## 사용자 선호도 (4순위 - 참고사항)\n");
            promptBuilder.append("- 선호도: ").append(userPrompt.getPreference()).append("\n");
            promptBuilder.append("**참고**: 위 선호도는 참고사항이며, 보유 재료, 사용자 요청, 개인화 정보가 우선입니다.\n\n");
        }
        
        // 5. 메뉴 추천 형식 지정
        promptBuilder.append("## 메뉴 추천 형식\n");
        promptBuilder.append("**다음 형식으로 5개의 메뉴명만 제공해주세요:**\n\n");
        promptBuilder.append("### 추천 메뉴 목록\n\n");
        promptBuilder.append("1. [메뉴명 1]\n");
        promptBuilder.append("2. [메뉴명 2]\n");
        promptBuilder.append("3. [메뉴명 3]\n");
        promptBuilder.append("4. [메뉴명 4]\n");
        promptBuilder.append("5. [메뉴명 5]\n\n");
        
        promptBuilder.append("### 추천 이유\n");
        promptBuilder.append("- 상황 분석: [사용자 요청에 맞는 상황 설명]\n");
        promptBuilder.append("- 추천 기준: [어떤 기준으로 메뉴를 선택했는지 설명]\n");
        promptBuilder.append("- 개인화 고려사항: [알레르기, 건강상태 등을 어떻게 고려했는지]\n");
        promptBuilder.append("- 추가 팁: [선택한 메뉴에 대한 추가 조언]\n\n");
        
        promptBuilder.append("**중요**: 메뉴 추천이므로 상세한 레시피가 아닌 메뉴명 목록과 추천 이유만 제공해주세요.\n");
        promptBuilder.append("**중요**: 각 메뉴의 추천 이유는 반드시 사용자 요청과의 연관성을 명확히 설명해주세요.\n");
        promptBuilder.append("**중요**: 사용자가 특정 메뉴의 레시피를 원한다면 해당 메뉴명을 클릭하거나 '레시피'라는 단어를 포함해서 다시 요청하라고 안내해주세요.\n");
        promptBuilder.append("**우선순위**: 1순위(보유 재료) > 2순위(사용자 요청) > 3순위(개인화 정보) > 4순위(선호도) 순으로 고려해주세요.\n");
        
        return promptBuilder.toString();
    }
    
    // 특정 레시피 요청용 프롬프트 생성
    private String generateSpecificRecipePrompt(Integer userId, String request, Boolean useRefrigerator, User user, UserPrompt userPrompt) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("당신은 레시피 전문가입니다. 사용자가 요청한 특정 요리의 상세한 레시피를 제공해주세요.\n\n");
        
        // 1. 보유 재료 우선 추천 (1순위)
        if (useRefrigerator != null && useRefrigerator) {
            List<String> userIngredients = userService.getUserIngredientNames(userId);
            if (userIngredients != null && !userIngredients.isEmpty()) {
                promptBuilder.append("## 보유 재료 우선 추천 (1순위)\n");
                for (String ingredient : userIngredients) {
                    promptBuilder.append("- ").append(ingredient).append("\n");
                }
                promptBuilder.append("**중요**: 위 재료 중 1개 이상을 반드시 포함한 레시피를 우선적으로 추천해주세요.\n\n");
            } else {
                promptBuilder.append("## 보유 재료\n");
                promptBuilder.append("등록된 보유 재료가 없습니다. 일반적인 재료로 레시피를 추천해주세요.\n\n");
            }
        }
        
        // 2. 사용자 요청 (2순위)
        promptBuilder.append("## 사용자 요청 (2순위)\n");
        promptBuilder.append("요청: ").append(request).append("\n");
        promptBuilder.append("**중요**: 위 요청에 맞는 특정 요리의 상세한 레시피를 제공해주세요.\n\n");
        
        // 3. 알레르기 체크 (3순위)
        if (userPrompt != null && userPrompt.getAllergy() != null && !userPrompt.getAllergy().trim().isEmpty()) {
            promptBuilder.append("## 알레르기 정보 (3순위)\n");
            promptBuilder.append("사용자 알레르기: ").append(userPrompt.getAllergy()).append("\n");
            promptBuilder.append("**중요**: 요청한 요리에 위 알레르기 재료가 포함되어 있다면, '알레르기 재료가 포함되어 섭취할 수 없습니다'라고 명시하고 대체 레시피를 제안하세요.\n\n");
        }
        
        // 4. 사용자 선호도 (4순위)
        if (userPrompt != null) {
            promptBuilder.append("## 사용자 선호도 (4순위 - 참고사항)\n");
            promptBuilder.append("- 나이: ").append(userPrompt.getAge()).append("세\n");
            promptBuilder.append("- 성별: ").append(userPrompt.getGender().equals("M") ? "남성" : "여성").append("\n");
            
            if (userPrompt.getHealthStatus() != null) {
                promptBuilder.append("- 건강 상태: ").append(userPrompt.getHealthStatus()).append("\n");
            }
            
            if (userPrompt.getPreference() != null && !userPrompt.getPreference().trim().isEmpty()) {
                promptBuilder.append("- 선호도: ").append(userPrompt.getPreference()).append("\n");
            }
            
            promptBuilder.append("**참고**: 위 정보는 참고사항이며, 보유 재료와 사용자 요청이 우선입니다.\n\n");
        }
        
        // 5. 레시피 형식 지정
        promptBuilder.append("## 레시피 형식\n");
        promptBuilder.append("**반드시 다음 형식으로 정확히 제공해주세요. 모든 섹션을 빠짐없이 포함해야 합니다:**\n\n");
        promptBuilder.append("1. 요리 이름: [요리명]\n");
        promptBuilder.append("2. 카테고리: [한식/중식/일식/양식/분식/기타]\n");
        promptBuilder.append("3. 조리 시간: [분]\n");
        promptBuilder.append("4. 조리 방법:\n");
        promptBuilder.append("   1. 첫 번째 단계 (구체적인 조리 방법)\n");
        promptBuilder.append("   2. 두 번째 단계 (구체적인 조리 방법)\n");
        promptBuilder.append("   3. 세 번째 단계 (구체적인 조리 방법)\n");
        promptBuilder.append("   ...\n\n");
        promptBuilder.append("5. 필요한 재료와 양:\n");
        promptBuilder.append("   - 재료명과 양\n");
        promptBuilder.append("   - 재료명과 양\n");
        promptBuilder.append("   ...\n\n");
        promptBuilder.append("6. 난이도: [상/중/하]\n");
        promptBuilder.append("7. 이미지 URL: [실제 요리 이미지 URL]\n");
        promptBuilder.append("   **중요**: Imgur (https://imgur.com) 사이트에서만 해당 요리의 이미지를 찾아주세요.\n");
        promptBuilder.append("   **중요**: Imgur에서 요리명으로 검색하여 실제 존재하는 요리 이미지의 URL을 제공해주세요.\n");
        promptBuilder.append("   **예시**: https://i.imgur.com/example.jpg\n");
        promptBuilder.append("   **참고**: 이는 교육 및 개인 학습 목적으로만 사용되며, 상업적 목적이 아닙니다.\n");
        promptBuilder.append("8. 요리 팁:\n");
        promptBuilder.append("   - 팁이나 주의사항\n");
        promptBuilder.append("   - 팁이나 주의사항\n\n");
        
        promptBuilder.append("**필수**: 반드시 위의 8개 섹션을 모두 포함해주세요. 1번부터 8번까지 빠짐없이 작성해야 합니다.\n");
        promptBuilder.append("**중요**: 재료는 5번에서만 나열하고, 4번 조리 방법에서는 실제 조리 단계만 설명해주세요.\n");
        promptBuilder.append("**중요**: 7번 이미지 URL은 인터넷 검색을 통해 실제 요리 이미지의 URL을 제공해주세요. (교육/개인 목적)\n");
        promptBuilder.append("**경고**: 섹션이 누락되면 답변하지 마세요. 모든 섹션이 완성된 레시피만 제공해주세요.\n");
        
        return promptBuilder.toString();
    }
} 