package ac.su.kdt.prompttest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "User", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "provider_Id"}, name = "uk_provider_provider_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = true, length = 100)
    private String password;

    @Column(length = 20)
    private String provider;

    @Column(name = "provider_id", length = 50)
    private String providerId;

    @Column(length = 100)
    private String email;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user")
    private List<UserIngredient> userIngredients;

    // 권한 목록 (기본값: ROLE_USER)
    @ElementCollection(fetch = FetchType.EAGER)
    @Builder.Default
    private List<String> roles = Collections.singletonList("ROLE_USER");

    // 생성/수정 시각 자동 설정
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== UserDetails 인터페이스 구현 =====

    /**
     * 사용자의 권한 목록 반환
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    /**
     * 사용자명 반환 (로그인 ID)
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 비밀번호 반환
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * 계정 만료 여부 (true: 만료 안 됨)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 계정 잠김 여부 (true: 잠기지 않음)
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 비밀번호 만료 여부 (true: 만료 안 됨)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 계정 활성화 여부 (true: 활성화)
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
