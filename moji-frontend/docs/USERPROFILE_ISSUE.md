# USERPROFILE_ISSUE — `incomeType` / `householdType` 입력 방식 결정

> **이 문서가 단일 출처(single source of truth)다.**
> ProfilePage(`/me/profile`)의 `incomeType`·`householdType` 두 필드 입력 방식에 대해 `SCREEN_SPEC.md §10-3`의 표기(자유 입력 `Input`)와 본 문서가 충돌하면 **본 문서가 우선한다.**

---

## TL;DR (한 줄 요약)

`incomeType`과 `householdType`은 백엔드 사양상 `String(30)` 자유 문자열이지만 **노년층 UX 원칙에 따라 프론트에서 RadioGroup으로 옵션 선택만 받는다**. 백엔드는 손대지 않는다 (현재 시연 일정상). 시연 종료 후 백엔드 enum 도입으로 정공법 리팩토링 예정. 자세한 보완 계획은 §3.

---

## §1. 문제 설명

### 1-1. 발견 경위

`SCREEN_SPEC.md §10-3` ProfileForm 컴포넌트 트리에 다음 항목이 자유 입력 `Input`으로 명시되어 있었다:

```
├── Input (label="소득 유형", placeholder="예: 기초연금수급자")
├── Input (label="가구 형태", placeholder="예: 독거")
```

ProfilePage 구현을 시작하려는 시점에 다음 두 가지가 충돌함을 발견:

1. **백엔드 사양**: 두 필드는 `String(30)` 자유 문자열을 받음 (검증 어노테이션 없음)
2. **본 프로젝트의 UX 원칙**: 노년층 대상이므로 키보드 자유 입력 최소화 (오타·조작 부담)

이 문서는 그 충돌의 해결 결정과, 향후 정공법 리팩토링 계획을 담는다.

### 1-2. 백엔드 현재 사양 (확인 결과)

**엔티티 정의** — `backend_project/src/main/java/com/mozi/backend/domain/user/entity/UserProfile.java`:

```java
@Column(name = "income_type", length = 30)
private String incomeType;       // 검증 어노테이션 없음

@Column(name = "household_type", length = 30)
private String householdType;    // 검증 어노테이션 없음
```

- **타입**: `String` (primitive, **enum 아님**)
- **JPA 제약**: 최대 30자, nullable
- **Bean Validation**: 없음 (`@Pattern`, `@Size`, `@NotNull` 모두 미부착)

**DTO** — `UserProfileUpdateRequest.java`, `UserProfileResponse.java`:

- 두 필드 모두 `String`으로 그대로 정의
- 검증 규칙 없음

**Service** — `UserProfileService`:

- 변환·검증 없이 그대로 저장
- 그대로 응답 반환

→ **백엔드 입장에선 어떤 문자열이 와도 수용한다. Postman으로 `incomeType="ㅋㅋㅋ"` 보내도 200 OK.**

### 1-3. 두 필드의 실제 백엔드 사용처

| 사용처                    | 사용 여부 | 설명                                                                                                                                   |
| ------------------------- | --------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| **저장 (PUT)**            | ✅        | 받은 문자열 그대로 DB에 INSERT/UPDATE                                                                                                  |
| **조회 (GET 응답)**       | ✅        | 본인이 입력한 값 그대로 응답 (`UserProfileResponse`)                                                                                   |
| **복지 검색 매칭**        | ❌        | `WelfareService.extractStatusCodesFromProfile`이 boolean 6종만 사용. `incomeType`에 대해서는 "부정확한 결과 위험으로 미적용" 주석 명시 |
| **챗봇 페이로드 forward** | ✅        | `ChatbotUserContext.from()`에서 그대로 문자열로 LLM에 전달                                                                             |

→ **백엔드 비즈니스 로직에서 직접 비교·필터·분기에 쓰이지 않음.** 본인 GET 조회 + 챗봇 LLM 자연어 컨텍스트로만 흐른다.

### 1-4. 시드 데이터의 실제 값

`backend_project/src/main/java/com/mozi/backend/global/seed/UserSeedLoader.java`에서 7명의 시드 사용자에 박힌 값:

**incomeType:**

- `"기초연금수급자"` (6명) 또는 `null` (1명)
- 즉 **사실상 1가지 값만 사용 중**

**householdType:**

- `"독거"` / `"부부"` / `"한부모"` / `"조손"` (총 4가지)

이 값들이 **암묵적 표준**이다. 프론트가 보내는 문자열은 이 시드값과 일관되어야 챗봇이 학습된 맥락대로 동작할 가능성이 높다 (LLM 학습/프롬프트가 이 표현을 가정했을 수 있음).

