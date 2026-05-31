package com.mozi.backend.domain.chat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mozi.backend.domain.user.dto.SignupRequest;
import com.mozi.backend.domain.user.repository.RefreshTokenRepository;
import com.mozi.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chat API 통합 테스트.
 *
 * `@SpringBootTest`로 전체 컨텍스트 + Security FilterChain + MockChatbotClient(시드 기반 무작위 추천) 부팅.
 * `application-local.yml`(`mock-enabled: true`)이 기본 활성화라 별도 프로파일 지정 불필요.
 *
 * 검증 대상:
 *  - POST /api/chat 정상 (200 + reply/welfares/conversationId)
 *  - conversationId 발급 (첫 요청) + 유지 (후속 요청)
 *  - welfares 형식 검증 (WelfareSummaryDto + categories + isBookmarked)
 *  - 무인증 → 401
 *  - blank message → 400 VALIDATION_FAILED
 *
 * 사용자 정리는 AuthControllerIntegrationTest 패턴 답습.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerIntegrationTest {

    private static final String TEST_EMAIL = "phase5-test@mozi.test";
    private static final String TEST_PASSWORD = "password1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void primeClean() {
        cleanupTestUser();
    }

    @AfterEach
    void cleanup() {
        cleanupTestUser();
    }

    private void cleanupTestUser() {
        userRepository.findByEmail(TEST_EMAIL).ifPresent(u -> {
            refreshTokenRepository.deleteAllByUser_Id(u.getId());
            userRepository.delete(u);
        });
    }

    /**
     * POST 정상 — 200 + reply/welfares/conversationId 응답 형식.
     */
    @Test
    void chat_정상_응답형식() throws Exception {
        String accessToken = signupAndGetAccess();
        String body = """
                {"message": "노인 일자리 알려줘"}
                """;

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.reply").isNotEmpty())
                .andExpect(jsonPath("$.data.welfares").isArray())
                .andExpect(jsonPath("$.data.conversationId").isNotEmpty());
    }

    /**
     * 첫 요청 — conversationId 없이 보내면 응답에 UUID v4 발급되어 옴.
     */
    @Test
    void chat_conversationId없음_UUID발급() throws Exception {
        String accessToken = signupAndGetAccess();
        String body = """
                {"message": "hello"}
                """;

        MvcResult res = mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();

        String convo = objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("conversationId").asText();
        // UUID v4 형식 (RFC 4122)
        assertThat(convo).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    /**
     * 후속 요청 — 같은 conversationId 보내면 그대로 응답에 반환.
     */
    @Test
    void chat_conversationId있음_그대로반환() throws Exception {
        String accessToken = signupAndGetAccess();

        // 1차 요청으로 conversationId 확보
        MvcResult first = mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hi\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String convo = objectMapper.readTree(first.getResponse().getContentAsString())
                .path("data").path("conversationId").asText();

        // 2차 요청 — 같은 conversationId 명시 전송
        String body = "{\"message\":\"more questions\",\"conversationId\":\"" + convo + "\"}";
        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(convo));
    }

    /**
     * welfares 응답 형식 — 4-2 검색과 동일한 WelfareSummaryDto.
     */
    @Test
    void chat_welfares_4_2와동일형식() throws Exception {
        String accessToken = signupAndGetAccess();

        MvcResult res = mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode welfares = objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("welfares");
        assertThat(welfares.size()).as("Mock이 3개 추천").isEqualTo(3);

        JsonNode first = welfares.get(0);
        assertThat(first.path("id").asText()).isNotEmpty();
        assertThat(first.path("title").asText()).isNotEmpty();
        assertThat(first.path("welfareType").asText()).matches("CENTRAL|LOCAL|PRIVATE|SEOUL");
        assertThat(first.has("categories")).isTrue();
        assertThat(first.has("isBookmarked")).isTrue();
        // 가입 직후라 북마크 없음
        assertThat(first.path("isBookmarked").asBoolean()).isFalse();
    }

    /**
     * 무인증 호출 → 401 UNAUTHORIZED.
     */
    @Test
    void chat_무인증_401() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hi\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    /**
     * blank message → 400 VALIDATION_FAILED.
     */
    @Test
    void chat_blank_message_400_VALIDATION_FAILED() throws Exception {
        String accessToken = signupAndGetAccess();

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.message").isNotEmpty());
    }

    /**
     * message 1000자 초과 → 400 VALIDATION_FAILED.
     */
    @Test
    void chat_message_초과_400_VALIDATION_FAILED() throws Exception {
        String accessToken = signupAndGetAccess();
        String tooLong = "a".repeat(1001);
        String body = "{\"message\":\"" + tooLong + "\"}";

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    /**
     * 같은 message 두 번 호출 → Mock의 결정적 응답 덕에 같은 welfareId 추천.
     */
    @Test
    void chat_Mock결정적응답_같은추천() throws Exception {
        String accessToken = signupAndGetAccess();
        String body = "{\"message\":\"같은 질문\"}";

        MvcResult r1 = mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        MvcResult r2 = mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();

        JsonNode w1 = objectMapper.readTree(r1.getResponse().getContentAsString())
                .path("data").path("welfares");
        JsonNode w2 = objectMapper.readTree(r2.getResponse().getContentAsString())
                .path("data").path("welfares");
        assertThat(extractIds(w1)).isEqualTo(extractIds(w2));
    }

    /**
     * signup 후 응답에서 accessToken 추출.
     */
    private String signupAndGetAccess() throws Exception {
        String body = objectMapper.writeValueAsString(new SignupRequest(TEST_EMAIL, TEST_PASSWORD));
        MvcResult res = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private java.util.List<String> extractIds(JsonNode welfares) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        welfares.forEach(item -> ids.add(item.path("id").asText()));
        return ids;
    }
}
// 이 테스트의 역할: POST /api/chat의 풀 HTTP 사이클 검증 — Mock 활성 상태에서 결정적 응답 + 인증/Validation 분기.
// 시드 데이터 + MockChatbotClient(application-local.yml mock-enabled=true) 조합으로 fixture 별도 셋업 없이 동작.
