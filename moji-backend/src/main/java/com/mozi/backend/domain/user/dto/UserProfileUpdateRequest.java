package com.mozi.backend.domain.user.dto;

import com.mozi.backend.domain.user.entity.Gender;
import com.mozi.backend.domain.user.entity.HouseholdType;
import com.mozi.backend.domain.user.entity.IncomeType;

import java.time.LocalDate;

/**
 * 프로필 부분 갱신(PUT) 요청 DTO. PATCH-like 동작을 위해 모든 필드가 nullable.
 *
 * Step 3 재설계 반영:
 *  - sido/sigungu (자유 텍스트) → sidoCode/sigunguCode (REGION 마스터 참조 코드)
 *  - incomeType/householdType (자유 텍스트) → IncomeType/HouseholdType Enum
 *  - boolean 6종 → 4종 (isLivingAlone·isSingleParentGrandparent 제거)
 *
 * 의미 단순화:
 *  - **null** (요청 본문에 키가 없거나 명시적 null): 해당 필드 변경하지 않음
 *  - **value**: 해당 값으로 갱신
 *
 * Jackson record + Optional<T> 조합은 absent와 explicit null을 모두 Optional.empty()로
 * 매핑해 구분이 불가능하므로, MVP 단계에서는 두 케이스를 모두 "무변경"으로 단순화한다.
 * "필드 클리어" 동작은 본 API에서 지원하지 않는다. 향후 명시적 클리어가 필요해지면
 * JsonNullable 라이브러리 도입을 검토. {@link IncomeType#UNKNOWN}은 우회 클리어 역할.
 *
 * boolean 4종은 primitive로 받으면 null 표현 불가능하므로 Boolean(박싱)으로 받음.
 *
 * @param birthDate 생년월일
 * @param gender 성별 (M/F/NONE)
 * @param sidoCode 시도 코드 (예: "11") — REGION 검증 통과 시에만 허용
 * @param sigunguCode 시군구 코드 (예: "11680") — sidoCode와 함께 검증
 * @param incomeType 소득 유형 Enum (예: BASIC_PENSION)
 * @param householdType 가구 형태 Enum (예: LIVING_ALONE — 독거 흡수)
 * @param isDisabled 장애 여부
 * @param isMultiChild 다자녀 여부
 * @param isMulticulturalNorthDefector 다문화·탈북민 여부
 * @param isVeteran 보훈대상자 여부
 */
public record UserProfileUpdateRequest(
        LocalDate birthDate,
        Gender gender,
        String sidoCode,
        String sigunguCode,
        IncomeType incomeType,
        HouseholdType householdType,
        Boolean isDisabled,
        Boolean isMultiChild,
        Boolean isMulticulturalNorthDefector,
        Boolean isVeteran
) {
}
// 이 record의 역할: 부분 갱신 요청을 nullable 필드로 표현 (null=무변경, value=갱신).
// Step 3 재설계로 행정구역·enum 정규화. boolean 6→4 축소.
// 모든 필드 누락도 허용 — 의미적으로는 "변경 없음" 호출이라 Validation 오류로 보지 않는다.