### 1-5. Welfare 카테고리(THEME/STATUS)와의 도메인 분리

`../backend_project/docs/CATEGORY_REFERENCE.md`에 명시된 카테고리 22종(THEME 15 + STATUS 7)은 **Welfare 분류용**이지 UserProfile 필드가 아니다.

| 개념                          | 도메인               | 정해진 코드                                    |
| ----------------------------- | -------------------- | ---------------------------------------------- |
| THEME 15종 / STATUS 7종       | Welfare(복지)를 분류 | ✅ Category 마스터 테이블 + STS001~STS007 코드 |
| `incomeType`, `householdType` | User(사용자) 정보    | ❌ DB는 String(30) 자유 문자열                 |

CATEGORY_REFERENCE.md 명시: **"같은 의미라도 도메인이 달라 각자 관리."** 즉 의미적으로 비슷해도 (`STS002 저소득` ↔ `incomeType="기초생활수급자"`) 다른 테이블의 다른 컬럼이다.

### 1-6. boolean 6종과의 의미 중복

UserProfile에는 다음 6개 boolean이 있다:

- `isLivingAlone`, `isDisabled`, `isSingleParentGrandparent`, `isMultiChild`, `isMulticulturalNorthDefector`, `isVeteran`

`householdType`의 값과 boolean이 의미적으로 겹치는 부분:

| householdType 값 | 의미 중복되는 boolean            |
| ---------------- | -------------------------------- |
| `"독거"`         | `isLivingAlone=true`             |
| `"한부모"`       | `isSingleParentGrandparent=true` |
| `"조손"`         | `isSingleParentGrandparent=true` |
| `"부부"`         | (의미 중복 없음)                 |

→ "독거"를 선택한 사용자가 `isLivingAlone=false`를 같이 체크하는 모순 가능. 백엔드는 **이 모순을 막지 않는다.** 두 값을 그대로 저장하고 충돌 시 어느 쪽이 진실인지 정해주지 않는다.

본 결정에서는 **두 입력 모두 받기로** 했다. 이유: 검색 필터링은 boolean만 쓰고 (boolean 우선), `householdType`은 챗봇 LLM에 자연어 맥락을 풍부화하는 용도. LLM 입장에선 `"독거" + isLivingAlone=true`가 함께 오면 단순 중복으로 무시할 수 있고, `"한부모" + isSingleParentGrandparent=true`처럼 의미가 보강되는 케이스도 있다.

### 1-7. 왜 노년층 UX와 충돌하는가

자유 텍스트 `Input`은:

- 사용자가 직접 키보드로 한 글자씩 타이핑 → 노안·운동 부담
- 표기 통일 어려움 ("기초연금" vs "기초연금 수급자" vs "기초연금수급자") → 챗봇 응답 품질 하락
- 오타 정정 비용 큼
- 무엇을 적어야 하는지 모호 (placeholder만 가이드)

본 프로젝트 디자인 토큰·UX 체크리스트(`STYLING_GUIDE.md §10`)는 "본문 18px+, 버튼 48px+" 같은 시각적 기준 위주이지만, **입력 방식 자체도 "선택형 우선"이 노년층 친화의 핵심**이다.

### 1-8. 디자인 의도 해석 — 백엔드 설계가 "이상한" 게 아닐 수도 있다

처음엔 "왜 이 두 필드만 정규화 안 했지? ERD 미흡 아닌가?"로 보였다. 하지만 §1-3을 다시 보면:

- boolean 6종 = 백엔드가 직접 매칭에 쓰는 **구조화된 신호** → boolean으로 정규화
- `incomeType`, `householdType` = LLM에 던질 **자연어 슬롯** → 굳이 enum화할 가치 없음
- `sido`, `sigungu` = 챗봇 forward + 검색 region 파라미터 → String

이렇게 보면 백엔드 설계자가 **"매칭 신호 vs 자연어 컨텍스트"를 의도적으로 분리**했을 가능성도 있다. boolean으로 핵심 매칭은 다 커버되고, 미세한 뉘앙스(`"기초연금수급자였구나"`)는 LLM이 자연어로 해석하라는 의도.

**다만** 이 분리를 코드로 명시(주석·문서)하지 않았고, 검증 부재로 다른 클라이언트가 임의값을 박을 수 있는 게 미흡함. 그래서 §3에서 보완 작업을 제안한다.

---

## §2. 현재 선택 — 프론트 우회 (옵션 A 확정)

### 2-1. 결정 요약

