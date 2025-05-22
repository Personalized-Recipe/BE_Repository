package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.UserDTO;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}") 
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(UserDTO.from(user));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateProfile(@PathVariable Integer id, @RequestBody UserDTO userDTO) {
        User user = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(UserDTO.from(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(UserDTO.from(user));
    }

    @PutMapping("/me/profile-image")
    public ResponseEntity<UserDTO> updateProfileImage(@RequestBody String profileImageUrl) {
        User user = userService.updateProfileImage(profileImageUrl);
        return ResponseEntity.ok(UserDTO.from(user));
    }
}