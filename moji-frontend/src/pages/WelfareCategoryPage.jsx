// pages/WelfareCategoryPage.jsx
//
// /categories — 복지 분류 페이지 (flowchart 의 "복지 분류 페이지").
// 백엔드의 THEME 15종을 큼직한 카드로 노출하고, 클릭 시 검색 페이지에 카테고리 필터를 적용해 이동한다.
// 카테고리 마스터는 모듈 캐시(api/category.js)가 세션당 1회만 호출하도록 보장.

import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { fetchCategories } from "../api/category";
import Spinner from "../components/common/Spinner";
import ErrorBox from "../components/common/ErrorBox";

// 카드 색상 — 카테고리 코드 끝자리로 순환. 단조로움을 피하기 위함.
const CARD_ACCENTS = [
  "from-brand to-brand-deep",
  "from-emerald-500 to-brand-deep",
  "from-lime-500 to-brand",
  "from-teal-500 to-brand-deep",
  "from-green-500 to-emerald-600",
];

// 카테고리별 짧은 설명. 백엔드는 name 만 내려주므로 화면 표시용 한 줄 설명은 프론트에서 관리한다.
const CATEGORY_DESCRIPTIONS = {
  THM001: "생계 안정과 기본생활 보장",
  THM002: "여가 · 문화 · 평생교육 활동",
  THM003: "주거비, 임대주택, 주거 환경",
  THM004: "건강검진, 의료비, 요양 지원",
  THM005: "일자리 · 취업 · 창업 지원",
  THM006: "교육비, 학습 지원, 장학금",
  THM007: "보육 · 양육 · 가족 지원",
  THM008: "장애인 복지 서비스",
  THM009: "보훈 · 안보 · 국가유공자",
  THM010: "안전 · 위기 가정 보호",
  THM011: "임신 · 출산 · 산모 지원",
  THM012: "법률 · 행정 · 권리 보호",
  THM013: "농어업인 · 농어촌 지원",
  THM014: "에너지 · 환경 · 생활 지원",
  THM015: "기타 생활편의 · 서비스",
};

/**
 * WelfareCategoryPage
 *
 * @returns {JSX.Element}
 */
export default function WelfareCategoryPage() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  function load() {
    setLoading(true);
    setError(null);
    fetchCategories("THEME")
      .then((list) => setCategories(list || []))
      .catch((err) => setError(err))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
  }, []);

  return (
    <main className="max-w-screen-xl mx-auto px-4 md:px-8 py-10 md:py-14">
      {/* 페이지 헤더 */}
      <header className="mb-10">
        <p className="text-senior-sm font-bold text-brand-deep">복지 분류</p>
        <h1 className="mt-2 text-senior-xl md:text-senior-2xl font-extrabold text-ink-strong">
          관심 있는 분야를 골라 살펴보세요
        </h1>
        <p className="mt-3 text-senior-base text-ink-weak max-w-2xl">
          15개 분야로 정리된 복지 정보입니다. 카드를 누르면 해당 분야의 복지
          목록으로 이동해요.
        </p>
      </header>

      {loading && <Spinner label="분류 정보를 불러오고 있어요" />}

      {error && (
        <ErrorBox
          message="분류 정보를 불러올 수 없어요. 잠시 후 다시 시도해주세요."
          onRetry={load}
        />
      )}

      {!loading && !error && (
        <ul className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {categories.map((cat, idx) => (
            <li key={cat.code}>
              <button
                type="button"
                onClick={() =>
                  navigate(`/welfares?category=${encodeURIComponent(cat.code)}`)
                }
                className="group w-full text-left rounded-xl-card overflow-hidden bg-surface border border-surface-border shadow-card hover:shadow-card-hover hover:-translate-y-1 transition-all"
              >
                <div
                  className={`h-28 bg-gradient-to-br ${CARD_ACCENTS[idx % CARD_ACCENTS.length]} flex items-center justify-center`}
                  aria-hidden="true"
                >
                  <span className="text-ink-invert text-senior-3xl font-extrabold opacity-90">
                    {String(idx + 1).padStart(2, "0")}
                  </span>
                </div>
                <div className="p-5">
                  <h2 className="text-senior-lg font-extrabold text-ink-strong">
                    {cat.name}
                  </h2>
                  <p className="mt-2 text-senior-sm text-ink-weak">
                    {CATEGORY_DESCRIPTIONS[cat.code] || "관련 복지 정보를 확인하세요."}
                  </p>
                  <p className="mt-4 text-senior-sm font-bold text-brand-deep group-hover:underline">
                    복지 목록 보기 →
                  </p>
                </div>
              </button>
            </li>
          ))}
        </ul>
      )}

      {/* 하단 보조 진입점: 키워드 검색으로 유도 */}
      {!loading && !error && (
        <div className="mt-12 p-6 md:p-8 rounded-xl-card bg-brand-subtle flex flex-col md:flex-row md:items-center md:justify-between gap-6">
          <div className="flex items-center gap-4">
            <img
              src="/reference/mozi-active.png"
              alt=""
              className="h-20 w-auto object-contain shrink-0"
            />
            <div>
              <h3 className="text-senior-lg font-extrabold text-ink-strong">
                원하는 복지를 모르시겠나요?
              </h3>
              <p className="mt-1 text-senior-base text-ink-weak">
                챗봇과 대화하면 상황에 맞는 복지를 찾아드려요.
              </p>
            </div>
          </div>
          <Link
            to="/chat"
            className="shrink-0 h-14 px-8 inline-flex items-center justify-center bg-brand text-ink-invert text-senior-base font-bold rounded-pill hover:bg-brand-hover transition-colors"
          >
            챗봇과 대화하기
          </Link>
        </div>
      )}
    </main>
  );
}
