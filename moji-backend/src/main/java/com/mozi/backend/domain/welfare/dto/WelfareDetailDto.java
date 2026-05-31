package com.mozi.backend.domain.welfare.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mozi.backend.domain.category.dto.CategoryDto;
import com.mozi.backend.domain.welfare.entity.WelfareCentral;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.entity.WelfareLocal;
import com.mozi.backend.domain.welfare.entity.WelfarePrivate;
import com.mozi.backend.domain.welfare.entity.WelfareSeoul;
import com.mozi.backend.domain.welfare.entity.WelfareType;

import java.util.List;

/**
 * 복지 상세 조회 응답 DTO.
 *
 * 부모 공통 필드(WelfareSummaryDto와 같은 7개 + targetAudience + applicationMethod) +
 * 자식 4종 detail 중 정확히 1개만 채워지는 구조. 응답 직렬화 시 null 자식 detail은
 * `@JsonInclude(NON_NULL)` 덕에 자동으로 누락되어 클라이언트는 한 번에 의미 있는
 * detail 1개만 받게 된다.
 *
 * sealed interface 대신 평면 record를 채택한 이유: Jackson이 sealed type 직렬화에
 * 추가 설정이 필요한 반면, nullable 필드는 NON_NULL 설정만으로 같은 효과를 낸다.
 *
 * @param id 자연키
 * @param title 복지 제목
 * @param summary 한 줄 요약
 * @param welfareType 출처
 * @param organizationName 담당 기관명
 * @param targetAudience 지원 대상 상세 (LONGTEXT, 부모 컬럼)
 * @param applicationMethod 신청 방법 상세 (LONGTEXT, 부모 컬럼)
 * @param categories 매핑된 카테고리 목록
 * @param isBookmarked 로그인 사용자의 북마크 여부
 * @param centralDetail welfareType=CENTRAL일 때만 채워짐
 * @param localDetail welfareType=LOCAL일 때만 채워짐
 * @param privateDetail welfareType=PRIVATE일 때만 채워짐
 * @param seoulDetail welfareType=SEOUL일 때만 채워짐
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WelfareDetailDto(
        String id,
        String title,
        String summary,
        WelfareType welfareType,
        String organizationName,
        String targetAudience,
        String applicationMethod,
        List<CategoryDto> categories,
        boolean isBookmarked,
        CentralDetailDto centralDetail,
        LocalDetailDto localDetail,
        PrivateDetailDto privateDetail,
        SeoulDetailDto seoulDetail
) {

    /**
     * 부모 + 자식 + 파생 필드를 합쳐 상세 응답 DTO 생성.
     *
     * Java 21 instanceof 패턴 매칭으로 자식 타입에 따라 4 detail 중 1개만 채운다.
     * 알 수 없는 자식 타입은 IllegalStateException — JOINED 매핑 정의상 도달 불가능.
     *
     * @param welfare 영속화된 부모 (자식 인스턴스로 반환됨)
     * @param categories 호출자가 채운 카테고리 목록
     * @param isBookmarked 호출자가 채운 북마크 여부
     * @return 자식 detail 1개만 채워진 응답 DTO
     */
    public static WelfareDetailDto of(WelfareCommon welfare, List<CategoryDto> categories, boolean isBookmarked) {
        // instanceof 패턴 매칭으로 자식 타입 분기 — JOINED 전략에서 findById는 자식 인스턴스 반환
        CentralDetailDto central = null;
        LocalDetailDto local = null;
        PrivateDetailDto priv = null;
        SeoulDetailDto seoul = null;
        if (welfare instanceof WelfareCentral c) {
            central = CentralDetailDto.from(c);
        } else if (welfare instanceof WelfareLocal l) {
            local = LocalDetailDto.from(l);
        } else if (welfare instanceof WelfarePrivate p) {
            priv = PrivateDetailDto.from(p);
        } else if (welfare instanceof WelfareSeoul s) {
            seoul = SeoulDetailDto.from(s);
        } else {
            throw new IllegalStateException("Unknown WelfareCommon subtype: " + welfare.getClass().getName());
        }
        return new WelfareDetailDto(
                welfare.getId(),
                welfare.getTitle(),
                welfare.getSummary(),
                welfare.getWelfareType(),
                welfare.getOrganizationName(),
                welfare.getTargetAudience(),
                welfare.getApplicationMethod(),
                categories,
                isBookmarked,
                central, local, priv, seoul
        );
    }
}
// 이 record의 역할: 4 자식의 detail을 단일 응답 형태로 통일하면서 정확히 1개만 노출.
// @JsonInclude(NON_NULL) + nullable 4 필드 조합이 sealed type 도입 없이 동일 효과를 낸다.
