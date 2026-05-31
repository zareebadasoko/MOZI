"""
MOZI 복지 추천 답변 생성 모듈.

리트리버가 검색해 온 contexts 와 사용자 질문을 받아 답변 텍스트를 생성합니다.

------------------------------------------------------------
사용 예시 (리트리버 담당자):

    from api.generate import generate_answer

    # 리트리버 측에서 사용자 질문에 대해 top-k 문서 검색
    contexts: list[str] = retriever.search(question, top_k=5)

    # 답변 생성 — 그게 다임
    answer = generate_answer(
        question="저 기초연금 받을 수 있어요?",
        contexts=contexts,
        user_info="72세, 단독가구, 소득 180만원",   # 모르면 None 또는 생략
    )
    print(answer)

------------------------------------------------------------
메타데이터(latency, tokens)가 필요하면 generate() 사용:

    from api.generate import generate
    result = generate(question, contexts, user_info)
    result.answer            # 답변 텍스트
    result.latency           # 응답 시간(초)
    result.input_tokens      # 입력 토큰 수
    result.output_tokens     # 출력 토큰 수

------------------------------------------------------------
멀티턴 사용 (Summary + 직전 1턴 원본 + Query Rewriting):

    from api.generate import (
        generate_answer, summarize_history, rewrite_query, format_last_turn_qa,
    )

    # 세션 상태는 caller가 관리 (백엔드: Redis/Postgres 등에 저장)
    state = {"summary": "", "last_q": "", "last_a": ""}
    user_info = "75세, 서울 성북구, 1인 가구"

    for user_msg in conversation:
        last_qa_str = format_last_turn_qa(state["last_q"], state["last_a"])

        # ① (선택) 질의 재작성 — retriever 정확도 보장
        rewritten = rewrite_query(
            user_msg, user_info, state["summary"], last_qa_str,
        )

        # ② retriever 호출 (리트리버 측)
        contexts = retriever.search(rewritten, top_k=5)

        # ③ 답변 생성
        answer = generate_answer(
            user_msg, contexts, user_info,
            prev_summary=state["summary"],
            last_turn_qa=last_qa_str,
        )

        # ④ 상태 갱신 — 직전 1턴이 요약으로 흡수됨 (turn 2부터)
        if state["last_q"] or state["last_a"]:
            state["summary"] = summarize_history(
                state["summary"], state["last_q"], state["last_a"],
            )
        state["last_q"], state["last_a"] = user_msg, answer

    # 세션 종료 시 state를 영속화 (Redis SET 등)

------------------------------------------------------------
필요 환경:
    - pip install openai python-dotenv
    - 같은 디렉토리(또는 상위)에 .env 파일이 있고 OPENAI_API_KEY 설정됨

모델/파라미터: 실험(prompt_eng/) 으로 결정된 최적 조합
    - model       : gpt-4.1-mini
    - prompt      : v3_literal_rules + system/user 분리
                    (Role/Instructions/Output Format = system,
                     Context + 사용자 질문 = user)
    - temperature : 0.1
    - max_tokens  : 768
    - top_p       : 0.9

system/user 분리 이점:
    - OpenAI prompt caching 자동 적용 가능 → system 입력 토큰 비용 절감
    - 역할 분리 표준 패턴 — 지시문과 사용자 입력 경계 명확
"""
import os
import time
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv
from openai import OpenAI

# LangSmith (관측성): LANGSMITH_TRACING=true 일 때만 실제 추적됨.
# env var 미설정 시 wrap_openai/traceable은 no-op이라 코드는 안전하게 동작.
from langsmith import traceable
from langsmith.wrappers import wrap_openai

# .env 는 이 파일과 같은 폴더에서 찾는다 (cwd 와 무관하게 동작)
load_dotenv(dotenv_path=Path(__file__).resolve().parent / ".env")


# =============================================================================
# 모델 & 파라미터 (실험으로 결정된 최적값 — 변경 시 generation 품질에 영향)
# =============================================================================
MODEL = "gpt-4.1-mini"
TEMPERATURE = 0.1
MAX_TOKENS = 768
TOP_P = 0.9

# 멀티턴 보조 LLM (요약·재작성·분류) — 메인보다 저렴한 모델 권장
HELPER_MODEL = "gpt-4.1-nano"
HELPER_MAX_TOKENS_SUMMARY = 400
HELPER_MAX_TOKENS_REWRITE = 200
HELPER_MAX_TOKENS_CLASSIFY = 10


