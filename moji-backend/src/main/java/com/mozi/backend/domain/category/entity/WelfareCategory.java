package com.mozi.backend.domain.category.entity;

import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 복지 ↔ 카테고리 N:M 매핑 해소 엔티티.
 *
 * 크롤링 데이터의 콤마 분리 텍스트(예: "보호·돌봄,안전·위기")를 시드 어댑터가
 * 분해해 본 매핑 행을 하나씩 생성한다. 한 복지에 여러 카테고리가 매핑될 수
 * 있고(THEME 다중 + STATUS 다중), 한 카테고리에 여러 복지가 매핑될 수 있다.
 *
 * WelfareCommon은 단방향(자식 → 부모)으로만 매핑하므로, "이 복지의 카테고리들"
 * 조회는 WelfareCategoryRepository.findByWelfareCommon_Id로 명시 호출.
 * 양방향 매핑을 두지 않는 이유는 phase2-welfare에서 정리된 WelfareCommon을
 * 또 건드리지 않고, JSON 직렬화 시 순환 참조 위험을 회피하기 위함.
 */
@Entity
@Getter
@Table(
        name = "welfare_category",
        indexes = {
                @Index(name = "idx_welfare_category_welfare_id", columnList = "welfare_id"),
                @Index(name = "idx_welfare_category_category_id", columnList = "category_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_welfare_category_welfare_category",
                columnNames = {"welfare_id", "category_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WelfareCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "welfare_id", nullable = false)
    private WelfareCommon welfareCommon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private WelfareCategory(WelfareCommon welfareCommon, Category category) {
        this.welfareCommon = welfareCommon;
        this.category = category;
    }

    /**
     * 복지-카테고리 매핑 행 생성용 정적 팩토리.
     *
     * Phase 2-4 시드 어댑터에서 콤마 분리된 카테고리 이름을 순회하며 호출.
     * 동일 (welfare, category) 조합은 복합 UNIQUE 제약에 의해 중복 저장이 막힘.
     *
     * @param welfareCommon 매핑 대상 복지 (영속화된 상태)
     * @param category 매핑 대상 카테고리 (영속화된 상태)
     * @return id 미할당 상태의 WelfareCategory (save 후 PK 채워짐)
     */
    public static WelfareCategory of(WelfareCommon welfareCommon, Category category) {
        return new WelfareCategory(welfareCommon, category);
    }
}
// 이 클래스의 역할: N:M 관계의 매핑 행 표현 + 정규화된 카테고리 검색의 기반.
// Phase 4 검색 API의 ?category=의료 필터링은 본 매핑 테이블 조인으로 구현.
// 복합 UNIQUE 덕에 시드 어댑터의 idempotent 보장 (재실행 시 중복 행 안 생김).
