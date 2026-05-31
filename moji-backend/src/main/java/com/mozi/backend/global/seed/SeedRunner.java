package com.mozi.backend.global.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 부팅 시 시드 데이터 적재를 트리거하는 마스터 Runner.
 *
 * `mozi.seed.enabled=true`(application-local.yml)일 때만 본 빈이 등록되어
 * ApplicationRunner.run이 실행된다. prod/mock 프로파일에선 빈 미등록 → 적재 안 함.
 *
 * 실행 순서:
 *   1) Category 22행 (다른 시드들의 lookup 캐시 기반)
 *   2) Region 229행 (Step 8: 행정구역 마스터 — UserProfile sidoCode/sigunguCode 검증의 기준)
 *   3) Welfare 4종 + WelfareCategory 매핑 (Category에 의존)
 *   4) 가짜 사용자 7명 + UserProfile (다른 도메인과 독립)
 *
 * 각 Loader는 자체 @Transactional 경계를 가지므로 한 도메인 실패가 다른 도메인을
 * 롤백시키지 않는다. 모든 Loader는 idempotent (count 또는 sentinel 체크 후 skip).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mozi.seed.enabled", havingValue = "true")
public class SeedRunner implements ApplicationRunner {

    private final CategorySeedLoader categorySeedLoader;
    private final RegionSeedLoader regionSeedLoader;
    private final WelfareSeedLoader welfareSeedLoader;
    private final UserSeedLoader userSeedLoader;

    /**
     * Spring Boot 부팅 후 자동 호출.
     *
     * @param args ApplicationArguments (시드 적재에선 미사용)
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("Seed loading started (mozi.seed.enabled=true)");
        categorySeedLoader.loadIfEmpty();
        regionSeedLoader.loadIfEmpty();
        welfareSeedLoader.loadIfEmpty();
        userSeedLoader.loadIfEmpty();
        log.info("Seed loading finished");
    }
}
// 이 클래스의 역할: 시드 적재의 단일 진입점.
// @ConditionalOnProperty 덕에 prod 환경에선 빈 자체가 등록 안 됨 → 시드 코드가 아예 실행되지 않음.
// 시드 데이터가 변경되어 재적재가 필요하면 사용자가 직접 DB truncate 후 재부팅.