# =============================================================================
# 입력 의도 분류 — CHATBOT_API_CONTRACT §2.6 사용자 입력 분류 정책
#   - WELFARE      : 복지 추천·정보 요청 (검색 + LLM 답변 생성)
#   - CHITCHAT     : 인사·감사·잡담 (canned 안내문)
#   - OUT_OF_SCOPE : 복지 외 주제 (canned 안내문)
#   - HARMFUL      : 욕설·차별·혐오 등 부적절 입력 (canned 거절문)
#
# WELFARE 외 카테고리는 retrieve/rerank/메인 LLM 모두 스킵하고
# 미리 정의된 안내문 + 빈 welfare_ids 반환 (pipeline.py 에서 분기).
# =============================================================================
INTENT_LABELS = ("WELFARE", "CHITCHAT", "OUT_OF_SCOPE", "HARMFUL")

CANNED_REPLIES = {
    "CHITCHAT": (
        "안녕하세요! 저는 어르신께 복지 정보를 안내해 드리는 MOZI예요.\n"
        "어떤 복지가 궁금하신가요?\n"
        "예: \"노인 일자리 알려줘\", \"기초연금 어떻게 받아?\""
    ),
    "OUT_OF_SCOPE": (
        "저는 복지 안내를 도와드리는 MOZI예요. 그 부분은 답해 드리기 어려워요.\n"
        "복지에 대해 궁금한 점을 물어봐 주세요.\n"
        "예: \"노인 의료비 지원\", \"한부모 양육 지원\""
    ),
    "HARMFUL": (
        "그런 표현은 사용하지 말아 주세요. 저는 어르신들의 복지를 돕는 도우미예요.\n"
        "복지에 대해 궁금한 점이 있으시면 편하게 물어봐 주세요."
    ),
}

CLASSIFY_PROMPT = """당신은 노년층 복지 상담 챗봇 MOZI의 입력 분류기입니다.
사용자가 방금 보낸 메시지를 다음 4개 카테고리 중 정확히 하나로 분류하세요.

# 카테고리 정의
- WELFARE      : 복지 제도·혜택·자격·신청 방법 등 복지와 관련된 모든 질문·요청
                 (예: "노인 일자리 알려줘", "기초연금 어떻게 받아", "병원비 도움받을 곳",
                       "복지 추천해줘" 처럼 막연한 요청도 포함)
- CHITCHAT     : 인사·감사·감정 표현·자기소개 요청 등 짧은 잡담 (복지와 무관)
                 (예: "안녕", "고마워", "잘 지내?", "이름이 뭐야", "ㅎㅇ")
- OUT_OF_SCOPE : 복지가 아닌 다른 주제에 대한 정보·의견 요청
                 (예: "오늘 날씨", "주식 추천", "정치 의견", "요리법", "수학 문제 풀어줘")
- HARMFUL      : 욕설·차별·혐오·자해·성적 표현 등 부적절한 입력

# 규칙
- 의미가 모호하거나 판단하기 어려우면 WELFARE 로 분류 (놓치는 것보다 검색하는 게 안전).
- 출력은 4개 라벨 중 한 단어만. 설명·이유·따옴표·다른 어떤 텍스트도 추가하지 말 것.

# 입력
사용자 메시지: {message}

# 출력
분류:"""


