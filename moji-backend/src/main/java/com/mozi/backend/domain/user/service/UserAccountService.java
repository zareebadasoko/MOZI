package com.mozi.backend.domain.user.service;

import com.mozi.backend.domain.user.dto.PasswordChangeRequest;
import com.mozi.backend.domain.user.dto.PasswordChangeResponse;
import com.mozi.backend.domain.user.dto.WithdrawResponse;
import com.mozi.backend.domain.user.entity.User;
import com.mozi.backend.domain.user.exception.PasswordMismatchException;
import com.mozi.backend.domain.user.exception.SamePasswordException;
import com.mozi.backend.domain.user.exception.UserNotFoundException;
import com.mozi.backend.domain.user.repository.RefreshTokenRepository;
import com.mozi.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 계정 라이프사이클(비밀번호 변경/회원 탈퇴) 비즈니스 로직.
 *
 * 두 메서드 모두 다중 기기 정책의 후속 효과를 갖는다:
 *  - changePassword: 본인의 모든 RefreshToken 삭제 → 다른 기기 강제 재로그인
 *  - withdraw: User cascade로 UserProfile/Bookmark/RefreshToken 일괄 삭제 → Hard Delete
 *
 * 메서드별 @Transactional로 password 갱신·token 삭제·cascade가 한 트랜잭션에서
 * 처리되어 부분 실패 위험을 차단한다.
 */
@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 비밀번호를 변경하고 본인의 모든 RefreshToken을 일괄 삭제한다.
     *
     * 검증 순서:
     * 1) 현재 비밀번호 BCrypt matches 검증 — 실패 시 PasswordMismatchException
     * 2) 새 비밀번호가 현재와 동일한지 검증 — 동일 시 SamePasswordException
     * 3) 새 비밀번호 BCrypt 해싱 후 User.changePassword 호출 (dirty checking으로 UPDATE)
     * 4) RefreshToken 일괄 삭제 → 다른 기기 강제 재로그인
     *
     * 동일 트랜잭션에서 처리되어 password 갱신만 commit되고 토큰 삭제가 실패하는
     * 보안상 위험한 부분 실패를 방지.
     *
     * @param userId 인증 사용자 PK
     * @param request 현재/새 비밀번호 평문
     * @return 항상 changed=true
     * @throws UserNotFoundException userId가 DB에 없는 모순 케이스 (안전망)
     * @throws PasswordMismatchException 현재 비밀번호 불일치
     * @throws SamePasswordException 새 비밀번호 = 현재 비밀번호
     */
    @Transactional
    public PasswordChangeResponse changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        // 현재 비밀번호 검증 — 평문 ↔ 저장된 해시 비교
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new PasswordMismatchException();
        }
        // 동일 비밀번호 입력 차단 — 변경 효과가 없음을 사용자에게 알림
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new SamePasswordException();
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        // 다중 기기 강제 재로그인 — 본인의 모든 RefreshToken row 삭제
        refreshTokenRepository.deleteAllByUser_Id(userId);
        return new PasswordChangeResponse(true);
    }

    /**
     * 회원 탈퇴를 처리한다 (Hard Delete + cascade).
     *
     * User 엔티티의 cascade 매핑(UserProfile/Bookmark/RefreshToken)이
     * 자동으로 4개 테이블 row를 일괄 삭제한다. 명시적 해당 row deleteAll을
     * 호출하지 않는 이유는 cascade 매핑이 이미 검증되어 있기 때문.
     *
     * 트랜잭션 컨텍스트가 필수 — cascade 동작은 영속성 컨텍스트 안에서 일어난다.
     *
     * @param userId 탈퇴할 사용자 PK
     * @return 항상 withdrawn=true
     * @throws UserNotFoundException userId가 DB에 없는 모순 케이스 (안전망)
     */
    @Transactional
    public WithdrawResponse withdraw(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        // cascade로 UserProfile/Bookmark/RefreshToken 자동 삭제 (Hard Delete)
        userRepository.delete(user);
        return new WithdrawResponse(true);
    }
}
// 이 클래스의 역할: 인증된 사용자의 계정 라이프사이클 후반부 처리.
// changePassword + withdraw 모두 다중 기기 무효화를 동반하는 보안 민감 동작이라
// 한 트랜잭션에서 처리되도록 메서드 단위 @Transactional을 명시적으로 둔다.
