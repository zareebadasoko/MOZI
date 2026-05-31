package com.mozi.backend.domain.chat.service;

import com.mozi.backend.domain.bookmark.repository.BookmarkRepository;
import com.mozi.backend.domain.category.dto.CategoryDto;
import com.mozi.backend.domain.category.repository.WelfareCategoryRepository;
import com.mozi.backend.domain.chat.client.ChatbotClient;
import com.mozi.backend.domain.chat.client.dto.ChatbotRequest;
import com.mozi.backend.domain.chat.client.dto.ChatbotResponse;
import com.mozi.backend.domain.chat.client.dto.ChatbotUserContext;
import com.mozi.backend.domain.chat.dto.ChatRequest;
import com.mozi.backend.domain.chat.dto.ChatResponse;
import com.mozi.backend.domain.region.entity.Region;
import com.mozi.backend.domain.region.repository.RegionRepository;
import com.mozi.backend.domain.user.entity.UserProfile;
import com.mozi.backend.domain.user.repository.UserProfileRepository;
import com.mozi.backend.domain.welfare.dto.WelfareSummaryDto;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.repository.WelfareCommonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 챗봇 브릿지 비즈니스 로직.
 *
 * 핵심 흐름:
 * 1) conversationId 정규화 — 없으면 UUID v4 발급
 * 2) UserProfile 조회 (lazy creation 정책상 null 가능) → ChatbotUserContext 생성
 *    Step 7: 행정구역 코드 → 한글 라벨, birthDate → age 변환을 본 서비스가 책임
 * 3) ChatbotClient 호출 — Mock 또는 실 구현체 (mock-enabled 토글로 분기)
 * 4) 추천 welfareId hydrate — 4-2/4-3 산출물(WelfareSummaryDto, EntityGraph 등) 재사용,
 *    챗봇 반환 순서 유지, 누락 ID는 WARN 로그 후 제외
 *
 * 트랜잭션 정책: 외부 호출(`chatbotClient.send`) 구간은 트랜잭션 외부 — 장시간 lock 회피.
 * DB 조회는 메서드 분리해서 readonly transactional 처리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatbotClient chatbotClient;
    private final UserProfileRepository userProfileRepository;
    private final RegionRepository regionRepository;
    private final WelfareCommonRepository welfareRepository;
    private final WelfareCategoryRepository welfareCategoryRepository;
    private final BookmarkRepository bookmarkRepository;
    private final Clock clock;

    /**
     * 챗봇 호출 전체 흐름.
     *
     * @param userId SecurityContext에서 추출한 인증 사용자 PK
     * @param request 클라이언트가 보낸 메시지 + 선택적 conversationId
     * @return 답변 텍스트 + 추천 복지 목록(WelfareSummaryDto[]) + conversationId
     */
    public ChatResponse chat(Long userId, ChatRequest request) {
        // 1. conversationId 정규화 — null/blank이면 백엔드가 UUID v4 발급
        String conversationId = normalizeConversationId(request.conversationId());

        // 2. UserProfile 조회 (없으면 anonymous context 전송)
        ChatbotUserContext userContext = loadUserContext(userId);

        // 3. 외부 호출 — 도메인 예외(ChatbotTimeout/Unavailable/InvalidResponse)는 그대로 전파
        ChatbotResponse chatbotResponse = chatbotClient.send(
                new ChatbotRequest(request.message(), conversationId, userContext));

        // 4. 추천 ID → WelfareSummaryDto[] hydrate (N+1 회피 batch lookup)
        List<WelfareSummaryDto> welfares = hydrateWelfares(userId, chatbotResponse.recommendedWelfareIds());

        return new ChatResponse(chatbotResponse.reply(), welfares, conversationId);
    }

    /**
     * conversationId 정규화 — null/blank이면 UUID v4 발급, 있으면 그대로 통과.
     */
    private String normalizeConversationId(String requestConversationId) {
        if (requestConversationId == null || requestConversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestConversationId;
    }

    /**
     * 사용자 프로필을 ChatbotUserContext로 변환 — 프로필 미입력 시 anonymous.
     *
     * Step 7 본격 변환:
     *  - 행정구역 코드 → REGION 마스터 lookup → sidoName/sigunguName 한글 라벨
     *  - birthDate → Clock 기반 만 나이 정수(age)
     *  - enum → label은 ChatbotUserContext.from()이 담당
     *
     * REGION 시드 미적재 상태에서는 sidoName/sigunguName이 null로 전송된다.
     *
     * @return userId만 채워진(anonymous) 또는 신규 스키마 8 필드 채운 ChatbotUserContext
     */
    @Transactional(readOnly = true)
    public ChatbotUserContext loadUserContext(Long userId) {
        Optional<UserProfile> maybeProfile = userProfileRepository.findByUser_Id(userId);
        if (maybeProfile.isEmpty()) {
            return ChatbotUserContext.anonymous(userId);
        }
        UserProfile profile = maybeProfile.get();
        String sidoName = resolveSidoName(profile);
        String sigunguName = resolveSigunguName(profile);
        Integer age = calculateAge(profile.getBirthDate());
        return ChatbotUserContext.from(userId, profile, sidoName, sigunguName, age);
    }

    /**
     * sidoCode → sidoName 변환. sigunguCode가 있으면 거기서 sidoName도 함께 추출되므로
     * 본 메서드는 "시도만 선택" 케이스를 위해 sigunguCode null인 경우만 lookup한다.
     *
     * @param profile 영속화된 UserProfile
     * @return 시도 한글 이름 (없거나 REGION 미매칭이면 null)
     */
    private String resolveSidoName(UserProfile profile) {
        if (profile.getSidoCode() == null) {
            return null;
        }
        // sigunguCode가 있으면 resolveSigunguName 쪽 lookup이 동일 row를 만진다 — 중복 호출 회피
        if (profile.getSigunguCode() != null) {
            return regionRepository.findBySigunguCode(profile.getSigunguCode())
                    .map(Region::getSidoName)
                    .orElse(null);
        }
        return regionRepository.findFirstBySidoCode(profile.getSidoCode())
                .map(Region::getSidoName)
                .orElse(null);
    }

    /**
     * sigunguCode → sigunguName 변환. sigunguCode가 null이면 null 반환.
     *
     * @param profile 영속화된 UserProfile
     * @return 시군구 한글 이름 (없거나 REGION 미매칭이면 null)
     */
    private String resolveSigunguName(UserProfile profile) {
        if (profile.getSigunguCode() == null) {
            return null;
        }
        return regionRepository.findBySigunguCode(profile.getSigunguCode())
                .map(Region::getSigunguName)
                .orElse(null);
    }

    /**
     * birthDate를 Clock 기준 만 나이로 변환.
     *
     * Clock 빈을 사용하므로 단위 테스트에서 시각 고정이 가능하다. Period 계산은
     * 윤년·월말 경계를 자연스럽게 처리.
     *
     * @param birthDate 생년월일 (null 가능)
     * @return 만 나이 정수 (birthDate가 null이면 null)
     */
    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now(clock)).getYears();
    }

    /**
     * 챗봇이 반환한 welfareId 목록을 WelfareSummaryDto[]로 hydrate한다.
     *
     * 처리:
     * - DB 미존재 ID는 응답에서 제외 + WARN 로그 (챗봇 학습 데이터 갱신 신호)
     * - 챗봇이 반환한 순서를 유지 (Map<id, entity>로 lookup 후 입력 순서대로 매핑)
     * - categories와 isBookmarked는 4-2 패턴 그대로 batch IN으로 조회 (N+1 회피)
     *
     * @param userId 인증 사용자 PK (isBookmarked 계산용)
     * @param recommendedIds 챗봇이 반환한 추천 ID 목록 (순서 유지)
     * @return 챗봇 순서대로 매핑된 WelfareSummaryDto 목록 (없으면 빈 리스트)
     */
    @Transactional(readOnly = true)
    public List<WelfareSummaryDto> hydrateWelfares(Long userId, List<String> recommendedIds) {
        if (recommendedIds == null || recommendedIds.isEmpty()) {
            return List.of();
        }

        List<WelfareCommon> welfares = welfareRepository.findAllById(recommendedIds);

        // DB 미존재 ID 로깅
        if (welfares.size() < recommendedIds.size()) {
            Set<String> foundIds = welfares.stream().map(WelfareCommon::getId).collect(Collectors.toSet());
            recommendedIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .forEach(missing -> log.warn("Chatbot recommended unknown welfareId: {}", missing));
        }

        Map<String, List<CategoryDto>> categoryMap = loadCategoryMap(recommendedIds);
        Set<String> bookmarkedIds = new HashSet<>(
                bookmarkRepository.findWelfareIdsByUser_IdAndWelfareCommon_IdIn(userId, recommendedIds));

        // 챗봇 반환 순서 유지를 위해 Map으로 lookup
        Map<String, WelfareCommon> byId = welfares.stream()
                .collect(Collectors.toMap(WelfareCommon::getId, w -> w));

        return recommendedIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(w -> WelfareSummaryDto.of(
                        w,
                        categoryMap.getOrDefault(w.getId(), List.of()),
                        bookmarkedIds.contains(w.getId())
                ))
                .toList();
    }

    /**
     * 4-2 N+1 회피 패턴 재사용 — welfare ID들로 카테고리를 한 번에 IN 쿼리한 뒤 ID별 그루핑.
     */
    private Map<String, List<CategoryDto>> loadCategoryMap(Collection<String> welfareIds) {
        if (welfareIds.isEmpty()) {
            return Map.of();
        }
        return welfareCategoryRepository.findByWelfareCommon_IdIn(welfareIds).stream()
                .collect(Collectors.groupingBy(
                        wc -> wc.getWelfareCommon().getId(),
                        Collectors.mapping(wc -> CategoryDto.from(wc.getCategory()), Collectors.toList())
                ));
    }
}
// 이 클래스의 역할: 챗봇 호출 흐름의 단일 진입점.
// Step 7에서 행정구역 라벨 변환 + 만 나이 계산까지 본 서비스가 책임. ChatbotUserContext는 단순 매핑만.
// 외부 호출과 DB 트랜잭션을 의도적으로 분리해 장시간 lock 회피.
// 추천 ID 누락은 그라스풀 처리 — UX 깨지지 않게 + WARN 로그로 추적성 확보.