# =============================================================================
# SYSTEM 프롬프트 (역할 + 규칙 + 출력 형식 — 매 호출 동일, prompt caching 대상)
#   근거: v3_literal_rules — gpt-4.1 literal instruction following 특성 활용,
#         "절대 하지 말 것" 부정 규칙으로 hallucination·막연한 안내 차단.
# =============================================================================
SYSTEM_PROMPT = """# Role and Objective
당신은 노년층(만 65세 이상)을 위한 복지 추천 서비스 'MOZI'의 상담 도우미입니다.
[복지 정보]에 명시된 내용만을 근거로 사용자의 질문에 답변하는 것이 목표입니다.

# Instructions

## 반드시 해야 할 것
- [복지 정보]에 명시된 내용만 사용해 답변
- 관련 정보가 [복지 정보]에 없으면 정확히 "제공된 자료에서는 해당 내용을 확인할 수 없습니다." 라고만 답변
- 어려운 행정용어는 풀어서 설명 (예: "수급권자" → "받을 수 있는 분")
- 정중하고 친근한 존댓말 ("~해요", "~드려요" 형태) 사용
- 숫자, 금액, 날짜, 연령 기준은 [복지 정보]에 적힌 그대로 정확히 인용
- 사용자 정보(나이, 거주 지역, 상황)가 주어지면 자격 기준과 비교하여 해당 여부를 명시

## 절대 하지 말 것
- [복지 정보]에 없는 복지명, 금액, 기준, 절차를 만들어내지 말 것
- "보통은", "일반적으로", "아마", "주민센터에 문의해보세요" 같은 막연한 표현 사용 금지
- 일반 상식, 외부 지식, 추측으로 [복지 정보]의 빈 칸 채우기 금지
- 사용자가 묻지 않은 부가 정보를 임의로 덧붙이지 말 것
- 인사말, 마무리말 같은 형식적 문구 사용 금지

## 대화 맥락 활용 (멀티턴)
- 사용자가 "그것", "그건", "그 중에서", "방금 말씀하신" 같은 지시 표현을 쓰면, [직전 대화] 원문을 먼저 보고 가리키는 대상을 찾고, 거기 없으면 [지난 대화 요약]을 참고할 것
- 이미 안내한 정보는 다시 처음부터 설명하지 말고, 이번 질문에서 새로 묻는 부분에만 집중해서 답할 것
- 사용자가 새로운 토픽으로 전환하면 이전 답변에 끌려가지 말고 새 질문에 맞춰 답할 것
- [직전 대화]가 "이전 대화 없음"이면 첫 턴으로 간주

# Output Format
- 추천 복지 이름
- 어떤 분이 받을 수 있는지 (지원 대상)
- 무엇을 받을 수 있는지 (지원 내용)
- 어떻게 신청하는지 (신청 방법)
- 문의처

여러 복지가 해당되면 가장 관련성 높은 것부터 최대 3개까지 위 형식을 반복합니다.

단, 사용자 메시지 끝에 "# 형식 지시 (이번 턴 한정)" 섹션이 있으면 위 기본 형식 대신 그 지시를 우선해서 따르세요.


# Examples

## 예시 1 — 정보형
[복지 정보]
[복지명] 노인 무릎인공관절수술 지원
[지원대상] 만 60세 이상, 건강보험급여 '인공관절치환술(슬관절)' 인정 기준에 준하는 질환자 중 기초생활수급자, 차상위계층, 한부모가족.
[지원내용] 검사비, 진료비 및 수술비에 대한 본인부담금(법정본인부담금)을 1인당 한쪽 무릎에 대해 최대 120만원까지 지원
[신청방법] 보건소 방문 신청
[문의처] 보건복지상담센터 129

[사용자 정보]
정보 없음

[사용자 질문]
무릎 수술비 지원받을 수 있나요?

[답변]
노인 무릎인공관절수술 지원
- 어떤 분이 받을 수 있는지: 만 60세 이상이면서 인공관절치환술 인정 기준에 해당하는 분 중 기초생활수급자, 차상위계층, 한부모가족
- 무엇을 받을 수 있는지: 한쪽 무릎당 검사비·진료비·수술비 본인부담금을 최대 120만 원까지 지원
- 어떻게 신청하는지: 가까운 보건소에 방문하여 신청
- 문의처: 보건복지상담센터 129

## 예시 2 — 자격조건형 (사용자 정보로 자격 판단)
[복지 정보]
[복지명] 기초연금
[지원대상] 만 65세 이상 어르신
[선정기준] 2025년 단독가구 소득인정액 2,280,000원 이하, 부부가구 3,648,000원 이하
[지원내용] 매월 25일 기준연금액(334,810원) 지급
[신청방법] 주민센터 방문 또는 복지로(www.bokjiro.go.kr) 온라인 신청
[문의처] 보건복지상담센터 129

[사용자 정보]
72세, 단독가구, 소득인정액 월 180만원

[사용자 질문]
저 기초연금 받을 수 있어요?

[답변]
네, 기초연금을 받으실 수 있어요. 만 65세 이상이시고 단독가구 소득인정액 228만 원 이하 기준에 해당하세요.
- 어떤 분이 받을 수 있는지: 만 65세 이상, 단독가구 소득인정액 228만 원 이하
- 무엇을 받을 수 있는지: 매월 25일에 기준연금액 33만 4,810원 지급
- 어떻게 신청하는지: 가까운 주민센터 방문 또는 복지로(www.bokjiro.go.kr)에서 온라인 신청
- 문의처: 보건복지상담센터 129

## 예시 3 — 정보부재형 (context에 답 없음)
[복지 정보]
[복지명] 노인 무릎인공관절수술 지원
[지원대상] 만 60세 이상, 기초생활수급자·차상위계층·한부모가족
[지원내용] 본인부담금 최대 120만원 지원
[문의처] 보건복지상담센터 129

[사용자 정보]
정보 없음

[사용자 질문]
치과 임플란트도 지원받을 수 있나요?

[답변]
제공된 자료에서는 해당 내용을 확인할 수 없습니다.
"""


