// components/welfare/PrivateDetailSection.jsx
//
// welfareType === "PRIVATE" 응답의 privateDetail 객체를 라벨링해 표시.
// startDate ~ endDate 는 한 행 "지원 기간" 으로 합쳐서 보여준다.

import DetailField from "./DetailField";

/**
 * @typedef {Object} PrivateDetail
 * @property {string} [startDate] - "YYYY-MM-DD"
 * @property {string} [endDate]
 * @property {string} [supportDetails]
 * @property {string} [requiredDocuments]
 * @property {string} [contactNumber]
 * @property {string} [contactEmail]
 * @property {string} [detailUrl]
 */

/**
 * 시작일~종료일을 사람이 읽기 좋은 한 줄로 합친다.
 * 한쪽만 있어도 그쪽만 표시.
 */
function formatPeriod(start, end) {
  if (!start && !end) return null;
  if (start && end) return `${start} ~ ${end}`;
  if (start) return `${start}부터`;
  return `${end}까지`;
}

/**
 * PrivateDetailSection
 *
 * @param {{ detail: PrivateDetail }} props
 * @returns {JSX.Element}
 */
export default function PrivateDetailSection({ detail }) {
  return (
    <section aria-label="민간 상세">
      <h2 className="text-senior-lg font-bold text-ink-strong mb-3">
        지원 상세 (민간)
      </h2>
      <dl>
        <DetailField label="지원 기간">
          {formatPeriod(detail.startDate, detail.endDate)}
        </DetailField>
        <DetailField label="지원 내용">{detail.supportDetails}</DetailField>
        <DetailField label="제출 서류">{detail.requiredDocuments}</DetailField>
        <DetailField label="문의 전화">{detail.contactNumber}</DetailField>
        <DetailField label="문의 이메일">{detail.contactEmail}</DetailField>
      </dl>
    </section>
  );
}
