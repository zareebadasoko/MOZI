package com.mozi.backend.domain.user.repository;

import com.mozi.backend.domain.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * RefreshToken 엔티티 영속성 접근 인터페이스.
 *
 * Refresh Token Rotation 정책에 필요한 두 가지 동작을 노출한다:
 * (1) tokenHash로 단일 토큰 조회 — refresh 검증 시 사용
 * (2) userId 기준 일괄 삭제 — 비밀번호 변경/로그아웃/탈퇴 시 다중 기기 무효화
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 해시된 토큰 값으로 RefreshToken 조회.
     *
     * /api/auth/refresh 호출 시 들어온 raw 토큰을 SHA-256으로 해싱한 결과로 lookup.
     * 일치하는 row가 없으면 INVALID_REFRESH_TOKEN으로 처리.
     *
     * @param tokenHash SHA-256 해시 결과 (hex 64자)
     * @return 일치하는 RefreshToken (없으면 Optional.empty)
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 특정 사용자가 발급받은 모든 RefreshToken을 일괄 삭제.
     *
     * 비밀번호 변경 시 다중 기기 강제 재로그인, 명시적 로그아웃, 회원 탈퇴 등에서 사용.
     * @Modifying은 SELECT 외의 변경 쿼리임을 Spring Data JPA에 알리는 표시.
     *
     * @param userId 토큰을 소유한 사용자의 PK
     */
    @Modifying
    @Transactional
    void deleteAllByUser_Id(Long userId);
}
// 이 인터페이스의 역할: Refresh Token의 단일 조회 + 일괄 삭제.
// Rotation 알고리즘은 서비스 레이어가 조립: 검증 → 이전 row 삭제(deleteById) → 새 row 저장(save).
// 만료된 row는 MVP에선 정리하지 않음 (누적 허용, v2에서 스케줄러 도입 검토).
