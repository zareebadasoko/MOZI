"""FAISS 벡터스토어 로드 (런타임 전용).

빌드(임베딩 호출 + 인덱스 저장)는 별도 단계. 본 모듈은 이미 빌드된 인덱스를
디스크에서 로드만 한다.

인덱스 디렉토리 기본값: api/vectorstores/welfare/chunk_record/
  - index.faiss : 벡터 인덱스 (binary)
  - index.pkl   : docstore + id 매핑 (pickle, allow_dangerous_deserialization 필요)

config.INDEX_DIR 또는 환경변수 INDEX_DIR 로 외부 경로 override 가능.
"""

from __future__ import annotations

from pathlib import Path

from langchain_community.vectorstores import FAISS
from langchain_openai import OpenAIEmbeddings

from .config import EMBEDDING_MODEL, INDEX_DIR, OPENAI_API_KEY


def load_faiss_vectorstore(name: str = "chunk_record") -> FAISS:
    """`{INDEX_DIR}/welfare/{name}/` 의 FAISS 인덱스 로드.

    Args:
        name: 인덱스 디렉토리 이름. 기본 "chunk_record" (1행=1청크 인덱스).

    allow_dangerous_deserialization=True 이유:
        FAISS 저장 파일(index.pkl)에 pickle 포함 — LangChain 이 명시적 옵트인 요구.
        이 코드가 직접 만든 파일만 로드하는 전제이므로 허용.
    """
    if not OPENAI_API_KEY:
        raise RuntimeError(
            "OPENAI_API_KEY 가 설정돼 있지 않습니다 (.env 또는 환경변수)."
        )
    index_path = INDEX_DIR / "welfare" / name
    if not (index_path / "index.faiss").exists():
        raise FileNotFoundError(
            f"FAISS 인덱스가 없습니다: {index_path}. "
            f"인덱스를 빌드한 뒤 INDEX_DIR 환경변수 또는 api/vectorstores/ 에 배치하세요."
        )

    embeddings = OpenAIEmbeddings(model=EMBEDDING_MODEL, api_key=OPENAI_API_KEY)
    return FAISS.load_local(
        str(index_path),
        embeddings,
        allow_dangerous_deserialization=True,
    )
