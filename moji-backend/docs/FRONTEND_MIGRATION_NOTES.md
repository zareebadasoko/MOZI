# 프론트엔드 변경 안내서 — USER_PROFILE_REDESIGN (2026-05-17)

> 본 문서는 백엔드 `refactor/userprofile-region-redesign` 작업(Step 1~10)으로 발생한 **프론트엔드 호환성 영향**을 한 곳에 정리한 것이다. 기존 화면이 있다면 본 문서의 체크리스트를 따라 갱신하면 된다.
>
> 자세한 명세는 `API_SPEC.md` · `ERROR_CODES.md` · `FRONTEND_INTEGRATION_GUIDE.md` 참조.

## 0. TL;DR (요약 5줄)

1. **`GET /api/regions` 신규 엔드포인트** — 시도/시군구 cascading select용. 17개 시도 + 약 229 시군구.
2. **프로필 응답·요청 필드 6개 교체**: `sido`/`sigungu`(자유 텍스트) → `sidoCode`/`sigunguCode`(REGION 코드 2/5자리), `incomeType`/`householdType`은 enum value(`BASIC_PENSION`, `LIVING_ALONE` 등).
3. **boolean 6종 → 4종** — `isLivingAlone`, `isSingleParentGrandparent` 제거. 독거/조손 정보는 `householdType` enum이 흡수.
4. **`GET /api/welfares`에서 `applyMyProfile` query param 제거** — 본인 프로필 기반 자동 필터 폐기. 검색은 명시 query param만 사용.
5. **신규 에러 `INVALID_REGION_CODE`(400)** — 잘못된 sidoCode/sigunguCode 또는 시도-시군구 불일치 입력 시 발생.

---

## 1. Breaking Changes 표

| 영역 | Before | After | 프론트 Action |
|---|---|---|---|
| 프로필 응답 `sido` / `sigungu` (자유 텍스트) | `"서울특별시"` / `"강남구"` | **제거** | `sidoCode`/`sigunguCode` 사용 |
| 프로필 응답 — 신규 필드 | (없음) | `sidoCode: "11"`, `sigunguCode: "11680"` | `GET /api/regions`로 한글 라벨 매핑 또는 백엔드가 응답 시 추가하는 다른 필드와 매핑 (현 API는 코드만 노출) |
| 프로필 응답 `incomeType` | 자유 텍스트 `"기초연금수급자"` | enum value `"BASIC_PENSION"` | switch/Map으로 한글 라벨 매핑 |
| 프로필 응답 `householdType` | 자유 텍스트 `"독거"` | enum value `"LIVING_ALONE"` | 동일하게 매핑 |
| 프로필 응답 boolean | 6종 (isLivingAlone/isDisabled/isSingleParentGrandparent/isMultiChild/isMulticulturalNorthDefector/isVeteran) | **4종** (isDisabled/isMultiChild/isMulticulturalNorthDefector/isVeteran) | UI에서 독거·조손 체크박스 제거 → `householdType` select로 통합 |
| 프로필 요청 (`PUT /api/users/me/profile`) | 위 자유 텍스트 + boolean 6종 | 위 신규 스키마 | 입력 폼 컴포넌트 재구성 |
| 검색 `GET /api/welfares` | `?applyMyProfile=true&...` | 파라미터 제거 | URL builder에서 해당 키 제거 |
| 행정구역 데이터 | (백엔드 미제공, 프론트가 하드코딩 추정) | `GET /api/regions` 응답 사용 | 페이지 진입 시 1회 호출 후 캐싱 |

---

## 2. 상세 변경

### 2-1. 프로필 응답 (`GET /api/users/me/profile`)

**Before**
```json
{
  "isCompleted": true,
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
```

**After**
```json
{
  "isCompleted": true,
  "sidoCode": "11",
  "sigunguCode": "11680",
  "incomeType": "BASIC_PENSION",
  "householdType": "LIVING_ALONE",
  "isDisabled": false,
  "isMultiChild": false,
  "isMulticulturalNorthDefector": false,
  "isVeteran": false
}
```

### 2-2. 프로필 요청 (`PUT /api/users/me/profile`)

**Before**
```json
{ "sido": "서울특별시", "sigungu": "강남구", "isLivingAlone": true }
```

**After**
```json
{ "sidoCode": "11", "sigunguCode": "11680", "householdType": "LIVING_ALONE" }
```

부분 갱신 정책(absent/null = 무변경)은 동일.

### 2-3. Enum value → 한글 라벨 매핑 (프론트가 보관할 표)

