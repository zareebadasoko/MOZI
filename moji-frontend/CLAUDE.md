# MOZI Frontend Project

## 1. 프로젝트 개요

디지털 조작에 익숙하지 않은 노년층(65세 이상)이 대화형 챗봇과 상황별 필터링으로 맞춤형 복지 혜택을 쉽게 찾을 수 있도록 지원하는 **웹 프론트엔드**.
백엔드(`backend_project`)가 이미 제공하는 REST API 16종에 충실히 맞추는 **API-First 클라이언트**다. 자체 비즈니스 로직(예: 직접 LLM 호출, 자체 DB 보관)은 두지 않는다.

### 프로젝트 컨텍스트
- **개발 규모**: 1인 풀스택. 백엔드를 먼저 완성하고(Phase 6 완료) 이어서 프론트 진행
- **작성자(사용자)의 프론트 숙련도**: **거의 처음**. React/Vite/Tailwind/fetch/react-router 모두 학습하면서 진행
  - → 따라서 코드/문서는 친절하게: 흐름 설명·주석·"왜 이렇게 하는지" 까지
- **목적**: 졸업 프로젝트 + 풀스택 시연
- **배포 방식**: 시연용 임시 배포 (AWS, 실제 사용자 운영 X)
- **유저 데이터**: 실제 유저 없음. 백엔드의 시드(가짜 사용자)로 시연

### 외부 시스템 연동
- **백엔드 API 서버**: `http://localhost:8080/api/*` (개발 환경)
  - 인증: JWT (`Authorization: Bearer ...`)
  - 응답 포맷: `ApiResponse<T>` 통일 래퍼
- **챗봇 LLM 서버**: 별도 팀이 개발 중이며, **프론트는 직접 호출하지 않는다**.
  - 백엔드의 `/api/chat` 엔드포인트만 호출 → 백엔드가 챗봇 서버와 통신
- 자세한 API 명세는 `../backend_project/docs/API_SPEC.md` 참조

---

## 2. 자주 쓰는 명령어 (Vite + npm 기준)

> npm은 Node.js를 설치하면 함께 설치되는 패키지 관리자다. (pnpm/yarn으로 바꿔도 무방하지만 본 프로젝트는 **npm으로 통일**)

