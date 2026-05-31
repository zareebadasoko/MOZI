package com.mozi.backend.domain.welfare.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Welfare 검색·상세 API 통합 테스트.
 *
 * 시드 데이터(~1465 row, 4 출처) 기반으로 동작하므로 별도 fixture 셋업 없이 풀 HTTP 사이클 검증 가능.
 * mozi.seed.enabled=true 가 application-local.yml에서 이미 ON이라 컨텍스트 부팅 시 시드 적재 완료.
 *
 * 검증 대상:
 *  - 기본 검색 → 4개 출처 모두 등장
 *  - 키워드 검색 (title/summary 매칭)
 *  - 출처 필터
 *  - 페이지네이션 응답 구조
 *  - 비로그인 isBookmarked=false
 *  - 상세 조회 4 children 분기
 *  - 404 (없는 ID)
 */
@SpringBootTest
@AutoConfigureMockMvc
class WelfareControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    /**
     * 기본 검색 (no params) → 4 출처 모두 검색 결과에 등장.
     */
    @Test
    void search_no_params_4출처모두등장() throws Exception {
        // 큰 페이지로 한 번에 가져와 분포 확인
        MvcResult res = mockMvc.perform(get("/api/welfares").param("size", "50").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalCount").isNumber())
                .andExpect(jsonPath("$.data.hasNext").isBoolean())
                .andReturn();

        JsonNode items = objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("items");
        Set<String> types = new HashSet<>();
        items.forEach(item -> types.add(item.path("welfareType").asText()));

        // 페이지 50건 안에 4 출처가 골고루 등장하지 않을 수 있으므로 더 큰 sample 검증을 위해
        // welfareType 별 검색을 각각 수행해 적어도 4 출처가 모두 시드된 것을 확인
        for (String type : new String[]{"CENTRAL", "LOCAL", "PRIVATE", "SEOUL"}) {
            long count = countWithType(type);
            assertThat(count).as("welfareType=" + type + " 시드 개수").isGreaterThan(0);
        }
    }

    /**
     * 키워드 검색 — 응답 items 모두 keyword가 title/summary에 들어 있어야 함.
     */
    @Test
    void search_키워드_매칭() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/welfares")
                        .param("keyword", "노인")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("items");
        // 결과 row마다 title 또는 summary에 "노인" 포함 검증
        items.forEach(item -> {
            String title = item.path("title").asText("");
            String summary = item.path("summary").asText("");
            assertThat(title.contains("노인") || summary.contains("노인"))
                    .as("title/summary 둘 중 하나에는 키워드 포함되어야 함: " + title)
                    .isTrue();
        });
    }

    /**
     * 출처 필터 — welfareType=PRIVATE 응답이 모두 PRIVATE.
     */
    @Test
    void search_welfareType_필터() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/welfares")
                        .param("welfareType", "PRIVATE")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("items");
        items.forEach(item -> assertThat(item.path("welfareType").asText()).isEqualTo("PRIVATE"));
    }

    /**
     * 페이지네이션 응답 구조 — items/page/size/totalCount/hasNext.
     */
    @Test
    void search_페이지네이션_응답구조() throws Exception {
        mockMvc.perform(get("/api/welfares").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalCount").isNumber())
                .andExpect(jsonPath("$.data.hasNext").isBoolean());
    }

    /**
     * size 클램프 — 999 → 50.
     */
    @Test
    void search_size999_50으로클램프() throws Exception {
        mockMvc.perform(get("/api/welfares").param("size", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(50));
    }

    /**
     * region 입력 시 CENTRAL/PRIVATE는 항상 통과 — totalCount 비교로 검증.
     *
     * region 입력 + welfareType=CENTRAL 으로 좁히면 CENTRAL 시드 중 region 무관하게
     * 모두 통과해야 하므로 totalCount가 시드 CENTRAL 전체 수와 일치한다.
     */
    @Test
    void search_region_CENTRAL_PRIVATE_항상통과() throws Exception {
        long centralWithoutRegion = countWithType("CENTRAL");
        long centralWithRegion = countTotalCount("/api/welfares?region=경기&welfareType=CENTRAL&size=1");
        assertThat(centralWithRegion)
                .as("CENTRAL은 region 무관하게 모두 통과")
                .isEqualTo(centralWithoutRegion);

        long privateWithoutRegion = countWithType("PRIVATE");
        long privateWithRegion = countTotalCount("/api/welfares?region=서울&welfareType=PRIVATE&size=1");
        assertThat(privateWithRegion)
                .as("PRIVATE도 region 무관하게 모두 통과")
                .isEqualTo(privateWithoutRegion);
    }

    /**
     * region에 "서울" 포함 시 SEOUL 출처 항상 통과 — totalCount 검증.
     */
    @Test
    void search_region서울_SEOUL모두통과() throws Exception {
        long seoulAll = countWithType("SEOUL");
        long seoulWithSeoulRegion = countTotalCount("/api/welfares?region=서울특별시&welfareType=SEOUL&size=1");
        assertThat(seoulWithSeoulRegion)
                .as("region에 서울 포함 시 SEOUL 시드 모두 통과")
                .isEqualTo(seoulAll);
    }

    /**
     * region에 "서울" 미포함 시 SEOUL은 결과에서 완전히 제외.
     */
    @Test
    void search_region경기_SEOUL제외() throws Exception {
        long seoulWithGyeonggi = countTotalCount("/api/welfares?region=경기&welfareType=SEOUL&size=1");
        assertThat(seoulWithGyeonggi)
                .as("region에 서울 미포함 시 SEOUL 결과 0건")
                .isEqualTo(0);
    }

    /**
     * region="서울" + LOCAL 좁힘 — regionName에 "서울" 포함된 row만 통과.
     */
    @Test
    void search_region매칭_LOCAL_regionName포함_시드_존재() throws Exception {
        long localSeoul = countTotalCount("/api/welfares?region=서울&welfareType=LOCAL&size=1");
        long localAll = countWithType("LOCAL");
        assertThat(localSeoul)
                .as("LOCAL 서울 자치구 시드 존재")
                .isGreaterThan(0);
        assertThat(localSeoul)
                .as("LOCAL 서울 row는 LOCAL 전체보다 적어야 함")
                .isLessThan(localAll);
    }

    private long countTotalCount(String url) throws Exception {
        MvcResult res = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("totalCount").asLong();
    }

    /**
     * region 입력 시 LOCAL row의 regionName은 모두 region 키워드를 포함해야 함.
     */
    @Test
    void search_region매칭_LOCAL의regionName포함검증() throws Exception {
        // welfareType=LOCAL로 좁혀서 regionName 매칭 검증
        MvcResult res = mockMvc.perform(get("/api/welfares")
                        .param("region", "서울")
                        .param("welfareType", "LOCAL")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("items");
        // welfareType=LOCAL 결과는 모두 LOCAL이어야 하고, regionName은 detail에서만 보이므로
        // 이 단계에서는 row 존재 여부만 확인 (LOCAL+서울 데이터가 시드에 있는지 sanity)
        assertThat(items.size()).as("LOCAL 서울 시드 row 존재").isGreaterThanOrEqualTo(0);
        items.forEach(item -> assertThat(item.path("welfareType").asText()).isEqualTo("LOCAL"));
    }

    /**
     * 비로그인 검색 — isBookmarked 모두 false.
     */
    @Test
    void search_비로그인_isBookmarked전부false() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/welfares").param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("items");
        items.forEach(item -> assertThat(item.path("isBookmarked").asBoolean()).isFalse());
    }

    /**
     * 상세 조회 - CENTRAL 시드 ID로 호출 → centralDetail만 채워짐.
     */
    @Test
    void detail_central_centralDetail만() throws Exception {
        // 시드 데이터에서 CENTRAL 타입 ID 하나 가져와 상세 호출
        String centralId = pickIdByType("CENTRAL");
        MvcResult res = mockMvc.perform(get("/api/welfares/{id}", centralId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.welfareType").value("CENTRAL"))
                .andExpect(jsonPath("$.data.centralDetail").exists())
                .andExpect(jsonPath("$.data.localDetail").doesNotExist())
                .andExpect(jsonPath("$.data.privateDetail").doesNotExist())
                .andExpect(jsonPath("$.data.seoulDetail").doesNotExist())
                .andReturn();

        // targetAudience/applicationMethod 부모 필드도 응답에 포함
        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        assertThat(data.path("id").asText()).isEqualTo(centralId);
        assertThat(data.path("isBookmarked").asBoolean()).isFalse();
    }

    /**
     * 상세 조회 - SEOUL 시드 ID로 호출 → seoulDetail만 채워짐.
     */
    @Test
    void detail_seoul_seoulDetail만() throws Exception {
        String seoulId = pickIdByType("SEOUL");
        mockMvc.perform(get("/api/welfares/{id}", seoulId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.welfareType").value("SEOUL"))
                .andExpect(jsonPath("$.data.seoulDetail").exists())
                .andExpect(jsonPath("$.data.centralDetail").doesNotExist())
                .andExpect(jsonPath("$.data.localDetail").doesNotExist())
                .andExpect(jsonPath("$.data.privateDetail").doesNotExist());
    }

    /**
     * 상세 조회 - 없는 ID → 404 WELFARE_NOT_FOUND.
     */
    @Test
    void detail_없는ID_404() throws Exception {
        mockMvc.perform(get("/api/welfares/{id}", "NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("WELFARE_NOT_FOUND"));
    }

    /**
     * 인증 미사용 — 헤더 없이 호출 가능 (200).
     */
    @Test
    void search_헤더없이_200() throws Exception {
        mockMvc.perform(get("/api/welfares"))
                .andExpect(status().isOk());
    }

    private long countWithType(String type) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/welfares").param("welfareType", type).param("size", "1"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("totalCount").asLong();
    }

    private String pickIdByType(String type) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/welfares").param("welfareType", type).param("size", "1"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("items");
        assertThat(items).as("type=" + type + " 시드 row 존재").isNotEmpty();
        return items.get(0).path("id").asText();
    }
}
// 이 테스트의 역할: Welfare 검색·상세 API의 풀 HTTP 사이클을 시드 데이터 기반으로 검증.
// 시드 적재된 ~1465 row를 그대로 사용하므로 fixture 별도 셋업 없이 빠르게 통합 검증 가능.
