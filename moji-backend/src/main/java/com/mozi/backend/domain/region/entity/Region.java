package com.mozi.backend.domain.region.entity;

import com.mozi.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 행정구역 마스터 엔티티 (시도 17 × 시군구 N 행).
 *
 * 각 행은 시군구 1개를 표현하며 시도 정보(sidoCode/sidoName)는 컬럼으로 비정규화한다.
 * 시도 17개라 중복 부담이 무시할 수 있는 수준이고, 단일 테이블로 두면 cascading
 * select(시도 → 시군구) 쿼리가 단순해진다. 시도 목록은
 * `SELECT DISTINCT sido_code, sido_name` 패턴으로 추출한다.
 *
 * 엣지 케이스:
 * - 세종특별자치시: 하위 시군구가 없어 자기 자신을 단일 행으로 표현 (sigunguCode/Name 모두
 *   "세종" 형태로 채우거나 nullable로 둘 수 있음 — 시드 적재 시 Step 8에서 결정).
 * - 제주: 제주시·서귀포시 2행.
 *
 * USER_PROFILE은 본 테이블에 FK를 걸지 않는다 — "시도만 선택" 케이스(sigunguCode=null)를
 * 단일 FK로 표현하기 까다롭기 때문. 대신 애플리케이션 레벨에서 RegionRepository로
 * 코드 존재 여부를 검증한다 (Step 4 작업).
 *
 * 시드 적재는 Step 8에서, UserProfile.sidoCode/sigunguCode 연결은 Step 3-4에서 진행한다.
 * 본 Step(1)에서는 테이블·엔티티·CRUD 골격만 마련하고 시드는 비워둔다.
 */
@Entity
@Getter
@Table(
        name = "region",
        uniqueConstraints = @UniqueConstraint(name = "uk_region_sigungu_code", columnNames = "sigungu_code")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sido_code", nullable = false, length = 2)
    private String sidoCode;

    @Column(name = "sido_name", nullable = false, length = 20)
    private String sidoName;

    // nullable — 세종시처럼 시도 자체가 단일 행 형태로 시군구를 갖지 않는 경우 대응
    @Column(name = "sigungu_code", length = 5)
    private String sigunguCode;

    @Column(name = "sigungu_name", length = 40)
    private String sigunguName;

    private Region(String sidoCode, String sidoName, String sigunguCode, String sigunguName) {
        this.sidoCode = sidoCode;
        this.sidoName = sidoName;
        this.sigunguCode = sigunguCode;
        this.sigunguName = sigunguName;
    }

    /**
     * 행정구역 마스터 행 생성용 정적 팩토리.
     *
     * Step 8 시드 어댑터가 행정표준코드 데이터를 순회하며 호출 예정.
     *
     * @param sidoCode 법정동코드 앞 2자리 (예: "11" = 서울특별시)
     * @param sidoName 시도 풀네임 (예: "서울특별시") — WELFARE_LOCAL.region_name 표기와 일치
     * @param sigunguCode 법정동코드 앞 5자리 (예: "11680" = 강남구). 시도만 선택 케이스(세종 등)는 null 허용
     * @param sigunguName 시군구 풀네임 (예: "강남구"). sigunguCode가 null이면 null 가능
     * @return id 미할당 상태의 Region (save 후 PK 채워짐)
     */
    public static Region of(String sidoCode, String sidoName, String sigunguCode, String sigunguName) {
        return new Region(sidoCode, sidoName, sigunguCode, sigunguName);
    }
}
// 이 엔티티의 역할: 행정구역 코드/명칭의 단일 진실 원천.
// Category 마스터와 동일한 read-heavy 패턴 — 시드 적재 후엔 사용자 입력으로 추가/수정되지 않는다.
// FK는 의도적으로 생략 — "시도만 선택" 케이스를 단일 FK로 표현하기 어려운 점을 회피.
