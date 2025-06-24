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
    
    public String generatePrompt(Integer userId, String request) {
        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            
            UserPrompt userPrompt = userPromptRepository.findByUser(user);
            StringBuilder promptBuilder = new StringBuilder();
            
            // 1. 기본 시스템 프롬프트
            promptBuilder.append("당신은 레시피 추천 전문가입니다. ");
            
            // 1-1. 유저가 가진 재료 목록 추가
            List<String> userIngredients = userService.getUserIngredientNames(userId);
            if (userIngredients != null && !userIngredients.isEmpty()) {
                promptBuilder.append("사용자가 가지고 있는 재료는 다음과 같습니다:\n");
                for (String ingredient : userIngredients) {
                    promptBuilder.append("- ").append(ingredient).append("\n");
                }
                promptBuilder.append("아래 보유재료를 반드시 1개 이상 포함해서 레시피를 추천해주세요. 보유재료가 하나도 포함되지 않으면 답변하지 마세요. 보유재료를 최대한 많이 활용한 레시피를 우선 추천하고, 불가피할 경우 1개 이상만이라도 반드시 포함해서 추천해주세요.  \n\n");
            }
            
            // 2. 사용자 개인화 정보 추가
            if (userPrompt != null) {
                promptBuilder.append("다음 사용자의 특성을 고려하여 레시피를 추천해주세요:\n\n");
                
                // 기본 정보 (nullable = false인 필드들)
                promptBuilder.append("- 이름: ").append(userPrompt.getName()).append("\n");
                promptBuilder.append("- 나이: ").append(userPrompt.getAge()).append("세\n");
                promptBuilder.append("- 성별: ").append(userPrompt.getGender().equals("M") ? "남성" : "여성").append("\n");
                
                // 선택적 정보 (nullable = true인 필드들)
                if (userPrompt.getNickname() != null) {
                    promptBuilder.append("- 닉네임: ").append(userPrompt.getNickname()).append("\n");
                }
                
                // 건강 관련 정보
                if (userPrompt.getHealthStatus() != null) {
                    promptBuilder.append("- 건강 상태: ").append(userPrompt.getHealthStatus()).append("\n");
                }
                
                // 알레르기 정보
                if (userPrompt.getAllergy() != null) {
                    promptBuilder.append("- 알레르기: ").append(userPrompt.getAllergy()).append("\n");
                }
                
                // 선호도 정보
                if (userPrompt.getPreference() != null) {
                    promptBuilder.append("- 선호하는 음식: ").append(userPrompt.getPreference()).append("\n");
                }
                
                // 임신 여부 (여성인 경우에만)
                if (userPrompt.getGender().equals("F") && userPrompt.getIsPregnant() != null && userPrompt.getIsPregnant()) {
                    promptBuilder.append("- 임신 중이므로 임산부에게 적합한 음식을 추천해주세요.\n");
                }
                
                promptBuilder.append("\n");
                
                // 건강 상태에 따른 특별 지시사항
                if (userPrompt.getHealthStatus() != null) {
                    promptBuilder.append("\n건강 상태에 따른 특별 지시사항:\n");
                    promptBuilder.append("현재 사용자는 ").append(userPrompt.getHealthStatus())
                        .append(" 상태입니다. 해당 건강 상태에 맞는 적절한 식단을 추천해주세요.\n\n")
                        .append("다음 사항을 고려하여 레시피를 추천해주세요:\n")
                        .append("1. 해당 건강 상태에 맞는 영양소를 포함한 재료를 선택해주세요.\n")
                        .append("2. 해당 건강 상태에 해로운 성분이 포함된 재료는 피해주세요.\n")
                        .append("3. 건강 상태 개선에 도움이 되는 조리 방법을 선택해주세요.\n")
                        .append("4. 필요한 경우, 특정 재료의 대체재를 제안해주세요.\n");
                }
                
                // 알레르기 정보가 있는 경우 주의사항
                if (userPrompt.getAllergy() != null) {
                    promptBuilder.append("\n알레르기 주의사항:\n");
                    promptBuilder.append("- ").append(userPrompt.getAllergy())
                        .append(" 알레르기가 있으므로 해당 재료를 제외한 레시피를 추천해주세요.\n");
                }
                
                promptBuilder.append("\n");
            }
            
            // 3. 사용자 요청 추가
            promptBuilder.append("## 사용자 요청\n").append(request).append("\n\n");
            
            // 4. 레시피 형식 지정
            promptBuilder.append("레시피는 다음 형식으로 제공해주세요:\n");
            promptBuilder.append("## 요리 이름\n");
            promptBuilder.append("\n**필요한 재료와 양**\n");
            promptBuilder.append("- 재료명: 양 단위\n");
            promptBuilder.append("- 재료명: 양 단위\n");
            promptBuilder.append("...\n\n");
            promptBuilder.append("**조리 시간**\n");
            promptBuilder.append("- 시간분\n\n");
            promptBuilder.append("**난이도**\n");
            promptBuilder.append("- 난이도 (상/중/하 중 하나)\n\n");
            promptBuilder.append("**상세한 조리 방법**\n");
            promptBuilder.append("1. 첫 번째 단계\n");
            promptBuilder.append("2. 두 번째 단계\n");
            promptBuilder.append("...\n\n");
            promptBuilder.append("**요리 팁과 주의사항**\n");
            promptBuilder.append("- 팁 또는 주의사항\n");
            promptBuilder.append("- 팁 또는 주의사항\n\n");
            promptBuilder.append("**완성된 요리의 이미지 URL**\n");
            promptBuilder.append("- 실제 이미지 URL\n\n");
            
            // 5. 주의사항 추가
            promptBuilder.append("주의사항:\n");
            promptBuilder.append("- 모든 재료의 양과 단위를 정확히 명시해주세요.\n");
            promptBuilder.append("- 조리 방법은 반드시 포함해야 하며, 순서대로 상세히 설명해주세요.\n");
            promptBuilder.append("- 요리 팁과 주의사항은 실제 조리 시 도움이 될 수 있는 내용을 포함해주세요.\n");
            promptBuilder.append("- 요리 이미지 URL은 실제 존재하는 이미지의 URL을 제공해주세요.\n");
            promptBuilder.append("- 난이도는 반드시 '상', '중', '하' 중 하나로만 표기해주세요.\n");
            promptBuilder.append("- 각 섹션은 정확히 위의 형식을 따라주세요.\n");
            promptBuilder.append("- 조리 방법 섹션을 절대 생략하지 마세요. 반드시 포함해주세요.\n");
            promptBuilder.append("- '상세한 조리 방법' 섹션이 누락되면 답변하지 마세요. 반드시 포함하세요.\n");
            promptBuilder.append("- 아래 예시처럼 모든 섹션이 빠짐없이 포함된 답변만 작성하세요.\n");
            
            promptBuilder.append("아래는 예시입니다. 반드시 이 형식을 따라 답변하세요.\n\n");
            promptBuilder.append("## 닭가슴살 감자볶음\n\n");
            promptBuilder.append("**필요한 재료와 양**\n");
            promptBuilder.append("- 닭가슴살: 200g\n");
            promptBuilder.append("- 감자: 2개(약 300g)\n");
            promptBuilder.append("- 양파: 1/2개\n");
            promptBuilder.append("- 대파: 1대\n");
            promptBuilder.append("- 다진 마늘: 1큰술\n");
            promptBuilder.append("- 식용유: 2큰술\n");
            promptBuilder.append("- 간장: 2큰술\n");
            promptBuilder.append("- 소금: 약간\n");
            promptBuilder.append("- 후추: 약간\n\n");
            promptBuilder.append("**조리 시간**\n");
            promptBuilder.append("- 20분\n\n");
            promptBuilder.append("**난이도**\n");
            promptBuilder.append("- 중\n\n");
            promptBuilder.append("**상세한 조리 방법**\n");
            promptBuilder.append("1. 감자는 껍질을 벗기고 깍둑썰기 해주세요.\n");
            promptBuilder.append("2. 닭가슴살은 한 입 크기로 썰어 소금, 후추로 밑간합니다.\n");
            promptBuilder.append("3. 팬에 식용유를 두르고 다진 마늘, 대파를 볶아 향을 냅니다.\n");
            promptBuilder.append("4. 닭가슴살을 넣고 익을 때까지 볶습니다.\n");
            promptBuilder.append("5. 감자, 양파를 넣고 5분간 볶은 뒤 간장으로 간을 맞춥니다.\n");
            promptBuilder.append("6. 감자가 익을 때까지 중불에서 볶아 완성합니다.\n\n");
            promptBuilder.append("**요리 팁과 주의사항**\n");
            promptBuilder.append("- 감자는 미리 전자레인지에 3분간 돌리면 더 빨리 익힙니다.\n");
            promptBuilder.append("- 닭가슴살 대신 돼지고기나 소고기를 사용해도 좋습니다.\n\n");
            promptBuilder.append("**완성된 요리의 이미지 URL**\n");
            promptBuilder.append("- https://example.com/sample.jpg\n\n");
            
            return promptBuilder.toString();
            
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

            **필요한 재료와 양**
            - 재료명: 양 단위
            - 재료명: 양 단위
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
            - 실제 이미지 URL

            주의사항:
            - 모든 재료의 양과 단위를 정확히 명시해주세요.
            - 조리 방법은 반드시 포함해야 하며, 순서대로 상세히 설명해주세요.
            - 요리 팁과 주의사항은 실제 조리 시 도움이 될 수 있는 내용을 포함해주세요.
            - 요리 이미지 URL은 실제 존재하는 이미지의 URL을 제공해주세요.
            - 난이도는 반드시 '상', '중', '하' 중 하나로만 표기해주세요.
            - 각 섹션은 정확히 위의 형식을 따라주세요.
            - 조리 방법 섹션을 절대 생략하지 마세요. 반드시 포함해주세요.
            - '상세한 조리 방법' 섹션이 누락되면 답변하지 마세요. 반드시 포함하세요.
            - 아래 예시처럼 모든 섹션이 빠짐없이 포함된 답변만 작성하세요.
            """, request);
    }
} 