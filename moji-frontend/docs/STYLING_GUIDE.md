# STYLING GUIDE — Tailwind 디자인 토큰 + 노년층 UX

> 이 문서는 `tailwind.config.js`에 박을 **정식 디자인 토큰**과 그 토큰을 컴포넌트에 어떻게 적용하는지를 정리한다.
> `PROJECT_SETUP.md`에서 임시로 박아둔 폰트 토큰을 이 문서 기준으로 전면 교체한다.
> 노년층 UX는 이 프로젝트의 차별화 핵심이므로 본 가이드의 규칙은 **권장이 아니라 강제**다.

---

## 1. 왜 디자인 토큰을 미리 정하는가

### 1-1. 일관성
- 한 페이지에서 본문이 18px인데 다른 페이지가 16px이면, 사용자는 "왜 이건 글씨가 작지?" 라고 인지함
- 색·간격·폰트를 모두 토큰으로 통일하면 화면 단위 일관성이 보장됨

### 1-2. 노년층 UX 강제력
- 토큰으로 "본문 최소 18px"을 박아두면 `text-sm`(14px) 같은 작은 클래스를 실수로 못 쓰게 됨
- 매번 "이거 노년층한테 너무 작나?" 고민할 필요 없음 → 토큰만 골라쓰면 자동 통과

### 1-3. 유지보수
- 색을 한 번 정해두면 나중에 "브랜드 색을 살짝 진하게 바꾸자" 했을 때 `tailwind.config.js` 한 곳만 수정
- 컴포넌트 100개를 일일이 찾아 바꿀 일이 사라짐

> **디자인 토큰**(Design Token): 색·폰트 사이즈·간격·둥근 모서리 같은 시각 속성을 이름이 붙은 변수로 정의한 것. 코드 전체가 이 이름만 참조하도록 만든다.

---

## 2. `tailwind.config.js` 전체 (복사해서 그대로 사용)

`PROJECT_SETUP.md` §3-2에서 만든 임시 파일을 다음으로 **덮어쓴다**.

```javascript
/** @type {import('tailwindcss').Configuration} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}",
  ],
  theme: {
    extend: {
      // ─────────────────────────────────────────
      // 색 (Color Tokens)
      //  - 노년층 친화: 채도 과하지 않고 대비 충분 (WCAG AA 4.5:1 이상)
      //  - 한 이름당 base + hover/active까지 정의해 클릭 피드백을 강하게
      // ─────────────────────────────────────────
      colors: {
        brand: {
          DEFAULT: "#2563EB", // blue-600 — 신뢰감 있는 파랑 (CTA 버튼/링크 기본)
          hover:   "#1D4ED8", // blue-700
          subtle:  "#DBEAFE", // blue-100 — 배경 강조용 연한 톤
        },
        ink: {
          // 텍스트 전용 컬러. "text-ink-strong" 식으로 사용
          strong: "#111827", // gray-900 — 제목/본문 핵심
          DEFAULT: "#1F2937", // gray-800 — 본문 기본
          weak:    "#4B5563", // gray-600 — 보조 텍스트 (라벨, 도움말)
          mute:    "#9CA3AF", // gray-400 — 비활성·placeholder
          invert:  "#FFFFFF", // 어두운 배경 위 흰 글씨
        },
        surface: {
          DEFAULT: "#FFFFFF", // 카드/모달 배경
          muted:   "#F9FAFB", // gray-50 — 페이지 배경
          border:  "#E5E7EB", // gray-200 — 카드/입력란 테두리
        },
        danger: {
          DEFAULT: "#DC2626", // red-600 — 에러 텍스트/테두리
          hover:   "#B91C1C", // red-700
          subtle:  "#FEE2E2", // red-100 — 에러 박스 배경
        },
        success: {
          DEFAULT: "#16A34A", // green-600 — 성공 토스트
          subtle:  "#DCFCE7", // green-100
        },
        warning: {
          DEFAULT: "#D97706", // amber-600 — 주의 안내
          subtle:  "#FEF3C7", // amber-100
        },
      },

      // ─────────────────────────────────────────
      // 폰트 사이즈 (Type Scale)
      //  - 노년층 친화: 본문 18px부터 시작
      //  - 일반 웹은 14~16px 기본이지만 본 서비스는 18px가 기본
      // ─────────────────────────────────────────
      fontSize: {
        "senior-sm":  ["16px", { lineHeight: "1.6" }], // 보조 라벨·푸터·작은 메타
        "senior-base":["18px", { lineHeight: "1.6" }], // 본문 기본 (대부분 텍스트)
        "senior-lg":  ["22px", { lineHeight: "1.5" }], // 카드 제목·중간 강조
        "senior-xl":  ["28px", { lineHeight: "1.4" }], // 페이지 제목
        "senior-2xl": ["32px", { lineHeight: "1.3" }], // 인사말·메인 헤드라인
      },

      // ─────────────────────────────────────────
      // 간격 (Spacing)
      //  - Tailwind 기본(0.25rem = 4px 단위)을 그대로 쓰되, 핵심 보조 토큰만 추가
      //  - 48px(=h-12)이 인터랙티브 요소 최소 높이의 기준
      // ─────────────────────────────────────────
      spacing: {
        "touch":     "48px", // 손가락 터치 최소 영역
        "touch-lg":  "56px", // 강조 CTA 버튼
      },

      // ─────────────────────────────────────────
      // 둥근 모서리 (Radius)
      //  - 노년층은 날카로운 모서리보다 둥근 게 친근하게 인지됨
      // ─────────────────────────────────────────
      borderRadius: {
        "soft":   "8px",   // 입력란/작은 버튼
        "card":   "12px",  // 카드/모달
        "pill":   "9999px",
      },

      // ─────────────────────────────────────────
      // 그림자 (Shadow)
      //  - 카드 분리감을 위해 옅게. 너무 진하면 노년층에게 시각 노이즈
      // ─────────────────────────────────────────
      boxShadow: {
        "card":   "0 1px 3px 0 rgba(0,0,0,0.08), 0 1px 2px -1px rgba(0,0,0,0.04)",
        "modal":  "0 20px 40px -10px rgba(0,0,0,0.20)",
      },

      // ─────────────────────────────────────────
      // 브레이크포인트 (Tailwind 기본 그대로)
      //  - sm: 640px, md: 768px, lg: 1024px, xl: 1280px
      // ─────────────────────────────────────────
    },
  },
  plugins: [],
};
```