| 항목           | 결정                                                                       |
| -------------- | -------------------------------------------------------------------------- |
| 백엔드 변경    | **없음** (코드·docs 모두 그대로)                                           |
| 프론트 UI      | **자유 Input 사용 금지**, 옵션 선택(RadioGroup) 전용                       |
| 두 필드 보존   | ✅ 유지 (제거 안 함). 챗봇 컨텍스트 풍부화 가치                            |
| 의미 중복 허용 | ✅ `householdType="독거"` + `isLivingAlone=true` 같이 받음                 |
| 백엔드 송신 값 | 시드 데이터와 일관된 정해진 한국어 라벨 (예: `"기초연금수급자"`, `"독거"`) |
| 시연 후 보완   | ✅ §3 계획대로 enum 도입 리팩토링 예정                                     |

**이유:**

1. 시연 일정상 백엔드 대공사 회피
2. 백엔드 매칭 로직에서 이 두 필드를 안 쓰므로 enum화 가치 낮음 (LLM이 자연어로 해석)
3. 데이터 무결성은 프론트가 통제 → 시연 환경에서 사실상 충분
4. 본 결정의 한계(다른 클라이언트 우회 가능)는 시연 종료 후 보완 작업으로 해결 가능

### 2-2. 프론트 구현 가이드

#### (A) ProfileForm 컴포넌트 트리 — RadioGroup으로 해석

> ⚠️ `SCREEN_SPEC.md §10-3`의 표기는 본 결정에 의해 **다음과 같이 오버라이드된다.**

수정된 트리 (SCREEN_SPEC.md를 직접 정정하지 않고 본 문서로만 명시):

```
ProfilePage
├── PageTitle ("내 정보 수정")
├── HelpText ("입력하실수록 추천이 더 정확해집니다.")
├── ProfileForm
│   ├── Input        (label="생년월일", type="date")            # 자유 입력 OK (date picker)
│   ├── RadioGroup   (label="성별", options=GENDER_OPTIONS)
│   ├── Select       (label="시도", options=SIDO_OPTIONS)
│   ├── Select       (label="시군구", options=SIGUNGU_OPTIONS_BY_SIDO[selectedSido])
│   ├── RadioGroup   (label="소득 유형", options=INCOME_TYPE_OPTIONS)        ← Input에서 변경
│   ├── RadioGroup   (label="가구 형태", options=HOUSEHOLD_TYPE_OPTIONS)     ← Input에서 변경
│   ├── ToggleGroup
│   │   ├── Toggle ("독거 여부", isLivingAlone)
│   │   ├── Toggle ("장애 여부", isDisabled)
│   │   ├── Toggle ("한부모·조손가정", isSingleParentGrandparent)
│   │   ├── Toggle ("다자녀 가정", isMultiChild)
│   │   ├── Toggle ("다문화·탈북민", isMulticulturalNorthDefector)
│   │   └── Toggle ("보훈대상자", isVeteran)
│   ├── Button (submit, primary, "저장")
│   └── Button (secondary, "취소", → /me)
```

핵심 변경점:

- `Input (소득 유형)` → **`RadioGroup`**
- `Input (가구 형태)` → **`RadioGroup`**
- 생년월일은 `<input type="date">`로 OS 기본 date picker 활용 (자유 텍스트 아님)

#### (B) 옵션 후보 값 — 시작점

> ⚠️ 본 시작점은 시드 데이터 + 노년층 복지 분류에서 흔히 쓰이는 값으로 구성. **확정값이 아니다.** Phase 4 ProfilePage 구현 시 사용자와 함께 한 번 더 정한다.

**`INCOME_TYPE_OPTIONS`** (4종 권장):

| 화면 라벨       | 백엔드 송신 값 (시드 일관)   | 비고           |
| --------------- | ---------------------------- | -------------- |
| 기초연금 수급자 | `"기초연금수급자"`           | ⭐ 시드 표준값 |
| 기초생활 수급자 | `"기초생활수급자"`           | 추가 후보      |
| 차상위 계층     | `"차상위계층"`               | 추가 후보      |
| 해당 없음       | `""` (빈 문자열) 또는 `null` | "선택 안 함"   |

**`HOUSEHOLD_TYPE_OPTIONS`** (5종 권장):

| 화면 라벨   | 백엔드 송신 값 (시드 일관) | 비고           |
| ----------- | -------------------------- | -------------- |
| 독거        | `"독거"`                   | ⭐ 시드 표준값 |
| 부부        | `"부부"`                   | ⭐ 시드 표준값 |
| 자녀와 거주 | `"자녀와거주"`             | 추가 후보      |
| 한부모      | `"한부모"`                 | ⭐ 시드 표준값 |
| 조손        | `"조손"`                   | ⭐ 시드 표준값 |

> 띄어쓰기 유의: 시드의 표기를 정확히 따른다. `"한부모"`(O), `"한 부모"`(X).

