"""한국어 형태소 분석 — BM25 인덱스/쿼리 토큰화.

kiwipiepy 로 형태소 분석 후 의미 있는 품사(명사·외국어·숫자·한자) 만 추출.
용언(VV/VA)은 어간 변형이 까다로워 제외해도 검색 신호에 큰 영향 없음.

BM25 인덱스 빌드 시점과 쿼리 시점 모두 동일한 함수를 사용해야 일관성 확보.
"""

from __future__ import annotations

from typing import Callable, List

from kiwipiepy import Kiwi

# 토큰화 대상 품사
#   NNG: 일반명사  NNP: 고유명사  SL: 외국어  SN: 숫자  SH: 한자
KEEP_TAGS = {"NNG", "NNP", "SL", "SN", "SH"}
MIN_TOKEN_LEN = 1


def make_tokenizer() -> Callable[[str], List[str]]:
    """kiwipiepy 인스턴스를 클로저에 가둔 토큰화 함수 반환.

    Kiwi 객체는 thread-safe 하지 않으므로 호출자가 worker 당 1개씩 관리하는 게 안전.
    여기서는 한 인스턴스 단일 보유 (FastAPI lifespan 안에서 1회 생성).
    """
    kiwi = Kiwi()

    def _tok(text: str) -> List[str]:
        return [
            t.form
            for t in kiwi.tokenize(text)
            if t.tag in KEEP_TAGS and len(t.form) >= MIN_TOKEN_LEN
        ]

    return _tok
