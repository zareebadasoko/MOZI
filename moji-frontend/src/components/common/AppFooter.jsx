// components/common/AppFooter.jsx
//
// 모든 페이지 하단의 공통 푸터. 서비스 소개 + 빠른 링크 + 안내 문구.

import { Link } from "react-router-dom";

/**
 * AppFooter
 *
 * @returns {JSX.Element}
 */
export default function AppFooter() {
  return (
    <footer className="bg-ink-strong text-ink-invert mt-16">
      <div className="max-w-screen-xl mx-auto px-4 md:px-8 py-10 grid gap-8 md:grid-cols-3">
        <div>
          <div className="flex items-center gap-2 mb-3">
            <img
              src="/reference/mozi-avatar.png"
              alt=""
              className="h-10 w-10 object-contain"
            />
            <span className="text-senior-lg font-extrabold">MOZI</span>
          </div>
          <p className="text-senior-sm text-white/75 leading-relaxed">
            어르신을 위한 맞춤 복지 도우미.
            <br />
            챗봇과 대화하며 나에게 맞는 복지를 쉽게 찾아보세요.
          </p>
        </div>

        <div>
          <h3 className="text-senior-base font-bold mb-3">바로가기</h3>
          <ul className="space-y-2 text-senior-sm text-white/80">
            <li>
              <Link to="/chat" className="hover:underline">
                챗봇 상담
              </Link>
            </li>
            <li>
              <Link to="/categories" className="hover:underline">
                복지 분류
              </Link>
            </li>
            <li>
              <Link to="/welfares" className="hover:underline">
                복지 찾기
              </Link>
            </li>
            <li>
              <Link to="/me" className="hover:underline">
                마이페이지
              </Link>
            </li>
          </ul>
        </div>

        <div>
          <h3 className="text-senior-base font-bold mb-3">안내</h3>
          <p className="text-senior-sm text-white/75 leading-relaxed">
            본 서비스는 졸업 프로젝트 시연용으로 운영됩니다. 실제 신청은 각
            기관 공식 창구를 이용해 주세요.
          </p>
        </div>
      </div>

      <div className="border-t border-white/10">
        <div className="max-w-screen-xl mx-auto px-4 md:px-8 py-4 text-senior-sm text-white/60">
          © {new Date().getFullYear()} MOZI Project. All rights reserved.
        </div>
      </div>
    </footer>
  );
}
