package com.mozi.backend.domain.user.controller;

import com.mozi.backend.domain.user.dto.PasswordChangeRequest;
import com.mozi.backend.domain.user.dto.PasswordChangeResponse;
import com.mozi.backend.domain.user.dto.UserProfileResponse;
import com.mozi.backend.domain.user.dto.UserProfileUpdateRequest;
import com.mozi.backend.domain.user.dto.WithdrawResponse;
import com.mozi.backend.domain.user.service.UserAccountService;
import com.mozi.backend.domain.user.service.UserProfileService;
import com.mozi.backend.global.common.ApiResponse;
import com.mozi.backend.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증된 사용자의 본인 라이프사이클 라우트(`/api/users/me`)의 단일 진입점.
 *
 * 4개 엔드포인트 모두 SecurityConfig의 .anyRequest().authenticated()로 보호되며
 * @AuthenticationPrincipal로 SecurityContext의 AuthenticatedUser를 직접 받는다.
 * 비즈니스 로직은 모두 UserProfileService / UserAccountService에 위임하고
 * 컨트롤러는 입력 검증과 응답 래핑만 담당.
 */
@Tag(name = "User", description = "본인 프로필 / 비밀번호 / 탈퇴 (인증 필수)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;
    private final UserAccountService userAccountService;

    /**
     * 본인 프로필 조회.
     *
     * Lazy creation 정책상 row가 없을 수 있으며, 그 경우 isCompleted=false인 빈 응답.
     *
     * @param principal SecurityContext의 인증 사용자
     * @return 프로필 응답 DTO
     */
    @Operation(summary = "내 프로필 조회", description = "프로필 row가 없으면 isCompleted=false 빈 응답 반환.")
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success(userProfileService.getMyProfile(principal.userId()));
    }

    /**
     * 본인 프로필 부분 갱신 (PATCH-like).
     *
     * row 부재 시 lazy creation. absent 필드 무변경 / 명시적 null 클리어.
     *
     * @param principal SecurityContext의 인증 사용자
     * @param request 부분 갱신 요청 (Optional 박싱)
     * @return 갱신본 응답 DTO
     */
    @Operation(summary = "내 프로필 수정", description = "없으면 lazy creation. 요청에 없는 필드는 변경 X, 명시적 null도 무변경(옵션 C 정책).")
    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(@AuthenticationPrincipal AuthenticatedUser principal,
                                                          @Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.success("프로필이 저장되었어요.",
                userProfileService.updateMyProfile(principal.userId(), request));
    }

    /**
     * 본인 비밀번호 변경.
     *
     * 현재 비밀번호 검증 후 새 비밀번호 해싱 + RefreshToken 일괄 삭제 → 다른 기기 강제 재로그인.
     *
     * @param principal SecurityContext의 인증 사용자
     * @param request 현재/새 비밀번호 평문
     * @return changed=true
     */
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 검증 후 새 비밀번호 해싱. 본인의 모든 RefreshToken 일괄 삭제(다른 기기 강제 재로그인).")
    @PutMapping("/password")
    public ApiResponse<PasswordChangeResponse> changePassword(@AuthenticationPrincipal AuthenticatedUser principal,
                                                              @Valid @RequestBody PasswordChangeRequest request) {
        return ApiResponse.success("비밀번호가 변경되었어요. 다른 기기는 다시 로그인이 필요해요.",
                userAccountService.changePassword(principal.userId(), request));
    }

    /**
     * 본인 회원 탈퇴 (Hard Delete + cascade).
     *
     * User 삭제 시 UserProfile/Bookmark/RefreshToken까지 cascade로 일괄 삭제.
     * 같은 이메일로 즉시 재가입 가능.
     *
     * @param principal SecurityContext의 인증 사용자
     * @return withdrawn=true
     */
    @Operation(summary = "회원 탈퇴", description = "Hard Delete + cascade — UserProfile/Bookmark/RefreshToken 일괄 삭제. 같은 이메일 즉시 재가입 가능.")
    @DeleteMapping
    public ApiResponse<WithdrawResponse> withdraw(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success("회원 탈퇴가 완료되었어요.",
                userAccountService.withdraw(principal.userId()));
    }
}
// 이 클래스의 역할: HTTP ↔ Profile/Account 서비스 어댑터.
// 실제 비즈니스 로직은 모두 서비스 레이어에 있으며 컨트롤러는 라우팅·검증·래핑만 담당한다.
