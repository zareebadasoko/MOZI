package com.mozi.backend.domain.user.entity;

/**
 * 사용자의 소득 유형 분류 enum.
 *
 * UserProfile.incomeType 컬럼의 정규화된 값. As-Is 자유 텍스트(VARCHAR)에서
 * To-Be Enum으로 변경되며(USER_PROFILE_REDESIGN_PLAN §2-3), 노년층이 정확한
 * 용어를 모르더라도 라디오 형태로 선택하도록 유도한다.
 *
 * 정책 메모:
 *  - 옵션 C 채택으로 검색(GET /api/welfares)에서는 이 enum이 사용되지 않는다.
 *    챗봇 컨텍스트 전용 — 챗봇 전송 시 {@link #label()} 한글값으로 변환해 전달.
 *  - {@code UNKNOWN}은 PUT 옵션 C 정책(null 입력=무변경, 클리어 미지원) 하에서
 *    "선택을 되돌리는" 우회로 역할을 겸한다. 즉 잘못 선택한 사용자가 다시
 *    UNKNOWN으로 갱신해 "잘 모르는 상태"로 자기를 재라벨링할 수 있다.
 *  - 화면 라벨은 노인 친화 표현. enum 상수명은 영문 고정으로 시드/마이그레이션
 *    안정성을 확보한다.
 *
 * Step 2는 신규 추가만 수행하며, UserProfile 엔티티/DTO/챗봇 매핑 연결은
 * Step 3-4·Step 7에서 진행한다.
 */
public enum IncomeType {

    NATIONAL_BASIC_LIVING("기초생활수급자"),
    NEAR_POVERTY("차상위계층"),
    BASIC_PENSION("기초연금수급자"),
    GENERAL("해당 없음"),
    UNKNOWN("잘 모르겠어요");

    private final String label;

    IncomeType(String label) {
        this.label = label;
    }

    /**
     * 화면 노출·챗봇 전송용 한글 라벨.
     *
     * 챗봇 서버(외부 LLM/RAG)는 한글 라벨을 그대로 컨텍스트에 활용하므로
     * 본 메서드의 반환값이 외부 계약의 일부가 된다. 변경 시 챗봇 팀과 협의 필요.
     *
     * @return 노년층 친화적 한글 라벨 (예: "기초생활수급자", "잘 모르겠어요")
     */
    public String label() {
        return label;
    }
}
// 이 enum의 역할: 소득 유형의 정규화된 분류값 + 한글 라벨 단일 출처.
// 검색에선 사용되지 않고(옵션 C) 챗봇 컨텍스트 전용 — 라벨 변경 시 외부 영향 주의.
