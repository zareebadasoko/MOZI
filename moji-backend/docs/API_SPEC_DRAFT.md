# MOZI Backend API Spec (Draft)

> Phase 0 작성 시점의 초안. **확정 명세는 `API_SPEC.md`**를 참조 (Phase 6 작성 + USER_PROFILE_REDESIGN Step 10 갱신).
> 본 문서는 초안이지만 일관성을 위해 주요 변경(2026-05-17: applyMyProfile 제거, GET /api/regions 신규 등)을 반영.

---

## 1. 공통 응답 규격

### 1-1. 성공
```json
{
  "status": "SUCCESS",
  "message": "...",
  "data": { /* 엔드포인트별 payload */ }
}
```

### 1-2. 실패
```json
{
  "status": "ERROR",
  "message": "사용자 친화 한글 메시지",
  "errorCode": "WELFARE_NOT_FOUND",
  "timestamp": "2026-05-03T21:30:00"
}
```

### 1-3. Validation 실패 (입력값 검증)
```json
{
  "status": "ERROR",
  "errorCode": "VALIDATION_FAILED",
  "message": "입력값을 확인해주세요",
  "fields": {
    "email": "이메일 형식이 아닙니다",
    "password": "8자 이상이어야 합니다"
  },
  "timestamp": "2026-05-03T21:30:00"
}
```
- `fields` 객체로 필드별 에러를 분리해 프론트가 각 입력란에 에러 표시 가능.

### 1-4. 페이지네이션 (`data` 형태)
```json
{
  "items": [ /* T[] */ ],
  "page": 0,
  "size": 10,
  "totalCount": 47,
  "hasNext": true
}
```

### 1-5. 인증
- 헤더: `Authorization: Bearer <JWT>`
- 토큰 만료 시간: Access 1시간 / Refresh 7일
- Refresh Token Rotation 적용 (매 갱신 시 새 refreshToken 발급, 이전 것 무효화)
- 비로그인 정책 (Phase 0 결정):

| 기능 | 비회원 | 회원 |
|---|---|---|
| 회원가입 / 로그인 | ✅ | ✅ |
| 카테고리 / 키워드 / 조건 검색 | ✅ (필터링 X) | ✅ (프로필 자동 필터링, 토글 가능) |
| 복지 상세 조회 | ✅ | ✅ |
| 카테고리 목록 조회 | ✅ | ✅ |
| 챗봇 사용 | ❌ (401) | ✅ |
| 프로필 설정 / 수정 / 탈퇴 | ❌ (401) | ✅ |
| 비밀번호 변경 | ❌ (401) | ✅ |
| 북마크 | ❌ (401) | ✅ |

### 1-6. CORS 정책 (Phase 6에서 최종 확정)
- 허용 Origin: `http://localhost:3000` (프론트 개발 환경), 추후 배포 도메인 추가
- 허용 Method: GET, POST, PUT, DELETE, OPTIONS
- 허용 Header: `Authorization`, `Content-Type`

---

## 2. 엔드포인트 매트릭스

