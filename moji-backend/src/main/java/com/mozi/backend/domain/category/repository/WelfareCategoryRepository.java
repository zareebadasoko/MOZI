package com.mozi.backend.domain.category.repository;

import com.mozi.backend.domain.category.entity.WelfareCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * WelfareCategory N:M 매핑 영속성 접근 인터페이스.
 *
 * 단일 복지의 카테고리 조회와, Phase 4 검색 응답의 N+1 회피를 위한 batch IN 조회를 함께 제공한다.
 * @EntityGraph로 category까지 함께 fetch해 응답 매핑 시 lazy 초기화로 인한 추가 쿼리를 막는다.
 */
public interface WelfareCategoryRepository extends JpaRepository<WelfareCategory, Long> {

    /**
     * 단일 복지에 매핑된 모든 카테고리 행 조회.
     *
     * Phase 4 상세 조회 API의 응답에서 categories[] 배열을 구성할 때 사용.
     * Category까지 EntityGraph로 함께 fetch.
     *
     * @param welfareId 복지 자연키 (WLF/BOK/SEL prefix VARCHAR(12))
     * @return 해당 복지에 매핑된 WelfareCategory 행들 (없으면 빈 리스트)
     */
    @EntityGraph(attributePaths = {"category"})
    List<WelfareCategory> findByWelfareCommon_Id(String welfareId);

    /**
     * 여러 복지의 카테고리 매핑을 한 번에 조회 (N+1 회피).
     *
     * GET /api/welfares 검색 응답을 만들 때 페이지의 welfare ID 목록을 한 번에 IN 쿼리로
     * 가져와 그루핑한다. category까지 EntityGraph로 함께 fetch해 후속 lazy 로딩을 막는다.
     *
     * @param welfareIds 복지 자연키 목록 (보통 페이지 size만큼 — 최대 50)
     * @return 해당 복지들에 매핑된 모든 WelfareCategory 행
     */
    @EntityGraph(attributePaths = {"category"})
    List<WelfareCategory> findByWelfareCommon_IdIn(Collection<String> welfareIds);
}
// 이 인터페이스의 역할: 복지-카테고리 매핑의 단일 영속성 진입점 + N+1 회피 batch lookup.
// EntityGraph로 category까지 즉시 fetch하므로 호출 측에서 추가 LAZY 초기화 비용 없음.
