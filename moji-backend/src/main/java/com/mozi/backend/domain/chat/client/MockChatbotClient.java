package com.mozi.backend.domain.chat.client;

import com.mozi.backend.domain.chat.client.dto.ChatbotRequest;
import com.mozi.backend.domain.chat.client.dto.ChatbotResponse;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.repository.WelfareCommonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 챗봇 서버를 흉내내는 Mock 구현체.
 *
 * 활성 조건: `mozi.chatbot.mock-enabled=true` (`application-local.yml`/`application-mock.yml`에서 ON).
 * 시연·통합 테스트에서 챗봇 서버 없이 단독으로 풀 흐름이 동작하도록 한다.
 *
 * 응답 결정성: `request.message().hashCode()`로 RNG seed를 고정 → 같은 메시지는 같은 추천 결과.
 * 이로써 통합 테스트가 안정적으로 통과한다.
 *
 * 추천 ID 선택: 시드 DB의 첫 100개 row에서 3개를 무작위 선택. 4개 출처가 골고루 등장하도록
 * 시드 적재 순서(Central→Local→Private→Seoul)에 의존.
 */
@Component
@ConditionalOnProperty(name = "mozi.chatbot.mock-enabled", havingValue = "true")
@RequiredArgsConstructor
public class MockChatbotClient implements ChatbotClient {

    /** Mock이 한 번에 추천할 복지 개수. */
    private static final int RECOMMEND_COUNT = 3;
    /** RNG 풀로 사용할 시드 복지 개수 — 4 출처 균형 등장 위해 충분히 크게. */
    private static final int POOL_SIZE = 100;

    private final WelfareCommonRepository welfareRepository;

    @Override
    @Transactional(readOnly = true)
    public ChatbotResponse send(ChatbotRequest request) {
        // 결정적 응답: 같은 message → 같은 추천 결과. 테스트 안정성 확보.
        Random rng = new Random(Math.abs((long) request.message().hashCode()));
        List<String> pool = welfareRepository.findAll(PageRequest.of(0, POOL_SIZE))
                .getContent().stream()
                .map(WelfareCommon::getId)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Collections.shuffle(pool, rng);
        List<String> picked = pool.stream().limit(RECOMMEND_COUNT).toList();

        String reply = "[Mock] '" + request.message() + "'에 대한 추천 복지입니다. 실제 LLM 추론 없이 시연용 응답입니다.";
        return new ChatbotResponse(reply, request.conversationId(), picked);
    }
}
// 이 클래스의 역할: 챗봇 서버 부재 시 풀 흐름 시연을 위한 결정적 Mock.
// 실 LLM 추론을 흉내내지 않으며, 추천 ID는 단순 무작위(시드 고정).
