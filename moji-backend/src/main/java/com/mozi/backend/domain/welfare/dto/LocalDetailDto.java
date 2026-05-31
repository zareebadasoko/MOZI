package com.mozi.backend.domain.welfare.dto;

import com.mozi.backend.domain.welfare.entity.WelfareLocal;

/**
 * 복지로 지자체 데이터 전용 detail 필드 DTO.
 *
 * WelfareDetailDto의 `localDetail` 필드에 들어가며, welfareType=LOCAL인 경우에만 채워진다.
 * regionName이 핵심이며 검색 API의 region 필터도 이 컬럼을 LIKE 매칭한다.
 *
 * @param regionName 지역명 (예: "경기도 의정부시")
 * @param supportYear 지원 연도 (nullable)
 * @param supportType 지원 유형
 * @param selectionCriteria 선정 기준 상세
 * @param supportDetails 지원 내용 상세
 * @param contact 문의처 (다중 부서 연결 케이스 대응 — VARCHAR 500)
 * @param detailUrl 상세 페이지 URL
 * @param supportCycle 지원 주기
 */
public record LocalDetailDto(
        String regionName,
        Integer supportYear,
        String supportType,
        String selectionCriteria,
        String supportDetails,
        String contact,
        String detailUrl,
        String supportCycle
) {

    /**
     * WelfareLocal 엔티티 → detail DTO 변환.
     */
    public static LocalDetailDto from(WelfareLocal l) {
        return new LocalDetailDto(
                l.getRegionName(),
                l.getSupportYear(),
                l.getSupportType(),
                l.getSelectionCriteria(),
                l.getSupportDetails(),
                l.getContact(),
                l.getDetailUrl(),
                l.getSupportCycle()
        );
    }
}
// 이 record의 역할: 지자체 자식 컬럼만 모아 detail 응답에 노출.
// regionName이 검색 API의 region 필터 핵심이라 LocalDetailDto 안에서도 첫 번째 필드로 둔다.
