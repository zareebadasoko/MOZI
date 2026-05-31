# MOZI 백엔드 API 명세 (확정)

> 본 문서는 MOZI 백엔드가 제공하는 모든 REST API의 확정 명세서다.
> **자동 생성 스펙**(Swagger UI / OpenAPI JSON)을 정확한 참조로 사용하고, 본 문서는 사람·LLM이 한눈에 훑기 위한 요약이다.
>
> - Swagger UI: http://localhost:8080/swagger-ui.html
> - OpenAPI JSON: http://localhost:8080/v3/api-docs
> - 에러 코드 표: [ERROR_CODES.md](./ERROR_CODES.md)
> - 프론트엔드 통합 가이드: [FRONTEND_INTEGRATION_GUIDE.md](./FRONTEND_INTEGRATION_GUIDE.md)

---

## 0. 목차

| § | 내용 |
|---|---|
| 1 | 공통 사항 (Base URL · 인증 · 응답 포맷 · 페이지네이션) |
| 2 | Auth — 4 엔드포인트 |
| 3 | User — 4 엔드포인트 (인증 필수) |
| 4 | Welfare — 2 엔드포인트 (Optional auth) |
| 5 | Category — 1 엔드포인트 (공개) |
| 5-2 | Region — 1 엔드포인트 (공개, Step 1·8 신규) |
| 6 | Bookmark — 3 엔드포인트 (인증 필수) |
| 7 | Chat — 1 엔드포인트 (인증 필수) |
| 8 | Health — 1 엔드포인트 (공개) |
| 9 | 엔드포인트 요약 표 (17개) |

---

## 1. 공통 사항

### 1-1. Base URL

| 환경 | Base URL |
|---|---|
| 로컬 개발 | `http://localhost:8080` |

모든 엔드포인트는 `/api/` prefix를 갖는다.

### 1-2. 인증

| 항목 | 값 |
|---|---|
| 방식 | JWT Bearer |
| 헤더 | `Authorization: Bearer <accessToken>` |
| access 만료 | 1시간 (3600s) |
| refresh 만료 | 7일 (604800s) |
| 갱신 정책 | Refresh Token Rotation — 갱신 시마다 raw 토큰 새로 발급 |

### 1-3. 공통 응답 포맷 — `ApiResponse<T>`

#### 성공
```json
{ "status": "SUCCESS", "data": { ... } }
{ "status": "SUCCESS", "message": "...", "data": { ... } }
```

#### 에러 (일반)
```json
{
  "status": "ERROR",
  "message": "...",
  "errorCode": "USER_NOT_FOUND",
  "timestamp": "2026-05-15T15:00:00"
}
```

#### 에러 (Validation 실패)
```json
{
  "status": "ERROR",
  "message": "입력값을 확인해주세요.",
  "errorCode": "VALIDATION_FAILED",
  "fields": { "email": "이메일 형식이 올바르지 않아요." },
  "timestamp": "2026-05-15T15:00:00"
}
```

`null` 필드는 자동 제외(`@JsonInclude(NON_NULL)`).

### 1-4. 페이지네이션 — `PageResponse<T>`

```json
{
  "items": [ ... ],
  "page": 0,
  "size": 10,
  "totalCount": 137,
  "hasNext": true
}
```

- `page`는 0-based.
- 서비스 레이어에서 size를 1~50 범위로 클램프.

### 1-5. 시간 포맷

ISO-8601 (예: `2026-05-15T15:00:00`). 응답의 `timestamp`, `createdAt` 등 모든 시각 필드에 적용.

### 1-6. CORS

| 허용 항목 | 값 |
|---|---|
| Origin | `http://localhost:5173`, `http://localhost:3000` |
| Method | GET, POST, PUT, DELETE, OPTIONS |
| Header | `Authorization`, `Content-Type` |
| Credentials | false |

---

## 2. Auth 도메인

### 2-1. `POST /api/auth/signup` — 회원가입

> 비인증. 가입 즉시 access + refresh 토큰 발급.

**Request**
```json
{ "email": "senior@mozi.test", "password": "password123" }
```

