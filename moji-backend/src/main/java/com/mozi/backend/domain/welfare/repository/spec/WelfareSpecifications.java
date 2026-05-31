package com.mozi.backend.domain.welfare.repository.spec;

import com.mozi.backend.domain.category.entity.WelfareCategory;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.entity.WelfareLocal;
import com.mozi.backend.domain.welfare.entity.WelfareType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

/**
 * Welfare 동적 검색용 JPA Specification 모음.
 *
 * 각 정적 메서드는 입력값이 없거나 비어 있으면 `cb.conjunction()`(항상 true)을 반환해
 * `Specification.where(s1).and(s2)...` 체인에서 자동으로 무시되도록 한다.
 * 이렇게 하면 호출 측 서비스 코드는 모든 spec을 일관되게 and로 연결할 수 있고
 * "조건이 있을 때만 spec 추가" 같은 분기 로직이 필요 없어진다.
 *
 * 모든 spec은 WelfareCommon 부모 기준이며 자식 컬럼에 접근해야 하는 region 같은 경우
 * `cb.treat()`로 자식 타입으로 다운캐스트해 컬럼 참조를 가능케 한다.
 */
public final class WelfareSpecifications {

    private WelfareSpecifications() {
        // 인스턴스화 방지
    }

    /**
     * 키워드 검색 — title 또는 summary에 LIKE %keyword% 매칭.
     *
     * targetAudience는 의도적으로 제외 (API_SPEC 명시). 검색 의미를 명확히 하고
     * 응답 노이즈를 줄이기 위함이며, 사용자가 "노인" 키워드로 호출했을 때
     * targetAudience에 "노인" 포함된 모든 row가 매칭되면 결과가 너무 광범위해진다.
     *
     * @param keyword null/blank이면 무시
     * @return 키워드 매칭 spec
     */
    public static Specification<WelfareCommon> byKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + keyword.trim() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(root.get("title"), pattern),
                cb.like(root.get("summary"), pattern)
        );
    }

    /**
     * 출처 유형(welfare_type) 필터.
     *
     * @param type null이면 무시 (전체 출처 검색)
     * @return welfareType equals spec
     */
    public static Specification<WelfareCommon> byWelfareType(WelfareType type) {
        if (type == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("welfareType"), type);
    }

    /**
     * 카테고리 코드 IN 필터 (단일 또는 여러).
     *
     * 서브쿼리로 WelfareCategory 테이블을 조회해 (해당 코드들에 매핑된 welfare_id) IN 절을 생성한다.
     * Step 6 이후엔 사용자가 명시한 query param category(단일)만 입력으로 들어온다 (프로필 자동 매핑 제거).
     *
     * @param codes null/empty면 무시
     * @return 카테고리 매칭 spec (welfare.id IN 서브쿼리)
     */
    public static Specification<WelfareCommon> byCategoryCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            // welfare.id IN (SELECT wc.welfare_id FROM WelfareCategory wc WHERE wc.category.code IN :codes)
            assert query != null;
            Subquery<String> sub = query.subquery(String.class);
            Root<WelfareCategory> wc = sub.from(WelfareCategory.class);
            sub.select(wc.get("welfareCommon").get("id"))
                    .where(wc.get("category").get("code").in(codes));
            return root.get("id").in(sub);
        };
    }

    /**
     * 지역명 필터 — 출처별로 다른 매칭 정책 적용.
     *
     * 노년층 사용자가 "서울" 같은 지역 입력 시 자치구 사업뿐 아니라 전국 단위 사업도
     * 함께 보여야 한다는 UX 결정에 따라 출처별로 분기:
     *  - **CENTRAL**: 전국 단위 정부 사업 → region 무관하게 항상 통과
     *  - **PRIVATE**: 민간 단체 사업 (전국 무관) → region 무관하게 항상 통과
     *  - **LOCAL**: regionName 컬럼 LIKE %region% 매칭하는 row만 통과
     *  - **SEOUL**: region 입력에 "서울"이 포함될 때만 모두 통과 (그 외 지역 입력 시 제외)
     *
     * SQL 의미적으로는 OR 조합:
     *   welfareType IN (CENTRAL, PRIVATE)
     *   OR (welfareType = LOCAL AND regionName LIKE :pattern)
     *   OR (welfareType = SEOUL AND :region.contains("서울"))
     *
     * `cb.treat()`로 LOCAL 자식 컬럼 접근. SEOUL 포함 여부는 자바 측에서 미리 판단해
     * boolean 상수로 spec에 박아 넣는다.
     *
     * @param region null/blank이면 무시
     * @return 출처별 분기 매칭 spec
     */
    public static Specification<WelfareCommon> byRegion(String region) {
        if (region == null || region.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String trimmed = region.trim();
        String pattern = "%" + trimmed + "%";
        // SEOUL 포함 여부는 입력 문자열에 "서울"이 들어가는지로 판단 (예: "서울특별시", "서울")
        boolean seoulIncluded = trimmed.contains("서울");

        return (root, query, cb) -> {
            // CENTRAL/PRIVATE는 region 무관하게 항상 통과
            Predicate centralOrPrivate = root.get("welfareType")
                    .in(WelfareType.CENTRAL, WelfareType.PRIVATE);
            // LOCAL은 자식의 regionName 매칭 (cb.treat로 다운캐스트)
            Root<WelfareLocal> localRoot = cb.treat(root, WelfareLocal.class);
            Predicate localMatched = cb.and(
                    cb.equal(root.get("welfareType"), WelfareType.LOCAL),
                    cb.like(localRoot.get("regionName"), pattern)
            );
            if (seoulIncluded) {
                // SEOUL은 모든 row가 서울특별시라 매칭 컬럼 없이 출처만 통과
                Predicate seoulPass = cb.equal(root.get("welfareType"), WelfareType.SEOUL);
                return cb.or(centralOrPrivate, localMatched, seoulPass);
            }
            return cb.or(centralOrPrivate, localMatched);
        };
    }
}
// 이 클래스의 역할: 동적 검색 조건을 재사용 가능한 정적 메서드로 분리.
// null/blank 입력에 대한 conjunction 처리로 호출 측 분기 로직을 제거.
// region 필터가 자식 컬럼이라는 특수성은 cb.treat()로 흡수해 호출 측은 알 필요가 없다.
