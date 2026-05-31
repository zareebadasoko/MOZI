# USER FLOW — 화면 단위 사용자 흐름

> 이 문서는 백엔드 `USER_FLOW.md`의 7개 시퀀스를 **프론트 화면 전환 관점**으로 재해석한다.
> 모든 mermaid는 머릿속에 그릴 수 있는 수준으로 단순화했다. 정확한 API 스키마는 `../backend_project/docs/API_SPEC.md` 참조.

---

## 1. 전체 화면 맵

```mermaid
flowchart LR
  subgraph Public[비로그인 가능]
    Login["/login"]
    Signup["/signup"]
    Search["/welfares"]
    Detail["/welfares/:id"]
  end
  subgraph Protected[로그인 필수]
    Chat["/"]
    Bookmarks["/bookmarks"]
    My["/me"]
    Profile["/me/profile"]
    Password["/me/password"]
  end

  Login --> Chat
  Signup --> Chat
  Chat --> Search
  Chat --> Detail
  Search --> Detail
  Detail --> Bookmarks
  Chat --> My
  My --> Profile
  My --> Password
  My -. 탈퇴 모달 .-> Login
```

라우팅 정책 요약:
- **로그인 필수(`Protected`)**: 토큰 없으면 `/login`으로 자동 이동
- **비로그인 가능(`Public`)**: 검색/상세는 누구나 조회. 단, 북마크 버튼은 로그인 사용자만 활성
- **`/`(홈)** = 챗봇 메인 = 로그인 필수

자세한 라우팅 가드 구현은 §9 + `API_CLIENT_GUIDE.md` §8 참조.

---

## 2. 시나리오 A — 신규 가입 → 프로필 입력 → 첫 검색

```mermaid
sequenceDiagram
  autonumber
  participant U as 사용자
  participant R as React (페이지)
  participant API as api/client.js
  participant BE as 백엔드

  U->>R: /signup 진입
  U->>R: 이메일·비밀번호·확인 입력 → 가입 클릭
  R->>API: signup({ email, password })
  API->>BE: POST /api/auth/signup
  BE-->>API: 200 { userId, accessToken, refreshToken }
  API-->>R: 성공
  R->>R: setAccessToken(메모리) + setRefreshToken(sessionStorage)
  R->>R: AuthContext.login() → isAuthenticated=true
  R-->>U: "환영합니다 + 프로필 입력 권유 모달"

  alt 사용자가 "지금 입력" 선택
    U->>R: 프로필 입력 (생년월일/성별/거주지 등)
    R->>API: updateMyProfile(partial)
    API->>BE: PUT /api/users/me/profile
    BE-->>API: 200 갱신된 프로필
  else "나중에" 선택
    R-->>U: 모달 닫고 / (홈 챗봇) 이동
  end

  U->>R: 챗봇 입력란에 "병원비 도움이 필요해"
  R->>API: sendChat({ message, conversationId: null })
  API->>BE: POST /api/chat (Bearer)
  BE-->>API: 200 { reply, welfares, conversationId }
  R->>R: setConversationId(sessionStorage)
  R-->>U: 답변 텍스트 + 추천 복지 카드 표시
```

핵심 포인트:
- 가입 직후 **즉시 토큰 발급되어 자동 로그인 상태** (별도 로그인 단계 없음)
- 프로필 입력 권유는 **모달**로 (페이지 이동 X — 사용자가 챗봇으로 바로 갈 수 있게)
- `conversationId`는 **첫 호출 시 null**, 응답으로 받은 ID를 sessionStorage에 저장

빈 상태 / 에러 처리:
- 이메일 중복(`EMAIL_ALREADY_EXISTS`) → "이미 가입된 이메일이에요" 인풋 옆 표시
- 검증 실패(`VALIDATION_FAILED`) → `fields` 객체를 각 입력란에 매핑
- 챗봇 타임아웃(`CHATBOT_TIMEOUT`) → "지금은 추천이 어려워요" 안내 + 재시도 버튼

