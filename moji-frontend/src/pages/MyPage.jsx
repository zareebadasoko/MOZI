// pages/MyPage.jsx
//
// /me — 마이페이지 허브. Protected.
//
// 구성:
//  · WelcomeCard: 큰 그라데이션 배너 + 마스코트 + 프로필 요약 칩 (지역/가구/소득/자격)
//  · StatStrip:    저장한 복지 수 등 빠른 통계 3종 (저장 / 챗봇 바로가기 / 복지 둘러보기)
//  · 메뉴 카드 4종: 내 정보 수정 / 비밀번호 변경 / 저장한 복지 / 로그아웃
//  · 회원 탈퇴: 별도 영역
//
// 호출 API:
//  · GET /api/users/me/profile        (마운트 시 1회) — isCompleted + 프로필 필드
//  · GET /api/regions                  (마운트 시 1회, 모듈 캐시)
//  · GET /api/bookmarks?page=0&size=1  (마운트 시 1회) — totalCount 만 활용
//
// 회원 탈퇴는 Modal 로 확인. 성공 시 토큰 클리어 + /login.

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Card from "../components/common/Card";
import Button from "../components/common/Button";
import Modal from "../components/common/Modal";
import { useAuth } from "../contexts/AuthContext";
import { useToast } from "../hooks/useToast";
import { getMyProfile, withdraw } from "../api/user";
import { logout as apiLogout } from "../api/auth";
import { fetchBookmarks } from "../api/bookmark";
import { getRegions } from "../api/region";
import {
  INCOME_TYPE_LABEL,
  HOUSEHOLD_TYPE_LABEL,
} from "../constants/profileEnums";

/** 4종 boolean 자격 → 한글 라벨. true 인 것만 칩으로 노출. */
const QUALIFICATION_LABEL = {
  isDisabled: "장애인",
  isMultiChild: "다자녀가구",
  isMulticulturalNorthDefector: "다문화 · 북한이탈",
  isVeteran: "국가유공자",
};

/**
 * sidoCode / sigunguCode 와 region 트리에서 한글 거주지 라벨을 만든다.
 * 시도만 있으면 "서울특별시", 시군구까지 있으면 "서울특별시 종로구".
 * 매핑 실패 시 null.
 *
 * @param {string|undefined|null} sidoCode
 * @param {string|undefined|null} sigunguCode
 * @param {Array} regionTree
 * @returns {string|null}
 */
function buildRegionLabel(sidoCode, sigunguCode, regionTree) {
  if (!sidoCode || !regionTree?.length) return null;
  const sido = regionTree.find((s) => s.sidoCode === sidoCode);
  if (!sido) return null;
  if (!sigunguCode) return sido.sidoName;
  const sigungu = sido.sigungus?.find((g) => g.code === sigunguCode);
  return sigungu?.name ? `${sido.sidoName} ${sigungu.name}` : sido.sidoName;
}

/**
 * MyPage
 *
 * @returns {JSX.Element}
 */
