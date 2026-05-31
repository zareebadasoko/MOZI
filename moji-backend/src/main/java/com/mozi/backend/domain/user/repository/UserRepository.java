package com.mozi.backend.domain.user.repository;

import com.mozi.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User 엔티티 영속성 접근 인터페이스.
 *
 * Spring Data JPA가 메서드명 규칙으로 쿼리를 자동 생성한다.
 * 이메일 중복 체크(존재 여부)와 로그인 시 조회(엔티티 반환) 두 가지가 핵심 사용처.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회.
     *
     * 로그인 시 비밀번호 검증 전에 사용자 존재 여부와 정보를 한 번에 가져오기 위함.
     *
     * @param email 조회할 이메일
     * @return 일치하는 User (없으면 Optional.empty)
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 가입 여부 확인.
     *
     * 회원가입 시 중복 체크용. findByEmail보다 가벼움(엔티티 로드 X, COUNT 쿼리).
     *
     * @param email 확인할 이메일
     * @return 이미 가입된 이메일이면 true
     */
    boolean existsByEmail(String email);
}
// 이 인터페이스의 역할: User 영속성 추상화.
// Spring Data JPA가 실제 구현체를 런타임에 자동 생성하므로 별도 클래스 작성 불필요.
