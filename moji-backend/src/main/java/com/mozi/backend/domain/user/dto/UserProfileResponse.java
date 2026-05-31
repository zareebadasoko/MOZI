package com.mozi.backend.domain.user.dto;

import com.mozi.backend.domain.user.entity.Gender;
import com.mozi.backend.domain.user.entity.HouseholdType;
import com.mozi.backend.domain.user.entity.IncomeType;
import com.mozi.backend.domain.user.entity.UserProfile;

import java.time.LocalDate;

/**
 * 프로필 조회 응답 DTO.
 *
 * Step 3 재설계 반영 — 행정구역 코드(sidoCode/sigunguCode) + Enum 필드.
 * 한글 라벨은 enum의 label()을 통해 클라이언트에서 추출하거나 별도 라벨링 API
 * (Step 5 영역)로 처리한다. 본 응답은 코드 값을 그대로 노출.
 *
 * `isCompleted`는 row 존재 여부를 의미한다 — 한 번이라도 PUT을 호출한 적이 있으면 true,
 * 가입 직후처럼 row가 없으면 false. 클라이언트는 false면 강제 입력 흐름으로,
 * true면 카드 + 수정 버튼 흐름으로 분기 가능.
 *
 * boolean 4종을 응답에서 Boolean(박싱)으로 둔 이유: row가 없을 때 모든 필드를
 * null로 표현해야 응답에서 자연스럽게 누락(@JsonInclude(NON_NULL))되기 때문.
 *
 * @param isCompleted 프로필 row 존재 여부
 * @param birthDate 생년월일 (nullable)
 * @param gender 성별 (M/F/NONE, nullable)
 * @param sidoCode 시도 코드 (예: "11", nullable)
 * @param sigunguCode 시군구 코드 (예: "11680", nullable)
 * @param incomeType 소득 유형 Enum (nullable)
 * @param householdType 가구 형태 Enum (nullable)
 * @param isDisabled 장애 여부 (row 없으면 null)
 * @param isMultiChild 다자녀 여부
 * @param isMulticulturalNorthDefector 다문화·탈북민 여부
 * @param isVeteran 보훈대상자 여부
 */
public record UserProfileResponse(
        boolean isCompleted,
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

    /**
     * 프로필 row가 없을 때 사용할 빈 응답 — 모든 필드 null + isCompleted=false.
     *
     * @return 빈 UserProfileResponse
     */
    public static UserProfileResponse empty() {
        return new UserProfileResponse(false, null, null, null, null, null, null,
                null, null, null, null);
    }

    /**
     * UserProfile 엔티티에서 응답 DTO로 변환.
     *
     * row가 존재한다는 사실 자체가 isCompleted=true의 조건이라 항상 true를 채운다.
     * boolean 4종은 자동 박싱(boolean → Boolean)으로 응답에 명시적 true/false 노출.
     *
     * @param profile 영속화된 UserProfile (null이면 안 됨)
     * @return profile의 모든 필드를 채운 응답 DTO
     */
    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
                true,
                profile.getBirthDate(),
                profile.getGender(),
                profile.getSidoCode(),
                profile.getSigunguCode(),
                profile.getIncomeType(),
                profile.getHouseholdType(),
                profile.isDisabled(),
                profile.isMultiChild(),
                profile.isMulticulturalNorthDefector(),
                profile.isVeteran()
        );
    }
}
// 이 record의 역할: 프로필 조회/갱신 응답 페이로드 (Step 3 재설계 — 코드/Enum 노출).
// row 부재(empty)와 존재(from)를 두 정적 팩토리로 명확히 구분해 호출 측 분기를 단순화.