---

## 3. 시나리오 B — 재방문 로그인 (토큰 자동 복구)

```mermaid
sequenceDiagram
  autonumber
  participant U as 사용자
  participant R as React (AuthProvider)
  participant API as api/client.js
  participant SS as sessionStorage
  participant BE as 백엔드

  U->>R: 브라우저에서 앱 URL 진입 (새로고침 포함)
  R->>R: bootstrapping=true → Spinner 표시
  R->>SS: getRefreshToken()
  alt sessionStorage에 refreshToken 있음
    SS-->>R: "rt_xxx..."
    R->>API: baseFetch("/api/users/me/profile")
    API->>BE: GET (Authorization 없음 = 토큰 없으므로)
    BE-->>API: 401 TOKEN_EXPIRED (또는 UNAUTHORIZED)
    API->>API: tryRefresh()
    API->>BE: POST /api/auth/refresh { refreshToken }
    BE-->>API: 200 새 accessToken + 새 refreshToken
    API->>SS: setRefreshToken(새 값)
    API->>API: setAccessToken(메모리)
    API->>BE: GET /api/users/me/profile (재시도, Bearer 부착)
    BE-->>API: 200 프로필
    API-->>R: 성공
    R->>R: isAuthenticated=true
    R-->>U: 마지막 페이지 또는 홈(/)
  else refreshToken 없음
    R-->>U: /login 화면
  end
```

핵심 포인트:
- **accessToken은 새로고침 시 항상 사라짐** (메모리 보관) → refresh가 필수 절차
- AuthProvider의 useEffect에서 **1회만** 시도 (조건: `getRefreshToken()`이 있을 때)
- `bootstrapping` 동안은 ProtectedRoute가 Spinner를 보여줘 깜빡임 방지
- refresh 실패 시 토큰 모두 클리어 → 자연스럽게 로그인 화면

> 본 흐름은 `API_CLIENT_GUIDE.md` §8 `AuthProvider`의 useEffect 구현과 일치.

---

## 4. 시나리오 C — 챗봇 첫 대화 → 후속 질문 → 새 대화

```mermaid
sequenceDiagram
  autonumber
  participant U as 사용자
  participant R as ChatPage
  participant API as api/chat.js
  participant SS as sessionStorage (conversationId)
  participant BE as 백엔드

  Note over R,SS: 처음 / 진입 — conversationId 없을 수도, 있을 수도

  U->>R: "노인 일자리 알려줘" 전송
  R->>SS: getConversationId() → null (첫 호출)
  R->>API: sendChat({ message, conversationId: null })
  API->>BE: POST /api/chat
  BE-->>API: 200 { reply, welfares: [...], conversationId: "uuid-A" }
  API-->>R: 결과
  R->>SS: setConversationId("uuid-A")
  R-->>U: 챗봇 응답 + 추천 복지 카드

  Note over R,SS: 후속 질문

  U->>R: "수원시 기준이야"
  R->>SS: getConversationId() → "uuid-A"
  R->>API: sendChat({ message, conversationId: "uuid-A" })
  API->>BE: POST /api/chat
  BE-->>API: 200 { reply, welfares, conversationId: "uuid-A" }
  R-->>U: 맥락 이어진 응답

  Note over R,SS: 새 대화 시작

  U->>R: "새 대화 시작" 버튼 클릭
  R->>SS: clearConversationId()
  R->>R: 메시지 리스트 비우기
  U->>R: 새 메시지 입력
  R->>API: sendChat({ message, conversationId: null }) → 새 ID 발급
```

핵심 포인트:
- `conversationId` = sessionStorage 단일 출처 (`utils/conversationStore.js`)
- 첫 호출에 `null`을 보내면 **백엔드가 UUID v4 발급**
- 후속 호출은 받은 ID를 그대로 재전송
- "새 대화 시작" = sessionStorage에서 ID 삭제 + 화면 리셋

