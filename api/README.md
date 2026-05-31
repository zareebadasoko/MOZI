# MOZI 챗봇 API 서버

CHATBOT_API_CONTRACT.md (Step 7 wire format) 명세를 구현한 FastAPI 서버.

**자기완결 (self-contained)**: 이 폴더만 git clone 하면 동작 (인덱스 빌드 1회 필요).
부모 디렉토리 의존 없음.

---

## 🚀 빠르게 시작하기 (팀원용)

```bash
# 1) clone
git clone <repo-url> mozi-chatbot
cd mozi-chatbot

# 2) Python 환경 + 의존성
python -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate
pip install -r requirements.txt

#    GPU 사용 시 (선택 — 권장):
#    pip install torch --index-url https://download.pytorch.org/whl/cu124

# 3) .env 작성 (api 폴더 안)
cp .env.example .env
# .env 열어서 OPENAI_API_KEY, CHATBOT_API_KEY 채우기

# 4) 원본 데이터 배치 (팀에서 별도 공유받은 파일)
mkdir -p data
cp /path/to/welfare_integrated_all.json data/

# 5) 인덱스 빌드 (최초 1회, OpenAI 임베딩 비용 ~$0.5, 5-10분 소요)
cd ..                              # api 의 부모 디렉토리로
python -m api.build_indexes

# 6) 서버 기동
python -m uvicorn api.main:app --host 0.0.0.0 --port 8000

# 7) 헬스 체크 (다른 터미널)
curl http://localhost:8000/health
```

> ⚠️ 명령어들은 `api/` 폴더가 패키지로 인식되도록 **부모 디렉토리에서** 실행하는 게 표준 (예: `python -m api.main` 이 아니라 `python -m uvicorn api.main:app`).
> repo 루트가 곧 `api/` 라면 폴더 이름을 `mozi_chatbot/` 등으로 두고 같은 패턴 사용.

---

## 파이프라인

```
사용자 메시지
   ↓
[① rewrite_query]      gpt-4.1-nano — 멀티턴 지시어 치환
   ↓
[② Hybrid retrieval]   Dense (OpenAI text-embedding-3-large, k=50)
                       BM25  (kiwipiepy 토큰화, k=50)
                          ↓ RRF (k=60)
                       후보 top-50
   ↓
[③ Reranker]           BAAI/bge-reranker-v2-m3 → top-3
   ↓
[④ generate_answer]    gpt-4.1-mini (v3_literal_rules 프롬프트)
   ↓
[⑤ summarize_history]  gpt-4.1-nano — 다음 턴 컨텍스트 (직전 1턴 있을 때만)
   ↓
응답 { reply, conversationId, recommendedWelfareIds }
```

확정 하이퍼파라미터 (sweep 실험 결과):

| 설정 | 값 | 근거 |
|---|---|---|
| Dense K, BM25 K | 50, 50 | K_dense × K_bm25 sweep 1위 |
| TOP_N (RRF→reranker) | 50 | TOP_N sweep 단조 증가, 50에서 최고 |
| TOP_K_GEN (reranker→LLM) | 3 | k 엘보우 — MRR@3 포화 지점 |
| Reranker | bge-reranker-v2-m3 | Cohere v3.5 와 거의 동등 + 로컬·무료 |

성능 (testset 100문항, 분모 93 — relevant 우선 평가):
- HitRate@3 = 0.78 / MRR@3 = 0.72 / nDCG@3 = 0.66
- Latency: mean 296ms / p99 383ms (GPU 기준)

---

## 파일 구성 (api/ 폴더)

```
api/
├── __init__.py
├── main.py            # FastAPI 앱 — POST /chat, GET /health
├── config.py          # 환경 + 하이퍼파라미터
├── schemas.py         # Pydantic Request/Response (Step 7 wire format)
├── profile.py         # Profile → user_info 자연어 변환
├── auth.py            # X-API-Key 검증
├── session.py         # conversationId → state 메모리 캐시 (TTL 30분)
├── tokenizer.py       # kiwipiepy 한국어 토큰화 (BM25)
├── vectorstore.py     # FAISS 인덱스 로드 (런타임)
├── hybrid_search.py   # Dense + BM25 + RRF
├── retriever.py       # HybridSearcher 싱글톤 래퍼
├── reranker.py        # bge-reranker-v2-m3 싱글톤
├── generate.py        # LLM 호출 + 프롬프트 + 멀티턴
├── pipeline.py        # 한 요청 처리 흐름
├── build_indexes.py   # FAISS + BM25 인덱스 빌드 (최초 1회)
├── requirements.txt
├── .env.example
├── .gitignore
├── README.md          # 본 문서
├── vectorstores/      # 인덱스 (빌드 후 자동 생성, .gitignore)
│   └── welfare/
│       ├── chunk_record/    index.faiss, index.pkl
│       └── bm25_record/     bm25.pkl, docs.json
└── data/              # 원본 데이터 (.gitignore — 팀에서 별도 공유)
    └── welfare_integrated_all.json
```

