// components/welfare/RegionSelector.jsx
//
// 시도 chip → (선택 시) 시군구 chip 로 이어지는 cascading 지역 선택 UI.
// 검색 페이지 전용이라 welfare/ 폴더에 둠. WelfareFilterBar 의 welfareType chip 과 동일한
// 스타일 토큰을 사용해 시각 일관성을 맞춘다.
//
// 정책:
//  - 첫 chip "전국" = 시도 미선택. 활성 chip 은 라디오처럼 한 개만.
//  - 시도가 "전국" 외로 선택되면 그 아래에 시군구 chip 묶음이 나타나고, 첫 chip 은 "전체"(=시군구 미선택).
//  - 시도를 다른 값으로 바꾸면 시군구 선택은 자동으로 비워진다(onChange 가 짝으로 흘려보냄).
//  - 세종특별자치시는 sigungus 가 [{ code: null, name: null }] 한 행으로 옴 → 비활성 안내 한 줄.

/**
 * @typedef {Object} Sigungu
 * @property {string | null} code
 * @property {string | null} name
 *
 * @typedef {Object} SidoEntry
 * @property {string} sidoCode
 * @property {string} sidoName
 * @property {Array<Sigungu>} sigungus
 *
 * @typedef {{ sido: string, sigungu: string }} RegionPair
 */

/**
 * RegionSelector
 *
 * @param {Object} props
 * @param {Array<SidoEntry>} props.regions - getRegions() 응답 (시도별 그루핑 트리)
 * @param {string} props.selectedSido - 시도명. "" 이면 "전국"
 * @param {string} props.selectedSigungu - 시군구명. "" 이면 "전체"
 * @param {(next: RegionPair) => void} props.onChange - 시도/시군구 짝으로 전달
 * @param {boolean} [props.disabled=false]
 * @returns {JSX.Element}
 */
export default function RegionSelector({
  regions = [],
  selectedSido,
  selectedSigungu,
  onChange,
  disabled = false,
}) {
  const isSidoSelected = selectedSido !== "";
  // 현재 선택된 시도의 시군구 목록 (세종은 [{code:null,name:null}] 한 행)
  const currentSido = regions.find((r) => r.sidoName === selectedSido) || null;
  const sigunguList = currentSido?.sigungus || [];
  // 세종처럼 "시군구 없음" 마커가 들어있는지
  const hasNoSigungu =
    sigunguList.length > 0 && sigunguList.every((s) => s.code === null);

  return (
    <div>
      <span
        id="region-sido-label"
        className="block text-senior-base text-ink-strong mb-2"
      >
        지역 (시도)
      </span>
      <div
        role="group"
        aria-labelledby="region-sido-label"
        className="flex flex-wrap gap-2"
      >
        {/* "전국" chip — 시도 미선택 상태 */}
        <ChipButton
          label="전국"
          active={!isSidoSelected}
          disabled={disabled}
          onClick={() => onChange({ sido: "", sigungu: "" })}
        />
        {regions.map((r) => (
          <ChipButton
            key={r.sidoCode}
            label={r.sidoName}
            active={r.sidoName === selectedSido}
            disabled={disabled}
            onClick={() =>
              // 시도 변경 시 시군구는 항상 "전체"로 리셋
              onChange({ sido: r.sidoName, sigungu: "" })
            }
          />
        ))}
      </div>

      {/* 시도가 선택된 경우에만 시군구 영역 노출 */}
      {isSidoSelected && (
        <div className="mt-4">
          <span
            id="region-sigungu-label"
            className="block text-senior-base text-ink-strong mb-2"
          >
            {selectedSido}의 시군구
          </span>
          {hasNoSigungu ? (
            <p className="text-senior-base text-ink-weak">
              {selectedSido}는 시군구 구분이 없어요.
            </p>
          ) : (
            <div
              role="group"
              aria-labelledby="region-sigungu-label"
              className="flex flex-wrap gap-2 max-h-48 overflow-y-auto pr-1"
            >
              {/* "전체" chip — 시군구 미선택 = 시도 단위 검색 */}
              <ChipButton
                label="전체"
                active={selectedSigungu === ""}
                disabled={disabled}
                onClick={() =>
                  onChange({ sido: selectedSido, sigungu: "" })
                }
              />
              {sigunguList.map((s) => (
                <ChipButton
                  key={s.code}
                  label={s.name}
                  active={s.name === selectedSigungu}
                  disabled={disabled}
                  onClick={() =>
                    onChange({ sido: selectedSido, sigungu: s.name })
                  }
                />
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

/**
 * 내부 chip 버튼. welfareType chip 스타일과 동일.
 * 별도 export 하지 않는다 — RegionSelector 전용.
 *
 * @param {Object} props
 * @param {string} props.label
 * @param {boolean} props.active
 * @param {boolean} [props.disabled]
 * @param {() => void} props.onClick
 */
function ChipButton({ label, active, disabled, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
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
      {label}
    </button>
  );
}
