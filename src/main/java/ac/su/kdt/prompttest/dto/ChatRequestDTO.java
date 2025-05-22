package ac.su.kdt.prompttest.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRequestDTO {
    private Long userId;
    private String message;
    private String sessionId;  // 선택적 필드: 없으면 새로운 세션 생성
} 