## 모듈 간 의존성 (모두 api/ 내부)

```
main.py
  └─ pipeline.py
       ├─ generate.py        (LLM 로직)
       ├─ retriever.py
       │    └─ hybrid_search.py
       │         ├─ tokenizer.py        (kiwipiepy)
       │         └─ vectorstore.py      (FAISS load)
       └─ reranker.py        (bge-reranker-v2-m3)
```

→ **부모 디렉토리 의존성 0**. 패키지 import 외에 외부 파일 참조 없음.

---

## 환경변수

| 변수 | 필수 | 용도 |
|---|---|---|
| `OPENAI_API_KEY` | ✅ | LLM (gpt-4.1-mini, gpt-4.1-nano) + Embed (text-embedding-3-large) |
| `CHATBOT_API_KEY` | ✅ | 본 서버 X-API-Key 인증 |
| `INDEX_DIR` | ❌ | 인덱스 디렉토리 (기본: `api/vectorstores/`) |
| `WELFARE_DATA_PATH` | ❌ | 원본 데이터 경로 (기본: `api/data/welfare_integrated_all.json`) |
| `LANGSMITH_TRACING` | ❌ | `true` 시 LangSmith 추적 자동 활성 |
| `LANGSMITH_API_KEY` | ❌ | LangSmith 인증 (TRACING=true 일 때만) |

---

## 테스트 호출

```bash
# 헬스 체크
curl http://localhost:8000/health

# /chat (X-API-Key 필요)
curl -X POST http://localhost:8000/chat \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $CHATBOT_API_KEY" \
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
```

응답:
```json
{
  "reply": "강남구에 사시는 78세 어르신께 다음 복지를 추천드려요. ...",
  "conversationId": "8c4f5a72-1f3e-4b2a-9c8e-7a1b3d4e5f6a",
  "recommendedWelfareIds": ["WLF00001234", "BOK00000010", "SEL00000005"]
}
```

API 문서: 서버 기동 후 http://localhost:8000/docs 에서 Swagger UI 확인 가능.

---

## 운영 시 주의

- **세션 저장소**: 현재 in-memory `SessionStore`. multi-instance / 재기동 영속성 필요하면 Redis 등으로 교체.
- **모델 로드 시간**: 앱 시작 시 reranker (~568MB) GPU 로딩 + FAISS 인덱스 메모리 로드 → 수 초. health 체크가 200 응답한다고 곧장 /chat 가능한 게 아님 — lifespan 완료 후 트래픽 받기.
- **타임아웃**: 백엔드가 8초 컷. 멀티턴 시 rewrite + retrieve + rerank + summarize 까지 다 합쳐 약 1.5~3초 예상 (GPU 기준).
- **GPU vs CPU**: reranker 는 GPU 권장 (CPU 도 동작하지만 ~10배 느림). FAISS 는 faiss-cpu 로 충분.
- **점수 컷오프 없음**: 리랭커 점수에 따른 abstention 적용 안 함. 항상 top-3 의 `recommendedWelfareIds` 반환. 명세 §2.6 의 "범위외/잡담" 응답 분기는 LLM 시스템 프롬프트의 "제공된 자료에서는 해당 내용을 확인할 수 없습니다." 룰로 처리.

---

## 인덱스 재빌드가 필요한 경우

다음 중 하나라도 변경되면 `python -m api.build_indexes` 다시 실행:
- 원본 데이터 (`welfare_integrated_all.json`) 갱신
- `EMBEDDING_MODEL` 변경 (config.py)
- `CONTENT_FIELDS` / `METADATA_FIELDS` 변경 (build_indexes.py)

빌드 결과는 `INDEX_DIR/welfare/{chunk_record, bm25_record}/` 에 덮어쓰임.
