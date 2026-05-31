package com.mozi.backend.domain.welfare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 복지로 사이트의 "민간복지서비스" 데이터를 담는 자식 엔티티.
 *
 * 사회복지법인/재단 등 민간 단체가 제공하는 복지 서비스. Central/Local과 달리
 * 지원 기간(start_date~end_date)이 명시되며 contact_email/required_documents가
 * 추가로 제공된다. selection_criteria, support_year, process_steps, support_cycle은
 * 원본에 없는 컬럼이라 본 엔티티엔 두지 않는다.
 *
 * ID prefix는 BOK00000xxx로 복지로(Bokjiro) 자체의 민간 ID를 그대로 사용.
 */
@Entity
@DiscriminatorValue("PRIVATE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "welfare_private")
public class WelfarePrivate extends WelfareCommon {

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "support_details", columnDefinition = "LONGTEXT")
    private String supportDetails;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "detail_url", length = 500)
    private String detailUrl;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "required_documents", columnDefinition = "TEXT")
    private String requiredDocuments;

    /**
     * Builder 패턴으로 인스턴스를 생성하기 위한 단일 진입 생성자.
     *
     * @param id 자연키 (BOK prefix)
     * @param title 복지 제목
     * @param summary 한 줄 요약
     * @param organizationName 담당 기관명 (예: "한국사회복지공제회")
     * @param targetAudience 지원 대상 상세
     * @param applicationMethod 신청 방법 상세
     * @param startDate 지원 시작일
     * @param endDate 지원 종료일 (만료된 사업 필터링 시 사용 가능)
     * @param supportDetails 지원 내용 상세
     * @param contactNumber 문의 전화번호
     * @param detailUrl 상세 페이지 URL
     * @param contactEmail 문의 이메일
     * @param requiredDocuments 제출 서류 안내
     */
    @Builder
    private WelfarePrivate(String id, String title, String summary, String organizationName,
                           String targetAudience, String applicationMethod,
                           LocalDate startDate, LocalDate endDate, String supportDetails,
                           String contactNumber, String detailUrl, String contactEmail,
                           String requiredDocuments) {
        super(id, title, summary, organizationName, targetAudience, applicationMethod);
        this.startDate = startDate;
        this.endDate = endDate;
        this.supportDetails = supportDetails;
        this.contactNumber = contactNumber;
        this.detailUrl = detailUrl;
        this.contactEmail = contactEmail;
        this.requiredDocuments = requiredDocuments;
    }
}
// 이 클래스의 역할: 민간 복지 서비스 전용 컬럼 보관.
// start_date / end_date 가 있는 점이 Central/Local과의 가장 큰 차이 — 한정된 기간 지원이 많음.
// API 검색에서 "기간 만료된 사업 제외" 같은 필터링 시 활용 가능 (Phase 4 검토).
