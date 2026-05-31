package com.mozi.backend.domain.user.entity;

import com.mozi.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refresh Token Rotation 정책을 강제하기 위한 토큰 엔티티.
 *
 * 매 /api/auth/refresh 호출 시 이전 row 삭제 + 새 row 발급(Rotation),
 * 비밀번호 변경/로그아웃/회원 탈퇴 시 해당 user의 모든 row 일괄 삭제로
 * 다중 기기 강제 무효화. 토큰 raw 값은 DB에 저장하지 않고 SHA-256 hash만
 * 보관하므로 DB 탈취 시에도 토큰 자체는 노출되지 않는다.
 *
 * BaseEntity를 상속해 createdAt이 자동 채워지며, updatedAt은 의미상 사용되지
 * 않지만 일관성을 위해 그대로 둔다.
 */
@Entity
@Getter
@Table(
        name = "refresh_token",
        indexes = @Index(name = "idx_refresh_token_user_id", columnList = "user_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token_token_hash", columnNames = "token_hash")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    private RefreshToken(User user, String tokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    /**
     * 새 refresh 토큰 발급 시 사용하는 정적 팩토리.
     *
     * 서비스 레이어가 raw 토큰을 SHA-256으로 해싱한 결과(tokenHash)와 만료 시각을
     * 계산해 전달한다. raw 값은 클라이언트 응답으로만 1회 노출되고 DB에 저장 X.
     *
     * @param user 토큰 소유자 (이미 영속화된 User)
     * @param tokenHash SHA-256 해시된 토큰 문자열 (hex 64자)
     * @param expiresAt 토큰 만료 시각 (보통 발급 시점 + 7일)
     * @return 발급 직후 상태의 RefreshToken (createdAt은 영속 시 자동 주입)
     */
    public static RefreshToken issue(User user, String tokenHash, LocalDateTime expiresAt) {
        return new RefreshToken(user, tokenHash, expiresAt);
    }

    /**
     * 만료 여부 확인.
     *
     * 호출자가 시간을 주입하므로 단위 테스트에서 임의 시각으로 검증 가능.
     *
     * @param now 비교 기준 시각 (보통 LocalDateTime.now())
     * @return now가 expiresAt 이후면 true
     */
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }
}
// 이 클래스의 역할: Refresh Token Rotation의 서버 측 상태 저장소.
// Rotation 강제 메커니즘: refresh API에서 이 row를 검증 → 통과 시 row 삭제 + 새 row 생성.
// 다중 기기 무효화: deleteAllByUser_Id로 한 번에 정리.
// 만료된 row는 MVP에선 누적 허용(Phase 3 plan 결정), v2에서 스케줄러 정리 검토.
