package com.mozi.backend.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 로그인 요청 DTO.
 *
 * SignupRequest와 동일한 검증 규칙을 적용해 입력값 검증 단계에서
 * "잘못된 폼"과 "잘못된 자격증명"을 명확히 구분한다.
 *
 * @param email 로그인 이메일
 * @param password 평문 비밀번호 (서비스 레이어가 BCrypt matches 검증)
 */
public record LoginRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않아요.")
        @Size(max = 255, message = "이메일이 너무 길어요.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하로 입력해주세요.")
        String password
) {
}
// 이 record의 역할: 로그인 요청 본문 검증.
// 검증 통과 후의 자격증명 불일치는 INVALID_CREDENTIALS로 별도 응답된다.
