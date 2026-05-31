package com.mozi.backend.domain.category.entity;

import com.mozi.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카테고리 마스터 엔티티 (THEME 15종 + STATUS 7종 = 총 22행).
 *
 * 크롤링 데이터의 콤마 분리 텍스트(`interest_theme_code`,
 * `household_status_code`)를 Category + WelfareCategory N:M 매핑으로
 * 정규화한 결과의 한쪽. 22행은 마스터 데이터로 정적 시드 적재되며
 * (Phase 2-4), 시연 중 사용자가 추가/수정하지 않는다.
 *
 * 코드/이름 매핑은 `docs/CATEGORY_REFERENCE.md` §1, §2의 표를 그대로 따른다.
 */
@Entity
@Getter
@Table(
        name = "category",
        uniqueConstraints = @UniqueConstraint(name = "uk_category_code", columnNames = "code")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryType type;

    private Category(String code, String name, CategoryType type) {
        this.code = code;
        this.name = name;
        this.type = type;
    }

    /**
     * 카테고리 마스터 행 생성용 정적 팩토리.
     *
     * Phase 2-4 시드 어댑터가 CATEGORY_REFERENCE.md의 22행 표를 순회하며 호출.
     *
     * @param code 코드 식별자 (예: "THM001", "STS003"). UNIQUE.
     * @param name 사용자 표시 이름 (예: "보호·돌봄", "장애인")
     * @param type 분류 (THEME / STATUS)
     * @return id 미할당 상태의 Category (save 후 PK 채워짐)
     */
    public static Category of(String code, String name, CategoryType type) {
        return new Category(code, name, type);
    }
}
// 이 클래스의 역할: 22개 카테고리 코드의 단일 진실 원천.
// 시드 적재 후엔 사실상 read-only로 동작 (사용자 입력에 의해 추가/수정되지 않음).
// 신규 카테고리 추가가 필요하면 CATEGORY_REFERENCE.md 갱신 + 시드 재실행.
