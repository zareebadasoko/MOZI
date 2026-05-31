# SCREEN SPEC — 페이지 명세서

> 9개 페이지의 URL · 인증 · 호출 API · 컴포넌트 트리 · UI 요소 · 빈/에러 상태를 정리한다.
> 본 문서는 **페이지 구현 직전에 펴놓는 체크리스트** 역할이다.
> 백엔드 API 응답 스키마는 `../backend_project/docs/API_SPEC.md`를 직접 참조 — 본 문서는 베끼지 않는다.

---

## 1. 페이지 한눈에 보기

| # | 페이지 | URL | 인증 | 주요 호출 API | 비고 |
|---|---|---|---|---|---|
| 1 | 로그인 | `/login` | Public | `POST /api/auth/login` | redirect 쿼리 지원 |
| 2 | 회원가입 | `/signup` | Public | `POST /api/auth/signup` | 가입 즉시 자동 로그인 |
| 3 | 챗봇 메인(홈) | `/` | **Protected** | `POST /api/chat` | Core 페이지 |
| 4 | 복지 검색 | `/welfares` | Optional | `GET /api/welfares` + `GET /api/categories` | 필터 + 페이지 번호 |
| 5 | 복지 상세 | `/welfares/:id` | Optional | `GET /api/welfares/:id` + 북마크 | 출처별 상세 분기 |
| 6 | 북마크 목록 | `/bookmarks` | **Protected** | `GET /api/bookmarks` | 페이지 번호 |
| 7 | 마이페이지 | `/me` | **Protected** | `GET /api/users/me/profile` | 허브 페이지 |
| 8 | 프로필 수정 | `/me/profile` | **Protected** | `GET` + `PUT /api/users/me/profile` | 부분 갱신 |
| 9 | 비밀번호 변경 | `/me/password` | **Protected** | `PUT /api/users/me/password` | 본 기기 포함 모든 기기 재로그인 |

회원 탈퇴는 별도 페이지가 아닌 `/me` 안의 확인 모달로 처리 (시나리오 F 참조).

---

## 2. 공통 레이아웃·컴포넌트

본 명세에서 반복 등장하는 공통 요소.

### 2-1. AppShell (모든 페이지 공통 골격)
```
<AppShell>
  <AppHeader />          ← 로고 + 로그인 상태 의존 메뉴
  <main>
    {페이지별 컨텐츠}
  </main>
  <ToastContainer />     ← 화면 하단 토스트 영역 (Context)
</AppShell>
```

### 2-2. AppHeader — 로그인 상태별 변경
| 상태 | 헤더에 보이는 항목 |
|---|---|
| 비로그인 | 로고 / "로그인" 버튼 |
| 로그인 | 로고 / "마이페이지" 링크 / "로그아웃" 버튼 / (챗봇 페이지에서만) "새 대화 시작" |

### 2-3. ProtectedRoute (라우팅 가드)
- `bootstrapping` 중 → Spinner
- 비로그인 상태 → `/login?redirect=현재경로`로 이동
- 로그인 상태 → 자식 렌더

### 2-4. Spinner / LoadingOverlay
- 로딩 중 회전하는 원. 본문 영역을 가리고 표시
- 페이지 첫 로드는 페이지 전체, 부분 갱신은 영역 단위

### 2-5. ErrorBox
- 에러 메시지 + 재시도 버튼
- 챗봇 에러 / 네트워크 끊김 등에 사용

---

## 3. 페이지 #1 — 로그인 (`/login`)

### 3-1. 기본 정보
- **URL 패턴**: `/login`, `/login?redirect=%2Fbookmarks`
- **인증**: Public (로그인 상태에서 진입 시 `/` 로 리다이렉트)

### 3-2. 호출 API
| 시점 | API | 비고 |
|---|---|---|
| 폼 제출 | `POST /api/auth/login` | `skipAuth: true` |

### 3-3. 컴포넌트 트리
```
LoginPage
├── PageTitle ("로그인")
├── LoginForm
│   ├── Input (label="이메일", type="email")
│   ├── Input (label="비밀번호", type="password")
│   ├── Button (submit, "로그인")
│   └── (FormError 영역)
└── LinkRow
    ├── Link → /signup
    └── (선택) Link → /password-reset (MVP 미구현)
```

