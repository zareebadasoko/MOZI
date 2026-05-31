package com.mozi.backend.domain.user.entity;

import com.mozi.backend.domain.user.dto.UserProfileUpdateRequest;
import com.mozi.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 추천 알고리즘에 사용되는 사용자 맞춤 정보 엔티티.
 *
 * USER_PROFILE_REDESIGN_PLAN §2-1 To-Be 스키마 적용 (Step 3):
 *  - 행정구역: 자유 텍스트(sido/sigungu) → REGION 마스터 참조 코드(sidoCode/sigunguCode)
 *  - 소득·가구: 자유 텍스트 → IncomeType / HouseholdType Enum
 *  - boolean 6종 → 4종 (isLivingAlone·isSingleParentGrandparent 제거 — HouseholdType이 흡수)
 *
 * User와 1:1로 연결되며, 회원가입 시점이 아니라 사용자가 처음으로
 * 프로필을 입력하는 시점(첫 PUT /api/users/me/profile 호출)에 lazy로 생성.
 * 따라서 row 존재 여부 자체가 API_SPEC §4-1의 isCompleted 플래그 의미를 갖는다.
 *
 * 행정구역 FK 정책: USER_PROFILE은 REGION에 FK를 걸지 않는다 — "시도만 선택"
 * 케이스(sigunguCode=null + sidoCode 존재)를 단일 FK로 표현하기 까다롭기 때문.
 * 대신 애플리케이션 레벨에서 RegionRepository.existsBy*로 코드 유효성을 검증한다.
 *
 * boolean 4종은 모두 NOT NULL + default false (primitive boolean)로 두고,
 * 부분 갱신 시 null=무변경 / true·false=명시적 변경의 구분은 DTO 레이어에서 처리.
 */
