package com.mozi.backend.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO.
 *
 * 이메일과 비밀번호 평문을 받는다. 비밀번호 길이 max 72는 BCrypt 알고리즘의
 * 입력 한계 — 그 이상은 서버 측에서 잘려 동등한 해시가 생성되므로
 * 클라이언트가 의도와 다른 비밀번호로 가입한다고 오해할 수 있어 미리 차단한다.
 *
 * @param email 가입 이메일 (RFC 5322 형식 검증)
 * @param password 평문 비밀번호 (BCrypt 해싱 후 저장)
 */
public record SignupRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않아요.")
        @Size(max = 255, message = "이메일이 너무 길어요.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하로 입력해주세요.")
        String password
) {
}
// 이 record의 역할: 회원가입 요청 본문 검증 + 컨트롤러 ↔ 서비스 전달 매개체.
// max 72는 BCrypt 입력 길이 한계라 실용적 제약이 자연스럽게 적용된다.
