package com.mozi.backend.domain.welfare.dto;

import com.mozi.backend.domain.welfare.entity.WelfarePrivate;

import java.time.LocalDate;

/**
 * 복지로 민간 데이터 전용 detail 필드 DTO.
 *
 * WelfareDetailDto의 `privateDetail` 필드에 들어가며, welfareType=PRIVATE인 경우에만 채워진다.
 * 다른 출처와 달리 시작일/종료일이 명시되며 contactEmail/requiredDocuments도 추가.
 *
 * @param startDate 지원 시작일
 * @param endDate 지원 종료일
 * @param supportDetails 지원 내용 상세
 * @param contactNumber 문의 전화번호
 * @param detailUrl 상세 페이지 URL
 * @param contactEmail 문의 이메일
 * @param requiredDocuments 제출 서류 안내
 */
public record PrivateDetailDto(
        LocalDate startDate,
        LocalDate endDate,
        String supportDetails,
        String contactNumber,
        String detailUrl,
        String contactEmail,
        String requiredDocuments
) {

    /**
     * WelfarePrivate 엔티티 → detail DTO 변환.
     */
    public static PrivateDetailDto from(WelfarePrivate p) {
        return new PrivateDetailDto(
                p.getStartDate(),
                p.getEndDate(),
                p.getSupportDetails(),
                p.getContactNumber(),
                p.getDetailUrl(),
                p.getContactEmail(),
                p.getRequiredDocuments()
        );
    }
}
// 이 record의 역할: 민간 자식 컬럼만 모아 detail 응답에 노출.
// startDate/endDate가 있는 게 다른 출처와의 핵심 차이.
