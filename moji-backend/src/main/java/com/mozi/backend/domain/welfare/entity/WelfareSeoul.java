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
 * 서울복지포털(wis.seoul.go.kr) 데이터를 담는 자식 엔티티.
 *
 * ⭐ 절대 WelfareLocal에 병합하지 말 것 — 출처 사이트 자체가 복지로와 다르고
 * 데이터 스키마도 근본적으로 다름:
 * - detail_content 필드는 복지로의 어떤 컬럼과도 1:1 대응되지 않음
 * - selection_criteria, support_details, support_year, region_name, process_steps 모두 없음
 * - region_name이 없는 이유: 모든 row가 "서울특별시" 단일 지역이라 redundant
 *
 * 이 자식 엔티티가 시연 시나리오 C("장애 노인 박할머니 — 활동지원")에서
 * 정상 동작하는 것이 Phase 0 ERD 결정의 핵심 검증 포인트.
 */
@Entity
@DiscriminatorValue("SEOUL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "welfare_seoul")
public class WelfareSeoul extends WelfareCommon {

    @Column(name = "support_type", columnDefinition = "TEXT")
    private String supportType;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "detail_content", columnDefinition = "LONGTEXT")
    private String detailContent;

    @Column(name = "support_cycle", length = 100)
    private String supportCycle;

    @Column(name = "required_documents", columnDefinition = "TEXT")
    private String requiredDocuments;

    /**
     * Builder 패턴으로 인스턴스를 생성하기 위한 단일 진입 생성자.
     *
     * @param id 자연키 (SEL prefix — 서울복지포털 자체 ID)
     * @param title 복지 제목
     * @param summary 한 줄 요약
     * @param organizationName 담당 부서 (서울시청 내부 부서명)
     * @param targetAudience 지원 대상 상세
     * @param applicationMethod 신청 방법 상세
     * @param supportType 지원 유형 (서비스 / 현금 등)
     * @param contactNumber 문의 전화번호
     * @param detailContent 상세 내용 (서울복지포털 고유 필드, 복지로의 support_details와 다름)
     * @param supportCycle 지원 주기 (상시 / 1개월 내 등)
     * @param requiredDocuments 제출 서류 안내
     */
    @Builder
    private WelfareSeoul(String id, String title, String summary, String organizationName,
                         String targetAudience, String applicationMethod,
                         String supportType, String contactNumber, String detailContent,
                         String supportCycle, String requiredDocuments) {
        super(id, title, summary, organizationName, targetAudience, applicationMethod);
        this.supportType = supportType;
        this.contactNumber = contactNumber;
        this.detailContent = detailContent;
        this.supportCycle = supportCycle;
        this.requiredDocuments = requiredDocuments;
    }
}
// 이 클래스의 역할: 서울복지포털 전용 컬럼 보관 + 출처 분리의 상징.
// CLAUDE.md §5-1, ERD §3-2에 "Local에 병합 금지"가 명시된 이유:
// 출처 사이트가 다르면 미래에 다른 출처(부산복지포털 등) 추가 시 또 같은 문제가 반복되므로
// 처음부터 출처 단위로 자식 테이블을 분리하는 게 정규화 관점에서 옳다.