# =============================================================================
# USER 메시지 템플릿 (Context + 질문 — 매 호출 가변)
# =============================================================================
USER_TEMPLATE = """# Context

## 사용자 정보
{user_info}

## 지난 대화 요약
{prev_summary}

## 직전 대화 (원문)
{last_turn_qa}

## 복지 정보
{retrieved_context}

# 사용자 질문
{user_question}
{format_directive}"""


# 후속 질의(같은 복지에 대한 추가 질문)일 때 USER 메시지 끝에 주입되는 형식 지시.
# initial / topic_switch 턴에는 빈 문자열을 넣어 SYSTEM_PROMPT의 기본 5개 항목 형식을 사용.
FOLLOWUP_FORMAT_DIRECTIVE = """

# 형식 지시 (이번 턴 한정)
이번 질문은 이전 대화에서 다룬 같은 복지에 대한 추가 질문입니다.
- 5개 항목 전체를 다시 출력하지 마세요.
- 사용자가 묻는 항목 하나에만 간결하게 답하세요 (해당 항목명 + 내용).
- 이미 안내한 정보를 반복하지 마세요."""


# =============================================================================
# 멀티턴 보조 프롬프트 — 요약(Summary) & 질의 재작성(Query Rewriting)
#
# - SUMMARY_PROMPT : 매 턴 답변 후, 이전 요약 + 직전 Q&A를 통합해 새 요약을 만듦.
#                    복지 도메인 특성상 숫자·정책명·전화번호 손실이 치명적이므로
#                    보존 규칙을 명시.
# - REWRITE_PROMPT : 후속 질의("그건 어떻게 신청해요?")를 standalone 문장으로 변환.
#                    서비스 모드에서 retriever 정확도를 보장하기 위함.
# =============================================================================
SUMMARY_PROMPT = """당신은 노년층 복지 상담 대화의 흐름을 요약하는 보조 모델입니다.
이전까지의 요약과 방금 진행된 한 턴(질문/답변)을 통합하여, 다음 턴에서 참고할 새 요약을 작성하세요.

# 반드시 보존할 정보 (절대 누락 금지)
- 사용자가 밝힌 개인 정보: 나이, 거주지, 가구 상황, 소득 수준, 가족 관계, 건강 상태 등
- 지금까지 안내된 복지명 (예: "기초연금", "노인맞춤돌봄서비스" — 정확한 명칭 그대로)
- 핵심 숫자: 금액, 연령 기준, 소득 기준, 전화번호, 한도액
- 사용자의 관심사·의도
- 아직 충분히 답변되지 않은 미해결 질문

# 제거해도 되는 정보
- 인사말, 형식적 마무리 ("더 궁금하신 점 있으시면 말씀해 주세요" 등)
- 반복 설명, 정중 표현
- 출력 형식상 덧붙은 부가 문장

# 출력 규칙
- 한국어 평서문, 300자 이내 한 문단
- 불릿·헤더·따옴표 없이 자연스러운 서술
- 사용자가 묻지 않은 추측은 추가하지 말 것
- "사용자는…", "지금까지…" 같이 3인칭 시점으로 객관적으로 기술

# 입력
## 이전까지의 요약
{prev_summary}

## 방금 진행된 한 턴
사용자 질문: {question}
도우미 답변: {answer}

# 출력
새 요약:"""


