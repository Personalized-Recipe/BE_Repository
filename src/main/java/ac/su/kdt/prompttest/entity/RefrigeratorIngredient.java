package ac.su.kdt.prompttest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "refrigerator_ingredient",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"refrigerator_id", "ingredient_id", "unit"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefrigeratorIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refrigerator_id", nullable = false)
    @JsonIgnore
    private Refrigerator refrigerator;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;
    
    @Positive(message = "수량은 양수여야 합니다")
    @Column(name = "quantity", nullable = false)
    private Float quantity; // 수량
    
    @Column(name = "unit", length = 20)
    private String unit; // 단위 (g, kg, ml, L, 개, 장, 쪽, 큰술, 작은술, 컵)
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate; // 유통기한
    
    @Column(name = "purchase_date")
    private LocalDate purchaseDate; // 구매일
    
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_location", length = 20)
    private StorageLocation storageLocation; // 보관 위치
    
    @Enumerated(EnumType.STRING)
    @Column(name = "freshness_status", length = 20)
    private FreshnessStatus freshnessStatus; // 신선도 상태
    
    @Column(name = "notes", length = 500)
    private String notes; // 메모
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 보관 위치 열거형
    public enum StorageLocation {
        FREEZER("냉동실"),
        REFRIGERATOR("냉장실"),
        DOOR("문쪽"),
        CRISPER("채소칸"),
        MEAT_DRAWER("육류칸");
        
        private final String description;
        
        StorageLocation(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 신선도 상태 열거형
    public enum FreshnessStatus {
        EXCELLENT("매우 신선"),
        GOOD("신선"),
        FAIR("보통"),
        POOR("나쁨"),
        EXPIRED("만료됨");
        
        private final String description;
        
        FreshnessStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // 신선도 상태 자동 설정
        if (freshnessStatus == null) {
            freshnessStatus = FreshnessStatus.GOOD;
        }
        
        // 보관 위치 기본값 설정
        if (storageLocation == null) {
            storageLocation = StorageLocation.REFRIGERATOR;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        
        // 유통기한에 따른 신선도 상태 자동 업데이트
        if (expiryDate != null) {
            LocalDate today = LocalDate.now();
            if (today.isAfter(expiryDate)) {
                freshnessStatus = FreshnessStatus.EXPIRED;
            } else if (today.isAfter(expiryDate.minusDays(3))) {
                freshnessStatus = FreshnessStatus.POOR;
            } else if (today.isAfter(expiryDate.minusDays(7))) {
                freshnessStatus = FreshnessStatus.FAIR;
            }
        }
    }
} 