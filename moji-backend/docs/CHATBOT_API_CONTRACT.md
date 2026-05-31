# 챗봇 서버 ↔ MOZI 백엔드 API 계약서

> 이 문서는 **MOZI 백엔드(브릿지)가 챗봇 서버(LLM + RAG)를 호출**할 때의 외부 API 계약이다.
> MOZI 백엔드가 호출자, 챗봇 서버가 응답자. 챗봇 팀이 본 명세에 맞춰 서버를 구현한다.
>
> 변경 이력은 git history로 관리. 본 문서가 갱신되면 `ChatbotRequest`/`ChatbotResponse`
> record도 함께 갱신되어야 한다.

---

## 📖 이 문서를 처음 보는 분께

본 문서는 챗봇 팀이 만들 챗봇 API의 "주문서"이다. **MOZI 백엔드가 어떻게 호출할지를 미리 약속**하는 문서이므로,
챗봇 팀은 이 명세에 맞춰 서버를 구현하면 된다.

**전체 흐름 한눈에 보기**:

```
사용자(노년층)
   │  ① 채팅 메시지 입력
   ▼
프론트엔드(웹/앱)
   │  ② POST /api/chat  { message, conversationId }
   ▼
MOZI 백엔드 ◀── 본 프로젝트
   │  ③ POST /chat  { message, conversationId, user(프로필) }   ← 본 문서가 약속하는 호출
   ▼
챗봇 서버 ◀── 챗봇 팀 담당 (LLM + RAG)
   │  ④ { reply, recommendedWelfareIds: [...] }
   ▼
MOZI 백엔드
   │  ⑤ welfareId → 복지 카드 정보 hydrate
   ▼
프론트엔드  →  사용자에게 reply + 복지 카드 노출
```

**핵심 분담**:
- **챗봇 팀**: 자연어 답변 생성(`reply`) + 어떤 복지를 추천할지 선택(`recommendedWelfareIds`만 반환, 복지 카드 정보는 X)
- **MOZI 백엔드**: 챗봇이 추천한 ID를 받아 DB에서 복지 카드 상세를 채워 사용자에게 전달

**용어 미리보기**:
- **브릿지(Bridge)**: 단순히 호출만 중계하는 역할. MOZI 백엔드는 챗봇 답변을 가공·해석하지 않고 그대로 통과시킴.
- **자연키(Natural Key)**: DB의 자동 증가 숫자 ID(1, 2, 3...)가 아니라 "이 복지 자체를 식별하는 의미 있는 문자열 ID" (예: `WLF00001234`). 챗봇 팀은 이 문자열을 그대로 반환하면 됨.
- **conversationId**: 같은 대화 세션을 식별하는 ID. 사용자가 "더 자세히 알려줘" 같은 후속 질문을 했을 때 챗봇이 이전 맥락을 기억하기 위함.

---

## 1. 개요

| 항목 | 값 |
|---|---|
| 방향 | MOZI 백엔드 → 챗봇 서버 (단방향) |
| 프로토콜 | HTTPS, JSON, 동기 POST |
| 인증 | API Key (`X-API-Key` 헤더) |
| 권장 평균 응답 시간 | 3초 이내 |
| 최대 응답 시간 | 8초 (백엔드 타임아웃) |
| 문자 인코딩 | UTF-8 |
| Content-Type | `application/json;charset=UTF-8` |

> 💡 **용어 풀이**
> - **단방향**: MOZI 백엔드가 챗봇 서버를 부르기만 하고, 챗봇 서버가 거꾸로 MOZI 백엔드를 부르는 경우는 없음. 챗봇 팀은 "수신만" 하면 됨.
> - **동기 POST**: 백엔드가 요청을 보내면 챗봇 서버 응답이 올 때까지 대기. WebSocket·스트리밍 X. 일반 REST API처럼 1요청 = 1응답.
> - **`X-API-Key` 헤더**: HTTP 헤더는 자유롭게 정의 가능하며 관습적으로 커스텀 헤더 앞에 `X-`를 붙임. 챗봇 팀이 발급한 비밀 키를 이 헤더에 담아 호출자가 "허락된 백엔드"임을 증명. 이 키가 없거나 잘못되면 챗봇 서버는 `401`로 거부해야 함.
> - **8초 타임아웃**: MOZI 백엔드는 8초가 넘으면 응답을 포기하고 사용자에게 "지금은 답변이 어려워요" 안내. LLM 추론이 길어질 수 있어 챗봇 팀과의 합의값.

