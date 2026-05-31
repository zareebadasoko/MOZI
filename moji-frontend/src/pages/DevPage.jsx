// pages/DevPage.jsx
//
// 공통 컴포넌트 8종을 한 화면에서 시각·동작 확인하는 쇼룸.
// Phase 3 검증 #1 (모든 공통 컴포넌트 시각 확인) + #2 (Toast 4초+) + #3 (Modal 닫기) 통과용.
// Phase 6에서 제거 예정.

import { useState } from "react";
import Button from "../components/common/Button";
import Input from "../components/common/Input";
import Card from "../components/common/Card";
import Modal from "../components/common/Modal";
import Spinner from "../components/common/Spinner";
import ErrorBox from "../components/common/ErrorBox";
import EmptyState from "../components/common/EmptyState";
import { useToast } from "../hooks/useToast";

/**
 * DevPage — 공통 컴포넌트 쇼룸
 *
 * @returns {JSX.Element}
 */
export default function DevPage() {
  const { showToast } = useToast();
  const [modalOpen, setModalOpen] = useState(false);
  const [inputValue, setInputValue] = useState("");

  return (
    <main className="flex flex-col gap-10">
      <h1 className="text-senior-xl font-bold text-ink-strong">
        공통 컴포넌트 쇼룸 (Dev)
      </h1>

      {/* Button — 3 variants × 2 sizes */}
      <Section title="Button">
        <div className="flex flex-wrap gap-3">
          <Button>Primary</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="danger">Danger</Button>
        </div>
        <div className="flex flex-wrap gap-3 mt-3">
          <Button large>Primary Large</Button>
          <Button variant="secondary" large>
            Secondary Large
          </Button>
          <Button variant="danger" large>
            Danger Large
          </Button>
        </div>
        <div className="mt-3">
          <Button disabled>Disabled</Button>
        </div>
      </Section>

      {/* Input — 3 상태 */}
      <Section title="Input">
        <div className="flex flex-col gap-4 max-w-md">
          <Input
            label="기본"
            placeholder="값 입력"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
          />
          <Input
            label="hint 포함"
            hint="예시: hong@example.com"
            placeholder="이메일"
          />
          <Input
            label="error 포함"
            error="이메일 형식이 올바르지 않아요."
            defaultValue="잘못된 값"
          />
        </div>
      </Section>

      {/* Card — 클릭 가능 + 일반 */}
      <Section title="Card">
        <div className="flex flex-col gap-3 max-w-md">
          <Card>
            <p className="text-senior-base text-ink-strong">일반 카드 (div)</p>
            <p className="text-senior-sm text-ink-weak mt-1">
              클릭 안 됨. 단순 표시용.
            </p>
          </Card>
          <Card onClick={() => alert("카드 클릭됨!")}>
            <p className="text-senior-base text-ink-strong">
              클릭 가능 카드 (button)
            </p>
            <p className="text-senior-sm text-ink-weak mt-1">
              호버 시 그림자 강해짐.
            </p>
          </Card>
        </div>
      </Section>

      {/* Modal */}
      <Section title="Modal (검증 #3: ESC + 배경 클릭으로 닫힘)">
        <Button onClick={() => setModalOpen(true)}>모달 열기</Button>
        <Modal
          open={modalOpen}
          onClose={() => setModalOpen(false)}
          title="확인"
        >
          <p className="mb-4">
            ESC 키를 누르거나 어두운 배경을 클릭하면 닫혀요. 모달 내부 클릭은
            닫기 트리거 X.
          </p>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setModalOpen(false)}>
              취소
            </Button>
            <Button onClick={() => setModalOpen(false)}>확인</Button>
          </div>
        </Modal>
      </Section>

      {/* Toast — 3 kinds. 4초 후 자동 사라짐 (검증 #2) */}
      <Section title="Toast (검증 #2: 4초 후 자동 사라짐)">
        <div className="flex flex-wrap gap-3">
          <Button
            variant="secondary"
            onClick={() => showToast({ kind: "success", message: "저장됐어요!" })}
          >
            Success Toast
          </Button>
          <Button
            variant="secondary"
            onClick={() =>
              showToast({ kind: "error", message: "다시 시도해주세요." })
            }
          >
            Error Toast
          </Button>
          <Button
            variant="secondary"
            onClick={() =>
              showToast({ kind: "info", message: "안내드릴 사항이 있어요." })
            }
          >
            Info Toast
          </Button>
        </div>
        <p className="text-senior-sm text-ink-weak mt-2">
          여러 번 클릭하면 최대 3개까지 쌓이고, 4개째부턴 가장 오래된 게
          밀려나요. 화면 하단 중앙에 표시됨.
        </p>
      </Section>

      {/* Spinner */}
      <Section title="Spinner">
        <div className="flex items-center gap-6">
          <Spinner size="sm" />
          <Spinner />
          <Spinner label="잠시만요…" />
        </div>
      </Section>

      {/* ErrorBox */}
      <Section title="ErrorBox">
        <ErrorBox
          message="복지 정보를 불러올 수 없어요."
          onRetry={() => alert("재시도 클릭됨")}
        />
      </Section>

      {/* EmptyState */}
      <Section title="EmptyState">
        <EmptyState
          icon="📌"
          message="아직 저장한 복지가 없어요."
          actionLabel="복지 찾으러 가기"
          onAction={() => alert("CTA 클릭됨")}
        />
      </Section>

      <p className="text-senior-sm text-ink-weak text-center mt-8">
        이 페이지는 Phase 6에서 제거됩니다.
      </p>
    </main>
  );
}

/** 섹션 헤더 + 본문 묶음 (DevPage 내부에서만 사용) */
function Section({ title, children }) {
  return (
    <section>
      <h2 className="text-senior-lg font-bold text-ink-strong mb-3">{title}</h2>
      {children}
    </section>
  );
}
