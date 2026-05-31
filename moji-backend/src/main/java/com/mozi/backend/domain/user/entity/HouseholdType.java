package com.mozi.backend.domain.user.entity;

/**
 * 사용자의 가구 형태 분류 enum.
 *
 * UserProfile.householdType 컬럼의 정규화된 값. As-Is 자유 텍스트(VARCHAR)에서
 * To-Be Enum으로 변경되며(USER_PROFILE_REDESIGN_PLAN §2-3), boolean 6종 중
 * is_living_alone·is_single_parent_grandparent 2종이 본 enum 값으로 흡수된다.
 *
 * 정책 메모:
 *  - 옵션 C 채택으로 검색(GET /api/welfares)에서는 이 enum이 사용되지 않는다.
 *    챗봇 컨텍스트 전용 — 챗봇 전송 시 {@link #label()} 한글값으로 변환해 전달.
 *  - {@code LIVING_ALONE}이 As-Is의 isLivingAlone=true 상태를 대체한다.
 *  - {@code GRANDPARENT_GRANDCHILD}이 As-Is의 isSingleParentGrandparent=true 중
 *    조손 케이스를 대체한다. 한부모(편모/편부) 케이스는 별도로 표현되지 않고
 *    WITH_CHILDREN으로 단순화되었다 — 노년층 타깃 특성상 한부모 시나리오는
 *    조부모-손주 동거가 압도적이라는 USER_FLOW 시드 분포에 따른 결정.
 *  - 화면 라벨은 노인 친화 표현(평서문 + 괄호 보조). enum 상수명은 영문 고정.
 *
 * Step 2는 신규 추가만 수행하며, UserProfile 엔티티/DTO/챗봇 매핑 연결은
 * Step 3-4·Step 7에서 진행한다.
 */
public enum HouseholdType {

    LIVING_ALONE("혼자 살아요 (독거)"),
    COUPLE("배우자와 둘이 살아요"),
    WITH_CHILDREN("자녀와 함께 살아요"),
    GRANDPARENT_GRANDCHILD("손주를 키우고 있어요 (조손)"),
    OTHER("그 외");

    private final String label;

    HouseholdType(String label) {
        this.label = label;
    }

    /**
     * 화면 노출·챗봇 전송용 한글 라벨.
     *
     * 챗봇 서버(외부 LLM/RAG)는 한글 라벨을 그대로 컨텍스트에 활용하므로
     * 본 메서드의 반환값이 외부 계약의 일부가 된다. 변경 시 챗봇 팀과 협의 필요.
     *
     * @return 노년층 친화적 한글 라벨 (예: "혼자 살아요 (독거)", "그 외")
     */
    public String label() {
        return label;
    }
}
// 이 enum의 역할: 가구 형태의 정규화된 분류값 + 한글 라벨 단일 출처.
// As-Is의 isLivingAlone·isSingleParentGrandparent 2개 boolean이 본 enum 값으로 흡수됨 (§2-1).
