// components/welfare/SeoulDetailSection.jsx
//
// welfareType === "SEOUL" 응답의 seoulDetail 객체를 라벨링해 표시.

import DetailField from "./DetailField";

/**
 * @typedef {Object} SeoulDetail
 * @property {string} [supportType]
 * @property {string} [detailContent]
 * @property {string} [supportCycle]
 * @property {string} [requiredDocuments]
 * @property {string} [contactNumber]
 * @property {string} [detailUrl] - 응답에 포함되지 않을 수도 있음
 */

/**
 * SeoulDetailSection
 *
 * @param {{ detail: SeoulDetail }} props
 * @returns {JSX.Element}
 */
export default function SeoulDetailSection({ detail }) {
  return (
    <section aria-label="서울시 상세">
      <h2 className="text-senior-lg font-bold text-ink-strong mb-3">
        지원 상세 (서울시)
      </h2>
      <dl>
        <DetailField label="지원 유형">{detail.supportType}</DetailField>
        <DetailField label="지원 주기">{detail.supportCycle}</DetailField>
        <DetailField label="상세 내용">{detail.detailContent}</DetailField>
        <DetailField label="제출 서류">{detail.requiredDocuments}</DetailField>
        <DetailField label="문의 전화">{detail.contactNumber}</DetailField>
      </dl>
    </section>
  );
}
