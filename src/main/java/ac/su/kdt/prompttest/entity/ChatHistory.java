package ac.su.kdt.prompttest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
// 첫 번째 인덱싱 : 특정 유저, 특정 세션의 대화기록 조회
// 두 번째 인덱싱 : 대화기록 생성 시간 순서대로 조회
@Table(name = "Chat_History", indexes = {
    @Index(name = "idx_chat_user_session", columnList = "user_id,session_id"),
    @Index(name = "idx_chat_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer chatId;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;
    
    @Column(name = "is_user_message", nullable = false)
    private boolean isUserMessage;
    
    @Column(name = "session_id", nullable = false, length = 50)
    private String sessionId;
    
    @Column(name = "recipe_id")
    private Integer recipeId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 