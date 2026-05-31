package com.mozi.backend.domain.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mozi.backend.domain.user.dto.SignupRequest;
import com.mozi.backend.domain.user.repository.RefreshTokenRepository;
import com.mozi.backend.domain.user.repository.UserProfileRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 통합 테스트.
 *
 * @SpringBootTest 로 전체 Security FilterChain 포함 풀 HTTP 사이클 검증.
 * AuthControllerIntegrationTest 패턴 답습:
 *  - mozi.seed.enabled=false
 *  - BeforeEach/AfterEach에서 TEST_EMAIL 잔재 정리 (시드 사용자 senior01~07 보존)
 *  - signup → accessToken 추출 헬퍼 한 번 작성 후 각 테스트 공유
 */
@SpringBootTest(properties = "mozi.seed.enabled=false")
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    private static final String TEST_EMAIL = "phase4-1-test@mozi.test";
    private static final String TEST_PASSWORD = "password1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
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
     * 가입 직후 GET 프로필 → 200 + isCompleted=false (lazy creation 미발생).
     */
    @Test
    void getProfile_가입직후_isCompletedFalse() throws Exception {
        String accessToken = signupAndGetAccess();

        mockMvc.perform(get("/api/users/me/profile").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.isCompleted").value(false))
                .andExpect(jsonPath("$.data.birthDate").doesNotExist());
    }

    /**
     * PUT 프로필 부분 → 200 + 지정 필드만 변경 + lazy creation.
     *
     * Step 3·4 재설계: 행정구역 필드는 REGION 시드 적재(Step 8) 전이라 사용 X.
     * 대신 gender + boolean 필드로 부분 갱신 동작 검증.
     */
    @Test
    void putProfile_부분필드만_lazyCreation_지정필드만변경() throws Exception {
        String accessToken = signupAndGetAccess();

        // gender만 보냄
        String body = """
                {"gender": "F"}
                """;
        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("프로필이 저장되었어요."))
                .andExpect(jsonPath("$.data.isCompleted").value(true))
                .andExpect(jsonPath("$.data.gender").value("F"))
                .andExpect(jsonPath("$.data.birthDate").doesNotExist())     // null → 응답에서 제외
                .andExpect(jsonPath("$.data.isDisabled").value(false));   // boolean primitive 기본값
    }

    /**
     * PUT 프로필 전체 → GET 시 모든 필드 보임 + isCompleted=true.
     *
     * Step 3·4 재설계: incomeType/householdType은 enum name으로 전송.
     * 행정구역(sidoCode/sigunguCode)은 REGION 시드 적재 전이라 본 테스트에서 제외 —
     * REGION 검증 통합 테스트는 Step 8·9에서 보강 예정.
     */
    @Test
    void putProfile_전체필드_GET시_isCompletedTrue() throws Exception {
        String accessToken = signupAndGetAccess();

        String body = """
                {
                  "birthDate": "1948-03-01",
                  "gender": "F",
                  "incomeType": "BASIC_PENSION",
                  "householdType": "LIVING_ALONE",
                  "isDisabled": false,
                  "isVeteran": false
                }
                """;
        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/profile").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isCompleted").value(true))
                .andExpect(jsonPath("$.data.birthDate").value("1948-03-01"))
                .andExpect(jsonPath("$.data.gender").value("F"))
                .andExpect(jsonPath("$.data.incomeType").value("BASIC_PENSION"))
                .andExpect(jsonPath("$.data.householdType").value("LIVING_ALONE"));
    }

    /**
     * 명시적 null 입력도 무변경으로 처리 (옵션 C 의미).
     *
     * Jackson record + nullable 필드 조합에서 absent와 explicit null을 구분할 수 없어
     * 두 케이스를 모두 "무변경"으로 통일. 한 번 채워진 필드는 새 값으로만 갱신 가능.
     */
    @Test
    void putProfile_명시적null_무변경() throws Exception {
        String accessToken = signupAndGetAccess();

        // 1) 먼저 gender + householdType 채우기
        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\":\"F\",\"householdType\":\"LIVING_ALONE\"}"))
                .andExpect(status().isOk());

        // 2) gender만 null로 보내도 무변경 — 옵션 C 정책상 클리어 동작은 미지원
        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.householdType").value("LIVING_ALONE"))   // 무변경
                .andExpect(jsonPath("$.data.gender").value("F"));                     // 무변경 (null이지만 클리어 X)
    }

    /**
     * 비밀번호 변경 정상 → 200 + DB password 갱신 + RefreshToken 0건.
     */
    @Test
    void changePassword_정상_토큰일괄삭제_확인() throws Exception {
        String accessToken = signupAndGetAccess();
        Long userId = userRepository.findByEmail(TEST_EMAIL).orElseThrow().getId();
        long refreshCountBefore = refreshTokenRepository.findAll().stream()
                .filter(r -> r.getUser().getId().equals(userId)).count();
        assertThat(refreshCountBefore).isEqualTo(1);   // signup이 토큰 1개 생성

        String body = """
                {"currentPassword": "%s", "newPassword": "newpass1234"}
                """.formatted(TEST_PASSWORD);
        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.changed").value(true));

        long refreshCountAfter = refreshTokenRepository.findAll().stream()
                .filter(r -> r.getUser().getId().equals(userId)).count();
        assertThat(refreshCountAfter).isEqualTo(0);   // 일괄 삭제됨
    }

    /**
     * 현재 비밀번호 불일치 → 400 PASSWORD_MISMATCH.
     */
    @Test
    void changePassword_현재비밀번호불일치_400() throws Exception {
        String accessToken = signupAndGetAccess();

        String body = """
                {"currentPassword": "wrongpw1234", "newPassword": "newpass1234"}
                """;
        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_MISMATCH"));
    }

    /**
     * 새 비밀번호 = 현재 비밀번호 → 400 SAME_PASSWORD.
     */
    @Test
    void changePassword_동일비밀번호_400() throws Exception {
        String accessToken = signupAndGetAccess();

        String body = """
                {"currentPassword": "%s", "newPassword": "%s"}
                """.formatted(TEST_PASSWORD, TEST_PASSWORD);
        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SAME_PASSWORD"));
    }

    /**
     * Validation: 8자 미만 newPassword → 400 VALIDATION_FAILED.
     */
    @Test
    void changePassword_새비밀번호짧음_VALIDATION_FAILED() throws Exception {
        String accessToken = signupAndGetAccess();

        String body = """
                {"currentPassword": "%s", "newPassword": "short"}
                """.formatted(TEST_PASSWORD);
        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.newPassword").isNotEmpty());
    }

    /**
     * 회원 탈퇴 → 200 + User/Profile/RefreshToken 모두 삭제 (cascade 동작 검증).
     */
    @Test
    void withdraw_정상_cascade_삭제검증() throws Exception {
        String accessToken = signupAndGetAccess();
        // 프로필 미리 생성 (cascade 검증용) — 행정구역 대신 gender 입력 (REGION 시드 비어 있음)
        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"gender\":\"F\"}"))
                .andExpect(status().isOk());
        Long userId = userRepository.findByEmail(TEST_EMAIL).orElseThrow().getId();
        assertThat(userProfileRepository.findByUser_Id(userId)).isPresent();

        mockMvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.withdrawn").value(true));

        // cascade: User/Profile/RefreshToken 모두 삭제
        assertThat(userRepository.findByEmail(TEST_EMAIL)).isEmpty();
        assertThat(userProfileRepository.findByUser_Id(userId)).isEmpty();
        long remaining = refreshTokenRepository.findAll().stream()
                .filter(r -> r.getUser().getId().equals(userId)).count();
        assertThat(remaining).isEqualTo(0);
    }

    /**
     * 탈퇴 직후 동일 이메일 재가입 가능 (UNIQUE 충돌 X).
     */
    @Test
    void withdraw_후_동일이메일_재가입_가능() throws Exception {
        String accessToken = signupAndGetAccess();
        mockMvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 재가입 시도
        String body = objectMapper.writeValueAsString(new SignupRequest(TEST_EMAIL, TEST_PASSWORD));
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    /**
     * 보호 라우트 무토큰 호출 → 401 UNAUTHORIZED.
     */
    @Test
    void getProfile_헤더누락_401() throws Exception {
        mockMvc.perform(get("/api/users/me/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    /**
     * Step 5: REGION 마스터에 없는 sidoCode 입력 → 400 INVALID_REGION_CODE.
     *
     * 본 테스트는 REGION 시드 적재(Step 8) 전 상태를 전제로 동작 — 시드가 적재되어도
     * "99"는 행정표준코드 범위 밖이라 동일 검증 결과를 유지한다.
     */
    @Test
    void putProfile_미존재sidoCode_400_INVALID_REGION_CODE() throws Exception {
        String accessToken = signupAndGetAccess();

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sidoCode\":\"99\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REGION_CODE"));
    }

    /**
     * Step 5: sidoCode 없이 sigunguCode만 입력 → 400 INVALID_REGION_CODE.
     */
    @Test
    void putProfile_sigunguCode단독입력_400_INVALID_REGION_CODE() throws Exception {
        String accessToken = signupAndGetAccess();

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sigunguCode\":\"11680\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REGION_CODE"));
    }

    /**
     * Step 9: 유효한 sidoCode + sigunguCode 미입력 → 200 (시도만 선택 케이스).
     *
     * REGION 시드 적재 후 "11"(서울특별시)이 REGION에 존재 → 검증 통과 + 정상 갱신.
     */
    @Test
    void putProfile_유효한sidoCode_시군구미입력_200() throws Exception {
        String accessToken = signupAndGetAccess();

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sidoCode\":\"11\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sidoCode").value("11"))
                .andExpect(jsonPath("$.data.sigunguCode").doesNotExist());
    }

    /**
     * Step 9: 유효한 sidoCode + sigunguCode 조합 → 200 + 응답에 두 코드 반영.
     *
     * REGION 시드에 ("11","11680") 행이 있어 시도-시군구 일관성 검증 통과.
     */
    @Test
    void putProfile_유효한시도시군구조합_200() throws Exception {
        String accessToken = signupAndGetAccess();

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sidoCode\":\"11\",\"sigunguCode\":\"11680\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sidoCode").value("11"))
                .andExpect(jsonPath("$.data.sigunguCode").value("11680"));
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
        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        return data.path("accessToken").asText();
    }
}
// 이 테스트의 역할: /api/users/me 4개 엔드포인트의 풀 HTTP 사이클 + 인증/인가/Validation/cascade 검증.
// AuthControllerIntegrationTest의 BeforeEach/AfterEach 정리 패턴을 답습해 시드 사용자 senior01~07은 보존.