### 3-4. UI 요소
- 이메일 입력란 — placeholder "예: hong@example.com", `autoComplete="email"`
- 비밀번호 입력란 — `autoComplete="current-password"`
- 로그인 버튼 — primary, 큰 크기 (`h-14`)
- "회원가입" 링크
- 폼 상단/하단 형식 검증 안내문 ("이메일을 정확히 입력해주세요")

### 3-5. 빈 상태
- 해당 없음 (입력 폼만 있음)

### 3-6. 에러 상태
| errorCode | 표시 위치 | 메시지 |
|---|---|---|
| `VALIDATION_FAILED` | 각 입력란 옆 | `fields.email` / `fields.password` |
| `INVALID_CREDENTIALS` | 폼 상단 | "이메일 또는 비밀번호가 올바르지 않아요." |
| `NETWORK_ERROR` / `INTERNAL_ERROR` | 폼 하단 + 토스트 | "잠시 후 다시 시도해주세요." |

### 3-7. 성공 흐름
1. 토큰 저장 (`AuthContext.login(access, refresh)`)
2. `redirect` 쿼리가 있으면 그쪽으로, 없으면 `/`로 이동

### 3-8. 노년층 UX 체크
- [ ] 입력란 라벨이 placeholder가 아닌 명시적 텍스트
- [ ] 비밀번호 입력 시 "보기/숨기기" 토글 (선택, 권장)
- [ ] 로그인 버튼 높이 56px (`h-14`)
- [ ] 실패 시 "어떤 부분이 잘못됐는지" 명확히

---

## 4. 페이지 #2 — 회원가입 (`/signup`)

### 4-1. 기본 정보
- **URL 패턴**: `/signup`
- **인증**: Public

### 4-2. 호출 API
| 시점 | API |
|---|---|
| 폼 제출 | `POST /api/auth/signup` (`skipAuth: true`) |

### 4-3. 컴포넌트 트리
```
SignupPage
├── PageTitle ("회원가입")
├── SignupForm
│   ├── Input (이메일)
│   ├── Input (비밀번호, 8~72자)
│   ├── Input (비밀번호 확인) ← 클라이언트 측 동일성 검증
│   ├── Button (submit, "가입하기")
│   └── FormError 영역
└── (가입 성공 후 등장) ProfilePromptModal
    ├── Title ("프로필을 입력하면 추천이 더 정확해져요")
    ├── Button (primary, "지금 입력하기" → /me/profile)
    └── Button (secondary, "나중에" → /)
```

### 4-4. UI 요소
- 비밀번호 강도 안내(선택): "8자 이상, 영문/숫자 조합 권장"
- 가입 버튼 — primary, 큰 크기

### 4-5. 빈 상태
- 해당 없음

### 4-6. 에러 상태
| errorCode | 표시 |
|---|---|
| `VALIDATION_FAILED` | 각 입력란 옆 (예: 이메일 형식·비밀번호 길이) |
| `EMAIL_ALREADY_EXISTS` | 이메일 입력란 옆 "이미 가입된 이메일이에요." |

### 4-7. 성공 흐름
1. 응답 토큰 저장 → 자동 로그인 상태
2. **프로필 입력 권유 모달** 표시 (자동 띄움, 닫을 수 있음)
3. 모달에서 "지금 입력" → `/me/profile`, "나중에" → `/`

### 4-8. 노년층 UX 체크
- [ ] 비밀번호 확인 입력란이 별도로 있음
- [ ] 가입 성공 후 갑자기 다른 화면으로 가지 않음 (모달로 부드럽게)

---

## 5. 페이지 #3 — 챗봇 메인 (`/`)

> 본 서비스의 **Core 페이지**. 가장 공을 들여야 함.

### 5-1. 기본 정보
- **URL 패턴**: `/`
- **인증**: Protected

### 5-2. 호출 API
| 시점 | API |
|---|---|
| 메시지 전송 | `POST /api/chat` |
| (선택, 첫 진입) | `GET /api/users/me/profile` — 인사말에 사용자 정보 표시할 때만 |

