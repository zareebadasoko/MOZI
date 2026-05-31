package com.mozi.backend.global.seed;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 시드 어댑터 공통 파싱 헬퍼.
 *
 * 4개 출처 어댑터에서 반복되는 String → Integer/LocalDate 변환을 한곳에 모아
 * 일관된 fallback(null + WARN 로그) 동작을 보장한다.
 */
@Slf4j
public final class SeedParseUtils {

    private SeedParseUtils() {}

    /**
     * "2026" 같은 연도 문자열을 Integer로 안전 파싱.
     *
     * 빈 문자열/null/parse 실패 시 null 반환 + WARN 로그.
     *
     * @param raw JSON에서 읽은 원본 문자열
     * @return 파싱된 연도 (실패 시 null)
     */
    public static Integer parseYear(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid support_year '{}', set null", raw);
            return null;
        }
    }

    /**
     * "2025-08-06" ISO 형식 LocalDate 안전 파싱.
     *
     * 빈 문자열/null/형식 불일치 시 null 반환 + WARN 로그.
     *
     * @param raw JSON에서 읽은 원본 문자열
     * @return 파싱된 LocalDate (실패 시 null)
     */
    public static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            log.warn("Invalid date '{}', set null", raw);
            return null;
        }
    }
}
// 이 클래스의 역할: 시드 데이터 파싱의 공통 fallback 동작을 한 곳에 응집.
// 비공식적 데이터(크롤링이라 비어있거나 형식 어긋난 경우 다수)를 안전하게 흡수.
