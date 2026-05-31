"""FastAPI 앱 — POST /chat 엔드포인트.

실행:
  cd /home/sojung/workspace/핀빅스캡스톤
  /home/sojung/miniconda3/envs/finvix-rag/bin/python -m uvicorn api.main:app \\
      --host 0.0.0.0 --port 8000 --reload

또는:
  uvicorn api.main:app --host 0.0.0.0 --port 8000

호출 예시 (CHATBOT_API_CONTRACT.md §2):
  curl -X POST http://localhost:8000/chat \\
       -H "Content-Type: application/json" \\
       -H "X-API-Key: $CHATBOT_API_KEY" \\
       -d '{
         "message": "노인 일자리 알려줘",
         "conversationId": "8c4f5a72-1f3e-4b2a-9c8e-7a1b3d4e5f6a",
         "user": {
           "userId": 1,
           "profile": {
             "age": 78, "gender": "F",
             "sidoName": "서울특별시", "sigunguName": "강남구",
             "incomeType": "기초연금수급자",
             "householdType": "혼자 살아요 (독거)",
             "isDisabled": false, "isMultiChild": false,
             "isMulticulturalNorthDefector": false, "isVeteran": false
           }
         }
       }'
"""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

from .auth import verify_api_key
from .pipeline import ChatPipeline
from .profile import profile_to_user_info
from .schemas import ChatRequest, ChatResponse, ErrorResponse
from .session import SessionStore

logger = logging.getLogger("mozi.chatbot")
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s | %(message)s")


# ============================================================
# Lifespan — 시작 시 retriever + reranker 로드 (1회)
# ============================================================
pipeline: ChatPipeline | None = None
session_store: SessionStore | None = None


@asynccontextmanager
async def lifespan(_: FastAPI):
    global pipeline, session_store
    logger.info("starting up — loading retriever + reranker...")
    pipeline = ChatPipeline()
    session_store = SessionStore()
    logger.info("ready.")
    yield
    logger.info("shutting down.")


app = FastAPI(
    title="MOZI 챗봇 서버",
    description="복지 정보 RAG + LLM 답변 생성 API",
    version="1.0.0",
    lifespan=lifespan,
)


# ============================================================
# 헬스 체크 (CHATBOT_API_CONTRACT §5)
# ============================================================
@app.get("/health")
async def health() -> dict:
    return {"status": "ok"}


# ============================================================
# /chat — 메인 엔드포인트
# ============================================================
@app.post(
    "/chat",
    response_model=ChatResponse,
    responses={
        400: {"model": ErrorResponse},
        401: {"model": ErrorResponse},
        500: {"model": ErrorResponse},
    },
)
async def chat(
    req: ChatRequest,
    _: str = Depends(verify_api_key),
) -> ChatResponse:
    if pipeline is None or session_store is None:
        # lifespan 이 정상이면 도달하지 않음 — 방어
        raise HTTPException(
            status_code=503,
            detail={"error": "OVERLOADED", "message": "service initializing"},
        )

    # 세션 상태 로드
    state = session_store.get(req.conversationId)

    # Profile (Step 7 wire format) → generate.py user_info 문자열
    user_info = profile_to_user_info(req.user.profile)

    try:
        reply, welfare_ids, new_state, debug = pipeline.run(
            message=req.message,
            user_info=user_info,
            state=state,
        )
    except Exception as e:  # noqa: BLE001
        logger.exception("pipeline failure: conv=%s", req.conversationId)
        raise HTTPException(
            status_code=500,
            detail={"error": "INTERNAL_ERROR", "message": str(e)},
        ) from e

    # 세션 갱신
    session_store.set(req.conversationId, new_state)

    logger.info(
        "userId=%s conv=%s msg=%r intent=%s → reply_len=%d welfares=%d top1=%.3f",
        req.user.userId, req.conversationId, req.message[:60],
        debug.get("intent", "WELFARE"),
        len(reply), len(welfare_ids),
        debug["top1_rerank_score"],
    )

    return ChatResponse(
        reply=reply,
        conversationId=req.conversationId,
        recommendedWelfareIds=welfare_ids,
    )


# ============================================================
# 예외 핸들러 — CHATBOT_API_CONTRACT §3 에러 응답 형식 통일
# ============================================================
@app.exception_handler(HTTPException)
async def http_exception_handler(_: Request, exc: HTTPException) -> JSONResponse:
    """HTTPException.detail 이 이미 {error, message} dict 면 그대로, 아니면 wrap."""
    if isinstance(exc.detail, dict) and "error" in exc.detail:
        return JSONResponse(status_code=exc.status_code, content=exc.detail)
    return JSONResponse(
        status_code=exc.status_code,
        content={"error": "INVALID_REQUEST", "message": str(exc.detail)},
    )