### 5-3. 컴포넌트 트리
```
ChatPage
├── ChatHeader
│   ├── Title ("MOZI 도우미")
│   └── Button ("새 대화 시작") ← conversationId 있을 때만
├── ChatMessageList
│   ├── ChatBubble (assistant, 초기 인사말)
│   ├── ChatBubble (user, 입력한 메시지)
│   ├── ChatBubble (assistant, 답변 텍스트)
│   │   └── RecommendedWelfares (welfares.length > 0일 때만)
│   │        ├── WelfareCard ...
│   │        └── ...
│   └── (Spinner — 응답 대기 중)
├── (에러 시) RetryBox
└── ChatInput
    ├── Textarea (자동 높이 조절, 다중 라인)
    └── Button ("전송")
```

### 5-4. UI 요소
- 메시지 영역: 사용자 메시지는 오른쪽 정렬, 챗봇은 왼쪽 정렬, 둘 다 카드 형태
- 입력란: 다중 라인 가능. Enter = 전송, Shift+Enter = 줄바꿈
- 한글 IME 조합 중 Enter 무시 (`e.isComposing === false`일 때만 전송)
- 응답 대기 중: 입력 버튼 disabled + 챗봇 영역에 Spinner
- 추천 복지 카드 클릭 → `/welfares/:id` 새 페이지 (뒤로가기 시 챗봇 상태 보존)
- "새 대화 시작" 버튼 → 확인 후 conversationId·메시지 리스트 초기화

### 5-5. ASCII 와이어프레임
```
┌────────────────────────────────────┐
│ MOZI 도우미        [새 대화 시작] │
├────────────────────────────────────┤
│                                    │
│  [챗봇]                            │
│  안녕하세요. 어떤 복지가 필요    │
│  하신가요?                         │
│                                    │
│                       [사용자]    │
│         병원비 도움이 필요해요    │
│                                    │
│  [챗봇]                            │
│  병원비 부담을 덜어주는 복지를   │
│  추천드려요...                     │
│  ┌──────────────────────────┐     │
│  │ [중앙부처]               │     │
│  │ 의료급여 지원            │     │
│  │ 의료비를 지원하는...     │     │
│  └──────────────────────────┘     │
│  ┌──────────────────────────┐     │
│  │ [지자체]                 │     │
│  │ ... 등                   │     │
│  └──────────────────────────┘     │
│                                    │
├────────────────────────────────────┤
│ ┌─────────────────────┐  [전송]  │
│ │ 메시지를 입력하세요 │          │
│ └─────────────────────┘          │
└────────────────────────────────────┘
```

### 5-6. 빈 상태
- 메시지 리스트 비어있음 → 챗봇의 초기 안내문 1개 자동 표시: "안녕하세요. 어떤 복지가 필요하신가요?"
- `welfares = []` (잡담 응답) → 답변 텍스트만, 카드 영역은 렌더링 X

### 5-7. 에러 상태
| errorCode | 표시 위치 | 동작 |
|---|---|---|
| `CHATBOT_TIMEOUT` | 마지막 메시지 아래 RetryBox | "지금은 추천이 어려워요. 다시 시도하시겠어요?" + 재시도 버튼 (같은 메시지 재전송) |
| `CHATBOT_UNAVAILABLE` | 마지막 메시지 아래 | "챗봇 서버가 일시적으로 응답하지 않아요. 잠시 후 다시 시도해주세요." |
| `CHATBOT_INVALID_RESPONSE` | 마지막 메시지 아래 | "응답에 문제가 있어요. 다시 시도해주세요." |
| `VALIDATION_FAILED` (드물게, 빈 메시지) | 입력란 옆 | "메시지를 입력해주세요." |
| `NETWORK_ERROR` | 토스트 + 입력 재활성 | "네트워크에 연결할 수 없어요." |

### 5-8. 노년층 UX 체크
- [ ] 메시지 폰트 `text-senior-base` (18px) 이상
- [ ] 응답 대기 시 Spinner와 함께 "답변을 준비 중이에요" 텍스트 (8초까지)
- [ ] 전송 버튼은 입력란 옆 고정 (스크롤해도 보임)
- [ ] "새 대화 시작" 클릭 시 확인 모달 ("지금 대화를 끝낼까요?")

---

## 6. 페이지 #4 — 복지 검색 (`/welfares`)