빈 상태 / 에러:
- 첫 진입 시 메시지 목록 비어있으면 → 안내문 "안녕하세요. 어떤 복지를 찾고 계신가요?"
- `recommendedWelfareIds = []` (잡담/범위 외) → 답변 텍스트만 표시, 카드 영역은 숨김
- `CHATBOT_TIMEOUT/UNAVAILABLE/INVALID_RESPONSE` → 마지막 메시지 옆 "다시 시도" 버튼

---

## 5. 시나리오 D — 복지 검색 → 상세 → 북마크

```mermaid
sequenceDiagram
  autonumber
  participant U as 사용자
  participant R as React
  participant API as api/welfare.js & bookmark.js
  participant BE as 백엔드

  U->>R: /welfares 진입 (필터 초기값 = 전체)
  R->>API: fetchWelfares({ page: 0, size: 10 })
  API->>BE: GET /api/welfares?page=0&size=10
  BE-->>API: { items, hasNext, ... }
  R-->>U: 카드 리스트 표시

  alt 사용자가 검색 버튼 클릭
    U->>R: 폼 변경 → "검색" 클릭
    R->>R: pendingFilters → URL 쿼리에 commit, page=0
    R->>R: useEffect(URL) 자동 트리거
    R->>API: fetchWelfares({...필터, region(파생), page: 0})
  end

  alt 페이지 번호 클릭
    U->>R: 페이지 N 선택
    R->>R: URL 의 page=N 만 갱신
    R->>API: fetchWelfares({...필터, region(파생), page: N})
    R->>R: results 교체 + 부드럽게 상단 스크롤
  end

  alt 카드 클릭 후 "목록으로" 또는 브라우저 뒤로가기
    U->>R: 상세에서 목록으로
    R->>R: navigate(-1) — 검색 URL 쿼리 그대로 복원
    Note over R: state.from fallback: 새로고침 후엔 카드 진입 시 부착한 from 으로
  end

  U->>R: 카드 클릭 (id="WLF00001234")
  R->>API: fetchWelfareDetail("WLF00001234")
  API->>BE: GET /api/welfares/WLF00001234
  BE-->>API: 상세 DTO (centralDetail/localDetail/... 중 1개 채워짐)
  R-->>U: 상세 페이지 + "저장하기" 버튼

  alt 로그인 상태
    U->>R: "저장하기" 클릭
    R->>API: createBookmark("WLF00001234")
    API->>BE: POST /api/bookmarks/WLF00001234
    BE-->>API: 200 { bookmarkId } (Idempotent)
    R-->>U: 버튼 라벨 "저장됨" 으로 변경 + 토스트

    U->>R: "저장됨" 다시 클릭
    R->>API: deleteBookmark("WLF00001234")
    API->>BE: DELETE /api/bookmarks/WLF00001234
    BE-->>API: 200 { deleted: true }
    R-->>U: 버튼 라벨 "저장하기" 복귀
  else 비로그인 상태
    R-->>U: "저장하려면 로그인이 필요해요" 안내 + 로그인 버튼
  end
```

핵심 포인트:
- 검색은 **optional auth** — 비로그인도 조회 가능. 로그인 시 응답에 `isBookmarked` 가 자동 채워진다.
- 본인 맞춤 추천(옛 `applyMyProfile`)은 2026-05-17 백엔드 변경으로 검색 API에서 폐기됨. 대신 `POST /api/chat`(챗봇)이 담당.
- **검색 조건과 페이지 번호는 URL 쿼리에 보관** — 브라우저 뒤로가기/새로고침/링크 공유 모두 자동 복원. "검색" 클릭 = URL commit, 폼 입력 중 임시값은 URL 에 안 들어감.
- 필터 변경(검색 클릭) 시 `page=0`으로 리셋 (이전 결과가 섞이지 않게)
- 페이지 번호 방식 (이전/다음 + 최대 5개 번호 chip, 무한 스크롤 X)
- 북마크는 **Idempotent** — 같은 ID로 POST 중복 호출해도 안전
- 비로그인 + 북마크 시도 → 모달이 아니라 인라인 안내 (의도된 흐름이라 모달 차단보다 친절)

