# MOZI — 노인 맞춤형 통합 복지 안내 서비스

노인 사용자가 자신의 프로필(나이·지역·소득·가구 형태 등)을 바탕으로 받을 수 있는 복지 혜택을
**대화형 챗봇**으로 안내받는 풀스택 서비스입니다. 졸업(캡스톤) 프로젝트.

복지 데이터에 대한 RAG(검색 증강 생성) 파이프라인을 직접 구축해, 사용자 질문에 맞는
복지 정보를 검색·재정렬한 뒤 LLM이 자연어로 추천합니다.

---

## 🏗️ 시스템 구성

```
┌──────────────┐      REST       ┌──────────────────┐    X-API-Key    ┌─────────────────────┐
│ moji-frontend │ ──────────────▶ │   moji-backend    │ ──────────────▶ │        api          │
│  React + Vite │                 │  Spring Boot · JWT │                 │  FastAPI · RAG 챗봇  │
│   (브라우저)   │ ◀────────────── │  MySQL · 복지 DB    │ ◀────────────── │  Hybrid 검색+리랭크  │
└──────────────┘                 └──────────────────┘                 └─────────────────────┘
        UI                          인증·복지·북마크 API                  LLM 복지 추천 (RAG)
```

| 컴포넌트 | 디렉터리 | 스택 | 역할 |
|---|---|---|---|
| **프론트엔드** | [`moji-frontend/`](moji-frontend/) | React 19 · Vite · Tailwind · React Router | 사용자 UI, 챗봇 화면, 복지 목록/북마크 |
| **백엔드** | [`moji-backend/`](moji-backend/) | Spring Boot 3.5 · Java 21 · JPA · Spring Security(JWT) · MySQL | 인증·인가, 복지/북마크/카테고리 API, 챗봇 브릿지 |
| **챗봇 API** | [`api/`](api/) | FastAPI · LangChain · FAISS · BM25(kiwipiepy) · bge-reranker | 복지 RAG — Hybrid 검색 → 재정렬 → LLM 답변 |

데이터 흐름: 프론트엔드가 백엔드에 요청 → 백엔드가 JWT로 사용자를 인증하고 프로필을 실어 챗봇 API(`api/`)에 위임 → 챗봇 API가 복지 RAG 파이프라인으로 추천 답변과 복지 ID를 반환.

---

## 🚀 빠른 시작

각 컴포넌트는 독립적으로 기동합니다. 자세한 안내는 각 폴더의 README를 참고하세요.

### 1) 챗봇 API ([`api/`](api/README.md))

```bash
cd api
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env          # OPENAI_API_KEY, CHATBOT_API_KEY 채우기
# 원본 데이터 배치 후 인덱스 빌드 (최초 1회)
cd .. && python -m api.build_indexes
python -m uvicorn api.main:app --port 8000
```

### 2) 백엔드 ([`moji-backend/`](moji-backend/README.md))

```bash
cd moji-backend
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# DB 접속 정보·JWT secret 채우기 (MySQL 8 필요)
./gradlew bootRun            # http://localhost:8080 · Swagger /swagger-ui.html
```

### 3) 프론트엔드 ([`moji-frontend/`](moji-frontend/README.md))

```bash
cd moji-frontend
npm install
cp .env.example .env.local   # VITE_API_BASE_URL=http://localhost:8080
npm run dev                  # http://localhost:5173
```

> 권장 기동 순서: **챗봇 API → 백엔드 → 프론트엔드**.
> 챗봇 서버 없이 백엔드만 시연하려면 `application-local.yml` 에서 `mozi.chatbot.mock-enabled: true`.

---

## 🔐 비밀 정보

비밀 키·DB 비밀번호는 **커밋되지 않습니다**. 각 컴포넌트에 템플릿이 있으니 복사해서 채우세요.

| 컴포넌트 | 템플릿 | 실제 파일 (gitignore) |
|---|---|---|
| api | `api/.env.example` | `api/.env` |
| moji-backend | `moji-backend/src/main/resources/application-local.yml.example` | `application-local.yml` |
| moji-frontend | `moji-frontend/.env.example` | `moji-frontend/.env.local` |

---

## 🧠 챗봇 RAG 파이프라인 (핵심)

```
사용자 메시지
  → ① 질의 재작성 (멀티턴 지시어 치환, gpt-4.1-nano)
  → ② Hybrid 검색  Dense(text-embedding-3-large, k=50) + BM25(kiwipiepy, k=50) → RRF
  → ③ 재정렬        bge-reranker-v2-m3 → top-3
  → ④ 답변 생성     gpt-4.1-mini
  → 응답 { reply, conversationId, recommendedWelfareIds }
```

확정 하이퍼파라미터는 sweep 실험으로 도출. 성능(testset 100문항): **HitRate@3 0.78 / MRR@3 0.72 / nDCG@3 0.66**, 지연 평균 296ms (GPU).
상세는 [api/README.md](api/README.md) 참고.

---

## 📁 저장소 구조

```
moji-project/
├── README.md           # 본 문서 — 전체 개요
├── .gitignore          # 통합 gitignore
├── api/                # FastAPI 복지 RAG 챗봇 서버
├── moji-backend/       # Spring Boot API 서버
└── moji-frontend/      # React + Vite 프론트엔드
```

각 폴더의 `docs/` 에 PRD·API 명세·ERD·사용자 플로우 등 설계 문서가 정리되어 있습니다.

## 프로젝트 정보

본 프로젝트는 노인 사용자를 위한 RAG 기반 맞춤형 복지 안내 서비스를 목표로 개발되었습니다.

---

## 📄 라이선스

학부 캡스톤 프로젝트 — 별도 명시가 없는 한 학습·시연 용도.
