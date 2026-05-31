package com.mozi.backend.domain.welfare.service;

import com.mozi.backend.domain.bookmark.repository.BookmarkRepository;
import com.mozi.backend.domain.category.entity.Category;
import com.mozi.backend.domain.category.entity.CategoryType;
import com.mozi.backend.domain.category.entity.WelfareCategory;
import com.mozi.backend.domain.category.repository.WelfareCategoryRepository;
import com.mozi.backend.domain.user.entity.Role;
import com.mozi.backend.domain.welfare.dto.WelfareDetailDto;
import com.mozi.backend.domain.welfare.dto.WelfareSearchCondition;
import com.mozi.backend.domain.welfare.dto.WelfareSummaryDto;
import com.mozi.backend.domain.welfare.entity.WelfareCentral;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.entity.WelfareLocal;
import com.mozi.backend.domain.welfare.entity.WelfarePrivate;
import com.mozi.backend.domain.welfare.entity.WelfareSeoul;
import com.mozi.backend.domain.welfare.exception.WelfareNotFoundException;
import com.mozi.backend.domain.welfare.repository.WelfareCommonRepository;
import com.mozi.backend.global.common.PageResponse;
import com.mozi.backend.global.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WelfareService 단위 테스트.
 *
 * Step 6 재설계 반영 (USER_PROFILE_REDESIGN §5 Step 6):
 * applyMyProfile + UserProfile 의존성이 모두 제거되었다. 이전에 있던 3개 케이스
 * (로그인_applyMyProfileTrue / 로그인_applyMyProfileFalse / 로그인_프로필미존재)는
 * 더 이상 의미가 없어 삭제되었고, 로그인 시 북마크 lookup이 호출되는 시나리오만
 * `search_로그인_북마크lookup_호출`로 단순화해 유지.
 *
 * 검증 대상:
 *  - 비로그인(principal=null) 시 isBookmarked 모두 false + 북마크 lookup 호출 X
 *  - 로그인 시 북마크 lookup 호출 + 결과에 isBookmarked 반영
 *  - size 클램프 (음수/0/초과)
 *  - 상세 조회: 4 자식 분기, isBookmarked 매핑, 404, categories 매핑
 */
@ExtendWith(MockitoExtension.class)
class WelfareServiceTest {

    @Mock private WelfareCommonRepository welfareRepository;
    @Mock private WelfareCategoryRepository welfareCategoryRepository;
    @Mock private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private WelfareService welfareService;

