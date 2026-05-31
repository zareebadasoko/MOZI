package com.mozi.backend.global.seed;

import com.mozi.backend.domain.category.entity.Category;
import com.mozi.backend.domain.category.entity.WelfareCategory;
import com.mozi.backend.domain.category.repository.CategoryRepository;
import com.mozi.backend.domain.category.repository.WelfareCategoryRepository;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.repository.WelfareCommonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 4개 출처 어댑터를 조합해 Welfare 엔티티 + WelfareCategory 매핑을 한 번에 적재.
 *
 * 절차:
 * 1) welfare_common이 비어있는지 확인 (idempotent)
 * 2) Category 22행을 한 번에 메모리 캐시 (이름 → 엔티티 Map)
 * 3) 4개 어댑터에서 시드 row 리스트 수집
 * 4) 모든 엔티티 saveAll → 영속화 (batch INSERT)
 * 5) 각 row의 콤마 분리 카테고리 raw → 캐시 lookup → WelfareCategory 매핑 saveAll
 *
 * Category 매핑은 카테고리 이름 lookup 시 매번 SELECT를 피하기 위해 Map 캐시.
 * 매칭 실패 코드는 WARN 로그만 남기고 skip — 시드 데이터 무결성을 강제하기보다
 * 시연용 MVP의 유연성 우선.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfareSeedLoader {

    private final WelfareCommonRepository welfareCommonRepository;
    private final CategoryRepository categoryRepository;
    private final WelfareCategoryRepository welfareCategoryRepository;

    private final WelfareCentralAdapter centralAdapter;
    private final WelfareLocalAdapter localAdapter;
    private final WelfarePrivateAdapter privateAdapter;
    private final WelfareSeoulAdapter seoulAdapter;

    /**
     * Welfare 엔티티 4종 + WelfareCategory 매핑 적재.
     *
     * welfare_common 테이블이 비어있을 때만 실행. 한 트랜잭션으로 묶어
     * 일부만 저장된 부분 실패 상태를 방지.
     */
    @Transactional
    public void loadIfEmpty() {
        long existing = welfareCommonRepository.count();
        if (existing > 0) {
            log.info("Welfare seed skip — already loaded ({} rows)", existing);
            return;
        }

        Map<String, Category> categoryByName = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getName, Function.identity()));
        log.info("Category cache loaded: {} entries", categoryByName.size());

        List<WelfareSeedRow> all = new ArrayList<>();
        all.addAll(centralAdapter.parse());
        all.addAll(localAdapter.parse());
        all.addAll(privateAdapter.parse());
        all.addAll(seoulAdapter.parse());
        log.info("Total welfare rows to load: {}", all.size());

        // 1단계: 모든 부모/자식 엔티티 영속화 (JOINED 상속 매핑 → 부모+자식 JOIN INSERT)
        List<WelfareCommon> entities = all.stream()
                .map(WelfareSeedRow::entity)
                .toList();
        welfareCommonRepository.saveAll(entities);

        // 2단계: 각 row의 카테고리 raw → 매핑 행 생성
        List<WelfareCategory> mappings = new ArrayList<>();
        for (WelfareSeedRow row : all) {
            collectMappings(row.entity(), row.interestThemeRaw(), categoryByName, mappings);
            collectMappings(row.entity(), row.householdStatusRaw(), categoryByName, mappings);
        }
        welfareCategoryRepository.saveAll(mappings);
        log.info("Welfare seed done — entities: {}, category mappings: {}", entities.size(), mappings.size());
    }

    /**
     * 콤마 분리 텍스트(예: "보호·돌봄,안전·위기")를 분해해 매핑 엔티티 리스트에 추가.
     *
     * 매칭 실패 케이스:
     * - raw가 null/blank → 무시
     * - 분해된 한 단어가 빈 문자열 → 무시
     * - Category 캐시에 없는 이름 → WARN 로그 후 skip (시드 무결성 깨지지 않음)
     *
     * @param welfare 매핑 대상 복지
     * @param raw 콤마 분리 카테고리 이름 텍스트
     * @param cache 이름 → Category 캐시
     * @param accumulator 매핑 엔티티를 누적할 리스트
     */
    private void collectMappings(WelfareCommon welfare, String raw,
                                 Map<String, Category> cache,
                                 List<WelfareCategory> accumulator) {
        if (raw == null || raw.isBlank()) return;
        for (String name : raw.split(",")) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) continue;
            Category cat = cache.get(trimmed);
            if (cat == null) {
                log.warn("Unknown category name '{}' for welfare {}", trimmed, welfare.getId());
                continue;
            }
            accumulator.add(WelfareCategory.of(welfare, cat));
        }
    }
}
// 이 클래스의 역할: 4개 출처 통합 적재의 마스터 + WelfareCategory 매핑.
// JOINED 전략 덕에 saveAll(parent list)이 자식 테이블 INSERT까지 자동으로 수행함.
// Category 캐시로 N+1 회피 — 4개 출처 합쳐 수천 row를 처리해도 SELECT는 22행 한 번뿐.
