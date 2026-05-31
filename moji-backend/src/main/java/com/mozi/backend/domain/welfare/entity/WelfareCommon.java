package com.mozi.backend.domain.welfare.entity;

import com.mozi.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 4가지 복지 출처(중앙/지자체/민간/서울)의 공통 속성을 담는 추상 부모 엔티티.
 *
 * JPA JOINED 상속 매핑으로 정규화된 4개 자식 테이블과 1:1로 연결된다.
 * 자식 종류에 무관하게 통합 검색/북마크가 가능하도록 부모를 기준 키로 사용하며,
 * 부모 테이블의 welfare_type 컬럼이 Discriminator 역할을 한다.
 *
 * ID 정책 (ERD §4-2 A):
 * - VARCHAR(12) 자연키. 크롤링 원본 ID를 그대로 보관 (WLF*, BOK*, SEL* prefix).
 * - 출처별 prefix가 있어 추적성 확보 + AUTO_INCREMENT 불필요.
 *
 * Category 정규화 (ERD §4-2 C):
 * - 모든 출처가 동일 컬럼명(interest_theme_code / household_status_code) 사용.
 *   서울 원본의 INTRS_THEMA_CD / FMLY_CIRC_CD는 시드 데이터 단계에서
 *   사전 정규화됨 (2026-05-08, 18건 일괄 변경).
 * - 위 두 컬럼은 엔티티에 직접 두지 않고, 시드 어댑터(Phase 2-3)가
 *   콤마 분리 텍스트를 파싱해 Category + WelfareCategory N:M 매핑으로 정규화.
 * - 출처별 분기 없이 단일 어댑터 로직으로 처리.
 * - 카테고리 코드 정의는 docs/CATEGORY_REFERENCE.md 참조.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "welfare_type", discriminatorType = DiscriminatorType.STRING, length = 20)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "welfare_common",
        indexes = @Index(name = "idx_welfare_common_welfare_type", columnList = "welfare_type")
)
public abstract class WelfareCommon extends BaseEntity {

    @Id
    @Column(length = 12)
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /**
     * 복지 출처 enum 매핑. JPA Discriminator 컬럼을 같은 이름으로 두 번 매핑하는
     * 트릭을 사용하므로 insertable=false / updatable=false로 readonly 처리한다.
     *
     * - 저장 시: JPA가 @DiscriminatorValue로부터 자동 기록 (자식 클래스 타입 기반)
     * - 조회 시: DB에 기록된 문자열 → 이 필드로 매핑되어 API 응답에 그대로 노출 가능
     *
     * 자식 클래스를 직접 인스턴스화하면 자동으로 올바른 값이 설정된다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "welfare_type", insertable = false, updatable = false)
    private WelfareType welfareType;

    @Column(name = "organization_name", columnDefinition = "TEXT")
    private String organizationName;

    @Column(name = "target_audience", columnDefinition = "LONGTEXT")
    private String targetAudience;

    @Column(name = "application_method", columnDefinition = "LONGTEXT")
    private String applicationMethod;

    /**
     * 자식 엔티티의 빌더/팩토리에서 부모 공통 필드를 초기화하기 위한 생성자.
     *
     * 자식 클래스는 자신의 @Builder 생성자에서 super(...)로 본 생성자를 호출해
     * 부모 필드를 채운다. 외부 코드는 자식의 builder()를 통해서만 인스턴스를
     * 생성할 수 있도록 protected로 제한.
     *
     * @param id 자연키 (WLF/BOK/SEL prefix + 숫자)
     * @param title 복지 제목 (NOT NULL, 최대 200자)
     * @param summary 한 줄 요약 (TEXT)
     * @param organizationName 담당 기관명 (TEXT)
     * @param targetAudience 지원 대상 상세 텍스트 (LONGTEXT)
     * @param applicationMethod 신청 방법 상세 텍스트 (LONGTEXT)
     */
    protected WelfareCommon(String id, String title, String summary, String organizationName,
                            String targetAudience, String applicationMethod) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.organizationName = organizationName;
        this.targetAudience = targetAudience;
        this.applicationMethod = applicationMethod;
    }
}
// 이 클래스의 역할: 4개 출처의 공통 속성 + JPA 상속 매핑의 부모.
// abstract라 직접 인스턴스화 불가 — 모든 row는 4개 자식 중 하나로 분류된다.
// JOINED 전략 덕에 자식 테이블별 컬럼이 분리 정규화되며, 부모 단일 조회는
// welfare_common 한 테이블만 참조하므로 검색 API에서 효율적.
// 자식 정보가 필요한 상세 조회는 Phase 4에서 EntityGraph 또는 fetch join으로 N+1 방지.