**`GENDER_OPTIONS`** (3종, 이미 백엔드 enum):

| 화면 라벨     | 백엔드 송신 값 (enum 그대로) |
| ------------- | ---------------------------- |
| 여성          | `"F"`                        |
| 남성          | `"M"`                        |
| 응답하지 않음 | `"NONE"`                     |

**시도/시군구**: 정적 상수 파일로 관리. 본 문서 범위 외 (Phase 4에서 한국 행정구역 정적 데이터를 `src/utils/regions.js`로 정의).

#### (C) 옵션 정의 위치 — 권장 파일

```
src/utils/profileOptions.js
```

```javascript
// src/utils/profileOptions.js

/**
 * profileOptions
 *
 * ProfilePage에서 사용하는 옵션 상수 모음.
 * 백엔드는 자유 문자열을 받지만, 본 프로젝트는 노년층 UX 원칙에 따라
 * 옵션 선택 UI만 사용하고 시드 데이터와 일관된 정해진 한국어 라벨만 전송한다.
 *
 * 자세한 결정 배경: docs/USERPROFILE_ISSUE.md §2
 */

export const GENDER_OPTIONS = [
  { label: "여성", value: "F" },
  { label: "남성", value: "M" },
  { label: "응답하지 않음", value: "NONE" },
];

export const INCOME_TYPE_OPTIONS = [
  { label: "기초연금 수급자", value: "기초연금수급자" },
  { label: "기초생활 수급자", value: "기초생활수급자" },
  { label: "차상위 계층", value: "차상위계층" },
  { label: "해당 없음", value: "" }, // 또는 null
];

export const HOUSEHOLD_TYPE_OPTIONS = [
  { label: "독거", value: "독거" },
  { label: "부부", value: "부부" },
  { label: "자녀와 거주", value: "자녀와거주" },
  { label: "한부모", value: "한부모" },
  { label: "조손", value: "조손" },
];
```

> 이 파일이 **프론트의 진실의 출처**가 된다. ProfileForm은 무조건 이 상수에서 옵션을 읽고, 사용자 선택값을 그대로 백엔드로 송신한다.

#### (D) RadioGroup 컴포넌트 — 의사 구현

본 프로젝트에 RadioGroup 컴포넌트가 아직 없다면 Phase 3 공통 컴포넌트로 만들고, 다음 형태를 따른다:

```jsx
/**
 * RadioGroup
 *
 * 옵션 중 하나를 선택하는 큰 버튼 그리드. 노년층 친화 (체크박스보다 시각적).
 *
 * @param {Object} props
 * @param {string} props.label
 * @param {Array<{label: string, value: string}>} props.options
 * @param {string} props.value
 * @param {(value: string) => void} props.onChange
 * @param {string} [props.error]
 */
function RadioGroup({ label, options, value, onChange, error }) {
  return (
    <fieldset className="block">
      <legend className="block text-senior-base text-ink-strong mb-3">
        {label}
      </legend>
      <div className="grid grid-cols-2 gap-3">
        {options.map((opt) => (
          <button
            key={opt.value}
            type="button"
            onClick={() => onChange(opt.value)}
            className={
              "h-12 px-4 rounded-soft border text-senior-base " +
              (value === opt.value
                ? "bg-brand text-ink-invert border-brand"
                : "bg-surface text-ink-strong border-surface-border hover:bg-surface-muted")
            }
          >
            {opt.label}
          </button>
        ))}
      </div>
      {error && <p className="mt-2 text-senior-sm text-danger">{error}</p>}
    </fieldset>
  );
}
```

> `STYLING_GUIDE.md §5`에 Button/Input/Card/Modal/Toast는 정의되어 있지만 RadioGroup은 없다. 위 코드를 `src/components/common/RadioGroup.jsx`에 추가하면 된다.

#### (E) PUT 요청 시 송신 형태

`api/user.js`의 `updateMyProfile(partial)` 호출 시 변경된 필드만 보낸다. 옵션 선택 결과를 그대로 string 또는 null로:

```javascript
// 예: 사용자가 소득 유형으로 "기초연금 수급자"를 골랐다면
await updateMyProfile({
  incomeType: "기초연금수급자", // INCOME_TYPE_OPTIONS의 value
  householdType: "독거", // HOUSEHOLD_TYPE_OPTIONS의 value
  isLivingAlone: true, // 토글
  // ... 다른 변경 필드만
});

// "해당 없음"을 골랐다면
await updateMyProfile({
  incomeType: "", // 또는 null — Phase 4 시점에 통일
});
```

빈 문자열로 보낼지 null로 보낼지는 백엔드 동작 확인 후 결정 (백엔드는 둘 다 받음 — 빈 문자열은 그대로 저장, null은 무변경 의미 가능성. Phase 4에서 점검).

