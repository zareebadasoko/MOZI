package com.mozi.backend.domain.bookmark.service;

import com.mozi.backend.domain.bookmark.dto.BookmarkCreateResponse;
import com.mozi.backend.domain.bookmark.dto.BookmarkDeleteResponse;
import com.mozi.backend.domain.bookmark.entity.Bookmark;
import com.mozi.backend.domain.bookmark.exception.BookmarkNotFoundException;
import com.mozi.backend.domain.bookmark.repository.BookmarkRepository;
import com.mozi.backend.domain.category.entity.Category;
import com.mozi.backend.domain.category.entity.CategoryType;
import com.mozi.backend.domain.category.entity.WelfareCategory;
import com.mozi.backend.domain.category.repository.WelfareCategoryRepository;
import com.mozi.backend.domain.user.entity.User;
import com.mozi.backend.domain.user.repository.UserRepository;
import com.mozi.backend.domain.welfare.dto.WelfareSummaryDto;
import com.mozi.backend.domain.welfare.entity.WelfareCentral;
import com.mozi.backend.domain.welfare.entity.WelfareLocal;
import com.mozi.backend.domain.welfare.exception.WelfareNotFoundException;
import com.mozi.backend.domain.welfare.repository.WelfareCommonRepository;
import com.mozi.backend.global.common.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BookmarkService 단위 테스트.
 *
 * 검증 대상:
 *  - create: 신규 save / idempotent 기존 ID 회수 / WELFARE_NOT_FOUND
 *  - delete: 정상 / WELFARE_NOT_FOUND / BOOKMARK_NOT_FOUND
 *  - list: 정상 페이지 응답 / size 클램프 / 빈 페이지 (categories lookup 호출 X)
 *  - GET 응답의 모든 항목 isBookmarked=true
 */
