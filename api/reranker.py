"""BAAI/bge-reranker-v2-m3 reranker 싱글톤 래퍼.

K_dense × K_bm25 × TOP_N sweep + Cohere v3.5 비교 후 확정된 최종 reranker.
앱 시작 시 1회 로드 (GPU 메모리 상주) — 매 요청마다 모델 로드 안 함.
"""

from __future__ import annotations

import math
from typing import Any, Dict, List, Tuple

import torch
from sentence_transformers import CrossEncoder

from .config import RERANKER_MAX_LENGTH, RERANKER_MODEL


def _sigmoid(x: float) -> float:
    return 1.0 / (1.0 + math.exp(-x))


class Reranker:
    def __init__(self) -> None:
        device = "cuda" if torch.cuda.is_available() else "cpu"
        self._device = device
        self._model = CrossEncoder(
            RERANKER_MODEL,
            device=device,
            max_length=RERANKER_MAX_LENGTH,
        )
        # GPU warmup — 첫 forward 가 느린 cuDNN/JIT 초기화 흡수
        _ = self._model.predict(
            [("warmup query", "warmup doc")] * 4,
            show_progress_bar=False,
        )
        if device == "cuda":
            torch.cuda.synchronize()

    def rerank(
        self,
        query: str,
        candidates: List[Dict[str, Any]],
        top_k: int,
    ) -> Tuple[List[Dict[str, Any]], float]:
        """후보 전체를 점수화 → 내림차순 정렬 → top_k 반환.

        Returns:
            (top_k 후보 리스트, top-1 sigmoid score)
            top-1 score 는 로그·디버깅용 (현재 컷오프엔 사용 안 함).
        """
        if not candidates:
            return [], 0.0
        pairs = [(query, c["page_content"]) for c in candidates]
        scores = self._model.predict(pairs, show_progress_bar=False)
        scores_sig = [_sigmoid(float(s)) for s in scores]
        ranked = sorted(
            zip(scores_sig, candidates),
            key=lambda x: -x[0],
        )
        top1_score = ranked[0][0] if ranked else 0.0
        # 각 후보에 reranker_score 도 같이 부착 (디버깅·로그용)
        out: List[Dict[str, Any]] = []
        for new_rank, (score, c) in enumerate(ranked[:top_k], start=1):
            item = dict(c)
            item["reranker_score"] = float(score)
            item["reranker_rank"] = new_rank
            out.append(item)
        return out, float(top1_score)
