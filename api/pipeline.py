"""챗봇 파이프라인 — 한 요청을 처리하는 단일 진입점.

흐름 (CHATBOT_API_CONTRACT 명세 기반, generate.py process_turn 변형):

  ① rewrite_query   — 멀티턴 지시어 치환 → standalone 질의
  ② hybrid retrieval — Dense(50) + BM25(50) + RRF → top-50
  ③ rerank          — bge-v2-m3 → top-3
  ④ generate_answer — LLM 답변 생성
  ⑤ summarize       — 다음 턴용 대화 요약 (직전 1턴이 있을 때만)
  ⑥ new_state 반환  — caller 가 SessionStore 에 저장
"""

from __future__ import annotations

from typing import Any, Dict, List, Tuple

from .config import TOP_K_GEN
from .generate import (
    CANNED_REPLIES,
    classify_intent,
    detect_followup,
    format_last_turn_qa,
    generate_answer,
    rewrite_query,
    summarize_history,
)
from .reranker import Reranker
from .retriever import HybridRetriever


def _build_search_query(rewritten_question: str, user_info: str | None) -> str:
    """검색용 쿼리. user_info 가 있으면 자격 키워드를 prefix 로 붙여 검색 정확도 ↑.

    run_k_elbow.py 의 make_final_query 와 동일한 패턴을 따르되, 이미 LLM 으로
    rewrite 된 standalone 질문을 base 로 사용.
    """
    if user_info:
        return f"{user_info} 조건의 사용자 질문: {rewritten_question}"
    return rewritten_question


class ChatPipeline:
    """앱 lifespan 동안 유지되는 single instance (retriever + reranker 1회 로드)."""

    def __init__(self) -> None:
        self.retriever = HybridRetriever()
        self.reranker = Reranker()

    def run(
        self,
        message: str,
        user_info: str | None,
        state: Dict[str, str],
    ) -> Tuple[str, List[str], Dict[str, str], Dict[str, Any]]:
        """한 요청 처리.

        Args:
            message: 사용자 메시지 (원문)
            user_info: profile_to_user_info() 변환 결과 또는 None
            state: 이전 세션 상태 {"summary", "last_q", "last_a"}

        Returns:
            (reply, recommended_welfare_ids, new_state, debug_meta)
        """
        last_qa_str = format_last_turn_qa(state["last_q"], state["last_a"])

        # ① rewrite
        rewritten = rewrite_query(
            current_question=message,
            user_info=user_info,
            prev_summary=state["summary"],
            last_turn_qa=last_qa_str,
        )
        is_followup = detect_followup(state["last_q"], message, rewritten)

        # ②  intent classification (후속 질의는 자동으로 WELFARE 처리 — "그거 어떻게 신청?" 등)
        intent = "WELFARE" if is_followup else classify_intent(message)

        # 복지 외 입력은 검색·LLM 모두 스킵하고 canned 응답 반환.
        # 세션 state 는 유지 (last_q/last_a/summary 안 갱신) — 다음 턴에 "그거"가 직전 복지를 가리킬 수 있게.
        if intent != "WELFARE":
            canned = CANNED_REPLIES[intent]
            debug_meta = {
                "rewritten_query": rewritten,
                "is_followup": is_followup,
                "intent": intent,
                "n_candidates": 0,
                "top1_rerank_score": 0.0,
                "top_k_doc_ids": [],
            }
            return canned, [], state, debug_meta

        # ③ hybrid retrieval
        search_query = _build_search_query(rewritten, user_info)
        candidates = self.retriever.search(search_query)

        # ③ rerank → top-3 (점수 컷오프 없이 항상 top_k 반환)
        top_k, top1_score = self.reranker.rerank(
            query=search_query,
            candidates=candidates,
            top_k=TOP_K_GEN,
        )
        contexts = [c["page_content"] for c in top_k]
        welfare_ids = [c["doc_id"] for c in top_k if c.get("doc_id")]

        # ④ generate
        answer = generate_answer(
            question=message,
            contexts=contexts,
            user_info=user_info,
            prev_summary=state["summary"],
            last_turn_qa=last_qa_str,
            is_followup=is_followup,
        )

        # ⑤ summarize (직전 1턴이 있을 때만)
        new_summary = state["summary"]
        if state["last_q"] or state["last_a"]:
            new_summary = summarize_history(
                prev_summary=state["summary"],
                question=state["last_q"],
                answer=state["last_a"],
            )

        new_state = {
            "summary": new_summary,
            "last_q": message,
            "last_a": answer,
        }
        debug_meta = {
            "rewritten_query": rewritten,
            "is_followup": is_followup,
            "intent": intent,
            "n_candidates": len(candidates),
            "top1_rerank_score": round(top1_score, 4),  # 참고용 (컷오프엔 사용 안 함)
            "top_k_doc_ids": [c.get("doc_id") for c in top_k],
        }
        return answer, welfare_ids, new_state, debug_meta