빈 상태:
- 검색 결과 0건 → "조건에 맞는 복지가 없어요. 다른 키워드를 시도해보세요"
- `WELFARE_NOT_FOUND` (잘못된 ID 직접 진입) → "복지 정보를 찾을 수 없어요" + 목록으로 돌아가기 버튼

---

## 6. 시나리오 E — 마이페이지 → 비밀번호 변경 (모든 기기 강제 재로그인)

```mermaid
sequenceDiagram
  autonumber
  participant U as 사용자 (기기 A)
  participant R as React
  participant API as api/user.js
  participant BE as 백엔드
  participant Other as 사용자 (기기 B)

  U->>R: /me/password 진입
  U->>R: 현재 비밀번호 + 새 비밀번호 + 새 비밀번호 확인 입력
  R->>R: 클라이언트 측 확인 (새/확인 일치, 새/현재 다름 등)
  R->>API: changePassword({ currentPassword, newPassword })
  API->>BE: PUT /api/users/me/password
  alt 현재 비밀번호 맞음
    BE-->>API: 200 { "changed": true }
    Note right of BE: 백엔드: 본 user의 모든 RefreshToken 삭제 (새 토큰 발급 X)
    API-->>R: 성공
    R->>R: clearTokens() + AuthContext.logout()
    R-->>U: "비밀번호가 변경되었어요. 다시 로그인해주세요." 토스트
    R->>R: navigate("/login")
  else PASSWORD_MISMATCH / SAME_PASSWORD
    BE-->>API: 400 + errorCode
    API-->>R: ApiError
    R-->>U: 현재 비밀번호 입력란 옆 에러 표시
  end

  Note over Other,BE: 기기 B에서 다음 API 호출 시
  Other->>BE: 기존 accessToken (아직 유효할 수 있음) 또는 refresh
  alt accessToken 유효 (1시간 이내)
    BE-->>Other: 200 (정상 동작)
    Note right of Other: 만료 후 refresh 시도 시점에 차단됨
  else refresh 시도
    Other->>BE: POST /api/auth/refresh (옛 refreshToken)
    BE-->>Other: 401 INVALID_REFRESH_TOKEN
    Other->>Other: 토큰 클리어 → /login 이동
  end
```

핵심 포인트:
- 현재 비밀번호 검증은 **백엔드가** 한다 (프론트는 입력 형식 검증만)
- 백엔드 응답은 `{ "changed": true }`만 — **새 토큰을 발급하지 않는다**
- 백엔드는 본 user의 **모든 refreshToken을 삭제** → 본 기기 포함 모든 기기에서 재로그인 필요
- 본 기기 프론트는 변경 성공 시 즉시 `clearTokens()` + `/login` 이동
- 토스트 문구: **"비밀번호가 변경되었어요. 다시 로그인해주세요."**
  - 본 기기까지 재로그인이 필요해진 점이 사용자에게 명확히 전달되어야 함

---

## 7. 시나리오 F — 회원 탈퇴 (Hard Delete)

```mermaid
sequenceDiagram
  autonumber
  participant U as 사용자
  participant R as React (MyPage)
  participant API as api/user.js
  participant BE as 백엔드

  U->>R: 마이페이지 → "회원 탈퇴" 클릭
  R-->>U: 확인 모달 표시
  Note right of R: 모달: "정말 탈퇴하시겠어요? 저장한 복지·프로필이 모두 삭제됩니다"
  U->>R: "탈퇴할게요" 확인
  R->>API: withdraw()
  API->>BE: DELETE /api/users/me
  BE->>BE: User + UserProfile + Bookmark + RefreshToken 일괄 삭제 (cascade)
  BE-->>API: 200 { (메시지) }
  API-->>R: 성공
  R->>R: clearTokens() + clearConversationId() + AuthContext.logout()
  R-->>U: "탈퇴가 완료되었어요" 토스트
  R->>R: /login 이동
```

