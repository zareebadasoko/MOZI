// pages/HomePage.jsx
//
// / — 메인 진입 페이지. (flowchart 의 "홈화면 진입" 박스)
// 구성 (flowchart 메인 화면 구성 노트):
//  - Hero: MOZI 캐릭터 + 인사말 + 챗봇 CTA
//  - 챗봇 상담 배너
//  - 맞춤 복지 추천 배너 (카테고리 4종)
//  - 신청 방법 안내 / 뉴스
//
// 비로그인도 접근 가능. /chat 등 보호 페이지로 이동할 때 ProtectedRoute 가 막아 /login 으로 보낸다.

import { Link } from "react-router-dom";

const QUICK_CATEGORIES = [
  {
    code: "THM005",
    title: "노인 일자리",
    summary: "활기찬 노후를 위한 일자리 정보를 확인하세요.",
    accent: "from-brand to-brand-deep",
  },
  {
    code: "THM004",
    title: "의료·건강",
    summary: "건강검진, 의료비 지원 등 건강을 챙겨드려요.",
    accent: "from-emerald-500 to-brand-deep",
  },
  {
    code: "THM003",
    title: "주거·생활",
    summary: "주거 안정과 생활비 지원 제도를 모았어요.",
    accent: "from-lime-500 to-brand",
  },
  {
    code: "THM002",
    title: "여가·문화",
    summary: "취미와 문화 활동으로 더 풍요로운 일상을.",
    accent: "from-teal-500 to-brand",
  },
];

const HOW_TO_STEPS = [
  {
    step: "1",
    title: "회원가입 & 프로필 입력",
    text: "연령·지역·상황을 입력하면 더 정확한 추천을 받을 수 있어요.",
  },
  {
    step: "2",
    title: "챗봇과 대화하기",
    text: "원하는 복지를 자연어로 물어보세요. 자주 묻는 질문 버튼도 있어요.",
  },
  {
    step: "3",
    title: "맞춤 복지 확인",
    text: "추천 카드에서 자세한 신청 방법과 대상을 확인할 수 있어요.",
  },
];

const NEWS_ITEMS = [
  {
    tag: "안내",
    title: "2026년 노인 일자리 사업 신청 시작",
    summary: "거주지 행정복지센터에서 신청 가능합니다.",
  },
  {
    tag: "정책",
    title: "기초연금 인상 — 월 최대 33만원",
    summary: "2026년 1월부터 인상된 기초연금이 지급됩니다.",
  },
  {
    tag: "뉴스",
    title: "치매검진 무료 지원 확대",
    summary: "전국 보건소에서 무료 검진을 받을 수 있어요.",
  },
];

/**
 * HomePage
 *
 * @returns {JSX.Element}
 */
