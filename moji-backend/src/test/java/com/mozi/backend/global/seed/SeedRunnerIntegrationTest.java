package com.mozi.backend.global.seed;

import com.mozi.backend.domain.category.repository.CategoryRepository;
import com.mozi.backend.domain.category.repository.WelfareCategoryRepository;
import com.mozi.backend.domain.region.repository.RegionRepository;
import com.mozi.backend.domain.user.repository.UserProfileRepository;
import com.mozi.backend.domain.user.repository.UserRepository;
import com.mozi.backend.domain.welfare.entity.WelfareType;
import com.mozi.backend.domain.welfare.repository.WelfareCommonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시드 적재의 통합 검증.
 *
 * @SpringBootTest로 전체 컨텍스트를 띄우면 SeedRunner가 자동 실행되어
 * 모든 시드 데이터가 DB에 적재된 상태가 된다. 본 테스트는 적재 결과의
 * 정합성(row count, 4개 출처 분포 등)을 확인하는 sanity check.
 *
 * 주의: SeedRunner의 트랜잭션은 테스트 트랜잭션과 분리되어 적재 데이터가
 * 실제 mozi_dev DB에 남는다. 시드 의도가 그러함 — 시연 환경 즉시 사용 가능.
 */
@SpringBootTest
@TestPropertySource(properties = "mozi.seed.enabled=true")
class SeedRunnerIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WelfareCommonRepository welfareCommonRepository;

    @Autowired
    private WelfareCategoryRepository welfareCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RegionRepository regionRepository;

    /**
     * 시드 적재 후 각 도메인의 row 수가 기대치를 만족하는지 sanity check.
     */
    @Test
    void seedRunner_적재후_row수_검증() {
        // Category: THEME 15 + STATUS 7 = 22
        assertThat(categoryRepository.count()).isEqualTo(22);

        // Welfare: 4개 출처 합쳐 충분히 많아야 함 (실제 데이터 200~500+ row 추정)
        assertThat(welfareCommonRepository.count()).isGreaterThan(50);

        // 4개 출처 모두 적재됐는지 확인
        assertThat(welfareCommonRepository.findByWelfareType(WelfareType.CENTRAL,
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements())
                .isGreaterThan(0);
        assertThat(welfareCommonRepository.findByWelfareType(WelfareType.LOCAL,
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements())
                .isGreaterThan(0);
        assertThat(welfareCommonRepository.findByWelfareType(WelfareType.PRIVATE,
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements())
                .isGreaterThan(0);
        assertThat(welfareCommonRepository.findByWelfareType(WelfareType.SEOUL,
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements())
                .isGreaterThan(0);

        // WelfareCategory 매핑은 row 수만큼 또는 그 이상
        assertThat(welfareCategoryRepository.count()).isGreaterThan(50);

        // 가짜 사용자 7명 + 프로필 7개
        assertThat(userRepository.count()).isEqualTo(7);
        assertThat(userProfileRepository.count()).isEqualTo(7);
    }

    /**
     * Step 8 추가: REGION 마스터 시드 적재 검증.
     *
     * 행정안전부 법정동코드 기준 17 시도 + 약 229 시군구 (세종 1행 포함).
     * 첫 행 sanity로 sidoCode 자릿수 + sidoName 풀네임 형태 확인.
     */
    @Test
    void seed_적재후_region_229행_검증() {
        assertThat(regionRepository.count()).isEqualTo(229);

        // sanity: 적어도 한 행은 채워진 형태 (sidoCode 2자리, 풀네임)
        var first = regionRepository.findAll().iterator().next();
        assertThat(first.getSidoCode()).hasSize(2);
        assertThat(first.getSidoName()).isNotBlank();
    }

    /**
     * 시드 사용자의 비밀번호가 BCrypt 형식으로 저장됐는지 확인.
     *
     * BCrypt 해시는 "$2a$" 또는 "$2b$" prefix로 시작 — 평문 저장 방지 검증.
     */
    @Test
    void 시드사용자_비밀번호_BCrypt해싱_검증() {
        var user = userRepository.findByEmail("senior01@mozi.test").orElseThrow();
        assertThat(user.getPassword())
                .startsWith("$2")
                .hasSize(60);  // BCrypt 해시는 항상 60자
    }
}
// 이 테스트의 역할: 시드 적재 결과의 정합성 + 보안 정책(BCrypt) 검증.
// @SpringBootTest는 전체 빈 등록 + ApplicationRunner 자동 실행이라
// SeedRunner가 컨텍스트 시작 시점에 1회 실행됨.
