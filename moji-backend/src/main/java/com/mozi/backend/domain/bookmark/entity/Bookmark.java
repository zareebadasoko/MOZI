package com.mozi.backend.domain.bookmark.entity;

import com.mozi.backend.domain.user.entity.User;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.global.common.BaseEntity;
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

/**
 * 사용자의 복지 즐겨찾기를 나타내는 N:M 해소 엔티티.
 *
 * User ↔ WelfareCommon 관계의 매핑 테이블이며, WelfareCommon(부모) 기준으로
 * 매핑하므로 자식 종류(Central/Local/Private/Seoul)에 무관하게 일관된 방식으로
 * 북마크가 가능하다.
 *
 * 동일 사용자가 같은 복지를 두 번 북마크하지 못하도록 (user_id, welfare_id)
 * 복합 UNIQUE 제약을 둔다. 사용자별 북마크 페이지 조회가 잦아 user_id 인덱스
 * 부여. 회원 탈퇴 시 User 쪽 cascade 매핑으로 일괄 삭제됨 — ERD §4-2 (H).
 */
@Entity
@Getter
@Table(
        name = "bookmark",
        indexes = @Index(name = "idx_bookmark_user_id", columnList = "user_id"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bookmark_user_welfare",
                columnNames = {"user_id", "welfare_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "welfare_id", nullable = false)
    private WelfareCommon welfareCommon;

    private Bookmark(User user, WelfareCommon welfareCommon) {
        this.user = user;
        this.welfareCommon = welfareCommon;
    }

    /**
     * 신규 북마크 생성용 정적 팩토리.
     *
     * 호출 전에 동일 (user, welfare) 북마크 존재 여부를 서비스 레이어에서
     * existsByUser_IdAndWelfareCommon_Id로 확인하면 idempotent 처리가 가능.
     *
     * @param user 북마크하는 사용자 (영속화된 상태)
     * @param welfareCommon 북마크 대상 복지 (4개 자식 중 어느 종류든 OK)
     * @return id가 비어있는 새 Bookmark (save 후 PK 채워짐)
     */
    public static Bookmark of(User user, WelfareCommon welfareCommon) {
        return new Bookmark(user, welfareCommon);
    }
}
// 이 클래스의 역할: 사용자별 즐겨찾기의 단일 진실 원천.
// WelfareCommon 부모 기준 매핑이라 자식 종류와 무관하게 통일 처리 가능.
// 회원 탈퇴 시 User 엔티티의 cascade 매핑이 본 row를 자동 삭제 — DB ON DELETE CASCADE에 의존하지 않음.
