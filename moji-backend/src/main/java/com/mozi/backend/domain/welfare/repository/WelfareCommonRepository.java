package com.mozi.backend.domain.welfare.repository;

import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.entity.WelfareType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Welfare 엔티티 영속성 접근 인터페이스 (부모 기준).
 *
 * 4개 자식 종류와 무관하게 통합 조회/페이지네이션을 지원하며, ID 타입은
 * 자연키 String(VARCHAR 12). 자식별 단독 Repository는 두지 않고, 출처별
 * 필터링은 본 인터페이스의 findByWelfareType으로 처리한다.
 *
 * 검색 API의 동적 조건(keyword + region + category 등) 조합 쿼리는
 * Specification으로 처리한다 (WelfareSpecifications 참조).
 */
public interface WelfareCommonRepository extends JpaRepository<WelfareCommon, String>, JpaSpecificationExecutor<WelfareCommon> {

    /**
     * 출처 유형(welfare_type)으로 필터링한 페이지 조회.
     *
     * 부모 테이블의 welfare_type 컬럼(INDEX 적용)을 기준으로 조회하므로
     * 4개 자식 테이블을 JOIN 하지 않는다 — 검색 응답에 필요한 공통 필드만 회수.
     * 자식별 상세 데이터가 필요하면 Phase 4의 상세 조회 API에서 별도 fetch.
     *
     * @param welfareType 조회 대상 출처 (CENTRAL/LOCAL/PRIVATE/SEOUL)
     * @param pageable 페이지/사이즈 (예: PageRequest.of(0, 10))
     * @return 해당 타입의 WelfareCommon 페이지
     */
    Page<WelfareCommon> findByWelfareType(WelfareType welfareType, Pageable pageable);
}
// 이 인터페이스의 역할: Welfare 통합 영속성 추상화.
// JpaRepository가 findById(String), save, deleteById, count 등 표준 메서드 자동 제공.
// 다형성 쿼리(JPQL "FROM WelfareCommon w") 사용 시 Hibernate가 4개 자식 테이블을
// 적절히 JOIN 또는 UNION으로 통합 조회한다.
