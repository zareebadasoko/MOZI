package com.mozi.backend.domain.welfare;

import com.mozi.backend.domain.welfare.entity.WelfareCentral;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.entity.WelfareLocal;
import com.mozi.backend.domain.welfare.entity.WelfarePrivate;
import com.mozi.backend.domain.welfare.entity.WelfareSeoul;
import com.mozi.backend.domain.welfare.entity.WelfareType;
import com.mozi.backend.domain.welfare.repository.WelfareCommonRepository;
import com.mozi.backend.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Welfare 엔티티의 JOINED 상속 매핑이 ERD 설계대로 동작하는지 검증.
 *
 * 핵심 검증 포인트:
 * 1) 4개 자식 round-trip — 저장 후 부모 ID로 조회 시 원래 자식 타입으로 복원
 * 2) Discriminator 자동 채움 — welfare_type 컬럼이 자식 클래스 기반으로 자동 기록
 * 3) findByWelfareType 필터링 정상 동작
 * 4) WelfareSeoul이 welfare_seoul 테이블에만 저장되고 welfare_local에 잘못
 *    저장되지 않는지 확인 (ERD §3-2 핵심 규칙)
 *
 * @DataJpaTest는 트랜잭션 롤백 기본 제공이라 테스트 간 데이터 오염 없음.
 * @AutoConfigureTestDatabase(replace=NONE)으로 application-local.yml의 실제
 * MySQL 사용 — JOINED 동작이 DB 의존적이라 H2 대체 시 검증 신뢰도 낮음.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class WelfareEntityTest {

    @Autowired
    private WelfareCommonRepository repository;

    @PersistenceContext
    private EntityManager em;

    /**
     * 4개 자식을 각각 저장한 뒤 부모 ID로 조회했을 때 원래 자식 타입과
     * welfareType enum이 정확히 복원되는지 검증.
     */
    @Test
    void save_4개자식_저장후조회_원래타입으로복원() {
        WelfareCentral central = WelfareCentral.builder()
                .id("WLF99001").title("긴급돌봄 지원사업").supportYear(2026).build();
        WelfareLocal local = WelfareLocal.builder()
                .id("WLF99002").title("보훈명예수당").regionName("경기도 의정부시").build();
        WelfarePrivate priv = WelfarePrivate.builder()
                .id("BOK99001").title("의료복지카드").build();
        WelfareSeoul seoul = WelfareSeoul.builder()
                .id("SEL99001").title("고령장애인 활동지원").build();

        repository.save(central);
        repository.save(local);
        repository.save(priv);
        repository.save(seoul);
        em.flush();
        em.clear();

        WelfareCommon foundCentral = repository.findById("WLF99001").orElseThrow();
        WelfareCommon foundLocal = repository.findById("WLF99002").orElseThrow();
        WelfareCommon foundPrivate = repository.findById("BOK99001").orElseThrow();
        WelfareCommon foundSeoul = repository.findById("SEL99001").orElseThrow();

        assertThat(foundCentral).isInstanceOf(WelfareCentral.class);
        assertThat(foundCentral.getWelfareType()).isEqualTo(WelfareType.CENTRAL);

        assertThat(foundLocal).isInstanceOf(WelfareLocal.class);
        assertThat(foundLocal.getWelfareType()).isEqualTo(WelfareType.LOCAL);

        assertThat(foundPrivate).isInstanceOf(WelfarePrivate.class);
        assertThat(foundPrivate.getWelfareType()).isEqualTo(WelfareType.PRIVATE);

        assertThat(foundSeoul).isInstanceOf(WelfareSeoul.class);
        assertThat(foundSeoul.getWelfareType()).isEqualTo(WelfareType.SEOUL);
    }

    /**
     * findByWelfareType이 부모 컬럼 기반 필터링으로 원하는 타입만 반환하는지 검증.
     *
     * 시드 데이터(Phase 2-4)가 적재된 상태에서도 동작해야 하므로 4개 타입을
     * 모두 저장 후 LOCAL 결과에 본인 ID가 포함되고, CENTRAL/PRIVATE/SEOUL ID는
     * 포함되지 않는지 확인. 시드의 다른 LOCAL row가 같이 회수되더라도 OK.
     */
    @Test
    void findByWelfareType_LOCAL필터에_저장한row포함() {
        repository.save(WelfareCentral.builder().id("WLF98001").title("c").build());
        repository.save(WelfareLocal.builder().id("WLF98002").title("l").regionName("서울").build());
        repository.save(WelfarePrivate.builder().id("BOK98001").title("p").build());
        repository.save(WelfareSeoul.builder().id("SEL98001").title("s").build());
        em.flush();

        // size를 시드 LOCAL row 1325개 + 여유로 크게 잡아 본인 row가 누락되지 않게 함
        Page<WelfareCommon> page = repository.findByWelfareType(WelfareType.LOCAL, PageRequest.of(0, 10000));

        assertThat(page.getContent())
                .extracting(WelfareCommon::getId)
                .contains("WLF98002")
                .doesNotContain("WLF98001", "BOK98001", "SEL98001");
    }

    /**
     * WelfareSeoul 저장 시 welfare_seoul 테이블에만 row가 생기고
     * welfare_local 테이블엔 잘못 들어가지 않는지 native query로 확인.
     *
     * ERD §3-2 핵심 규칙(WelfareSeoul ≠ WelfareLocal)이 코드 레벨에서
     * 실제로 보장되는지 검증하는 안전장치.
     */
    @Test
    void seoul_local_별도테이블_검증() {
        repository.save(WelfareSeoul.builder().id("SEL97001").title("seoul-only").build());
        em.flush();
        em.clear();

        Number seoulCount = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM welfare_seoul WHERE id = :id")
                .setParameter("id", "SEL97001")
                .getSingleResult();
        Number localCount = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM welfare_local WHERE id = :id")
                .setParameter("id", "SEL97001")
                .getSingleResult();

        assertThat(seoulCount.intValue()).isEqualTo(1);
        assertThat(localCount.intValue()).isEqualTo(0);
    }
}
// 이 테스트의 역할: ERD §4-2 (B) JOINED 매핑이 실제 DB에서 올바르게 동작하는지 검증.
// 주의: @DataJpaTest는 H2 인메모리 대체가 기본이므로 @AutoConfigureTestDatabase로 끄지 않으면
// LONGTEXT/YEAR 같은 MySQL 특화 columnDefinition 검증이 신뢰성 떨어진다.
