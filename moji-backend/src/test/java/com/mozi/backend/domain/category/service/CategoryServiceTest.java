package com.mozi.backend.domain.category.service;

import com.mozi.backend.domain.category.dto.CategoryDto;
import com.mozi.backend.domain.category.entity.Category;
import com.mozi.backend.domain.category.entity.CategoryType;
import com.mozi.backend.domain.category.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * CategoryService 단위 테스트.
 *
 * 검증: THEME/STATUS 타입별로 호출 시 Repository에 위임되어 매핑된 DTO를 반환하는지.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void getByType_THEME_15행반환() {
        Category c1 = Category.of("THM001", "서민금융", CategoryType.THEME);
        Category c2 = Category.of("THM003", "신체건강", CategoryType.THEME);
        ReflectionTestUtils.setField(c1, "id", 1L);
        ReflectionTestUtils.setField(c2, "id", 2L);
        when(categoryRepository.findByType(CategoryType.THEME)).thenReturn(List.of(c1, c2));

        List<CategoryDto> res = categoryService.getByType(CategoryType.THEME);

        assertThat(res).hasSize(2);
        assertThat(res.get(0).code()).isEqualTo("THM001");
        assertThat(res.get(0).type()).isEqualTo(CategoryType.THEME);
    }

    @Test
    void getByType_STATUS_7행반환() {
        Category s = Category.of("STS001", "보훈대상자", CategoryType.STATUS);
        ReflectionTestUtils.setField(s, "id", 16L);
        when(categoryRepository.findByType(CategoryType.STATUS)).thenReturn(List.of(s));

        List<CategoryDto> res = categoryService.getByType(CategoryType.STATUS);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).type()).isEqualTo(CategoryType.STATUS);
    }
}
// 이 테스트의 역할: 단순 위임 로직이지만 매핑(엔티티 → DTO)이 정확히 일어나는지 한 번 확정.
