# MOZI 프론트엔드

노인 맞춤형 통합 복지 안내 서비스 **MOZI**의 웹 프론트엔드. React + Vite SPA.
백엔드([`../moji-backend/`](../moji-backend/)) REST API와 연동하며, 전체 개요는 [상위 README](../README.md) 참고.

---

## 🚀 빠른 시작

```bash
# 1) 의존성 설치
npm install

# 2) 환경 변수 (백엔드 주소)
cp .env.example .env.local
#   .env.local 열어서 VITE_API_BASE_URL=http://localhost:8080

# 3) 개발 서버
npm run dev          # http://localhost:5173
```

| 명령 | 용도 |
|---|---|
| `npm run dev` | 개발 서버 (HMR) |
| `npm run build` | 프로덕션 빌드 (`dist/`) |
| `npm run preview` | 빌드 결과 미리보기 |
| `npm run lint` | ESLint 검사 |

---

## 🧰 기술 스택

- **React 19** + **Vite 8** (SPA, CSR, HMR)
- **React Router 7** — 클라이언트 라우팅
- **Tailwind CSS 3** — 스타일링 (노년층 접근성: 큰 글씨·고대비·48px 터치 타깃)
- **fetch** — HTTP 클라이언트 (axios 미사용, 의존성 0)
- **ESLint** — 코드 품질

> JavaScript 전용(TypeScript 미사용), 상태관리는 React Context 우선.

---

## 📁 구조

```
src/
├── main.jsx            # 진입점
├── App.jsx             # 라우트 정의
├── api/                # 백엔드 API 클라이언트 (client.js wrapper + 도메인별 함수)
├── contexts/           # 전역 상태 (인증 등)
├── hooks/              # 커스텀 훅
├── components/         # common / welfare / chat 컴포넌트
├── pages/              # 화면 단위 (로그인, 챗봇, 복지 검색/상세, 북마크, 마이페이지 등)
├── constants/ utils/ styles/
└── index.css           # Tailwind base + 글로벌
```

## 🔐 환경 변수

| 변수 | 용도 |
|---|---|
| `VITE_API_BASE_URL` | 백엔드 API 서버 주소 (예: `http://localhost:8080`) |

> ⚠️ Vite는 `VITE_` 접두사 변수만 클라이언트 번들에 주입합니다. 비밀 키는 절대 `VITE_` 변수에 넣지 마세요(번들에 평문 노출). 토큰은 accessToken=메모리 / refreshToken=sessionStorage 정책.

## 📚 문서

`docs/` 에 PRD · 화면 명세 · 컴포넌트 아키텍처 · API 클라이언트 가이드 · 스타일 가이드 · 사용자 플로우가 정리되어 있습니다. 작업 컨벤션은 [CLAUDE.md](CLAUDE.md) 참고.
