package com.mozi.backend.domain.bookmark.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mozi.backend.domain.bookmark.repository.BookmarkRepository;
import com.mozi.backend.domain.user.dto.SignupRequest;
import com.mozi.backend.domain.user.repository.RefreshTokenRepository;
import com.mozi.backend.domain.user.repository.UserRepository;
import com.mozi.backend.domain.welfare.entity.WelfareType;
import com.mozi.backend.domain.welfare.repository.WelfareCommonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bookmark API 통합 테스트.
 *
 * @SpringBootTest 로 전체 컨텍스트(Security FilterChain 포함) 띄워 풀 HTTP 사이클 검증.
 * AuthControllerIntegrationTest 패턴을 답습:
 *  - mozi.seed.enabled=true 로 시드 적재(시드 사용자 + ~1465 복지 + 22 카테고리) 활용
 *  - BeforeEach/AfterEach에서 TEST_EMAIL 잔재 정리 (시드 사용자 senior01~07 보존)
 *  - signup → accessToken 추출 헬퍼 한 번 작성 후 각 테스트 공유
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookmarkControllerIntegrationTest {

    private static final String TEST_EMAIL = "phase4-3-test@mozi.test";
    private static final String TEST_PASSWORD = "password1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private BookmarkRepository bookmarkRepository;
    @Autowired private WelfareCommonRepository welfareCommonRepository;

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
            // bookmark, refreshToken 모두 cascade 또는 수동 삭제
            refreshTokenRepository.deleteAllByUser_Id(u.getId());
            userRepository.delete(u);
        });
    }

    /**
     * POST 정상 — 200 + bookmarkId.
     */
    @Test
    void post_정상_bookmarkId반환() throws Exception {
        String accessToken = signupAndGetAccess();
        String welfareId = pickAnyWelfareId();

        mockMvc.perform(post("/api/bookmarks/{welfareId}", welfareId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("북마크에 추가되었어요."))
                .andExpect(jsonPath("$.data.bookmarkId").isNumber());
    }

    /**
     * POST idempotent — 같은 welfareId 두 번 호출 → 두 번 다 200 + 같은 bookmarkId.
     */
    @Test
    void post_idempotent_같은bookmarkId반환() throws Exception {
        String accessToken = signupAndGetAccess();
        String welfareId = pickAnyWelfareId();

        Long firstId = postAndGetBookmarkId(accessToken, welfareId);
        Long secondId = postAndGetBookmarkId(accessToken, welfareId);

        assertThat(firstId).isEqualTo(secondId);
    }

    /**
     * POST 없는 welfareId → 404 WELFARE_NOT_FOUND.
     */
    @Test
    void post_없는welfareId_404() throws Exception {
        String accessToken = signupAndGetAccess();

        mockMvc.perform(post("/api/bookmarks/{welfareId}", "NOEXIST")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("WELFARE_NOT_FOUND"));
    }

    /**
     * POST 무인증 → 401 UNAUTHORIZED.
     */
    @Test
    void post_무인증_401() throws Exception {
        String welfareId = pickAnyWelfareId();
        mockMvc.perform(post("/api/bookmarks/{welfareId}", welfareId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    /**
     * DELETE 정상 — 200 + deleted=true.
     */
    @Test
    void delete_정상_deletedTrue() throws Exception {
        String accessToken = signupAndGetAccess();
        String welfareId = pickAnyWelfareId();
        postAndGetBookmarkId(accessToken, welfareId);   // 사전 추가

        mockMvc.perform(delete("/api/bookmarks/{welfareId}", welfareId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("북마크에서 삭제되었어요."))
                .andExpect(jsonPath("$.data.deleted").value(true));
    }

    /**
     * DELETE 없는 welfareId → 404 WELFARE_NOT_FOUND.
     */
    @Test
    void delete_없는welfareId_404_WELFARE_NOT_FOUND() throws Exception {
        String accessToken = signupAndGetAccess();
        mockMvc.perform(delete("/api/bookmarks/{welfareId}", "NOEXIST")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("WELFARE_NOT_FOUND"));
    }

    /**
     * DELETE 본인 북마크 없음 → 404 BOOKMARK_NOT_FOUND (welfare는 존재).
     */
    @Test
    void delete_없는북마크_404_BOOKMARK_NOT_FOUND() throws Exception {
        String accessToken = signupAndGetAccess();
        String welfareId = pickAnyWelfareId();   // 존재하는 welfare지만 북마크는 안 함

        mockMvc.perform(delete("/api/bookmarks/{welfareId}", welfareId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BOOKMARK_NOT_FOUND"));
    }

    /**
     * DELETE 무인증 → 401 UNAUTHORIZED.
     */
    @Test
    void delete_무인증_401() throws Exception {
        String welfareId = pickAnyWelfareId();
        mockMvc.perform(delete("/api/bookmarks/{welfareId}", welfareId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    /**
     * GET 정상 — PageResponse 형식 + isBookmarked=true 고정.
     */
    @Test
    void get_정상_PageResponse_isBookmarkedTrue() throws Exception {
        String accessToken = signupAndGetAccess();
        String welfareId = pickAnyWelfareId();
        postAndGetBookmarkId(accessToken, welfareId);

        MvcResult res = mockMvc.perform(get("/api/bookmarks").param("size", "10")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andReturn();

        JsonNode items = objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("items");
        assertThat(items.size()).isEqualTo(1);
        assertThat(items.get(0).path("isBookmarked").asBoolean()).isTrue();
        assertThat(items.get(0).path("id").asText()).isEqualTo(welfareId);
    }

    /**
     * GET size 클램프 — 999 → 50.
     */
    @Test
    void get_size999_50으로클램프() throws Exception {
        String accessToken = signupAndGetAccess();
        mockMvc.perform(get("/api/bookmarks").param("size", "999")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(50));
    }

    /**
     * GET 무인증 → 401 UNAUTHORIZED.
     */
    @Test
    void get_무인증_401() throws Exception {
        mockMvc.perform(get("/api/bookmarks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    /**
     * 정렬 — 두 개 추가 후 GET 응답에서 가장 최근 추가가 첫 번째 (createdAt DESC).
     */
    @Test
    void get_정렬_최근저장순() throws Exception {
        String accessToken = signupAndGetAccess();
        // 두 개의 다른 welfareId 확보 (시드에서 LOCAL 2개 사용)
        String welfareId1 = pickWelfareId(WelfareType.LOCAL, 0);
        String welfareId2 = pickWelfareId(WelfareType.LOCAL, 1);

        postAndGetBookmarkId(accessToken, welfareId1);
        Thread.sleep(10);   // createdAt 분리를 위한 짧은 sleep (밀리초 해상도 보장)
        postAndGetBookmarkId(accessToken, welfareId2);

        MvcResult res = mockMvc.perform(get("/api/bookmarks")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("items");
        // 가장 최근 추가(welfareId2)가 첫 번째
        assertThat(items.get(0).path("id").asText()).isEqualTo(welfareId2);
        assertThat(items.get(1).path("id").asText()).isEqualTo(welfareId1);
    }

    /**
     * signup 후 accessToken 추출.
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

    /**
     * POST 후 응답에서 bookmarkId 추출.
     */
    private Long postAndGetBookmarkId(String accessToken, String welfareId) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/bookmarks/{welfareId}", welfareId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("bookmarkId").asLong();
    }

    /**
     * 시드에서 임의의 welfareId 하나를 가져온다 (CENTRAL 시드 첫 row).
     */
    private String pickAnyWelfareId() {
        return welfareCommonRepository.findByWelfareType(WelfareType.CENTRAL, PageRequest.of(0, 1))
                .getContent().get(0).getId();
    }

    /**
     * 특정 welfareType의 N번째 시드 ID.
     */
    private String pickWelfareId(WelfareType type, int index) {
        return welfareCommonRepository.findByWelfareType(type, PageRequest.of(0, index + 1))
                .getContent().get(index).getId();
    }
}
// 이 테스트의 역할: Bookmark 3개 엔드포인트의 풀 HTTP 사이클 + 인증/idempotent/404 분리/정렬 검증.
// 시드 적재된 ~1465 복지를 활용해 fixture 별도 셋업 없이 빠르게 통합 검증.