REWRITE_PROMPT = """당신은 노년층 복지 상담 챗봇의 멀티턴 대화에서, 사용자의 현재 질문을 standalone 문장으로 재작성하는 보조 모델입니다.

# 규칙
1. 현재 질문이 이미 standalone(맥락 없이도 의미가 통함)이면 **원문 그대로** 출력하세요.
2. 사용자가 새로운 토픽으로 전환했다면(이전 대화와 무관) **원문 그대로** 출력하세요.
3. "그것", "그건", "그 중에서", "방금 말씀하신", "아까 그" 등 지시·생략 표현이 있으면, 직전 대화에서 가리키는 대상을 찾아 명시적으로 치환하세요.
4. 사용자 정보(나이, 거주지, 상황)가 현재 질문의 자격 판단에 결정적이면 standalone 문장에 자연스럽게 포함하세요. 단, 불필요하게 길게 만들지 말 것.
5. 출력은 **재작성된 질문 한 문장만**. 설명·인사·따옴표 추가 금지.

# 예시

## 예시 1: 후속 질의 (지시어 치환)
사용자 정보: 정보 없음
이전 대화 요약: (없음)
직전 대화:
사용자: 기초연금이 뭐예요?
도우미: 기초연금은 만 65세 이상 어르신께 매달 드리는 연금이에요. ...
현재 질문: 그건 어떻게 신청해요?
재작성: 기초연금은 어떻게 신청하나요?

## 예시 2: 이미 standalone (변경 없음)
사용자 정보: 정보 없음
이전 대화 요약: (없음)
직전 대화: (이전 대화 없음)
현재 질문: 노인일자리 사업이 뭐죠?
재작성: 노인일자리 사업이 뭐죠?

## 예시 3: 토픽 전환 + 사용자 정보 활용
사용자 정보: 나이: 75세 / 거주지: 서울 성북구 / 상황: 1인 가구
이전 대화 요약: 사용자는 1인 가구 어르신을 위한 복지로 노인맞춤돌봄서비스를 안내받음.
직전 대화:
사용자: 그 중에서 돌봄 서비스 좀 더 자세히 알려주세요.
도우미: 노인맞춤돌봄서비스는 ...
현재 질문: 그럼 독감 예방주사도 무료로 맞을 수 있나요?
재작성: 75세 어르신이 독감 예방주사를 무료로 맞을 수 있나요?

# 입력
사용자 정보: {user_info}
이전 대화 요약: {prev_summary}
직전 대화:
{last_turn_qa}
현재 질문: {current_question}

# 출력
재작성:"""


# =============================================================================
# 응답 구조체
# =============================================================================
@dataclass
class GenerationResult:
    answer: str
    latency: float       # 응답 시간 (초)
    input_tokens: int
    output_tokens: int


# =============================================================================
# 내부: OpenAI 클라이언트 지연 초기화 (모듈 import 시점에 키 검증하지 않음)
# =============================================================================
_client: OpenAI | None = None


def _get_client() -> OpenAI:
    global _client
    if _client is None:
        api_key = os.getenv("OPENAI_API_KEY")
        if not api_key:
            raise RuntimeError(
                "OPENAI_API_KEY 환경변수가 필요합니다. .env 파일을 확인하세요."
            )
        # wrap_openai: LANGSMITH_TRACING=true 일 때만 자동 추적되고,
        # 아니면 일반 OpenAI 클라이언트처럼 동작 (no-op).
        _client = wrap_openai(OpenAI(api_key=api_key))
    return _client


# =============================================================================
# Public API
# =============================================================================
def format_contexts(contexts: list[str]) -> str:
    """리트리버 결과 list[str] 를 프롬프트용 단일 문자열로 변환."""
    return "\n\n".join(f"[문서 {i+1}]\n{ctx}" for i, ctx in enumerate(contexts))


def format_last_turn_qa(last_question: str, last_answer: str) -> str:
    """직전 1턴의 Q&A를 프롬프트용 문자열로 변환.

    첫 턴(둘 다 빈 문자열)이면 빈 문자열을 반환 — build_messages 쪽에서
    "이전 대화 없음"으로 치환됨.
    """
    if not last_question and not last_answer:
        return ""
    return f"사용자: {last_question.strip()}\n도우미: {last_answer.strip()}"


def build_messages(
    question: str,
    contexts: list[str],
    user_info: str | None = None,
    prev_summary: str = "",
    last_turn_qa: str = "",
    is_followup: bool = False,
) -> list[dict]:
    """OpenAI Chat API 에 그대로 넘길 messages 리스트 생성 (디버깅·검증용).

    멀티턴: prev_summary, last_turn_qa 전달.
    싱글턴: 둘 다 비워두면 "이전 대화 없음"으로 치환됨 (기존 동작과 동일).
    is_followup=True 면 USER 메시지 끝에 짧은 답변 형식 지시 주입.
        - 같은 복지에 대한 추가 질문에 사용 (대명사·생략 질의)
        - initial / topic_switch 턴에는 False (기본 5개 항목 형식 유지)
    """
    return [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": USER_TEMPLATE.format(
            user_info=(user_info or "정보 없음").strip(),
            prev_summary=prev_summary.strip() or "이전 대화 없음",
            last_turn_qa=last_turn_qa.strip() or "이전 대화 없음",
            retrieved_context=format_contexts(contexts).strip(),
            user_question=question.strip(),
            format_directive=FOLLOWUP_FORMAT_DIRECTIVE if is_followup else "",
        )},
    ]