---

## 2. 엔드포인트

### `POST {CHATBOT_BASE_URL}/chat`

사용자 메시지 + 사용자 컨텍스트를 받아 답변 텍스트 + 추천 복지 ID 목록을 반환.

> 💡 **왜 사용자 프로필도 같이 보내나요?**
> 같은 "노인 일자리 알려줘"라는 질문이라도 "78세 서울 강남구 독거" 어르신과 "65세 강원도 거주, 다자녀 가정" 어르신은 추천해야 할 복지가 다름.
> 챗봇이 더 정확한 추천을 하도록 사용자 프로필을 함께 전달함. **프로필이 없는 사용자(미입력)는 `user.profile: null`**로 보내며, 이 경우 챗봇은 일반적인 답변만 생성.

#### Request Header

```
Content-Type: application/json
X-API-Key: <issued-key>
```

#### Request Body

> ⚠️ **2026-05-17 Step 7 wire format 변경**: profile 페이로드가 12 필드 → **10 필드**로 축소되었고,
> `birthDate` 대신 `age`(만 나이 정수), `sido`/`sigungu` 대신 `sidoName`/`sigunguName`(한글 라벨),
> `incomeType`/`householdType`은 enum 라벨(한글)로 변환되어 전송됩니다.
> 자세한 변경 내용은 `docs/CHATBOT_MIGRATION_NOTES.md` 참조.

```json
{
  "message": "노인 일자리 알려줘",
  "conversationId": "8c4f5a72-1f3e-4b2a-9c8e-7a1b3d4e5f6a",
  "user": {
    "userId": 1,
    "profile": {
      "age": 78,
      "gender": "F",
      "sidoName": "서울특별시",
      "sigunguName": "강남구",
      "incomeType": "기초연금수급자",
      "householdType": "혼자 살아요 (독거)",
      "isDisabled": false,
      "isMultiChild": false,
      "isMulticulturalNorthDefector": false,
      "isVeteran": false
    }
  }
}
```

**필드 명세**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `message` | String | ✅ | 사용자 메시지 (자연어, 최대 1000자). 빈 문자열 X. |
| `conversationId` | String (UUID v4) | ✅ | 백엔드가 발급. 같은 사용자의 후속 메시지에서 동일 ID 재전송. |
| `user.userId` | Long | ✅ | MOZI DB의 사용자 PK. 챗봇 서버는 로그·세션 추적에 사용. |
| `user.profile` | object | ⭕ | 사용자가 프로필을 입력하지 않은 경우 `null`. 프로필 없으면 LLM이 일반적 답변 생성. |
| `user.profile.age` | Integer | ⭕ | 만 나이 정수 (예: 78). 백엔드가 Clock 기반 계산. birthDate 미입력 시 `null`. |
| `user.profile.gender` | `M` / `F` / `NONE` | ⭕ | 성별 (`NONE`은 명시적 비공개). nullable. |
| `user.profile.sidoName` | String | ⭕ | 거주 시도 한글 풀네임 (예: "서울특별시"). REGION 미매칭 시 `null`. |
| `user.profile.sigunguName` | String | ⭕ | 거주 시군구 한글 풀네임 (예: "강남구"). "시도만 선택"이거나 미매칭이면 `null`. |
| `user.profile.incomeType` | String | ⭕ | 소득 유형 **한글 라벨** (예: "기초연금수급자", "차상위계층", "잘 모르겠어요"). 다섯 가지 enum 라벨 중 하나 또는 `null`. |
| `user.profile.householdType` | String | ⭕ | 가구 형태 **한글 라벨** (예: "혼자 살아요 (독거)", "배우자와 둘이 살아요", "손주를 키우고 있어요 (조손)"). 다섯 가지 enum 라벨 중 하나 또는 `null`. |
| `user.profile.isDisabled` | boolean | ✅ | 장애 여부 |
| `user.profile.isMultiChild` | boolean | ✅ | 다자녀 여부 |
| `user.profile.isMulticulturalNorthDefector` | boolean | ✅ | 다문화·탈북민 여부 |
| `user.profile.isVeteran` | boolean | ✅ | 보훈대상자 여부 |

