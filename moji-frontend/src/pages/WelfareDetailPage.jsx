// pages/WelfareDetailPage.jsx
//
// /welfares/:id 상세 페이지. Optional Auth (비로그인도 진입 가능).
// 단일 GET 조회 + 분기 렌더링이라 별도 훅 없이 페이지 안에서 useEffect 처리.
// API_CLIENT_GUIDE.md §7-1 의 cancel 가드 패턴 그대로 사용.

import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { fetchWelfareDetail } from "../api/welfare";
import WelfareDetailBody from "../components/welfare/WelfareDetailBody";
import Spinner from "../components/common/Spinner";
import ErrorBox from "../components/common/ErrorBox";

/**
 * WelfareDetailPage
 *
 * 진입 경로: 검색 페이지의 WelfareCard 클릭 또는 직링크.
 * 응답이 WELFARE_NOT_FOUND 면 페이지 전체 NotFound, 그 외 에러는 ErrorBox 로 재시도.
 *
 * @returns {JSX.Element}
 */
export default function WelfareDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  // 재시도 버튼은 이 카운터를 증가시켜 useEffect 를 다시 트리거한다 — cleanup 까지 자동 처리.
  const [refetchKey, setRefetchKey] = useState(0);
  const retry = () => setRefetchKey((k) => k + 1);

  /**
   * "목록으로" 동작.
   *  - location.key === "default": 직접 URL 진입 또는 상세 페이지에서 새로고침 직후.
   *    이 경우 브라우저 history 에 검색 페이지가 없을 수 있으므로 state.from(카드 클릭 시 부착)
   *    또는 /welfares 로 fallback.
   *  - 그 외(앱 내 정상 진입): 브라우저 뒤로가기와 동등한 navigate(-1) 로 이전 URL 의
   *    검색 쿼리(필터·페이지)를 그대로 복원.
   */
  const handleBack = () => {
    if (location.key === "default") {
      navigate(location.state?.from || "/welfares");
    } else {
      navigate(-1);
    }
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetchWelfareDetail(id)
      .then((d) => {
        if (!cancelled) setData(d);
      })
      .catch((e) => {
        if (!cancelled) setError(e);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [id, refetchKey]);

  return (
    <main className="max-w-screen-lg mx-auto px-4 md:px-8 py-10">
      <div className="mb-4">
        <button
          type="button"
          onClick={handleBack}
          className="inline-flex items-center h-12 text-senior-base text-ink-weak hover:text-ink-strong"
        >
          ← 목록으로
        </button>
      </div>

      {loading ? (
        <div className="py-10 flex justify-center">
          <Spinner label="복지 정보를 불러오고 있어요" />
        </div>
      ) : error?.errorCode === "WELFARE_NOT_FOUND" ? (
        <div className="py-10 text-center">
          <p className="text-senior-lg text-ink-strong mb-2">
            복지 정보를 찾을 수 없어요.
          </p>
          <p className="text-senior-base text-ink-weak mb-6">
            주소를 다시 확인하시거나 목록에서 다시 선택해주세요.
          </p>
          <Link
            to="/welfares"
            className="inline-flex items-center justify-center h-12 px-6 rounded-pill bg-brand text-ink-invert text-senior-base font-bold hover:bg-brand-hover"
          >
            목록으로
          </Link>
        </div>
      ) : error ? (
        <ErrorBox
          message={
            error.message || "복지 정보를 불러올 수 없어요. 잠시 후 다시 시도해주세요."
          }
          onRetry={retry}
        />
      ) : data ? (
        <WelfareDetailBody welfare={data} />
      ) : null}
    </main>
  );
}

// 본 페이지는 단일 GET 으로 끝나는 단순한 화면이라 별도 훅을 두지 않고 useEffect 로 직접 페칭한다.
// 북마크 토글은 BookmarkButton 이 내부에서 독립적으로 처리하므로 페이지의 data 를 다시 가져올 필요 없음.