@traceable(name="generate_answer", run_type="chain", metadata={"phase": "answer"})
def generate(
    question: str,
    contexts: list[str],
    user_info: str | None = None,
    prev_summary: str = "",
    last_turn_qa: str = "",
    is_followup: bool = False,
) -> GenerationResult:
    """답변 + 메타데이터(latency, tokens) 반환.

    멀티턴 사용 시 prev_summary, last_turn_qa 전달 (caller가 세션 상태 관리).
    is_followup=True 시 같은 복지에 대한 추가 질문으로 보고 짧은 답변 형식 강제.
    """
    messages = build_messages(
        question, contexts, user_info, prev_summary, last_turn_qa, is_followup,
    )
    client = _get_client()

    start = time.time()
    resp = client.chat.completions.create(
        model=MODEL,
        messages=messages,
        temperature=TEMPERATURE,
        max_tokens=MAX_TOKENS,
        top_p=TOP_P,
    )
    latency = time.time() - start

    return GenerationResult(
        answer=resp.choices[0].message.content,
        latency=latency,
        input_tokens=resp.usage.prompt_tokens,
        output_tokens=resp.usage.completion_tokens,
    )


def generate_answer(
    question: str,
    contexts: list[str],
    user_info: str | None = None,
    prev_summary: str = "",
    last_turn_qa: str = "",
    is_followup: bool = False,
) -> str:
    """답변 문자열만 반환하는 간편 함수 (가장 흔한 사용)."""
    return generate(
        question, contexts, user_info, prev_summary, last_turn_qa, is_followup,
    ).answer


def detect_followup(
    last_question: str,
    current_question: str,
    rewritten_question: str,
) -> bool:
    """후속질의(같은 복지 추가 질문) 휴리스틱.

    판단 기준:
      - 직전 턴이 없으면 → False (initial)
      - 재작성 결과가 원본과 동일하면 → False (이미 standalone 또는 새 토픽)
      - 재작성 결과가 원본을 의미있게 바꿨으면 → True (지시어 치환됨)

    한계: 토픽 전환(예: "그럼 독감 주사는?")에서도 재작성기가 user_info를 추가하면
    True로 잘못 분류될 수 있음. 정밀도가 중요한 경우 caller가 직접 turn_type을
    명시하는 게 안전 (예: goldset의 turn_type 필드 사용, 또는 별도 분류기).
    """
    if not last_question:
        return False
    return rewritten_question.strip() != current_question.strip()


# =============================================================================
# 멀티턴 오케스트레이션 — process_turn / process_conversation
#
# @traceable 데코레이터로 감싸면, 안에서 호출되는 다른 @traceable 함수들
# (rewrite_query, generate, summarize_history)이 자동으로 child trace가 되어
# LangSmith UI에서 계층 구조로 보임.
#
# 백엔드 통합 시 process_turn 하나만 호출하면 됨 (state in → state out 패턴).
# =============================================================================
@traceable(name="process_turn", run_type="chain")
def process_turn(
    user_msg: str,
    contexts: list[str],
    user_info: str | None,
    state: dict,
) -> dict:
    """한 턴 전체 처리: rewrite → answer → summarize.

    Args:
        user_msg: 사용자의 이번 턴 입력
        contexts: 리트리버가 반환한 복지 문서 리스트
        user_info: 사용자 프로필 ("72세, 단독가구..." 등). 없으면 None
        state: 직전 세션 상태 — {"summary": str, "last_q": str, "last_a": str}

    Returns:
        {
            "answer": str,              # 모델 답변
            "rewritten_query": str,     # 재작성된 standalone 질의 (retriever 입력용 / 로그용)
            "is_followup": bool,        # 후속질의 여부 (휴리스틱)
            "new_state": dict,          # 다음 턴에 넘길 갱신된 세션 상태
        }
    """
    last_qa_str = format_last_turn_qa(state["last_q"], state["last_a"])

    # ① 질의 재작성 (retriever 정확도 + followup 신호)
    rewritten = rewrite_query(user_msg, user_info, state["summary"], last_qa_str)

    # ② 후속질의 휴리스틱
    is_followup = detect_followup(state["last_q"], user_msg, rewritten)

    # ③ 답변 생성 (is_followup=True 면 짧은 형식)
    answer = generate_answer(
        user_msg, contexts, user_info,
        prev_summary=state["summary"],
        last_turn_qa=last_qa_str,
        is_followup=is_followup,
    )

    # ④ 상태 갱신 (직전 1턴이 요약으로 흡수됨 — turn 2부터)
    new_summary = state["summary"]
    if state["last_q"] or state["last_a"]:
        new_summary = summarize_history(
            state["summary"], state["last_q"], state["last_a"],
        )

    return {
        "answer": answer,
        "rewritten_query": rewritten,
        "is_followup": is_followup,
        "new_state": {
            "summary": new_summary,
            "last_q": user_msg,
            "last_a": answer,
        },
    }


