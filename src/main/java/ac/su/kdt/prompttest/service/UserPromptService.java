package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.entity.UserPrompt;
import ac.su.kdt.prompttest.repository.UserPromptRepository;
import ac.su.kdt.prompttest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPromptService {

    private final UserPromptRepository userPromptRepository;
    private final UserRepository userRepository;

    /**
     * 사용자의 프롬프트 정보를 조회합니다.
     * @param userId 사용자 ID
     * @return UserPrompt 객체
     */
    public UserPrompt getUserPrompt(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userPromptRepository.findByUser(user);
    }

    /**
     * 사용자의 프롬프트 정보를 조회합니다.
     * @param userId 사용자 ID
     * @return List of UserPrompt objects as Map
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserPrompts(Integer userId) {
        List<UserPrompt> userPrompts = userPromptRepository.findByUserId(userId);
        return userPrompts.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    /**
     * UserPrompt를 Map으로 변환합니다.
     * @param userPrompt UserPrompt 객체
     * @return Map representation
     */
    private Map<String, Object> convertToMap(UserPrompt userPrompt) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", userPrompt.getId());
        map.put("name", userPrompt.getName());
        map.put("age", userPrompt.getAge());
        map.put("gender", userPrompt.getGender());
        map.put("isPregnant", userPrompt.getIsPregnant());
        map.put("healthStatus", userPrompt.getHealthStatus());
        map.put("allergy", userPrompt.getAllergy());
        map.put("preference", userPrompt.getPreference());
        map.put("nickname", userPrompt.getNickname());
        map.put("createdAt", userPrompt.getCreatedAt() != null ? userPrompt.getCreatedAt().toString() : null);
        map.put("updatedAt", userPrompt.getUpdatedAt() != null ? userPrompt.getUpdatedAt().toString() : null);
        return map;
    }

    /**
     * 새로운 사용자 프롬프트 정보를 생성합니다.
     * @param userId 사용자 ID
     * @param userPrompt 생성할 프롬프트 정보
     * @return 생성된 UserPrompt 객체
     */
    @Transactional
    public UserPrompt createUserPrompt(Integer userId, UserPrompt userPrompt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userPrompt.setUser(user);
        return userPromptRepository.save(userPrompt);
    }

    /**
     * 사용자의 프롬프트 정보를 업데이트합니다.
     * @param userId 사용자 ID
     * @param promptId 프롬프트 ID
     * @param userPromptDetails 업데이트할 프롬프트 정보
     * @return 업데이트된 UserPrompt 객체
     */
    @Transactional
    public UserPrompt updateUserPrompt(Integer userId, Integer promptId, UserPrompt userPromptDetails) {
        UserPrompt existingPrompt = userPromptRepository.findById(promptId)
                .orElseThrow(() -> new RuntimeException("Prompt not found with id: " + promptId));

        if (!existingPrompt.getUser().getId().equals(userId)) {
            throw new RuntimeException("User not authorized to update this prompt");
        }

        existingPrompt.setName(userPromptDetails.getName());
        existingPrompt.setAge(userPromptDetails.getAge());
        existingPrompt.setGender(userPromptDetails.getGender());
        existingPrompt.setHealthStatus(userPromptDetails.getHealthStatus());
        existingPrompt.setAllergy(userPromptDetails.getAllergy());
        existingPrompt.setPreference(userPromptDetails.getPreference());
        existingPrompt.setIsPregnant(userPromptDetails.getIsPregnant());
        existingPrompt.setNickname(userPromptDetails.getNickname());

        return userPromptRepository.save(existingPrompt);
    }

    /**
     * 사용자의 프롬프트 정보를 삭제합니다.
     * @param userId 사용자 ID
     * @param promptId 프롬프트 ID
     */
    @Transactional
    public void deleteUserPrompt(Integer userId, Integer promptId) {
        UserPrompt prompt = userPromptRepository.findById(promptId)
                .orElseThrow(() -> new RuntimeException("Prompt not found with id: " + promptId));
        if (!prompt.getUser().getId().equals(userId)) {
            throw new RuntimeException("User not authorized to delete this prompt");
        }
        userPromptRepository.deleteById(promptId);
    }

    /**
     * 특정 필드만 업데이트합니다.
     * @param userId 사용자 ID
     * @param field 업데이트할 필드명
     * @param value 업데이트할 값
     * @return 업데이트된 UserPrompt 객체
     */
    @Transactional
    public UserPrompt updateUserPromptField(Integer userId, String field, Object value) {
        // This logic needs to be re-evaluated. Which prompt do we update if a user has many?
        // For now, updating the first one found. A promptId should probably be passed in.
        UserPrompt userPrompt = userPromptRepository.findByUserId(userId).stream().findFirst()
                .orElseThrow(() -> new RuntimeException("User prompt not found for user: " + userId));

        switch (field) {
            case "name":
                userPrompt.setName((String) value);
                break;
            case "age":
                userPrompt.setAge((Integer) value);
                break;
            case "gender":
                userPrompt.setGender((String) value);
                break;
            case "preference":
                userPrompt.setPreference((String) value);
                break;
            case "healthStatus":
                userPrompt.setHealthStatus((String) value);
                break;
            case "allergy":
                userPrompt.setAllergy((String) value);
                break;
            default:
                throw new IllegalArgumentException("Invalid field: " + field);
        }
        return userPromptRepository.save(userPrompt);
    }
} 