export default function MyPage() {
  const navigate = useNavigate();
  const { logout: contextLogout } = useAuth();
  const { showToast } = useToast();

  // 프로필 응답 전체를 보관 (요약 칩 렌더용)
  const [profile, setProfile] = useState(null); // null = 로딩 중
  const [regionTree, setRegionTree] = useState([]);
  const [bookmarkCount, setBookmarkCount] = useState(null); // null = 로딩 중
  const [withdrawOpen, setWithdrawOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let cancelled = false;

    // 프로필 — 핵심 정보
    getMyProfile()
      .then((data) => {
        if (!cancelled) setProfile(data || { isCompleted: false });
      })
      .catch((err) => {
        if (cancelled) return;
        if (err.errorCode === "USER_NOT_FOUND") {
          contextLogout();
          navigate("/login", { replace: true });
          return;
        }
        // 그 외 조회 실패는 메뉴는 그대로 노출 (치명적 아님)
        setProfile({ isCompleted: true });
      });

    // 지역 트리 — 거주지 라벨 변환용 (모듈 캐시라 실제로는 세션당 1회만 호출됨)
    getRegions()
      .then((tree) => {
        if (!cancelled) setRegionTree(tree || []);
      })
      .catch(() => {
        // 지역 매핑 실패해도 코드는 표시되지 않을 뿐 — 무시
      });

    // 북마크 개수 — totalCount 만 사용
    fetchBookmarks({ page: 0, size: 1 })
      .then((res) => {
        if (!cancelled) setBookmarkCount(res?.totalCount ?? 0);
      })
      .catch(() => {
        if (!cancelled) setBookmarkCount(0);
      });

    return () => {
      cancelled = true;
    };
  }, [navigate, contextLogout]);

  async function handleLogout() {
    try {
      await apiLogout();
    } catch {
      // best-effort
    }
    navigate("/login", { replace: true });
    contextLogout();
  }

  async function handleWithdraw() {
    if (submitting) return;
    setSubmitting(true);
    try {
      await withdraw();
      navigate("/login", { replace: true });
      contextLogout();
      showToast({ kind: "success", message: "탈퇴되었어요." });
    } catch (err) {
      setSubmitting(false);
      showToast({
        kind: "error",
        message: err.message || "잠시 후 다시 시도해주세요.",
      });
    }
  }

  // 표시용 파생 값 — 프로필 미입력 시 모두 null
  const isCompleted = profile?.isCompleted;
  const regionLabel = isCompleted
    ? buildRegionLabel(profile.sidoCode, profile.sigunguCode, regionTree)
    : null;
  const householdLabel = isCompleted
    ? HOUSEHOLD_TYPE_LABEL[profile.householdType]
    : null;
  const incomeLabel = isCompleted
    ? INCOME_TYPE_LABEL[profile.incomeType]
    : null;
  const qualifications = isCompleted
    ? Object.keys(QUALIFICATION_LABEL).filter((key) => profile[key])
    : [];

  return (
    <main className="max-w-screen-xl mx-auto px-4 md:px-8 py-10 md:py-14">
      {/* ───── Welcome Card ───── */}
      <section className="relative overflow-hidden rounded-xl-card bg-gradient-to-br from-brand-subtle via-surface to-accent-subtle border border-brand-soft shadow-card mb-6">
        {/* 배경 블러 데코 */}
        <div
          aria-hidden="true"
          className="absolute -top-20 -right-20 w-72 h-72 bg-brand/20 rounded-full blur-3xl"
        />
        <div
          aria-hidden="true"
          className="absolute -bottom-24 -left-16 w-64 h-64 bg-accent/20 rounded-full blur-3xl"
        />

        <div className="relative grid md:grid-cols-[auto_1fr_auto] items-center gap-6 p-6 md:p-8">
          {/* 좌: 마스코트 */}
          <img
            src="/reference/mozi-mascot.png"
            alt=""
            className="hidden sm:block w-28 md:w-36 h-auto object-contain drop-shadow-md"
          />

          {/* 중: 인사 + 프로필 요약 */}
          <div className="min-w-0">
            <p className="text-senior-sm font-bold text-brand-deep">마이페이지</p>
            <h1 className="mt-1 text-senior-xl md:text-senior-2xl font-extrabold text-ink-strong">
              안녕하세요, 어르신 👋
            </h1>

            {profile === null ? (
              // 로딩 중 — skeleton
              <div className="mt-4 flex gap-2">
                <div className="h-10 w-32 rounded-pill bg-surface/70 animate-pulse" />
                <div className="h-10 w-40 rounded-pill bg-surface/70 animate-pulse" />
                <div className="h-10 w-28 rounded-pill bg-surface/70 animate-pulse" />
              </div>
            ) : isCompleted ? (
              <>
                <p className="mt-2 text-senior-base text-ink-weak">
                  이 정보를 바탕으로 맞춤 복지를 추천해드려요.
                </p>

                <ul className="mt-4 flex flex-wrap gap-2">
                  {regionLabel && (
                    <SummaryChip icon="📍" label={regionLabel} />
                  )}
                  {householdLabel && (
                    <SummaryChip icon="👨‍👩‍👧" label={householdLabel} />
                  )}
                  {incomeLabel && (
                    <SummaryChip icon="💰" label={incomeLabel} />
                  )}
                  {qualifications.map((key) => (
                    <SummaryChip key={key} label={QUALIFICATION_LABEL[key]} accent />
                  ))}
                </ul>
              </>
            ) : (
              <>
                <p className="mt-2 text-senior-base text-ink-weak">
                  프로필을 입력하면 추천이 더 정확해져요.
                  <br className="hidden sm:block" />
                  거주지 · 소득 · 가구 정보를 알려주세요.
                </p>
              </>
            )}
          </div>

          {/* 우: CTA */}
          <div className="md:self-end shrink-0">
            <Button
              onClick={() => navigate("/me/profile")}
              variant={isCompleted === false ? "primary" : "secondary"}
              large
            >
              {isCompleted === false ? "지금 입력하기" : "정보 수정 →"}
            </Button>
          </div>
        </div>
      </section>

      {/* ───── Stat Strip ───── */}
      <section className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-10">
        <StatTile
          icon="🔖"
          label="저장한 복지"
          value={bookmarkCount === null ? "—" : `${bookmarkCount}건`}
          hint="다시 살펴보기"
          onClick={() => navigate("/bookmarks")}
        />
        <StatTile
          icon="💬"
          label="챗봇 도우미"
          value="바로 상담"
          hint="궁금한 점을 물어보세요"
          onClick={() => navigate("/chat")}
        />
        <StatTile
          icon="🔍"
          label="복지 둘러보기"
          value="분류별 보기"
          hint="15개 분야 카테고리"
          onClick={() => navigate("/categories")}
        />
      </section>

      {/* ───── 메뉴 카드 ───── */}
      <h2 className="text-senior-lg font-extrabold text-ink-strong mb-4">
        계정 관리
      </h2>
      <nav
        aria-label="마이페이지 메뉴"
        className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-10"
      >
        <MenuCard
          icon="👤"
          title="내 정보 수정"
          subtitle="거주지·소득·가구 등 맞춤 추천에 사용되는 정보"
          onClick={() => navigate("/me/profile")}
        />
        <MenuCard
          icon="🔒"
          title="비밀번호 변경"
          subtitle="보안을 위해 정기적으로 변경해주세요"
          onClick={() => navigate("/me/password")}
        />
        <MenuCard
          icon="🔖"
          title="저장한 복지"
          subtitle="관심 있는 복지를 다시 살펴보세요"
          onClick={() => navigate("/bookmarks")}
        />
        <MenuCard
          icon="🚪"
          title="로그아웃"
          subtitle="안전하게 로그아웃합니다"
          onClick={handleLogout}
        />
      </nav>

      {/* ───── 회원 탈퇴 ───── */}
      <div className="border-t border-surface-border pt-8">
        <button
          type="button"
          onClick={() => setWithdrawOpen(true)}
          className="block w-full md:w-auto px-6 py-4 rounded-card bg-danger-subtle text-danger text-senior-base font-bold hover:bg-danger/10 transition-colors"
        >
          회원 탈퇴
        </button>
      </div>

      <Modal
        open={withdrawOpen}
        onClose={() => !submitting && setWithdrawOpen(false)}
        title="정말 탈퇴하시겠어요?"
      >
        <p className="text-senior-base text-ink-strong mb-6 whitespace-pre-line">
          {
            "저장한 복지·프로필·대화 기록이 모두 삭제됩니다.\n이 작업은 되돌릴 수 없어요."
          }
        </p>
        <div className="flex flex-col-reverse sm:flex-row gap-2 justify-end">
          <Button
            variant="secondary"
            onClick={() => setWithdrawOpen(false)}
            disabled={submitting}
          >
            취소
          </Button>
          <Button
            variant="danger"
            large
            onClick={handleWithdraw}
            disabled={submitting}
          >
            {submitting ? "처리 중…" : "탈퇴할게요"}
          </Button>
        </div>
      </Modal>
    </main>
  );
}

