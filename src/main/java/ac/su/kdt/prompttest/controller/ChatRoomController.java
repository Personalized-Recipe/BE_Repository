package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.ChatRoomDTO;
import ac.su.kdt.prompttest.dto.ChatRoomRequestDTO;
import ac.su.kdt.prompttest.entity.ChatRoom;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {
    
    private final ChatRoomService chatRoomService;
    
    /**
     * 사용자의 모든 활성 채팅방 조회
     * @return 사용자의 채팅방 목록
     */
    @GetMapping
    public ResponseEntity<List<ChatRoomDTO>> getUserChatRooms() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            List<ChatRoom> chatRooms = chatRoomService.getUserChatRooms(user.getUserId());
            List<ChatRoomDTO> chatRoomDTOs = chatRooms.stream()
                    .map(ChatRoomDTO::from)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(chatRoomDTOs);
        } catch (Exception e) {
            log.error("채팅방 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 특정 채팅방 조회
     * @param chatRoomId 채팅방 ID
     * @return 채팅방 정보
     */
    @GetMapping("/{chatRoomId}")
    public ResponseEntity<ChatRoomDTO> getChatRoom(@PathVariable Integer chatRoomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            ChatRoom chatRoom = chatRoomService.getChatRoom(chatRoomId);
            
            // 본인의 채팅방만 조회 가능
            if (!chatRoom.getUserId().equals(user.getUserId())) {
                return ResponseEntity.badRequest().build();
            }
            
            return ResponseEntity.ok(ChatRoomDTO.from(chatRoom));
        } catch (Exception e) {
            log.error("채팅방 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 새로운 채팅방 생성
     * @param request 채팅방 생성 요청
     * @return 생성된 채팅방 정보
     */
    @PostMapping
    public ResponseEntity<ChatRoomDTO> createChatRoom(@RequestBody ChatRoomRequestDTO request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            ChatRoom chatRoom = chatRoomService.createChatRoom(user.getUserId(), request.getTitle());
            return ResponseEntity.ok(ChatRoomDTO.from(chatRoom));
        } catch (Exception e) {
            log.error("채팅방 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 채팅방 제목 수정
     * @param chatRoomId 채팅방 ID
     * @param request 수정할 제목 정보
     * @return 수정된 채팅방 정보
     */
    @PutMapping("/{chatRoomId}")
    public ResponseEntity<ChatRoomDTO> updateChatRoom(
            @PathVariable Integer chatRoomId,
            @RequestBody ChatRoomRequestDTO request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            // 본인의 채팅방인지 확인
            ChatRoom existingChatRoom = chatRoomService.getChatRoom(chatRoomId);
            if (!existingChatRoom.getUserId().equals(user.getUserId())) {
                return ResponseEntity.badRequest().build();
            }
            
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            ChatRoom updatedChatRoom = chatRoomService.updateChatRoom(chatRoomId, request.getTitle());
            return ResponseEntity.ok(ChatRoomDTO.from(updatedChatRoom));
        } catch (Exception e) {
            log.error("채팅방 수정 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 채팅방 비활성화 (삭제)
     * @param chatRoomId 채팅방 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{chatRoomId}")
    public ResponseEntity<Void> deactivateChatRoom(@PathVariable Integer chatRoomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            // 본인의 채팅방인지 확인
            ChatRoom existingChatRoom = chatRoomService.getChatRoom(chatRoomId);
            if (!existingChatRoom.getUserId().equals(user.getUserId())) {
                return ResponseEntity.badRequest().build();
            }
            
            chatRoomService.deactivateChatRoom(chatRoomId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("채팅방 비활성화 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
} 