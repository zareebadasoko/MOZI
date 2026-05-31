package com.mozi.backend.global.seed;

import com.mozi.backend.domain.user.entity.Gender;
import com.mozi.backend.domain.user.entity.HouseholdType;
import com.mozi.backend.domain.user.entity.IncomeType;
import com.mozi.backend.domain.user.entity.User;
import com.mozi.backend.domain.user.entity.UserProfile;
import com.mozi.backend.domain.user.repository.UserProfileRepository;
import com.mozi.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 시연용 가짜 사용자 7명 + UserProfile을 시드 적재.
 *
 * Step 3 재설계 반영 (임시): 새 스키마(sidoCode/sigunguCode + Enum) 값으로
 * 기존 시드 데이터를 매핑. 행정표준코드 사용 — REGION 시드가 비어 있는 시점이라
 * 실제 검증은 통과(시드 흐름은 검증 우회). REGION 시드 본격 적재 + 시드 데이터
 * 본격 정리는 Step 8에서 진행한다.
 *
 * 비밀번호는 모두 동일하게 "test1234" → BCrypt 해싱 후 저장. 시연 시
 * 발표자가 이 비번으로 로그인해 시나리오 A/B/C를 시연할 수 있다.
 *
 * UserProfile은 원래 Lazy creation(첫 PUT 호출 시 생성) 정책이지만, 시연
 * 즉시 동작이 목적인 시드 단계에선 미리 완성된 프로필을 함께 생성한다.
 *
 * 7명 구성 (USER_FLOW.md 시연 시나리오 기반):
 *   1. senior01 — 시나리오 A: 78세, 서울 강남구, 기초연금, 독거
 *   2. senior02 — 시나리오 B: 72세, 경기 의정부, 보훈, 부부
 *   3. senior03 — 시나리오 C: 67세, 서울 송파구, 장애
 *   4. senior04 — 한부모·조손
 *   5. senior05 — 다자녀(조손)
 *   6. senior06 — 다문화·탈북민
 *   7. senior07 — 일반(대조군)
 *
 * 이메일/비번: senior01~07@mozi.test / test1234
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSeedLoader {

    private static final String SHARED_PASSWORD = "test1234";
    private static final String SENTINEL_EMAIL = "senior01@mozi.test";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    // BCryptPasswordEncoder를 빈으로 등록하지 않고 시드 시점에만 직접 생성 — Phase 3 진입 시
    // spring-boot-starter-security가 PasswordEncoder 빈을 자동 등록하는 것과의 충돌 회피.
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 가짜 사용자 7명 + UserProfile 적재. 첫 사용자(senior01) 존재 시 skip.
     */
    @Transactional
    public void loadIfEmpty() {
        if (userRepository.existsByEmail(SENTINEL_EMAIL)) {
            log.info("User seed skip — already loaded (sentinel found: {})", SENTINEL_EMAIL);
            return;
        }

        String hashed = passwordEncoder.encode(SHARED_PASSWORD);
        for (UserSeed seed : SEEDS) {
            User user = User.of(seed.email(), hashed);
            userRepository.save(user);

            UserProfile profile = UserProfile.fullFor(
                    user,
                    seed.birthDate(), seed.gender(),
                    seed.sidoCode(), seed.sigunguCode(),
                    seed.incomeType(), seed.householdType(),
                    seed.isDisabled(),
                    seed.isMultiChild(),
                    seed.isMulticulturalNorthDefector(),
                    seed.isVeteran()
            );
            userProfileRepository.save(profile);
        }
        // 비밀번호 평문은 로그에 출력하지 않음 (CLAUDE.md §5-4). 시연용 시드 자격증명은 SHARED_PASSWORD 상수 또는 docs 참조.
        log.info("User seed done: {} users + profiles", SEEDS.size());
    }

    /**
     * 7명 시드 데이터 정의 — USER_FLOW.md 시연 시나리오 매칭 (Step 3 신규 스키마).
     *
     * 행정구역 코드는 법정동코드 앞 2/5자리:
     *  - 서울특별시: 11, 강남구 11680, 송파구 11710, 노원구 11350, 영등포구 11560
     *  - 경기도: 41, 의정부시 41150
     *  - 부산광역시: 26, 해운대구 26350
     *  - 인천광역시: 28, 부평구 28237
     *
     * 독거(LIVING_ALONE)·조손(GRANDPARENT_GRANDCHILD)은 HouseholdType이 흡수.
     * senior04(한부모)는 WITH_CHILDREN으로 단순화(노년층 타깃 특성).
     * senior07(소득 미지정)은 incomeType=null 유지 — UNKNOWN과 null의 의미 차이를 보존.
     */
    private static final List<UserSeed> SEEDS = List.of(
            new UserSeed("senior01@mozi.test",
                    LocalDate.of(1948, 3, 15), Gender.F,
                    "11", "11680",
                    IncomeType.BASIC_PENSION, HouseholdType.LIVING_ALONE,
                    false, false, false, false),
            new UserSeed("senior02@mozi.test",
                    LocalDate.of(1954, 7, 22), Gender.M,
                    "41", "41150",
                    IncomeType.BASIC_PENSION, HouseholdType.COUPLE,
                    false, false, false, true),
            new UserSeed("senior03@mozi.test",
                    LocalDate.of(1959, 11, 3), Gender.F,
                    "11", "11710",
                    IncomeType.BASIC_PENSION, HouseholdType.LIVING_ALONE,
                    true, false, false, false),
            new UserSeed("senior04@mozi.test",
                    LocalDate.of(1961, 5, 10), Gender.F,
                    "11", "11350",
                    IncomeType.BASIC_PENSION, HouseholdType.WITH_CHILDREN,
                    false, false, false, false),
            new UserSeed("senior05@mozi.test",
                    LocalDate.of(1956, 8, 28), Gender.M,
                    "26", "26350",
                    IncomeType.BASIC_PENSION, HouseholdType.GRANDPARENT_GRANDCHILD,
                    false, true, false, false),
            new UserSeed("senior06@mozi.test",
                    LocalDate.of(1958, 1, 19), Gender.F,
                    "11", "11560",
                    IncomeType.BASIC_PENSION, HouseholdType.COUPLE,
                    false, false, true, false),
            new UserSeed("senior07@mozi.test",
                    LocalDate.of(1961, 4, 6), Gender.M,
                    "28", "28237",
                    null, HouseholdType.COUPLE,
                    false, false, false, false)
    );

    /**
     * 시드 사용자 정의용 내부 record. 외부 노출 X.
     */
    private record UserSeed(
            String email,
            LocalDate birthDate, Gender gender,
            String sidoCode, String sigunguCode,
            IncomeType incomeType, HouseholdType householdType,
            boolean isDisabled, boolean isMultiChild,
            boolean isMulticulturalNorthDefector, boolean isVeteran
    ) {}
}
// 이 클래스의 역할: 시연용 7명 가짜 사용자 적재.
// Step 3 재설계 적용 — 행정구역 코드 + Enum 값으로 시드 데이터 매핑.
// 비밀번호는 BCrypt 해싱 1번만 수행 후 7명에게 동일하게 적용 (해싱 비용 절감).
// senior01의 존재 여부로 idempotent 판단 — 일반 사용자가 가입한 경우와 분리.
