"""인덱스 빌드 — FAISS (Dense) + BM25.

팀원 첫 실행 시 1회 (OpenAI 임베딩 API 호출 비용 발생, 약 ~$0.5 / 수천 청크).
이후 vectorstores/ 디렉토리 그대로 두면 챗봇 서버는 로드만 함.

원본 데이터:
  - 환경변수 WELFARE_DATA_PATH 또는 기본 api/data/welfare_integrated_all.json
  - JSON 배열, 각 원소 = 한 복지 항목 dict (필드: title, summary, target_audience, ...)

산출:
  - {INDEX_DIR}/welfare/chunk_record/{index.faiss, index.pkl}   ← FAISS
  - {INDEX_DIR}/welfare/bm25_record/{bm25.pkl, docs.json}        ← BM25

실행:
  cd <api 폴더의 부모>
  python -m api.build_indexes

  # 또는 원본 데이터 경로를 환경변수로 지정
  WELFARE_DATA_PATH=/path/to/welfare.json python -m api.build_indexes
"""

from __future__ import annotations

import json
import os
import pickle
from pathlib import Path
from typing import Any, Dict, List

from langchain_community.vectorstores import FAISS
from langchain_core.documents import Document
from langchain_openai import OpenAIEmbeddings
from rank_bm25 import BM25Okapi

from .config import (
    EMBEDDING_MODEL,
    INDEX_DIR,
    OPENAI_API_KEY,
    PROJECT_ROOT,
)
from .tokenizer import make_tokenizer

# 원본 데이터 경로 (기본: api/data/welfare_integrated_all.json)
DEFAULT_DATA_PATH = PROJECT_ROOT / "data" / "welfare_integrated_all.json"
DATA_PATH = Path(os.getenv("WELFARE_DATA_PATH", str(DEFAULT_DATA_PATH)))

# 임베딩에 들어가는 본문 필드 (검색 신호 강한 자유 텍스트 위주)
CONTENT_FIELDS = [
    "title",
    "summary",
    "target_audience",
    "support_details",
    "application_method",
    "required_documents",
    "welfare_type",
    "organization_name",
    "interest_theme_code",
    "household_status_code",
]

# 메타데이터로 보존할 필드 (검색 결과 표시·식별·연락처용)
METADATA_FIELDS = [
    "ID",
    "title",
    "organization_name",
    "welfare_type",
    "detail_url",
    "contact_number",
    "contact_email",
    "start_date",
    "end_date",
]


def _row_to_text(row: dict) -> str:
    """복지 항목 dict → 검색용 단일 텍스트.

    각 필드 앞에 [필드명] 라벨을 붙여 임베딩 모델이 정보 종류 구분 가능하게.
    빈 값은 건너뜀.
    """
    parts: List[str] = []
    for f in CONTENT_FIELDS:
        v = row.get(f)
        if v:
            parts.append(f"[{f}] {v}")
    return "\n".join(parts)


def load_source_documents() -> List[Document]:
    if not DATA_PATH.exists():
        raise SystemExit(
            f"원본 데이터를 찾을 수 없습니다: {DATA_PATH}\n"
            f"WELFARE_DATA_PATH 환경변수로 위치를 지정하거나, "
            f"api/data/welfare_integrated_all.json 에 파일을 두세요."
        )
    with open(DATA_PATH, "r", encoding="utf-8") as f:
        rows = json.load(f)
    docs: List[Document] = []
    for row in rows:
        text = _row_to_text(row)
        if not text.strip():
            continue
        metadata = {k: row.get(k) for k in METADATA_FIELDS}
        docs.append(Document(page_content=text, metadata=metadata))
    return docs


# ============================================================
# FAISS (Dense)
# ============================================================
def build_faiss() -> None:
    if not OPENAI_API_KEY:
        raise SystemExit("OPENAI_API_KEY 가 .env 에 없습니다.")
    print(f"[faiss] source: {DATA_PATH}")
    docs = load_source_documents()
    print(f"[faiss] documents: {len(docs)}")
    print(f"[faiss] embedding ({EMBEDDING_MODEL})  — OpenAI API 호출 시작...")
    embeddings = OpenAIEmbeddings(model=EMBEDDING_MODEL, api_key=OPENAI_API_KEY)
    vs = FAISS.from_documents(docs, embeddings)
    out_dir = INDEX_DIR / "welfare" / "chunk_record"
    out_dir.mkdir(parents=True, exist_ok=True)
    vs.save_local(str(out_dir))
    print(f"[faiss] saved → {out_dir}")


# ============================================================
# BM25 (Sparse)
# ============================================================
def build_bm25() -> None:
    print(f"[bm25] source: {DATA_PATH}")
    docs = load_source_documents()
    serial_docs: List[Dict[str, Any]] = [
        {"page_content": d.page_content, "metadata": dict(d.metadata)}
        for d in docs
    ]
    print(f"[bm25] documents: {len(serial_docs)}")
    print("[bm25] tokenizing (kiwipiepy)...")
    tokenize = make_tokenizer()
    corpus_tokens: List[List[str]] = []
    for i, d in enumerate(serial_docs):
        corpus_tokens.append(tokenize(d["page_content"]))
        if (i + 1) % 500 == 0:
            print(f"  ...{i + 1}/{len(serial_docs)}")
    avg = sum(len(t) for t in corpus_tokens) / max(len(corpus_tokens), 1)
    print(f"[bm25] avg tokens/doc: {avg:.1f}")

    print("[bm25] building BM25Okapi...")
    bm25 = BM25Okapi(corpus_tokens)

    out_dir = INDEX_DIR / "welfare" / "bm25_record"
    out_dir.mkdir(parents=True, exist_ok=True)
    with open(out_dir / "bm25.pkl", "wb") as f:
        pickle.dump(bm25, f)
    with open(out_dir / "docs.json", "w", encoding="utf-8") as f:
        json.dump(serial_docs, f, ensure_ascii=False)
    print(f"[bm25] saved → {out_dir}")


def main() -> None:
    INDEX_DIR.mkdir(parents=True, exist_ok=True)
    print(f"== building indexes into: {INDEX_DIR} ==")
    build_faiss()
    print()
    build_bm25()
    print("\n== done ==")
    print("이제 다음 명령으로 서버 기동:")
    print("  python -m uvicorn api.main:app --host 0.0.0.0 --port 8000")


if __name__ == "__main__":
    main()
