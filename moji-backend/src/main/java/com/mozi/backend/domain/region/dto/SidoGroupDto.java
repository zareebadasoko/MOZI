package com.mozi.backend.domain.region.dto;

import java.util.List;

/**
 * 시도 + 산하 시군구 묶음 응답 DTO.
 *
 * GET /api/regions(?sido 미지정)에서 사용. cascading select(시도 → 시군구)를
 * 프론트가 한 번의 요청으로 채울 수 있도록 시도 단위로 묶어 응답한다.
 * 시도별로 sigungus 배열을 내포하므로 평면 응답({@link RegionDto})과 구분된다.
 *
 * @param sidoCode 시도 코드 (예: "11")
 * @param sidoName 시도 풀네임 (예: "서울특별시")
 * @param sigungus 해당 시도의 시군구 목록 (정렬은 Service 책임)
 */
public record SidoGroupDto(
        String sidoCode,
        String sidoName,
        List<Sigungu> sigungus
) {

    /**
     * 시군구 1행을 단일 그룹 안에서 표현하는 내부 record.
     *
     * 시도 정보는 부모 그룹이 들고 있으므로 시군구 단위에서는 코드/이름만 노출.
     *
     * @param code 시군구 코드 (예: "11680"). 세종 단일 행 케이스는 null 가능
     * @param name 시군구 풀네임 (예: "강남구")
     */
    public record Sigungu(String code, String name) {}

    /**
     * 시도 그룹 생성용 정적 팩토리.
     *
     * Service의 LinkedHashMap 그루핑 결과를 List 형태로 묶을 때 호출.
     *
     * @param sidoCode 시도 코드
     * @param sidoName 시도 풀네임
     * @param sigungus 해당 시도의 시군구 목록 (이미 정렬된 상태로 전달)
     * @return 시도 단위 응답 그룹
     */
    public static SidoGroupDto of(String sidoCode, String sidoName, List<Sigungu> sigungus) {
        return new SidoGroupDto(sidoCode, sidoName, sigungus);
    }
}
// 이 record의 역할: cascading select 친화적인 시도 묶음 응답 형식.
// 평면 시군구 응답이 필요한 ?sido={code} 케이스는 RegionDto를 사용하므로 두 DTO가 서로 보완 관계.
