package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.entity.UserPrompt;
import ac.su.kdt.prompttest.repository.UserPromptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

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
            promptBuilder.append("- 요리 이름\n");
            promptBuilder.append("- 필요한 재료와 양\n");
            promptBuilder.append("- 조리 시간\n");
            promptBuilder.append("- 난이도\n");
            promptBuilder.append("- 상세한 조리 방법\n");
            promptBuilder.append("- 요리 팁과 주의사항\n");
            promptBuilder.append("- 완성된 요리의 이미지 URL\n\n");
            
            // 5. 주의사항 추가
            promptBuilder.append("주의사항:\n");
            promptBuilder.append("- 모든 재료의 양과 단위를 정확히 명시해주세요.\n");
            promptBuilder.append("- 조리 방법은 순서대로 상세히 설명해주세요.\n");
            promptBuilder.append("- 요리 팁과 주의사항은 실제 조리 시 도움이 될 수 있는 내용을 포함해주세요.\n");
            promptBuilder.append("- 요리 이미지 URL은 실제 존재하는 이미지의 URL을 제공해주세요.\n");
            
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

            레시피를 알려주세요. 다음 정보를 포함해주세요:
            - 요리 이름
            - 필요한 재료와 양
            - 조리 시간
            - 난이도
            - 상세한 조리 방법
            - 요리 팁과 주의사항
            - 완성된 요리의 이미지 URL

            주의사항:
            - 모든 재료의 양과 단위를 정확히 명시해주세요.
            - 조리 방법은 순서대로 상세히 설명해주세요.
            - 요리 팁과 주의사항은 실제 조리 시 도움이 될 수 있는 내용을 포함해주세요.
            - 요리 이미지 URL은 실제 존재하는 이미지의 URL을 제공해주세요.
            """, request);
    }
} 