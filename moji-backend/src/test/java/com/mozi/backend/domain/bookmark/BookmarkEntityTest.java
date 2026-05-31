package com.mozi.backend.domain.bookmark;

import com.mozi.backend.domain.bookmark.entity.Bookmark;
import com.mozi.backend.domain.bookmark.repository.BookmarkRepository;
import com.mozi.backend.domain.user.entity.User;
import com.mozi.backend.domain.user.repository.UserRepository;
import com.mozi.backend.domain.welfare.entity.WelfareCentral;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.repository.WelfareCommonRepository;
import com.mozi.backend.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Bookmark 엔티티의 저장/조회/중복방지/cascade 동작을 검증.
 *
 * 핵심 검증 포인트:
 * 1) 정상 저장 후 user_id로 페이지 조회
 * 2) 동일 (user, welfare) 중복 저장 시 UNIQUE 제약 위반
 * 3) existsBy* 메서드 정상 동작 (Phase 4 idempotent 처리 기반)
 * 4) User 삭제 시 Bookmark cascade 삭제 — ERD §4-2 (H) 핵심 검증
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class BookmarkEntityTest {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WelfareCommonRepository welfareCommonRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * 북마크 저장 후 user_id 기준 페이지 조회가 정상 회수되는지.
     */
    @Test
    void save_북마크저장_user기준조회_정상() {
        User user = userRepository.save(User.of("bm-test1@example.com", "hashed"));
        WelfareCommon welfare = welfareCommonRepository.save(
                WelfareCentral.builder().id("WLF11001").title("test-central").build());

        bookmarkRepository.save(Bookmark.of(user, welfare));
        em.flush();
        em.clear();

        Page<Bookmark> page = bookmarkRepository.findByUser_Id(user.getId(), PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getWelfareCommon().getId()).isEqualTo("WLF11001");
    }

    /**
     * 동일 (user, welfare) 조합 두 번 저장 시 복합 UNIQUE 제약 위반 확인.
     */
    @Test
    void 중복_같은조합_저장시_UniqueConstraint위반() {
        User user = userRepository.save(User.of("bm-test2@example.com", "hashed"));
        WelfareCommon welfare = welfareCommonRepository.save(
                WelfareCentral.builder().id("WLF11002").title("test-central").build());

        bookmarkRepository.save(Bookmark.of(user, welfare));
        em.flush();

        // 같은 (user, welfare) 두 번째 저장 시도 — IDENTITY 전략은 save() 호출 시 즉시 INSERT 실행
        // 되므로 UNIQUE 위반이 save() 단계에서 바로 발생.
        Bookmark duplicate = Bookmark.of(user, welfare);
        assertThatThrownBy(() -> bookmarkRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * existsBy* 메서드는 idempotent POST 처리에 사용되므로 정확해야 함.
     */
    @Test
    void existsByUserIdAndWelfareCommonId_true_false_검증() {
        User user = userRepository.save(User.of("bm-test3@example.com", "hashed"));
        WelfareCommon welfare = welfareCommonRepository.save(
                WelfareCentral.builder().id("WLF11003").title("test-central").build());

        assertThat(bookmarkRepository.existsByUser_IdAndWelfareCommon_Id(user.getId(), "WLF11003"))
                .isFalse();

        bookmarkRepository.save(Bookmark.of(user, welfare));
        em.flush();

        assertThat(bookmarkRepository.existsByUser_IdAndWelfareCommon_Id(user.getId(), "WLF11003"))
                .isTrue();
    }

    /**
     * ⭐ ERD §4-2 (H) 핵심 검증 — User 삭제 시 Bookmark도 cascade로 함께 삭제.
     *
     * User.@OneToMany(cascade = ALL, orphanRemoval = true)가 정상 동작하는지
     * 확인. 회원 탈퇴(Hard Delete) 시 사용자가 남긴 북마크가 자동 정리됨을 보장.
     */
    @Test
    void cascade_사용자삭제시_북마크자동삭제() {
        User user = userRepository.save(User.of("bm-cascade@example.com", "hashed"));
        WelfareCommon welfare = welfareCommonRepository.save(
                WelfareCentral.builder().id("WLF11004").title("test-central").build());
        bookmarkRepository.save(Bookmark.of(user, welfare));
        em.flush();
        em.clear();

        Long userId = user.getId();
        userRepository.deleteById(userId);
        em.flush();
        em.clear();

        // User cascade로 Bookmark도 사라져야 함
        Page<Bookmark> after = bookmarkRepository.findByUser_Id(userId, PageRequest.of(0, 10));
        assertThat(after.getContent()).isEmpty();
    }
}
// 이 테스트의 역할: 북마크 도메인의 핵심 비즈니스 규칙 검증.
// cascade 테스트는 ERD §4-2 (H) Hard Delete 정책의 안전성 보장 — 회귀 시 즉시 발견.
