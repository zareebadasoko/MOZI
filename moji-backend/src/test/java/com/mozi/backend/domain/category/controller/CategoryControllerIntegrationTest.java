package com.mozi.backend.domain.category.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Category API 통합 테스트.
 *
 * 시드 적재된 22개 마스터(THEME 15 + STATUS 7) 기반으로 type별 응답 크기 검증.
 * type 필수 정책의 위반 케이스(미지정/오타)도 함께 확인.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void getCategories_THEME_15행() throws Exception {
        mockMvc.perform(get("/api/categories").param("type", "THEME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(15))
                .andExpect(jsonPath("$.data[0].type").value("THEME"));
    }

    @Test
    void getCategories_STATUS_7행() throws Exception {
        mockMvc.perform(get("/api/categories").param("type", "STATUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(7))
                .andExpect(jsonPath("$.data[0].type").value("STATUS"));
    }

    /**
     * type 미지정 → 400 (필수 파라미터 누락).
     */
    @Test
    void getCategories_type미지정_400() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 잘못된 type 값 → 400 (enum 매핑 실패).
     */
    @Test
    void getCategories_잘못된type_400() throws Exception {
        mockMvc.perform(get("/api/categories").param("type", "WRONG"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategories_헤더없이_200() throws Exception {
        // 인증 미사용 라우트
        mockMvc.perform(get("/api/categories").param("type", "THEME"))
                .andExpect(status().isOk());
    }
}
// 이 테스트의 역할: 카테고리 API의 정상/필수/오류 케이스 검증.
// 시드 데이터 기반이라 fixture 셋업 불필요 — 22행이 보장된 상태에서 길이 정확 검증.
