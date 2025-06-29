package ac.su.kdt.prompttest.dto;

import ac.su.kdt.prompttest.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDTO {
    private Integer chatRoomId;
    private Integer userId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
    private Integer messageCount;
    
    public static ChatRoomDTO from(ChatRoom chatRoom) {
        return ChatRoomDTO.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .userId(chatRoom.getUserId())
                .title(chatRoom.getTitle())
                .createdAt(chatRoom.getCreatedAt())
                .updatedAt(chatRoom.getUpdatedAt())
                .isActive(chatRoom.getIsActive())
                .messageCount(chatRoom.getMessageCount())
                .build();
    }
} 