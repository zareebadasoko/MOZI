package com.mozi.backend.domain.region.service;

import com.mozi.backend.domain.region.dto.RegionDto;
import com.mozi.backend.domain.region.dto.SidoGroupDto;
import com.mozi.backend.domain.region.entity.Region;
import com.mozi.backend.domain.region.repository.RegionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * RegionService 단위 테스트.
 *
 * 검증 포인트:
 *  - 시도별 그루핑이 LinkedHashMap 기반으로 시도 순서를 보존하는지
 *  - 빈 Repository 응답에서도 NPE 없이 빈 리스트로 응답하는지
 *  - 특정 시도 조회가 평면 RegionDto 리스트로 변환되는지
 *
 * Repository 결과는 Step 8 시드 적재 후 채워지므로 본 단위 테스트는 mock으로 대체.
 */
@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private RegionService regionService;

    @Test
    void getAllGroupedBySido_여러시도_시도별로그루핑정렬() {
        // Repository는 sidoCode → sigunguCode ASC 순으로 정렬된 결과를 반환한다고 가정
        Region seoul1 = Region.of("11", "서울특별시", "11680", "강남구");
        Region seoul2 = Region.of("11", "서울특별시", "11740", "강동구");
        Region gyeonggi1 = Region.of("41", "경기도", "41280", "고양시");
        Region gyeonggi2 = Region.of("41", "경기도", "41480", "파주시");
        Region busan = Region.of("26", "부산광역시", "26350", "해운대구");
        when(regionRepository.findAllByOrderBySidoCodeAscSigunguCodeAsc())
                .thenReturn(List.of(seoul1, seoul2, busan, gyeonggi1, gyeonggi2));

        List<SidoGroupDto> result = regionService.getAllGroupedBySido();

        // 3개 시도 그룹으로 묶이며, Repository 정렬 순서(서울 → 부산 → 경기)가 보존됨
        assertThat(result).hasSize(3);
        assertThat(result.get(0).sidoCode()).isEqualTo("11");
        assertThat(result.get(0).sidoName()).isEqualTo("서울특별시");
        assertThat(result.get(0).sigungus()).hasSize(2);
        assertThat(result.get(0).sigungus().get(0).code()).isEqualTo("11680");
        assertThat(result.get(0).sigungus().get(0).name()).isEqualTo("강남구");

        assertThat(result.get(1).sidoCode()).isEqualTo("26");
        assertThat(result.get(1).sigungus()).hasSize(1);

        assertThat(result.get(2).sidoCode()).isEqualTo("41");
        assertThat(result.get(2).sigungus()).hasSize(2);
    }

    @Test
    void getAllGroupedBySido_빈리스트_빈응답() {
        when(regionRepository.findAllByOrderBySidoCodeAscSigunguCodeAsc()).thenReturn(List.of());

        List<SidoGroupDto> result = regionService.getAllGroupedBySido();

        assertThat(result).isEmpty();
    }

    @Test
    void getSigungusBySido_지정시도_평면리스트반환() {
        Region g1 = Region.of("41", "경기도", "41280", "고양시");
        Region g2 = Region.of("41", "경기도", "41480", "파주시");
        when(regionRepository.findBySidoCodeOrderBySigunguCodeAsc("41")).thenReturn(List.of(g1, g2));

        List<RegionDto> result = regionService.getSigungusBySido("41");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).sidoCode()).isEqualTo("41");
        assertThat(result.get(0).sigunguCode()).isEqualTo("41280");
        assertThat(result.get(0).sigunguName()).isEqualTo("고양시");
        assertThat(result.get(1).sigunguCode()).isEqualTo("41480");
    }

    @Test
    void getSigungusBySido_미존재시도_빈리스트() {
        when(regionRepository.findBySidoCodeOrderBySigunguCodeAsc("99")).thenReturn(List.of());

        List<RegionDto> result = regionService.getSigungusBySido("99");

        assertThat(result).isEmpty();
    }
}
// 이 테스트의 역할: Region 마스터 조회의 그루핑·평면 양쪽 응답이 정확히 만들어지는지 확정.
// 시드 데이터가 비어 있는 Step 1 시점이라 mock으로 대체. 시드 적재 검증은 Step 8 또는 통합 테스트로.
