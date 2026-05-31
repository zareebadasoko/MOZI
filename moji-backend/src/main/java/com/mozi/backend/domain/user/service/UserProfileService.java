package com.mozi.backend.domain.user.service;

import com.mozi.backend.domain.region.exception.InvalidRegionException;
import com.mozi.backend.domain.region.repository.RegionRepository;
import com.mozi.backend.domain.user.dto.UserProfileResponse;
import com.mozi.backend.domain.user.dto.UserProfileUpdateRequest;
import com.mozi.backend.domain.user.entity.User;
import com.mozi.backend.domain.user.entity.UserProfile;
import com.mozi.backend.domain.user.exception.UserNotFoundException;
import com.mozi.backend.domain.user.repository.UserProfileRepository;
import com.mozi.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserProfile 조회/갱신 비즈니스 로직.
 *
 * 핵심 정책:
 * (1) Lazy creation — 회원가입 시점에 프로필 row 미생성. 첫 PUT에서 emptyFor로 생성.
 * (2) GET은 row 부재 시 isCompleted=false인 빈 응답을 반환 (예외 X).
 * (3) PUT은 absent/explicit-null/value 3-state semantics를 엔티티 applyUpdate가 처리.
 * (4) Step 4 추가: PUT 시 sidoCode/sigunguCode가 REGION 마스터에 존재하는지 검증.
 *     FK 미설정 정책(§2-2)으로 인해 앱 레벨 검증이 유일한 안전망.
 *
 * 클래스 레벨에 @Transactional(readOnly=true)를 두고 갱신 메서드만 @Transactional로 덮어써
 * 조회 트랜잭션을 가볍게 유지한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RegionRepository regionRepository;

    /**
     * 인증된 사용자의 프로필을 조회한다.
     *
     * Lazy creation 정책상 row가 없을 수 있으며, 그 경우 isCompleted=false인
     * 빈 응답을 반환한다. 클라이언트는 이 플래그로 강제 입력 화면 진입을 결정.
     *
     * @param userId SecurityContext에서 추출한 인증 사용자 PK
     * @return 프로필 응답 DTO (row 부재 시 isCompleted=false인 empty)
     */
    public UserProfileResponse getMyProfile(Long userId) {
        return userProfileRepository.findByUser_Id(userId)
                .map(UserProfileResponse::from)
                .orElseGet(UserProfileResponse::empty);
    }

    /**
     * 인증된 사용자의 프로필을 부분 갱신한다 (PATCH-like, lazy creation 포함).
     *
     * 흐름:
     * 1) REGION 코드 검증 — sidoCode/sigunguCode가 있으면 REGION 마스터에 존재하는지 확인
     * 2) row 부재 시 emptyFor로 신규 생성 후 save (lazy creation 완성)
     * 3) applyUpdate로 absent/null/value 3-state 처리 — dirty checking이 UPDATE 발행
     * 4) 갱신본을 응답 DTO로 변환해 반환
     *
     * @param userId 인증 사용자 PK
     * @param request 부분 갱신 요청 (Optional 박싱 필드)
     * @return 갱신본 응답 DTO (isCompleted=true)
     * @throws UserNotFoundException SecurityContext에 userId가 있지만 DB에 user가 없는 모순 케이스
     * @throws InvalidRegionException sidoCode 또는 sigunguCode가 REGION 마스터에 없거나
     *                                 시도-시군구 조합이 일치하지 않거나 sigunguCode만 단독 입력된 경우
     */
    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UserProfileUpdateRequest request) {
        validateRegionCodes(request);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        // Lazy creation: row 없으면 emptyFor로 생성 후 save
        UserProfile profile = userProfileRepository.findByUser_Id(userId)
                .orElseGet(() -> userProfileRepository.save(UserProfile.emptyFor(user)));
        profile.applyUpdate(request);
        return UserProfileResponse.from(profile);
    }

    /**
     * 요청에 포함된 sidoCode/sigunguCode가 REGION 마스터와 정합한지 검증.
     *
     * 규칙:
     *  - sidoCode 없이 sigunguCode만 입력은 거부 (시도 컨텍스트 누락)
     *  - sidoCode가 있으면 REGION에 존재해야 함
     *  - sigunguCode가 있으면 sidoCode와의 조합이 REGION에 존재해야 함
     *  - 양쪽 모두 null이면 검증 통과 (부분 갱신상 행정구역 무변경)
     *
     * 본 Step 시점에 REGION 시드가 비어 있으면 사실상 모든 행정구역 입력이
     * 거부되지만, 그건 의도된 동작 — Step 8 시드 적재 전까진 행정구역 PUT을
     * 자제하도록 가이드한다.
     *
     * @param request 부분 갱신 요청
     * @throws InvalidRegionException 규칙 위반 시
     */
    private void validateRegionCodes(UserProfileUpdateRequest request) {
        String sidoCode = request.sidoCode();
        String sigunguCode = request.sigunguCode();
        // sigunguCode 단독 입력 차단
        if (sidoCode == null && sigunguCode != null) {
            throw new InvalidRegionException("시군구만 입력할 수 없어요. 시도부터 선택해주세요.");
        }
        if (sidoCode != null && !regionRepository.existsBySidoCode(sidoCode)) {
            throw new InvalidRegionException("존재하지 않는 시도 코드예요.");
        }
        if (sigunguCode != null && !regionRepository.existsBySidoCodeAndSigunguCode(sidoCode, sigunguCode)) {
            throw new InvalidRegionException("입력한 시도에 속하지 않는 시군구예요.");
        }
    }
}
// 이 클래스의 역할: UserProfile lifecycle(GET/PUT)의 단일 진입점.
// Lazy creation + 3-state partial update + REGION 검증이 핵심.
// Step 3 재설계 적용 — 엔티티/DTO가 신규 스키마로 바뀐 후에도 본 서비스 로직은 동일 패턴 유지.
