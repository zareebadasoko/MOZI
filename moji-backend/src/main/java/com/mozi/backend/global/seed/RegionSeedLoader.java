package com.mozi.backend.global.seed;

import com.mozi.backend.domain.region.entity.Region;
import com.mozi.backend.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * REGION 마스터(약 229행)를 시드 적재하는 Loader.
 *
 * CategorySeedLoader 패턴과 동일 — count > 0이면 skip(idempotent). region 테이블이
 * 비어있을 때만 RegionSeedAdapter를 호출해 JSON 파싱 → saveAll. 모든 row를 단일
 * 트랜잭션 + Hibernate batch insert로 처리한다.
 *
 * REGION 마스터는 시드 적재 후 사실상 read-only — 사용자 입력으로 추가/수정되지 않는다.
 * 데이터 갱신이 필요하면 region.json 수정 + DB truncate 후 재부팅.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegionSeedLoader {

    private final RegionRepository regionRepository;
    private final RegionSeedAdapter regionSeedAdapter;

    /**
     * region.json을 파싱해 REGION 테이블에 적재. 이미 적재된 상태라면 skip.
     *
     * @Transactional로 229행을 단일 트랜잭션에 묶어 모두 성공 또는 모두 롤백.
     */
    @Transactional
    public void loadIfEmpty() {
        long existing = regionRepository.count();
        if (existing > 0) {
            log.info("Region seed skip — already loaded ({} rows)", existing);
            return;
        }

        List<Region> entities = regionSeedAdapter.parse();
        regionRepository.saveAll(entities);
        log.info("Region seeded: {} rows", entities.size());
    }
}
// 이 클래스의 역할: REGION 마스터의 1회성 적재.
// 229행이라 단일 트랜잭션 batch insert로 처리. batch_size=50 (application.yml)으로 5번 round-trip.
// UserSeedLoader의 sidoCode/sigunguCode 값이 본 시드 이후 검증 통과 가능 (단 시드 흐름은 검증 우회).