| 목적 | 명령어 |
|---|---|
| 개발 서버 실행 (http://localhost:5173) | `npm run dev` |
| 프로덕션 빌드 (`dist/` 산출) | `npm run build` |
| 빌드 결과 로컬 미리보기 | `npm run preview` |
| 린트 검사 | `npm run lint` |
| 의존성 설치 (clone 직후, package.json 변경 후) | `npm install` |
| 새 라이브러리 추가 | `npm install <pkg>` (단, **사전 사용자 승인 필수**) |
| 의존성 트리 확인 | `npm ls` |

> Vite: 빠른 개발 서버 + 번들러. CRA(Create React App)의 후속 표준.

---

## 3. 폴더 구조 (Feature-Light 구조)

```
frontend_project/
├── public/                       # 정적 파일 (favicon 등)
├── src/
│   ├── main.jsx                  # 진입점 (ReactDOM.createRoot)
│   ├── App.jsx                   # 최상위 컴포넌트 (라우터 설정)
│   ├── index.css                 # Tailwind base + 글로벌 스타일
│   │
│   ├── pages/                    # 라우팅 단위 페이지 (URL과 1:1)
│   │   ├── LoginPage.jsx
│   │   ├── SignupPage.jsx
│   │   ├── ChatPage.jsx          # 홈 = 챗봇 메인
│   │   ├── WelfareSearchPage.jsx
│   │   ├── WelfareDetailPage.jsx
│   │   ├── BookmarksPage.jsx
│   │   └── MyPage.jsx            # 프로필/비밀번호/탈퇴 묶음
│   │
│   ├── components/               # 재사용 컴포넌트
│   │   ├── common/               # Button, Input, Card, Modal, Toast 등
│   │   ├── welfare/              # WelfareCard, WelfareFilterBar 등
│   │   └── chat/                 # ChatBubble, ChatInput 등
│   │
│   ├── hooks/                    # 커스텀 훅 (useAuth, useApi 등)
│   ├── contexts/                 # React Context Provider (AuthContext 등)
│   ├── api/                      # API 클라이언트 (도메인별 함수)
│   │   ├── client.js             # baseFetch + 자동 재시도 wrapper
│   │   ├── auth.js
│   │   ├── user.js
│   │   ├── welfare.js
│   │   ├── bookmark.js
│   │   ├── category.js
│   │   └── chat.js
│   ├── utils/                    # 순수 함수 유틸 (date 포맷, 토큰 저장소 등)
│   └── styles/                   # tailwind.config.js 토큰 + 글로벌 css
│
├── docs/                         # 프로젝트 문서 모음
│   ├── PRD.md
│   ├── EXECUTION_PLAN.md
│   ├── USER_FLOW.md
│   ├── SCREEN_SPEC.md
│   ├── PROJECT_SETUP.md
│   ├── STYLING_GUIDE.md
│   ├── COMPONENT_ARCHITECTURE.md
│   └── API_CLIENT_GUIDE.md
│
├── CLAUDE.md                     # ← 이 파일 (Claude Code 작업 원칙, 프로젝트 루트에 위치)
├── .env.local                    # gitignore. VITE_API_BASE_URL 등
├── .gitignore
├── package.json
├── vite.config.js
├── tailwind.config.js
└── postcss.config.js
```

> CLAUDE.md는 Claude Code가 프로젝트 컨텍스트로 자동 로드하는 파일이므로 **반드시 프로젝트 루트**에 둔다 (`docs/` 안 X). 백엔드의 `backend_project/CLAUDE.md`와 동일 위치 정책.
>
> 자세한 디렉토리 의도와 분리 기준은 `docs/COMPONENT_ARCHITECTURE.md` 참조.

---

## 4. 코딩 규칙 (Coding Conventions)

### 4-1. 컴포넌트 작성 규칙
- **함수형 컴포넌트만 사용** (class 컴포넌트 금지)
- 파일·컴포넌트명 **PascalCase** (`WelfareCard.jsx`). 페이지는 `~Page.jsx` 접미사
- props는 객체 구조분해로 받기:
  ```jsx
  function WelfareCard({ welfare, onBookmarkToggle }) { ... }
  ```
- 한 파일에 컴포넌트 하나만 export (default export 권장)
- props·반환값에 JSDoc 작성 (자세한 형식은 §4-6 참조)
- 100줄 넘으면 분리 검토 (`docs/COMPONENT_ARCHITECTURE.md` §4 참조)

### 4-2. 상태 관리 규칙
> 작은 상태부터 시작. 무거운 라이브러리는 정말 필요할 때만.

- **로컬 상태**: `useState` / `useReducer`
- **여러 컴포넌트 공유**: `React Context` (예: 로그인 사용자, 토큰)
- **복잡도 신호 발생 시 zustand 도입 검토**:
  - Context Provider가 5개 이상 중첩되거나
  - prop drilling이 3단계 이상 깊어지거나
  - 같은 데이터를 여러 페이지에서 동시에 갱신해야 할 때
- zustand 도입 결정은 **사용자 사전 승인 후** (§5-3 의존성 관리)
- 서버 데이터 캐싱(SWR / tanstack-query)은 도입하지 않는다 — 본 프로젝트 규모에 과함

### 4-3. API 호출 규칙
- **fetch만 사용** (axios 금지). 브라우저 내장 = 의존성 0
- 컴포넌트에서 `fetch()` 직접 호출 금지. 항상 `src/api/*` 함수만 사용
- 모든 호출은 `src/api/client.js`의 wrapper를 거친다:
  - Authorization 헤더 자동 부착
  - `TOKEN_EXPIRED` 응답 시 자동으로 `/api/auth/refresh` → 원 요청 재시도
- 응답 분기는 `errorCode`로 판단. `status === "ERROR"`인 경우 다음 규칙:
  - `VALIDATION_FAILED` → `fields` 객체를 폼 입력란에 매핑
  - `CHATBOT_*` → 재시도 안내 메시지
  - `INVALID_REFRESH_TOKEN` → 토큰 클리어 → 로그인 페이지
  - 기타 → `message` 그대로 토스트
- 자세한 패턴·코드 예시는 `docs/API_CLIENT_GUIDE.md` 참조

### 4-4. 스타일링 규칙
- **Tailwind CSS만 사용**. inline `style={{}}` 금지(예외: 동적으로 계산되는 색·크기)
- 색·폰트·간격은 **`tailwind.config.js`의 디자인 토큰**만 사용
  - ❌ `text-[17px]` 처럼 임의값 금지
  - ✅ `text-senior-base` 같은 토큰 사용
- 노년층 UX 강제 규칙:
  - 본문 텍스트 ≥ `senior-base` (18px)
  - 인터랙티브 요소(버튼/링크/탭) 최소 높이 48px (`h-12`)
  - 색 대비 WCAG AA(4.5:1) 이상
- 자세한 토큰 목록·사용 예시는 `docs/STYLING_GUIDE.md` 참조

### 4-5. 토큰 정책 ⭐
> 보안과 사용성을 동시에 잡기 위한 결정값. **절대 변경 금지** (변경하려면 사용자 사전 승인)

| 토큰 | 저장 위치 | 이유 |
|---|---|---|
| `accessToken` | **메모리 (Context/모듈 변수)** | XSS 노출 면적 최소화. 새로고침 시 refresh로 즉시 복구 |
| `refreshToken` | **sessionStorage** | 새로고침 후 유지, 탭 닫으면 자동 삭제. localStorage보다 보안↑ |
| `conversationId` | **sessionStorage** | 페이지 이동 시 대화 유지, 탭 닫으면 새 대화 |

- **localStorage에 토큰 저장 금지** (§5-1)
- 로그아웃 / 비밀번호 변경 / 회원 탈퇴 시 → 두 토큰 + conversationId 모두 클리어
- 백엔드는 비밀번호 변경 시 서버 측 모든 refreshToken을 무효화하고 새 토큰을 발급하지 않는다 → **본 기기 포함 모든 기기에서 재로그인 필요**. 프론트는 변경 성공 시 즉시 `clearTokens()` + `/login` 이동

### 4-6. 작업 순서 (반드시 지킬 것)
백엔드와 동일 원칙:
1. `docs/PRD.md` / `docs/USER_FLOW.md` / `docs/SCREEN_SPEC.md` / 백엔드 `../backend_project/docs/API_SPEC.md`를 먼저 읽고 맥락 확보
2. **코드 작성 전 화면 구조와 호출 흐름을 먼저 제안하고 사용자 승인을 받는다**
3. 승인된 설계대로 단계별로 구현
4. 각 단계마다 검증(브라우저 수동 확인 또는 테스트) 후 다음 단계 진행
5. UI 변경은 반드시 `npm run dev`로 띄워 골든 패스 + 에러 케이스를 눌러보고 보고

### 4-7. 주석 정책 (학습 목적 강화) ⭐
> 1인 학습 프로젝트. 작성자(사용자)가 코드를 직접 이해할 수 있도록 주석을 적극 작성한다.

#### 1. 컴포넌트/함수/훅 위에는 JSDoc 필수
모든 export 컴포넌트, 커스텀 훅, API 함수, 핵심 헬퍼 위에 JSDoc 작성.

```javascript
/**
 * (한 줄 요약: 무엇을 하는 컴포넌트/함수인가)
 *
 * (2~3줄 상세 설명: 왜 필요한지, 어떤 동작 흐름인지)
 *
 * @param {Object} props
 * @param {WelfareSummary} props.welfare - 표시할 복지 카드 데이터
 * @param {(id: string) => void} props.onBookmarkToggle - 북마크 토글 핸들러
 * @returns {JSX.Element}
 */
function WelfareCard({ welfare, onBookmarkToggle }) { ... }
```

빠뜨리면 안 되는 항목:
- 함수/컴포넌트의 목적
- 각 props·인자의 의미
- 반환값의 의미
- 발생 가능한 에러 상황 (해당 시)

#### 2. 내부 코드는 핵심만 한 줄 주석
모든 줄에 주석 X. 다음 경우에만 `//` 주석:
- 비즈니스 규칙이 적용되는 부분 (예: "applyMyProfile=false면 가족 검색")
- 왜 이렇게 했는지 직관적이지 않은 코드 (예: useEffect의 의존성 배열 의도)
- 외부 시스템과 통신하는 지점
- 보안/토큰 관련 로직

```javascript
// TOKEN_EXPIRED 응답 시 1회만 재시도 (refresh 후)
if (json.errorCode === 'TOKEN_EXPIRED' && !retried) { ... }
```

#### 3. 하지 말 것
- ❌ 자명한 코드에 주석 (`// state 변수 선언`)
- ❌ 변수명만으로 알 수 있는 내용 반복
- ❌ 한국어/영어 혼용 → 주석은 **한국어로 통일**

#### 4. 파일 말미 요약
복잡한 컴포넌트/훅 파일 끝에 1~2문장 단일 주석:
```javascript
// 이 컴포넌트는 챗봇 페이지의 입력란을 담당하며, IME(한글 조합) 처리를 위해 keydown에서 isComposing을 체크한다.
```

---

## 5. 절대 하지 말 것 (Out of Scope & Restrictions)

### 5-1. 아키텍처/보안 위반 금지
- ❌ **localStorage에 토큰 저장 금지** — XSS 시 영구 노출. accessToken=메모리, refreshToken=sessionStorage 고정 (§4-5)
- ❌ **챗봇 서버 직접 호출 금지** — 항상 백엔드 `/api/chat`만 호출 (인증/로깅/Mock 전환 모두 백엔드 책임)
- ❌ **API 응답 스키마 임의 가공 금지** — 받은 그대로 컴포넌트로 흘려보내고, 가공이 필요하면 `src/utils/`에 함수로 분리
- ❌ **백엔드 docs 복제 금지** — `../../backend_project/docs/*` 상대경로로 링크만. (자세한 5종 목록은 §10 참조)

### 5-2. 과잉 개발 금지
- ❌ 지시받지 않은 기능을 선제적으로 개발하지 않는다
  - 예: 다크모드 전환, 다국어, PWA 설치 배너, 자동 로그인 기억하기, 푸시 알림 등
- ❌ TypeScript 도입 금지 (확정: JavaScript)
- ❌ SSR/SSG(Next.js 등) 도입 금지 (확정: Vite + CSR)

### 5-3. 의존성 관리
- ❌ `package.json`에 새 라이브러리 추가 시 **반드시 사전에 이유 명시 + 사용자 허락**
- ❌ 다음 라이브러리는 도입 금지(이미 결정됨):
  - `axios` (fetch로 충분)
  - `tanstack-query` / `swr` (캐싱이 본 프로젝트에 과함)
  - `typescript` (확정: JS)
  - `styled-components` / `emotion` (Tailwind로 통일)
  - `redux` / `mobx` (zustand만 검토)
- ✅ 도입 검토 가능한 것: `zustand`(상태가 복잡해질 때만), `clsx`(조건부 클래스가 많아질 때만)

### 5-4. 데이터/보안
- ❌ 가짜 데이터라도 비밀번호 등 민감 정보를 `console.log` 하지 않는다
- ❌ 토큰을 URL 쿼리·로그·에러 메시지에 노출 금지
- ❌ 운영 코드에 `console.log` 남발 금지 (개발 디버깅 후 반드시 정리)

### 5-5. 비밀 정보 보호
- ❌ CLAUDE.md, ./docs, src/** 등 깃 추적 대상 파일에 API Key/Secret/실 사용자 정보 게시 금지
- ❌ 환경 변수 값을 코드에 하드코딩 금지 (반드시 `import.meta.env.VITE_*` 사용)

---

## 6. 테스트 규칙 (MVP 가벼운 운용)

본 프로젝트는 1인 시연용이므로 자동화 테스트 비중은 낮게. **수동 브라우저 확인을 1차 검증**으로 한다.

- **수동 검증 (필수)**: UI 변경 후 반드시 `npm run dev`로 띄워 다음 시나리오 눌러본다.
  - 로그인 → 새로고침 → 자동 복구
  - 챗봇 첫 메시지 → 후속 → 새 대화
  - 검색 → 상세 → 북마크 토글
  - 비로그인 상태에서 보호 페이지 접근 시 리다이렉트
- **컴포넌트 단위 테스트 (선택)**: 핵심 유틸 함수(`utils/tokenStore.js`, `api/client.js`의 errorCode 분기)는 Vitest로 작성 검토
- **E2E 테스트**: 도입하지 않는다 (졸업 프로젝트 범위 외)
- **테스트 작성 시점**: Phase 6 마무리 단계에서 핵심 유틸만 추가

---

## 7. Git 규칙

- **커밋 메시지**: Conventional Commits 형식 (백엔드와 동일)
  - `feat:` 새 기능
  - `fix:` 버그 수정
  - `refactor:` 리팩토링
  - `docs:` 문서 수정
  - `style:` 코드 포맷팅(로직 변경 X)
  - `test:` 테스트
  - `chore:` 설정/빌드
- 예시: `feat(welfare): 검색 결과 카드 컴포넌트 구현`
- **브랜치**: `main`에 직접 푸시 금지. 작업은 `feature/*` 브랜치에서
- Claude Code가 커밋 메시지 생성 시 사용자가 검토 후 승인

---

## 8. 환경 변수 및 비밀 관리

- API URL, 외부 키 등은 `.env.local`에 작성하고 `import.meta.env.VITE_XXX`로 참조
- Vite는 **`VITE_` 접두사 변수만 클라이언트에 노출**한다 (그 외는 빌드에 포함되지 않음)
- `.gitignore`에 등록:
  - `.env.local`
  - `.env.*.local`
- `.env.example` 파일로 키 이름만 깃에 올린다 (값은 비움)

예시 `.env.local`:
```
VITE_API_BASE_URL=http://localhost:8080
```

---

## 9. AWS 배포 관련 (참고)

- 실제 배포는 **백엔드/프론트 완성 이후**에 학습 + 진행 예정
- 현 단계에서는 배포 코드(`amplify.yml`, `Dockerfile`, GitHub Actions)를 작성하지 않는다
- 단, **배포 가능한 형태로 설계**는 유지: 환경 변수 분리, 정적 산출물(`dist/`) 기반, 라우팅은 SPA fallback 가능 구조

---

## 10. 참고 문서

### 본 프로젝트 docs/
> 이 파일(`CLAUDE.md`)은 프로젝트 루트, 나머지 문서는 `docs/` 하위에 위치한다.

| 문서 | 경로 | 용도 |
|---|---|---|
| 제품 명세 (PRD) | `./docs/PRD.md` | 무엇을 만들 것인가 (프론트 관점) |
| 실행 계획 | `./docs/EXECUTION_PLAN.md` | 어떤 순서로 만들 것인가 |
| 사용자 흐름 | `./docs/USER_FLOW.md` | 화면 전환·시나리오 (mermaid) |
| 화면 명세 | `./docs/SCREEN_SPEC.md` | 페이지별 URL/API/컴포넌트/상태 |
| 첫 세팅 가이드 | `./docs/PROJECT_SETUP.md` | Vite 생성부터 첫 페이지까지 |
| 스타일링 가이드 | `./docs/STYLING_GUIDE.md` | Tailwind 디자인 토큰 + 노년층 UX |
| 컴포넌트 아키텍처 | `./docs/COMPONENT_ARCHITECTURE.md` | 파일 분리 원칙·폴더 구조 |
| API 클라이언트 가이드 | `./docs/API_CLIENT_GUIDE.md` | fetch wrapper, 토큰 갱신, 에러 분기 |

### 백엔드 docs (직접 링크만, 복제 금지) ⭐
| 문서 | 경로 | 한 줄 요약 |
|---|---|---|
| API 명세서 | `../backend_project/docs/API_SPEC.md` | 16개 엔드포인트 확정 명세 (메소드/URL/요청/응답) |
| 에러 코드 | `../backend_project/docs/ERROR_CODES.md` | 17개 errorCode + 사용자 친화 한글 메시지 |
| 프론트 연계 가이드 | `../backend_project/docs/FRONTEND_INTEGRATION_GUIDE.md` | 백엔드가 작성한 인증/흐름 가이드. 우리 `docs/API_CLIENT_GUIDE.md`의 보완 |
| 카테고리 코드맵 | `../backend_project/docs/CATEGORY_REFERENCE.md` | THEME 15종 + STATUS 7종 코드↔한글명 |
| 챗봇 계약서 | `../backend_project/docs/CHATBOT_API_CONTRACT.md` | 챗봇 서버↔백엔드 계약. 프론트는 직접 호출하지 않음 (참고용) |

### Swagger UI (백엔드 실행 시)
- http://localhost:8080/swagger-ui.html — 실제 호출/응답을 브라우저에서 시험 가능

---

## 11. 외부 라이브러리·도구 한 줄 풀이

> 프론트 처음이라면 헷갈리는 이름들. 한 번씩 짚고 가기.

- **React**: UI를 컴포넌트(=함수)로 쪼개서 상태가 바뀌면 화면을 자동 갱신해주는 라이브러리
- **Vite**: 개발 서버 + 번들러. CRA(Create React App)의 후속 표준. 매우 빠름
- **JSX**: 자바스크립트 안에 HTML처럼 마크업을 쓸 수 있게 해주는 문법 확장
- **fetch**: 브라우저 내장 HTTP 호출 함수. axios 대체로 충분
- **react-router-dom**: 클라이언트 사이드 라우팅. URL ↔ 컴포넌트 매핑
- **Tailwind CSS**: 미리 정의된 클래스(`p-4`, `text-lg`)로 스타일링하는 유틸리티 우선 CSS 프레임워크
- **PostCSS**: CSS를 변환하는 도구(Tailwind가 내부적으로 사용). 직접 만질 일은 없음
- **ESLint**: 코드 정적 분석 도구(버그·스타일 위반 감지)
- **Prettier**: 코드 포맷터(들여쓰기·따옴표 등 자동 정리)
- **JSDoc**: JavaScript에 타입·문서 주석을 다는 표준 양식 (`/** ... */`)
- **Context API**: React 내장 전역 상태 메커니즘. 트리 어디서나 값 접근 가능
- **Zustand**: 가벼운 전역 상태관리 라이브러리(필요 시 도입 검토)

---

## 12. 변경 이력

| 날짜 | 변경 내용 |
|---|---|
| 2026-05-15 | 초안 작성 — 프로젝트 정체성·코딩 규칙·금지 사항·참고 문서 정리 |
| 2026-05-15 | 위치 정정 — `docs/CLAUDE.md` → 프로젝트 루트 `CLAUDE.md`로 이동. 백엔드와 동일한 위치 정책(Claude Code 자동 로드를 위해 루트 필수)에 맞춰 내부 경로 참조 일괄 갱신 |
