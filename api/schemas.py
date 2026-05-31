"""Pydantic Request/Response 모델 (CHATBOT_API_CONTRACT §2 wire format).

Step 7 (2026-05-17) 적용 — profile 10 필드, age 정수, sidoName/sigunguName,
incomeType/householdType 한글 라벨.
"""

from __future__ import annotations

from typing import Literal, Optional

from pydantic import BaseModel, Field


# ============================================================
# Request
# ============================================================
class Profile(BaseModel):
    """사용자 프로필 (Step 7 wire format)."""
    age: Optional[int] = None
    gender: Optional[Literal["M", "F", "NONE"]] = None
    sidoName: Optional[str] = None
    sigunguName: Optional[str] = None
    incomeType: Optional[str] = None        # 한글 라벨 5종 또는 None
    householdType: Optional[str] = None     # 한글 라벨 5종 또는 None
    isDisabled: bool = False
    isMultiChild: bool = False
    isMulticulturalNorthDefector: bool = False
    isVeteran: bool = False


class UserContext(BaseModel):
    userId: int
    profile: Optional[Profile] = None       # 사용자가 프로필 미입력이면 None


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=1000)
    conversationId: str
    user: UserContext


# ============================================================
# Response
# ============================================================
class ChatResponse(BaseModel):
    reply: str
    conversationId: str
    recommendedWelfareIds: list[str]


# ============================================================
# Error
# ============================================================
class ErrorResponse(BaseModel):
    error: str
    message: str