### 6-1. 기본 정보
- **URL 패턴**: `/welfares`
- **인증**: Optional
- **쿼리 파라미터**: `keyword`, `category`, `sido`, `sigungu`, `welfareType`, `page` 가 URL 에 동기화된다. 상세 페이지에서 "목록으로" 또는 브라우저 뒤로가기 시 검색 조건과 페이지 번호가 그대로 복원되고, 새로고침/링크 공유에도 보존된다. 빈 값과 `page=0` 은 URL 에 적지 않아 주소를 깨끗하게 유지.

### 6-2. 호출 API
| 시점 | API |
|---|---|
| 마운트 / 필터 변경 / 페이지 번호 클릭 | `GET /api/welfares` |
| 마운트 (카테고리 옵션) | `GET /api/categories?type=THEME` |

### 6-3. 컴포넌트 트리
```
WelfareSearchPage
├── PageTitle ("복지 찾기")
├── WelfareFilterBar
│   ├── Input (keyword)
│   ├── Select (category — 옵션은 fetchCategories)
│   ├── Input (region)
│   ├── ChipGroup (welfareType: CENTRAL/LOCAL/PRIVATE/SEOUL)
│   └── Button ("검색")
├── ResultSummary ("총 N건")
├── WelfareCardList
│   ├── WelfareCard ...
│   └── ...
├── (totalPages > 1) Pagination (이전/다음 + 최대 5개 번호 chip)
└── (empty) EmptyState
```

### 6-4. UI 요소
- 필터: 모바일에서는 접힘/펼침 가능. 데스크탑에서는 상단 고정
- 카드: 출처 뱃지 + 제목 + 요약 + 카테고리 칩 + 북마크 버튼 (로그인 시만)
- 페이지네이터: 화면 가운데 정렬. 현재 페이지 강조(`bg-brand`). 페이지 변경 시 부드럽게 상단 스크롤

### 6-5. 빈 상태
- 첫 진입 시 결과 없음:
  ```
  ┌──────────────────────────┐
  │  🔍                      │
  │  조건에 맞는 복지가      │
  │  없어요.                 │
  │  다른 키워드로 검색해    │
  │  보시겠어요?             │
  │  [필터 초기화]          │
  └──────────────────────────┘
  ```
- 검색 후 결과 0건: 동일 EmptyState 표시

### 6-6. 에러 상태
| errorCode | 표시 |
|---|---|
| `VALIDATION_FAILED` | 필터 입력란 옆 (예: 잘못된 카테고리 코드) |
| `NETWORK_ERROR` / `INTERNAL_ERROR` | 페이지 본문에 ErrorBox + 재시도 버튼 |

### 6-7. 노년층 UX 체크
- [ ] 필터 항목별 라벨 명시 (드롭다운만 X)
- [ ] 페이지 번호/이전/다음 버튼이 모두 h-12(48px) 이상
- [ ] 결과 카드 사이 간격 충분 (`gap-4` 이상)

> 2026-05-17 백엔드 변경으로 `applyMyProfile` 자동 필터 토글은 폐기됨. 본인 맞춤 추천은 `POST /api/chat`(챗봇 페이지)에서 제공.

---

## 7. 페이지 #5 — 복지 상세 (`/welfares/:id`)

### 7-1. 기본 정보
- **URL 패턴**: `/welfares/WLF00001234`
- **인증**: Optional

### 7-2. 호출 API
| 시점 | API |
|---|---|
| 마운트 | `GET /api/welfares/:id` |
| "저장하기" 클릭 | `POST /api/bookmarks/:id` |
| "저장됨" 클릭 | `DELETE /api/bookmarks/:id` |

### 7-3. 컴포넌트 트리
```
WelfareDetailPage
├── BackLink ("← 목록으로")
├── WelfareDetailHeader
│   ├── Badge (출처: 중앙부처/지자체/민간/서울)
│   ├── Title
│   ├── Summary
│   └── BookmarkButton (로그인 시 활성, 비로그인 시 비활성+안내)
├── CategoryChips
├── WelfareDetailBody
│   ├── Section ("지원 대상")
│   ├── Section ("신청 방법")
│   ├── Section ("담당 기관")
│   └── (출처별 분기) — 아래 §7-4 참조
└── ExternalLinkButton ("자세히 보기" → 외부 detailUrl)
```