@traceable(name="process_conversation", run_type="chain")
def process_conversation(
    turns: list[dict],
    user_info: str | None = None,
    initial_state: dict | None = None,
) -> list[dict]:
    """대화(여러 턴) 전체 처리. 평가·데모용 진입점.

    Args:
        turns: [{"question": str, "contexts": list[str]}, ...]
        user_info: 사용자 프로필 (대화 전체에 동일하게 적용)
        initial_state: 시작 상태. None이면 빈 상태에서 시작

    Returns:
        턴별 결과 리스트 (각각은 process_turn 반환값과 동일 구조, new_state 제외).
    """
    state = initial_state or {"summary": "", "last_q": "", "last_a": ""}
    results = []
    for turn in turns:
        result = process_turn(turn["question"], turn["contexts"], user_info, state)
        state = result["new_state"]
        results.append({
            "answer": result["answer"],
            "rewritten_query": result["rewritten_query"],
            "is_followup": result["is_followup"],
        })
    return results


# =============================================================================
# 멀티턴 보조 함수 — 요약(summarize_history) & 재작성(rewrite_query)
#
# 매 턴 호출 흐름 (caller가 세션 상태 {summary, last_q, last_a}를 보유):
#   ① rewritten = rewrite_query(user_msg, user_info, summary, last_qa_str)
#      → retriever 입력으로 사용 (서비스 모드)
#   ② contexts = retriever.search(rewritten)        # 서비스 측 로직
#   ③ answer   = generate_answer(user_msg, contexts, user_info,
#                                prev_summary=summary, last_turn_qa=last_qa_str)
#   ④ 상태 갱신:
#      if last_q or last_a:
#          summary = summarize_history(summary, last_q, last_a)
#      last_q, last_a = user_msg, answer
# =============================================================================
@traceable(name="summarize_history", run_type="chain", metadata={"phase": "summarize"})
def summarize_history(
    prev_summary: str,
    question: str,
    answer: str,
    model: str = HELPER_MODEL,
) -> str:
    """이전 요약 + 직전 Q&A를 통합해 새 요약 문자열 반환."""
    prompt = SUMMARY_PROMPT.format(
        prev_summary=prev_summary.strip() or "(없음 — 이번이 첫 턴)",
        question=question.strip(),
        answer=answer.strip(),
    )
    client = _get_client()
    resp = client.chat.completions.create(
        model=model,
        messages=[{"role": "user", "content": prompt}],
        temperature=0.0,
        max_tokens=HELPER_MAX_TOKENS_SUMMARY,
        top_p=1.0,
    )
    return resp.choices[0].message.content.strip()


@traceable(name="rewrite_query", run_type="chain", metadata={"phase": "rewrite"})
def rewrite_query(
    current_question: str,
    user_info: str | None = None,
    prev_summary: str = "",
    last_turn_qa: str = "",
    model: str = HELPER_MODEL,
) -> str:
    """후속 질의를 standalone 문장으로 재작성해 반환.

    이미 standalone이거나 새 토픽이면 원문을 그대로 반환하도록 프롬프트에서 지시됨.
    서비스 모드에서 retriever 입력 정확도 보장용.
    """
    prompt = REWRITE_PROMPT.format(
        user_info=(user_info or "정보 없음").strip(),
        prev_summary=prev_summary.strip() or "(없음)",
        last_turn_qa=last_turn_qa.strip() or "(이전 대화 없음)",
        current_question=current_question.strip(),
    )
    client = _get_client()
    resp = client.chat.completions.create(
        model=model,
        messages=[{"role": "user", "content": prompt}],
        temperature=0.0,
        max_tokens=HELPER_MAX_TOKENS_REWRITE,
        top_p=1.0,
    )
    return resp.choices[0].message.content.strip()


