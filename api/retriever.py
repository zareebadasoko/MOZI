"""Hybrid 검색 (Dense + BM25 + RRF) 싱글톤 래퍼.

api/hybrid_search.HybridSearcher 를 한 번만 로드해 FastAPI lifespan 동안 재사용.
부모 디렉토리 의존 없음.
"""

from __future__ import annotations

from typing import Any, Dict, List

from .config import BM25_K, DENSE_K, RRF_K_CONST, TOP_N
from .hybrid_search import HybridSearcher


class HybridRetriever:
    """앱 lifespan 동안 유지되는 single instance."""

    def __init__(self) -> None:
        # FAISS + BM25 인덱스 로드 (record 청크, 1행=1청크)
        self._searcher = HybridSearcher(rrf_k_const=RRF_K_CONST)

    def search(self, query: str) -> List[Dict[str, Any]]:
        """확정 설정 (DENSE_K=50, BM25_K=50, TOP_N=50) 으로 후보 검색."""
        return self._searcher.search(
            query,
            dense_k=DENSE_K,
            bm25_k=BM25_K,
            top_n=TOP_N,
        )