### 7-4. 출처별 추가 섹션
| welfareType | 추가 표시 |
|---|---|
| CENTRAL | `centralDetail.supportYear` / `supportType` / `selectionCriteria` 등 |
| LOCAL | `localDetail.regionName` / `supportYear` / `contact` 등 |
| PRIVATE | `privateDetail.startDate ~ endDate` / `requiredDocuments` / `contactEmail` 등 |
| SEOUL | `seoulDetail.supportType` / `detailContent` / `requiredDocuments` 등 |

> 정확한 필드 목록은 `../backend_project/docs/API_SPEC.md`의 WelfareDetailDto 참조.

### 7-5. UI 요소
- 출처 뱃지 색상으로 시각적 구분 (`brand-subtle` 톤)
- "자세히 보기" 외부 링크는 새 탭으로 (`target="_blank" rel="noopener"`)
- 모바일에서는 모든 섹션이 세로 흐름

### 7-6. 빈 상태
- 비로그인 + 북마크 클릭 → 인라인 안내 박스: "저장하려면 로그인이 필요해요. [로그인하기]"

### 7-7. 에러 상태
| errorCode | 표시 |
|---|---|
| `WELFARE_NOT_FOUND` | 페이지 전체 NotFoundView ("복지 정보를 찾을 수 없어요. [목록으로]") |
| `BOOKMARK_NOT_FOUND` (삭제 시) | 무시 가능 (이미 삭제된 상태로 UI 갱신) |
| `NETWORK_ERROR` | ErrorBox + 재시도 |

### 7-8. 노년층 UX 체크
- [ ] 핵심 정보(지원 대상/신청 방법)가 폴드 위에 보이는가
- [ ] 외부 링크는 "복지로/서울복지포털로 이동합니다" 안내 텍스트와 함께
- [ ] 북마크 버튼 라벨이 상태에 따라 명확히 변함 ("저장하기" ↔ "저장됨")

---

## 8. 페이지 #6 — 북마크 목록 (`/bookmarks`)

### 8-1. 기본 정보
- **URL 패턴**: `/bookmarks`
- **인증**: Protected

### 8-2. 호출 API
| 시점 | API |
|---|---|
| 마운트 / 페이지 번호 클릭 | `GET /api/bookmarks?page=&size=10` |
| 카드의 "저장됨" 토글 | `DELETE /api/bookmarks/:id` |

### 8-3. 컴포넌트 트리
```
BookmarksPage
├── PageTitle ("저장한 복지")
├── ResultSummary ("총 N건")
├── WelfareCardList (재사용 — 검색 페이지와 동일 컴포넌트)
│   └── WelfareCard ... (모든 항목 isBookmarked=true)
├── (totalPages > 1) Pagination (검색 페이지와 동일 공통 컴포넌트)
└── (empty) EmptyState
```

### 8-4. UI 요소
- 모든 카드는 `isBookmarked=true` 고정 표시
- 카드의 북마크 버튼 클릭 시 → 즉시 리스트에서 제거 (낙관적 업데이트) → DELETE 실패 시 복구
- 검색 페이지의 카드와 동일 컴포넌트 재사용 (코드 중복 X)

### 8-5. 빈 상태
```
┌──────────────────────────┐
│  🔖                      │
│  아직 저장한 복지가      │
│  없어요.                 │
│  복지 찾기에서 마음에    │
│  드는 복지를 저장해      │
│  보세요.                 │
│  [복지 찾기]            │
└──────────────────────────┘
```
"복지 찾기" 버튼 → `/welfares`

### 8-6. 에러 상태
| errorCode | 표시 |
|---|---|
| `NETWORK_ERROR` | ErrorBox + 재시도 |
| (없을 가능성) `UNAUTHORIZED` | ProtectedRoute가 막아 발생 안 함 |

### 8-7. 노년층 UX 체크
- [ ] 정렬 기준 안내 ("최근 저장한 순으로 보여드려요")
- [ ] 북마크 해제 시 살짝 fade-out 애니메이션 (선택)
- [ ] 페이지네이터가 카드 리스트 바로 아래에 배치 (페이지 1개일 땐 자동 숨김)

