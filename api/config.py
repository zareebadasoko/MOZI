"""환경 설정 + 상수.

api 폴더 self-contained. 부모 디렉토리 의존 없음.

환경변수 (.env 또는 OS env):
  - OPENAI_API_KEY  (필수)  — LLM + Embed
  - CHATBOT_API_KEY (필수)  — 본 서버 X-API-Key 인증
  - INDEX_DIR       (선택)  — 인덱스 디렉토리. 기본: api/vectorstores/
  - LANGSMITH_TRACING (선택) — true 면 LangSmith 추적 자동

실험으로 확정된 하이퍼파라미터:
  - Dense K, BM25 K : 각 50 (K_dense × K_bm25 sweep 결과)
  - TOP_N           : 50 (RRF 통합 후, TOP_N sweep 결과)
  - TOP_K_GEN       : 3 (LLM 입력 후보 수)
  - Reranker        : BAAI/bge-reranker-v2-m3
"""

from __future__ import annotations

import os
from pathlib import Path

from dotenv import load_dotenv

# api 폴더가 곧 PROJECT_ROOT (self-contained)
PROJECT_ROOT = Path(__file__).resolve().parent

# .env 우선순위: api/.env → 부모 디렉토리 .env (fallback). OS 환경변수가 둘 다 이김.
load_dotenv(PROJECT_ROOT / ".env")
load_dotenv(PROJECT_ROOT.parent / ".env", override=False)


# ============================================================
# 환경변수
# ============================================================
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
CHATBOT_API_KEY = os.getenv("CHATBOT_API_KEY", "dev-key-change-me")


# ============================================================
# 인덱스 디렉토리
# ============================================================
# 기본: api/vectorstores/welfare/{chunk_record, bm25_record}/
# 다른 경로 사용 시 환경변수 INDEX_DIR 지정 (e.g. /var/lib/mozi/vectorstores).
INDEX_DIR = Path(os.getenv("INDEX_DIR", str(PROJECT_ROOT / "vectorstores")))


# ============================================================
# 임베딩 모델 (FAISS 인덱스 빌드 시 사용한 모델과 동일해야 함)
# ============================================================
EMBEDDING_MODEL = "text-embedding-3-large"


# ============================================================
# Hybrid retrieval
# ============================================================
DENSE_K = 50
BM25_K = 50
TOP_N = 50          # RRF 통합 후 자를 후보 수
RRF_K_CONST = 60


# ============================================================
# Reranker
# ============================================================
RERANKER_MODEL = "BAAI/bge-reranker-v2-m3"
RERANKER_MAX_LENGTH = 512
TOP_K_GEN = 3       # LLM 에 넘기는 최종 후보 수


# ============================================================
# Session
# ============================================================
SESSION_TTL_SECONDS = 1800   # 30분 (CHATBOT_API_CONTRACT §4 권장값)
