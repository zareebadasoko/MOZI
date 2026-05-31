"""Step 7 wire format Profile → generate.py 의 user_info 자연어 문자열 변환.

generate.py 의 user_info 형식 예: "72세, 단독가구, 소득 월 180만원" 같은 자연어.
LLM 프롬프트에 자연스럽게 끼워 넣기 위해 한국어 한 문장으로 변환한다.
"""

from __future__ import annotations

from typing import Optional

from .schemas import Profile


def profile_to_user_info(profile: Optional[Profile]) -> Optional[str]:
    """Profile (Step 7 wire format) → "72세 여성, 서울특별시 강남구 거주, 기초연금수급자, 혼자 살아요 (독거)" 형식.

    None / 빈 정보면 None 반환 → generate.py 가 "정보 없음" 으로 처리.
    """
    if profile is None:
        return None

    parts: list[str] = []

    # 나이 + 성별
    age_gender = []
    if profile.age is not None:
        age_gender.append(f"{profile.age}세")
    if profile.gender == "F":
        age_gender.append("여성")
    elif profile.gender == "M":
        age_gender.append("남성")
    if age_gender:
        parts.append(" ".join(age_gender))

    # 거주지
    if profile.sidoName:
        region = profile.sidoName
        if profile.sigunguName:
            region += f" {profile.sigunguName}"
        parts.append(f"{region} 거주")

    # 소득 유형 / 가구 형태 (한글 라벨 그대로)
    if profile.incomeType:
        parts.append(profile.incomeType)
    if profile.householdType:
        parts.append(profile.householdType)

    # 추가 자격 플래그
    flags: list[str] = []
    if profile.isDisabled:
        flags.append("장애인")
    if profile.isMultiChild:
        flags.append("다자녀 가구")
    if profile.isMulticulturalNorthDefector:
        flags.append("다문화·북한이탈주민")
    if profile.isVeteran:
        flags.append("국가유공자")
    if flags:
        parts.append(", ".join(flags))

    return ", ".join(parts) if parts else None
