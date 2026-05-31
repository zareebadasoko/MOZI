package com.mozi.backend.global.seed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mozi.backend.domain.welfare.entity.WelfarePrivate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 복지로 민간 JSON(private.json)을 파싱해 WelfarePrivate 엔티티로 변환.
 *
 * Central/Local과 가장 차이가 큼:
 * - ID prefix가 BOK00000xxx (복지로 자체 민간 ID)
 * - start_date / end_date LocalDate (다른 출처엔 없음)
 * - contact_email, required_documents 추가
 * - support_year, selection_criteria, process_steps, support_cycle, support_type 없음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfarePrivateAdapter {

    private static final String SEED_PATH = "seed-data/welfare-crawled/full/private.json";

    private final ObjectMapper objectMapper;

    /**
     * private.json을 파싱해 시드 row 리스트로 반환.
     *
     * @return 각 row를 (엔티티, 관심주제 raw, 가구상황 raw)로 묶은 리스트
     */
    public List<WelfareSeedRow> parse() {
        FileSystemResource resource = new FileSystemResource(SEED_PATH);
        try (InputStream is = resource.getInputStream()) {
            PrivateRow[] rows = objectMapper.readValue(is, PrivateRow[].class);
            List<WelfareSeedRow> result = List.of(rows).stream()
                    .map(this::toSeedRow)
                    .toList();
            log.info("Parsed {} private rows from {}", result.size(), SEED_PATH);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse " + SEED_PATH, e);
        }
    }

    private WelfareSeedRow toSeedRow(PrivateRow r) {
        WelfarePrivate entity = WelfarePrivate.builder()
                .id(r.id)
                .title(r.title)
                .summary(r.summary)
                .organizationName(r.organizationName)
                .targetAudience(r.targetAudience)
                .applicationMethod(r.applicationMethod)
                .startDate(SeedParseUtils.parseDate(r.startDate))
                .endDate(SeedParseUtils.parseDate(r.endDate))
                .supportDetails(r.supportDetails)
                .contactNumber(r.contactNumber)
                .detailUrl(r.detailUrl)
                .contactEmail(r.contactEmail)
                .requiredDocuments(r.requiredDocuments)
                .build();
        return new WelfareSeedRow(entity, r.interestThemeCode, r.householdStatusCode);
    }

    private record PrivateRow(
            @JsonProperty("ID") String id,
            String title,
            String summary,
            @JsonProperty("organization_name") String organizationName,
            @JsonProperty("target_audience") String targetAudience,
            @JsonProperty("application_method") String applicationMethod,
            @JsonProperty("interest_theme_code") String interestThemeCode,
            @JsonProperty("household_status_code") String householdStatusCode,
            @JsonProperty("start_date") String startDate,
            @JsonProperty("end_date") String endDate,
            @JsonProperty("support_details") String supportDetails,
            @JsonProperty("contact_number") String contactNumber,
            @JsonProperty("detail_url") String detailUrl,
            @JsonProperty("contact_email") String contactEmail,
            @JsonProperty("required_documents") String requiredDocuments
    ) {}
}
// 이 클래스의 역할: private.json 파싱 + WelfarePrivate 변환.
// start_date/end_date는 ISO 형식이라 LocalDate.parse 직접 사용 가능.