| # | 도메인 | Method | Path | 인증 | Request | Response (`data`) | Phase |
|---|---|---|---|---|---|---|---|
| 1 | Health | GET | `/api/health` | ❌ | - | `{ status: "UP" }` | Phase 1 |
| 2 | Auth | POST | `/api/auth/signup` | ❌ | `{ email, password }` | `{ userId, accessToken, refreshToken, tokenType, expiresIn }` | Phase 3 |
| 3 | Auth | POST | `/api/auth/login` | ❌ | `{ email, password }` | `{ accessToken, refreshToken, tokenType, expiresIn }` | Phase 3 |
| 4 | Auth | POST | `/api/auth/refresh` | ❌ | `{ refreshToken }` | `{ accessToken, refreshToken, tokenType, expiresIn }` | Phase 3 |
| 5 | Auth | POST | `/api/auth/logout` | ✅ | - | `{ loggedOut: true }` | Phase 3 |
| 6 | UserProfile | GET | `/api/users/me/profile` | ✅ | - | `UserProfileDto` (`isCompleted` 플래그 포함) | Phase 4 |
| 7 | UserProfile | PUT | `/api/users/me/profile` | ✅ | `UserProfileDto` (부분 필드 허용) | `UserProfileDto` (갱신본) | Phase 4 |
| 8 | User | PUT | `/api/users/me/password` | ✅ | `{ currentPassword, newPassword }` | `{ changed: true }` | Phase 4 |
| 9 | User | DELETE | `/api/users/me` | ✅ | - | `{ withdrawn: true }` | Phase 4 |
| 10 | Welfare | GET | `/api/welfares` | ⭕ Optional† | `keyword?, category?, region?, welfareType?, page=0, size=10` (Step 6: applyMyProfile 제거) | 페이지(`WelfareSummaryDto[]`) | Phase 4 |
| 17 | Region | GET | `/api/regions` | ❌ | `sido?` | 시도 그루핑 또는 시군구 평면 (Step 1·8 신규) | USER_PROFILE_REDESIGN |
| 11 | Welfare | GET | `/api/welfares/{id}` | ⭕ Optional | - | `WelfareDetailDto` (`isBookmarked` 포함) | Phase 4 |
| 12 | Bookmark | POST | `/api/bookmarks/{welfareId}` | ✅ | - | `{ bookmarkId }` (이미 있으면 200 idempotent) | Phase 4 |
| 13 | Bookmark | DELETE | `/api/bookmarks/{welfareId}` | ✅ | - | `{ deleted: true }` | Phase 4 |
| 14 | Bookmark | GET | `/api/bookmarks` | ✅ | `page=0, size=10` | 페이지(`WelfareSummaryDto[]`) | Phase 4 |
| 15 | Chat | POST | `/api/chat` | ✅ | `{ message, conversationId? }` | `{ reply, welfares: WelfareSummaryDto[], conversationId }` | Phase 5 |
| 16 | Category | GET | `/api/categories` | ❌ | `type=THEME\|STATUS` | `CategoryDto[]` | Phase 4 |

> † **Optional 인증**: 토큰이 있으면 응답의 `isBookmarked`가 자동 채워짐. **2026-05-17 USER_PROFILE_REDESIGN Step 6 이후**: 본인 프로필 자동 필터(`applyMyProfile`) 정책 폐기 — 검색은 명시 query param만 사용.

---

## 3. 검색·필터링 상세 (#10 GET /api/welfares)

> ⭐ **MVP 핵심 기능 중 하나**. 챗봇 외에 사용자가 직접 복지를 찾는 **기본 검색** 통로.
> 기존 복지 사이트(복지로 등)가 제공하는 키워드 검색 + 조건 필터링 형태를 백엔드 단에서 지원.

### 3-1. Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `keyword` | String | ❌ | 제목/요약 텍스트에서 부분 일치 검색 (LIKE %keyword%) | `병원비` |
| `category` | String | ❌ | Category code 단일 값 | `의료` |
| `region` | String | ❌ | 지역명. CENTRAL/PRIVATE는 region 무관하게 항상 포함, LOCAL은 regionName LIKE 매칭, SEOUL은 region에 "서울" 포함 시에만 통과 | `서울특별시` |
| `welfareType` | enum | ❌ | `CENTRAL` / `LOCAL` / `PRIVATE` / `SEOUL` 중 하나 | `LOCAL` |
| `page` | int | ❌ | 0부터 시작, 기본 0 | `0` |
| `size` | int | ❌ | 페이지당 건수, 기본 10, 최대 50 | `10` |

> ⚠️ **Step 6에서 `applyMyProfile` 파라미터 폐기됨** (2026-05-17). 본인 맞춤 추천은 `POST /api/chat` 흐름이 전담.

### 3-2. 동작 규칙

**모든 파라미터는 AND 조건으로 결합**:
```
keyword=병원비 & category=의료 & region=서울특별시
→ 제목/요약에 "병원비" 포함 + 카테고리 "의료" + 서울 지역
```

**파라미터 미지정 = 해당 조건 무시**:
```
GET /api/welfares
→ 전체 복지를 최신순으로 페이지네이션 반환
```

### 3-3. `region` 파라미터 동작 (출처별 분기)

`region`은 **단순 AND 조건이 아니라 출처별로 다른 매칭 정책**을 적용한다 — 노년층 사용자가 "서울"로 검색해도 전국 단위 정부 사업이 보여야 한다는 UX 결정.

