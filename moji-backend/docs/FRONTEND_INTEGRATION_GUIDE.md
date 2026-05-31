# MOZI 프론트엔드 통합 가이드 (React + Vite)

> 본 가이드는 MOZI 백엔드 API를 React + Vite SPA로 소비하는 프론트엔드를 만들기 위한 안내서다.
> **프론트엔드 경험이 없는 1인 개발자가 Claude Code를 활용해 처음부터 끝까지 구현하는 시나리오**를 전제로 작성되었다.
>
> 같이 봐야 할 문서:
> - [API_SPEC.md](./API_SPEC.md) — 16개 엔드포인트 명세
> - [ERROR_CODES.md](./ERROR_CODES.md) — 에러 코드 + 프론트 노출 정책
> - Swagger UI: http://localhost:8080/swagger-ui.html (백엔드 부팅 후)

---

## 📖 목차

| § | 내용 |
|---|---|
| 1 | 사용 전제 (Vite 설치·환경 변수·HTTP 라이브러리 선택) |
| 2 | 인증 흐름 (토큰 발급·저장·갱신·로그아웃) |
| 3 | 사용자 시나리오별 API 호출 순서 |
| 4 | CORS 동작 원리 |
| 5 | 페이지네이션 사용 패턴 |
| 6 | 에러 표시 가이드 |
| 7 | 노년층 UX 메모 |
| 8 | Claude Code로 프론트 작업 시작하는 법 |

---

## 1. 사용 전제

### 1-1. Vite 프로젝트 부팅 (한 번만)

```bash
cd /Users/hoyoung/Desktop/moji_project
npm create vite@latest frontend_project -- --template react
cd frontend_project
npm install
npm run dev
```

- 기본 포트: **5173**
- 백엔드는 본 가이드와 동일 디렉터리 트리의 `backend_project`에서 `./gradlew bootRun`으로 띄움.
- CORS는 5173, 3000 둘 다 허용되어 있으니 포트 충돌 시 3000으로 옮겨도 OK.

### 1-2. 환경 변수

프로젝트 루트에 `.env.local` (gitignore 대상):

```env
VITE_API_BASE_URL=http://localhost:8080
```

코드 안에서:
```javascript
const API_BASE = import.meta.env.VITE_API_BASE_URL;
```

**중요**: Vite의 환경 변수는 반드시 `VITE_` prefix가 있어야 클라이언트 번들에 포함된다. JWT secret 같은 비밀 정보는 절대 클라이언트에 두지 않는다 — 백엔드 통신용 URL/공개키만 OK.

### 1-3. HTTP 라이브러리 선택

| 옵션 | 장점 | 단점 |
|---|---|---|
| `fetch` (브라우저 내장) | 의존성 0 | 인터셉터 없음 — wrapper 직접 작성 |
| `axios` | 인터셉터 / 자동 JSON 변환 / 좋은 에러 분기 | 의존성 1개 |

**본 가이드는 fetch 기반 예시를 제공**한다 — 의존성을 줄이고 학습 비용을 낮추기 위함. 인터셉터가 필요해지면 axios 도입을 검토하면 됨.

### 1-4. (선택) TypeScript 사용

JavaScript로 시작하는 것을 권장 (학습 부담 절감). 안정성이 더 필요해지면 `--template react-ts`로 다시 부팅 가능. 본 가이드의 의사 코드는 JS.

---

## 2. 인증 흐름

### 2-1. 토큰 저장 정책

| 토큰 | 저장 위치 | 이유 |
|---|---|---|
| `accessToken` | **메모리 (React Context 또는 Zustand)** | XSS 노출 시 탈취 위험 최소화. 새로고침 시 사라짐 — refresh로 즉시 복원 |
| `refreshToken` | **sessionStorage** | 새로고침 후에도 유지. 탭 닫으면 자동 삭제. localStorage보다 노출 범위↓ |

**왜 sessionStorage인가**:
- localStorage: 탭/창이 닫혀도 유지 → XSS 위험 시간↑
- sessionStorage: 같은 탭 안에서만 유지 → 탭 닫으면 자동 클리어
- 졸업 프로젝트 수준에서는 sessionStorage가 보안·UX 균형이 좋음

> ⚠️ 운영 환경에서는 refreshToken을 HttpOnly Secure Cookie로 옮기는 게 정석. 본 프로젝트는 졸업 발표 시연용이라 sessionStorage 채택.