@traceable(name="classify_intent", run_type="chain", metadata={"phase": "classify"})
def classify_intent(message: str, model: str = HELPER_MODEL) -> str:
    """사용자 메시지를 WELFARE / CHITCHAT / OUT_OF_SCOPE / HARMFUL 중 하나로 분류.

    pipeline.py 에서 분기에 사용 — WELFARE 외 카테고리는 검색·메인 LLM 모두 스킵하고
    canned 응답을 반환한다.

    안전장치:
      - 분류기 출력이 4개 라벨에 매칭 안 되면 OUT_OF_SCOPE 로 폴백
        → 알 수 없는 입력은 안전하게 범위 외로 처리.
      - 멀티턴 후속 질의는 caller 가 is_followup=True 로 판단해 분류 자체를 스킵.
    """
    prompt = CLASSIFY_PROMPT.format(message=message.strip())
    client = _get_client()
    resp = client.chat.completions.create(
        model=model,
        messages=[{"role": "user", "content": prompt}],
        temperature=0.0,
        max_tokens=HELPER_MAX_TOKENS_CLASSIFY,
        top_p=1.0,
    )
    raw = resp.choices[0].message.content.strip().upper()
    for label in INTENT_LABELS:
        if raw.startswith(label):
            return label
    return "OUT_OF_SCOPE"


# =============================================================================
# 단독 실행 시 동작 확인용 (python generation.py)
# =============================================================================
if __name__ == "__main__":
    example_contexts = [
        (
            "[복지명] 기초연금\n"
            "[운영기관] 보건복지부\n"
            "[지원대상] 만 65세 이상 어르신으로서 소득인정액이 보건복지부장관이 "
            "정하여 고시하는 금액 이하인 사람\n"
            "[선정기준] 2025년 단독가구 소득인정액 2,280,000원 이하, "
            "부부가구 3,648,000원 이하\n"
            "[지원내용] 매월 25일에 기준연금액(334,810원)과 국민연금 급여액 등을 "
            "고려하여 기초연금액을 지급\n"
            "[신청방법] 가까운 주민센터 방문 또는 복지로(www.bokjiro.go.kr) 온라인 신청\n"
            "[문의처] 보건복지상담센터 129"
        )
    ]

    # ----- [1] 싱글턴 데모 -----
    print("=" * 60)
    print("싱글턴 데모")
    print("=" * 60)
    result = generate(
        question="저 기초연금 받을 수 있어요?",
        contexts=example_contexts,
        user_info="72세, 단독가구, 소득 월 180만원",
    )
    print(result.answer)
    print()
    print(f"latency: {result.latency:.2f}s | "
          f"tokens: in={result.input_tokens} out={result.output_tokens}")

    # ----- [2] 멀티턴 데모 (Summary + 직전 1턴 + Query Rewriting) -----
    print()
    print("=" * 60)
    print("멀티턴 데모")
    print("=" * 60)

    user_info = "72세, 단독가구, 소득 월 180만원"

    # 데모: 같은 contexts를 모든 턴에서 사용 (실제 서비스에선 턴마다 retrieve)
    demo_turns = [
        {"question": "저 기초연금 받을 수 있어요?",  "contexts": example_contexts},  # initial
        {"question": "그건 어떻게 신청해요?",         "contexts": example_contexts},  # followup
        {"question": "얼마나 받을 수 있나요?",        "contexts": example_contexts},  # followup
    ]

    # 한 줄 호출 — 내부적으로 turn 1, 2, 3 각각 process_turn 호출됨.
    # LangSmith trace 트리: process_conversation > process_turn x3 > {rewrite, answer, summarize}
    results = process_conversation(demo_turns, user_info=user_info)

    for i, (turn, r) in enumerate(zip(demo_turns, results), start=1):
        print(f"\n--- Turn {i} ---")
        print(f"User       : {turn['question']}")
        print(f"Rewrite    : {r['rewritten_query']}")
        print(f"is_followup: {r['is_followup']}")
        print(f"Answer     : {r['answer']}")
