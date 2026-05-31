package com.mozi.backend.domain.welfare.dto;

import com.mozi.backend.domain.welfare.entity.WelfareCentral;

/**
 * 복지로 중앙부처 데이터 전용 detail 필드 DTO.
 *
 * WelfareDetailDto의 `centralDetail` 필드에 들어가며, welfareType=CENTRAL인 경우에만 채워진다.
 * processSteps는 중앙부처 사업의 행정 흐름 텍스트로 다른 출처에는 없는 컬럼.
 *
 * @param supportYear 지원 연도 (nullable — 일부 row는 연도 미상)
 * @param supportType 지원 유형
 * @param selectionCriteria 선정 기준 상세
 * @param supportDetails 지원 내용 상세
 * @param contactNumber 문의 전화번호
 * @param detailUrl 상세 페이지 URL
 * @param supportCycle 지원 주기
 * @param processSteps 처리 절차 텍스트
 */
public record CentralDetailDto(
        Integer supportYear,
        String supportType,
        String selectionCriteria,
        String supportDetails,
        String contactNumber,
        String detailUrl,
        String supportCycle,
        String processSteps
) {

    /**
     * WelfareCentral 엔티티 → detail DTO 변환.
     */
    public static CentralDetailDto from(WelfareCentral c) {
        return new CentralDetailDto(
                c.getSupportYear(),
                c.getSupportType(),
                c.getSelectionCriteria(),
                c.getSupportDetails(),
                c.getContactNumber(),
                c.getDetailUrl(),
                c.getSupportCycle(),
                c.getProcessSteps()
        );
    }
}
// 이 record의 역할: 중앙부처 자식 컬럼만 모아 detail 응답에 노출.
