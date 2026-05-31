// components/welfare/LocalDetailSection.jsx
//
// welfareType === "LOCAL" 응답의 localDetail 객체를 라벨링해 표시.

import DetailField from "./DetailField";

/**
 * @typedef {Object} LocalDetail
 * @property {string} [regionName]
 * @property {number|null} [supportYear]
 * @property {string} [supportType]
 * @property {string} [selectionCriteria]
 * @property {string} [supportDetails]
 * @property {string} [supportCycle]
 * @property {string} [contact]
 * @property {string} [detailUrl]
 */

/**
 * LocalDetailSection
 *
 * @param {{ detail: LocalDetail }} props
 * @returns {JSX.Element}
 */
export default function LocalDetailSection({ detail }) {
  return (
    <section aria-label="지자체 상세">
      <h2 className="text-senior-lg font-bold text-ink-strong mb-3">
        지원 상세 (지자체)
      </h2>
      <dl>
        <DetailField label="지역">{detail.regionName}</DetailField>
        <DetailField label="지원 연도">{detail.supportYear}</DetailField>
        <DetailField label="지원 유형">{detail.supportType}</DetailField>
        <DetailField label="지원 주기">{detail.supportCycle}</DetailField>
        <DetailField label="지원 내용">{detail.supportDetails}</DetailField>
        <DetailField label="선정 기준">{detail.selectionCriteria}</DetailField>
        <DetailField label="문의처">{detail.contact}</DetailField>
      </dl>
    </section>
  );
}
