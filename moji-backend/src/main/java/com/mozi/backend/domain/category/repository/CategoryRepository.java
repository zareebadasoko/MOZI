package com.mozi.backend.domain.category.repository;

import com.mozi.backend.domain.category.entity.Category;
import com.mozi.backend.domain.category.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Category 마스터 영속성 접근 인터페이스.
 *
 * 시드 어댑터(Phase 2-4)에서 한글 이름 기반 lookup이 필요하고,
 * GET /api/categories?type=THEME (Phase 4)에서 타입별 목록 조회가 필요하다.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 카테고리 코드로 조회.
     *
     * 코드 기반 직접 lookup이 필요할 때 (예: 디버깅, 관리 도구).
     *
     * @param code 카테고리 코드 (예: "THM001", "STS003")
     * @return 일치하는 Category (없으면 Optional.empty)
     */
    Optional<Category> findByCode(String code);

    /**
     * 한글 이름으로 카테고리 조회.
     *
     * Phase 2-4 시드 어댑터가 크롤링 데이터의 콤마 분리 텍스트
     * (예: "보호·돌봄,안전·위기")를 분해해 한 단어씩 본 메서드로 조회 →
     * 매칭되는 Category의 id를 WelfareCategory 매핑에 사용한다.
     *
     * @param name 카테고리 한글 이름 (예: "보호·돌봄")
     * @return 일치하는 Category (없으면 Optional.empty — 시드 어댑터는 WARN 로그)
     */
    Optional<Category> findByName(String name);

    /**
     * 타입별 카테고리 목록 조회.
     *
     * Phase 4 GET /api/categories?type=THEME 응답용.
     *
     * @param type THEME 또는 STATUS
     * @return 해당 타입의 모든 Category (정렬은 Phase 4에서 결정)
     */
    List<Category> findByType(CategoryType type);
}
// 이 인터페이스의 역할: Category 마스터의 단일 영속성 진입점.
// 시드 적재 후엔 read-heavy 패턴이라 캐시 도입(v2)도 검토 가능.
