package com.mozi.backend.domain.region.dto;

import com.mozi.backend.domain.region.entity.Region;

/**
 * Region 평면 응답 DTO.
 *
 * GET /api/regions?sido={code} 응답에서 사용. 시도가 이미 고정된 컨텍스트라
 * 응답 단위가 시군구 1행이 자연스러워 평면 record로 둔다. 시도별 그루핑
 * 응답은 별도 {@link SidoGroupDto}를 사용.
 *
 * BaseEntity의 createdAt/updatedAt은 클라이언트에 불필요하므로 노출하지 않는다.
 *
 * @param sidoCode 시도 코드 (예: "11")
 * @param sidoName 시도 풀네임 (예: "서울특별시")
 * @param sigunguCode 시군구 코드 (예: "11680"). 시도만 단일 행으로 갖는 케이스(세종)는 null 가능
 * @param sigunguName 시군구 풀네임 (예: "강남구"). sigunguCode가 null이면 null 가능
 */
public record RegionDto(
        String sidoCode,
        String sidoName,
        String sigunguCode,
        String sigunguName
) {

    /**
     * Region 엔티티 → 평면 응답 DTO.
     *
     * @param region 영속화된 Region (id 채워진 상태)
     * @return sidoCode/sidoName/sigunguCode/sigunguName 4필드만 추출한 DTO
     */
    public static RegionDto from(Region region) {
        return new RegionDto(
                region.getSidoCode(),
                region.getSidoName(),
                region.getSigunguCode(),
                region.getSigunguName()
        );
    }
}
// 이 record의 역할: 시도가 단일 컨텍스트인 시군구 목록 응답 형식.
// 시도별 그루핑이 필요한 GET /api/regions(?sido 없음)는 SidoGroupDto를 별도로 사용한다.