/* ─────────────────── 내부 보조 컴포넌트 ─────────────────── */

/**
 * 프로필 요약 칩 (지역/가구/소득). accent=true 면 강조 톤 (자격).
 */
function SummaryChip({ icon, label, accent = false }) {
  return (
    <li
      className={
        "inline-flex items-center gap-2 px-4 h-11 rounded-pill text-senior-sm font-bold border " +
        (accent
          ? "bg-brand text-ink-invert border-brand"
          : "bg-surface text-ink-strong border-surface-border")
      }
    >
      {icon && <span aria-hidden="true">{icon}</span>}
      <span>{label}</span>
    </li>
  );
}

/**
 * 통계/바로가기 타일. 클릭 가능한 카드.
 */
function StatTile({ icon, label, value, hint, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="text-left p-5 rounded-xl-card bg-surface border border-surface-border shadow-card hover:shadow-card-hover hover:-translate-y-0.5 transition-all"
    >
      <div className="flex items-center gap-3">
        <span
          aria-hidden="true"
          className="w-12 h-12 rounded-pill bg-brand-subtle text-senior-xl flex items-center justify-center"
        >
          {icon}
        </span>
        <div className="min-w-0">
          <p className="text-senior-sm text-ink-weak">{label}</p>
          <p className="text-senior-lg font-extrabold text-ink-strong">
            {value}
          </p>
        </div>
      </div>
      <p className="mt-3 text-senior-sm text-brand-deep font-bold">
        {hint} →
      </p>
    </button>
  );
}

/**
 * 메뉴 카드 — 아이콘 + 제목 + 부가설명 + 우측 화살표.
 */
function MenuCard({ icon, title, subtitle, onClick }) {
  return (
    <Card onClick={onClick}>
      <div className="flex items-center gap-4">
        <span
          aria-hidden="true"
          className="w-14 h-14 rounded-pill bg-brand-subtle text-senior-2xl flex items-center justify-center shrink-0"
        >
          {icon}
        </span>
        <div className="flex-1 min-w-0">
          <p className="text-senior-lg text-ink-strong font-extrabold">
            {title}
          </p>
          <p className="text-senior-sm text-ink-weak mt-1">{subtitle}</p>
        </div>
        <span aria-hidden="true" className="text-ink-mute text-senior-lg">
          →
        </span>
      </div>
    </Card>
  );
}
