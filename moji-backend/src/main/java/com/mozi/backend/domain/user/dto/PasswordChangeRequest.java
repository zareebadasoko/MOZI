package com.mozi.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 요청 DTO.
 *
 * 현재/새 비밀번호 모두 평문으로 받으며, 서비스 레이어가 BCrypt matches로 현재
 * 비밀번호를 검증하고 새 비밀번호는 인코딩 후 저장한다. newPassword max 72는
 * BCrypt 입력 한계이며 그 이상은 자동 절단되어 사용자가 의도와 다른 비밀번호로
 * 변경하는 일을 막기 위해 미리 차단.
 *
 * @param currentPassword 현재 비밀번호 (평문)
 * @param newPassword 새 비밀번호 (평문, BCrypt 해싱 후 저장)
 */
public record PasswordChangeRequest(
        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Size(min = 8, max = 72, message = "새 비밀번호는 8자 이상 72자 이하로 입력해주세요.")
        String newPassword
) {
}
// 이 record의 역할: 비밀번호 변경 요청 본문 검증.
// 현재 비밀번호 일치 검증과 새 비밀번호 해싱은 서비스 레이어에서 처리.
