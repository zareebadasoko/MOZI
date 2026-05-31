// components/welfare/CentralDetailSection.jsx
//
// welfareType === "CENTRAL" 응답의 centralDetail 객체를 라벨링해 표시.
// 빈 필드는 DetailField 가 자동 생략한다.

import DetailField from "./DetailField";

/**
 * @typedef {Object} CentralDetail
 * @property {number|null} [supportYear]
 * @property {string} [supportType]
 * @property {string} [selectionCriteria]
 * @property {string} [supportDetails]
 * @property {string} [supportCycle]
 * @property {string} [processSteps]
 * @property {string} [contactNumber]
 * @property {string} [detailUrl]
 */

/**
 * CentralDetailSection
 *
 * @param {{ detail: CentralDetail }} props
 * @returns {JSX.Element}
 */
export default function CentralDetailSection({ detail }) {
  return (
    <section aria-label="중앙부처 상세">
      <h2 className="text-senior-lg font-bold text-ink-strong mb-3">
        지원 상세 (중앙부처)
      </h2>
      <dl>
        <DetailField label="지원 연도">{detail.supportYear}</DetailField>
        <DetailField label="지원 유형">{detail.supportType}</DetailField>
        <DetailField label="지원 주기">{detail.supportCycle}</DetailField>
        <DetailField label="지원 내용">{detail.supportDetails}</DetailField>
        <DetailField label="선정 기준">{detail.selectionCriteria}</DetailField>
        <DetailField label="처리 절차">{detail.processSteps}</DetailField>
        <DetailField label="문의 전화">{detail.contactNumber}</DetailField>
      </dl>
    </section>
  );
}