### 2-3. 백엔드와의 송수신 약속

| 항목               | 약속                                                                                               |
| ------------------ | -------------------------------------------------------------------------------------------------- |
| 보내는 문자열      | `profileOptions.js`에 정의된 `value`만                                                             |
| 시드 데이터 일관성 | 시드값과 동일 표기 사용 (예: `"한부모"` 정확히)                                                    |
| 검증 책임          | 프론트 단독 (백엔드는 어떤 값이든 수용)                                                            |
| 응답 처리          | GET 응답에서 받은 문자열을 그대로 옵션 라벨에 매핑 (옵션에 없는 값이 오면 "기타" 또는 미선택 표시) |

GET 응답에서 옵션에 없는 값이 올 가능성:

- 시드값이 직접 박힌 경우 (시연용 사용자) → 옵션과 100% 일치
- 다른 클라이언트(Postman 등)가 임의값을 박은 경우 → 본 프로젝트 외에서 발생. 시연 환경에선 없음
- 보완 작업(§3) 진행 후 enum 도입 시 → 검증으로 차단됨

**방어 코드 권장:**

```javascript
// GET 응답 처리 시
const matched = INCOME_TYPE_OPTIONS.find((o) => o.value === profile.incomeType);
const selectedValue = matched ? matched.value : ""; // 못 찾으면 "해당 없음"
```

### 2-4. 기존 frontend docs와의 관계 — 본 문서가 오버라이드

본 문서는 시간 제약으로 SCREEN_SPEC.md·CLAUDE.md를 정정하지 않고 만들어졌다. 따라서:

| 기존 docs 표기                                        | 본 문서의 해석                                                                                      |
| ----------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| `SCREEN_SPEC.md §10-3`: `Input (label="소득 유형")`   | **`RadioGroup`으로 읽어라** (§2-2 참조)                                                             |
| `SCREEN_SPEC.md §10-3`: `Input (label="가구 형태")`   | **`RadioGroup`으로 읽어라**                                                                         |
| `SCREEN_SPEC.md §10-4`: 토글 옆 도움말 한 줄          | 그대로 적용 ("독거 여부 — 혼자 거주하시는 경우 켜주세요")                                           |
| `SCREEN_SPEC.md §10-5`: 빈 상태 (isCompleted=false)   | 그대로 적용                                                                                         |
| `SCREEN_SPEC.md §10-6`: 에러 상태 `VALIDATION_FAILED` | 그대로 적용 — 단 본 결정 하에선 프론트가 옵션만 보내므로 백엔드 VALIDATION_FAILED는 거의 발생 안 함 |
| `CLAUDE.md`: "자유 텍스트 최소화" 원칙                | **본 문서에만 명시**되어 있음                                                                       |

**일반 원칙 (본 문서로 박아둠):**

> 사용자 입력 중 자유 텍스트는 **챗봇 메시지에 한정**.
> 그 외 모든 입력(프로필·검색 필터·카테고리·옵션 선택)은 **버튼/Select/RadioGroup 등 선택형 UI로 통일**.
> 예외: 이메일·비밀번호·날짜(date picker)·챗봇 메시지.

### 2-5. 현재 결정의 한계 (솔직한 평가)

| 한계                                                                      | 시연 환경에서의 영향                | 시연 후 영향                          |
| ------------------------------------------------------------------------- | ----------------------------------- | ------------------------------------- |
| 다른 클라이언트가 임의 문자열 주입 가능                                   | 거의 없음 (시연자가 프론트만 사용)  | 운영 환경 가정 시 위험 — §3 보완 필요 |
| 데이터 무결성 = 프론트 신뢰만                                             | 거의 없음                           | 동일                                  |
| 옵션 외 값이 DB에 섞일 가능성                                             | 시드는 일관, 사용자 입력은 통제됨   | 동일                                  |
| 백엔드 docs와 프론트 UI 불일치 (백엔드는 자유 String 표기, 프론트는 옵션) | 본 문서가 가교 역할                 | enum 도입 후 자연 해소                |
| `householdType` ↔ `isLivingAlone` 모순 가능                               | 사용자 자유 의지 (백엔드도 안 막음) | LLM이 의미 우선순위 판단              |

→ **시연 환경에서는 실질적 문제 없음.** 단 졸업 프로젝트의 "정상 API" 자존심 측면에서 §3 보완 작업을 권장한다.

### 2-6. 검증 체크리스트 (ProfilePage 구현 후 확인)

