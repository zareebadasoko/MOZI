package com.mozi.backend.global.seed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mozi.backend.domain.welfare.entity.WelfareLocal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 복지로 지자체 JSON(local.json)을 파싱해 WelfareLocal 엔티티로 변환.
 *
 * Central과 컬럼 차이:
 * - region_name 필드 추가 (지역 검색 인덱스 대상)
 * - contact_number → contact (다른 컬럼명, 더 길어 100자)
 * - process_steps 없음
 *
 * local.json은 약 1.9MB로 가장 큰 파일이지만 한 번에 메모리 로드 가능 수준.
 * Hibernate batch_size=50 설정으로 INSERT는 효율적으로 처리됨.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfareLocalAdapter {

    private static final String SEED_PATH = "seed-data/welfare-crawled/full/local.json";

    private final ObjectMapper objectMapper;

    /**
     * local.json을 파싱해 시드 row 리스트로 반환.
     *
     * @return 각 row를 (엔티티, 관심주제 raw, 가구상황 raw)로 묶은 리스트
     */
    public List<WelfareSeedRow> parse() {
        FileSystemResource resource = new FileSystemResource(SEED_PATH);
        try (InputStream is = resource.getInputStream()) {
            LocalRow[] rows = objectMapper.readValue(is, LocalRow[].class);
            List<WelfareSeedRow> result = List.of(rows).stream()
                    .map(this::toSeedRow)
                    .toList();
            log.info("Parsed {} local rows from {}", result.size(), SEED_PATH);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse " + SEED_PATH, e);
        }
    }

    private WelfareSeedRow toSeedRow(LocalRow r) {
        WelfareLocal entity = WelfareLocal.builder()
                .id(r.id)
                .title(r.title)
                .summary(r.summary)
                .organizationName(r.organizationName)
                .targetAudience(r.targetAudience)
                .applicationMethod(r.applicationMethod)
                .regionName(r.regionName)
                .supportYear(SeedParseUtils.parseYear(r.supportYear))
                .supportType(r.supportType)
                .selectionCriteria(r.selectionCriteria)
                .supportDetails(r.supportDetails)
                .contact(r.contact)
                .detailUrl(r.detailUrl)
                .supportCycle(r.supportCycle)
                .build();
        return new WelfareSeedRow(entity, r.interestThemeCode, r.householdStatusCode);
    }

    private record LocalRow(
            @JsonProperty("ID") String id,
            String title,
            String summary,
            @JsonProperty("organization_name") String organizationName,
            @JsonProperty("target_audience") String targetAudience,
            @JsonProperty("application_method") String applicationMethod,
            @JsonProperty("interest_theme_code") String interestThemeCode,
            @JsonProperty("household_status_code") String householdStatusCode,
            @JsonProperty("region_name") String regionName,
            @JsonProperty("support_year") String supportYear,
            @JsonProperty("support_type") String supportType,
            @JsonProperty("selection_criteria") String selectionCriteria,
            @JsonProperty("support_details") String supportDetails,
            String contact,
            @JsonProperty("detail_url") String detailUrl,
            @JsonProperty("support_cycle") String supportCycle
    ) {}
}
// 이 클래스의 역할: local.json 파싱 + WelfareLocal 변환.
// 1.9MB 파일이지만 Jackson이 한 번에 처리 가능. 배치 INSERT는 batch_size=50 적용.
