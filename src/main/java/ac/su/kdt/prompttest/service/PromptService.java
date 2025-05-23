package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.PromptTemplate;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.entity.UserPrompt;
import ac.su.kdt.prompttest.repository.PromptTemplateRepository;
import ac.su.kdt.prompttest.repository.UserPromptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptService {
    
    private final PromptTemplateRepository promptTemplateRepository;
    private final UserService userService;
    private final UserPromptRepository userPromptRepository;
    
    public String generatePrompt(Integer userId, String request) {
        User user = userService.getUserById(userId);
        UserPrompt userPrompt = userPromptRepository.findByUserId(userId);
        PromptTemplate template = promptTemplateRepository.findByIsActive(true)
                .orElseGet(() -> createDefaultTemplate());
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("당신은 레시피 추천 전문가입니다. 사용자의 요청에 맞는 레시피를 추천해주세요.\n\n");
        
        if (userPrompt != null) {
            // 사용자 기본 정보
            promptBuilder.append("## 사용자 정보\n");
            if (userPrompt.getAge() != null) {
                promptBuilder.append(String.format("- 연령: %d세\n", userPrompt.getAge()));
            }
            if (userPrompt.getGender() != null) {
                promptBuilder.append(String.format("- 성별: %s\n", userPrompt.getGender()));
            }
            
            // 건강 상태
            promptBuilder.append("\n## 건강 정보\n");
            if (userPrompt.getIsPregnant() != null && userPrompt.getIsPregnant()) {
                promptBuilder.append("- 임신 중\n");
            }
            if (userPrompt.getHealthStatus() != null) {
                promptBuilder.append(String.format("- 건강 상태: %s\n", userPrompt.getHealthStatus()));
            }
            
            // 식이 제한사항
            if (userPrompt.getAllergy() != null) {
                promptBuilder.append("\n## 식이 제한사항\n");
                promptBuilder.append(String.format("- 알레르기: %s\n", userPrompt.getAllergy()));
            }
            
            // 선호도
            if (userPrompt.getPreference() != null) {
                promptBuilder.append("\n## 선호도\n");
                promptBuilder.append(String.format("- %s\n", userPrompt.getPreference()));
            }
        }
        
        // 사용자 요청
        promptBuilder.append("\n## 사용자 요청\n");
        promptBuilder.append(request);
        
        // 상세한 지시사항 추가
        promptBuilder.append("\n\n위 요청에 맞는 레시피를 다음 형식으로 제공해주세요:\n");
        promptBuilder.append("1. 요리 이름: [요리 이름]\n");
        promptBuilder.append("2. 필요한 재료와 양:\n");
        promptBuilder.append("   - [재료1]: [양] [단위]\n");
        promptBuilder.append("   - [재료2]: [양] [단위]\n");
        promptBuilder.append("   ...\n");
        promptBuilder.append("3. 조리 시간: [시간]분\n");
        promptBuilder.append("4. 난이도: [상/중/하]\n");
        promptBuilder.append("5. 조리 방법:\n");
        promptBuilder.append("   [1] [첫 번째 단계]\n");
        promptBuilder.append("   [2] [두 번째 단계]\n");
        promptBuilder.append("   [3] [세 번째 단계]\n");
        promptBuilder.append("   ... (필요한 만큼 단계 추가)\n");
        promptBuilder.append("6. 요리 팁과 주의사항:\n");
        promptBuilder.append("   - [팁1]\n");
        promptBuilder.append("   - [팁2]\n");
        promptBuilder.append("   ...\n");
        promptBuilder.append("7. 요리 이미지 URL: [완성된 요리의 이미지 URL]\n\n");
        promptBuilder.append("주의사항:\n");
        promptBuilder.append("- 모든 재료의 양과 단위를 정확히 명시해주세요.\n");
        promptBuilder.append("- 조리 방법은 번호를 매겨 순서대로 상세히 설명해주세요. 단계 수는 제한이 없습니다.\n");
        promptBuilder.append("- 요리 팁과 주의사항은 실제 조리 시 도움이 될 수 있는 내용을 포함해주세요.\n");
        promptBuilder.append("- 요리 이미지 URL은 실제 존재하는 이미지의 URL을 제공해주세요.\n");
        
        return promptBuilder.toString();
    }
    
    private PromptTemplate createDefaultTemplate() {
        PromptTemplate template = PromptTemplate.builder()
                .template("당신은 레시피 추천 전문가입니다. 사용자의 요청에 맞는 레시피를 추천해주세요.")
                .isActive(true)
                .build();
        return promptTemplateRepository.save(template);
    }
} 