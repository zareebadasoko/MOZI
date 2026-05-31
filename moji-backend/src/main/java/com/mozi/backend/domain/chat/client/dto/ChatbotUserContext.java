package com.mozi.backend.domain.chat.client.dto;

import com.mozi.backend.domain.user.entity.Gender;
import com.mozi.backend.domain.user.entity.HouseholdType;
import com.mozi.backend.domain.user.entity.IncomeType;
import com.mozi.backend.domain.user.entity.UserProfile;

/**
 * 챗봇 서버로 전송할 사용자 컨텍스트 record (외부 API 페이로드).
 *
 * UserProfile 엔티티를 외부로 그대로 노출하지 않기 위해 별도 DTO로 매핑한다.
 * 사용자가 프로필을 입력하지 않았으면 `profile = null`로 전송 — 챗봇 LLM이
 * 컨텍스트 없이도 일반적 답변을 줄 수 있도록 처리 권장 (CHATBOT_API_CONTRACT 참조).
 *
 * Step 7 본격 매핑 (USER_PROFILE_REDESIGN §2-5):
 *  - 행정구역: sidoCode/sigunguCode → sidoName/sigunguName 한글 라벨로 변환 전송
 *  - 소득·가구: IncomeType/HouseholdType enum → enum.label() 한글 라벨
 *  - 나이: birthDate(YYYY-MM-DD) → age(만 나이 정수). 개인정보 최소화 원칙상 birthDate 미전송
 *  - boolean 4종 (isDisabled/isMultiChild/isMulticulturalNorthDefector/isVeteran)
 *
 * REGION 마스터 lookup으로 sidoName/sigunguName을 변환하므로 정적 from()이 아니라
 * ChatService에서 미리 라벨/나이를 계산해 본 record에 주입하는 방식으로 협력한다
 * (Region 시드 미적재 상태에서는 sidoName/sigunguName이 null로 전달됨).
 *
 * `null`인 필드는 Jackson 기본 직렬화로 JSON에 그대로 포함됨 (챗봇 측에서 누락 처리).
 *
 * @param userId MOZI DB의 사용자 PK (Long)
 * @param profile 신규 스키마 프로필 (null 가능 — 프로필 미입력 시)
 */
public record ChatbotUserContext(
        Long userId,
        Profile profile
) {

    /**
     * UserProfile 엔티티 + 사전 변환된 라벨/나이 → 외부 페이로드 변환.
     *
     * sidoName/sigunguName/age는 ChatService가 REGION 마스터 lookup과 Clock 기반
     * 만 나이 계산을 수행해 미리 변환한 값을 전달한다. 본 메서드는 단순 매핑.
     *
     * @param userId 사용자 PK
     * @param userProfile 영속화된 UserProfile (null 아님)
     * @param sidoName 변환된 시도 한글 이름 (예: "서울특별시"). REGION 미존재면 null
     * @param sigunguName 변환된 시군구 한글 이름 (예: "강남구"). 시도만 선택했거나 미존재면 null
     * @param age 만 나이 정수 (birthDate가 null이면 null)
     * @return profile 채워진 ChatbotUserContext
     */
    public static ChatbotUserContext from(Long userId, UserProfile userProfile,
                                          String sidoName, String sigunguName, Integer age) {
        return new ChatbotUserContext(
                userId,
                new Profile(
                        age,
                        userProfile.getGender(),
                        sidoName,
                        sigunguName,
                        labelOf(userProfile.getIncomeType()),
                        labelOf(userProfile.getHouseholdType()),
                        userProfile.isDisabled(),
                        userProfile.isMultiChild(),
                        userProfile.isMulticulturalNorthDefector(),
                        userProfile.isVeteran()
                )
        );
    }

    /**
     * 프로필이 없는 사용자(가입 직후·미입력)를 위한 컨텍스트 — profile=null.
     *
     * @param userId 사용자 PK
     * @return userId만 채워진 ChatbotUserContext
     */
    public static ChatbotUserContext anonymous(Long userId) {
        return new ChatbotUserContext(userId, null);
    }

    /**
     * IncomeType enum → 한글 라벨 변환 (null-safe).
     *
     * @param incomeType IncomeType enum (null 가능)
     * @return 한글 라벨 또는 null
     */
    private static String labelOf(IncomeType incomeType) {
        return incomeType != null ? incomeType.label() : null;
    }

    /**
     * HouseholdType enum → 한글 라벨 변환 (null-safe).
     *
     * @param householdType HouseholdType enum (null 가능)
     * @return 한글 라벨 또는 null
     */
    private static String labelOf(HouseholdType householdType) {
        return householdType != null ? householdType.label() : null;
    }

    /**
     * 챗봇 서버로 전송할 프로필 페이로드 (Step 7 본격 매핑).
     *
     * Wire format: 행정구역/소득/가구 모두 한글 라벨, 나이는 만 나이 정수.
     * 챗봇 LLM이 한국어 컨텍스트를 그대로 활용 가능하도록 설계.
     *
     * @param age 만 나이 (예: 78). birthDate 미입력 시 null
     * @param gender 성별 (M/F/NONE)
     * @param sidoName 거주 시도 한글 이름 (예: "서울특별시"). REGION 미매칭 시 null
     * @param sigunguName 거주 시군구 한글 이름 (예: "강남구"). 시도만 선택 또는 미매칭 시 null
     * @param incomeType 소득 유형 한글 라벨 (예: "기초연금수급자")
     * @param householdType 가구 형태 한글 라벨 (예: "혼자 살아요 (독거)")
     * @param isDisabled 장애 여부
     * @param isMultiChild 다자녀 여부
     * @param isMulticulturalNorthDefector 다문화·탈북민 여부
     * @param isVeteran 보훈대상자 여부
     */
    public record Profile(
            Integer age,
            Gender gender,
            String sidoName,
            String sigunguName,
            String incomeType,
            String householdType,
            boolean isDisabled,
            boolean isMultiChild,
            boolean isMulticulturalNorthDefector,
            boolean isVeteran
    ) {
    }
}
// 이 record의 역할: 챗봇 서버 외부 페이로드 정의 — UserProfile 노출 회피 + 외부 계약 안정성.
// Step 7 본격 매핑 — 행정구역 코드→한글 라벨, birthDate→age, enum→label 모두 적용.
// 라벨/나이 변환은 ChatService 책임 (REGION 마스터·Clock 의존)이고 본 record는 단순 매핑만 담당.