| 필드 | 타입 | 검증 |
|---|---|---|
| `email` | String | NotBlank + Email + Size≤255 |
| `password` | String | NotBlank + Size 8~72 (BCrypt 한계) |

**Response 200**
```json
{
  "status": "SUCCESS",
  "message": "회원가입이 완료되었어요.",
  "data": {
    "userId": 1,
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "k1pV...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

**주요 에러**: `VALIDATION_FAILED` (400), `EMAIL_ALREADY_EXISTS` (409)

---

### 2-2. `POST /api/auth/login` — 로그인

> 비인증. 새 토큰 1쌍 발급 (다른 기기 토큰 유지).

**Request**
```json
{ "email": "senior@mozi.test", "password": "password123" }
```

**Response 200**
```json
{
  "status": "SUCCESS",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "k1pV...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

**주요 에러**: `VALIDATION_FAILED` (400), `INVALID_CREDENTIALS` (401)

---

### 2-3. `POST /api/auth/refresh` — 토큰 갱신

> 비인증. **이전 raw 토큰 즉시 무효화** (Rotation).

**Request**
```json
{ "refreshToken": "k1pV..." }
```

**Response 200**: TokenResponse (2-2와 동일 구조 — 새 1쌍)

**주요 에러**: `INVALID_REFRESH_TOKEN` (401)

---

### 2-4. `POST /api/auth/logout` — 로그아웃 (전체 기기)

> 인증 필수. 본인의 모든 RefreshToken row 일괄 삭제.

**Request**: 본문 없음

**Response 200**
```json
{ "status": "SUCCESS", "message": "로그아웃되었어요.", "data": { "loggedOut": true } }
```

**주요 에러**: `UNAUTHORIZED` (401)

---

## 3. User 도메인 (모두 인증 필수)

### 3-1. `GET /api/users/me/profile` — 내 프로필 조회

**Response 200 (row 없음)**
```json
{ "status": "SUCCESS", "data": { "isCompleted": false } }
```

**Response 200 (row 있음)**
```json
{
  "status": "SUCCESS",
  "data": {
    "isCompleted": true,
    "birthDate": "1948-03-01",
    "gender": "F",
    "sidoCode": "11",
    "sigunguCode": "11680",
    "incomeType": "BASIC_PENSION",
    "householdType": "LIVING_ALONE",
    "isDisabled": false,
    "isMultiChild": false,
    "isMulticulturalNorthDefector": false,
    "isVeteran": false
  }
}
```

`isCompleted`는 row 존재 여부 — 프론트가 입력 강제 / 카드 표시 흐름 분기에 사용.

**필드 변경 이력**: Step 1~10 재설계로 `sido`/`sigungu`(자유 텍스트) → `sidoCode`/`sigunguCode`(REGION 코드), `incomeType`/`householdType` 문자열 → Enum name(`BASIC_PENSION`, `LIVING_ALONE` 등), boolean 6종 → 4종(`isLivingAlone`/`isSingleParentGrandparent` 제거 — `householdType`이 흡수). 한글 라벨은 클라이언트가 `GET /api/regions`로 lookup하거나 enum value 자체를 화면용 label과 매핑해 사용.

---

### 3-2. `PUT /api/users/me/profile` — 프로필 부분 갱신 (없으면 생성)

**옵션 C 정책**:
- 요청에 없는 필드 → **변경 없음**
- 명시적 `null` → **변경 없음** (Jackson record 한계로 absent ↔ explicit null 구분 불가)
- 필드 클리어 동작 미지원 (필요해지면 별도 phase에서 JsonNullable 도입 검토)

**Request (예시 — 거주지·가구형태만 갱신)**
```json
{ "sidoCode": "11", "sigunguCode": "11680", "householdType": "LIVING_ALONE" }
```

| 필드 | 타입 | 허용값 |
|---|---|---|
| `birthDate` | String (YYYY-MM-DD) | LocalDate |
| `gender` | enum | `M` / `F` / `NONE` |
| `sidoCode` | String (2자리) | REGION에 존재해야 함. 예: `"11"` |
| `sigunguCode` | String (5자리, nullable) | sidoCode와 같은 행에 존재해야 함. 예: `"11680"` |
| `incomeType` | enum | `NATIONAL_BASIC_LIVING` / `NEAR_POVERTY` / `BASIC_PENSION` / `GENERAL` / `UNKNOWN` |
| `householdType` | enum | `LIVING_ALONE` / `COUPLE` / `WITH_CHILDREN` / `GRANDPARENT_GRANDCHILD` / `OTHER` |
| `isDisabled`, `isMultiChild`, `isMulticulturalNorthDefector`, `isVeteran` | Boolean | true/false (총 4종) |

**REGION 검증 규칙** (Step 4~5):
- `sigunguCode`만 단독 입력 → 거부
- `sidoCode`가 REGION에 없음 → 거부
- `sigunguCode`가 입력한 `sidoCode` 산하가 아님 → 거부
- 위 거부 시 모두 `INVALID_REGION_CODE` (HTTP 400).

**Response 200**: 3-1과 동일 구조 (갱신된 전체 프로필).

**주요 에러**: `INVALID_REGION_CODE` (400) — 행정구역 검증 실패

---

### 3-3. `PUT /api/users/me/password` — 비밀번호 변경

**Request**
```json
{ "currentPassword": "password123", "newPassword": "newpw98765" }
```

| 필드 | 검증 |
|---|---|
| `currentPassword` | NotBlank |
| `newPassword` | NotBlank + Size 8~72 |

**Response 200**
```json
{
  "status": "SUCCESS",
  "message": "비밀번호가 변경되었어요. 다른 기기는 다시 로그인이 필요해요.",
  "data": { "changed": true }
}
```

비밀번호 변경 시 본인의 모든 RefreshToken 삭제 → 다른 기기 강제 재로그인.

**주요 에러**: `PASSWORD_MISMATCH` (400), `SAME_PASSWORD` (400)

---

### 3-4. `DELETE /api/users/me` — 회원 탈퇴

> Hard Delete + cascade. UserProfile/Bookmark/RefreshToken 일괄 삭제.

**Request**: 본문 없음

**Response 200**
```json
{ "status": "SUCCESS", "message": "회원 탈퇴가 완료되었어요.", "data": { "withdrawn": true } }
```

---

## 4. Welfare 도메인 (Optional auth)

### 4-1. `GET /api/welfares` — 동적 검색

> 비로그인 접근 가능. 로그인 시 본인 북마크 여부만 자동 반영(`isBookmarked`). Step 6에서 `applyMyProfile` 정책 폐기 — 검색 조건은 사용자가 명시한 query param만 사용한다.

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `keyword` | String | - | title/summary LIKE %keyword% |
| `category` | String | - | 단일 카테고리 코드 (예: `THM001`) |
| `region` | String | - | 지역명. LOCAL 자식만 매칭. CENTRAL/PRIVATE는 항상 통과, SEOUL은 "서울" 포함 시 통과 |
| `welfareType` | enum | - | `CENTRAL` / `LOCAL` / `PRIVATE` / `SEOUL` |
| `page` | int | 0 | 0-based |
| `size` | int | 10 | 1~50 클램프 |

**Response 200**
```json
{
  "status": "SUCCESS",
  "data": {
    "items": [
      {
        "id": "WLF00001234",
        "title": "노인 일자리 및 사회활동 지원사업",
        "summary": "어르신 일자리 매칭...",
        "welfareType": "CENTRAL",
        "organizationName": "보건복지부",
        "categories": [
          { "code": "THM005", "name": "고용", "type": "THEME" }
        ],
        "isBookmarked": false
      }
    ],
    "page": 0,
    "size": 10,
    "totalCount": 137,
    "hasNext": true
  }
}
```

---

### 4-2. `GET /api/welfares/{id}` — 복지 상세

**Path**: `id` — 자연키 (`WLF...` / `BOK...` / `SEL...`)

**Response 200**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": "WLF00001234",
    "title": "...",
    "summary": "...",
    "welfareType": "CENTRAL",
    "organizationName": "...",
    "targetAudience": "...",
    "applicationMethod": "...",
    "categories": [ ... ],
    "isBookmarked": true,
    "centralDetail": { "ministryName": "보건복지부", "lifeCycleCode": "..." },
    "localDetail": null,
    "privateDetail": null,
    "seoulDetail": null
  }
}
```

`*Detail` 4종 중 출처에 해당하는 1개만 채워지고 나머지는 응답에서 제외(NON_NULL).

**주요 에러**: `WELFARE_NOT_FOUND` (404)

---

## 5. Category 도메인 (공개)

### 5-1. `GET /api/categories?type=THEME|STATUS`

| 파라미터 | 타입 | 필수 | 값 |
|---|---|---|---|
| `type` | enum | ✅ | `THEME` (15행) 또는 `STATUS` (7행) |

**Response 200**
```json
{
  "status": "SUCCESS",
  "data": [
    { "code": "THM001", "name": "노인", "type": "THEME" },
    { "code": "THM002", "name": "장애인", "type": "THEME" }
  ]
}
```

**주요 에러**: `VALIDATION_FAILED` (400) — `type` 미지정 또는 잘못된 값

---

## 5-2. Region 도메인 (공개, Step 1·8 신규)

### `GET /api/regions` — 행정구역 마스터 조회

> 비로그인 접근 가능. 시드 적재(Step 8) 후 17 시도 + 약 229 시군구 응답.
> 프론트 cascading select(시도→시군구) 패턴에 사용.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `sido` | String | - | 시도 코드 (2자리). 지정 시 해당 시도 시군구 평면 응답, 미지정 시 시도별 그루핑 |

#### 응답 형태 A — sido 미지정: 시도별 그루핑

```json
{
  "status": "SUCCESS",
  "data": [
    {
      "sidoCode": "11",
      "sidoName": "서울특별시",
      "sigungus": [
        { "code": "11110", "name": "종로구" },
        { "code": "11680", "name": "강남구" }
      ]
    },
    {
      "sidoCode": "36",
      "sidoName": "세종특별자치시",
      "sigungus": [ { "code": null, "name": null } ]
    }
  ]
}
```

#### 응답 형태 B — sido 지정: 해당 시도 시군구 평면

```json
{
  "status": "SUCCESS",
  "data": [
    { "sidoCode": "11", "sidoName": "서울특별시", "sigunguCode": "11110", "sigunguName": "종로구" },
    { "sidoCode": "11", "sidoName": "서울특별시", "sigunguCode": "11680", "sigunguName": "강남구" }
  ]
}
```

**세종특별자치시**: 시군구 없음 → 단일 행, `sigunguCode/sigunguName = null`.

**유효 sido 코드**: 17개 — `11`(서울특별시), `26`(부산광역시), `27`(대구광역시), `28`(인천광역시), `29`(광주광역시), `30`(대전광역시), `31`(울산광역시), `36`(세종특별자치시), `41`(경기도), `43`(충청북도), `44`(충청남도), `46`(전라남도), `47`(경상북도), `48`(경상남도), `50`(제주특별자치도), `51`(강원특별자치도), `52`(전북특별자치도).

---

## 6. Bookmark 도메인 (모두 인증 필수)

### 6-1. `POST /api/bookmarks/{welfareId}` — 북마크 추가 (Idempotent)

**Path**: `welfareId` — 복지 자연키

**Response 200**
```json
{
  "status": "SUCCESS",
  "message": "북마크에 추가되었어요.",
  "data": { "bookmarkId": 42 }
}
```

> 이미 존재하면 같은 `bookmarkId`로 200 반환. 두 번 호출 모두 같은 응답.

**주요 에러**: `WELFARE_NOT_FOUND` (404)

---

### 6-2. `DELETE /api/bookmarks/{welfareId}` — 북마크 삭제

**Response 200**
```json
{ "status": "SUCCESS", "message": "북마크에서 삭제되었어요.", "data": { "deleted": true } }
```

**2단계 404**:
- welfare가 DB에 없음 → `WELFARE_NOT_FOUND`
- welfare는 있는데 본인 북마크가 없음 → `BOOKMARK_NOT_FOUND`

---

### 6-3. `GET /api/bookmarks` — 내 북마크 목록

**Query**: `page` (0), `size` (10, 1~50 클램프)

**Response 200**: `PageResponse<WelfareSummaryDto>` — 4-1과 동일 구조, 모든 항목 `isBookmarked: true` 고정, `createdAt DESC` 정렬.

---

## 7. Chat 도메인 (인증 필수)

### 7-1. `POST /api/chat` — 챗봇 호출

**Request**
```json
{ "message": "노인 일자리 알려줘", "conversationId": null }
```

| 필드 | 타입 | 검증 | 설명 |
|---|---|---|---|
| `message` | String | NotBlank + Size≤1000 | 사용자 메시지 |
| `conversationId` | String | nullable | 첫 호출 null, 후속 호출 이전 응답값 재전송 |

**Response 200**
```json
{
  "status": "SUCCESS",
  "data": {
    "reply": "강남구에 사시는 78세 어르신께 다음 복지를 추천드려요...",
    "welfares": [
      { "id": "WLF00001234", "title": "...", "isBookmarked": false, "categories": [ ... ], ... }
    ],
    "conversationId": "8c4f5a72-1f3e-4b2a-9c8e-7a1b3d4e5f6a"
  }
}
```

> 챗봇이 인사·잡담·범위 외 입력을 받으면 `welfares: []`로 반환 — `reply`만 표시. 자세한 약속은 [CHATBOT_API_CONTRACT.md §2.6](./CHATBOT_API_CONTRACT.md).

**주요 에러**: `CHATBOT_TIMEOUT` (504), `CHATBOT_UNAVAILABLE` (503), `CHATBOT_INVALID_RESPONSE` (502)

---

## 8. Health (공개)

### 8-1. `GET /api/health`

**Response 200**
```json
{ "status": "SUCCESS", "data": { "status": "UP" } }
```

---

## 9. 엔드포인트 요약 표 (17개)

| # | Method | Path | 인증 | 도메인 | 설명 |
|---|---|---|---|---|---|
| 1 | POST | `/api/auth/signup` | - | Auth | 회원가입 + 토큰 발급 |
| 2 | POST | `/api/auth/login` | - | Auth | 로그인 |
| 3 | POST | `/api/auth/refresh` | - | Auth | 토큰 갱신 (Rotation) |
| 4 | POST | `/api/auth/logout` | ✅ | Auth | 전체 기기 로그아웃 |
| 5 | GET | `/api/users/me/profile` | ✅ | User | 내 프로필 조회 |
| 6 | PUT | `/api/users/me/profile` | ✅ | User | 프로필 부분 갱신 |
| 7 | PUT | `/api/users/me/password` | ✅ | User | 비밀번호 변경 |
| 8 | DELETE | `/api/users/me` | ✅ | User | 회원 탈퇴 |
| 9 | GET | `/api/welfares` | Optional | Welfare | 동적 검색 (Step 6: applyMyProfile 제거됨) |
| 10 | GET | `/api/welfares/{id}` | Optional | Welfare | 상세 조회 |
| 11 | GET | `/api/categories` | - | Category | 카테고리 목록 |
| 12 | GET | `/api/regions` | - | Region | **신규**: 행정구역 마스터 (cascading select용) |
| 13 | POST | `/api/bookmarks/{welfareId}` | ✅ | Bookmark | 북마크 추가 (Idempotent) |
| 14 | DELETE | `/api/bookmarks/{welfareId}` | ✅ | Bookmark | 북마크 삭제 |
| 15 | GET | `/api/bookmarks` | ✅ | Bookmark | 내 북마크 목록 |
| 16 | POST | `/api/chat` | ✅ | Chat | 챗봇 호출 |
| 17 | GET | `/api/health` | - | Health | 헬스체크 |

> `Optional`은 비로그인도 접근 가능하나 로그인 시 응답이 풍부해짐을 의미.

---

## 10. 변경 이력

| 일자 | 변경 | 작성자 |
|---|---|---|
| 2026-05-15 | Phase 6 초안 작성 — Phase 1~5까지 16개 엔드포인트 정리 | MOZI 백엔드 |
| 2026-05-17 | USER_PROFILE_REDESIGN Step 1~10 반영 — GET /api/regions 신규(17개), User 프로필 필드 6개 교체(sidoCode/sigunguCode + Enum + boolean 4종), Welfare 검색 applyMyProfile 제거, INVALID_REGION_CODE 에러 신규 | MOZI 백엔드 |