---

## 9. 페이지 #7 — 마이페이지 (`/me`)

### 9-1. 기본 정보
- **URL 패턴**: `/me`
- **인증**: Protected

### 9-2. 호출 API
| 시점 | API |
|---|---|
| 마운트 | `GET /api/users/me/profile` |
| 탈퇴 확인 | `DELETE /api/users/me` |
| 로그아웃 클릭 | `POST /api/auth/logout` |

### 9-3. 컴포넌트 트리
```
MyPage
├── PageTitle ("마이페이지")
├── (isCompleted=false) ProfilePromptCard
│   └── "프로필을 입력하면 추천이 더 정확해져요" + "지금 입력" 버튼
├── MenuList
│   ├── MenuItem ("프로필 수정" → /me/profile)
│   ├── MenuItem ("비밀번호 변경" → /me/password)
│   ├── MenuItem ("저장한 복지" → /bookmarks)
│   ├── MenuItem ("로그아웃" — 클릭 시 logout 호출)
│   └── MenuItem ("회원 탈퇴" — 클릭 시 WithdrawModal 열림, 빨간 텍스트)
└── (open) WithdrawModal
    ├── Title ("정말 탈퇴하시겠어요?")
    ├── Description ("저장한 복지·프로필·대화 기록이 모두 삭제됩니다. 이 작업은 되돌릴 수 없어요.")
    ├── Button ("취소", secondary)
    └── Button ("탈퇴할게요", danger, large)
```

### 9-4. UI 요소
- 각 메뉴 항목은 카드 형태로 큰 클릭 영역
- 회원 탈퇴 메뉴는 시각적으로 분리 (다른 메뉴와 간격 크게 + 옅은 빨간 톤)
- 로그아웃은 즉시 동작 (확인 모달 없이도 OK — 토큰만 클리어해도 재로그인 쉬움)

### 9-5. 빈 상태
- 해당 없음

### 9-6. 에러 상태
| errorCode | 표시 |
|---|---|
| `USER_NOT_FOUND` (극히 드묾) | 로그아웃 처리 후 로그인 페이지로 |

### 9-7. 노년층 UX 체크
- [ ] 메뉴 항목 폰트 22px (`text-senior-lg`) — 한눈에 보이게
- [ ] 메뉴 항목 높이 64~80px (`h-16` ~ `h-20`)
- [ ] 탈퇴 모달은 핵심 행동(취소)을 primary로, 위험 행동(탈퇴)을 danger로

---

## 10. 페이지 #8 — 프로필 수정 (`/me/profile`)

> 2026-05-17 백엔드 USER_PROFILE_REDESIGN 반영. 자세한 스키마는 `../../backend_project/docs/FRONTEND_MIGRATION_NOTES.md` 참조.

### 10-1. 기본 정보
- **URL 패턴**: `/me/profile`
- **인증**: Protected

### 10-2. 호출 API
| 시점 | API |
|---|---|
| 마운트 | `GET /api/users/me/profile` |
| 마운트 (시도/시군구 옵션 로드) | `GET /api/regions` — 페이지 진입 시 1회. 응답 캐싱(`src/api/region.js` 모듈 변수 또는 Context). Phase 4 산출물. |
| 저장 클릭 | `PUT /api/users/me/profile` (변경된 필드만 body에) |

