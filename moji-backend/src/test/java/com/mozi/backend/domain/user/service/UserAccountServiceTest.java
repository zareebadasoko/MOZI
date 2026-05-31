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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserAccountService 단위 테스트.
 *
 * 검증 대상:
 *  - changePassword: 정상 흐름(BCrypt 검증 + 토큰 일괄 삭제), 현재 비밀번호 불일치, 동일 비밀번호
 *  - withdraw: cascade 의존(userRepository.delete만 호출), userId 부재 안전망
 */
@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAccountService userAccountService;

    /**
     * 정상 비밀번호 변경 — User.password 갱신 + RefreshToken 일괄 삭제.
     */
    @Test
    void changePassword_정상_비밀번호갱신_토큰일괄삭제() {
        User user = User.of("ok@mozi.test", "$2a$10$old");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldpass1234", "$2a$10$old")).thenReturn(true);
        when(passwordEncoder.matches("newpass1234", "$2a$10$old")).thenReturn(false);
        when(passwordEncoder.encode("newpass1234")).thenReturn("$2a$10$new");

        PasswordChangeResponse res = userAccountService.changePassword(1L,
                new PasswordChangeRequest("oldpass1234", "newpass1234"));

        assertThat(res.changed()).isTrue();
        assertThat(user.getPassword()).isEqualTo("$2a$10$new");
        verify(refreshTokenRepository, times(1)).deleteAllByUser_Id(1L);
    }

    /**
     * 현재 비밀번호 불일치 → PasswordMismatchException + 토큰 삭제 호출 X.
     */
    @Test
    void changePassword_현재비밀번호불일치_PasswordMismatchException() {
        User user = User.of("ok@mozi.test", "$2a$10$old");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpw1234", "$2a$10$old")).thenReturn(false);

        assertThatThrownBy(() -> userAccountService.changePassword(1L,
                new PasswordChangeRequest("wrongpw1234", "newpass1234")))
                .isInstanceOf(PasswordMismatchException.class);

        verify(refreshTokenRepository, never()).deleteAllByUser_Id(1L);
        assertThat(user.getPassword()).isEqualTo("$2a$10$old");
    }

    /**
     * 새 비밀번호 = 현재 비밀번호 → SamePasswordException.
     */
    @Test
    void changePassword_새비밀번호동일_SamePasswordException() {
        User user = User.of("ok@mozi.test", "$2a$10$old");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("samepass1234", "$2a$10$old")).thenReturn(true);

        assertThatThrownBy(() -> userAccountService.changePassword(1L,
                new PasswordChangeRequest("samepass1234", "samepass1234")))
                .isInstanceOf(SamePasswordException.class);

        verify(refreshTokenRepository, never()).deleteAllByUser_Id(1L);
    }

    /**
     * userId가 DB에 없는 모순 케이스 → UserNotFoundException.
     */
    @Test
    void changePassword_user없음_UserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAccountService.changePassword(99L,
                new PasswordChangeRequest("oldpass1234", "newpass1234")))
                .isInstanceOf(UserNotFoundException.class);
    }

    /**
     * 정상 회원 탈퇴 — userRepository.delete 한 번만 호출 (cascade 의존).
     */
    @Test
    void withdraw_정상_userRepositoryDelete호출() {
        User user = User.of("ok@mozi.test", "$2a$10$h");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        WithdrawResponse res = userAccountService.withdraw(1L);

        assertThat(res.withdrawn()).isTrue();
        verify(userRepository, times(1)).delete(user);
        // 명시적 RefreshToken 삭제는 호출 X — cascade에 위임
        verify(refreshTokenRepository, never()).deleteAllByUser_Id(1L);
    }

    /**
     * 탈퇴 시 userId가 DB에 없는 모순 케이스 → UserNotFoundException.
     */
    @Test
    void withdraw_user없음_UserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAccountService.withdraw(99L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
// 이 테스트의 역할: 비밀번호 변경/회원 탈퇴의 모든 분기를 외부 의존성 없이 결정적으로 검증.
// withdraw가 cascade에 의존한다는 결정은 통합 테스트로 별도 검증한다 (단위 테스트는 Mock).
