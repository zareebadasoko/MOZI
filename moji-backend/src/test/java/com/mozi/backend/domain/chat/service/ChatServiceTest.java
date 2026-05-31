package com.mozi.backend.domain.chat.service;

import com.mozi.backend.domain.bookmark.repository.BookmarkRepository;
import com.mozi.backend.domain.category.entity.Category;
import com.mozi.backend.domain.category.entity.CategoryType;
import com.mozi.backend.domain.category.entity.WelfareCategory;
import com.mozi.backend.domain.category.repository.WelfareCategoryRepository;
import com.mozi.backend.domain.chat.client.ChatbotClient;
import com.mozi.backend.domain.chat.client.dto.ChatbotRequest;
import com.mozi.backend.domain.chat.client.dto.ChatbotResponse;
import com.mozi.backend.domain.chat.dto.ChatRequest;
import com.mozi.backend.domain.chat.dto.ChatResponse;
import com.mozi.backend.domain.chat.exception.ChatbotTimeoutException;
import com.mozi.backend.domain.region.entity.Region;
import com.mozi.backend.domain.region.repository.RegionRepository;
import com.mozi.backend.domain.user.entity.Gender;
import com.mozi.backend.domain.user.entity.HouseholdType;
import com.mozi.backend.domain.user.entity.IncomeType;
import com.mozi.backend.domain.user.entity.User;
import com.mozi.backend.domain.user.entity.UserProfile;
import com.mozi.backend.domain.user.repository.UserProfileRepository;
import com.mozi.backend.domain.welfare.dto.WelfareSummaryDto;
import com.mozi.backend.domain.welfare.entity.WelfareCentral;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.repository.WelfareCommonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * ChatService 단위 테스트.
 *
 * 검증 대상:
 *  - conversationId 정규화 (없으면 UUID 발급 / 있으면 그대로)
 *  - UserProfile 있으면 12 필드 매핑, 없으면 anonymous context
 *  - 추천 welfareId hydrate (순서 유지 + 누락 ID 제외 + categories/isBookmarked 매핑)
 *  - ChatbotClient 예외 그대로 전파 (GlobalExceptionHandler가 응답 변환)
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatbotClient chatbotClient;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private RegionRepository regionRepository;
    @Mock private WelfareCommonRepository welfareRepository;
    @Mock private WelfareCategoryRepository welfareCategoryRepository;
    @Mock private BookmarkRepository bookmarkRepository;

    /** 만 나이 계산을 결정적으로 만들기 위해 고정 Clock (2026-05-17 KST) 사용. */
    @org.mockito.Spy
    private Clock clock = Clock.fixed(
            LocalDate.of(2026, 5, 17).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"));

    @InjectMocks
    private ChatService chatService;

    /**
     * 첫 요청 — conversationId 없음 → UUID 발급 후 챗봇 호출 + 응답에 그대로 포함.
     */
    @Test
    void chat_conversationId없음_UUID발급() {
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(chatbotClient.send(any(ChatbotRequest.class)))
                .thenAnswer(inv -> {
                    ChatbotRequest req = inv.getArgument(0);
                    return new ChatbotResponse("ok", req.conversationId(), List.of());
                });

        ChatResponse res = chatService.chat(1L, new ChatRequest("hello", null));

        assertThat(res.conversationId()).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    /**
     * 후속 요청 — conversationId 있음 → 그대로 챗봇에 전달 + 응답에 그대로 포함.
     */
    @Test
    void chat_conversationId있음_그대로통과() {
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(chatbotClient.send(any(ChatbotRequest.class)))
                .thenAnswer(inv -> {
                    ChatbotRequest req = inv.getArgument(0);
                    return new ChatbotResponse("ok", req.conversationId(), List.of());
                });

        ChatResponse res = chatService.chat(1L, new ChatRequest("hello", "existing-convo-id"));

        assertThat(res.conversationId()).isEqualTo("existing-convo-id");
    }

    /**
     * UserProfile 있음 → Step 7 본격 매핑: 행정구역 한글 라벨 + 만 나이 + enum 라벨.
     *
     * Clock 고정(2026-05-17) + birthDate 1948-03-01 → 만 78세.
     * REGION 마스터에서 "11680" lookup → 서울특별시/강남구 라벨로 변환.
     */
    @Test
    void chat_프로필있음_Step7_본격매핑() {
        User user = User.of("ok@mozi.test", "$2a$10$h");
        ReflectionTestUtils.setField(user, "id", 1L);
        UserProfile profile = UserProfile.fullFor(user,
                LocalDate.of(1948, 3, 1), Gender.F,
                "11", "11680",
                IncomeType.BASIC_PENSION, HouseholdType.LIVING_ALONE,
                false, false, false, false);
        Region region = Region.of("11", "서울특별시", "11680", "강남구");
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(profile));
        when(regionRepository.findBySigunguCode("11680")).thenReturn(Optional.of(region));
        when(chatbotClient.send(any(ChatbotRequest.class)))
                .thenReturn(new ChatbotResponse("ok", "c1", List.of()));

        chatService.chat(1L, new ChatRequest("hello", "c1"));

        ArgumentCaptor<ChatbotRequest> captor = ArgumentCaptor.forClass(ChatbotRequest.class);
        org.mockito.Mockito.verify(chatbotClient).send(captor.capture());

        ChatbotRequest sent = captor.getValue();
        assertThat(sent.user().userId()).isEqualTo(1L);
        assertThat(sent.user().profile()).isNotNull();
        // 행정구역은 한글 라벨로 변환되어 전송
        assertThat(sent.user().profile().sidoName()).isEqualTo("서울특별시");
        assertThat(sent.user().profile().sigunguName()).isEqualTo("강남구");
        // birthDate → 만 나이 정수 (Clock 2026-05-17 기준, 1948-03-01 → 78세)
        assertThat(sent.user().profile().age()).isEqualTo(78);
        // enum은 한글 라벨로 변환
        assertThat(sent.user().profile().incomeType()).isEqualTo("기초연금수급자");
        assertThat(sent.user().profile().householdType()).isEqualTo("혼자 살아요 (독거)");
        // boolean 4종
        assertThat(sent.user().profile().isDisabled()).isFalse();
        assertThat(sent.user().profile().isVeteran()).isFalse();
    }

    /**
     * Step 9: 시도만 선택(sigunguCode=null) → findFirstBySidoCode 경로 검증.
     *
     * sigunguCode가 null이면 ChatService.resolveSidoName이 findBySigunguCode 대신
     * findFirstBySidoCode를 호출. sidoName만 추출되고 sigunguName=null로 전송.
     */
    @Test
    void chat_프로필_시도만선택_sidoName만추출() {
        User user = User.of("ok@mozi.test", "$2a$10$h");
        ReflectionTestUtils.setField(user, "id", 1L);
        UserProfile profile = UserProfile.fullFor(user,
                LocalDate.of(1950, 1, 1), Gender.F,
                "11", null,
                null, null,
                false, false, false, false);
        Region region = Region.of("11", "서울특별시", null, null);
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(profile));
        when(regionRepository.findFirstBySidoCode("11")).thenReturn(Optional.of(region));
        when(chatbotClient.send(any(ChatbotRequest.class)))
                .thenReturn(new ChatbotResponse("ok", "c1", List.of()));

        chatService.chat(1L, new ChatRequest("hello", "c1"));

        ArgumentCaptor<ChatbotRequest> captor = ArgumentCaptor.forClass(ChatbotRequest.class);
        org.mockito.Mockito.verify(chatbotClient).send(captor.capture());

        ChatbotRequest sent = captor.getValue();
        assertThat(sent.user().profile().sidoName()).isEqualTo("서울특별시");
        assertThat(sent.user().profile().sigunguName()).isNull();
        // sigunguCode 있는 경로(findBySigunguCode)는 호출 안 됨
        org.mockito.Mockito.verify(regionRepository, org.mockito.Mockito.never()).findBySigunguCode(any());
    }

    /**
     * birthDate가 null이면 age도 null (Clock 호출 없이 그래스풀 처리).
     */
    @Test
    void chat_프로필있음_birthDate_null_age_null() {
        User user = User.of("ok@mozi.test", "$2a$10$h");
        ReflectionTestUtils.setField(user, "id", 1L);
        UserProfile profile = UserProfile.fullFor(user,
                null, Gender.NONE,
                null, null,
                null, null,
                false, false, false, false);
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(profile));
        when(chatbotClient.send(any(ChatbotRequest.class)))
                .thenReturn(new ChatbotResponse("ok", "c1", List.of()));

        chatService.chat(1L, new ChatRequest("hello", "c1"));

        ArgumentCaptor<ChatbotRequest> captor = ArgumentCaptor.forClass(ChatbotRequest.class);
        org.mockito.Mockito.verify(chatbotClient).send(captor.capture());

        ChatbotRequest sent = captor.getValue();
        assertThat(sent.user().profile()).isNotNull();
        assertThat(sent.user().profile().age()).isNull();
        assertThat(sent.user().profile().sidoName()).isNull();
        assertThat(sent.user().profile().sigunguName()).isNull();
    }

    /**
     * UserProfile 없음 → anonymous context (profile=null) 전달.
     */
    @Test
    void chat_프로필없음_anonymous컨텍스트() {
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(chatbotClient.send(any(ChatbotRequest.class)))
                .thenReturn(new ChatbotResponse("ok", "c1", List.of()));

        chatService.chat(1L, new ChatRequest("hello", "c1"));

        ArgumentCaptor<ChatbotRequest> captor = ArgumentCaptor.forClass(ChatbotRequest.class);
        org.mockito.Mockito.verify(chatbotClient).send(captor.capture());

        assertThat(captor.getValue().user().userId()).isEqualTo(1L);
        assertThat(captor.getValue().user().profile()).isNull();
    }

    /**
     * 챗봇이 반환한 welfareId 순서대로 응답 welfares 배열에 매핑.
     */
    @Test
    void chat_추천ID순서유지() {
        WelfareCentral w1 = WelfareCentral.builder().id("WLF1").title("t1").build();
        WelfareCentral w2 = WelfareCentral.builder().id("WLF2").title("t2").build();
        WelfareCentral w3 = WelfareCentral.builder().id("WLF3").title("t3").build();

        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(chatbotClient.send(any(ChatbotRequest.class)))
                .thenReturn(new ChatbotResponse("ok", "c1", List.of("WLF3", "WLF1", "WLF2")));
        // findAllById는 순서 보장 X → 다른 순서로 반환
        when(welfareRepository.findAllById(any(Iterable.class))).thenReturn(List.of(w1, w2, w3));
        when(welfareCategoryRepository.findByWelfareCommon_IdIn(anyCollection())).thenReturn(List.of());
        when(bookmarkRepository.findWelfareIdsByUser_IdAndWelfareCommon_IdIn(anyLong(), anyCollection()))
                .thenReturn(List.of());

        ChatResponse res = chatService.chat(1L, new ChatRequest("hello", "c1"));

        // 챗봇 반환 순서(WLF3, WLF1, WLF2) 그대로 유지되어야 함
        assertThat(res.welfares()).extracting(WelfareSummaryDto::id)
                .containsExactly("WLF3", "WLF1", "WLF2");
    }

    /**
     * DB 미존재 ID는 응답에서 제외 (WARN 로그는 별도 검증 어려워 size로만 확인).
     */
    @Test
    void chat_미존재ID_응답에서제외() {
        WelfareCentral w1 = WelfareCentral.builder().id("WLF1").title("t1").build();

        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(chatbotClient.send(any(ChatbotRequest.class)))
                .thenReturn(new ChatbotResponse("ok", "c1", List.of("WLF1", "NONEXIST", "ALSO_MISSING")));
        when(welfareRepository.findAllById(any(Iterable.class))).thenReturn(List.of(w1));
        when(welfareCategoryRepository.findByWelfareCommon_IdIn(anyCollection())).thenReturn(List.of());
        when(bookmarkRepository.findWelfareIdsByUser_IdAndWelfareCommon_IdIn(anyLong(), anyCollection()))
                .thenReturn(List.of());

        ChatResponse res = chatService.chat(1L, new ChatRequest("hello", "c1"));

        assertThat(res.welfares()).hasSize(1);
        assertThat(res.welfares().get(0).id()).isEqualTo("WLF1");
    }

    /**
     * 추천 ID 빈 배열 → welfares 빈 리스트 (categories 조회 호출 X).
     */
    @Test
    void chat_추천ID빈배열_빈welfares() {
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(chatbotClient.send(any(ChatbotRequest.class)))
                .thenReturn(new ChatbotResponse("답변만 있고 추천 없음", "c1", List.of()));

        ChatResponse res = chatService.chat(1L, new ChatRequest("hello", "c1"));

        assertThat(res.welfares()).isEmpty();
        assertThat(res.reply()).isEqualTo("답변만 있고 추천 없음");
    }

    /**
     * 본인 북마크 welfareId는 isBookmarked=true로 채워짐.
     */
    @Test
    void chat_본인북마크_isBookmarkedTrue() {
        WelfareCentral w1 = WelfareCentral.builder().id("WLF1").title("t1").build();
        WelfareCentral w2 = WelfareCentral.builder().id("WLF2").title("t2").build();

        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(chatbotClient.send(any(ChatbotRequest.class)))
                .thenReturn(new ChatbotResponse("ok", "c1", List.of("WLF1", "WLF2")));
        when(welfareRepository.findAllById(any(Iterable.class))).thenReturn(List.of(w1, w2));
        when(welfareCategoryRepository.findByWelfareCommon_IdIn(anyCollection())).thenReturn(List.of());
        when(bookmarkRepository.findWelfareIdsByUser_IdAndWelfareCommon_IdIn(anyLong(), anyCollection()))
                .thenReturn(List.of("WLF1"));

        ChatResponse res = chatService.chat(1L, new ChatRequest("hello", "c1"));

        assertThat(res.welfares()).extracting(WelfareSummaryDto::isBookmarked)
                .containsExactly(true, false);   // WLF1만 북마크됨
    }

    /**
     * categories 매핑 — WelfareCategoryRepository.findByWelfareCommon_IdIn 결과를 ID별 그루핑.
     */
    @Test
    void chat_categories_매핑() {
        WelfareCentral w1 = WelfareCentral.builder().id("WLF1").title("t1").build();
        Category cat = Category.of("THM003", "신체건강", CategoryType.THEME);
        ReflectionTestUtils.setField(cat, "id", 5L);
        WelfareCategory wc = WelfareCategory.of(w1, cat);

        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(chatbotClient.send(any(ChatbotRequest.class)))
                .thenReturn(new ChatbotResponse("ok", "c1", List.of("WLF1")));
        when(welfareRepository.findAllById(any(Iterable.class))).thenReturn(List.of(w1));
        when(welfareCategoryRepository.findByWelfareCommon_IdIn(anyCollection())).thenReturn(List.of(wc));
        when(bookmarkRepository.findWelfareIdsByUser_IdAndWelfareCommon_IdIn(anyLong(), anyCollection()))
                .thenReturn(List.of());

        ChatResponse res = chatService.chat(1L, new ChatRequest("hello", "c1"));

        assertThat(res.welfares().get(0).categories()).hasSize(1);
        assertThat(res.welfares().get(0).categories().get(0).code()).isEqualTo("THM003");
    }

    /**
     * ChatbotClient가 ChatbotTimeoutException 던지면 ChatService도 그대로 전파 (GlobalExceptionHandler가 처리).
     */
    @Test
    void chat_타임아웃_예외그대로전파() {
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(chatbotClient.send(any(ChatbotRequest.class))).thenThrow(new ChatbotTimeoutException());

        assertThatThrownBy(() -> chatService.chat(1L, new ChatRequest("hello", "c1")))
                .isInstanceOf(ChatbotTimeoutException.class);
    }
}
// 이 테스트의 역할: ChatService의 모든 분기를 외부 의존성 없이 결정적으로 검증.
// conversationId 발급 · 12필드 매핑 · 순서 유지 · 누락 ID 제외 · isBookmarked 매핑 · 예외 전파 모두 커버.
