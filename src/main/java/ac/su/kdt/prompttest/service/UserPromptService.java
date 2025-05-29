package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.entity.UserPrompt;
import ac.su.kdt.prompttest.repository.UserPromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPromptService {

    private final UserPromptRepository userPromptRepository;
    private final UserService userService;

    /**
     * 사용자의 프롬프트 정보를 조회합니다.
     * @param userId 사용자 ID
     * @return UserPrompt 객체
     */
    public UserPrompt getUserPrompt(Integer userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return userPromptRepository.findByUser(user);
    }

    /**
     * 새로운 사용자 프롬프트 정보를 생성합니다.
     * @param userId 사용자 ID
     * @param userPrompt 생성할 프롬프트 정보
     * @return 생성된 UserPrompt 객체
     */
    @Transactional
    public UserPrompt createUserPrompt(Integer userId, UserPrompt userPrompt) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // 이미 존재하는 프롬프트가 있는지 확인
        if (userPromptRepository.findByUser(user) != null) {
            throw new RuntimeException("User prompt already exists");
        }

        userPrompt.setUser(user);
        return userPromptRepository.save(userPrompt);
    }

    /**
     * 사용자의 프롬프트 정보를 업데이트합니다.
     * @param userId 사용자 ID
     * @param updatedPrompt 업데이트할 프롬프트 정보
     * @return 업데이트된 UserPrompt 객체
     */
    @Transactional
    public UserPrompt updateUserPrompt(Integer userId, UserPrompt updatedPrompt) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        UserPrompt existingPrompt = userPromptRepository.findByUser(user);
        if (existingPrompt == null) {
            throw new RuntimeException("User prompt not found");
        }

        // 필수 필드 업데이트
        existingPrompt.setName(updatedPrompt.getName());
        existingPrompt.setAge(updatedPrompt.getAge());
        existingPrompt.setGender(updatedPrompt.getGender());

        // 선택적 필드 업데이트
        existingPrompt.setNickname(updatedPrompt.getNickname());
        existingPrompt.setHealthStatus(updatedPrompt.getHealthStatus());
        existingPrompt.setAllergy(updatedPrompt.getAllergy());
        existingPrompt.setPreference(updatedPrompt.getPreference());
        existingPrompt.setIsPregnant(updatedPrompt.getIsPregnant());

        return userPromptRepository.save(existingPrompt);
    }

    /**
     * 사용자의 프롬프트 정보를 삭제합니다.
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteUserPrompt(Integer userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        UserPrompt userPrompt = userPromptRepository.findByUser(user);
        if (userPrompt == null) {
            throw new RuntimeException("User prompt not found");
        }

        userPromptRepository.delete(userPrompt);
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
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        UserPrompt userPrompt = userPromptRepository.findByUser(user);
        if (userPrompt == null) {
            throw new RuntimeException("User prompt not found");
        }

        switch (field) {
            case "nickname":
                userPrompt.setNickname((String) value);
                break;
            case "healthStatus":
                userPrompt.setHealthStatus((String) value);
                break;
            case "allergy":
                userPrompt.setAllergy((String) value);
                break;
            case "preference":
                userPrompt.setPreference((String) value);
                break;
            case "isPregnant":
                userPrompt.setIsPregnant((Boolean) value);
                break;
            default:
                throw new RuntimeException("Invalid field name: " + field);
        }

        return userPromptRepository.save(userPrompt);
    }
} 