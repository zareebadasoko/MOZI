# 챗봇 팀 변경 안내서 — USER_PROFILE_REDESIGN (2026-05-17)

> 본 문서는 MOZI 백엔드의 `refactor/userprofile-region-redesign` 작업으로 발생한 **챗봇 서버(`POST {CHATBOT_BASE_URL}/chat`)의 Request Body 변경 사항**을 한 곳에 정리한 것이다.
>
> 자세한 명세는 `CHATBOT_API_CONTRACT.md` §2 (변경 후 wire format) 참조.

## 0. TL;DR

`user.profile` 페이로드가 **12 필드 → 10 필드**로 축소·변환됩니다:

| 변경 종류 | 변경 내용 |
|---|---|
| 제거 | `birthDate`, `sido`, `sigungu`, `isLivingAlone`, `isSingleParentGrandparent` |
| 추가 | `age` (만 나이 정수), `sidoName` (한글), `sigunguName` (한글) |
| 변환 | `incomeType` 자유 텍스트 → 한글 라벨 5종 중 하나, `householdType` 동일 |

`message`, `conversationId`, `user.userId`, 응답 형태(`reply` + `recommendedWelfareIds`)는 변경 없음.

---

## 1. Before / After wire format

### Before (Step 3 이전)

```json
{
  "message": "노인 일자리 알려줘",
  "conversationId": "8c4f5a72-1f3e-4b2a-9c8e-7a1b3d4e5f6a",
  "user": {
    "userId": 1,
    "profile": {
      "birthDate": "1948-03-01",
      "gender": "F",
      "sido": "서울특별시",
      "sigungu": "강남구",
      "incomeType": "기초연금수급자",
      "householdType": "독거",
      "isLivingAlone": true,
      "isDisabled": false,
      "isSingleParentGrandparent": false,
      "isMultiChild": false,
      "isMulticulturalNorthDefector": false,
      "isVeteran": false
    }
  }
}
```

### After (Step 7 본격 매핑, 2026-05-17 적용)

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

---

## 2. 상세 변경

### 2-1. `birthDate` → `age` 정수

- **이유**: 개인정보 최소화. 챗봇 LLM은 만 나이만 알면 충분.
- **계산 책임**: 백엔드(`ChatService.calculateAge()`) — Clock 빈 기반 `Period.between(birthDate, today).getYears()`.
- **null 처리**: 사용자가 생년월일을 입력하지 않았으면 `age: null`.
- **챗봇 측 영향**: birthDate를 파싱하던 코드가 있으면 age 정수를 그대로 사용하도록 교체.

### 2-2. `sido` / `sigungu` → `sidoName` / `sigunguName`

- **이유**: 행정구역 코드 정규화(`sido_code`/`sigungu_code` VARCHAR 2/5자리)로 변경되면서, 챗봇에는 LLM 친화적인 한글 풀네임만 전달.
- **변환 책임**: 백엔드(`ChatService.resolveSidoName()` / `resolveSigunguName()`) — `RegionRepository.findFirstBySidoCode()` 또는 `findBySigunguCode()` lookup 후 sidoName/sigunguName 추출.
- **null 처리**:
  - 사용자가 시도를 미입력 → 둘 다 `null`
  - "시도만 선택" 케이스 (sigunguCode=null) → `sidoName` 있고 `sigunguName: null`
  - REGION 마스터에 없는 코드 → `null` (그래스풀 처리)
  - 세종특별자치시 → `sidoName: "세종특별자치시"`, `sigunguName: null`
- **챗봇 측 영향**: 코드("11", "11680")가 아닌 한글 풀네임("서울특별시", "강남구")이 도착하므로 LLM 프롬프트에 그대로 삽입 가능.

### 2-3. `incomeType` 한글 라벨 변환

| 백엔드 Enum | wire format (한글 라벨) |
|---|---|
| `NATIONAL_BASIC_LIVING` | `"기초생활수급자"` |
| `NEAR_POVERTY` | `"차상위계층"` |
| `BASIC_PENSION` | `"기초연금수급자"` |
| `GENERAL` | `"해당 없음"` |
| `UNKNOWN` | `"잘 모르겠어요"` |
| (사용자 미입력) | `null` |

### 2-4. `householdType` 한글 라벨 변환