핵심 포인트:
- **확인 모달 필수** (액션이 비가역) — 노년층은 실수로 누를 가능성 ↑
- 모달 본문에 "무엇이 삭제되는지" 명시
- 탈퇴 후 같은 이메일로 재가입 가능 (백엔드 보장)

---

## 8. 시나리오 G — 토큰 만료 자동 복구 (백그라운드, 모든 호출 공통)

```mermaid
sequenceDiagram
  autonumber
  participant Comp as 임의 컴포넌트
  participant API as baseFetch
  participant BE as 백엔드

  Comp->>API: fetchWelfares({...})
  API->>BE: GET (Bearer: 만료된 accessToken)
  BE-->>API: 401 TOKEN_EXPIRED

  rect rgb(245,250,255)
  Note over API: 자동 처리 — 호출자는 모름
  API->>API: getRefreshToken() → "rt_..."
  API->>BE: POST /api/auth/refresh
  alt refresh 성공
    BE-->>API: 200 { 새 access, 새 refresh }
    API->>API: setAccessToken / setRefreshToken
    API->>BE: 원 요청 재시도 (새 Bearer)
    BE-->>API: 200 정상 응답
    API-->>Comp: data
  else refresh 실패
    BE-->>API: 401 INVALID_REFRESH_TOKEN
    API->>API: clearTokens()
    API-->>Comp: throw ApiError(INVALID_REFRESH_TOKEN)
    Note right of Comp: 호출자 또는 글로벌 핸들러가 /login 이동
  end
  end
```

핵심 포인트:
- 컴포넌트는 **이 흐름을 몰라도 된다** — `baseFetch`가 모두 처리
- 재시도는 **1회만** (`retried` 플래그) — 무한 루프 방지
- refresh 실패 시 `ApiError`가 컴포넌트까지 올라옴 → 글로벌 에러 핸들러가 라우팅 결정

> 자세한 코드는 `API_CLIENT_GUIDE.md` §4 (`doFetch` 함수) 참조.

---

## 9. 라우팅 가드 — 페이지별 인증 정책

```mermaid
flowchart TD
  Req[페이지 진입 요청] --> Check{인증 정책}
  Check -->|Public| Render[그대로 렌더]
  Check -->|Optional Auth| RenderOpt[렌더 + 토큰 있으면 헤더 추가]
  Check -->|Protected| Auth{로그인됨?}
  Auth -->|예| Render2[렌더]
  Auth -->|아니오| Redirect["/login으로 리다이렉트 + redirect 쿼리"]
```

| 페이지 | 정책 | 비로그인 동작 |
|---|---|---|
| `/login` | Public | 그대로 |
| `/signup` | Public | 그대로 |
| `/` (챗봇) | Protected | `/login`으로 리다이렉트 |
| `/welfares` | Optional Auth | 그대로 (로그인 시 응답에 isBookmarked 자동 채움) |
| `/welfares/:id` | Optional Auth | 그대로 (북마크 버튼만 비활성) |
| `/bookmarks` | Protected | `/login`으로 리다이렉트 |
| `/me` | Protected | `/login`으로 리다이렉트 |
| `/me/profile` | Protected | `/login`으로 리다이렉트 |
| `/me/password` | Protected | `/login`으로 리다이렉트 |

구현은 `<ProtectedRoute>` 래퍼로 (`API_CLIENT_GUIDE.md` §8 참조).

리다이렉트 시 쿼리:
```
/login?redirect=%2Fbookmarks
```
로그인 성공 후 `redirect` 쿼리가 있으면 그쪽으로 이동 (UX 향상).

---

## 10. sessionStorage 라이프사이클

```mermaid
stateDiagram-v2
  [*] --> Empty
  Empty --> HasRT: 로그인/가입 성공
  HasRT --> HasBoth: AuthProvider가 access도 메모리에 설정
  HasBoth --> HasRT: 새로고침 (access 메모리 손실)
  HasRT --> HasBoth: AuthProvider useEffect → refresh 자동 성공
  HasBoth --> Empty: 로그아웃 / 탈퇴 / refresh 실패
  HasRT --> Empty: 탭 닫기
  HasBoth --> Empty: 탭 닫기
```

