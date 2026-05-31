/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}",
  ],
  theme: {
    extend: {
      // ─────────────────────────────────────────
      // 색 (Color Tokens) — MOZI 브랜드 그린 팔레트
      //  - 로고 캐릭터(연두/그린)에 맞춘 친근한 분위기
      //  - 노년층 친화: 채도 적당히 + 대비 충분 (WCAG AA 4.5:1 이상)
      // ─────────────────────────────────────────
      colors: {
        brand: {
          DEFAULT: "#5BBE65",  // MOZI 메인 그린 (로고 캐릭터 톤)
          hover:   "#4AA854",  // 진한 그린 (hover)
          subtle:  "#E7F5E9",  // 매우 옅은 그린 (배경 강조용)
          deep:    "#2F8E3B",  // 더 진한 그린 (강조 텍스트)
          soft:    "#A7DCAE",  // 부드러운 그린 (보조)
        },
        accent: {
          DEFAULT: "#88D080",  // 새싹 톤
          subtle:  "#F0FAF1",
        },
        ink: {
          strong: "#1A2B22", // 진한 그린-블랙
          DEFAULT: "#243B30",
          weak:    "#566B5C",
          mute:    "#9CA89F",
          invert:  "#FFFFFF",
        },
        surface: {
          DEFAULT: "#FFFFFF",
          muted:   "#F7FAF6",  // 살짝 그린 톤이 도는 페이지 배경
          soft:    "#F2F7F1",
          border:  "#E0E7DD",
        },
        danger: {
          DEFAULT: "#DC2626",
          hover:   "#B91C1C",
          subtle:  "#FEE2E2",
        },
        success: {
          DEFAULT: "#16A34A",
          subtle:  "#DCFCE7",
        },
        warning: {
          DEFAULT: "#D97706",
          subtle:  "#FEF3C7",
        },
      },

      // ─────────────────────────────────────────
      // 폰트 사이즈 — 노년층 본문 18px 기준
      // ─────────────────────────────────────────
      fontSize: {
        "senior-sm":  ["16px", { lineHeight: "1.6" }],
        "senior-base":["18px", { lineHeight: "1.6" }],
        "senior-lg":  ["22px", { lineHeight: "1.5" }],
        "senior-xl":  ["28px", { lineHeight: "1.4" }],
        "senior-2xl": ["32px", { lineHeight: "1.3" }],
        "senior-3xl": ["40px", { lineHeight: "1.2" }],
      },

      // ─────────────────────────────────────────
      // 간격 — 48px 터치 타깃 기준
      // ─────────────────────────────────────────
      spacing: {
        "touch":     "48px",
        "touch-lg":  "56px",
      },

      // ─────────────────────────────────────────
      // 둥근 모서리
      // ─────────────────────────────────────────
      borderRadius: {
        "soft":   "8px",
        "card":   "16px",
        "xl-card":"24px",
        "pill":   "9999px",
      },

      // ─────────────────────────────────────────
      // 최대 높이 — sticky sidebar 등 viewport 기준 계산값
      // ─────────────────────────────────────────
      maxHeight: {
        // 헤더 영역(top-24 = 6rem) 만큼 뺀 viewport 높이.
        // sticky 필터 사이드바가 viewport 안에서 내부 스크롤 가능하도록.
        "sticky-side": "calc(100vh - 6rem)",
      },

      // ─────────────────────────────────────────
      // 그림자
      // ─────────────────────────────────────────
      boxShadow: {
        "card":   "0 2px 8px 0 rgba(47,142,59,0.06), 0 1px 3px -1px rgba(0,0,0,0.04)",
        "card-hover": "0 8px 20px -4px rgba(47,142,59,0.15), 0 4px 8px -2px rgba(0,0,0,0.06)",
        "modal":  "0 20px 40px -10px rgba(0,0,0,0.20)",
        "soft":   "0 1px 2px 0 rgba(0,0,0,0.04)",
      },

      // 배너용 그라데이션은 인라인 클래스에서 from-/to- 토큰 활용
    },
  },
  plugins: [],
};