| 백엔드 Enum | wire format (한글 라벨) |
|---|---|
| `LIVING_ALONE` | `"혼자 살아요 (독거)"` |
| `COUPLE` | `"배우자와 둘이 살아요"` |
| `WITH_CHILDREN` | `"자녀와 함께 살아요"` |
| `GRANDPARENT_GRANDCHILD` | `"손주를 키우고 있어요 (조손)"` |
| `OTHER` | `"그 외"` |
| (사용자 미입력) | `null` |

> ⚠️ Before에는 자유 텍스트(`"독거"`, `"부부"`, `"조손"` 등)로 도착하던 값이, After부터는 위 5종 라벨 중 하나로만 도착합니다. 챗봇 측에서 자유 텍스트를 가정한 파싱이 있다면 위 라벨 매핑 표로 갱신 필요.

### 2-5. boolean 6종 → 4종

| 제거 | 흡수 위치 |
|---|---|
| `isLivingAlone` | `householdType == "혼자 살아요 (독거)"` |
| `isSingleParentGrandparent` | `householdType == "손주를 키우고 있어요 (조손)"` |

남은 4종은 동일한 의미 + 동일한 위치(`user.profile.is*`):
- `isDisabled` (장애 여부)
- `isMultiChild` (다자녀 여부)
- `isMulticulturalNorthDefector` (다문화·탈북민 여부)
- `isVeteran` (보훈대상자 여부)

---

## 3. 변경되지 않은 부분 (호환 유지)

| 항목 | 상태 |
|---|---|
| `POST {CHATBOT_BASE_URL}/chat` 엔드포인트 경로 | ✅ 동일 |
| `X-API-Key` 헤더 | ✅ 동일 |
| Request `message`, `conversationId`, `user.userId` | ✅ 동일 |
| `user.profile = null` 케이스 (사용자가 프로필 미입력) | ✅ 동일 |
| Response 스키마 `{ reply, recommendedWelfareIds, conversationId }` | ✅ 동일 |
| ConversationId 정책 (백엔드 발급, UUID v4) | ✅ 동일 |
| 에러 응답 (4xx/5xx) | ✅ 동일 |

---

## 4. 챗봇팀 마이그레이션 체크리스트

- [ ] `user.profile.birthDate` 파싱 코드 제거 → `user.profile.age`(Integer) 사용
- [ ] `user.profile.sido` / `user.profile.sigungu` 참조 코드 제거 → `sidoName` / `sigunguName` 사용
- [ ] `user.profile.isLivingAlone` 참조 제거 → `householdType == "혼자 살아요 (독거)"` 로 판단 (필요 시)
- [ ] `user.profile.isSingleParentGrandparent` 참조 제거 → `householdType == "손주를 키우고 있어요 (조손)"` 로 판단 (필요 시)
- [ ] `incomeType` / `householdType` 한글 라벨 5종 매핑 표를 LLM 프롬프트에 반영
- [ ] `age`, `sidoName`, `sigunguName`이 `null`인 케이스 그래스풀 처리 (해당 정보 없는 사용자로 응대)

---

## 5. 적용 시점 / 롤백

- **백엔드 적용**: 2026-05-17 (Step 7 머지 후). 본 브랜치(`refactor/userprofile-region-redesign`) → `main` 머지 시점.
- **챗봇 측 대응 기한**: 백엔드 머지 직후. Mock 모드(`mock-enabled=true`) 사용 중인 환경에선 영향 없음.
- **롤백**: 백엔드 측에서 `git revert` 가능하지만 USER_PROFILE 스키마 변경이 포함되어 있어 DB 마이그레이션도 필요. 머지 전 챗봇 팀 확인 권장.

---

## 6. 참조 링크

- [CHATBOT_API_CONTRACT.md](./CHATBOT_API_CONTRACT.md) — 챗봇 ↔ 백엔드 정식 API 계약서 (본 변경 반영 완료)
- [USER_PROFILE_REDESIGN_PLAN.md](./USER_PROFILE_REDESIGN_PLAN.md) §2-5 — 본 변경의 근거 (옵션 C 채택 + 챗봇 페이로드 정책)
- 코드 위치 — `src/main/java/com/mozi/backend/domain/chat/client/dto/ChatbotUserContext.java` (Profile record 정의)
- 코드 위치 — `src/main/java/com/mozi/backend/domain/chat/service/ChatService.java` (loadUserContext / resolveSidoName / calculateAge)