> 💡 boolean **4종**은 사용자가 입력하지 않으면 모두 `false` (기본값). String/날짜·정수 필드는 `null` 가능.

**Enum 라벨 매핑** (Step 7 정책 — 백엔드가 변환 후 전송):

| IncomeType enum | 한글 라벨 (wire format) |
|---|---|
| `NATIONAL_BASIC_LIVING` | `"기초생활수급자"` |
| `NEAR_POVERTY` | `"차상위계층"` |
| `BASIC_PENSION` | `"기초연금수급자"` |
| `GENERAL` | `"해당 없음"` |
| `UNKNOWN` | `"잘 모르겠어요"` |

| HouseholdType enum | 한글 라벨 (wire format) |
|---|---|
| `LIVING_ALONE` | `"혼자 살아요 (독거)"` |
| `COUPLE` | `"배우자와 둘이 살아요"` |
| `WITH_CHILDREN` | `"자녀와 함께 살아요"` |
| `GRANDPARENT_GRANDCHILD` | `"손주를 키우고 있어요 (조손)"` |
| `OTHER` | `"그 외"` |

**제거된 필드 (Step 3·7 — 챗봇 측 처리 시 무시 가능)**:
- `birthDate` → `age` 정수로 변경 (개인정보 최소화)
- `sido` / `sigungu` (코드 + 자유 텍스트 혼용) → `sidoName` / `sigunguName` (한글 라벨)
- `isLivingAlone` → `householdType == "혼자 살아요 (독거)"`로 표현
- `isSingleParentGrandparent` → `householdType == "손주를 키우고 있어요 (조손)"`로 표현

#### Response Body (200 OK)

```json
{
  "reply": "강남구에 사시는 78세 어르신께 다음 복지를 추천드려요. ...",
  "conversationId": "8c4f5a72-1f3e-4b2a-9c8e-7a1b3d4e5f6a",
  "recommendedWelfareIds": ["WLF00001234", "BOK00000010", "SEL00000005"]
}
```

**응답 필드**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `reply` | String | ✅ | 사용자에게 보여줄 답변 텍스트 (자연어, 한국어) |
| `conversationId` | String | ✅ | Request의 conversationId를 **그대로 반환** (검증 + 클라이언트 동일성 확인) |
| `recommendedWelfareIds` | String[] | ✅ | 추천 복지 자연키 목록. 없으면 빈 배열 `[]`. 순서는 챗봇이 추천한 순서대로. |

**welfareId 형식**: MOZI DB의 `welfare_common.id` 자연키. 형식 예시:
- `WLF00001234` — 복지로 중앙부처/지자체
- `BOK00000010` — 복지로 민간
- `SEL00000005` — 서울복지포털

> 💡 **왜 복지 카드 정보(이름·설명·이미지 등)는 안 보내나요?**
> 챗봇 팀이 복지 카드 전체 정보를 만들 필요 X. **ID 문자열만 정확히 반환하면 MOZI 백엔드가 DB에서 카드 상세를 조회해 채워 넣음.** 챗봇 팀은 LLM·RAG에 집중하면 됨.
>
> 챗봇 팀이 사용할 복지 ID 목록은 MOZI 백엔드 측이 `seed-data/welfare-crawled/` 원본 데이터를 별도로 공유. 본 ID 문자열을 그대로 RAG 인덱스에 넣어두면 됨 (자체 변환 X).
>
> 💡 **`reply` 텍스트는 어디까지 자유롭나요?**
> 자연어 한국어 한 단락이면 충분. Markdown·HTML 같은 서식 문법은 사용 X (사용자에게 그대로 노출됨). 길이는 권장 200~500자 (노년층 가독성).

---

## 2.6 사용자 입력 분류 정책

챗봇 서버는 사용자 메시지를 다음 4가지 카테고리로 분류하고, 각 카테고리에 맞는 응답 형식을 따른다.
이 분류는 LLM 시스템 프롬프트 등 챗봇 서버 측 책임이며, MOZI 백엔드는 결과 형식만 검증한다.

