package com.mozi.backend.global.seed;

import com.mozi.backend.domain.welfare.entity.WelfareCommon;

/**
 * 4개 출처 어댑터의 통합 반환 타입.
 *
 * 어댑터는 (엔티티, 관심 주제 raw, 가구 상황 raw) 트리플렛 리스트를 반환하고,
 * WelfareSeedLoader가 엔티티는 saveAll 처리, 두 raw 문자열은 콤마 분리 후
 * Category lookup → WelfareCategory 매핑으로 처리한다.
 *
 * 카테고리 raw 문자열을 어댑터가 파싱하지 않는 이유: 4개 출처 모두 동일한
 * 콤마 분리 포맷이라 WelfareSeedLoader 한 곳에서 통합 처리하는 게 단순.
 *
 * @param entity 영속화 전 자식 엔티티 (Central/Local/Private/Seoul 중 하나)
 * @param interestThemeRaw 콤마 분리 텍스트 또는 null/빈 (예: "보호·돌봄,안전·위기")
 * @param householdStatusRaw 콤마 분리 텍스트 또는 null/빈 (예: "장애인,저소득")
 */
public record WelfareSeedRow(
        WelfareCommon entity,
        String interestThemeRaw,
        String householdStatusRaw
) {
}
// 이 record의 역할: 어댑터 ↔ Loader 간 데이터 전달 통일 형태.
// 4개 어댑터 모두 List<WelfareSeedRow> 반환 → Loader는 단일 로직으로 매핑 처리.