@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private WelfareCommonRepository welfareCommonRepository;
    @Mock private WelfareCategoryRepository welfareCategoryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private BookmarkService bookmarkService;

    /**
     * create 정상 — welfare 존재 + 북마크 없음 → 신규 save + 새 ID 반환.
     */
    @Test
    void create_신규_save호출_새ID반환() {
        when(welfareCommonRepository.existsById("WLF1")).thenReturn(true);
        when(bookmarkRepository.findByUser_IdAndWelfareCommon_Id(1L, "WLF1")).thenReturn(Optional.empty());
        User userProxy = User.of("ok@mozi.test", "$2a$10$h");
        WelfareCentral welfareProxy = WelfareCentral.builder().id("WLF1").title("t").build();
        when(userRepository.getReferenceById(1L)).thenReturn(userProxy);
        when(welfareCommonRepository.getReferenceById("WLF1")).thenReturn(welfareProxy);
        when(bookmarkRepository.save(any(Bookmark.class))).thenAnswer(inv -> {
            Bookmark b = inv.getArgument(0);
            ReflectionTestUtils.setField(b, "id", 99L);
            return b;
        });

        BookmarkCreateResponse res = bookmarkService.create(1L, "WLF1");

        assertThat(res.bookmarkId()).isEqualTo(99L);
        verify(bookmarkRepository, times(1)).save(any(Bookmark.class));
    }

    /**
     * create idempotent — 이미 존재하는 북마크면 기존 row의 ID 반환 + save 호출 X.
     */
    @Test
    void create_idempotent_기존ID반환_save호출X() {
        Bookmark existing = Bookmark.of(
                User.of("ok@mozi.test", "$2a$10$h"),
                WelfareCentral.builder().id("WLF1").title("t").build()
        );
        ReflectionTestUtils.setField(existing, "id", 42L);
        when(welfareCommonRepository.existsById("WLF1")).thenReturn(true);
        when(bookmarkRepository.findByUser_IdAndWelfareCommon_Id(1L, "WLF1")).thenReturn(Optional.of(existing));

        BookmarkCreateResponse res = bookmarkService.create(1L, "WLF1");

        assertThat(res.bookmarkId()).isEqualTo(42L);
        verify(bookmarkRepository, never()).save(any(Bookmark.class));
        verify(userRepository, never()).getReferenceById(any());
        verify(welfareCommonRepository, never()).getReferenceById(any());
    }

    /**
     * create welfare 없음 — WelfareNotFoundException + save 호출 X.
     */
    @Test
    void create_없는welfareId_WelfareNotFoundException() {
        when(welfareCommonRepository.existsById("NONE")).thenReturn(false);

        assertThatThrownBy(() -> bookmarkService.create(1L, "NONE"))
                .isInstanceOf(WelfareNotFoundException.class);

        verify(bookmarkRepository, never()).save(any(Bookmark.class));
    }

    /**
     * delete 정상 — welfare/bookmark 모두 존재 → deleteBy 호출 + deleted=true.
     */
    @Test
    void delete_정상_deleted_true() {
        when(welfareCommonRepository.existsById("WLF1")).thenReturn(true);
        when(bookmarkRepository.existsByUser_IdAndWelfareCommon_Id(1L, "WLF1")).thenReturn(true);

        BookmarkDeleteResponse res = bookmarkService.delete(1L, "WLF1");

        assertThat(res.deleted()).isTrue();
        verify(bookmarkRepository, times(1)).deleteByUser_IdAndWelfareCommon_Id(1L, "WLF1");
    }

    /**
     * delete welfare 없음 — WelfareNotFoundException 우선.
     */
    @Test
    void delete_없는welfareId_WelfareNotFoundException() {
        when(welfareCommonRepository.existsById("NONE")).thenReturn(false);

        assertThatThrownBy(() -> bookmarkService.delete(1L, "NONE"))
                .isInstanceOf(WelfareNotFoundException.class);

        verify(bookmarkRepository, never()).deleteByUser_IdAndWelfareCommon_Id(any(), any());
    }

    /**
     * delete bookmark 없음 — BookmarkNotFoundException.
     */
    @Test
    void delete_없는북마크_BookmarkNotFoundException() {
        when(welfareCommonRepository.existsById("WLF1")).thenReturn(true);
        when(bookmarkRepository.existsByUser_IdAndWelfareCommon_Id(1L, "WLF1")).thenReturn(false);

        assertThatThrownBy(() -> bookmarkService.delete(1L, "WLF1"))
                .isInstanceOf(BookmarkNotFoundException.class);

        verify(bookmarkRepository, never()).deleteByUser_IdAndWelfareCommon_Id(any(), any());
    }

    /**
     * list 정상 — 페이지 응답 구조 + isBookmarked=true 고정 + categories 매핑.
     */
    @Test
    void list_정상_isBookmarked_true_categories매핑() {
        User user = User.of("ok@mozi.test", "$2a$10$h");
        WelfareLocal welfare = WelfareLocal.builder().id("WLF1").title("title").regionName("서울").build();
        Bookmark bookmark = Bookmark.of(user, welfare);
        ReflectionTestUtils.setField(bookmark, "id", 10L);
        Page<Bookmark> page = new PageImpl<>(List.of(bookmark));

        Category cat = Category.of("THM003", "신체건강", CategoryType.THEME);
        ReflectionTestUtils.setField(cat, "id", 5L);
        WelfareCategory wc = WelfareCategory.of(welfare, cat);

        when(bookmarkRepository.findByUser_Id(eq(1L), any(Pageable.class))).thenReturn(page);
        when(welfareCategoryRepository.findByWelfareCommon_IdIn(anyCollection())).thenReturn(List.of(wc));

        PageResponse<WelfareSummaryDto> res = bookmarkService.list(1L, 0, 10);

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).isBookmarked()).isTrue();   // 본인 북마크 목록이라 true 고정
        assertThat(res.items().get(0).categories()).hasSize(1);
        assertThat(res.items().get(0).categories().get(0).code()).isEqualTo("THM003");
    }

    /**
     * list size 클램프 — 999 입력 시 50으로 클램프.
     */
    @Test
    void list_size999_50으로클램프() {
        when(bookmarkRepository.findByUser_Id(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        bookmarkService.list(1L, 0, 999);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookmarkRepository).findByUser_Id(eq(1L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    /**
     * list size 0 → 1로 클램프.
     */
    @Test
    void list_size0_1로클램프() {
        when(bookmarkRepository.findByUser_Id(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        bookmarkService.list(1L, 0, 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookmarkRepository).findByUser_Id(eq(1L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    /**
     * list 빈 페이지 — categories lookup 호출 X (welfareIds.isEmpty() 분기).
     */
    @Test
    void list_빈페이지_categoriesLookup호출X() {
        when(bookmarkRepository.findByUser_Id(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        PageResponse<WelfareSummaryDto> res = bookmarkService.list(1L, 0, 10);

        assertThat(res.items()).isEmpty();
        verify(welfareCategoryRepository, never()).findByWelfareCommon_IdIn(anyCollection());
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
// 이 테스트의 역할: BookmarkService의 모든 분기를 외부 의존성 없이 결정적으로 검증.
// idempotent · 2단계 404 분기 · isBookmarked=true 고정 · size 클램프 · 빈 페이지 최적화 모두 커버.