| 입력 유형 | 예시 | `reply` | `recommendedWelfareIds` |
|---|---|---|---|
| 복지 추천 요청 | "노인 일자리 알려줘", "기초연금 어떻게 받아?" | 자연어 답변 + 추천 복지 안내 | 관련 복지 자연키 1~10개 (순서 = 추천도) |
| 일반 인사·잡담 | "안녕", "고마워" | 친근한 인사 + 도움 안내 | `[]` (빈 배열) |
| 서비스 범위 외 | "오늘 날씨", "주식 추천", "정치 의견" | "저는 복지 안내를 도와드리는 서비스예요. 복지에 대해 궁금한 점을 물어봐 주세요." 류 | `[]` (빈 배열) |
| 부적절·유해 입력 | 욕설, 차별·혐오 표현 | 정중한 거절 + 가이드 메시지 | `[]` (빈 배열) |

**핵심 약속**:
- **복지 추천이 적절하지 않은 모든 입력은 `recommendedWelfareIds: []` 빈 배열**. 챗봇이 자율 판단으로 무작위 복지를 추천하지 말 것.
- `reply`는 어떤 카테고리든 한국어 자연어로 채움 (빈 문자열 X).
- 노년층 사용자를 위해 답변 톤은 친절·존댓말 유지.

**MOZI 백엔드 측 동작 (참고)**:
- `recommendedWelfareIds=[]` 응답 → 백엔드는 그대로 `welfares: []`로 사용자에게 전달. reply만 표시됨.
- 백엔드는 "복지 카드 영역"을 별도로 비우거나 숨길지 결정 (프론트 UI 책임).

---

## 3. 에러 응답

> 💡 **4xx vs 5xx 구분 의미**
> HTTP 상태 코드 관례 — **4xx는 "호출자 잘못"**(요청을 잘못 보냄), **5xx는 "응답자 잘못"**(서버 내부 문제). 챗봇 팀은 어떤 상황에 어떤 코드를 반환할지 본 절 표를 기준으로 구현하면 됨.

### 4xx — 호출자(MOZI 백엔드) 측 오류
```json
{ "error": "INVALID_REQUEST", "message": "message field is required" }
```
- `400 INVALID_REQUEST` — message 누락, 필수 필드 누락, 타입 불일치
- `401 INVALID_API_KEY` — X-API-Key 헤더 누락 또는 잘못된 키

### 5xx — 챗봇 서버 측 오류
```json
{ "error": "INTERNAL_ERROR", "message": "..." }
```
- `500 INTERNAL_ERROR` — 챗봇 서버 내부 예외 (LLM 호출 실패, DB 장애 등)
- `503 OVERLOADED` — 일시적 과부하 (rate limit / queue full)

### MOZI 백엔드가 받아 사용자에게 안내하는 코드

챗봇 서버의 응답·실패는 MOZI 백엔드가 아래처럼 사용자용 코드로 변환해 전달한다. 챗봇 팀이 직접 다룰 일은 없으나, 어떤 상황이 어떤 사용자 메시지로 이어지는지 참고용:

| 챗봇 서버 상황 | MOZI 백엔드 → 사용자 코드 | HTTP |
|---|---|---|
| 챗봇 서버 5xx 응답 또는 연결 실패 | `CHATBOT_UNAVAILABLE` | 503 |
| 8초 타임아웃 초과 | `CHATBOT_TIMEOUT` | 504 |
| 응답 JSON이 명세와 다름 (예: `reply` 필드 없음) | `CHATBOT_INVALID_RESPONSE` | 502 |

> 💡 **"JSON 스키마"란?**
> "응답 본문에 어떤 필드가 어떤 타입으로 와야 하는가"의 약속. 본 문서 §2의 응답 필드 표가 그 스키마. 챗봇 서버가 `reply`를 빠뜨리거나 `recommendedWelfareIds`를 문자열(배열이 아닌)로 보내면 502로 분류되어 디버깅 신호가 됨.

---

## 4. ConversationId 정책

