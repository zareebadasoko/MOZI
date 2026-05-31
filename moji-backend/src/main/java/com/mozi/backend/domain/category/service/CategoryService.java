package com.mozi.backend.domain.category.service;

import com.mozi.backend.domain.category.dto.CategoryDto;
import com.mozi.backend.domain.category.entity.CategoryType;
import com.mozi.backend.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Category 마스터 조회 서비스.
 *
 * 22개 마스터 row는 시드 적재 후 사실상 read-only로 동작하므로 로직이 매우 단순하다.
 * 향후 캐시(@Cacheable) 도입 가능 — Phase 6 이후 검토.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 분류(THEME 또는 STATUS)별 카테고리 목록 반환.
     *
     * GET /api/categories?type=THEME → 15행, ?type=STATUS → 7행 응답.
     * 정렬은 DB 기본 순서(insertion order, IDENTITY PK 오름차순)를 따른다.
     *
     * @param type CategoryType enum (필수)
     * @return 해당 타입의 모든 CategoryDto
     */
    public List<CategoryDto> getByType(CategoryType type) {
        return categoryRepository.findByType(type).stream().map(CategoryDto::from).toList();
    }
}
// 이 클래스의 역할: 카테고리 마스터의 단일 조회 진입점.
// 로직이 단순하지만 컨트롤러가 직접 Repository를 호출하지 않게 하는 책임 분리 효과.