### 2-2. 로그인 → 토큰 수신

```javascript
async function login(email, password) {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  const json = await res.json();

  if (json.status === 'ERROR') {
    throw new Error(json.message);  // INVALID_CREDENTIALS 등
  }

  // 토큰 저장
  authStore.setAccessToken(json.data.accessToken);  // 메모리
  sessionStorage.setItem('refreshToken', json.data.refreshToken);

  return json.data;
}
```

### 2-3. 보호된 API 호출 + 자동 재시도 wrapper

```javascript
// 인증이 필요한 모든 API 호출은 이 함수를 거친다
async function apiCall(path, options = {}) {
  const accessToken = authStore.getAccessToken();
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken ? { 'Authorization': `Bearer ${accessToken}` } : {}),
      ...(options.headers || {})
    }
  });
  const json = await res.json();

  // access 만료 → refresh 후 1회 재시도
  if (json.status === 'ERROR' && json.errorCode === 'TOKEN_EXPIRED') {
    const refreshed = await tryRefresh();
    if (refreshed) {
      return apiCall(path, options);  // 재시도
    }
    redirectToLogin();
    return;
  }

  // refresh 실패류
  if (json.status === 'ERROR' &&
      (json.errorCode === 'INVALID_TOKEN' || json.errorCode === 'INVALID_REFRESH_TOKEN')) {
    clearTokens();
    redirectToLogin();
    return;
  }

  return json;
}

async function tryRefresh() {
  const refreshToken = sessionStorage.getItem('refreshToken');
  if (!refreshToken) return false;
  const res = await fetch(`${API_BASE}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });
  const json = await res.json();
  if (json.status !== 'SUCCESS') return false;
  authStore.setAccessToken(json.data.accessToken);
  sessionStorage.setItem('refreshToken', json.data.refreshToken);  // Rotation: 새 raw 토큰
  return true;
}

function clearTokens() {
  authStore.setAccessToken(null);
  sessionStorage.removeItem('refreshToken');
}
```

### 2-4. 로그아웃

```javascript
async function logout() {
  await apiCall('/api/auth/logout', { method: 'POST' });
  clearTokens();
  // 같은 사용자의 모든 기기 RefreshToken row가 서버에서 삭제됨
}
```

### 2-5. 비밀번호 변경 후 처리 ⚠️ 보안 권장

백엔드는 비밀번호 변경 시 **본인의 모든 RefreshToken row를 삭제**하지만 access 토큰은 stateless JWT라 서버 측에서 무효화하지 못한다 — 즉 변경 직후에도 기존 access 토큰은 만료 시각(최대 1시간)까지 살아있다.

**프론트 정책**: 비밀번호 변경 성공 응답을 받으면 **즉시 클라이언트 측에서 토큰을 클리어하고 로그인 페이지로 리다이렉트**한다. 도난당한 access 토큰이 한 시간 동안 살아남는 보안 갭을 막아준다.

```javascript
async function changePassword(currentPassword, newPassword) {
  const res = await apiCall('/api/users/me/password', {
    method: 'PUT',
    body: JSON.stringify({ currentPassword, newPassword })
  });

  if (res.status === 'SUCCESS') {
    // 1) 사용자에게 변경 완료 안내 (4초 토스트 권장 — 노년층 가독성)
    toast(res.message);  // "비밀번호가 변경되었어요. 다른 기기는 다시 로그인이 필요해요."
    // 2) 짧은 지연 후 토큰 클리어 + 로그인 페이지로
    setTimeout(() => {
      clearTokens();
      navigate('/login', { state: { reason: 'password-changed' } });
    }, 2000);
  } else {
    // PASSWORD_MISMATCH(400) / SAME_PASSWORD(400) / VALIDATION_FAILED(400)
    handleError(res);
  }
}
```

> 백엔드 정책상 다른 기기는 access 만료(최대 1시간) 뒤 자연스럽게 끊긴다 — 별도 클라이언트 처리 X.



---

## 3. 사용자 시나리오별 API 호출 순서

> [USER_FLOW.md](./USER_FLOW.md) 시나리오와 매핑.

### 3-1. 신규 사용자 가입 후 첫 검색

```
1) POST /api/auth/signup
     ↓ accessToken + refreshToken 저장
