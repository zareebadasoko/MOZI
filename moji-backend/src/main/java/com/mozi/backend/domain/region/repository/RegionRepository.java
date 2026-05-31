package com.mozi.backend.domain.region.repository;

import com.mozi.backend.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Region 마스터 영속성 접근 인터페이스.
 *
 * 조회: GET /api/regions 응답용 2종.
 * 검증(Step 4): UserProfile PUT 시 sidoCode/sigunguCode 유효성 검사용 existsBy 3종.
 */
public interface RegionRepository extends JpaRepository<Region, Long> {

    /**
     * 전체 region을 시도코드 → 시군구코드 오름차순으로 조회.
     *
     * 그루핑은 Service 레이어에서 수행. 정렬을 DB 쿼리에서 처리해 두면
     * Service의 LinkedHashMap 그루핑이 자연스럽게 시도 순서를 유지한다.
     *
     * @return 정렬된 전체 region 목록 (시드 적재 전이면 빈 리스트)
     */
    List<Region> findAllByOrderBySidoCodeAscSigunguCodeAsc();

    /**
     * 특정 시도의 시군구 목록을 시군구코드 오름차순으로 조회.
     *
     * @param sidoCode 시도 코드 (예: "11" = 서울특별시)
     * @return 해당 시도의 region 행들 (시드 적재 전이거나 미존재 시도면 빈 리스트)
     */
    List<Region> findBySidoCodeOrderBySigunguCodeAsc(String sidoCode);

    /**
     * 시도 코드의 REGION 마스터 존재 여부 검증 (UserProfile PUT 검증용).
     *
     * USER_PROFILE은 REGION에 FK를 걸지 않는 정책(§2-2)이라 앱 레벨 검증이
     * 유일한 안전망. PUT 요청 처리 시 사용자가 보낸 sidoCode가 유효한지 확인.
     *
     * @param sidoCode 검증할 시도 코드
     * @return 해당 sidoCode를 가진 region 행이 하나라도 있으면 true
     */
    boolean existsBySidoCode(String sidoCode);

    /**
     * sidoCode + sigunguCode 조합의 일관성 검증.
     *
     * 사용자가 sigunguCode를 보낼 때 그 시군구가 정말 입력한 sidoCode 산하인지
     * 확인해 "서울특별시 + 41480(파주시)" 같은 모순 조합을 차단한다.
     *
     * @param sidoCode 시도 코드
     * @param sigunguCode 시군구 코드
     * @return 두 코드가 같은 행에 모두 존재하면 true
     */
    boolean existsBySidoCodeAndSigunguCode(String sidoCode, String sigunguCode);

    /**
     * 시군구 코드로 region 행 lookup (Step 7 챗봇 라벨 변환용).
     *
     * sigunguCode는 UNIQUE 제약이 있어 0~1개만 가능. 결과 row의 sidoName/sigunguName을
     * 챗봇에 전송한다.
     *
     * @param sigunguCode 시군구 코드 (예: "11680")
     * @return 해당 region 행 (없으면 Optional.empty)
     */
    Optional<Region> findBySigunguCode(String sigunguCode);

    /**
     * 시도 코드로 첫 region 행 lookup (Step 7 챗봇 라벨 변환용).
     *
     * 사용자가 "시도만 선택"한 경우 sidoName만 추출하기 위함. 동일 sidoCode를 가진
     * 여러 행 중 어느 것이든 sidoName은 같으므로 first로 충분.
     *
     * @param sidoCode 시도 코드
     * @return 해당 sidoCode를 가진 region 행 중 하나 (없으면 Optional.empty)
     */
    Optional<Region> findFirstBySidoCode(String sidoCode);
}
// 이 인터페이스의 역할: Region 마스터의 단일 영속성 진입점.
// 시드 적재(Step 8) 후엔 read-only로 동작하므로 향후 캐시(@Cacheable) 도입도 고려 가능.
