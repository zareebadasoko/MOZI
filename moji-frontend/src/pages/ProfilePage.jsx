// pages/ProfilePage.jsx
//
// /me/profile — 내 정보 수정. Protected.
// 백엔드는 sidoCode/sigunguCode + incomeType/householdType (enum) + boolean 4종을 받는다.
// 옵션 C 부분 갱신: 요청에 없는 키는 변경 안 됨. 빈 값(코드/enum)은 요청 바디에서 제외 → 미변경.
// boolean 4종은 항상 명시 전송 (false 도 사용자의 선택).
//
// gender 는 응답에는 있지만 PUT 으로 갱신 불가 → 본 폼에 표시하지 않음.
// birthDate 는 PUT 갱신 가능 (LocalDate, nullable) — 본 폼의 type="date" 입력으로 처리.
//
// 명세 출처: docs/SCREEN_SPEC.md §10 + FRONTEND_MIGRATION_NOTES.md.

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Button from "../components/common/Button";
import Select from "../components/common/Select";
import Toggle from "../components/common/Toggle";
import Spinner from "../components/common/Spinner";
import ErrorBox from "../components/common/ErrorBox";
import { useToast } from "../hooks/useToast";
import { getMyProfile, updateMyProfile } from "../api/user";
import { getRegions } from "../api/region";
import {
  INCOME_TYPE_LABEL,
  INCOME_TYPE_ORDER,
  HOUSEHOLD_TYPE_LABEL,
  HOUSEHOLD_TYPE_ORDER,
} from "../constants/profileEnums";

/** 폼 초기값(빈 프로필). boolean 4종은 false 가 기본. birthDate 는 빈 문자열. */
const EMPTY_FORM = {
  birthDate: "",
  sidoCode: "",
  sigunguCode: "",
  incomeType: "",
  householdType: "",
  isDisabled: false,
  isMultiChild: false,
  isMulticulturalNorthDefector: false,
  isVeteran: false,
};

// 생년월일 최소·최대 — 1920-01-01 ~ 오늘. 한 번만 계산.
const MIN_BIRTH_DATE = "1920-01-01";
const TODAY_ISO = new Date().toISOString().slice(0, 10);

/**
 * 폼 state → PUT body 변환.
 *  - 코드/enum 빈 문자열은 키 자체에서 제외 (옵션 C: 변경 안 함)
 *  - boolean 4종은 항상 명시 (true/false 모두 사용자의 선택)
 */
function formToPutBody(form) {
  const body = {
    isDisabled: !!form.isDisabled,
    isMultiChild: !!form.isMultiChild,
    isMulticulturalNorthDefector: !!form.isMulticulturalNorthDefector,
    isVeteran: !!form.isVeteran,
  };
  if (form.birthDate) body.birthDate = form.birthDate;
  if (form.sidoCode) body.sidoCode = form.sidoCode;
  if (form.sigunguCode) body.sigunguCode = form.sigunguCode;
  if (form.incomeType) body.incomeType = form.incomeType;
  if (form.householdType) body.householdType = form.householdType;
  return body;
}

/**
 * 응답 프로필 → 폼 state 변환. null 은 빈 문자열로(Select 가 "선택 안 함" 으로 표시).
 */
function profileToForm(p) {
  return {
    birthDate: p?.birthDate ?? "",
    sidoCode: p?.sidoCode ?? "",
    sigunguCode: p?.sigunguCode ?? "",
    incomeType: p?.incomeType ?? "",
    householdType: p?.householdType ?? "",
    isDisabled: !!p?.isDisabled,
    isMultiChild: !!p?.isMultiChild,
    isMulticulturalNorthDefector: !!p?.isMulticulturalNorthDefector,
    isVeteran: !!p?.isVeteran,
  };
}

/**
 * 시도/시군구 cascading Select 2개. ProfilePage 내부 헬퍼 (재사용처 없음).
 */
