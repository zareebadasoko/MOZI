package com.mozi.backend.global.seed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mozi.backend.domain.region.entity.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 행정구역 JSON(`seed-data/region/region.json`)을 파싱해 Region 엔티티로 변환.
 *
 * WelfareLocalAdapter와 동일 패턴 — FileSystemResource로 working dir 기준 상대 경로 로드,
 * Jackson record 매핑으로 안전한 정형 파싱. 데이터는 행정안전부 법정동코드 기준
 * 17개 시도 + 약 229개 시군구(세종 1행 + 일반행정구 제외)로 정렬된 상태.
 *
 * 명칭(sidoName/sigunguName)은 WELFARE_LOCAL.region_name 크롤링 표기와 같은 풀네임
 * (예: "서울특별시", "강남구")로 두어 검색 LIKE 매칭에 일관성 확보.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegionSeedAdapter {

    private static final String SEED_PATH = "seed-data/region/region.json";

    private final ObjectMapper objectMapper;

    /**
     * region.json을 파싱해 Region 엔티티 리스트로 반환.
     *
     * @return 영속화 직전 상태의 Region 엔티티 목록 (id 미할당)
     */
    public List<Region> parse() {
        FileSystemResource resource = new FileSystemResource(SEED_PATH);
        try (InputStream is = resource.getInputStream()) {
            RegionRow[] rows = objectMapper.readValue(is, RegionRow[].class);
            List<Region> result = List.of(rows).stream()
                    .map(r -> Region.of(r.sidoCode, r.sidoName, r.sigunguCode, r.sigunguName))
                    .toList();
            log.info("Parsed {} region rows from {}", result.size(), SEED_PATH);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse " + SEED_PATH, e);
        }
    }

    /**
     * region.json 단일 row 매핑. 세종(sigunguCode/Name=null) 허용.
     */
    private record RegionRow(
            @JsonProperty("sidoCode") String sidoCode,
            @JsonProperty("sidoName") String sidoName,
            @JsonProperty("sigunguCode") String sigunguCode,
            @JsonProperty("sigunguName") String sigunguName
    ) {}
}
// 이 클래스의 역할: region.json 파싱 + Region 엔티티 변환.
// 229행 정도라 한 번에 메모리 로드 가능. WelfareLocalAdapter 패턴 그대로 답습.
