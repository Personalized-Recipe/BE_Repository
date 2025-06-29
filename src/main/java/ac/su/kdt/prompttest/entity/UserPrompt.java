package ac.su.kdt.prompttest.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_prompt")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserPrompt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer promptId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;
    
    @Column(nullable = false, length = 10)
    private String name;
    
    @Min(value = 1, message = "나이는 양수여야 합니다")
    private Integer age;
    
    @Pattern(regexp = "^(M|F)$", message = "성별은 'M' 또는 'F'여야 합니다")
    @Column(length = 2)
    private String gender;
    
    @Column(name = "is_pregnant")
    private Boolean isPregnant;
    
    @Column(name = "health_status", length = 50)
    private String healthStatus;
    
    @Column(length = 50)
    private String allergy;
    
    @Column(length = 100)
    private String preference;
    
    @Column(length = 50)
    private String nickname;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        validatePregnancyStatus();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validatePregnancyStatus();
    }

    private void validatePregnancyStatus() {
        if ("M".equals(gender)) {
            isPregnant = false;
        }
    }
} 