> 💡 **conversationId가 왜 필요한가요?**
> 챗봇은 사용자가 "더 자세히 알려줘", "방금 그거 신청 어떻게 해?" 같은 후속 질문을 했을 때 **이전 대화 맥락을 기억**해야 자연스러운 답변이 가능. 그 "같은 대화"를 구분하는 식별자가 conversationId.
>
> 예시 흐름:
> 1. 사용자가 "노인 일자리 알려줘" 첫 질문 → MOZI 백엔드가 UUID 발급해 챗봇 서버에 함께 전달 → 챗봇이 답변 + ID를 응답에 그대로 반환
> 2. 프론트가 받은 ID를 sessionStorage에 보관
> 3. 사용자가 "그중 첫 번째 더 알려줘" 후속 질문 → 프론트가 같은 ID를 다시 보냄 → 챗봇 서버가 ID로 이전 대화 이력을 찾아 맥락 이해 후 답변
>
> **UUID v4**란 충돌 가능성이 극히 낮은 36자 문자열(예: `8c4f5a72-1f3e-4b2a-9c8e-7a1b3d4e5f6a`). 자바·파이썬 표준 라이브러리에 생성 함수가 있음.

### 발급 책임
- **MOZI 백엔드가 UUID v4 발급**. Request에 `conversationId`가 없으면 백엔드가 새로 생성해 챗봇 서버에 전달.
- 챗봇 서버는 **절대 새 conversationId를 발급하지 않는다**. 받은 그대로 응답에 반환.

### 저장 책임
- **MOZI 백엔드**: 영속 저장 X (브릿지 역할만). DB에 ChatLog 테이블 X.
- **챗봇 서버**: LLM 컨텍스트 유지를 위해 메모리/Redis 등에 conversationId 기준 대화 이력 보관.
- **클라이언트**: 응답에서 받은 conversationId를 `sessionStorage`에 보관, 후속 요청 시 재전송.

### 유효 기간
- 챗봇 서버 보관 최소 30분 (한 세션 평균 길이) — 권장값, 합의 사항.
- 만료된 conversationId 재전송 시 챗봇 서버 동작: 새 컨텍스트로 시작해도 무방 (LLM 답변 품질만 영향).

### 새 대화 시작
- 클라이언트가 sessionStorage clear → 새 요청에 conversationId 없음 → 백엔드가 새 UUID 발급.

---

## 5. 운영 약속 (챗봇 팀 요청 사항)

> 본 섹션은 챗봇 팀과 회신을 주고받으며 빈칸을 채워나간다. 합의된 후 본 문서가 정식 계약이 된다.
>
> 💡 **각 항목 풀이**
> - **base URL**: 챗봇 서버 주소(예: `https://chatbot-dev.example.com`). MOZI 백엔드 설정에 박아넣을 값. 개발용·운영용 분리해서 알려주면 됨. 졸업 프로젝트 발표용이라 운영 URL이 따로 없으면 개발 URL 하나만 채워도 OK.
> - **API Key 발급 방식**: 챗봇 팀이 비밀 키를 만들어 MOZI 측에 공유 (예: 비공개 메신저로 일회성 전달). "키 회전"은 일정 주기로 키를 새로 발급해 보안성을 높이는 것 — 졸업 프로젝트 수준에서는 1회 발급으로 충분.
> - **헬스 체크**: 챗봇 서버가 살아 있는지 확인하는 단순 API. `GET /health` 호출 시 `200 OK`만 돌려주면 됨. 모니터링·시연 시 사전 확인용.
> - **Rate Limit (RPS = Requests Per Second)**: 초당 몇 건까지 받을 수 있는지. 예: "사용자당 1 RPS, 전체 20 RPS". 졸업 프로젝트 시연 수준에선 동시 1~2명이므로 넉넉히 두면 됨.
> - **SLA (Service Level Agreement)**: "어느 정도 안정성을 보장한다"는 약속. 예: "가용성 99%, 새벽 3~4시 점검". 졸업 프로젝트라면 "발표 기간 동안 가동 보장" 정도면 됨.
> - **로그 정책 / end-to-end 추적**: MOZI 백엔드가 발급한 conversationId를 챗봇 서버 로그에도 남기면, 문제 발생 시 양쪽 로그를 같은 ID로 매칭해 원인 추적이 쉬워짐.

