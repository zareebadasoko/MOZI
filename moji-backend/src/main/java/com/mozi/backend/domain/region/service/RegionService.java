package com.mozi.backend.domain.region.service;

import com.mozi.backend.domain.region.dto.RegionDto;
import com.mozi.backend.domain.region.dto.SidoGroupDto;
import com.mozi.backend.domain.region.entity.Region;
import com.mozi.backend.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Region 마스터 조회 서비스.
 *
 * 시드 적재(Step 8) 후엔 read-only 패턴이라 로직이 단순하다. 본 Step(1) 시점에는
 * 빈 테이블에서도 정상적으로 빈 응답을 내도록 설계.
 *
 * 응답 형태 2종:
 *  - {@link #getAllGroupedBySido()}: cascading select 친화적 시도 그루핑
 *  - {@link #getSigungusBySido(String)}: 특정 시도 시군구 평면 목록
 *
 * 그루핑은 LinkedHashMap으로 처리해 Repository 정렬 순서(시도코드 ASC)를 유지한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;

    /**
     * 전체 region을 시도별로 묶어 반환.
     *
     * Repository가 sidoCode → sigunguCode 오름차순으로 정렬해 주므로,
     * LinkedHashMap 그루핑이 자연스럽게 시도 순서를 보존한다.
     * 시드 적재 전이면 빈 리스트 반환.
     *
     * @return 시도 그룹 목록 (각 그룹은 산하 시군구 배열 포함)
     */
    public List<SidoGroupDto> getAllGroupedBySido() {
        List<Region> all = regionRepository.findAllByOrderBySidoCodeAscSigunguCodeAsc();
        // 시도 순서 보존을 위해 LinkedHashMap 사용 — HashMap이면 사용자에게 시도 순서가 들쭉날쭉 보일 수 있음
        Map<String, SidoBuffer> buffer = new LinkedHashMap<>();
        for (Region r : all) {
            buffer.computeIfAbsent(r.getSidoCode(), k -> new SidoBuffer(r.getSidoName()))
                    .sigungus.add(new SidoGroupDto.Sigungu(r.getSigunguCode(), r.getSigunguName()));
        }
        List<SidoGroupDto> result = new ArrayList<>(buffer.size());
        buffer.forEach((sidoCode, buf) -> result.add(SidoGroupDto.of(sidoCode, buf.sidoName, buf.sigungus)));
        return result;
    }

    /**
     * 특정 시도의 시군구 목록을 평면 리스트로 반환.
     *
     * sidoCode 미존재나 빈 테이블 양쪽 모두 빈 리스트로 응답 — 본 Step 범위에선
     * 별도 검증·예외를 두지 않는다 (시드가 비어 있는 단계라 빈 응답이 자연스러움).
     *
     * @param sidoCode 시도 코드 (예: "11"). null/blank여도 Repository가 빈 리스트 반환
     * @return 해당 시도의 시군구 평면 목록
     */
    public List<RegionDto> getSigungusBySido(String sidoCode) {
        return regionRepository.findBySidoCodeOrderBySigunguCodeAsc(sidoCode).stream()
                .map(RegionDto::from)
                .toList();
    }

    /**
     * 그루핑 중간 상태 보관용 내부 헬퍼.
     *
     * sidoName은 한 그룹 안에서 동일하므로 첫 행에서 1회만 캡처해 보관하고,
     * sigungus는 순차 누적한다. record 대신 mutable class를 사용한 이유는
     * sigungus 리스트에 add()를 누적해야 하기 때문.
     */
    private static class SidoBuffer {
        final String sidoName;
        final List<SidoGroupDto.Sigungu> sigungus = new ArrayList<>();

        SidoBuffer(String sidoName) {
            this.sidoName = sidoName;
        }
    }
}
// 이 클래스의 역할: Region 마스터 조회의 단일 진입점.
// 시도별 그루핑(LinkedHashMap)으로 응답 순서를 보존해 프론트의 select UI가 안정적으로 동작.
// 시드 적재 전이라 빈 응답이 자연스러우며, 별도 검증·예외를 두지 않는다.