### 10-3. 컴포넌트 트리
```
ProfilePage
├── PageTitle ("내 정보 수정")
├── HelpText ("입력하실수록 추천이 더 정확해집니다.")
├── ProfileForm
│   ├── Select (label="시도", options=GET /api/regions 응답 → [{value: sidoCode, label: sidoName}])
│   ├── Select (label="시군구", options=선택된 시도의 sigungus, disabled=시도 미선택 시)
│   │   └── cascading: 시도 변경 시 setSelectedSigungu("") 로 초기화. 세종(시군구 없음)은 select 비활성 + "시군구 없음" 안내
│   ├── Select (label="소득 유형", options=INCOME_TYPE_LABEL 5종)
│   │   └── NATIONAL_BASIC_LIVING "기초생활수급자" / NEAR_POVERTY "차상위계층" / BASIC_PENSION "기초연금수급자" / GENERAL "해당 없음" / UNKNOWN "잘 모르겠어요"
│   ├── Select (label="가구 형태", options=HOUSEHOLD_TYPE_LABEL 5종)
│   │   └── LIVING_ALONE "혼자 살아요 (독거)" / COUPLE "배우자와 둘이 살아요" / WITH_CHILDREN "자녀와 함께 살아요" / GRANDPARENT_GRANDCHILD "손주를 키우고 있어요 (조손)" / OTHER "그 외"
│   ├── ToggleGroup (boolean 4종)
│   │   ├── Toggle ("장애 여부", key=isDisabled)
│   │   ├── Toggle ("다자녀 가정", key=isMultiChild)
│   │   ├── Toggle ("다문화·탈북민", key=isMulticulturalNorthDefector)
│   │   └── Toggle ("보훈대상자", key=isVeteran)
│   ├── Button (submit, primary, "저장")
│   └── Button (secondary, "취소", → /me)
```

> 옛 명세의 자유 텍스트 Input("소득 유형"/"가구 형태")과 토글 6종 중 "독거 여부"·"한부모·조손가정"은 제거되었다.
> 독거/조손은 `householdType` enum (`LIVING_ALONE` / `GRANDPARENT_GRANDCHILD`) 이 흡수하므로 중복 입력 X.
> 백엔드 USER 테이블에서 birthDate/gender는 회원가입 시점에 받지 않으며 본 폼의 범위가 아니다 (FRONTEND_MIGRATION_NOTES.md §1 참조).

### 10-4. UI 요소
- 각 토글은 ON/OFF 명확히 보이도록 (체크박스보다는 슬라이드 토글 권장)
- 토글 옆 도움말 한 줄: 예 "장애 여부 — 등록된 장애가 있으신 경우 켜주세요"
- 시도/시군구 select 옆에 도움말 한 줄: "거주하시는 지역을 선택해주세요"
- 저장 후 토스트 "저장되었어요" + `/me`로 자동 이동(또는 머무름 선택)

### 10-5. 빈 상태
- 첫 진입 시 `isCompleted=false` → 모든 select가 placeholder("선택해주세요") 상태로 표시

### 10-6. 에러 상태
| errorCode | HTTP | 표시 / 처리 |
|---|---|---|
| `VALIDATION_FAILED` | 400 | 각 입력란 옆 (`fields` 객체를 select에 매핑) |
| `INVALID_REGION_CODE` | 400 | 토스트 + 시군구 select 초기화. 메시지는 백엔드가 케이스별로 다르게 줌:<br>· "시군구만 입력할 수 없어요. 시도부터 선택해주세요." → 시도 select 강조<br>· "존재하지 않는 시도 코드예요." → `GET /api/regions` 재호출해 캐시 갱신<br>· "입력한 시도에 속하지 않는 시군구예요." → 시군구 값만 초기화 후 재선택 안내 |
| `NETWORK_ERROR` | 0 | 폼 상단 + 토스트 |

### 10-7. 노년층 UX 체크
- [ ] 입력 항목이 너무 많지 않게 시각적 그룹화 (거주 지역 / 소득·가구 / 추가 상황 3그룹)
- [ ] "저장하지 않고 나가시겠어요?" 확인 (변경 사항 있을 때만)
- [ ] 시도/시군구 드롭다운은 옵션이 많으므로 검색 가능 (예: 시군구 select 안에 인풋)
- [ ] 세종처럼 시군구가 없는 시도에서는 "시군구 선택 없이 저장 가능" 안내

---

## 11. 페이지 #9 — 비밀번호 변경 (`/me/password`)

### 11-1. 기본 정보
- **URL 패턴**: `/me/password`
- **인증**: Protected

### 11-2. 호출 API
| 시점 | API |
|---|---|
| 폼 제출 | `PUT /api/users/me/password` |

### 11-3. 컴포넌트 트리
```
PasswordPage
├── PageTitle ("비밀번호 변경")
├── NoticeBox ("변경 후 본 기기를 포함한 모든 기기에서 다시 로그인해야 해요.")
├── PasswordForm
│   ├── Input (label="현재 비밀번호", type="password")
│   ├── Input (label="새 비밀번호", type="password", helpText="8~72자")
│   ├── Input (label="새 비밀번호 확인", type="password")
│   ├── Button (submit, primary, "변경하기")
│   └── Button (secondary, "취소", → /me)
```