> ⚠️ 토큰을 확장·수정할 때는 **반드시 이 파일만 수정**. 컴포넌트에 임의 색·임의 px값 박지 않는다 (§4 위반 사례 참조).

---

## 3. 사용 가이드라인 (강제 규칙)

### 3-1. 폰트 사이즈
| 상황 | 사용 토큰 | 금지 사항 |
|---|---|---|
| 본문 텍스트 | `text-senior-base` (18px) | `text-sm`, `text-base`(16px) ❌ |
| 라벨·도움말·푸터 | `text-senior-sm` (16px) | `text-xs`(12px) ❌ |
| 카드/섹션 제목 | `text-senior-lg` (22px) | |
| 페이지 제목 | `text-senior-xl` (28px) | |
| 메인 헤드라인 | `text-senior-2xl` (32px) | |

> **❗ 16px 미만 금지** — 노년층에게는 너무 작다. 디자인적으로 작게 보이고 싶으면 색을 `text-ink-weak`로 약화시키는 것으로 대체.

### 3-2. 인터랙티브 요소 (버튼·링크·탭·입력란)
| 항목 | 규칙 |
|---|---|
| 최소 높이 | `h-12` (48px) 또는 `min-h-touch` |
| 강조 CTA 높이 | `h-14` (56px) 또는 `min-h-touch-lg` |
| 가로 패딩 | `px-6` 이상 |
| 폰트 | 본문과 동일 (`text-senior-base`) — 버튼이 본문보다 작으면 잘못된 위계 |

> **❗ 32~40px 정도의 작은 버튼 금지** — 손가락으로 정확히 누르기 어렵다.

### 3-3. 색
| 상황 | 사용 토큰 |
|---|---|
| 페이지 배경 | `bg-surface-muted` (회색 톤) |
| 카드 배경 | `bg-surface` (흰색) |
| 본문 텍스트 | `text-ink-strong` 또는 `text-ink` |
| 보조 텍스트 | `text-ink-weak` |
| 비활성 텍스트 | `text-ink-mute` |
| CTA 버튼 | `bg-brand text-ink-invert` |
| 에러 메시지 | `text-danger` + `bg-danger-subtle` |
| 성공 토스트 | `bg-success text-ink-invert` 또는 `bg-success-subtle text-success` |
| 테두리 (입력란/카드) | `border-surface-border` |