| 출처 | region 동작 |
|---|---|
| `CENTRAL` | 전국 단위 사업 → region 무관하게 **항상 포함** |
| `PRIVATE` | 민간 단체 사업 → region 무관하게 **항상 포함** |
| `LOCAL` | `regionName LIKE %region%` 매칭하는 row만 |
| `SEOUL` | region에 "서울" 문자열이 포함될 때만 모두 통과 (그 외 지역 입력 시 제외) |

예시:
- `region=서울` → CENTRAL + PRIVATE 전체 + LOCAL 서울 자치구 + SEOUL 전체
- `region=경기` → CENTRAL + PRIVATE 전체 + LOCAL 경기도 시·군 + SEOUL 제외

> ⚠️ region 컬럼은 LOCAL 자식에만 있어 CENTRAL/PRIVATE/SEOUL은 매칭 컬럼이 없다. 위 정책은 출처별 의미 차이(전국 vs 지자체)를 반영해 자동 분기 처리.

### 3-4. 응답 정렬
- 기본: 최신 등록순(`createdAt DESC`)
- v2 후보: 인기순, 마감 임박순 (MVP에서는 미지원)

### 3-5. 키워드 검색 대상 컬럼
- `WelfareCommon.title`
- `WelfareCommon.summary`

> 💡 **성능 주의**: LIKE 검색은 컬럼이 많을수록 느려짐.
> MVP는 `title + summary`만으로 한정 (targetAudience는 텍스트 길이가 너무 길어 제외).
> v2에서 Full-Text Search(MySQL FULLTEXT 인덱스 또는 Elasticsearch) 검토.

### 3-5. 사용 예시

```
# 키워드만
GET /api/welfares?keyword=병원비

# 카테고리만 (기존 동작)
GET /api/welfares?category=의료

# 키워드 + 카테고리
GET /api/welfares?keyword=지원&category=주거

# 검색은 명시 query param만 사용 (Step 6 이후)
GET /api/welfares?keyword=병원비
  → 로그인이든 비로그인이든 동일한 검색 결과
  → 로그인 사용자만 응답에 isBookmarked 자동 채워짐

# 모든 조건 조합
GET /api/welfares?keyword=노인&category=의료&region=서울특별시&welfareType=LOCAL&page=0&size=20
```

---

## 4. DTO 필드 요지

### 4-1. `UserProfileDto` (Step 1~10 재설계 적용)
| 필드 | 타입 | 비고 |
|---|---|---|
| `isCompleted` | `boolean` | 프로필이 한 번이라도 저장됐는지 (응답 전용) |
| `birthDate` | `LocalDate` | nullable |
| `gender` | `enum { M, F, NONE }` | nullable |
| `sidoCode` | `String(2)` | REGION 마스터 참조 (예: `"11"`) |
| `sigunguCode` | `String(5)` | REGION 마스터 참조, 시도만 선택은 null (예: `"11680"`) |
| `incomeType` | `enum IncomeType` | 5종: `NATIONAL_BASIC_LIVING`/`NEAR_POVERTY`/`BASIC_PENSION`/`GENERAL`/`UNKNOWN` |
| `householdType` | `enum HouseholdType` | 5종: `LIVING_ALONE`/`COUPLE`/`WITH_CHILDREN`/`GRANDPARENT_GRANDCHILD`/`OTHER` |
| `isDisabled` | `boolean` | 장애 여부 |
| `isMultiChild` | `boolean` | 다자녀 |
| `isMulticulturalNorthDefector` | `boolean` | 다문화·탈북민 |
| `isVeteran` | `boolean` | 보훈대상자 |

> ⚠️ **2026-05-17 USER_PROFILE_REDESIGN 적용**: `sido`/`sigungu` 자유 텍스트 → 코드 + REGION, `incomeType`/`householdType` 문자열 → Enum, boolean 6종 → 4종(`isLivingAlone`/`isSingleParentGrandparent` 제거 — `householdType`이 흡수). 자세한 영향은 `FRONTEND_MIGRATION_NOTES.md` 참조.

