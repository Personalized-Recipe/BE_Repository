package ac.su.kdt.prompttest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHistoryDTO {
    private Integer chatId;
    private Integer userId;
    private String message;
    private boolean isUserMessage;
    private String sessionId;
    private Integer recipeId;
    private LocalDateTime createdAt;
    private Integer roomId;
    private String roomName;
} 