@Entity
@Getter
@Table(
        name = "user_profile",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_profile_user_id", columnNames = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    // 법정동코드 앞 2자리 (예: "11"=서울특별시). REGION 마스터의 sido_code 참조 (FK 없음 — 앱 검증)
    @Column(name = "sido_code", length = 2)
    private String sidoCode;

    // 법정동코드 앞 5자리 (예: "11680"=강남구). sidoCode만 있고 sigunguCode=null인 "시도만 선택" 허용
    @Column(name = "sigungu_code", length = 5)
    private String sigunguCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "income_type", length = 30)
    private IncomeType incomeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "household_type", length = 30)
    private HouseholdType householdType;

    @Column(name = "is_disabled", nullable = false)
    private boolean isDisabled;

    @Column(name = "is_multi_child", nullable = false)
    private boolean isMultiChild;

    @Column(name = "is_multicultural_north_defector", nullable = false)
    private boolean isMulticulturalNorthDefector;

    @Column(name = "is_veteran", nullable = false)
    private boolean isVeteran;

    private UserProfile(User user) {
        this.user = user;
    }

    /**
     * User에 연결된 빈 프로필을 생성하는 정적 팩토리.
     *
     * 첫 PUT 호출 시 서비스 레이어가 호출. 모든 nullable 필드는 null,
     * boolean 필드는 primitive 기본값인 false로 초기화된 상태.
     *
     * @param user 이 프로필이 속할 사용자 (이미 영속화된 상태여야 cascade 동작)
     * @return user_id만 채워진 빈 프로필 엔티티
     */
    public static UserProfile emptyFor(User user) {
        return new UserProfile(user);
    }

    /**
     * 시드 데이터 적재 전용 정적 팩토리 — 모든 필드를 한 번에 채워 생성.
     *
     * Step 8의 UserSeedLoader가 시연 시나리오에 맞춘 가짜 사용자의 프로필을
     * 즉시 완성된 상태로 만들기 위해 사용한다. 일반 사용자 흐름(PUT 부분 갱신)은
     * {@link #applyUpdate(UserProfileUpdateRequest)}로 처리.
     *
     * 행정구역 코드는 본 메서드에서 검증하지 않는다. 시드 흐름은 신뢰 가능한
     * 입력으로 가정하고, 사용자 입력 경로(updateMyProfile)에서만 REGION 존재
     * 여부를 검증한다.
     *
     * @param user 영속화된 User
     * @param birthDate 생년월일 (nullable)
     * @param gender 성별 (M/F/NONE, nullable)
     * @param sidoCode 시도 코드 (예: "11") — nullable
     * @param sigunguCode 시군구 코드 (예: "11680") — nullable, sidoCode 있을 때만 의미
     * @param incomeType 소득 유형 enum (nullable)
     * @param householdType 가구 형태 enum (nullable) — 독거·조손 정보를 흡수
     * @param isDisabled 장애 여부
     * @param isMultiChild 다자녀 여부
     * @param isMulticulturalNorthDefector 다문화·탈북민 여부
     * @param isVeteran 보훈대상자 여부
     * @return 모든 필드가 채워진 UserProfile
     */
    public static UserProfile fullFor(User user,
                                      LocalDate birthDate, Gender gender,
                                      String sidoCode, String sigunguCode,
                                      IncomeType incomeType, HouseholdType householdType,
                                      boolean isDisabled, boolean isMultiChild,
                                      boolean isMulticulturalNorthDefector, boolean isVeteran) {
        UserProfile profile = new UserProfile(user);
        profile.birthDate = birthDate;
        profile.gender = gender;
        profile.sidoCode = sidoCode;
        profile.sigunguCode = sigunguCode;
        profile.incomeType = incomeType;
        profile.householdType = householdType;
        profile.isDisabled = isDisabled;
        profile.isMultiChild = isMultiChild;
        profile.isMulticulturalNorthDefector = isMulticulturalNorthDefector;
        profile.isVeteran = isVeteran;
        return profile;
    }

    /**
     * 부분 갱신 요청을 적용한다 (PATCH-like 동작).
     *
     * 각 필드는 단순 null/value 의미로 처리:
     *  - request 필드가 null (요청 본문에 키 없음 또는 명시적 null) → 무변경
     *  - value → 해당 값으로 갱신
     *
     * Jackson의 absent vs explicit null 구분 한계로 "필드 클리어" 동작은 지원하지 않는다.
     * 한 번 채워진 필드는 새 값이 들어와야 변경된다. boolean 4종은 Boolean(박싱) 타입으로
     * 받아 null=무변경 의미를 표현. IncomeType.UNKNOWN은 "잘 모르겠어요" 라벨로
     * 사실상 클리어 효과를 낼 수 있는 우회로 역할 (§2-3 참조).
     *
     * Setter 회피 정책상 외부에서 필드를 직접 set하지 않고 이 메서드를 통해서만
     * 갱신하도록 강제한다. 호출자(UserProfileService)는 이미 영속화된 엔티티를
     * 트랜잭션 안에서 호출하면 dirty checking으로 자동 UPDATE 발행.
     *
     * REGION 코드(sidoCode/sigunguCode)의 유효성은 본 메서드 호출 전 서비스 레이어에서
     * 검증한다 — 엔티티는 단순 매핑만 담당.
     *
     * @param request 부분 갱신 요청 (nullable 필드)
     */
    public void applyUpdate(UserProfileUpdateRequest request) {
        // null = 무변경 / value = 갱신. 단순 if 분기로 통일.
        if (request.birthDate() != null) this.birthDate = request.birthDate();
        if (request.gender() != null) this.gender = request.gender();
        if (request.sidoCode() != null) this.sidoCode = request.sidoCode();
        if (request.sigunguCode() != null) this.sigunguCode = request.sigunguCode();
        if (request.incomeType() != null) this.incomeType = request.incomeType();
        if (request.householdType() != null) this.householdType = request.householdType();
        // boolean 4종도 동일 — null=무변경, true/false=명시적 갱신
        if (request.isDisabled() != null) this.isDisabled = request.isDisabled();
        if (request.isMultiChild() != null) this.isMultiChild = request.isMultiChild();
        if (request.isMulticulturalNorthDefector() != null) this.isMulticulturalNorthDefector = request.isMulticulturalNorthDefector();
        if (request.isVeteran() != null) this.isVeteran = request.isVeteran();
    }
}
// 이 클래스의 역할: 추천에 쓰일 사용자 맞춤 조건 보관 (Step 3 재설계 — REGION 코드 + Enum 정규화).
// Lazy creation 정책: 회원가입 직후엔 row 없음 → GET 시 빈 객체 응답 + isCompleted=false.
// 첫 PUT 호출 시 emptyFor()로 row 생성 후 부분 갱신.
// User 삭제 시 cascade로 함께 삭제됨 (User.@OneToOne(cascade=ALL, orphanRemoval=true)).
