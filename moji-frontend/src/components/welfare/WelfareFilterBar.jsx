// components/welfare/WelfareFilterBar.jsx
//
// 검색 페이지의 필터 입력 영역. 키워드/카테고리/지역/제공기관(welfareType chip) + 검색·초기화 버튼.
// 노년층 UX: 각 입력 라벨 명시, 명시적 "검색" 버튼으로만 조회 시작 (자동 디바운스 없음).
//
// 지역은 RegionSelector(시도→시군구 cascading chip)로 입력을 통제 — 자유 텍스트 오타 사고 방지.
// welfareType chip 은 본 페이지 전용이라 공통 컴포넌트로 분리하지 않고 인라인 유지.

import Input from "../common/Input";
import Select from "../common/Select";
import Button from "../common/Button";
import RegionSelector from "./RegionSelector";
import {
  WELFARE_TYPE_LABEL,
  WELFARE_TYPE_ORDER,
} from "../../constants/welfareType";

/**
 * WelfareFilterBar
 *
 * @param {Object} props
 * @param {{ keyword: string, category: string, sido: string, sigungu: string, welfareType: string }} props.filters
 * @param {Array<{ code: string, name: string }>} props.categories - THEME 15종
 * @param {Array<Object>} props.regions - getRegions() 트리 응답
 * @param {(key: string, value: any) => void} props.onChange - 키워드/카테고리/welfareType 단일 키 갱신
 * @param {(pair: { sido: string, sigungu: string }) => void} props.onRegionChange - 시도/시군구 짝 갱신
 * @param {() => void} props.onSubmit - "검색" 클릭
 * @param {() => void} props.onReset - "필터 초기화" 클릭
 * @param {boolean} [props.disabled=false] - 로딩 중 입력 잠금용
 * @returns {JSX.Element}
 */
export default function WelfareFilterBar({
  filters,
  categories,
  regions,
  onChange,
  onRegionChange,
  onSubmit,
  onReset,
  disabled = false,
}) {
  // 카테고리 select 옵션: "전체" + 백엔드 THEME 15종
  const categoryOptions = [
    { value: "", label: "전체" },
    ...(categories || []).map((c) => ({ value: c.code, label: c.name })),
  ];

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit();
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-4 p-4 bg-surface rounded-card border border-surface-border"
    >
      <Input
        label="키워드"
        placeholder="예: 노인 일자리, 의료비"
        value={filters.keyword}
        onChange={(e) => onChange("keyword", e.target.value)}
        disabled={disabled}
      />

      <Select
        label="카테고리"
        options={categoryOptions}
        value={filters.category}
        onChange={(e) => onChange("category", e.target.value)}
        disabled={disabled}
      />

      <RegionSelector
        regions={regions}
        selectedSido={filters.sido}
        selectedSigungu={filters.sigungu}
        onChange={onRegionChange}
        disabled={disabled}
      />

      <div>
        <span
          id="welfareType-label"
          className="block text-senior-base text-ink-strong mb-2"
        >
          제공 기관
        </span>
        <div
          role="group"
          aria-labelledby="welfareType-label"
          className="flex flex-wrap gap-2"
        >
          {WELFARE_TYPE_ORDER.map((type) => {
            const active = filters.welfareType === type;
            return (
              <button
                key={type}
                type="button"
                onClick={() => onChange("welfareType", active ? "" : type)}
                disabled={disabled}
                aria-pressed={active}
                className={
                  "h-12 px-4 rounded-pill border text-senior-base transition-colors " +
                  (active
                    ? "bg-brand text-ink-invert border-brand"
                    : "bg-surface text-ink-strong border-surface-border hover:bg-surface-muted") +
                  " disabled:opacity-50 disabled:cursor-not-allowed"
                }
              >
                {WELFARE_TYPE_LABEL[type]}
              </button>
            );
          })}
        </div>
      </div>

      <div className="flex flex-wrap gap-2 justify-end">
        <Button variant="secondary" type="button" onClick={onReset} disabled={disabled}>
          필터 초기화
        </Button>
        <Button type="submit" disabled={disabled}>
          검색
        </Button>
      </div>
    </form>
  );
}