- [ ] ProfileForm 안에 자유 `Input`이 0건이다 (생년월일 date picker 제외)
- [ ] `incomeType`, `householdType`이 `RadioGroup`으로 구현되어 있다
- [ ] 옵션 값이 `profileOptions.js`의 상수에서만 온다 (하드코딩 0건)
- [ ] 옵션 값이 시드 표기와 정확히 일치한다 (`"한부모"` 등)
- [ ] PUT 요청 body에 옵션 외 문자열이 없다 (DevTools Network 탭 확인)
- [ ] GET 응답에 옵션 외 값이 와도 화면이 깨지지 않는다 (방어 코드)
- [ ] 노년층 UX 체크: 모든 RadioGroup 버튼 높이 48px 이상, 폰트 18px 이상

---

## §3. 향후 리팩토링 — 시연 종료 후 모범 해결

> 이 섹션은 시연 발표 끝나고 졸업 프로젝트 마무리/포트폴리오화 단계에서 실행한다.
> 현재 결정(옵션 A)의 한계를 정공법으로 메우는 작업이다.

### 3-1. 보완 작업 진입 시점

- 시연 발표 끝난 직후 ~ 졸업 프로젝트 최종 제출 사이
- 프론트엔드와 백엔드 동시 작업 가능 (이미 양쪽 모두 손에 익은 상태)
- 권장 일정: **1~2일** (옵션 C 기준)

### 3-2. 보완 옵션 비교

| 옵션                                | 작업량     | 데이터 무결성 | ERD 일관성           | 권장                         |
| ----------------------------------- | ---------- | ------------- | -------------------- | ---------------------------- |
| **B. DTO 검증만 강화** (`@Pattern`) | 1~2시간    | 강함          | 변화 없음            | 최소 침습                    |
| **C. Java enum 도입**               | 반나절~1일 | 가장 강함     | enum 패턴            | ⭐ 모범 (권장)               |
| **D. 마스터 테이블 추가**           | 1~2일      | 가장 강함     | Category와 동일 패턴 | 과도. 졸업 프로젝트엔 C 충분 |

옵션 B의 한계:

- DB 컬럼은 여전히 String → 운영 시 새 값 추가는 `@Pattern` 코드 변경 필요
- 일관성: Category처럼 정규화된 도메인과 비교하면 여전히 어색

옵션 D의 가치:

- 새 값 추가가 DB 시드만 변경하면 됨 → 운영 친화
- Category와 패턴 일관 → 코드 전반의 균질감

졸업 프로젝트 규모에선 **C가 80점 → 95점 갭을 메우는 최적해**. D는 95점 → 98점인데 작업량은 2배.

### 3-3. 권장 경로 — 옵션 C: Java enum 도입 (상세)

#### 3-3-1. 새 enum 정의

위치: `backend_project/src/main/java/com/mozi/backend/domain/user/entity/`

```java
// IncomeType.java
public enum IncomeType {
    BASIC_PENSION("기초연금수급자"),
    BASIC_LIVING("기초생활수급자"),
    NEAR_POVERTY("차상위계층");

    private final String displayName;

    IncomeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // 챗봇 페이로드에서 자연어 표기로 변환할 때 사용
    @Override
    public String toString() {
        return displayName;
    }
}

// HouseholdType.java
public enum HouseholdType {
    ALONE("독거"),
    COUPLE("부부"),
    WITH_CHILDREN("자녀와거주"),
    SINGLE_PARENT("한부모"),
    GRANDPARENT("조손");

    private final String displayName;
    // ... 동일 패턴
}
```

#### 3-3-2. UserProfile 엔티티 변경

```java
@Enumerated(EnumType.STRING)
@Column(name = "income_type", length = 30)
private IncomeType incomeType;     // String → enum, DB 컬럼 String 유지

@Enumerated(EnumType.STRING)
@Column(name = "household_type", length = 30)
private HouseholdType householdType;
```

**DB 마이그레이션 회피 포인트:**

- `@Enumerated(EnumType.STRING)`은 DB에는 enum 이름(`BASIC_PENSION`, `ALONE` 등)을 저장
- 기존 DB의 `"기초연금수급자"` 같은 한국어 문자열과 불일치 → 마이그레이션 SQL 필요
- **대안**: enum의 `name()`을 영어 enum 이름으로 하지 말고, **`displayName`을 그대로 저장**하는 `AttributeConverter` 사용

```java
@Converter
public class IncomeTypeConverter implements AttributeConverter<IncomeType, String> {
    @Override
    public String convertToDatabaseColumn(IncomeType attr) {
        return attr == null ? null : attr.getDisplayName();
    }
    @Override
    public IncomeType convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        return Arrays.stream(IncomeType.values())
            .filter(e -> e.getDisplayName().equals(dbValue))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Unknown IncomeType: " + dbValue));
    }
}
```

