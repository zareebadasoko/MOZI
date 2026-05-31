package com.mozi.backend.domain.category.controller;

import com.mozi.backend.domain.category.dto.CategoryDto;
import com.mozi.backend.domain.category.entity.CategoryType;
import com.mozi.backend.domain.category.service.CategoryService;
import com.mozi.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 카테고리 마스터 조회 라우트(`/api/categories`)의 단일 진입점.
 *
 * 비로그인도 접근 가능한 공개 라우트(SecurityConfig permitAll). type 파라미터는 필수이며
 * 미지정 시 Spring이 MissingServletRequestParameterException을 발생시켜 400 응답.
 * 잘못된 enum 값(예: ?type=WRONG)은 MethodArgumentTypeMismatchException → 400.
 * 두 케이스 모두 GlobalExceptionHandler의 fallback 또는 Spring 기본 응답으로 처리됨.
 */
@Tag(name = "Category", description = "카테고리 마스터 조회 — 비로그인 접근 가능")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 분류별 카테고리 목록 조회.
     *
     * @param type THEME(15행) 또는 STATUS(7행). 필수.
     * @return 해당 타입의 모든 카테고리
     */
    @Operation(summary = "카테고리 목록 조회", description = "type=THEME(15행) 또는 STATUS(7행). 필수 파라미터.")
    @GetMapping
    public ApiResponse<List<CategoryDto>> getCategories(@RequestParam CategoryType type) {
        return ApiResponse.success(categoryService.getByType(type));
    }
}
// 이 클래스의 역할: 카테고리 마스터 조회 단일 엔드포인트.
// type 필수 정책으로 응답 크기 제한 + 의도 명확화. 미지정/오타 모두 400으로 안내.