### 11-4. UI 요소
- "변경 후 모든 기기에서 다시 로그인 필요" 안내문은 페이지 상단에 항상 표시 (본 기기 포함)
- 새 비밀번호 ≠ 현재 비밀번호 클라이언트 측 사전 검증
- 새 비밀번호 === 새 비밀번호 확인 클라이언트 측 사전 검증

### 11-5. 빈 상태
- 해당 없음

### 11-6. 에러 상태
| errorCode | 표시 |
|---|---|
| `VALIDATION_FAILED` | 각 입력란 옆 (예: 길이 미달) |
| `PASSWORD_MISMATCH` | 현재 비밀번호 입력란 옆 ("현재 비밀번호가 일치하지 않아요.") |
| `SAME_PASSWORD` | 새 비밀번호 입력란 옆 ("현재 비밀번호와 다른 값을 입력해주세요.") |

### 11-7. 성공 흐름
백엔드 응답은 `{ "changed": true }`만 포함하고 **새 토큰을 발급하지 않는다**. 또한 백엔드가 본 user의 모든 refreshToken을 삭제하므로 본 기기까지 재로그인이 필요하다.

1. `clearTokens()` + `AuthContext.logout()` 호출
2. 토스트: **"비밀번호가 변경되었어요. 다시 로그인해주세요."**
3. `/login`으로 이동

### 11-8. 노년층 UX 체크
- [ ] 현재 비밀번호 입력란이 가장 위 (논리 순서)
- [ ] 입력 시 비밀번호 보기/숨기기 토글 (오타 방지)
- [ ] 새 비밀번호 입력 시 실시간 도움말 ("8자 이상 필요해요" 등)

---

## 12. 공통 에러 페이지 & 보조 라우트

### 12-1. NotFound (`*`)
- 어떤 라우트에도 매칭되지 않을 때
- 메시지: "페이지를 찾을 수 없어요. 주소를 다시 확인해주세요."
- "홈으로" 버튼

### 12-2. 글로벌 토스트 영역
- `AppShell` 내 고정 위치
- 동시에 여러 토스트가 뜨면 세로로 쌓임 (최대 3개, 초과 시 가장 오래된 것 제거)

---

## 13. 페이지 구현 우선순위 (EXECUTION_PLAN.md 동기화 예고)

EXECUTION_PLAN.md의 Phase별 순서:

| Phase | 다루는 페이지 |
|---|---|
| Phase 2 | LoginPage, SignupPage |
| Phase 3 | AppShell, AppHeader, ProtectedRoute, 공통 컴포넌트 (Button/Input/Card/Modal/Toast) |
| Phase 4 | WelfareSearchPage, WelfareDetailPage, BookmarksPage, MyPage, ProfilePage, PasswordPage |
| Phase 5 | ChatPage (가장 복잡, 따로 분리) |
| Phase 6 | NotFoundPage + 모든 페이지 빈/에러 상태 점검 |

---

## 14. 변경 이력

| 날짜 | 변경 내용 |
|---|---|
| 2026-05-15 | 초안 작성 — 9개 페이지 + 공통 레이아웃 + 우선순위 매핑 |
| 2026-05-17 | 백엔드 USER_PROFILE_REDESIGN 반영 — §10 ProfilePage 컴포넌트 트리 재작성(자유 텍스트 Input → Select, 토글 6종 → 4종, sido/sigungu cascading, GET /api/regions 호출 추가, INVALID_REGION_CODE 에러 행 신설). §6 WelfareFilterBar에서 applyMyProfile 토글 제거 + 노년층 UX 안내 정정 |
| 2026-05-17 | 페이지네이션 룰 정정 — §6/§8 의 "더 보기" 버튼 → Pagination(이전/다음 + 번호 chip) 공통 컴포넌트로 일괄 교체 |
| 2026-05-17 | 검색 상태 URL 동기화로 전환 — §6-1 "MVP 는 in-memory" 정정. 상세 페이지 "목록으로" 가 navigate(-1) + state.from fallback 으로 동작해 이전 검색 조건/페이지 복원 |