이러면 **기존 DB의 한국어 문자열을 그대로 두고 코드만 enum화** 가능. 마이그레이션 SQL 없이 진행.

#### 3-3-3. DTO 변경

```java
public record UserProfileUpdateRequest(
    // ...
    IncomeType incomeType,           // String → enum
    HouseholdType householdType,
    // ...
) {}
```

Spring이 자동으로 enum 변환을 시도하므로, 클라이언트가 `"기초연금수급자"` 외의 값을 보내면 **400 Bad Request로 자동 차단**된다. 단 이 자동 변환은 enum의 `name()`을 기본으로 한다 — `displayName` 기반으로 받으려면 `@JsonCreator` 또는 커스텀 deserializer 필요:

```java
public enum IncomeType {
    BASIC_PENSION("기초연금수급자"),
    // ...

    @JsonCreator
    public static IncomeType fromDisplayName(String value) {
        return Arrays.stream(values())
            .filter(e -> e.getDisplayName().equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid IncomeType: " + value));
    }

    @JsonValue
    public String toJson() {
        return displayName;
    }
}
```

`@JsonValue`로 응답 시에도 displayName으로 직렬화 → 프론트는 변경 없이 한국어 값 그대로 받음.

#### 3-3-4. 시드 변경

`UserSeedLoader.java`:

```java
// Before
.incomeType("기초연금수급자")
.householdType("독거")

// After
.incomeType(IncomeType.BASIC_PENSION)
.householdType(HouseholdType.ALONE)
```

#### 3-3-5. 챗봇 페이로드 변환

`ChatbotUserContext.from(UserProfile p)`:

```java
// Before
.incomeType(p.getIncomeType())                     // String 그대로

// After
.incomeType(p.getIncomeType() == null ? null : p.getIncomeType().getDisplayName())
```

또는 enum의 `toString()`이 displayName을 반환하도록 해두면 자동으로 처리됨.

#### 3-3-6. 테스트 추가

- enum 값 변환 단위 테스트 (정상값 / 잘못된 값 / null)
- DTO 검증 통합 테스트 (잘못된 문자열로 PUT → 400 응답 + `VALIDATION_FAILED` errorCode)
- 시드 적재 후 DB에 한국어 displayName이 저장되는지 확인 (Converter 동작)
- GET 응답에서 한국어 displayName이 그대로 반환되는지 확인 (`@JsonValue`)

#### 3-3-7. 백엔드 docs 정정

- `ERD_REQUIREMENTS.md`: UserProfile 필드에서 두 enum 추가, 의도 명시 ("LLM 컨텍스트 슬롯이지만 enum으로 검증 강화")
- `API_SPEC.md`: `UserProfileUpdateRequest`의 두 필드 타입을 enum 값 목록과 함께 명시
- `CATEGORY_REFERENCE.md`: enum 값 ↔ Category STATUS 매핑 표 추가 (가능하면), 또는 "별개 도메인" 명시 강화

#### 3-3-8. 프론트 정정 (보완 작업 마무리)

- `profileOptions.js`의 `value`를 enum displayName과 정확히 일치시킴 (이미 일치하면 변경 없음)
- 본 문서(`USERPROFILE_ISSUE.md`) §2를 "Legacy" 표기로 보존 (히스토리 자료) 또는 §3 완료 후 본 문서 자체를 archive로 이동
- `SCREEN_SPEC.md §10-3`을 정정 (Input → RadioGroup) — 보완 후 본 문서의 오버라이드 역할 종료
- `CLAUDE.md`에 자유 텍스트 최소화 일반 원칙 추가 (선택사항)

### 3-4. 더 모범적 옵션 — 옵션 D: 마스터 테이블 (참고)

옵션 D를 진짜로 가고 싶다면:

```sql
CREATE TABLE income_types (
  code VARCHAR(30) PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  display_order INT
);

INSERT INTO income_types VALUES
  ('BASIC_PENSION', '기초연금수급자', 1),
  ('BASIC_LIVING', '기초생활수급자', 2),
  ('NEAR_POVERTY', '차상위계층', 3);

ALTER TABLE user_profile
  ADD CONSTRAINT fk_income_type
    FOREIGN KEY (income_type) REFERENCES income_types(code);
```

장점:

- 새 값 추가가 DB INSERT만으로 가능
- Category 패턴과 완전 일관
- 운영 도구로 값 관리 가능

단점:

- 작업량 2배 (테이블·FK·시드 추가)
- 졸업 프로젝트 범위엔 과도

**권장: 옵션 C로 충분. D는 별도 학습 목적이 있을 때만.**

### 3-5. 추천 마이그레이션 절차 — 안전한 단계별 진행

