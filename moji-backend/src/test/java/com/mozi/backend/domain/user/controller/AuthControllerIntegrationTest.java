package com.mozi.backend.domain.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mozi.backend.domain.user.dto.LoginRequest;
import com.mozi.backend.domain.user.dto.RefreshRequest;
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
 * 인증 API 4개의 통합 테스트.
 *
 * @SpringBootTest 로 전체 컨텍스트(Security FilterChain 포함)를 띄워 풀 HTTP 사이클을 검증한다.
 * mozi.seed.enabled=false 로 시드 적재를 끄고, 매 테스트마다 생성한 user/token row는
 * @AfterEach 에서 직접 정리한다 (시드 사용자 senior01~07 보존).
 */
@SpringBootTest(properties = "mozi.seed.enabled=false")
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    private static final String TEST_EMAIL = "phase3-test@mozi.test";
    private static final String TEST_PASSWORD = "password1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    /**
     * 매 테스트 전후로 TEST_EMAIL 잔재 제거.
     *
     * BeforeEach: 이전 실행이 비정상 종료되어 row가 남아 있는 경우 대비.
     * AfterEach: 이번 테스트가 만든 row 정리 — 시드 사용자(senior01~07)는 보존.
     */
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
            // RefreshToken 먼저 비우고 User 삭제 — FK 참조 무결성 유지
            refreshTokenRepository.deleteAllByUser_Id(u.getId());
            userRepository.delete(u);
        });
    }

    /**
     * 회원가입 정상 흐름 — 200 + userId/accessToken/refreshToken 반환.
     */
    @Test
    void signup_정상_200과_토큰반환() throws Exception {
        String body = objectMapper.writeValueAsString(new SignupRequest(TEST_EMAIL, TEST_PASSWORD));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }

    /**
     * 같은 이메일로 두 번 가입 시도 → 첫 번째는 200, 두 번째는 409 EMAIL_ALREADY_EXISTS.
     */
    @Test
    void signup_중복이메일_409_EMAIL_ALREADY_EXISTS() throws Exception {
        signupAndGetTokens();

        String body = objectMapper.writeValueAsString(new SignupRequest(TEST_EMAIL, TEST_PASSWORD));
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }

    /**
     * 잘못된 비밀번호 로그인 → 401 INVALID_CREDENTIALS.
     */
    @Test
    void login_비밀번호불일치_401_INVALID_CREDENTIALS() throws Exception {
        signupAndGetTokens();

        String body = objectMapper.writeValueAsString(new LoginRequest(TEST_EMAIL, "wrongpw1234"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    /**
     * Refresh Rotation 풀 사이클: refresh 시 새 토큰 발급 + 기존 raw 재사용 시 INVALID_REFRESH_TOKEN.
     */
    @Test
    void refresh_rotation_재사용시_INVALID_REFRESH_TOKEN() throws Exception {
        TokenPair pair = signupAndGetTokens();

        // 1차 refresh — 정상
        String body = objectMapper.writeValueAsString(new RefreshRequest(pair.refresh()));
        MvcResult res = mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        String newRefresh = root.path("data").path("refreshToken").asText();
        assertThat(newRefresh).isNotEqualTo(pair.refresh());

        // 같은 raw 재사용 — 두 번째 호출은 차단되어야 함 (Rotation 핵심)
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
    }

    /**
     * 로그아웃 → 사용자의 모든 RefreshToken row 삭제 → 이후 refresh 시도는 INVALID_REFRESH_TOKEN.
     */
    @Test
    void logout_이후_refresh시도_INVALID_REFRESH_TOKEN() throws Exception {
        TokenPair pair = signupAndGetTokens();

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + pair.access()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loggedOut").value(true));

        String body = objectMapper.writeValueAsString(new RefreshRequest(pair.refresh()));
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
    }

    /**
     * 보호 라우트(logout) 무토큰 호출 → 401 UNAUTHORIZED.
     */
    @Test
    void logout_헤더누락_401_UNAUTHORIZED() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    /**
     * 보호 라우트(logout) 변조 토큰 호출 → 401 INVALID_TOKEN.
     */
    @Test
    void logout_변조토큰_401_INVALID_TOKEN() throws Exception {
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    /**
     * Validation 실패 — blank email → 400 + VALIDATION_FAILED + fields.email 메시지.
     */
    @Test
    void signup_blank_email_400_VALIDATION_FAILED() throws Exception {
        String body = """
                {"email": "", "password": "password1234"}
                """;
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.email").isNotEmpty());
    }

    /**
     * 한 번 signup 한 뒤 응답에서 access + refresh raw 둘 다 추출해 다음 검증으로 넘긴다.
     */
    private TokenPair signupAndGetTokens() throws Exception {
        String body = objectMapper.writeValueAsString(new SignupRequest(TEST_EMAIL, TEST_PASSWORD));
        MvcResult res = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        return new TokenPair(data.path("accessToken").asText(), data.path("refreshToken").asText());
    }

    private record TokenPair(String access, String refresh) {
    }
}
// 이 테스트의 역할: Security FilterChain 포함 풀 HTTP 사이클로 인증 흐름 검증.
// signupAndGetTokens 헬퍼로 매 테스트가 새 사용자를 만들고 @AfterEach 에서 정리 — 시드 사용자는 보존.
