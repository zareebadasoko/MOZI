package com.mozi.backend.domain.user.entity;

import com.mozi.backend.domain.bookmark.entity.Bookmark;
import com.mozi.backend.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 인증 정보를 담는 사용자 엔티티.
 *
 * 회원가입 시 생성되며 이메일/비밀번호/권한만 보관한다. 거주지·가구형태 같은
 * 맞춤 추천용 정보는 UserProfile로 분리해 1:1로 연결한다. 회원 탈퇴 시 cascade로
 * UserProfile/RefreshToken/Bookmark를 함께 삭제(Hard Delete) — ERD §4-2 (H) 정책.
 *
 * 테이블명을 "users"로 둔 이유: MySQL의 USER가 reserved keyword라 native query에서
 * 백틱이 필요해 안전하게 복수형으로 회피.
 */
@Entity
@Getter
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_email", columnNames = "email")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserProfile profile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Bookmark> bookmarks = new ArrayList<>();

    private User(String email, String hashedPassword, Role role) {
        this.email = email;
        this.password = hashedPassword;
        this.role = role;
    }

    /**
     * 신규 회원가입 시 사용하는 정적 팩토리.
     *
     * password는 반드시 BCrypt로 이미 해싱된 값을 받는다. 평문 비밀번호 누출을
     * 막기 위해 엔티티는 해싱 책임을 지지 않으며, 서비스 레이어가 해싱 후 호출.
     * role은 일반 사용자 기본값(USER)으로 자동 설정.
     *
     * @param email 가입 이메일 (UNIQUE 제약, 호출 전에 중복 검사 완료 가정)
     * @param hashedPassword BCrypt 해싱된 비밀번호 (평문 X)
     * @return role=USER, profile=null, refreshTokens=빈 리스트로 초기화된 User
     */
    public static User of(String email, String hashedPassword) {
        return new User(email, hashedPassword, Role.USER);
    }

    /**
     * 비밀번호를 변경한다 (Setter 회피).
     *
     * 호출자는 반드시 BCrypt로 해싱된 값을 전달해야 한다. 엔티티는 해싱
     * 책임을 지지 않으며 평문이 도메인 엔티티로 흘러들지 않게 가드 역할만 한다.
     * 트랜잭션 컨텍스트에서 호출 시 dirty checking이 UPDATE 쿼리를 자동 발행.
     *
     * @param hashedNewPassword BCrypt 해싱된 새 비밀번호 (평문 X)
     */
    public void changePassword(String hashedNewPassword) {
        this.password = hashedNewPassword;
    }
}
// 이 클래스의 역할: 인증 + 권한의 단일 진실 원천.
// UserProfile은 Lazy creation(첫 PUT 호출 시 생성)이라 가입 직후엔 null.
// Bookmark cascade 매핑까지 포함되어 ERD §4-2 (H) 정책 완성:
// 탈퇴 시 UserProfile/RefreshToken/Bookmark 모두 일괄 삭제.
// password 컬럼 길이가 100인 이유: BCrypt 해시 결과는 60자라 여유 두고 100으로.
