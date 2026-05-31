// components/welfare/WelfareDetailBody.jsx
//
// 상세 페이지 본문 조립 컴포넌트. 페이지 파일을 단순하게 유지하려고 분리.
// 응답에서 welfareType 별로 정확히 1개의 자식 detail 만 포함되므로(NON_NULL 직렬화)
// 분기에서 해당 자식이 있을 때만 섹션을 렌더한다.

import BookmarkButton from "./BookmarkButton";
import DetailField from "./DetailField";
import CentralDetailSection from "./CentralDetailSection";
import LocalDetailSection from "./LocalDetailSection";
import PrivateDetailSection from "./PrivateDetailSection";
import SeoulDetailSection from "./SeoulDetailSection";
import { WELFARE_TYPE_LABEL } from "../../constants/welfareType";

/**
 * welfareType 별로 분기된 자식 detail 섹션을 렌더한다.
 * 응답 모양에 맞춰 4 종류 중 정확히 1개만 그려진다.
 */
function SourceSpecificSection({ welfare }) {
  switch (welfare.welfareType) {
    case "CENTRAL":
      return welfare.centralDetail ? (
        <CentralDetailSection detail={welfare.centralDetail} />
      ) : null;
    case "LOCAL":
      return welfare.localDetail ? (
        <LocalDetailSection detail={welfare.localDetail} />
      ) : null;
    case "PRIVATE":
      return welfare.privateDetail ? (
        <PrivateDetailSection detail={welfare.privateDetail} />
      ) : null;
    case "SEOUL":
      return welfare.seoulDetail ? (
        <SeoulDetailSection detail={welfare.seoulDetail} />
      ) : null;
    default:
      return null;
  }
}

/**
 * 응답에서 출처별 자식 안의 detailUrl 을 꺼낸다. 없으면 null.
 */
function pickDetailUrl(welfare) {
  return (
    welfare.centralDetail?.detailUrl ||
    welfare.localDetail?.detailUrl ||
    welfare.privateDetail?.detailUrl ||
    welfare.seoulDetail?.detailUrl ||
    null
  );
}

/**
 * WelfareDetailBody
 *
 * @param {Object} props
 * @param {Object} props.welfare - WelfareDetailDto 응답 그대로
 * @returns {JSX.Element}
 */
export default function WelfareDetailBody({ welfare }) {
  const {
    id,
    title,
    summary,
    welfareType,
    organizationName,
    targetAudience,
    applicationMethod,
    categories,
    isBookmarked,
  } = welfare;

  const detailUrl = pickDetailUrl(welfare);
  const visibleCategories = categories || [];

  return (
    <article className="flex flex-col gap-6">
      <header className="flex flex-col gap-3">
        <span className="self-start inline-flex items-center px-3 py-1 rounded-pill bg-brand-subtle text-brand text-senior-sm font-medium">
          {WELFARE_TYPE_LABEL[welfareType] || welfareType}
        </span>
        <h1 className="text-senior-xl font-bold text-ink-strong">{title}</h1>
        {summary && (
          <p className="text-senior-base text-ink-weak whitespace-pre-line">
            {summary}
          </p>
        )}
        <BookmarkButton
          welfareId={id}
          initialBookmarked={isBookmarked}
        />
      </header>

      {visibleCategories.length > 0 && (
        <ul className="flex flex-wrap gap-2">
          {visibleCategories.map((cat) => (
            <li
              key={cat.code}
              className="px-2 py-1 rounded-pill bg-surface-muted text-ink-weak text-senior-sm"
            >
              {cat.name}
            </li>
          ))}
        </ul>
      )}

      <section
        aria-label="기본 정보"
        className="bg-surface rounded-card border border-surface-border p-5"
      >
        <dl>
          <DetailField label="지원 대상">{targetAudience}</DetailField>
          <DetailField label="신청 방법">{applicationMethod}</DetailField>
          <DetailField label="담당 기관">{organizationName}</DetailField>
        </dl>
      </section>

      <section
        aria-label="출처별 상세"
        className="bg-surface rounded-card border border-surface-border p-5"
      >
        <SourceSpecificSection welfare={welfare} />
      </section>

      {detailUrl && (
        <div className="flex flex-col items-center gap-2">
          <a
            href={detailUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center justify-center h-14 px-8 rounded-soft bg-brand text-ink-invert text-senior-base font-medium hover:bg-brand-hover"
          >
            자세히 보기 →
          </a>
          <p className="text-senior-sm text-ink-weak">
            복지로/서울복지포털 등 외부 사이트로 이동해요.
          </p>
        </div>
      )}
    </article>
  );
}