export default function HomePage() {
  return (
    <main>
      {/* ──────────────── Hero ──────────────── */}
      <section className="relative overflow-hidden bg-gradient-to-br from-brand-subtle via-surface to-accent-subtle">
        <div className="max-w-screen-xl mx-auto px-4 md:px-8 py-12 md:py-20 grid md:grid-cols-2 gap-8 items-center">
          <div>
            <span className="inline-flex items-center gap-2 px-4 py-2 rounded-pill bg-surface text-brand-deep text-senior-sm font-bold shadow-soft">
              <span className="w-2 h-2 rounded-pill bg-brand" />
              어르신 맞춤 복지 도우미
            </span>
            <h1 className="mt-5 text-senior-2xl md:text-senior-3xl font-extrabold text-ink-strong leading-tight">
              복잡한 복지 정보,
              <br />
              <span className="text-brand-deep">MOZI 와 대화</span>로 쉽게.
            </h1>
            <p className="mt-5 text-senior-base md:text-senior-lg text-ink-weak leading-relaxed">
              나이, 지역, 상황만 알려주시면
              <br className="hidden md:block" />
              나에게 꼭 맞는 복지 혜택을 찾아드려요.
            </p>
            <div className="mt-7 flex flex-col sm:flex-row gap-3">
              <Link
                to="/chat"
                className="h-14 px-8 inline-flex items-center justify-center bg-brand text-ink-invert text-senior-base font-bold rounded-pill shadow-card hover:bg-brand-hover transition-colors"
              >
                챗봇과 대화 시작
              </Link>
              <Link
                to="/categories"
                className="h-14 px-8 inline-flex items-center justify-center bg-surface text-ink-strong text-senior-base font-bold rounded-pill border border-surface-border hover:bg-surface-muted transition-colors"
              >
                복지 분류 보기
              </Link>
            </div>
          </div>

          {/* MOZI 메인 캐릭터 — 인사하는 마스코트 */}
          <div className="relative flex items-center justify-center">
            <div className="absolute inset-0 bg-brand/15 rounded-full blur-3xl" aria-hidden="true" />
            <div className="relative flex flex-col items-center gap-4">
              <img
                src="/reference/mozi-mascot.png"
                alt="MOZI 마스코트"
                className="w-64 md:w-80 h-auto object-contain drop-shadow-xl"
              />
              <div className="px-6 py-4 rounded-xl-card bg-surface shadow-card border border-surface-border max-w-sm break-keep">
                <p className="text-senior-base text-ink-strong font-bold">
                  안녕하세요 어르신!
                </p>
                <p className="text-senior-sm text-ink-weak mt-1">
                  무엇을 도와드릴까요? 편하게 말씀해 주세요.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ──────────────── 챗봇 상담 배너 ──────────────── */}
      <section className="max-w-screen-xl mx-auto px-4 md:px-8 -mt-6 md:-mt-12 relative z-10">
        <Link
          to="/chat"
          className="group block rounded-xl-card overflow-hidden bg-gradient-to-r from-brand to-brand-deep text-ink-invert shadow-card hover:shadow-card-hover transition-shadow"
        >
          <div className="px-6 md:px-10 py-8 md:py-10 flex flex-col md:flex-row md:items-center md:justify-between gap-6">
            <div>
              <p className="text-senior-sm font-bold text-white/80">
                AI 챗봇 상담
              </p>
              <h2 className="mt-2 text-senior-xl md:text-senior-2xl font-extrabold">
                말로 물어보면 척척, 맞춤 복지를 찾아드려요
              </h2>
              <p className="mt-3 text-senior-base text-white/85">
                자주 묻는 질문을 눌러도 좋고, 직접 입력해도 좋아요.
              </p>
            </div>
            <span className="shrink-0 h-14 px-8 inline-flex items-center justify-center bg-surface text-brand-deep text-senior-base font-bold rounded-pill group-hover:bg-brand-subtle transition-colors">
              지금 상담하기 →
            </span>
          </div>
        </Link>
      </section>

      {/* ──────────────── 맞춤 복지 추천 (카테고리 4종) ──────────────── */}
      <section className="max-w-screen-xl mx-auto px-4 md:px-8 py-16">
        <div className="flex items-end justify-between mb-8">
          <div>
            <p className="text-senior-sm font-bold text-brand-deep">맞춤 복지 추천</p>
            <h2 className="mt-2 text-senior-xl md:text-senior-2xl font-extrabold text-ink-strong">
              관심 있는 분야를 골라보세요
            </h2>
          </div>
          <Link
            to="/categories"
            className="hidden sm:inline-flex h-12 px-5 items-center text-senior-sm font-bold text-ink-strong border border-surface-border rounded-pill hover:bg-surface-muted"
          >
            전체 분류 보기 →
          </Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {QUICK_CATEGORIES.map((cat) => (
            <Link
              key={cat.code}
              to={`/welfares?category=${cat.code}`}
              className="group relative rounded-xl-card overflow-hidden bg-surface border border-surface-border shadow-card hover:shadow-card-hover transition-all hover:-translate-y-1"
            >
              <div className={`h-32 bg-gradient-to-br ${cat.accent}`} aria-hidden="true" />
              <div className="p-5">
                <h3 className="text-senior-lg font-extrabold text-ink-strong">
                  {cat.title}
                </h3>
                <p className="mt-2 text-senior-sm text-ink-weak">
                  {cat.summary}
                </p>
                <p className="mt-4 text-senior-sm font-bold text-brand-deep group-hover:underline">
                  자세히 보기 →
                </p>
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* ──────────────── 이용 방법 안내 ──────────────── */}
      <section className="bg-surface border-y border-surface-border">
        <div className="max-w-screen-xl mx-auto px-4 md:px-8 py-16">
          <div className="text-center mb-10">
            <p className="text-senior-sm font-bold text-brand-deep">이용 안내</p>
            <h2 className="mt-2 text-senior-xl md:text-senior-2xl font-extrabold text-ink-strong">
              세 단계로 끝내는 복지 찾기
            </h2>
          </div>

          <ol className="grid md:grid-cols-3 gap-6">
            {HOW_TO_STEPS.map((step) => (
              <li
                key={step.step}
                className="p-6 rounded-card bg-surface-muted border border-surface-border"
              >
                <div className="w-12 h-12 rounded-pill bg-brand text-ink-invert text-senior-lg font-extrabold flex items-center justify-center">
                  {step.step}
                </div>
                <h3 className="mt-4 text-senior-lg font-bold text-ink-strong">
                  {step.title}
                </h3>
                <p className="mt-2 text-senior-base text-ink-weak leading-relaxed">
                  {step.text}
                </p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* ──────────────── 뉴스 / 알림 ──────────────── */}
      <section className="max-w-screen-xl mx-auto px-4 md:px-8 py-16">
        <div className="flex items-end justify-between mb-8">
          <div>
            <p className="text-senior-sm font-bold text-brand-deep">소식 · 안내</p>
            <h2 className="mt-2 text-senior-xl md:text-senior-2xl font-extrabold text-ink-strong">
              최근 복지 소식
            </h2>
          </div>
        </div>

        <ul className="grid md:grid-cols-3 gap-4">
          {NEWS_ITEMS.map((news) => (
            <li
              key={news.title}
              className="p-6 rounded-card bg-surface border border-surface-border shadow-card"
            >
              <span className="inline-flex items-center px-3 py-1 rounded-pill bg-brand-subtle text-brand-deep text-senior-sm font-bold">
                {news.tag}
              </span>
              <h3 className="mt-4 text-senior-lg font-bold text-ink-strong">
                {news.title}
              </h3>
              <p className="mt-2 text-senior-base text-ink-weak leading-relaxed">
                {news.summary}
              </p>
            </li>
          ))}
        </ul>
      </section>
    </main>
  );
}
