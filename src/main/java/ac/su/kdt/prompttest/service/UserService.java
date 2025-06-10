package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.UserDTO;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public UserDTO registerUser(UserDTO userDTO) {
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(userDTO.getPassword());
        user.setName(userDTO.getName());
        user.setAge(userDTO.getAge());
        user.setGender(userDTO.getGender());
        user.setPreferences(userDTO.getPreferences());
        user.setHealthConditions(userDTO.getHealthConditions());
        user.setAllergies(userDTO.getAllergies());

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional
    public User updateUser(Integer id, UserDTO userDTO) {
        User user = getUserById(id);
        if (userDTO.getName() != null) user.setName(userDTO.getName());
        if (userDTO.getAge() != null) user.setAge(userDTO.getAge());
        if (userDTO.getGender() != null) user.setGender(userDTO.getGender());
        if (userDTO.getPreferences() != null) user.setPreferences(userDTO.getPreferences());
        if (userDTO.getHealthConditions() != null) user.setHealthConditions(userDTO.getHealthConditions());
        if (userDTO.getAllergies() != null) user.setAllergies(userDTO.getAllergies());
        return userRepository.save(user);
    }

    public User getCurrentUser() {
        // TODO: Implement proper authentication
        // For now, return a default user for testing
        return userRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Default user not found"));
    }

    @Transactional
    public User updateProfileImage(String profileImageUrl) {
        User user = getCurrentUser();
        user.setProfileImage(profileImageUrl);
        return userRepository.save(user);
    }

    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .age(user.getAge())
                .gender(user.getGender())
                .preferences(user.getPreferences())
                .healthConditions(user.getHealthConditions())
                .allergies(user.getAllergies())
                .build();
    }
} 