> 💡 **PUT 동작 정책 (MVP)**: 요청에 포함되지 않은 필드는 변경되지 않음. 명시적으로 `null`을 보내도 변경되지 않음(무변경).
> Jackson record + nullable 필드 조합에서 absent와 explicit null을 구분할 수 없어 두 케이스를 모두 "무변경"으로 통일했다.
> 한 번 채워진 필드는 새 값으로만 갱신 가능하며 클리어 동작은 미지원. 필요 시 향후 `JsonNullable` 라이브러리 도입 검토.

### 4-2. `WelfareSummaryDto` (목록·검색 카드용)
| 필드 | 타입 |
|---|---|
| `id` | `String` (자연키) |
| `title` | `String` |
| `summary` | `String` |
| `welfareType` | `enum { CENTRAL, LOCAL, PRIVATE, SEOUL }` |
| `organizationName` | `String` |
| `categories` | `CategoryDto[]` |
| `isBookmarked` | `boolean` (로그인 시: 본인 북마크 여부 / 비로그인 시: false) |

### 4-3. `WelfareDetailDto` (상세 조회 — 자식 종류별 필드 포함)
- 부모 공통 필드: `WelfareSummaryDto` 전체 + `targetAudience`, `applicationMethod`
- 자식별 nullable 필드 (4개 중 1개만 채워짐):
  - `centralDetail`: `{ supportYear, supportType, selectionCriteria, supportDetails, contactNumber, detailUrl, supportCycle, processSteps }`
  - `localDetail`: `{ regionName, supportYear, supportType, selectionCriteria, supportDetails, contact, detailUrl, supportCycle }`
  - `privateDetail`: `{ startDate, endDate, supportDetails, contactNumber, detailUrl, contactEmail, requiredDocuments }`
  - `seoulDetail`: `{ supportType, contactNumber, detailContent, supportCycle, requiredDocuments }`

### 4-4. `CategoryDto`
| 필드 | 타입 |
|---|---|
| `id` | `Long` |
| `code` | `String` |
| `name` | `String` |
| `type` | `enum { THEME, STATUS }` |

### 4-5. Auth 응답 표준 (`signup`, `login`, `refresh` 공통)
```json
{
  "userId": 1,
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```
- `userId`는 signup 응답에만 포함 (login/refresh는 userId 생략 또는 포함 — 정책 결정)
- `expiresIn`: access token의 만료 시간(초)
- `refresh`는 매 갱신 시 refreshToken도 새로 발급(Rotation), 이전 refreshToken은 무효화

### 4-6. 회원 탈퇴 정책 (#9 DELETE /api/users/me)
- **Hard Delete** 적용 (Soft Delete 미사용)
- 삭제 대상: `User` + `UserProfile` + 해당 사용자의 `Bookmark` 전체
- JPA의 `cascade = CascadeType.ALL` + `orphanRemoval = true`로 일괄 삭제
- 탈퇴 후 발급된 토큰(Access/Refresh)은 즉시 무효화

> 💡 MVP는 시연용이라 Soft Delete 불필요. 개인정보 보호 측면에서도 Hard가 명확.

---

## 5. Chat 정책 (`POST /api/chat`)

### 5-1. 응답 형태
```json
{
  "reply": "안녕하세요. 다음 복지 혜택을 추천드려요...",
  "welfares": [ /* WelfareSummaryDto[] */ ],
  "conversationId": "uuid"
}
```

### 5-2. conversationId 정책 (MVP)

#### 발급
- 클라이언트가 `conversationId` 없이 **첫 메시지** 전송
- 백엔드가 UUID 생성 → 챗봇 서버에 `message + conversationId` 함께 전달
- 응답에 `conversationId` 포함하여 반환

#### 유지
- 클라이언트는 `sessionStorage`에 `conversationId` 저장
- 같은 브라우저 세션 동안 같은 `conversationId` 사용
  - 페이지 이동 / 새로고침 OK (sessionStorage 유지됨)
  - 추천 복지 클릭 후 챗봇 페이지 복귀 시 이전 대화 그대로 유지
- 두 번째 메시지부터 클라이언트가 `conversationId` 함께 전송

#### 새 대화 시작 (사용자 액션)
- 챗봇 화면의 **"새 대화 시작" 버튼** 클릭 시
- 클라이언트가 `sessionStorage`의 `conversationId` 삭제
- 다음 메시지는 새 `conversationId` 발급