**저장 항목 3종**:

| 키 | 값 | 채우는 시점 | 비우는 시점 |
|---|---|---|---|
| `mozi_refresh_token` (sessionStorage) | refreshToken raw 문자열 | 로그인/가입/refresh 성공 시 | 로그아웃·탈퇴·refresh 실패·탭 닫기(자동) |
| `mozi_conversation_id` (sessionStorage) | UUID v4 | 챗봇 첫 메시지 응답 시 | "새 대화 시작"·로그아웃·탈퇴·탭 닫기(자동) |
| `accessToken` (메모리) | JWT | 로그인/가입/refresh 성공 시 | 로그아웃·탈퇴·새로고침(자동)·refresh 실패 |

핵심 규칙:
- **로그아웃 = 3개 모두 비우기** (`AuthContext.logout` 한 곳에서 일괄 처리)
- **탈퇴 = 로그아웃과 동일 + 이후 `/login` 이동**
- **탭 닫기 = 자동으로 모두 비워짐** (sessionStorage 특성 + 메모리 휘발)
- **새로고침 = accessToken만 사라짐 → refresh로 자동 복구**

---

## 11. 노년층 UX 흐름 체크포인트

각 시나리오에서 "사용자가 헷갈리지 않는가" 확인 포인트.

| 시나리오 | 체크포인트 |
|---|---|
| 가입 | "회원가입 → 로그인" 2단계가 아니라 가입 즉시 로그인됨을 명확히 안내 |
| 챗봇 | 응답 대기 중 로딩 인디케이터 명시적으로 보이는가 (8초까지 기다림) |
| 검색 | 필터 변경 시 결과가 바로 갱신됨이 시각적으로 보이는가 (Spinner 또는 결과 영역 깜빡임) |
| 북마크 | 토글 직후 버튼 라벨 변경 + 가벼운 토스트 — "저장됐다"는 신호 명확 |
| 비밀번호 변경 | "변경 후 본 기기를 포함한 모든 기기에서 재로그인 필요"가 사용자에게 충분히 전달되는가 (변경 직후 자동으로 /login으로 이동) |
| 탈퇴 | "어떤 데이터가 삭제되는지" 모달에 명시 |
| 토큰 만료 | 자동 복구 시 사용자에게 보이지 않게 — "갑자기 로딩이 길어졌다" 인식조차 안 들게 |

---

## 12. 참고 — 백엔드 USER_FLOW.md와의 차이

| 항목 | 백엔드 문서 | 본 문서 |
|---|---|---|
| 시점 | API 호출 시퀀스 (POST/GET 순서) | 화면 전환 + 사용자 액션 |
| 관심사 | DB 상태 변화 | 컴포넌트 상태 + sessionStorage |
| 대상 | 백엔드 개발자 | 프론트 개발자(나) |

두 문서가 모순될 경우 **API_SPEC.md(엔드포인트 사양)가 항상 우선**. 본 문서는 그 위에서 화면을 짤 때의 흐름만 정리.

---

## 13. 변경 이력

| 날짜 | 변경 내용 |
|---|---|
| 2026-05-15 | 초안 작성 — 7개 시나리오 mermaid + 라우팅 가드 + sessionStorage 라이프사이클 + UX 체크포인트 |
| 2026-05-17 | 페이지네이션 룰 정정 — 시나리오 D 의 "더 보기" 클릭 흐름을 페이지 번호 클릭 + 결과 교체로 갱신 |
| 2026-05-17 | 검색 상태 URL 동기화로 전환 — 시나리오 D 의 필터/페이지 흐름 및 "목록으로" 동작(navigate(-1) + state.from fallback) 명시. 새로고침/뒤로가기/링크 공유에도 검색 상태 보존 |