2) (선택) GET /api/regions       — 프로필 입력 화면 진입 전 시도/시군구 옵션 캐싱
3) (선택) PUT /api/users/me/profile
     - 노년층은 프로필 입력 진입장벽이 높음. 건너뛰기 허용.
     - sidoCode/sigunguCode는 GET /api/regions의 코드 값을 그대로 전송
4) GET /api/welfares?keyword=...&category=...
     - Step 6에서 applyMyProfile 정책 폐기 — 본인 프로필 자동 반영 X
     - 프로필 입력 안 했어도 키워드만으로 검색 가능
```

### 3-2. 재방문 로그인

```
1) POST /api/auth/login
     ↓ accessToken 메모리 / refreshToken sessionStorage
2) GET /api/users/me/profile   (홈 화면 인사말용)
3) GET /api/welfares  또는  GET /api/bookmarks
```

### 3-3. 새로고침 후 세션 복구

```
1) refreshToken이 sessionStorage에 있는지 확인
2) 있으면 → POST /api/auth/refresh로 access 토큰 복원
3) 없으면 → 비로그인 상태로 진입 (Welfare 검색은 가능)
```

### 3-4. 챗봇 대화

```
1) POST /api/chat  { message: "노인 일자리 알려줘", conversationId: null }
     ↓ 응답의 conversationId를 sessionStorage 저장
2) 후속 메시지: POST /api/chat
     { message: "그 중 첫 번째 더 알려줘", conversationId: "<이전 값>" }
3) 응답의 welfares가 []이면 "복지 카드 영역 비움" — reply 텍스트만 표시
```

### 3-5. 북마크 토글

```
- 추가:  POST /api/bookmarks/{welfareId}   (Idempotent)
- 해제:  DELETE /api/bookmarks/{welfareId}

UI는 항상 "낙관적 업데이트"(클릭 즉시 별 아이콘 토글) + 응답 실패 시 롤백을 권장.
404(WELFARE_NOT_FOUND / BOOKMARK_NOT_FOUND)는 토스트 X, UI 상태만 복원.
```

### 3-6. 검색 결과에서 상세 보기

```
1) GET /api/welfares  (목록)
2) 카드 클릭 → GET /api/welfares/{id}  (상세)
3) 응답의 *Detail 4종 중 채워진 1개로 분기 렌더링
```

### 3-7. 행정구역 cascading select (Step 1·8 신규)

프로필 입력 화면에서 시도→시군구 2단계 선택을 위해 `GET /api/regions`를 한 번 호출해 캐싱:

```javascript
// 페이지 진입 시 1회 호출 (비로그인도 가능, permitAll)
const { data: sidoGroups } = await apiCall('/api/regions');
// sidoGroups: [
//   { sidoCode: "11", sidoName: "서울특별시", sigungus: [{ code: "11680", name: "강남구" }, ...] },
//   { sidoCode: "36", sidoName: "세종특별자치시", sigungus: [{ code: null, name: null }] },
//   ...
// ]

// 1단계: 시도 select
<select onChange={e => setSelectedSido(e.target.value)}>
  {sidoGroups.map(g => <option value={g.sidoCode}>{g.sidoName}</option>)}
</select>

// 2단계: 선택된 시도의 시군구 select
const sigungus = sidoGroups.find(g => g.sidoCode === selectedSido)?.sigungus ?? [];
<select onChange={e => setSelectedSigungu(e.target.value)}>
  <option value="">(시도만 선택)</option>
  {sigungus.filter(s => s.code !== null).map(s =>
    <option value={s.code}>{s.name}</option>
  )}
</select>

// PUT 저장 시 코드 값 그대로 전송
await apiCall('/api/users/me/profile', {
  method: 'PUT',
  body: JSON.stringify({ sidoCode: selectedSido, sigunguCode: selectedSigungu || null })
});
```

**세종 처리**: `sidoCode="36"`은 산하 시군구가 없는 단일 행 — 시군구 선택을 비활성화하거나 "(시도만 선택)" 옵션 자동 적용.

**대안 호출**: 특정 시도만 필요하면 `GET /api/regions?sido=11` — 평면 `RegionDto[]` 응답.

---

## 4. CORS 동작 원리

### 4-1. 왜 OPTIONS 요청이 먼저 가는가

브라우저는 다른 origin(예: 5173 → 8080)으로 가는 "복잡한" 요청 전에 **자동으로 OPTIONS preflight**를 보낸다. 서버가 "이 origin·method·header 조합을 허용한다"고 회신해야 본 요청을 보낸다. 직접 코딩할 필요 X — 브라우저가 자동.

### 4-2. 백엔드가 무엇을 허용하는가

`backend_project/src/main/java/com/mozi/backend/global/config/CorsConfig.java` 참조:
- **Origin**: `http://localhost:5173`, `http://localhost:3000`
- **Method**: GET / POST / PUT / DELETE / OPTIONS
- **Header**: `Authorization`, `Content-Type`
- **Credentials**: false (쿠키 X — 토큰은 헤더로만 전달)