function ProfileRegionFields({ regions, form, setForm, fieldErrors }) {
  const sidoOptions = [
    { value: "", label: "선택 안 함" },
    ...regions.map((r) => ({ value: r.sidoCode, label: r.sidoName })),
  ];

  const currentSido = regions.find((r) => r.sidoCode === form.sidoCode) || null;
  const sigungus = currentSido?.sigungus || [];
  const hasNoSigungu =
    sigungus.length > 0 && sigungus.every((s) => s.code === null);

  const sigunguOptions = [
    { value: "", label: hasNoSigungu ? "시군구 구분 없음" : "선택 안 함" },
    ...sigungus
      .filter((s) => s.code !== null)
      .map((s) => ({ value: s.code, label: s.name })),
  ];

  const onSidoChange = (e) => {
    // 시도 변경 시 시군구는 즉시 초기화 (cascading)
    setForm((prev) => ({
      ...prev,
      sidoCode: e.target.value,
      sigunguCode: "",
    }));
  };

  const onSigunguChange = (e) => {
    setForm((prev) => ({ ...prev, sigunguCode: e.target.value }));
  };

  return (
    <>
      <Select
        label="거주 시도"
        options={sidoOptions}
        value={form.sidoCode}
        onChange={onSidoChange}
        error={fieldErrors.sidoCode}
      />
      <Select
        label="거주 시군구"
        options={sigunguOptions}
        value={form.sigunguCode}
        onChange={onSigunguChange}
        disabled={!form.sidoCode || hasNoSigungu}
        hint={
          !form.sidoCode
            ? "시도를 먼저 선택해주세요."
            : hasNoSigungu
              ? "이 시도는 시군구 구분이 없어요."
              : undefined
        }
        error={fieldErrors.sigunguCode}
      />
    </>
  );
}

/**
 * ProfilePage
 *
 * @returns {JSX.Element}
 */