    /**
     * 비로그인 검색: isBookmarked 전부 false + 북마크 lookup 호출 X.
     */
    @Test
    void search_비로그인_isBookmarked전부false_북마크lookup호출X() {
        WelfareCentral w = WelfareCentral.builder().id("WLF1").title("t").build();
        Page<WelfareCommon> page = new PageImpl<>(List.of(w));
        when(welfareRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(welfareCategoryRepository.findByWelfareCommon_IdIn(anyCollection())).thenReturn(List.of());

        WelfareSearchCondition cond = new WelfareSearchCondition(null, null, null, null, 0, 10);
        PageResponse<WelfareSummaryDto> res = welfareService.search(cond, null);

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).isBookmarked()).isFalse();
        // 비로그인이므로 북마크 lookup 호출 X
        verify(bookmarkRepository, never()).findWelfareIdsByUser_IdAndWelfareCommon_IdIn(anyLong(), anyCollection());
    }

    /**
     * 로그인 검색: 북마크 lookup 한 번 호출 + 결과에 isBookmarked 반영.
     */
    @Test
    void search_로그인_북마크lookup_호출() {
        WelfareLocal w = WelfareLocal.builder().id("WLF98001").title("t").regionName("서울").build();
        Page<WelfareCommon> page = new PageImpl<>(List.of(w));

        when(welfareRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(welfareCategoryRepository.findByWelfareCommon_IdIn(anyCollection())).thenReturn(List.of());
        when(bookmarkRepository.findWelfareIdsByUser_IdAndWelfareCommon_IdIn(eq(1L), anyCollection()))
                .thenReturn(List.of("WLF98001"));

        AuthenticatedUser principal = new AuthenticatedUser(1L, Role.USER);
        WelfareSearchCondition cond = new WelfareSearchCondition(null, null, null, null, 0, 10);
        PageResponse<WelfareSummaryDto> res = welfareService.search(cond, principal);

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).isBookmarked()).isTrue();
        verify(bookmarkRepository, times(1)).findWelfareIdsByUser_IdAndWelfareCommon_IdIn(eq(1L), anyCollection());
    }

    /**
     * size 클램프: 999 입력 시 50으로 클램프되어 Pageable에 전달됨.
     */
    @Test
    void search_size999_50으로클램프() {
        Page<WelfareCommon> page = new PageImpl<>(List.of());
        when(welfareRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        WelfareSearchCondition cond = new WelfareSearchCondition(null, null, null, null, 0, 999);
        welfareService.search(cond, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(welfareRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    /**
     * size 클램프: 0 또는 음수 입력 시 1로 강제.
     */
    @Test
    void search_size0_1로클램프() {
        Page<WelfareCommon> page = new PageImpl<>(List.of());
        when(welfareRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        WelfareSearchCondition cond = new WelfareSearchCondition(null, null, null, null, 0, 0);
        welfareService.search(cond, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(welfareRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    /**
     * 상세 조회 - WelfareCentral 자식 → centralDetail만 채워지고 나머지는 null.
     */
    @Test
    void getDetail_central자식_centralDetail만채워짐() {
        WelfareCentral w = WelfareCentral.builder().id("WLF1").title("t").supportYear(2026).build();
        when(welfareRepository.findById("WLF1")).thenReturn(Optional.of(w));
        when(welfareCategoryRepository.findByWelfareCommon_Id("WLF1")).thenReturn(List.of());

        WelfareDetailDto res = welfareService.getDetail("WLF1", null);

        assertThat(res.centralDetail()).isNotNull();
        assertThat(res.centralDetail().supportYear()).isEqualTo(2026);
        assertThat(res.localDetail()).isNull();
        assertThat(res.privateDetail()).isNull();
        assertThat(res.seoulDetail()).isNull();
        assertThat(res.isBookmarked()).isFalse();
    }

    /**
     * 상세 조회 - WelfareSeoul → seoulDetail만 채워짐.
     */
    @Test
    void getDetail_seoul자식_seoulDetail만채워짐() {
        WelfareSeoul w = WelfareSeoul.builder().id("SEL1").title("t").detailContent("서울 전용 내용").build();
        when(welfareRepository.findById("SEL1")).thenReturn(Optional.of(w));
        when(welfareCategoryRepository.findByWelfareCommon_Id("SEL1")).thenReturn(List.of());

        WelfareDetailDto res = welfareService.getDetail("SEL1", null);

        assertThat(res.seoulDetail()).isNotNull();
        assertThat(res.seoulDetail().detailContent()).isEqualTo("서울 전용 내용");
        assertThat(res.centralDetail()).isNull();
        assertThat(res.localDetail()).isNull();
        assertThat(res.privateDetail()).isNull();
    }

    /**
     * 상세 조회 - 로그인 사용자가 북마크한 경우 isBookmarked=true.
     */
    @Test
    void getDetail_로그인_북마크있음_isBookmarkedTrue() {
        WelfarePrivate w = WelfarePrivate.builder().id("BOK1").title("t").build();
        when(welfareRepository.findById("BOK1")).thenReturn(Optional.of(w));
        when(welfareCategoryRepository.findByWelfareCommon_Id("BOK1")).thenReturn(List.of());
        when(bookmarkRepository.existsByUser_IdAndWelfareCommon_Id(1L, "BOK1")).thenReturn(true);

        AuthenticatedUser principal = new AuthenticatedUser(1L, Role.USER);
        WelfareDetailDto res = welfareService.getDetail("BOK1", principal);

        assertThat(res.isBookmarked()).isTrue();
        assertThat(res.privateDetail()).isNotNull();
    }

    /**
     * 상세 조회 - 없는 ID → WelfareNotFoundException.
     */
    @Test
    void getDetail_없는ID_WelfareNotFoundException() {
        when(welfareRepository.findById("NONE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> welfareService.getDetail("NONE", null))
                .isInstanceOf(WelfareNotFoundException.class);
    }

    /**
     * 상세 조회 - categories 매핑 검증.
     */
    @Test
    void getDetail_categories매핑() {
        WelfareCentral w = WelfareCentral.builder().id("WLF1").title("t").build();
        Category cat = Category.of("THM003", "신체건강", CategoryType.THEME);
        ReflectionTestUtils.setField(cat, "id", 10L);
        WelfareCategory wc = WelfareCategory.of(w, cat);
        when(welfareRepository.findById("WLF1")).thenReturn(Optional.of(w));
        when(welfareCategoryRepository.findByWelfareCommon_Id("WLF1")).thenReturn(List.of(wc));

        WelfareDetailDto res = welfareService.getDetail("WLF1", null);

        assertThat(res.categories()).hasSize(1);
        assertThat(res.categories().get(0).code()).isEqualTo("THM003");
        assertThat(res.categories().get(0).name()).isEqualTo("신체건강");
    }
}
// 이 테스트의 역할: WelfareService의 모든 분기를 외부 의존성 없이 결정적으로 검증.
// Step 6 정리 — applyMyProfile + 프로필 STATUS 매핑 관련 3건 삭제, 로그인 북마크 lookup 케이스만 단순화 유지.
// size 클램프·instanceof 자식 분기·N+1 회피 호출 패턴 모두 커버.
