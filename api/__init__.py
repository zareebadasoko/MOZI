"""MOZI 챗봇 API 서버.

CHATBOT_API_CONTRACT.md 명세 기반 FastAPI 구현.

파이프라인:
  쿼리 → Dense(50) + BM25(50) → RRF → top-50 → bge-reranker-v2-m3 → top-3 → LLM
"""