export default function ProfilePage() {
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [form, setForm] = useState(EMPTY_FORM);
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});

  // 재시도 트리거용 카운터 — useEffect 가 cleanup 까지 자동 처리
  const [refetchKey, setRefetchKey] = useState(0);
  const retry = () => setRefetchKey((k) => k + 1);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    Promise.all([getMyProfile(), getRegions()])
      .then(([profile, regionTree]) => {
        if (cancelled) return;
        setRegions(regionTree);
        if (profile?.isCompleted) {
          setForm(profileToForm(profile));
        } else {
          setForm(EMPTY_FORM);
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [refetchKey]);

  const setFieldOnForm = (key) => (e) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }));

  const setToggleOnForm = (key) => (next) =>
    setForm((prev) => ({ ...prev, [key]: next }));

  async function handleSubmit(e) {
    e.preventDefault();
    if (submitting) return;
    setFieldErrors({});
    setSubmitting(true);
    try {
      const updated = await updateMyProfile(formToPutBody(form));
      // 응답이 갱신된 프로필이므로 폼 state 도 동기화 (백엔드가 정규화한 값 반영)
      setForm(profileToForm(updated));
      showToast({ kind: "success", message: "저장되었어요." });
    } catch (err) {
      if (err.errorCode === "VALIDATION_FAILED") {
        setFieldErrors(err.fields || {});
        showToast({ kind: "error", message: "입력값을 확인해주세요." });
      } else if (err.errorCode === "INVALID_REGION_CODE") {
        // 시도-시군구 불일치 등 — 백엔드 메시지를 그대로 토스트 + 시군구만 초기화
        showToast({ kind: "error", message: err.message });
        setForm((prev) => ({ ...prev, sigunguCode: "" }));
      } else {
        showToast({
          kind: "error",
          message: err.message || "잠시 후 다시 시도해주세요.",
        });
      }
    } finally {
      setSubmitting(false);
    }
  }

  // 소득 / 가구 Select 옵션 (라벨 매핑 + "선택 안 함")
  const incomeOptions = [
    { value: "", label: "선택 안 함" },
    ...INCOME_TYPE_ORDER.map((v) => ({ value: v, label: INCOME_TYPE_LABEL[v] })),
  ];
  const householdOptions = [
    { value: "", label: "선택 안 함" },
    ...HOUSEHOLD_TYPE_ORDER.map((v) => ({
      value: v,
      label: HOUSEHOLD_TYPE_LABEL[v],
    })),
  ];

  return (
    <main className="max-w-screen-md mx-auto px-4 py-6">
      <div className="mb-4">
        <button
          type="button"
          onClick={() => navigate("/me")}
          className="inline-flex items-center h-12 text-senior-base text-ink-weak hover:text-ink-strong"
        >
          ← 마이페이지
        </button>
      </div>

      <h1 className="text-senior-xl font-bold text-ink-strong mb-2">
        내 정보 수정
      </h1>
      <p className="text-senior-base text-ink-weak mb-6">
        입력하실수록 추천이 더 정확해집니다.
      </p>

      {loading ? (
        <div className="py-10 flex justify-center">
          <Spinner label="내 정보를 불러오고 있어요" />
        </div>
      ) : error ? (
        <ErrorBox
          message={
            error.message ||
            "내 정보를 불러올 수 없어요. 잠시 후 다시 시도해주세요."
          }
          onRetry={retry}
        />
      ) : (
        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
          <section
            aria-label="생년월일"
            className="flex flex-col gap-4 p-4 bg-surface rounded-card border border-surface-border"
          >
            <label className="flex flex-col gap-2">
              <span className="text-senior-base font-bold text-ink-strong">
                생년월일
              </span>
              <input
                type="date"
                value={form.birthDate}
                onChange={setFieldOnForm("birthDate")}
                min={MIN_BIRTH_DATE}
                max={TODAY_ISO}
                aria-invalid={fieldErrors.birthDate ? "true" : undefined}
                className="h-12 px-4 rounded-soft border border-surface-border text-senior-base text-ink-strong focus:outline-none focus:ring-2 focus:ring-brand"
              />
              {fieldErrors.birthDate ? (
                <span className="text-senior-sm text-danger">
                  {fieldErrors.birthDate}
                </span>
              ) : (
                <span className="text-senior-sm text-ink-weak">
                  예: 1948-03-01 (선택 입력)
                </span>
              )}
            </label>
          </section>

          <section
            aria-label="거주 지역"
            className="flex flex-col gap-4 p-4 bg-surface rounded-card border border-surface-border"
          >
            <ProfileRegionFields
              regions={regions}
              form={form}
              setForm={setForm}
              fieldErrors={fieldErrors}
            />
          </section>

          <section
            aria-label="소득·가구 정보"
            className="flex flex-col gap-4 p-4 bg-surface rounded-card border border-surface-border"
          >
            <Select
              label="소득 유형"
              options={incomeOptions}
              value={form.incomeType}
              onChange={setFieldOnForm("incomeType")}
              error={fieldErrors.incomeType}
            />
            <Select
              label="가구 형태"
              options={householdOptions}
              value={form.householdType}
              onChange={setFieldOnForm("householdType")}
              error={fieldErrors.householdType}
            />
          </section>

          <section
            aria-label="추가 상황"
            className="flex flex-col gap-3"
          >
            <Toggle
              label="장애 등록"
              hint="등록된 장애가 있으신 경우 켜주세요."
              checked={form.isDisabled}
              onChange={setToggleOnForm("isDisabled")}
            />
            <Toggle
              label="다자녀 가정"
              checked={form.isMultiChild}
              onChange={setToggleOnForm("isMultiChild")}
            />
            <Toggle
              label="다문화·탈북민"
              checked={form.isMulticulturalNorthDefector}
              onChange={setToggleOnForm("isMulticulturalNorthDefector")}
            />
            <Toggle
              label="보훈 대상자"
              checked={form.isVeteran}
              onChange={setToggleOnForm("isVeteran")}
            />
          </section>

          <div className="flex flex-col-reverse sm:flex-row gap-2 justify-end mt-2">
            <Button
              variant="secondary"
              type="button"
              onClick={() => navigate("/me")}
              disabled={submitting}
            >
              취소
            </Button>
            <Button type="submit" large disabled={submitting}>
              {submitting ? "저장 중…" : "저장"}
            </Button>
          </div>
        </form>
      )}
    </main>
  );
}