```javascript
const INCOME_TYPE_LABEL = {
  NATIONAL_BASIC_LIVING: "기초생활수급자",
  NEAR_POVERTY: "차상위계층",
  BASIC_PENSION: "기초연금수급자",
  GENERAL: "해당 없음",
  UNKNOWN: "잘 모르겠어요",
};

const HOUSEHOLD_TYPE_LABEL = {
  LIVING_ALONE: "혼자 살아요 (독거)",
  COUPLE: "배우자와 둘이 살아요",
  WITH_CHILDREN: "자녀와 함께 살아요",
  GRANDPARENT_GRANDCHILD: "손주를 키우고 있어요 (조손)",
  OTHER: "그 외",
};
```

### 2-4. Welfare 검색 (`GET /api/welfares`)

`applyMyProfile` 파라미터 제거. 검색은 `keyword`/`category`/`region`/`welfareType` + `page`/`size`만 사용. 로그인 사용자는 응답 `isBookmarked`만 자동 채워짐.

**Before**
```
GET /api/welfares?keyword=병원비&applyMyProfile=true
```
**After**
```
GET /api/welfares?keyword=병원비
```

본인 맞춤 추천이 필요하면 `POST /api/chat`(챗봇)을 사용.

### 2-5. `GET /api/regions` 신규 사용법

```javascript
// 페이지 진입 시 1회
const { data } = await apiCall('/api/regions');
// data: [{ sidoCode, sidoName, sigungus: [{ code, name }] }, ...]

// 시도 select onChange
const onSidoChange = (sidoCode) => {
  setSelectedSido(sidoCode);
  setSelectedSigungu('');  // 시군구 초기화
};

// PUT 시
await apiCall('/api/users/me/profile', {
  method: 'PUT',
  body: JSON.stringify({
    sidoCode: selectedSido,
    sigunguCode: selectedSigungu || null,  // 시도만 선택은 null
    householdType: selectedHousehold,      // enum value
    incomeType: selectedIncome,
    isDisabled, isMultiChild, isMulticulturalNorthDefector, isVeteran,
  }),
});
```

자세한 코드 예시는 `FRONTEND_INTEGRATION_GUIDE.md §3-7` 참조.

---

## 3. 신규 에러 — `INVALID_REGION_CODE` (HTTP 400)

| 케이스 | 백엔드 메시지 (예시) | 프론트 처리 |
|---|---|---|
| `sigunguCode`만 단독 입력 | "시군구만 입력할 수 없어요. 시도부터 선택해주세요." | "시도부터 선택해주세요" 토스트 + 시도 select 강조 |
| `sidoCode`가 REGION에 없음 | "존재하지 않는 시도 코드예요." | `GET /api/regions` 재호출해서 캐시 갱신 |
| 시도-시군구 조합 불일치 | "입력한 시도에 속하지 않는 시군구예요." | 시군구 select 값 초기화 후 재선택 안내 |

```javascript
if (res.errorCode === 'INVALID_REGION_CODE') {
  toast(res.message);
  setSelectedSigungu('');
}
```

---

## 4. 빠른 점검 체크리스트 (화면별)

- [ ] **회원가입 직후 프로필 입력 화면** — 시도/시군구를 `<input type="text">`에서 `<select>`(cascading)로 교체. 독거/조손 체크박스 제거 후 가구형태 select 추가.
- [ ] **프로필 수정 화면** — 동일.
- [ ] **마이페이지 프로필 카드** — `sido`/`sigungu` 응답 필드 → `sidoCode`/`sigunguCode`로 변경 후 한글 라벨 매핑 (`GET /api/regions` 또는 enum 매핑표 활용).
- [ ] **복지 검색 화면** — `applyMyProfile` 토글이 있으면 UI 제거. 검색은 키워드+카테고리+지역 select만.
- [ ] **에러 토스트 / 폼 에러** — `INVALID_REGION_CODE`(400) 핸들러 추가.
- [ ] **챗봇 화면** — 변경 없음 (백엔드가 페이로드 변환 책임).

---

## 5. 참조 링크

- [API_SPEC.md](./API_SPEC.md) — 확정 엔드포인트 명세 (17개)
- [ERROR_CODES.md](./ERROR_CODES.md) — 신규 `INVALID_REGION_CODE` 포함 전체 에러 표
- [FRONTEND_INTEGRATION_GUIDE.md](./FRONTEND_INTEGRATION_GUIDE.md) — React + Vite 패턴 + cascading select 코드 예시
- [USER_PROFILE_REDESIGN_PLAN.md](./USER_PROFILE_REDESIGN_PLAN.md) — 본 변경의 근거 계획서 (As-Is / To-Be 비교 표 §8)
- Swagger UI — `http://localhost:8080/swagger-ui.html` (백엔드 부팅 시 자동 생성, 실제 응답 형태 직접 확인 가능)