### 4-3. 발생하기 쉬운 실수

| 증상 | 원인 |
|---|---|
| 콘솔에 "CORS error" | 5173/3000이 아닌 다른 포트로 부팅한 경우. CorsConfig에 추가 필요. |
| OPTIONS 200 OK인데 본 요청은 401 | 정상 — 토큰이 잘못된 별개 문제. |
| `Authorization` 헤더가 안 보임 | preflight 단계에서 fetch가 헤더를 제거함. 정상. |

---

## 5. 페이지네이션 사용 패턴

### 5-1. 응답 구조

```json
{
  "items": [...],
  "page": 0,
  "size": 10,
  "totalCount": 137,
  "hasNext": true
}
```

### 5-2. "더 보기" 패턴 (권장 — 노년층 UX에 친숙)

```javascript
async function loadMore() {
  const res = await apiCall(`/api/welfares?page=${nextPage}&size=10`);
  setItems(prev => [...prev, ...res.data.items]);
  setNextPage(res.data.page + 1);
  setHasNext(res.data.hasNext);
}
```

화면에 "더 보기" 큰 버튼 1개. `hasNext: false`면 버튼 숨김.

### 5-3. 페이지 번호 vs 무한 스크롤

| 패턴 | 노년층 적합도 |
|---|---|
| "더 보기" 버튼 | ★★★★★ (가장 명시적) |
| 페이지 번호 (1/2/3...) | ★★★★☆ (익숙하지만 작은 번호 클릭은 어려움) |
| 무한 스크롤 | ★★☆☆☆ (스크롤 끝을 모름 — 비추천) |

---

## 6. 에러 표시 가이드

### 6-1. 응답 분기 패턴

```javascript
const res = await apiCall(...);
if (res.status === 'SUCCESS') {
  // 정상
} else {
  switch (res.errorCode) {
    case 'VALIDATION_FAILED':
      // res.fields의 각 필드를 폼 입력란 옆에 표시
      Object.entries(res.fields).forEach(([field, msg]) => {
        setFieldError(field, msg);
      });
      break;
    case 'TOKEN_EXPIRED':
      // 자동 재시도는 apiCall wrapper가 처리. 여기 도달 X.
      break;
    case 'CHATBOT_TIMEOUT':
    case 'CHATBOT_UNAVAILABLE':
      showRetryButton('지금은 추천이 어려워요. 잠시 후 다시 시도해주세요.');
      break;
    default:
      toast(res.message);  // 대부분의 도메인 에러는 message 그대로 노출 OK
  }
}
```