#### 종료
- 창 닫기 → `sessionStorage` 자동 삭제 → 다음 접속 시 새 대화
- 로그아웃 → 클라이언트가 `sessionStorage` 삭제 → 다음 로그인 시 새 대화

#### 저장 책임
- **MOZI 백엔드**: 저장 X (단순 브릿지)
- **챗봇 서버**: 자체 정책으로 보관 (LLM 컨텍스트용 + 자체 보관 기간)
- **MOZI 백엔드는 명시적 삭제 요청을 보내지 않음** (챗봇 서버가 자체 만료 정책으로 정리)

### 5-3. 동작 규칙
- 챗봇 서버가 반환한 `recommendedWelfareIds`를 `WelfareCommon` 조회로 hydrate 후 반환
- 누락된 ID(우리 DB에 없음)는 응답에서 제외하고 서버 로그 남김
- 챗봇 서버 timeout(8초) 시: `errorCode=CHATBOT_TIMEOUT`, message="지금은 추천이 어려워요. 잠시 후 다시 시도해주세요."

### 5-4. 비로그인 / 프로필 미설정
- 비로그인 호출: `401 UNAUTHORIZED`
- 프로필 미설정 사용자: 빈 프로필로 챗봇 호출 (일반 추천) — Phase 5 직전 최종 확정

### 5-5. 챗봇팀과 협의 필요 사항
- [ ] conversationId를 백엔드가 발급해도 OK인가? (방식 A 채택 가정)
- [ ] 같은 conversationId로 여러 메시지 시 이전 대화 컨텍스트 반영 동작 확인
- [ ] 챗봇 서버의 대화 이력 보관 기간 (24h / 7d / 30d 등)
- [ ] 사용자별 이력 조회 API 제공 여부 (v2 검토용)

---

## 6. 주요 에러 코드

| errorCode | HTTP | message (예시) |
|---|---|---|
| `VALIDATION_FAILED` | 400 | 입력값을 확인해주세요. |
| `EMAIL_ALREADY_EXISTS` | 409 | 이미 가입된 이메일입니다. |
| `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호가 올바르지 않습니다. |
| `UNAUTHORIZED` | 401 | 로그인 후 이용 가능합니다. |
| `TOKEN_EXPIRED` | 401 | 로그인 시간이 만료됐어요. 다시 로그인해주세요. |
| `INVALID_TOKEN` | 401 | 로그인 정보가 올바르지 않아요. |
| `INVALID_REFRESH_TOKEN` | 401 | 다시 로그인해주세요. |
| `PASSWORD_MISMATCH` | 400 | 현재 비밀번호가 일치하지 않습니다. |
| `WELFARE_NOT_FOUND` | 404 | 해당 복지 정보가 없습니다. |
| `BOOKMARK_NOT_FOUND` | 404 | 북마크가 존재하지 않습니다. |
| `CHATBOT_TIMEOUT` | 504 | 지금은 추천이 어려워요. 잠시 후 다시 시도해주세요. |
| `INTERNAL_ERROR` | 500 | 잠시 후 다시 시도해주세요. 문제가 계속되면 고객센터로 문의해주세요. |

> Phase 6에서 `ERROR_CODES.md`로 분리 + Springdoc OpenAPI에 자동 노출.

---

## 7. v2 후보 (MVP 완료 후 시간 여유 시 검토)

| 우선순위 | 기능 | 비고 |
|---|---|---|
| 1순위 | 검색 키워드 자동완성 (`GET /api/welfares/suggestions`) | 노년층 친화 |
| 1순위 | 인기 복지 / 추천 큐레이션 | 빈 검색 시 노출 |
| 1순위 | 마감 임박 복지 안내 | 공익 가치 |
| 2순위 | 챗봇 대화 이력 조회/삭제 (현재 백엔드 저장 X) | 챗봇 팀 협의 필요 |
| 2순위 | 북마크 폴더/태그 | UX 개선 |
| 3순위 | 관리자 대시보드 API | PRD 외 영역 |
| 3순위 | Full-Text Search (MySQL FULLTEXT 또는 Elasticsearch) | 검색 성능 |
| 3순위 | Rate Limiting | 보안/안정성 |
| 3순위 | Redis 캐싱 (카테고리/인기 복지) | 성능 |