> **❗ 임의 색 금지** — `text-[#333]`, `bg-blue-300` 같은 직접 색 코드/Tailwind 원색 사용 금지. 반드시 토큰 이름으로.

### 3-4. 색 대비 검증 (WCAG AA 4.5:1 이상)

위 토큰 조합은 모두 AA 만족하도록 설계되어 있다. 새 조합을 만들 때는 다음 도구로 검증:

- **WebAIM Contrast Checker**: https://webaim.org/resources/contrastchecker/
- 브라우저 DevTools: Inspect → Computed → Color → 대비 비율 표시

붉은 글씨를 강조하고 싶을 때 채도 너무 높이지 말 것. `text-danger` (#DC2626)는 흰 배경 위에서 AA 통과.

### 3-5. 간격·둥근 모서리
- 카드 내부 패딩 최소 `p-4` (16px)
- 카드끼리 간격 `gap-4` 이상
- 입력란 둥글기 `rounded-soft` (8px)
- 카드/모달 둥글기 `rounded-card` (12px)

---

## 4. 위반 사례 (하지 말 것)

```jsx
{/* ❌ 잘못된 예 */}
<button className="h-8 px-2 text-sm bg-blue-500 text-white rounded">
  로그인
</button>

<p className="text-[14px] text-[#666]">
  비밀번호를 잊으셨나요?
</p>
```

문제점:
- `h-8` (32px) → 터치 영역 미달
- `text-sm` (14px) → 노년층에 너무 작음
- `bg-blue-500` → 토큰이 아닌 Tailwind 원색 직접 사용
- `text-[14px]`, `text-[#666]` → 임의값 사용

```jsx
{/* ✅ 올바른 예 */}
<button className="h-12 px-6 text-senior-base bg-brand text-ink-invert rounded-soft hover:bg-brand-hover">
  로그인
</button>

<p className="text-senior-sm text-ink-weak">
  비밀번호를 잊으셨나요?
</p>
```

---

## 5. 컴포넌트 스타일 패턴 (Cookbook)

> 자주 만드는 5개 컴포넌트에 토큰을 어떻게 조합하는지 정리. 그대로 `src/components/common/`에 옮겨 써도 무방.

### 5-1. Button — 1차/2차/위험 3종
```jsx
/**
 * Button
 *
 * @param {Object} props
 * @param {"primary"|"secondary"|"danger"} [props.variant="primary"]
 * @param {boolean} [props.large=false] - 강조 CTA(높이 56px)
 * @param {React.ReactNode} props.children
 * @param {...any} rest - button 기본 props (onClick, type, disabled 등)
 */
function Button({ variant = "primary", large = false, children, ...rest }) {
  const base =
    "inline-flex items-center justify-center rounded-soft text-senior-base font-medium " +
    "transition-colors disabled:opacity-50 disabled:cursor-not-allowed " +
    (large ? "h-14 px-8" : "h-12 px-6");

  const variantClass = {
    primary:   "bg-brand text-ink-invert hover:bg-brand-hover",
    secondary: "bg-surface text-ink-strong border border-surface-border hover:bg-surface-muted",
    danger:    "bg-danger text-ink-invert hover:bg-danger-hover",
  }[variant];

  return (
    <button className={`${base} ${variantClass}`} {...rest}>
      {children}
    </button>
  );
}
```

사용:
```jsx
<Button onClick={handleLogin}>로그인</Button>
<Button variant="secondary">취소</Button>
<Button variant="danger" large>회원 탈퇴</Button>
```

### 5-2. Input — 라벨 필수, 에러 표시 포함
```jsx
/**
 * Input
 *
 * 라벨/도움말/에러 메시지를 한 단위로 묶은 입력란.
 * 노년층 UX 강제: 라벨 텍스트 반드시 표시, placeholder만으로 의미 전달 금지.
 *
 * @param {Object} props
 * @param {string} props.label - 입력란 위에 항상 표시되는 라벨
 * @param {string} [props.hint] - 입력란 아래의 도움말 (예시값 등)
 * @param {string} [props.error] - 에러 메시지 (있으면 빨간 테두리 + 메시지 표시)
 * @param {...any} rest - input 기본 props (value, onChange, type 등)
 */
function Input({ label, hint, error, ...rest }) {
  return (
    <label className="block">
      <span className="block text-senior-base text-ink-strong mb-2">{label}</span>
      <input
        className={
          "block w-full h-12 px-4 text-senior-base text-ink-strong rounded-soft border " +
          (error ? "border-danger" : "border-surface-border focus:border-brand") +
          " focus:outline-none focus:ring-2 focus:ring-brand-subtle"
        }
        {...rest}
      />
      {hint && !error && (
        <span className="block mt-2 text-senior-sm text-ink-weak">{hint}</span>
      )}
      {error && (
        <span className="block mt-2 text-senior-sm text-danger">{error}</span>
      )}
    </label>
  );
}
```

사용:
```jsx
<Input
  label="이메일"
  hint="예: hong@example.com"
  type="email"
  value={email}
  onChange={(e) => setEmail(e.target.value)}
  error={fieldErrors.email}
/>
```

### 5-3. Card — 복지 카드 등 리스트 아이템
```jsx
/**
 * Card
 *
 * 흰 배경 + 옅은 그림자 + 둥근 카드. 호버 시 살짝 떠오름.
 * 클릭 가능한 경우 onClick을 받고 전체가 버튼처럼 동작.
 */
function Card({ onClick, children }) {
  const Tag = onClick ? "button" : "div";
  return (
    <Tag
      type={onClick ? "button" : undefined}
      onClick={onClick}
      className={
        "block w-full text-left p-5 rounded-card bg-surface border border-surface-border " +
        "shadow-card " +
        (onClick ? "hover:shadow-modal transition-shadow cursor-pointer" : "")
      }
    >
      {children}
    </Tag>
  );
}
```

### 5-4. Modal — 회원 탈퇴 확인 등
```jsx
/**
 * Modal
 *
 * 화면 중앙에 카드 형태로 띄우고 배경을 어둡게.
 * ESC/배경 클릭으로 닫기 가능.
 *
 * @param {Object} props
 * @param {boolean} props.open
 * @param {() => void} props.onClose
 * @param {string} props.title
 * @param {React.ReactNode} props.children
 */
function Modal({ open, onClose, title, children }) {
  if (!open) return null;
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
      onClick={onClose}
    >
      <div
        className="w-full max-w-md bg-surface rounded-card shadow-modal p-6"
        onClick={(e) => e.stopPropagation()} // 모달 내부 클릭은 닫힘 방지
      >
        <h2 className="text-senior-lg text-ink-strong mb-4">{title}</h2>
        <div className="text-senior-base text-ink">{children}</div>
      </div>
    </div>
  );
}
```

> ⚠️ 접근성: 실제 운영용 모달은 ESC 키 / 포커스 트랩 / `aria-modal` 처리가 필요하지만, 본 MVP는 위 최소 구조로 시작하고 Phase 6에서 보강.

### 5-5. Toast — 성공/에러 안내
```jsx
/**
 * Toast
 *
 * 화면 하단에 4초 표시되는 짧은 안내.
 * 노년층 UX: 4초 이상 + 폰트 senior-base + 색 대비 강.
 *
 * @param {Object} props
 * @param {"success"|"error"|"info"} [props.kind="info"]
 * @param {string} props.message
 */
function Toast({ kind = "info", message }) {
  const color = {
    success: "bg-success text-ink-invert",
    error:   "bg-danger text-ink-invert",
    info:    "bg-ink-strong text-ink-invert",
  }[kind];
  return (
    <div className={`fixed bottom-6 left-1/2 -translate-x-1/2 px-6 h-12 flex items-center rounded-pill shadow-modal text-senior-base ${color}`}>
      {message}
    </div>
  );
}
```

> 실제로는 동시에 여러 토스트가 뜰 수 있으므로 Context + 큐 구조로 발전시키는 게 좋다. Phase 3에서 도입.

---

## 6. 조건부 클래스 정리 (clsx 없이)

> **clsx**: 조건부 className을 깔끔히 합쳐주는 작은 라이브러리. 본 프로젝트는 외부 의존성 최소화를 위해 도입하지 않고 템플릿 리터럴(`)로 처리.

### 권장 패턴
```jsx
const className =
  "h-12 px-6 rounded-soft text-senior-base " +
  (isPrimary ? "bg-brand text-ink-invert" : "bg-surface text-ink-strong") +
  (disabled ? " opacity-50 cursor-not-allowed" : " hover:bg-brand-hover");
```

### 안 권장 (가독성 떨어짐)
```jsx
className={`h-12 ${isPrimary && "bg-brand"} ${disabled ? "opacity-50" : "hover:bg-brand-hover"} text-senior-base ${size === "lg" && "px-8"}`}
```

조건이 4개를 넘으면 `clsx` 도입을 사용자에게 제안 (CLAUDE.md §5-3에서 도입 검토 가능 라이브러리 목록에 포함됨).

---

## 7. 반응형 (Mobile-First)

본 프로젝트는 시연 시 태블릿/스마트폰 사용 비중이 높다고 가정한다. 따라서:

- **기본 작성은 모바일(폭 < 640px) 기준**
- 더 큰 화면용 스타일은 `sm:` `md:` `lg:` 접두사로 추가

예시 — 본문 카드가 모바일에선 한 줄, 데스크탑에선 두 줄:
```jsx
<div className="grid grid-cols-1 md:grid-cols-2 gap-4">
  <WelfareCard ... />
  <WelfareCard ... />
</div>
```

페이지 폭 제한:
```jsx
<main className="w-full max-w-screen-md mx-auto px-4 md:px-6 py-6">
  ...
</main>
```

> `max-w-screen-md` (768px) — 노년층은 한 화면에 정보가 너무 많으면 인지 부하. 본문은 한 컬럼 위주로.

---

## 8. 다크모드 — 도입하지 않음

본 MVP는 라이트 모드만 지원한다. 이유:
- 노년층은 다크모드 친숙도 낮음
- 토큰 2배로 정의해야 함 (관리 부담)
- CLAUDE.md §5-2의 "지시받지 않은 기능 선제 개발 금지" 원칙

Phase 6에서 사용성 검증 후 정말 필요하면 추가 검토.

---

## 9. 자주 묻는 함정

### 9-1. `text-senior-base`가 적용되지 않는다
- 원인: `tailwind.config.js`의 `content`가 `./src/**/*.{js,jsx}`로 되어 있는지 확인
- 또는 개발 서버를 안 껐다 켰음 → `Ctrl+C` 후 `npm run dev`

### 9-2. 색이 안 변한다
- 원인: `bg-brand` 같은 토큰명을 오타 (`bg-brnad` 등) → DevTools에서 클래스가 적용된 게 보이는데 색이 기본값이면 토큰명 확인
- 또는 `tailwind.config.js`에서 `extend.colors` 안에 정의했는지 확인

### 9-3. 둥근 모서리가 안 보인다
- 원인: 카드에 `rounded-card`만 있고 `overflow-hidden`이 없어 내부 자식이 모서리 밖으로 비져나옴
- 해결: 이미지 등 내부 자식이 모서리를 따라야 한다면 카드에 `overflow-hidden` 추가

### 9-4. 그림자가 잘 안 보인다
- 원인: 부모 배경이 흰색이고 카드도 흰색이면 그림자 차이가 미미함
- 해결: 페이지 배경을 `bg-surface-muted` (gray-50)로 두고 카드만 `bg-surface` (white)로

---

## 10. 노년층 UX 체크리스트 (페이지 완성 후 확인)

- [ ] 본문 텍스트가 18px 이상으로 보이는가 (브라우저 100% 기준)
- [ ] 모든 버튼/링크/탭이 높이 48px 이상인가
- [ ] 모든 입력란에 라벨이 표시되는가 (placeholder만 X)
- [ ] 에러 메시지가 영어/기술 용어 없이 한글로 자연스러운가
- [ ] 한 화면의 핵심 액션이 1~2개로 명확한가
- [ ] 색 대비가 4.5:1 이상인가 (DevTools 확인)
- [ ] 토스트 표시 시간이 4초 이상인가
- [ ] 페이지네이션이 페이지 번호 방식인가 (이전/다음 + 번호 chip, 무한 스크롤 X). 모든 버튼은 h-12 이상 터치 타깃 확보
- [ ] 폭 640px 화면에서도 가로 스크롤 없이 보이는가

이 체크리스트는 `EXECUTION_PLAN.md` Phase 6에서 다시 인용된다.

---

## 11. 변경 이력

| 날짜 | 변경 내용 |
|---|---|
| 2026-05-15 | 초안 작성 — color/fontSize/spacing/radius/shadow 토큰 정의, 컴포넌트 패턴 5종, 위반 사례, 체크리스트 |
| 2026-05-17 | 페이지네이션 룰 정정 — "더 보기" 패턴 → 페이지 번호 방식 (이전/다음 + 최대 5개 번호 chip). h-12 터치 타깃 유지 |
