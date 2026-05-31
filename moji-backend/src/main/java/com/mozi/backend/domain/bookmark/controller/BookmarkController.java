package com.mozi.backend.domain.bookmark.controller;

import com.mozi.backend.domain.bookmark.dto.BookmarkCreateResponse;
import com.mozi.backend.domain.bookmark.dto.BookmarkDeleteResponse;
import com.mozi.backend.domain.bookmark.service.BookmarkService;
import com.mozi.backend.domain.welfare.dto.WelfareSummaryDto;
import com.mozi.backend.global.common.ApiResponse;
import com.mozi.backend.global.common.PageResponse;
import com.mozi.backend.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 북마크 라우트(`/api/bookmarks`)의 단일 진입점.
 *
 * 3개 엔드포인트 모두 인증 필수 — SecurityConfig의 `.anyRequest().authenticated()`로 자동 강제되며
 * `@AuthenticationPrincipal AuthenticatedUser principal`로 SecurityContext의 사용자 정보를 받는다.
 * 미인증 호출 시 JwtAuthenticationEntryPoint가 401 UNAUTHORIZED 응답.
 */
@Tag(name = "Bookmark", description = "북마크 추가 / 삭제 / 본인 목록 조회 (인증 필수)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    /**
     * 북마크 추가 — idempotent (이미 있으면 기존 ID 반환).
     *
     * @param welfareId 북마크 대상 복지 자연키
     * @param principal 인증 사용자
     * @return 200 + bookmarkId
     */
    @Operation(summary = "북마크 추가", description = "Idempotent — 이미 존재하면 기존 bookmarkId 반환. 두 번 호출 모두 200.")
    @PostMapping("/{welfareId}")
    public ApiResponse<BookmarkCreateResponse> create(
            @PathVariable String welfareId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success("북마크에 추가되었어요.",
                bookmarkService.create(principal.userId(), welfareId));
    }

    /**
     * 북마크 삭제.
     *
     * @param welfareId 삭제 대상 복지 자연키
     * @param principal 인증 사용자
     * @return 200 + deleted=true
     */
    @Operation(summary = "북마크 삭제", description = "2단계 404: welfare 미존재 → WELFARE_NOT_FOUND, 북마크 미존재 → BOOKMARK_NOT_FOUND.")
    @DeleteMapping("/{welfareId}")
    public ApiResponse<BookmarkDeleteResponse> delete(
            @PathVariable String welfareId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success("북마크에서 삭제되었어요.",
                bookmarkService.delete(principal.userId(), welfareId));
    }

    /**
     * 본인 북마크 목록 조회 (페이지네이션).
     *
     * @param page 0-based 페이지 (기본 0)
     * @param size 페이지 크기 (기본 10, 서비스에서 1~50 클램프)
     * @param principal 인증 사용자
     * @return PageResponse<WelfareSummaryDto> — 모든 항목 isBookmarked=true 고정
     */
    @Operation(summary = "내 북마크 목록", description = "createdAt DESC 정렬, 모든 항목 isBookmarked=true 고정.")
    @GetMapping
    public ApiResponse<PageResponse<WelfareSummaryDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success(bookmarkService.list(principal.userId(), page, size));
    }
}
// 이 클래스의 역할: HTTP ↔ BookmarkService 어댑터.
// 비즈니스 로직은 모두 서비스 레이어에 있고 컨트롤러는 라우팅·기본값·응답 래핑만 담당.