- [ ] **개발 환경 base URL**: `___________________`
- [ ] **운영 환경 base URL**: `___________________`
- [ ] **API Key 발급 방식**: 챗봇 팀이 발급해 MOZI 백엔드에 공유. 키 회전 정책 합의 필요.
- [ ] **헬스 체크 엔드포인트**: `GET {CHATBOT_BASE_URL}/health` → 200 OK (모니터링용). 응답 본문 형식: TBD.
- [ ] **Rate Limit**: 사용자당 RPS / 전역 RPS — `___________________`
- [ ] **SLA**: 가용성 % + 점검 시간대 — `___________________`
- [ ] **로그 정책**: 챗봇 서버는 받은 `conversationId`를 로그에 포함해 end-to-end 추적 가능하게.
- [ ] **타임아웃 합의**: MOZI 백엔드는 8초로 끊는다. 챗봇 서버는 그 안에 응답 보장? 그 이상 걸리는 메시지는 별도 처리?

---

## 6. MOZI 백엔드 측 구현 메모

본 문서를 보는 챗봇 팀에게 참고 정보:

- MOZI 백엔드는 본 phase에서 **Mock 우선 구현** 완료 — 챗봇 서버 없이도 단독 시연 가능 (시드 복지 ID 무작위 추천).
- 실 챗봇 서버가 준비되면 `RestChatbotClient` 구현체를 별도 작업 단위로 추가, `mozi.chatbot.mock-enabled=false` 환경 설정으로 전환.
- 응답으로 받은 `recommendedWelfareIds` 중 MOZI DB에 존재하지 않는 ID는 자동으로 응답에서 제외 + WARN 로그. 챗봇 학습 데이터 갱신 신호로 활용 가능.
- 백엔드는 챗봇 서버 호출 시 사용자 JWT를 그대로 전달하지 않음. `X-API-Key`만 신뢰. 챗봇 서버는 `user.userId`로 사용자 식별.

> 💡 **용어 풀이**
> - **Mock(목)**: 실제 챗봇 서버 없이 "비슷한 척" 동작하는 가짜 구현. 챗봇 서버 개발 일정과 독립적으로 MOZI 백엔드가 먼저 동작하도록 만들어둔 것. 추후 챗봇 서버가 준비되면 설정 한 줄(`mock-enabled=false`)로 진짜 호출로 전환.
> - **JWT(JSON Web Token)**: MOZI 백엔드가 자체 사용자 인증에 쓰는 토큰. 챗봇 서버에는 이 토큰을 전달하지 않음 (보안 분리). 챗봇 서버 입장에서는 본 문서의 `X-API-Key`만 신뢰하고, 사용자 식별은 요청 본문의 `user.userId`로 하면 됨.
> - **WARN 로그**: 즉시 장애는 아니지만 추적이 필요한 경고. MOZI 백엔드가 모르는 ID가 챗봇으로부터 오면 챗봇 측 RAG 데이터가 최신이 아닐 수 있다는 신호.

---

## 7. 변경 이력

| 일자 | 변경 | 작성자 |
|---|---|---|
| 2026-05-15 | 초안 작성 (Phase 5) | MOZI 백엔드 |
| 2026-05-15 | §2.6 사용자 입력 분류 정책 추가 — 복지 추천 외 입력은 모두 `recommendedWelfareIds: []` 약속 | MOZI 백엔드 |
| 2026-05-15 | 챗봇 팀(학생 협업)을 위한 친절 풀이 보강 — 도입부 흐름도, 각 절 💡 용어·개념 풀이 추가 | MOZI 백엔드 |
| 2026-05-17 | **USER_PROFILE_REDESIGN Step 7 wire format 변경** — profile 12→10 필드, `birthDate`→`age`, `sido`/`sigungu`→`sidoName`/`sigunguName`(한글), `incomeType`/`householdType` 한글 라벨, `isLivingAlone`/`isSingleParentGrandparent` 제거. 챗봇팀 안내서는 `docs/CHATBOT_MIGRATION_NOTES.md` 참조. | MOZI 백엔드 |

향후 합의 사항이 추가될 때마다 §5와 본 표를 함께 갱신.
