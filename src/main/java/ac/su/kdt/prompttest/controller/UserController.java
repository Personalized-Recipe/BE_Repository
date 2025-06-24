package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.UserDTO;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    /**
     * 사용자 프로필 정보 조회 (프로필 수정 페이지용)
     * @param userId 사용자 ID
     * @return 사용자 프로필 정보 (nickname, profileImage 포함)
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable Integer userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserDTO.from(user));
    }
    
    /**
     * 현재 로그인한 사용자 정보 조회
     * @return 현재 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        log.info("=== /api/users/me GET 요청 수신 ===");
        try {
            User user = userService.getCurrentUser();
            log.info("현재 사용자 조회 성공: userId={}, username={}", user.getUserId(), user.getUsername());
            return ResponseEntity.ok(UserDTO.from(user));
        } catch (Exception e) {
            log.error("현재 사용자 조회 실패: {}", e.getMessage(), e);
            
            // 임시로 테스트용 사용자 반환 (개발 중에만 사용)
            log.warn("인증 실패로 인해 테스트용 사용자를 반환합니다.");
            User testUser = userService.getUserById(2); // user_id=2인 사용자 반환
            return ResponseEntity.ok(UserDTO.from(testUser));
        }
    }
    
    /**
     * 특정 사용자 정보 조회
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Integer userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserDTO.from(user));
    }
    
    /**
     * 사용자 프로필 업데이트
     * @param userId 사용자 ID
     * @param userDTO 업데이트할 사용자 정보
     * @return 업데이트된 사용자 정보
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserDTO> updateProfile(@PathVariable Integer userId, @RequestBody UserDTO userDTO) {
        User user = userService.updateUser(userId, userDTO);
        return ResponseEntity.ok(UserDTO.from(user));
    }

    /**
     * 사용자 프로필 이미지 업데이트
     * @param profileImageUrl 새로운 프로필 이미지 URL
     * @return 업데이트된 사용자 정보
     */
    @PutMapping("/me/profile-image")
    public ResponseEntity<UserDTO> updateProfileImage(@RequestBody String profileImageUrl) {
        User user = userService.updateProfileImage(profileImageUrl);
        return ResponseEntity.ok(UserDTO.from(user));
    }
}