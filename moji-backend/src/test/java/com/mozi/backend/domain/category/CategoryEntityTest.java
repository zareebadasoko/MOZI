package com.mozi.backend.domain.category;

import com.mozi.backend.domain.category.entity.Category;
import com.mozi.backend.domain.category.entity.CategoryType;
import com.mozi.backend.domain.category.entity.WelfareCategory;
import com.mozi.backend.domain.category.repository.CategoryRepository;
import com.mozi.backend.domain.category.repository.WelfareCategoryRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Category + WelfareCategory 매핑 도메인의 영속성 동작 검증.
 *
 * 핵심 검증 포인트:
 * 1) Category 저장 후 code/name/type별 lookup 정상 동작
 * 2) WelfareCategory 매핑 저장 후 welfare_id 기준 조회
 * 3) 동일 (welfare, category) 조합 중복 저장 시 UNIQUE 제약 위반
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class CategoryEntityTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WelfareCategoryRepository welfareCategoryRepository;

    @Autowired
    private WelfareCommonRepository welfareCommonRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * Category 저장 후 code 기반 lookup이 정상 동작하는지.
     */
    @Test
    void findByCode_정상() {
        categoryRepository.save(Category.of("THM999", "테스트주제", CategoryType.THEME));
        em.flush();
        em.clear();

        assertThat(categoryRepository.findByCode("THM999")).isPresent()
                .get()
                .satisfies(c -> {
                    assertThat(c.getName()).isEqualTo("테스트주제");
                    assertThat(c.getType()).isEqualTo(CategoryType.THEME);
                });
    }

    /**
     * 시드 어댑터의 한글 이름 lookup 패턴 검증.
     *
     * Phase 2-4 어댑터가 "보호·돌봄,안전·위기" 콤마 분리 텍스트를 분해해
     * 각 단어를 findByName으로 조회하여 Category id를 획득.
     */
    @Test
    void findByName_한글이름_정상() {
        categoryRepository.save(Category.of("STS999", "테스트상황", CategoryType.STATUS));
        em.flush();
        em.clear();

        assertThat(categoryRepository.findByName("테스트상황")).isPresent()
                .get()
                .satisfies(c -> assertThat(c.getCode()).isEqualTo("STS999"));
    }

    /**
     * THEME / STATUS 섞어 저장 후 type 필터링이 정확한지.
     */
    @Test
    void findByType_THEME만필터링() {
        categoryRepository.save(Category.of("THM998", "테마A", CategoryType.THEME));
        categoryRepository.save(Category.of("THM997", "테마B", CategoryType.THEME));
        categoryRepository.save(Category.of("STS998", "상황A", CategoryType.STATUS));
        em.flush();

        List<Category> themes = categoryRepository.findByType(CategoryType.THEME);
        assertThat(themes).extracting(Category::getCode)
                .contains("THM998", "THM997")
                .doesNotContain("STS998");
    }

    /**
     * WelfareCategory 매핑 저장 후 welfare_id 기준 조회.
     */
    @Test
    void welfareCategory_매핑저장_조회() {
        WelfareCommon welfare = welfareCommonRepository.save(
                WelfareCentral.builder().id("WLF22001").title("test-central").build());
        Category cat = categoryRepository.save(Category.of("THM996", "주제Z", CategoryType.THEME));
        welfareCategoryRepository.save(WelfareCategory.of(welfare, cat));
        em.flush();
        em.clear();

        List<WelfareCategory> mappings =
                welfareCategoryRepository.findByWelfareCommon_Id("WLF22001");
        assertThat(mappings).hasSize(1);
        assertThat(mappings.get(0).getCategory().getCode()).isEqualTo("THM996");
    }

    /**
     * 동일 (welfare, category) 조합 두 번 저장 시 복합 UNIQUE 제약 위반.
     *
     * 시드 어댑터의 idempotent 보장의 근거가 되는 제약. 재실행해도
     * 중복 행이 쌓이지 않는다.
     */
    @Test
    void welfareCategory_복합UNIQUE_위반() {
        WelfareCommon welfare = welfareCommonRepository.save(
                WelfareCentral.builder().id("WLF22002").title("test-central").build());
        Category cat = categoryRepository.save(Category.of("THM995", "주제Y", CategoryType.THEME));

        welfareCategoryRepository.save(WelfareCategory.of(welfare, cat));
        em.flush();

        // IDENTITY 전략은 save() 호출 시 즉시 INSERT 실행되어 UNIQUE 위반이 바로 발생.
        WelfareCategory duplicate = WelfareCategory.of(welfare, cat);
        assertThatThrownBy(() -> welfareCategoryRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
// 이 테스트의 역할: 카테고리 도메인의 영속성 + 제약 동작 검증.
// findByName 패턴은 Phase 2-4 시드 어댑터의 핵심 lookup 경로라 회귀 방지에 중요.
