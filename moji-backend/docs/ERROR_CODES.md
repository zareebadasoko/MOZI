# MOZI 백엔드 에러 코드 정의

> 본 문서는 MOZI 백엔드 API가 반환하는 모든 에러 응답의 정의서다.
> 프론트엔드는 응답의 `errorCode` 값을 기준으로 분기 처리하고, 사용자에게는 본 표의 "프론트 노출 정책"에 따라 메시지를 표시한다.

---

## 1. 응답 포맷

### 1-1. 공통 구조 (`ApiResponse<T>`)

성공/에러 모두 동일한 래퍼를 사용한다. `null` 필드는 응답에서 자동 제외된다.

#### 에러 응답 (일반)

```json
{
  "status": "ERROR",
  "message": "사용자에게 보여줄 한글 메시지",
  "errorCode": "USER_NOT_FOUND",
  "timestamp": "2026-05-15T15:00:00"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `status` | String | 항상 `"ERROR"` |
| `message` | String | 사용자 친화 한글 메시지 (그대로 노출 가능) |
| `errorCode` | String | 프론트 분기용 식별자 (본 문서 §2 표 참조) |
| `timestamp` | String (ISO-8601) | 발생 시각 |

#### 에러 응답 (Validation 실패)

```json
{
  "status": "ERROR",
  "message": "입력값을 확인해주세요.",
  "errorCode": "VALIDATION_FAILED",
  "fields": {
    "email": "이메일 형식이 올바르지 않아요.",
    "password": "8자 이상 입력해주세요."
  },
  "timestamp": "2026-05-15T15:00:00"
}
```

`fields` 객체의 키는 요청 DTO 필드명, 값은 해당 필드 검증 실패 메시지다. 프론트는 폼 입력란별로 이 메시지를 표시한다.

### 1-2. HTTP 상태 코드와의 관계

`errorCode`와 HTTP 상태 코드는 1:1 매핑되지 않는다 — 같은 401이라도 `UNAUTHORIZED`(로그인 필요)와 `TOKEN_EXPIRED`(세션 만료)는 프론트 UX가 달라야 하기 때문. 분기는 항상 `errorCode` 기준.

---

## 2. 에러 코드 표

### 2-1. 공통 / 인프라

| errorCode | HTTP | message | 발생 조건 | 프론트 노출 정책 |
|---|---|---|---|---|
| `VALIDATION_FAILED` | 400 | "입력값을 확인해주세요." / "필수 입력값이 빠졌어요." | 요청 본문 `@Valid` 검증 실패, 필수 query param 누락, enum 매핑 실패 | `fields`를 폼 입력란 옆에 표시 + 폼 상단에 `message` |
| `UNAUTHORIZED` | 401 | "로그인이 필요해요." | 보호된 API에 Authorization 헤더 없이 접근 | 로그인 페이지로 리다이렉트 + 토스트 |
| `INVALID_TOKEN` | 401 | "유효하지 않은 인증 정보예요." | JWT 위·변조, 잘못된 서명 | 토큰 클리어 + 로그인 페이지 리다이렉트 |
| `TOKEN_EXPIRED` | 401 | "로그인 세션이 만료되었어요. 다시 로그인해주세요." | access 토큰 만료 | **자동 재시도**: refresh 토큰으로 `/api/auth/refresh` 호출 후 원 요청 재시도. refresh도 실패 시 로그인 페이지 |
| `INTERNAL_ERROR` | 500 | "잠시 후 다시 시도해주세요. 문제가 계속되면 고객센터로 문의해주세요." | 처리되지 않은 모든 예외 (fallback) | 토스트 + 페이지 유지 (상세 정보 노출 X) |

### 2-2. Auth 도메인 (가입 / 로그인 / 토큰)

| errorCode | HTTP | message | 발생 조건 | 프론트 노출 정책 |
|---|---|---|---|---|
| `EMAIL_ALREADY_EXISTS` | 409 | "이미 가입된 이메일이에요." | `POST /api/auth/signup` 시 동일 이메일 존재 | 이메일 입력란 옆에 표시 + 로그인 페이지 안내 |
| `INVALID_CREDENTIALS` | 401 | "이메일 또는 비밀번호가 올바르지 않아요." | `POST /api/auth/login` 시 비밀번호 불일치 또는 미가입 이메일 | 폼 상단에 표시 (어느 필드가 틀렸는지 알려주지 않음 — 보안) |
| `INVALID_REFRESH_TOKEN` | 401 | "재로그인이 필요해요." | refresh 토큰 위조 / 만료 / 이미 회전됨 | 토큰 전체 클리어 + 로그인 페이지 |

### 2-3. User 도메인 (프로필 / 비밀번호 / 탈퇴)

| errorCode | HTTP | message | 발생 조건 | 프론트 노출 정책 |
|---|---|---|---|---|
| `USER_NOT_FOUND` | 404 | "사용자를 찾을 수 없어요." | 토큰의 userId가 DB에 없을 때 (정상 흐름에선 거의 안 나옴 — 탈퇴 후 토큰 재사용 등) | 토큰 클리어 + 로그인 페이지 |
| `PASSWORD_MISMATCH` | 400 | "현재 비밀번호가 일치하지 않아요." | `PUT /api/users/me/password` 시 currentPassword 불일치 | 현재 비밀번호 입력란 옆에 표시 |
| `SAME_PASSWORD` | 400 | "새 비밀번호가 현재 비밀번호와 같아요." | 새 비밀번호가 현재와 동일 | 새 비밀번호 입력란 옆에 표시 |
| `INVALID_REGION_CODE` | 400 | "행정구역 코드가 올바르지 않아요." (구체 메시지는 케이스별 다름) | `PUT /api/users/me/profile` 시 sidoCode/sigunguCode 검증 실패. 케이스 3종: (1) sigunguCode 단독 입력, (2) sidoCode가 REGION에 없음, (3) sidoCode-sigunguCode 조합 불일치 | 지역 선택 UI 옆에 표시 + `GET /api/regions`로 cascading select 재구성 안내 |

### 2-4. Welfare 도메인 (복지 검색·상세)

| errorCode | HTTP | message | 발생 조건 | 프론트 노출 정책 |
|---|---|---|---|---|
| `WELFARE_NOT_FOUND` | 404 | "복지 정보를 찾을 수 없어요." | `GET /api/welfares/{id}` 또는 북마크 호출 시 해당 ID 없음 | 안내 페이지로 이동 또는 토스트 |

### 2-5. Bookmark 도메인

| errorCode | HTTP | message | 발생 조건 | 프론트 노출 정책 |
|---|---|---|---|---|
| `BOOKMARK_NOT_FOUND` | 404 | "북마크를 찾을 수 없어요." | `DELETE /api/bookmarks/{welfareId}` 시 welfare는 있으나 북마크 row가 없음 | UI 상태만 갱신(이미 해제된 것으로 간주) — 사용자에게 굳이 노출 안 해도 OK |

> 참고: 북마크 추가(POST)는 **idempotent** — 이미 존재해도 200 + 같은 bookmarkId 반환. 별도 에러 없음.
> DELETE는 2단계 404: 먼저 welfare 존재 확인(`WELFARE_NOT_FOUND` 가능) → 북마크 존재 확인(`BOOKMARK_NOT_FOUND` 가능).

### 2-6. Chat 도메인 (챗봇 브릿지)

| errorCode | HTTP | message | 발생 조건 | 프론트 노출 정책 |
|---|---|---|---|---|
| `CHATBOT_UNAVAILABLE` | 503 | "챗봇 서버가 일시적으로 응답하지 않아요." | 챗봇 서버 5xx 응답 또는 연결 실패 | 챗봇 카드 안에 안내 + 재시도 버튼 |
| `CHATBOT_TIMEOUT` | 504 | "지금은 추천이 어려워요. 잠시 후 다시 시도해주세요." | 8초 타임아웃 초과 | 챗봇 카드 안에 안내 + 재시도 버튼 |
| `CHATBOT_INVALID_RESPONSE` | 502 | "챗봇 응답에 문제가 있어요. 잠시 후 다시 시도해주세요." | 챗봇 서버 응답이 명세 스키마 위반 | 동일 |

---

## 3. VALIDATION_FAILED 상세 — 필드별 메시지

각 API의 요청 DTO에는 Bean Validation 어노테이션이 붙어 있다. 검증 실패 시 `fields` 객체에 아래 메시지가 그대로 들어간다.

### 3-1. 회원가입 (`POST /api/auth/signup`)

| 필드 | 검증 | 실패 메시지 |
|---|---|---|
| `email` | `@Email` + `@NotBlank` | "이메일 형식이 올바르지 않아요." 등 |
| `password` | `@Size(min=8, max=64)` | "8자 이상 64자 이하로 입력해주세요." 등 |
| `nickname` | `@NotBlank` + 길이 제한 | "닉네임을 입력해주세요." 등 |

### 3-2. 비밀번호 변경 (`PUT /api/users/me/password`)

| 필드 | 검증 | 실패 메시지 |
|---|---|---|
| `currentPassword` | `@NotBlank` | "현재 비밀번호를 입력해주세요." |
| `newPassword` | `@Size(min=8)` | "새 비밀번호는 8자 이상이어야 해요." |

### 3-3. 채팅 (`POST /api/chat`)

| 필드 | 검증 | 실패 메시지 |
|---|---|---|
| `message` | `@NotBlank` | "메시지를 입력해주세요." |
| `message` | `@Size(max=1000)` | "메시지가 너무 길어요." |

> 정확한 검증 규칙은 각 DTO 소스 또는 Swagger UI의 schema에서 확인.

---

## 4. 프론트엔드 노출 가이드 요약

### 4-1. "그대로 노출 가능"한 메시지
다음 코드의 `message`는 한글 친화어로 작성되어 있어 토스트·다이얼로그에 **그대로 표시**해도 무방하다:
- 모든 도메인 에러 (`EMAIL_ALREADY_EXISTS`, `INVALID_CREDENTIALS`, `WELFARE_NOT_FOUND`, `PASSWORD_MISMATCH`, `SAME_PASSWORD`, `BOOKMARK_NOT_FOUND` 등)
- 챗봇 3종 (`CHATBOT_*`)
- 공통 `UNAUTHORIZED`, `INTERNAL_ERROR`

### 4-2. "분기 처리"가 필요한 코드

| errorCode | 권장 동작 |
|---|---|
| `TOKEN_EXPIRED` | 메시지 노출 X. 자동으로 `/api/auth/refresh` 호출 후 재시도. |
| `INVALID_TOKEN` / `INVALID_REFRESH_TOKEN` | 메시지 잠깐 표시 후 토큰 클리어 + 로그인 페이지 리다이렉트. |
| `VALIDATION_FAILED` | `message`는 폼 상단, `fields`는 각 입력란 옆에 표시. |
| `INTERNAL_ERROR` | 토스트 + 사용자 동작 차단 X (페이지 유지). |

### 4-3. 노년층 UX 고려 사항
- 에러 메시지는 모달보다 인라인 표시 우선 (모달이 많으면 인지 부담↑)
- 토스트는 4초 이상 표시 (노년층 읽기 속도)
- 색상 외에 아이콘·텍스트로도 에러 식별 가능하게 (색맹 대비)

---

## 5. 신규 에러 추가 시 절차

본 표는 백엔드 추가/변경에 맞춰 함께 갱신되어야 한다.

1. 새 BusinessException 서브클래스 추가 (도메인 폴더의 `exception/`)
2. `CODE` / `MESSAGE` / `HttpStatus` 3종 상수 명시
3. 본 문서 §2의 도메인 표에 한 줄 추가
4. 프론트 노출 정책 결정 (인라인 / 토스트 / 모달 / 자동 분기)
5. PR 머지

---

## 6. 변경 이력

| 일자 | 변경 | 작성자 |
|---|---|---|
| 2026-05-15 | Phase 6 초안 작성 — Phase 1~5까지 전체 16개 코드 정리 | MOZI 백엔드 |