| 단계 | 작업                                                 | DB 영향                   | 검증                          |
| ---- | ---------------------------------------------------- | ------------------------- | ----------------------------- |
| 1    | enum + Converter 정의, DTO 변경 (필드만 변경)        | 없음                      | 컴파일 통과                   |
| 2    | UserSeedLoader enum 사용으로 변경                    | 없음 (시드값 한국어 동일) | 시드 적재 후 DB 값 확인       |
| 3    | ChatbotUserContext.from() enum displayName 사용      | 없음                      | 챗봇 호출 시 페이로드 확인    |
| 4    | DTO `@JsonCreator`/`@JsonValue` 추가                 | 없음                      | 잘못된 값 PUT → 400 응답 확인 |
| 5    | 백엔드 docs 정정 (ERD/API_SPEC/CATEGORY_REFERENCE)   | 없음                      | 문서 grep 정합성              |
| 6    | 프론트 `profileOptions.js` 검증 (현재값과 일치 확인) | 없음                      | ProfilePage 동작 확인         |
| 7    | 통합 테스트 (E2E 시연 시나리오 재실행)               | 없음                      | 골든 패스 통과                |

**DB 마이그레이션이 단계별로 없는 게 핵심.** Converter 트릭 덕분에 기존 DB 값 그대로 두고 코드만 정공법으로 바꾼다.

### 3-6. 보완 후 검증

- [ ] Postman으로 `incomeType="ㅋㅋㅋ"` PUT → **400 VALIDATION_FAILED** (전엔 200이었음)
- [ ] Postman으로 `incomeType="기초연금수급자"` PUT → 200 OK
- [ ] 시드 적재 후 DB의 `income_type` 컬럼이 여전히 한국어로 저장됨
- [ ] GET `/api/users/me/profile` 응답의 `incomeType`이 한국어로 반환됨 (프론트 변경 없음)
- [ ] 챗봇 호출 시 페이로드의 `incomeType`이 한국어 자연어로 LLM에 전달됨
- [ ] ProfilePage 기능 동작 변경 없음 (사용자 관점에선 같음)
- [ ] 본 문서(`USERPROFILE_ISSUE.md`) §2의 모든 우회 가이드가 더 이상 필요 없음 → 본 문서를 archive 처리

---

## §4. 참고 자료

### 백엔드 파일 경로 (본 결정의 근거 코드)

- `backend_project/src/main/java/com/mozi/backend/domain/user/entity/UserProfile.java` — 엔티티
- `backend_project/src/main/java/com/mozi/backend/domain/user/dto/UserProfileUpdateRequest.java` — 입력 DTO
- `backend_project/src/main/java/com/mozi/backend/domain/user/service/UserProfileService.java` — 비즈니스 로직
- `backend_project/src/main/java/com/mozi/backend/domain/welfare/service/WelfareService.java` — 검색 매칭 로직 (`extractStatusCodesFromProfile`)
- `backend_project/src/main/java/com/mozi/backend/global/seed/UserSeedLoader.java` — 시드값 출처
- `backend_project/src/main/java/com/mozi/backend/domain/chat/client/dto/ChatbotUserContext.java` — 챗봇 페이로드 변환

### 백엔드 docs

- `../../backend_project/docs/ERD_REQUIREMENTS.md` — UserProfile 필드 정의
- `../../backend_project/docs/API_SPEC.md` — UserProfileUpdateRequest 사양
- `../../backend_project/docs/CATEGORY_REFERENCE.md` — Welfare 카테고리 22종 (별개 도메인, 도메인 분리 원칙 명시)

### 프론트 docs와의 관계

- `SCREEN_SPEC.md §10-3`: 본 문서가 RadioGroup으로 오버라이드
- `STYLING_GUIDE.md §5`: Button/Input/Card/Modal/Toast 코드 템플릿. RadioGroup은 §2-2(D)의 의사 구현 참조
- `COMPONENT_ARCHITECTURE.md`: 폴더 구조에 `src/utils/profileOptions.js` 추가 권장
- `EXECUTION_PLAN.md` Phase 4: ProfilePage 작업 시 본 문서를 참고 문서로 추가

### 본 문서의 수명

- **현재**: 단일 출처 (single source of truth)
- **시연 후 §3 보완 완료**: archive 처리 (히스토리 자료로만 가치)

---

## §5. 변경 이력

| 날짜       | 변경 내용                                                                                                 |
| ---------- | --------------------------------------------------------------------------------------------------------- |
| 2026-05-15 | 초안 작성 — 문제 설명 §1, 현재 결정 §2 (옵션 A 프론트 우회), 시연 후 보완 계획 §3 (옵션 C enum 도입 권장) |
