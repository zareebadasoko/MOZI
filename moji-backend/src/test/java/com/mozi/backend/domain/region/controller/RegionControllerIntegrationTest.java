package com.mozi.backend.domain.region.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Region API 통합 테스트.
 *
 * Step 8 시드 적재 후 — 실데이터(17개 시도 + 약 229 시군구) 기반으로 동작 검증.
 * Category 통합 테스트와 동일 패턴: 시드가 적재된 상태를 전제로 응답 크기·구조 검증.
 *
 * 검증 대상:
 *  - GET /api/regions → 17개 시도 그루핑 응답
 *  - GET /api/regions?sido=11 → 서울 25개 자치구 평면 응답
 *  - 비로그인 접근 가능(permitAll)
 */
@SpringBootTest
@AutoConfigureMockMvc
class RegionControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    /**
     * GET /api/regions — 17개 시도 그루핑 응답.
     */
    @Test
    void getRegions_시드적재_시도17개그루핑() throws Exception {
        mockMvc.perform(get("/api/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(17))
                .andExpect(jsonPath("$.data[0].sidoCode").exists())
                .andExpect(jsonPath("$.data[0].sigungus").isArray());
    }

    /**
     * GET /api/regions?sido=11 — 서울 25개 자치구 평면 응답.
     */
    @Test
    void getRegions_sido11_서울25개_시군구() throws Exception {
        mockMvc.perform(get("/api/regions").param("sido", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(25))
                .andExpect(jsonPath("$.data[0].sidoCode").value("11"))
                .andExpect(jsonPath("$.data[0].sidoName").value("서울특별시"));
    }

    /**
     * 비로그인 접근 가능 — Authorization 헤더 없이 200 응답.
     * SecurityConfig의 permitAll 화이트리스트에 /api/regions 등록 여부 검증.
     */
    @Test
    void getRegions_비로그인_접근가능() throws Exception {
        mockMvc.perform(get("/api/regions"))
                .andExpect(status().isOk());
    }
}
// 이 테스트의 역할: Region API의 라우팅·보안 정책·실데이터 응답 검증.
// Step 8 시드 적재 이후로 갱신 — 빈 배열 가정에서 실 데이터 카운트(17, 25)로 검증.
