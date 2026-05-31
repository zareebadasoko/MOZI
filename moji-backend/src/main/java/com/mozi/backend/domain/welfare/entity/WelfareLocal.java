package com.mozi.backend.domain.welfare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 복지로 사이트의 "지자체복지서비스" 데이터를 담는 자식 엔티티.
 *
 * 시·군·구 단위 지방자치단체가 제공하는 복지 서비스. region_name 컬럼이
 * 핵심이며 검색 API의 지역 필터링에서 자주 쓰여 인덱스를 명시 부여한다.
 *
 * Central(복지로 중앙)과 컬럼 명명에 미묘한 차이가 있다:
 * - Central: contact_number (VARCHAR 50)
 * - Local:   contact (VARCHAR 100, 부서명 + 팀명까지 들어가 더 길 수 있음)
 *
 * 원본 크롤링 데이터가 그렇게 분리돼 있어 ERD §4-1에서 그대로 유지하기로 결정.
 */
@Entity
@DiscriminatorValue("LOCAL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "welfare_local",
        indexes = @Index(name = "idx_welfare_local_region_name", columnList = "region_name")
)
public class WelfareLocal extends WelfareCommon {

    @Column(name = "region_name", length = 100)
    private String regionName;

    @Column(name = "support_year", columnDefinition = "YEAR")
    private Integer supportYear;

    @Column(name = "support_type", columnDefinition = "TEXT")
    private String supportType;

    @Column(name = "selection_criteria", columnDefinition = "LONGTEXT")
    private String selectionCriteria;

    @Column(name = "support_details", columnDefinition = "LONGTEXT")
    private String supportDetails;

    // 실측 데이터 max 279자 (여러 시·구 부서가 "|"로 연결된 케이스). 여유 두고 500.
    @Column(length = 500)
    private String contact;

    @Column(name = "detail_url", length = 500)
    private String detailUrl;

    @Column(name = "support_cycle", length = 100)
    private String supportCycle;

    /**
     * Builder 패턴으로 인스턴스를 생성하기 위한 단일 진입 생성자.
     *
     * @param id 자연키 (WLF prefix)
     * @param title 복지 제목
     * @param summary 한 줄 요약
     * @param organizationName 담당 기관명
     * @param targetAudience 지원 대상 상세
     * @param applicationMethod 신청 방법 상세
     * @param regionName 지역명 (예: "경기도 의정부시"), 검색 인덱스 적용
     * @param supportYear 지원 연도 (nullable — 일부 지자체 데이터는 연도 미상)
     * @param supportType 지원 유형
     * @param selectionCriteria 선정 기준 상세
     * @param supportDetails 지원 내용 상세
     * @param contact 문의처 (부서명 + 팀명 포함 가능, 최대 100자)
     * @param detailUrl 상세 페이지 URL
     * @param supportCycle 지원 주기
     */
    @Builder
    private WelfareLocal(String id, String title, String summary, String organizationName,
                         String targetAudience, String applicationMethod,
                         String regionName, Integer supportYear, String supportType,
                         String selectionCriteria, String supportDetails, String contact,
                         String detailUrl, String supportCycle) {
        super(id, title, summary, organizationName, targetAudience, applicationMethod);
        this.regionName = regionName;
        this.supportYear = supportYear;
        this.supportType = supportType;
        this.selectionCriteria = selectionCriteria;
        this.supportDetails = supportDetails;
        this.contact = contact;
        this.detailUrl = detailUrl;
        this.supportCycle = supportCycle;
    }
}
// 이 클래스의 역할: 지자체 복지 서비스 전용 컬럼 보관.
// region_name은 검색 API의 ?region=서울특별시 같은 필터링 핵심이라 INDEX 부여.
// process_steps가 없는 것이 Central과의 큰 차이점 — 지자체 사업은 행정 절차가 다양해 일관 표현 어려움.
