package com.mozi.backend.domain.bookmark.service;

import com.mozi.backend.domain.bookmark.dto.BookmarkCreateResponse;
import com.mozi.backend.domain.bookmark.dto.BookmarkDeleteResponse;
import com.mozi.backend.domain.bookmark.entity.Bookmark;
import com.mozi.backend.domain.bookmark.exception.BookmarkNotFoundException;
import com.mozi.backend.domain.bookmark.repository.BookmarkRepository;
import com.mozi.backend.domain.category.dto.CategoryDto;
import com.mozi.backend.domain.category.repository.WelfareCategoryRepository;
import com.mozi.backend.domain.user.entity.User;
import com.mozi.backend.domain.user.repository.UserRepository;
import com.mozi.backend.domain.welfare.dto.WelfareSummaryDto;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.exception.WelfareNotFoundException;
import com.mozi.backend.domain.welfare.repository.WelfareCommonRepository;
import com.mozi.backend.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 북마크 도메인 비즈니스 로직 (POST/DELETE/GET).
 *
 * 핵심 정책:
 * - POST: idempotent — 이미 있으면 기존 row의 ID 반환, 없으면 신규 save (모두 200)
 * - DELETE: 2단계 분기 — WELFARE_NOT_FOUND 우선 → BOOKMARK_NOT_FOUND
 * - GET: PageResponse<WelfareSummaryDto> 형식, 모든 항목 isBookmarked=true 고정,
 *        createdAt DESC 정렬, size 1~50 클램프
 * - N+1 회피: GET 시 페이지의 welfare ID들로 categories를 한 번에 IN lookup
 *
 * 4-2 산출물(WelfareSummaryDto, PageResponse, WelfareNotFoundException,
 * WelfareCategoryRepository.findByWelfareCommon_IdIn)을 그대로 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class BookmarkService {

    /** size 파라미터 허용 최대치. 4-2와 동일 (50). */
    private static final int MAX_PAGE_SIZE = 50;

    private final BookmarkRepository bookmarkRepository;
    private final WelfareCommonRepository welfareCommonRepository;
    private final WelfareCategoryRepository welfareCategoryRepository;
    private final UserRepository userRepository;

    /**
     * 북마크 추가 — idempotent 처리.
     *
     * 흐름:
     * 1) welfare 존재 검증 (FK 위반 의존 회피, 명시적 404)
     * 2) 이미 북마크되어 있으면 기존 row의 ID 반환
     * 3) 없으면 User/WelfareCommon proxy로 FK만 채워 신규 save
     *
     * @param userId 인증 사용자 PK
     * @param welfareId 북마크 대상 복지 자연키
     * @return 신규/기존 모두 동일 형식의 BookmarkCreateResponse
     * @throws WelfareNotFoundException welfareId가 DB에 없는 경우
     */
    @Transactional
    public BookmarkCreateResponse create(Long userId, String welfareId) {
        if (!welfareCommonRepository.existsById(welfareId)) {
            throw new WelfareNotFoundException();
        }
        // idempotent: 같은 (user, welfare) 조합이 이미 있으면 기존 ID 반환
        Optional<Bookmark> existing = bookmarkRepository.findByUser_IdAndWelfareCommon_Id(userId, welfareId);
        if (existing.isPresent()) {
            return new BookmarkCreateResponse(existing.get().getId());
        }
        // 신규 save — getReferenceById로 SELECT 회피하고 FK만 채움
        User user = userRepository.getReferenceById(userId);
        WelfareCommon welfare = welfareCommonRepository.getReferenceById(welfareId);
        Bookmark saved = bookmarkRepository.save(Bookmark.of(user, welfare));
        return new BookmarkCreateResponse(saved.getId());
    }

    /**
     * 북마크 삭제 — 2단계 404 분기.
     *
     * 1) welfare 존재 X → WELFARE_NOT_FOUND
     * 2) 본인 북마크 row 부재 → BOOKMARK_NOT_FOUND
     * 3) 위 둘 모두 통과 → 실제 삭제
     *
     * 두 404를 분리하는 이유: 클라이언트가 "복지 자체가 없음"인지 "내가 북마크 안 함"인지
     * 구분해 다른 안내(예: 토스트 메시지)를 띄울 수 있게 한다.
     *
     * @param userId 인증 사용자 PK
     * @param welfareId 삭제 대상 복지 자연키
     * @return 항상 deleted=true
     * @throws WelfareNotFoundException welfareId가 DB에 없는 경우
     * @throws BookmarkNotFoundException 본인의 북마크 row가 없는 경우
     */
    @Transactional
    public BookmarkDeleteResponse delete(Long userId, String welfareId) {
        if (!welfareCommonRepository.existsById(welfareId)) {
            throw new WelfareNotFoundException();
        }
        if (!bookmarkRepository.existsByUser_IdAndWelfareCommon_Id(userId, welfareId)) {
            throw new BookmarkNotFoundException();
        }
        bookmarkRepository.deleteByUser_IdAndWelfareCommon_Id(userId, welfareId);
        return new BookmarkDeleteResponse(true);
    }

    /**
     * 본인 북마크 목록 페이지 조회.
     *
     * 정책:
     * - 정렬: createdAt DESC (최근 저장 우선)
     * - size: 1~50 범위로 클램프, 음수/0은 1로
     * - N+1 회피: BookmarkRepository.findByUser_Id가 @EntityGraph로 welfareCommon eager fetch +
     *             categories는 4-2의 WelfareCategoryRepository.findByWelfareCommon_IdIn 1회 IN lookup
     * - isBookmarked: 본인 북마크 목록이라 모두 true 고정
     *
     * @param userId 인증 사용자 PK
     * @param page 0-based 페이지 (음수는 0으로 클램프)
     * @param size 페이지 크기 (1~50 범위 클램프)
     * @return PageResponse<WelfareSummaryDto> — 4-2 검색 응답과 동일 구조
     */
    @Transactional(readOnly = true)
    public PageResponse<WelfareSummaryDto> list(Long userId, int page, int size) {
        int clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, clampedSize, Sort.by("createdAt").descending());
        Page<Bookmark> bookmarks = bookmarkRepository.findByUser_Id(userId, pageable);

        // categories N+1 회피 — 페이지의 welfare ID들로 한 번에 IN lookup (4-2 패턴 재사용)
        List<String> welfareIds = bookmarks.getContent().stream()
                .map(b -> b.getWelfareCommon().getId())
                .toList();
        Map<String, List<CategoryDto>> categoryMap = loadCategoryMap(welfareIds);

        // 본인 북마크 목록이라 isBookmarked는 모두 true 고정
        return PageResponse.from(bookmarks, b -> WelfareSummaryDto.of(
                b.getWelfareCommon(),
                categoryMap.getOrDefault(b.getWelfareCommon().getId(), List.of()),
                true
        ));
    }

    /**
     * welfare ID 목록에 대응하는 카테고리 매핑을 한 번의 IN 쿼리로 조회한 뒤 ID별 그루핑.
     *
     * 4-2의 WelfareService.loadCategoryMap과 동일한 패턴. EntityGraph로 category까지
     * 함께 fetch되어 LAZY 추가 쿼리 없음.
     *
     * @param welfareIds 페이지의 welfare ID 목록 (최대 size)
     * @return welfareId → CategoryDto[] 매핑 (없으면 빈 리스트)
     */
    private Map<String, List<CategoryDto>> loadCategoryMap(Collection<String> welfareIds) {
        if (welfareIds.isEmpty()) {
            return Map.of();
        }
        return welfareCategoryRepository.findByWelfareCommon_IdIn(welfareIds).stream()
                .collect(Collectors.groupingBy(
                        wc -> wc.getWelfareCommon().getId(),
                        Collectors.mapping(wc -> CategoryDto.from(wc.getCategory()), Collectors.toList())
                ));
    }
}
// 이 클래스의 역할: 북마크 추가/삭제/조회 흐름의 단일 진입점.
// 4-2의 검색 응답과 동일 구조(PageResponse<WelfareSummaryDto>)를 GET에 재사용해 프론트 컴포넌트 통일.
// idempotent + 404 분리 + N+1 회피 세 가지가 핵심 비즈니스 결정.
