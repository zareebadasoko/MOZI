package com.mozi.backend.domain.chat.client;

import com.mozi.backend.domain.chat.client.dto.ChatbotRequest;
import com.mozi.backend.domain.chat.client.dto.ChatbotResponse;
import com.mozi.backend.domain.chat.client.dto.ChatbotUserContext;
import com.mozi.backend.domain.welfare.entity.WelfareCentral;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.repository.WelfareCommonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * MockChatbotClient 단위 테스트.
 *
 * 검증 대상:
 *  - 결정적 응답 — 같은 message → 같은 추천 결과 (테스트 안정성)
 *  - 다른 message → 다른 추천 결과 (RNG seed 분기)
 *  - reply 본문에 사용자 message가 포함됨 (시연용 echo)
 *  - conversationId는 Request 그대로 통과
 */
@ExtendWith(MockitoExtension.class)
class MockChatbotClientTest {

    @Mock private WelfareCommonRepository welfareRepository;

    @InjectMocks
    private MockChatbotClient mockChatbotClient;

    /**
     * 같은 message로 두 번 호출 → 추천 ID 목록이 동일해야 함 (결정적 응답).
     */
    @Test
    void send_같은message_결정적응답() {
        Page<WelfareCommon> page = new PageImpl<>(generateWelfareCommons(50));
        when(welfareRepository.findAll(any(Pageable.class))).thenReturn(page);

        ChatbotRequest req = new ChatbotRequest("노인 일자리 알려줘", "conv-1",
                ChatbotUserContext.anonymous(1L));

        ChatbotResponse res1 = mockChatbotClient.send(req);
        ChatbotResponse res2 = mockChatbotClient.send(req);

        assertThat(res1.recommendedWelfareIds()).isEqualTo(res2.recommendedWelfareIds());
        assertThat(res1.recommendedWelfareIds()).hasSize(3);
    }

    /**
     * 다른 message → 다른 추천 ID (대부분의 경우. hashCode 충돌 매우 드뭄).
     */
    @Test
    void send_다른message_다른추천() {
        Page<WelfareCommon> page = new PageImpl<>(generateWelfareCommons(50));
        when(welfareRepository.findAll(any(Pageable.class))).thenReturn(page);

        ChatbotResponse res1 = mockChatbotClient.send(
                new ChatbotRequest("노인 일자리", "c1", ChatbotUserContext.anonymous(1L)));
        ChatbotResponse res2 = mockChatbotClient.send(
                new ChatbotRequest("기초연금 신청 방법", "c1", ChatbotUserContext.anonymous(1L)));

        assertThat(res1.recommendedWelfareIds())
                .as("다른 message는 다른 hashCode → 다른 무작위 시드 → 다른 추천")
                .isNotEqualTo(res2.recommendedWelfareIds());
    }

    /**
     * conversationId는 Request 값을 그대로 응답에 반환 (백엔드 발급한 ID 유지).
     */
    @Test
    void send_conversationId_그대로반환() {
        Page<WelfareCommon> page = new PageImpl<>(generateWelfareCommons(50));
        when(welfareRepository.findAll(any(Pageable.class))).thenReturn(page);

        ChatbotRequest req = new ChatbotRequest("hello", "convo-abc-123",
                ChatbotUserContext.anonymous(1L));

        ChatbotResponse res = mockChatbotClient.send(req);

        assertThat(res.conversationId()).isEqualTo("convo-abc-123");
    }

    /**
     * reply 텍스트에 [Mock] prefix + 사용자 message 포함 (시연 안내).
     */
    @Test
    void send_reply_Mock표시_message포함() {
        Page<WelfareCommon> page = new PageImpl<>(generateWelfareCommons(50));
        when(welfareRepository.findAll(any(Pageable.class))).thenReturn(page);

        ChatbotResponse res = mockChatbotClient.send(
                new ChatbotRequest("의료비 지원", "c1", ChatbotUserContext.anonymous(1L)));

        assertThat(res.reply()).contains("[Mock]").contains("의료비 지원");
    }

    /**
     * 시드 row가 3개 미만이면 반환되는 picked도 그만큼 (POOL_SIZE보다 작은 경우 안전 처리 확인).
     */
    @Test
    void send_시드row적음_picked도그만큼() {
        Page<WelfareCommon> page = new PageImpl<>(generateWelfareCommons(2));   // 2 row only
        when(welfareRepository.findAll(any(Pageable.class))).thenReturn(page);

        ChatbotResponse res = mockChatbotClient.send(
                new ChatbotRequest("hello", "c1", ChatbotUserContext.anonymous(1L)));

        assertThat(res.recommendedWelfareIds()).hasSize(2);
    }

    private List<WelfareCommon> generateWelfareCommons(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> (WelfareCommon) WelfareCentral.builder()
                        .id(String.format("WLF%08d", i))
                        .title("title-" + i)
                        .build())
                .toList();
    }
}
// 이 테스트의 역할: Mock 구현체의 결정성 + 응답 형식 검증.
// 통합 테스트에서 RNG seed가 안정적인지 확인하는 것이 가장 중요한 검증 포인트.
