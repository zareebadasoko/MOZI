// pages/NotFoundPage.jsx
//
// 라우터의 * 경로. 잘못된 URL 진입 시 표시.

import { Link } from "react-router-dom";

/**
 * NotFoundPage
 *
 * @returns {JSX.Element}
 */
export default function NotFoundPage() {
  return (
    <main className="min-h-[70vh] flex items-center justify-center px-4 py-12 text-center">
      <div>
        <img
          src="/reference/mozi-active.png"
          alt=""
          className="h-32 w-auto mx-auto object-contain opacity-90"
        />
        <p className="mt-6 text-senior-2xl md:text-senior-3xl font-extrabold text-brand-deep">
          404
        </p>
        <h1 className="mt-3 text-senior-xl font-extrabold text-ink-strong">
          페이지를 찾을 수 없어요
        </h1>
        <p className="mt-3 text-senior-base text-ink-weak max-w-md mx-auto">
          요청하신 주소가 잘못되었거나 페이지가 옮겨졌을 수 있어요.
        </p>
        <Link
          to="/"
          className="mt-8 inline-flex items-center h-14 px-8 rounded-pill bg-brand text-ink-invert text-senior-base font-bold hover:bg-brand-hover transition-colors"
        >
          홈으로 가기
        </Link>
      </div>
    </main>
  );
}
