package com.mozi.backend.domain.welfare.service;

import com.mozi.backend.domain.bookmark.repository.BookmarkRepository;
import com.mozi.backend.domain.category.dto.CategoryDto;
import com.mozi.backend.domain.category.repository.WelfareCategoryRepository;
import com.mozi.backend.domain.welfare.dto.WelfareDetailDto;
import com.mozi.backend.domain.welfare.dto.WelfareSearchCondition;
import com.mozi.backend.domain.welfare.dto.WelfareSummaryDto;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.exception.WelfareNotFoundException;
import com.mozi.backend.domain.welfare.repository.WelfareCommonRepository;
import com.mozi.backend.domain.welfare.repository.spec.WelfareSpecifications;
import com.mozi.backend.global.common.PageResponse;
import com.mozi.backend.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 복지 검색·상세 조회 비즈니스 로직.
 *
 * 핵심 정책:
 * - Optional auth: principal이 null이면 비로그인 흐름 (isBookmarked=false)
 * - Step 6(USER_PROFILE_REDESIGN §5 Step 6): 옵션 C 채택으로 본인 프로필 자동 반영(applyMyProfile)
 *   제거. 검색 조건은 사용자가 명시한 query param(keyword/category/region/welfareType)만 사용.
 *   본인 프로필 기반 추천은 챗봇 흐름이 전담.
 * - N+1 회피: 검색 결과의 categories와 isBookmarked를 각각 한 번의 IN 쿼리로 배치 lookup
 * - 정렬: createdAt DESC 고정 (최신순)
 * - size 클램프: 1~50 범위로 강제
 * - 상세 응답의 4 children 분기는 WelfareDetailDto.of() 안에서 instanceof 패턴 매칭
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WelfareService {

    /** size 파라미터의 허용 최대치. 노년층 사용성 + N+1 회피 batch 한도 고려. */
    private static final int MAX_PAGE_SIZE = 50;

    private final WelfareCommonRepository welfareRepository;
    private final WelfareCategoryRepository welfareCategoryRepository;
    private final BookmarkRepository bookmarkRepository;

    /**
     * 동적 검색 + 페이지네이션.
     *
     * 흐름:
     * 1) Specification 조립 (keyword + welfareType + region + categoryCodes)
     *    — categoryCodes는 query param `category`(단일)만 포함, 빈 경우 IN 조건 무시
     * 2) Page 조회 (createdAt DESC, size 클램프)
     * 3) categories/isBookmarked batch IN lookup (N+1 회피)
     * 4) WelfareSummaryDto로 변환해 PageResponse 래핑
     *
     * @param condition 검색 조건 묶음
     * @param principal SecurityContext의 인증 사용자 (비로그인 시 null — isBookmarked만 영향)
     * @return 페이지 응답
     */
    public PageResponse<WelfareSummaryDto> search(WelfareSearchCondition condition, AuthenticatedUser principal) {
        Long userId = principal != null ? principal.userId() : null;

        // category(단일) query param만 IN 조건에 포함. Step 6 이전엔 프로필 STATUS 코드도 합쳤지만 제거됨.
        List<String> categoryCodes = new ArrayList<>();
        if (condition.category() != null && !condition.category().isBlank()) {
            categoryCodes.add(condition.category().trim());
        }

        // 모든 spec은 입력값이 비어 있으면 conjunction()을 반환하므로 null 우려 없이 직접 체이닝 가능
        Specification<WelfareCommon> spec = WelfareSpecifications.byKeyword(condition.keyword())
                .and(WelfareSpecifications.byWelfareType(condition.welfareType()))
                .and(WelfareSpecifications.byRegion(condition.region()))
                .and(WelfareSpecifications.byCategoryCodes(categoryCodes));

        // size 클램프: 1~50 범위 강제
        int clampedSize = Math.min(Math.max(condition.size(), 1), MAX_PAGE_SIZE);
        int safePage = Math.max(condition.page(), 0);
        Pageable pageable = PageRequest.of(safePage, clampedSize, Sort.by("createdAt").descending());
        Page<WelfareCommon> page = welfareRepository.findAll(spec, pageable);

        // N+1 회피: 페이지의 welfare ID들로 categories와 isBookmarked를 각 한 번의 IN으로 조회
        List<String> ids = page.getContent().stream().map(WelfareCommon::getId).toList();
        Map<String, List<CategoryDto>> categoryMap = loadCategoryMap(ids);
        Set<String> bookmarkedIds = userId != null ? loadBookmarkedIds(userId, ids) : Set.of();

        return PageResponse.from(page, w -> WelfareSummaryDto.of(
                w,
                categoryMap.getOrDefault(w.getId(), List.of()),
                bookmarkedIds.contains(w.getId())
        ));
    }

    /**
     * 단일 복지 상세 조회.
     *
     * JOINED 매핑 덕에 findById는 자식 인스턴스(WelfareCentral/Local/Private/Seoul)를 반환하며,
     * WelfareDetailDto.of()의 instanceof 분기가 자식 detail 1개만 채운다.
     *
     * @param welfareId 자연키 (VARCHAR 12)
     * @param principal 인증 사용자 (비로그인 null)
     * @return 부모 + 자식 detail 1개 + categories + isBookmarked
     * @throws WelfareNotFoundException 해당 ID row 부재
     */
    public WelfareDetailDto getDetail(String welfareId, AuthenticatedUser principal) {
        WelfareCommon welfare = welfareRepository.findById(welfareId)
                .orElseThrow(WelfareNotFoundException::new);
        List<CategoryDto> categories = welfareCategoryRepository.findByWelfareCommon_Id(welfareId).stream()
                .map(wc -> CategoryDto.from(wc.getCategory()))
                .toList();
        // 단일 조회는 IN 배치보다 existsBy 한 번 호출이 더 단순하고 비용도 작음
        boolean isBookmarked = principal != null
                && bookmarkRepository.existsByUser_IdAndWelfareCommon_Id(principal.userId(), welfareId);
        return WelfareDetailDto.of(welfare, categories, isBookmarked);
    }

    /**
     * 검색 결과의 welfare ID 목록에 대한 카테고리 매핑을 한 번에 조회한 뒤 ID별로 그루핑.
     *
     * EntityGraph 덕에 category까지 함께 fetch되어 LAZY 추가 쿼리 없음.
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

    /**
     * 사용자의 북마크 row 중 페이지의 welfare ID들에 해당하는 것만 한 번에 조회 → Set으로 변환.
     *
     * @param userId 인증 사용자 PK
     * @param welfareIds 페이지의 welfare ID 목록
     * @return 사용자가 북마크한 welfare ID Set
     */
    private Set<String> loadBookmarkedIds(Long userId, Collection<String> welfareIds) {
        if (welfareIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(bookmarkRepository.findWelfareIdsByUser_IdAndWelfareCommon_IdIn(userId, welfareIds));
    }
}
// 이 클래스의 역할: 검색·상세 조회 흐름의 단일 진입점. Specification 조립과 N+1 회피 batch lookup을 담당.
// Step 6 정리 — applyMyProfile + UserProfile 의존성 + STATUS 매핑 메서드 모두 제거.
// 단일 상세 조회는 N+1 우려가 없어 단순 existsBy로 isBookmarked를 채운다.
