package com.mozi.backend.global.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 페이지네이션 응답 래퍼.
 *
 * Spring Data의 `Page<T>` 객체를 그대로 직렬화하면 pageable/sort 관련 필드가 다수 노출되어
 * 응답이 무거워지므로, API_SPEC에 정의된 다섯 필드(items/page/size/totalCount/hasNext)만
 * 노출하는 경량 record로 변환한다. 모든 페이지 응답이 동일한 키 구조를 갖도록 통일하는 효과도 있다.
 *
 * @param items 현재 페이지의 아이템 목록 (이미 DTO로 변환된 상태)
 * @param page 0-based 페이지 번호
 * @param size 페이지 크기
 * @param totalCount 전체 row 수 (모든 페이지 합산)
 * @param hasNext 다음 페이지 존재 여부
 * @param <T> 아이템 DTO 타입
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalCount,
        boolean hasNext
) {

    /**
     * Spring `Page<E>`와 매핑 함수를 받아 PageResponse<T>로 변환한다.
     *
     * 엔티티/DTO 변환을 호출 측에서 람다로 주입받아 PageResponse가 도메인에 의존하지 않게 한다.
     *
     * @param page Spring Data가 반환한 Page 객체
     * @param mapper 엔티티(또는 중간 객체) → DTO 변환 람다
     * @param <E> Page 내부 요소 타입
     * @param <T> 응답 아이템 DTO 타입
     * @return 변환된 PageResponse
     */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.hasNext()
        );
    }
}
// 이 record의 역할: 모든 페이지 응답의 통일된 형태 제공 + Spring Page의 무거운 직렬화 회피.
// from() 정적 팩토리로 도메인 변환 책임을 호출 측 람다에 위임해 타입 의존성 최소화.
