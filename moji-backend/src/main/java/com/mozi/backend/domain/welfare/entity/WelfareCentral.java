package com.mozi.backend.domain.welfare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 복지로 사이트의 "중앙부처복지사업" 데이터를 담는 자식 엔티티.
 *
 * 복지로 출처(Central/Local/Private)와 서울복지포털(Seoul) 간 스키마가 달라
 * 별도 자식 테이블로 분리한 4종 중 하나. process_steps(처리 절차) 컬럼이
 * Local/Private/Seoul과 다른 점이며, 이는 중앙부처 사업의 행정 흐름을
 * 표현하기 위해 복지로에서 별도 제공하는 데이터.
 */
@Entity
@DiscriminatorValue("CENTRAL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "welfare_central")
public class WelfareCentral extends WelfareCommon {

    @Column(name = "support_year", columnDefinition = "YEAR")
    private Integer supportYear;

    @Column(name = "support_type", columnDefinition = "TEXT")
    private String supportType;

    @Column(name = "selection_criteria", columnDefinition = "LONGTEXT")
    private String selectionCriteria;

    @Column(name = "support_details", columnDefinition = "LONGTEXT")
    private String supportDetails;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "detail_url", length = 500)
    private String detailUrl;

    @Column(name = "support_cycle", length = 100)
    private String supportCycle;

    @Column(name = "process_steps", columnDefinition = "TEXT")
    private String processSteps;

    /**
     * Builder 패턴으로 인스턴스를 생성하기 위한 단일 진입 생성자.
     *
     * 부모 공통 필드(id/title/summary 등)는 super(...)로 위임해 채우고,
     * 본 자식의 특화 필드는 직접 할당. private이라 빌더(WelfareCentral.builder())
     * 외에는 호출 불가 — 외부 코드의 일관된 생성 경로 강제.
     *
     * @param id 자연키 (WLF prefix)
     * @param title 복지 제목
     * @param summary 한 줄 요약
     * @param organizationName 담당 기관명
     * @param targetAudience 지원 대상 상세
     * @param applicationMethod 신청 방법 상세
     * @param supportYear 지원 연도 (예: 2026)
     * @param supportType 지원 유형 (현금지급, 바우처 등)
     * @param selectionCriteria 선정 기준 상세
     * @param supportDetails 지원 내용 상세
     * @param contactNumber 문의 전화번호
     * @param detailUrl 상세 페이지 URL
     * @param supportCycle 지원 주기 (1회성, 월, 년 등)
     * @param processSteps 처리 절차 텍스트 ("001(읍면동) -> 002(시군구)" 형태)
     */
    @Builder
    private WelfareCentral(String id, String title, String summary, String organizationName,
                           String targetAudience, String applicationMethod,
                           Integer supportYear, String supportType, String selectionCriteria,
                           String supportDetails, String contactNumber, String detailUrl,
                           String supportCycle, String processSteps) {
        super(id, title, summary, organizationName, targetAudience, applicationMethod);
        this.supportYear = supportYear;
        this.supportType = supportType;
        this.selectionCriteria = selectionCriteria;
        this.supportDetails = supportDetails;
        this.contactNumber = contactNumber;
        this.detailUrl = detailUrl;
        this.supportCycle = supportCycle;
        this.processSteps = processSteps;
    }
}
// 이 클래스의 역할: 복지로 중앙부처 데이터 전용 컬럼 보관.
// 부모(WelfareCommon)의 공통 컬럼 + 본 클래스의 특화 컬럼 = 자식 테이블이
// JOINED 전략으로 별도 테이블에 분리됨.
// process_steps는 "001(읍면동) -> 002(시군구) -> 005(...)" 같은 행정 절차 텍스트.
