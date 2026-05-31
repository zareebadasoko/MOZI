package com.mozi.backend.domain.user.repository;

import com.mozi.backend.domain.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * UserProfile 엔티티 영속성 접근 인터페이스.
 *
 * Lazy creation 정책상 row가 없을 수 있으므로 Optional 반환.
 * GET /api/users/me/profile 처리 시 row가 없으면 빈 응답 + isCompleted=false로 반환.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * userId로 프로필 조회.
     *
     * UserProfile.user 연관관계의 user.id를 기준으로 조회. 메서드명의
     * "User_Id"는 Spring Data JPA의 nested property 표기로 user 객체의
     * id 필드를 의미한다.
     *
     * @param userId 조회 대상 사용자의 PK
     * @return 일치하는 UserProfile (Lazy creation 전이면 Optional.empty)
     */
    Optional<UserProfile> findByUser_Id(Long userId);
}
// 이 인터페이스의 역할: UserProfile 영속성 추상화.
// Lazy creation으로 인해 row 부재가 정상 흐름이라 호출자는 항상 Optional 처리 필요.
