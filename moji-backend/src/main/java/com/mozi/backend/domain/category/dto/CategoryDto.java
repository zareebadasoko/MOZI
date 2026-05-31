package com.mozi.backend.domain.category.dto;

import com.mozi.backend.domain.category.entity.Category;
import com.mozi.backend.domain.category.entity.CategoryType;

/**
 * Category 마스터 응답 DTO.
 *
 * GET /api/categories 응답과 WelfareSummaryDto.categories[] 양쪽에서 동일하게 사용된다.
 * 22개 마스터 row를 그대로 노출하되 BaseEntity의 createdAt/updatedAt은 의미상
 * 클라이언트에 불필요해 record 필드에 포함하지 않는다.
 *
 * @param id 카테고리 PK (정렬·식별용)
 * @param code 카테고리 코드 (예: "THM001", "STS003")
 * @param name 한글 이름 (예: "보호·돌봄", "장애인")
 * @param type 분류 (THEME / STATUS)
 */
public record CategoryDto(
        Long id,
        String code,
        String name,
        CategoryType type
) {

    /**
     * Category 엔티티에서 응답 DTO로 변환.
     *
     * @param category 영속화된 Category (id 채워진 상태)
     * @return id/code/name/type만 추출한 DTO
     */
    public static CategoryDto from(Category category) {
        return new CategoryDto(category.getId(), category.getCode(), category.getName(), category.getType());
    }
}
// 이 record의 역할: 카테고리 마스터의 응답 페이로드 통일.
// GET /api/categories와 WelfareSummaryDto.categories[] 양쪽에서 재사용되는 단일 표현.
