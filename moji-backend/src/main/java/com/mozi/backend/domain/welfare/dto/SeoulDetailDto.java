package com.mozi.backend.domain.welfare.dto;

import com.mozi.backend.domain.welfare.entity.WelfareSeoul;

/**
 * 서울복지포털 데이터 전용 detail 필드 DTO.
 *
 * WelfareDetailDto의 `seoulDetail` 필드에 들어가며, welfareType=SEOUL인 경우에만 채워진다.
 * detailContent는 서울복지포털 고유 컬럼으로 복지로의 supportDetails와는 다른 의미.
 *
 * @param supportType 지원 유형
 * @param contactNumber 문의 전화번호
 * @param detailContent 상세 내용 (서울 고유)
 * @param supportCycle 지원 주기
 * @param requiredDocuments 제출 서류 안내
 */
public record SeoulDetailDto(
        String supportType,
        String contactNumber,
        String detailContent,
        String supportCycle,
        String requiredDocuments
) {

    /**
     * WelfareSeoul 엔티티 → detail DTO 변환.
     */
    public static SeoulDetailDto from(WelfareSeoul s) {
        return new SeoulDetailDto(
                s.getSupportType(),
                s.getContactNumber(),
                s.getDetailContent(),
                s.getSupportCycle(),
                s.getRequiredDocuments()
        );
    }
}
// 이 record의 역할: 서울 자식 컬럼만 모아 detail 응답에 노출.
// 다른 자식과 병합 금지 — 서울복지포털은 출처 사이트 자체가 다른 데이터 소스.
