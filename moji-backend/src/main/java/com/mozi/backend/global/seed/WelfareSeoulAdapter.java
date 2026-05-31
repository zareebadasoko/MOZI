package com.mozi.backend.global.seed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mozi.backend.domain.welfare.entity.WelfareSeoul;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 서울복지포털 JSON(seoul.json)을 파싱해 WelfareSeoul 엔티티로 변환.
 *
 * 가장 작은 파일이지만 스키마는 가장 다름:
 * - ID prefix SEL00000xxx (서울복지포털 자체)
 * - detail_content 필드 (Seoul 전용, 복지로의 어떤 컬럼과도 1:1 대응 X)
 * - region_name 없음 (모두 서울특별시)
 * - selection_criteria, support_details, support_year, process_steps 없음
 *
 * 카테고리 컬럼은 시드 데이터 정규화 단계(2026-05-08)에서 이미
 * interest_theme_code / household_status_code로 통일됨 (원본 INTRS_THEMA_CD →).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfareSeoulAdapter {

    private static final String SEED_PATH = "seed-data/welfare-crawled/full/seoul.json";

    private final ObjectMapper objectMapper;

    /**
     * seoul.json을 파싱해 시드 row 리스트로 반환.
     *
     * @return 각 row를 (엔티티, 관심주제 raw, 가구상황 raw)로 묶은 리스트
     */
    public List<WelfareSeedRow> parse() {
        FileSystemResource resource = new FileSystemResource(SEED_PATH);
        try (InputStream is = resource.getInputStream()) {
            SeoulRow[] rows = objectMapper.readValue(is, SeoulRow[].class);
            List<WelfareSeedRow> result = List.of(rows).stream()
                    .map(this::toSeedRow)
                    .toList();
            log.info("Parsed {} seoul rows from {}", result.size(), SEED_PATH);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse " + SEED_PATH, e);
        }
    }

    private WelfareSeedRow toSeedRow(SeoulRow r) {
        WelfareSeoul entity = WelfareSeoul.builder()
                .id(r.id)
                .title(r.title)
                .summary(r.summary)
                .organizationName(r.organizationName)
                .targetAudience(r.targetAudience)
                .applicationMethod(r.applicationMethod)
                .supportType(r.supportType)
                .contactNumber(r.contactNumber)
                .detailContent(r.detailContent)
                .supportCycle(r.supportCycle)
                .requiredDocuments(r.requiredDocuments)
                .build();
        return new WelfareSeedRow(entity, r.interestThemeCode, r.householdStatusCode);
    }

    private record SeoulRow(
            @JsonProperty("ID") String id,
            String title,
            String summary,
            @JsonProperty("organization_name") String organizationName,
            @JsonProperty("target_audience") String targetAudience,
            @JsonProperty("application_method") String applicationMethod,
            @JsonProperty("interest_theme_code") String interestThemeCode,
            @JsonProperty("household_status_code") String householdStatusCode,
            @JsonProperty("support_type") String supportType,
            @JsonProperty("contact_number") String contactNumber,
            @JsonProperty("detail_content") String detailContent,
            @JsonProperty("support_cycle") String supportCycle,
            @JsonProperty("required_documents") String requiredDocuments
    ) {}
}
// 이 클래스의 역할: seoul.json 파싱 + WelfareSeoul 변환.
// 시연 시나리오 C("장애 노인 박할머니 — 활동지원")의 데이터 출처.
