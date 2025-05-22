package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.PromptTemplate;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.repository.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptService {
    
    private final PromptTemplateRepository promptTemplateRepository;
    private final UserService userService;
    
    public String generatePrompt(Integer userId, String request) {
        User user = userService.getUserById(userId);
        PromptTemplate template = promptTemplateRepository.findByIsActive(true)
                .orElseGet(() -> createDefaultTemplate());
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("당신은 레시피 추천 전문가입니다. 다음과 같은 사용자의 정보와 요구사항을 고려하여 답변해주세요:\n\n");
        
        // 사용자 기본 정보
        promptBuilder.append("## 사용자 정보\n");
        promptBuilder.append(String.format("- 연령: %d세\n", user.getAge()));
        promptBuilder.append(String.format("- 성별: %s\n", user.getGender()));
        
        // 건강 상태
        promptBuilder.append("\n## 건강 정보\n");
        if (user.getIsPregnant() != null && user.getIsPregnant()) {
            promptBuilder.append("- 임신 중\n");
        }
        if (user.getHealthConditions() != null && !user.getHealthConditions().isEmpty()) {
            promptBuilder.append(String.format("- 건강 상태: %s\n", user.getHealthConditions()));
        }
        
        // 식이 제한사항
        if (user.getAllergies() != null && !user.getAllergies().isEmpty()) {
            promptBuilder.append("\n## 식이 제한사항\n");
            promptBuilder.append(String.format("- 알레르기: %s\n", user.getAllergies()));
        }
        
        // 선호도
        if (user.getPreferences() != null && !user.getPreferences().isEmpty()) {
            promptBuilder.append("\n## 선호도\n");
            promptBuilder.append(String.format("- %s\n", user.getPreferences()));
        }
        
        // 사용자 요청
        promptBuilder.append("\n## 사용자 요청\n");
        promptBuilder.append(request).append("\n");
        
        // 응답 형식 지정
        promptBuilder.append("\n## 응답 형식\n");
        promptBuilder.append("다음 형식으로 레시피를 제공해주세요:\n");
        promptBuilder.append("1. 요리 이름\n");
        promptBuilder.append("2. 필요한 재료와 정확한 양\n");
        promptBuilder.append("3. 예상 조리 시간\n");
        promptBuilder.append("4. 난이도 (상/중/하)\n");
        promptBuilder.append("5. 영양 정보\n");
        promptBuilder.append("6. 단계별 조리 방법\n");
        promptBuilder.append("7. 주의사항 및 팁\n");
        
        // 추가 지침
        promptBuilder.append("\n## 추가 고려사항\n");
        promptBuilder.append("1. 사용자의 건강 상태와 식이 제한을 반드시 고려해주세요.\n");
        promptBuilder.append("2. 가능한 경우 건강에 좋은 대체 재료를 제안해주세요.\n");
        promptBuilder.append("3. 초보자도 이해하기 쉽게 설명해주세요.\n");
        promptBuilder.append("4. 조리 과정에서 주의해야 할 점을 상세히 설명해주세요.\n");
        
        return promptBuilder.toString();
    }
    
    private PromptTemplate createDefaultTemplate() {
        PromptTemplate template = PromptTemplate.builder()
                .template("기본 템플릿")
                .isActive(true)
                .build();
        return promptTemplateRepository.save(template);
    }
} 