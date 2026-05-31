package com.mozi.backend.domain.user.service;

import com.mozi.backend.domain.region.exception.InvalidRegionException;
import com.mozi.backend.domain.region.repository.RegionRepository;
import com.mozi.backend.domain.user.dto.UserProfileResponse;
import com.mozi.backend.domain.user.dto.UserProfileUpdateRequest;
import com.mozi.backend.domain.user.entity.Gender;
import com.mozi.backend.domain.user.entity.HouseholdType;
import com.mozi.backend.domain.user.entity.IncomeType;
import com.mozi.backend.domain.user.entity.User;
import com.mozi.backend.domain.user.entity.UserProfile;
import com.mozi.backend.domain.user.exception.UserNotFoundException;
import com.mozi.backend.domain.user.repository.UserProfileRepository;
import com.mozi.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserProfileService 단위 테스트.
 *
 * Step 3·4 재설계 반영 — 신규 스키마(sidoCode/sigunguCode + Enum, boolean 4종)로 갱신.
 * REGION 코드 검증은 sidoCode가 null인 케이스로 단순화해 RegionRepository 호출 없이 검증.
 * (REGION 검증 로직 자체의 검증은 Step 9에서 별도 테스트 보강 예정)
 *
 * 검증 대상:
 *  - getMyProfile: row 존재/부재 시 isCompleted 분기
 *  - updateMyProfile: lazy creation, partial update (null=무변경), 모든 null 호출
 *  - userId가 DB에 없는 모순 케이스(UserNotFoundException)
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private RegionRepository regionRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    /**
     * row 없음 → isCompleted=false인 빈 응답.
     */
    @Test
    void getMyProfile_프로필없음_isCompletedFalse() {
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        UserProfileResponse res = userProfileService.getMyProfile(1L);

        assertThat(res.isCompleted()).isFalse();
        assertThat(res.birthDate()).isNull();
        assertThat(res.isVeteran()).isNull();
    }

    /**
     * row 존재 → isCompleted=true + 모든 필드 매핑.
     */
    @Test
    void getMyProfile_프로필있음_isCompletedTrue() {
        User user = User.of("ok@mozi.test", "$2a$10$h");
        ReflectionTestUtils.setField(user, "id", 1L);
        UserProfile profile = UserProfile.fullFor(user,
                LocalDate.of(1948, 3, 1), Gender.F,
                "11", "11680",
                IncomeType.BASIC_PENSION, HouseholdType.LIVING_ALONE,
                false, false, false, false);
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(profile));

        UserProfileResponse res = userProfileService.getMyProfile(1L);

        assertThat(res.isCompleted()).isTrue();
        assertThat(res.gender()).isEqualTo(Gender.F);
        assertThat(res.sidoCode()).isEqualTo("11");
        assertThat(res.sigunguCode()).isEqualTo("11680");
        assertThat(res.incomeType()).isEqualTo(IncomeType.BASIC_PENSION);
        assertThat(res.householdType()).isEqualTo(HouseholdType.LIVING_ALONE);
        assertThat(res.isVeteran()).isFalse();
    }

    /**
     * Lazy creation: row 부재 시 emptyFor로 생성 후 save 호출.
     */
    @Test
    void updateMyProfile_프로필없음_lazyCreation호출() {
        User user = User.of("ok@mozi.test", "$2a$10$h");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 행정구역 코드는 null로 — REGION 검증 우회
        UserProfileUpdateRequest req = new UserProfileUpdateRequest(
                LocalDate.of(1955, 1, 1),
                null, null, null, null, null,
                null, null, null, null);

        UserProfileResponse res = userProfileService.updateMyProfile(1L, req);

        verify(userProfileRepository).save(any(UserProfile.class));   // lazy creation
        assertThat(res.isCompleted()).isTrue();
        assertThat(res.birthDate()).isEqualTo(LocalDate.of(1955, 1, 1));
    }

    /**
     * 일부 필드만 지정 → 지정된 필드만 변경, 나머지 원본 유지.
     */
    @Test
    void updateMyProfile_부분필드만지정_지정된필드만변경() {
        User user = User.of("ok@mozi.test", "$2a$10$h");
        ReflectionTestUtils.setField(user, "id", 1L);
        UserProfile existing = UserProfile.fullFor(user,
                LocalDate.of(1948, 3, 1), Gender.F,
                "11", "11680",
                IncomeType.BASIC_PENSION, HouseholdType.LIVING_ALONE,
                false, false, false, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(existing));

        // householdType만 변경 (REGION 검증 우회 위해 sidoCode/sigunguCode는 null)
        UserProfileUpdateRequest req = new UserProfileUpdateRequest(
                null, null, null, null, null, HouseholdType.COUPLE,
                null, null, null, null);

        UserProfileResponse res = userProfileService.updateMyProfile(1L, req);

        assertThat(res.sidoCode()).isEqualTo("11");                              // 무변경
        assertThat(res.householdType()).isEqualTo(HouseholdType.COUPLE);         // 변경됨
        assertThat(res.gender()).isEqualTo(Gender.F);                            // 무변경
        verify(userProfileRepository, never()).save(any(UserProfile.class));    // 기존 row, save 호출 X (dirty checking)
    }

    /**
     * 모든 필드 null → 무변경. (옵션 C: null=무변경 의미)
     */
    @Test
    void updateMyProfile_모든필드null_무변경() {
        User user = User.of("ok@mozi.test", "$2a$10$h");
        ReflectionTestUtils.setField(user, "id", 1L);
        UserProfile existing = UserProfile.fullFor(user,
                LocalDate.of(1948, 3, 1), Gender.F,
                "11", "11680",
                IncomeType.BASIC_PENSION, HouseholdType.LIVING_ALONE,
                false, false, false, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(existing));

        UserProfileUpdateRequest req = new UserProfileUpdateRequest(
                null, null, null, null, null, null,
                null, null, null, null);

        UserProfileResponse res = userProfileService.updateMyProfile(1L, req);

        assertThat(res.gender()).isEqualTo(Gender.F);
        assertThat(res.sidoCode()).isEqualTo("11");
        assertThat(res.sigunguCode()).isEqualTo("11680");
        assertThat(res.householdType()).isEqualTo(HouseholdType.LIVING_ALONE);
    }

    /**
     * boolean 명시적 갱신: null → 무변경, false → 명시 false, true → 명시 true.
     */
    @Test
    void updateMyProfile_boolean필드_명시적갱신() {
        User user = User.of("ok@mozi.test", "$2a$10$h");
        ReflectionTestUtils.setField(user, "id", 1L);
        UserProfile existing = UserProfile.fullFor(user,
                null, null, null, null, null, null,
                false, false, false, false);   // 모든 boolean false 시작
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(existing));

        // isDisabled를 명시적 true로 갱신, isVeteran true로 갱신, 나머지 null
        UserProfileUpdateRequest req = new UserProfileUpdateRequest(
                null, null, null, null, null, null,
                true, null, null, true);

        UserProfileResponse res = userProfileService.updateMyProfile(1L, req);

        assertThat(res.isDisabled()).isTrue();             // false → true 갱신됨
        assertThat(res.isVeteran()).isTrue();              // false → true 갱신됨
        assertThat(res.isMultiChild()).isFalse();          // null → 무변경 (원본 false 유지)
    }

    /**
     * userId가 DB에 없는 모순 케이스 → UserNotFoundException.
     */
    @Test
    void updateMyProfile_user없음_UserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UserProfileUpdateRequest req = new UserProfileUpdateRequest(
                LocalDate.of(1955, 1, 1),
                null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> userProfileService.updateMyProfile(99L, req))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ─── Step 5: REGION 검증 경로 단위 테스트 ───────────────────────────────

    /**
     * sigunguCode만 단독 입력 (sidoCode 없음) → InvalidRegionException.
     * 시도 컨텍스트 없이 시군구만 보낼 수 없다는 정책 검증.
     */
    @Test
    void updateMyProfile_sigunguCode단독입력_InvalidRegion() {
        UserProfileUpdateRequest req = new UserProfileUpdateRequest(
                null, null, null, "11680", null, null, null, null, null, null);

        assertThatThrownBy(() -> userProfileService.updateMyProfile(1L, req))
                .isInstanceOf(InvalidRegionException.class);
    }

    /**
     * REGION 마스터에 없는 sidoCode 입력 → InvalidRegionException.
     */
    @Test
    void updateMyProfile_미존재sidoCode_InvalidRegion() {
        when(regionRepository.existsBySidoCode("99")).thenReturn(false);

        UserProfileUpdateRequest req = new UserProfileUpdateRequest(
                null, null, "99", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> userProfileService.updateMyProfile(1L, req))
                .isInstanceOf(InvalidRegionException.class);
    }

    /**
     * sidoCode는 유효하지만 sigunguCode가 해당 시도 산하가 아님 → InvalidRegionException.
     * 예: 서울("11") + 파주시("41480") 조합 거부.
     */
    @Test
    void updateMyProfile_시도시군구불일치_InvalidRegion() {
        when(regionRepository.existsBySidoCode("11")).thenReturn(true);
        when(regionRepository.existsBySidoCodeAndSigunguCode("11", "41480")).thenReturn(false);

        UserProfileUpdateRequest req = new UserProfileUpdateRequest(
                null, null, "11", "41480", null, null, null, null, null, null);

        assertThatThrownBy(() -> userProfileService.updateMyProfile(1L, req))
                .isInstanceOf(InvalidRegionException.class);
    }

    /**
     * Step 9: 유효한 시도+시군구 조합 → 검증 통과 → 정상 갱신.
     * existsBySidoCodeAndSigunguCode 분기를 단위 mock으로 확인.
     */
    @Test
    void updateMyProfile_유효한시도시군구_정상갱신() {
        User user = User.of("ok@mozi.test", "$2a$10$h");
        ReflectionTestUtils.setField(user, "id", 1L);
        UserProfile existing = UserProfile.fullFor(user,
                null, null, null, null, null, null,
                false, false, false, false);
        when(regionRepository.existsBySidoCode("11")).thenReturn(true);
        when(regionRepository.existsBySidoCodeAndSigunguCode("11", "11680")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(existing));

        UserProfileUpdateRequest req = new UserProfileUpdateRequest(
                null, null, "11", "11680", null, null, null, null, null, null);

        UserProfileResponse res = userProfileService.updateMyProfile(1L, req);

        assertThat(res.sidoCode()).isEqualTo("11");
        assertThat(res.sigunguCode()).isEqualTo("11680");
    }

    /**
     * 유효한 sidoCode + sigunguCode 미입력 → 검증 통과 → 정상 갱신.
     * "시도만 선택" 케이스가 허용되는지 확인.
     */
    @Test
    void updateMyProfile_유효한sidoCode_시군구미입력_통과() {
        User user = User.of("ok@mozi.test", "$2a$10$h");
        ReflectionTestUtils.setField(user, "id", 1L);
        UserProfile existing = UserProfile.fullFor(user,
                null, null, null, null, null, null,
                false, false, false, false);
        when(regionRepository.existsBySidoCode("11")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(existing));

        UserProfileUpdateRequest req = new UserProfileUpdateRequest(
                null, null, "11", null, null, null, null, null, null, null);

        UserProfileResponse res = userProfileService.updateMyProfile(1L, req);

        assertThat(res.sidoCode()).isEqualTo("11");
        assertThat(res.sigunguCode()).isNull();
    }
}
// 이 테스트의 역할: UserProfileService의 모든 분기를 결정적으로 검증.
// Step 3·4 재설계 — 신규 스키마(sidoCode/sigunguCode + Enum, boolean 4종) 적용.
// REGION 검증 로직 자체 (sidoCode 유효성, 시군구-시도 일관성) 테스트는 Step 9에서 보강 예정.