### 6-2. 각 에러 코드별 정책
[ERROR_CODES.md §4](./ERROR_CODES.md#4-프론트엔드-노출-가이드-요약) 참조.

### 6-2-1. `INVALID_REGION_CODE` 처리 (Step 5 신규)

`PUT /api/users/me/profile` 시 sidoCode/sigunguCode 검증 실패. 케이스 3종:
1. **sigunguCode만 단독 입력** — 시도 선택 누락 → "시도부터 선택해주세요" UI 안내
2. **sidoCode가 REGION에 없음** — 잘못된 코드 → cascading select를 `GET /api/regions`로 재구성
3. **시도-시군구 조합 불일치** — UI 상태가 깨진 케이스 → 시도 변경 시 시군구 selectedValue 초기화

```javascript
if (res.errorCode === 'INVALID_REGION_CODE') {
  toast(res.message);  // 백엔드 message가 케이스별로 구체적임
  setSelectedSigungu('');  // 시군구 초기화
  // 시도 변경 시 시군구 자동 초기화 핸들러도 점검 권장
}
```

### 6-3. VALIDATION_FAILED 폼 필드 매핑

```javascript
// 회원가입 폼
const handleSignupError = (json) => {
  if (json.errorCode !== 'VALIDATION_FAILED') return;
  // fields = { email: "이메일 형식이 올바르지 않아요.", password: "..." }
  setErrors(json.fields);  // React Hook Form 등에 매핑
};
```

각 필드 이름은 백엔드 DTO 필드명과 동일 (`email`, `password`, `nickname`, `message`, `currentPassword`, `newPassword`).

---

## 7. 노년층 UX 메모

상세는 PRD/USER_FLOW 참조. 프론트 작업 시 핵심 원칙만 정리:

| 항목 | 권장값 |
|---|---|
| 본문 폰트 | 최소 18px, 가급적 20px+ |
| 터치 영역 | 최소 48 × 48 px |
| 색 대비 | WCAG AA (4.5:1) 이상 |
| 한 화면 한 task | 한 페이지에 여러 입력 폼 X — 단계로 분리 |
| 색 외 의미 전달 | 빨간 글씨에 ⚠️ 아이콘 함께. 색맹 사용자 대비 |
| 토스트 표시 시간 | 4초 이상 |
| 버튼 텍스트 | "확인" / "취소" 같은 짧은 동사보다 "회원가입하기" / "검색하기" 처럼 행동 명시 |
| 입력 도움말 | 입력란 옆 placeholder 외에 상시 표시 텍스트 1줄 |
| 에러 위치 | 입력란 바로 아래/옆 — 폼 상단 한 줄 요약은 어려움 |

---

## 8. Claude Code로 프론트 작업 시작하는 법

### 8-1. 새 작업 디렉터리 진입

```bash
cd /Users/hoyoung/Desktop/moji_project/frontend_project
claude
```

`frontend_project/CLAUDE.md`(없으면 생성)에 프론트엔드 컨벤션을 작성. 백엔드와 동일하게 1인 개발자 학습 목적이라고 명시.

### 8-2. 첫 프롬프트 권장 패턴

```
나는 백엔드(../backend_project)를 직접 구현했고 이제 프론트엔드를 처음부터 만든다.
다음 3개 문서를 먼저 정독해줘:
1) ../backend_project/docs/API_SPEC.md
2) ../backend_project/docs/FRONTEND_INTEGRATION_GUIDE.md  (← 본 문서)
3) ../backend_project/docs/ERROR_CODES.md

그리고 Swagger UI(http://localhost:8080/swagger-ui.html)에서 정확한 스키마도 확인 가능해.

오늘 시작할 작업은: [예: 로그인 페이지 + 인증 흐름 wrapper 구현].
React + Vite + JS, 의존성은 fetch만 사용. 작업 전에 설계 제안하고 내 승인 받고 시작.
```

### 8-3. 문서 우선 순위

- 정확한 스키마가 필요할 때 → **Swagger UI** (자동 생성, 항상 최신)
- 사람이 훑어보고 싶을 때 → **API_SPEC.md**
- 흐름·시나리오·의사 코드 → **본 가이드**
- 에러 처리 분기 → **ERROR_CODES.md**

### 8-4. 백엔드와 프론트 양쪽 동시 실행

```bash
# 터미널 1
cd backend_project && ./gradlew bootRun
# 터미널 2
cd frontend_project && npm run dev
```

브라우저: `http://localhost:5173` (프론트) / `http://localhost:8080/swagger-ui.html` (백엔드 API 테스트)

---

## 9. 자주 막힐 만한 지점 미리보기

| 문제 | 해결 |
|---|---|
| 새로고침 후 로그인 풀림 | 정상 — accessToken은 메모리. refreshToken으로 자동 복구하도록 앱 부팅 시 `tryRefresh()` 한번 호출. |
| `welfares: []` 빈 배열에 카드 렌더 | 챗봇이 인사·잡담을 받았을 때. 정상. UI에서 "추천 복지 없음" 영역 비우면 됨. |
| 로그인은 됐는데 401만 옴 | accessToken에 `Bearer ` prefix 누락. fetch wrapper 확인. |
| 같은 refreshToken 두 번 사용 | Rotation 정책상 두 번째는 `INVALID_REFRESH_TOKEN`. wrapper에 race condition 있는지 확인. |
| 카테고리 코드 의미를 모르겠음 | [CATEGORY_REFERENCE.md](./CATEGORY_REFERENCE.md) 참조. |

---

## 10. 변경 이력

| 일자 | 변경 | 작성자 |
|---|---|---|
| 2026-05-15 | Phase 6 초안 — 프론트 작업 Claude Code의 인풋용 가이드 | MOZI 백엔드 |
