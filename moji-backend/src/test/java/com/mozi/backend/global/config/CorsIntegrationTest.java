package com.mozi.backend.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS 정책 통합 테스트.
 *
 * 프론트엔드(Vite·CRA 개발 서버) Origin에서 보내는 preflight `OPTIONS` 요청이
 * 200 OK + 적절한 Access-Control-* 응답 헤더를 반환하는지 검증한다.
 *
 * 본 테스트는 SecurityConfig.cors() 활성화와 CorsConfig 빈이 함께 동작하는지의 회귀 안전망 역할.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsIntegrationTest {

    @Autowired private MockMvc mockMvc;

    /**
     * Vite 기본 포트(5173) Origin에서 보낸 preflight가 허용되는지 검증.
     */
    @Test
    void preflight_Vite_Origin_허용() throws Exception {
        mockMvc.perform(options("/api/welfares")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    /**
     * CRA·Next 관습 포트(3000) Origin도 허용 목록에 있는지 검증.
     */
    @Test
    void preflight_3000_Origin_허용() throws Exception {
        mockMvc.perform(options("/api/welfares")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    /**
     * 허용 목록에 없는 Origin은 거부되는지 검증.
     * Spring Security CORS 처리상 preflight 자체는 차단되며 200 미반환.
     */
    @Test
    void preflight_허용되지않은_Origin_거부() throws Exception {
        mockMvc.perform(options("/api/welfares")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
// 이 테스트 클래스의 역할: CorsConfig + SecurityConfig.cors() 통합 동작의 회귀 안전망.
// 허용 Origin 추가/제거 시 본 테스트의 케이스도 함께 갱신해야 한다.
