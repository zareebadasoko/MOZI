package com.mozi.backend.global.seed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mozi.backend.domain.welfare.entity.WelfareCentral;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 복지로 중앙부처 JSON(seed-data/welfare-crawled/full/central.json)을 파싱해
 * WelfareCentral 엔티티 + 카테고리 raw 문자열 트리플렛 리스트로 변환.
 *
 * 본 어댑터는 변환만 책임 — DB 저장과 카테고리 매핑은 WelfareSeedLoader가
 * 호출 후 통합 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfareCentralAdapter {

    private static final String SEED_PATH = "seed-data/welfare-crawled/full/central.json";

    private final ObjectMapper objectMapper;

    /**
     * central.json을 파싱해 시드 row 리스트로 반환.
     *
     * @return 각 row를 (엔티티, 관심주제 raw, 가구상황 raw)로 묶은 리스트
     */
    public List<WelfareSeedRow> parse() {
        FileSystemResource resource = new FileSystemResource(SEED_PATH);
        try (InputStream is = resource.getInputStream()) {
            CentralRow[] rows = objectMapper.readValue(is, CentralRow[].class);
            List<WelfareSeedRow> result = List.of(rows).stream()
                    .map(this::toSeedRow)
                    .toList();
            log.info("Parsed {} central rows from {}", result.size(), SEED_PATH);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse " + SEED_PATH, e);
        }
    }

    private WelfareSeedRow toSeedRow(CentralRow r) {
        WelfareCentral entity = WelfareCentral.builder()
                .id(r.id)
                .title(r.title)
                .summary(r.summary)
                .organizationName(r.organizationName)
                .targetAudience(r.targetAudience)
                .applicationMethod(r.applicationMethod)
                .supportYear(SeedParseUtils.parseYear(r.supportYear))
                .supportType(r.supportType)
                .selectionCriteria(r.selectionCriteria)
                .supportDetails(r.supportDetails)
                .contactNumber(r.contactNumber)
                .detailUrl(r.detailUrl)
                .supportCycle(r.supportCycle)
                .processSteps(r.processSteps)
                .build();
        return new WelfareSeedRow(entity, r.interestThemeCode, r.householdStatusCode);
    }

    /**
     * central.json row의 JSON 매핑 record. 외부 노출 X.
     */
    private record CentralRow(
            @JsonProperty("ID") String id,
            String title,
            String summary,
            @JsonProperty("organization_name") String organizationName,
            @JsonProperty("target_audience") String targetAudience,
            @JsonProperty("application_method") String applicationMethod,
            @JsonProperty("interest_theme_code") String interestThemeCode,
            @JsonProperty("household_status_code") String householdStatusCode,
            @JsonProperty("support_year") String supportYear,
            @JsonProperty("support_type") String supportType,
            @JsonProperty("selection_criteria") String selectionCriteria,
            @JsonProperty("support_details") String supportDetails,
            @JsonProperty("contact_number") String contactNumber,
            @JsonProperty("detail_url") String detailUrl,
            @JsonProperty("support_cycle") String supportCycle,
            @JsonProperty("process_steps") String processSteps
    ) {}
}
// 이 클래스의 역할: central.json 파싱 + WelfareCentral 변환.
// support_year의 String → Integer 변환 같은 출처별 미세 차이는 SeedParseUtils의 공통 헬퍼 사용.
