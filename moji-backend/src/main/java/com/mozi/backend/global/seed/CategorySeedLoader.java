package com.mozi.backend.global.seed;

import com.mozi.backend.domain.category.entity.Category;
import com.mozi.backend.domain.category.entity.CategoryType;
import com.mozi.backend.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Category 마스터 22행을 시드 적재하는 Loader.
 *
 * CATEGORY_REFERENCE.md §1, §2의 표를 그대로 코드로 반영. 시드 데이터는
 * 외부 파일이 아닌 클래스 상수로 보관해, 변경 시 코드 리뷰 + git history로
 * 추적 가능하게 한다. category 테이블이 비어있을 때만 적재 (idempotent).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategorySeedLoader {

    private final CategoryRepository categoryRepository;

    /**
     * THEME 15종 + STATUS 7종 = 총 22행.
     * 코드 명명 규칙: THM001~THM015 (THEME), STS001~STS007 (STATUS).
     */
    private static final List<CategorySeed> SEEDS = List.of(
            // THEME (관심 주제) — 15종
            new CategorySeed("THM001", "서민금융", CategoryType.THEME),
            new CategorySeed("THM002", "안전·위기", CategoryType.THEME),
            new CategorySeed("THM003", "신체건강", CategoryType.THEME),
            new CategorySeed("THM004", "임신·출산", CategoryType.THEME),
            new CategorySeed("THM005", "일자리", CategoryType.THEME),
            new CategorySeed("THM006", "주거", CategoryType.THEME),
            new CategorySeed("THM007", "교육", CategoryType.THEME),
            new CategorySeed("THM008", "에너지", CategoryType.THEME),
            new CategorySeed("THM009", "생활지원", CategoryType.THEME),
            new CategorySeed("THM010", "문화·여가", CategoryType.THEME),
            new CategorySeed("THM011", "정신건강", CategoryType.THEME),
            new CategorySeed("THM012", "보육", CategoryType.THEME),
            new CategorySeed("THM013", "입양·위탁", CategoryType.THEME),
            new CategorySeed("THM014", "보호·돌봄", CategoryType.THEME),
            new CategorySeed("THM015", "법률", CategoryType.THEME),
            // STATUS (가구 상황) — 7종
            new CategorySeed("STS001", "보훈대상자", CategoryType.STATUS),
            new CategorySeed("STS002", "저소득", CategoryType.STATUS),
            new CategorySeed("STS003", "장애인", CategoryType.STATUS),
            new CategorySeed("STS004", "한부모·조손", CategoryType.STATUS),
            new CategorySeed("STS005", "다자녀", CategoryType.STATUS),
            new CategorySeed("STS006", "다문화·탈북민", CategoryType.STATUS),
            new CategorySeed("STS007", "퇴직자", CategoryType.STATUS)
    );

    /**
     * Category 22행을 적재. 이미 적재된 상태라면 skip.
     *
     * @Transactional로 22행을 한 트랜잭션에 묶어 모두 성공 또는 모두 롤백.
     */
    @Transactional
    public void loadIfEmpty() {
        long existing = categoryRepository.count();
        if (existing > 0) {
            log.info("Category seed skip — already loaded ({} rows)", existing);
            return;
        }

        List<Category> entities = SEEDS.stream()
                .map(s -> Category.of(s.code(), s.name(), s.type()))
                .toList();
        categoryRepository.saveAll(entities);
        log.info("Category seeded: {} rows (THEME 15 + STATUS 7)", entities.size());
    }

    /**
     * 시드 정의용 내부 record. 외부 노출 X.
     */
    private record CategorySeed(String code, String name, CategoryType type) {}
}
// 이 클래스의 역할: Category 마스터의 1회성 적재.
// 22행이 작아 단일 트랜잭션 batch insert로 처리.
// 코드 변경 시(예: 신규 카테고리 추가) DB truncate 후 재실행하면 반영됨.
