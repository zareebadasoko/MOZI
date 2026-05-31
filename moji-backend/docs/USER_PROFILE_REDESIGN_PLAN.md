# [MOZI] UserProfile 재설계 및 마이그레이션 계획서

> 작성 시점: Phase 6 (코드 구현 완료 상태)
> 본 문서는 USER_PROFILE 도메인의 설계 결함을 바로잡기 위한 **단계별 마이그레이션 계획서**다.
> 이미 구현·테스트된 코드가 존재하므로, 한 번에 전부 바꾸지 않고 영향 범위를 단계적으로 검토하며 수정한다.
> 작업은 별도 브랜치에서 진행하고, 전체 검증 통과 후 머지한다.
>
> ⚙️ **이 문서는 작업을 수행하는 Claude Code가 직접 참조하는 기준 문서다.** 각 Step을 진행할 때
> 본 문서의 해당 절(§2 설계, §5 Step 정의, §6 검증 기준)을 근거로 삼는다.
>
> 📌 **현재 버전: v2** — Step 0 영향 조사 결과를 반영했다. 변경 내역은 §9 참조.

---

## 0. 이 문서의 목적

- Phase 6 프론트엔드 구현 중 발견된 `USER_PROFILE`의 설계 결함을 정리한다.
- 확정된 To-Be 설계를 명시한다.
- **이미 구현된 코드에 미치는 영향을 단계적으로 안전하게 반영**하기 위한 작업 순서·브랜치 전략·검증 기준을 제공한다.

---

## 1. 배경: 무엇이, 왜 잘못되었나

### 1-1. 발견된 결함

`USER_PROFILE`의 4개 컬럼이 자유 텍스트(`VARCHAR`)로 설계되었다.

| 컬럼 | 현재(As-Is) | 문제 |
|---|---|---|
| `sido` | VARCHAR 자유 입력 | 사용자 오타 가능, 표기 불일치 |
| `sigungu` | VARCHAR 자유 입력 | 시도에 종속된 선택(cascading) 불가 |
| `income_type` | VARCHAR 자유 입력 | 노인이 정확한 용어를 모름, 표기 제각각 |
| `household_type` | VARCHAR 자유 입력 | 값이 고정적인데 자유 텍스트로 방치 |

같은 종류의 "코드성 데이터"인 Welfare의 `interest_theme_code` 등은 이미 `Category` 테이블로 정규화되어 있다. **UserProfile만 같은 원칙이 적용되지 않은 일관성 누락**이 결함의 본질이다.

### 1-2. 설계 결정 (논의를 거쳐 확정)

- **옵션 C 채택**: `UserProfile`은 기본 검색(`GET /api/welfares`)에서 사용하지 않는다. 100% 챗봇 컨텍스트 전용으로 한다.
  - 기본 검색은 사용자가 직접 지정한 query param(`keyword`/`category`/`region`/`welfareType`)만 사용.
  - `applyMyProfile` 파라미터와 boolean→STATUS 자동 변환 로직을 **제거**한다.
  - 근거: 검색과 챗봇의 역할 분리, 노년층 UX 단순화(토글 제거), AND/OR 모호성·STS002 사각지대 문제 원천 제거.
- **행정구역은 `REGION` 마스터 테이블 + 시드**로 정규화한다 (`Category`와 동일 패턴).
- **`income_type` / `household_type`은 Enum**으로 정규화한다.
- **boolean 6종 → 4종**: `is_living_alone`은 `household_type`으로 흡수, `is_single_parent_grandparent`도 `household_type`으로 흡수하여 컬럼 제거.
- 행정구역 코드 체계는 **행정표준코드**(법정동코드 앞자리)를 사용한다.
- `birth_date`는 저장은 그대로, 챗봇 전송 시 **만 나이로 변환**한다.
- 건강·질병 등 신규 필드 추가는 **이번 범위에서 제외**(v2 후보). 이번 마이그레이션은 확정 스키마로 못 박고 더 흔들지 않는다.

---

## 2. 확정 설계 (To-Be)

### 2-1. USER_PROFILE 최종 스키마

```
USER_PROFILE   ─ 모든 필드 nullable (프로필 미입력 허용 원칙)
  id PK                              BIGINT AUTO_INCREMENT
  user_id FK                         UK, 1:1 with USER
  birth_date                         DATE          nullable
  gender                             ENUM(M, F, NONE)        nullable
  sido_code                          VARCHAR(2)    nullable   -- REGION 참조
  sigungu_code                       VARCHAR(5)    nullable   -- REGION 참조 (시도만 선택 허용)
  income_type                        ENUM(IncomeType)         nullable
  household_type                     ENUM(HouseholdType)      nullable
  is_disabled                        BOOLEAN       DEFAULT false
  is_multi_child                     BOOLEAN       DEFAULT false
  is_multicultural_north_defector    BOOLEAN       DEFAULT false
  is_veteran                         BOOLEAN       DEFAULT false
  created_at / updated_at            DATETIME
```

제거되는 컬럼: `sido`(VARCHAR), `sigungu`(VARCHAR), `income_type`(VARCHAR), `household_type`(VARCHAR), `is_living_alone`, `is_single_parent_grandparent`.
추가되는 컬럼: `sido_code`, `sigungu_code`.
타입 변경: `income_type`, `household_type` → Enum.

> `is_multi_child`는 노인 타겟에서 의미가 약하다는 의견이 있어 챗봇팀 협의 후 제거 가능. 이번에는 유지한다.

### 2-2. REGION 마스터 테이블 (신규)

```
REGION
  id PK                BIGINT AUTO_INCREMENT
  sido_code            VARCHAR(2)    NOT NULL   -- 예: "41"     (법정동코드 앞 2자리)
  sido_name            VARCHAR(20)   NOT NULL   -- 예: "경기도"
  sigungu_code         VARCHAR(5)    UK         -- 예: "41480"  (앞 5자리)
  sigungu_name         VARCHAR(40)              -- 예: "파주시"
```

- 각 행 = 시군구 1개. 시도 정보는 컬럼으로 비정규화(17개 값이라 무해).
- 시도 목록: `SELECT DISTINCT sido_code, sido_name`. 특정 시도의 시군구: `WHERE sido_code = ?`.
- **명칭(`sido_name`/`sigungu_name`)은 `WELFARE_LOCAL.region_name` 크롤링 표기와 일치**시킬 것("경기도"·"의정부시" 풀네임). 시드 작성 전 크롤링 데이터의 region 표기를 먼저 추출해 기준으로 삼는다.
- `USER_PROFILE`의 FK 제약은 걸지 않고 애플리케이션 레벨 검증(REGION 조회)으로 처리한다 — "시도만 선택" 케이스가 단일 FK로는 까다롭기 때문.
- 엣지 케이스: 세종특별자치시(하위 시군구 없음)는 자기 자신을 단일 행으로, 제주는 제주시·서귀포시 2행.

### 2-3. IncomeType / HouseholdType Enum (신규)

```
IncomeType {
  NATIONAL_BASIC_LIVING   "기초생활수급자"
  NEAR_POVERTY            "차상위계층"
  BASIC_PENSION           "기초연금수급자"
  GENERAL                 "해당 없음"
  UNKNOWN                 "잘 모르겠어요"
}

HouseholdType {
  LIVING_ALONE            "혼자 살아요 (독거)"
  COUPLE                  "배우자와 둘이 살아요"
  WITH_CHILDREN           "자녀와 함께 살아요"
  GRANDPARENT_GRANDCHILD  "손주를 키우고 있어요 (조손)"
  OTHER                   "그 외"
}
```

- 화면 라벨은 노인 친화 표현, enum 상수명은 위 영문 고정.
- 옵션 C 하에서 두 Enum은 검색 매핑이 없다. 챗봇 전송 시 한글 라벨로 변환해 전달한다.
- `UNKNOWN`은 PUT 옵션 C 정책(클리어 미지원) 하에서 "되돌리기" 우회로 역할도 한다.
- **소득 미상 사용자 정책**: 시드 등에서 소득 유형이 비어 있는 경우 `NULL`이 아니라 `IncomeType.UNKNOWN`으로 적재한다(챗봇 컨텍스트 일관성). Step 8 시드 작성에 적용.

### 2-4. 신규 API — GET /api/regions

cascading select(시도 → 시군구) 지원용. 엔드포인트 총 16 → 17개.

```
GET /api/regions               → 전체를 시도별로 묶어 반환
GET /api/regions?sido=41       → 해당 시도의 시군구 목록만
```

- 인증 불필요(공개). `Category` 조회와 동일 성격.

### 2-5. 챗봇 전송 데이터 변경 (CHATBOT_API_CONTRACT)

- profile boolean 6종 → **4종**. 남기는 4종은 §2-1 엔티티와 동일하다:
  `is_disabled`, `is_multi_child`, `is_multicultural_north_defector`, `is_veteran`.
  제거되는 2종은 `isLivingAlone`, `isSingleParentGrandparent` (정보는 `householdType`이 대신 담음).
- `sido`/`sigungu`/`incomeType`/`householdType`은 백엔드가 **코드/enum → 한글 라벨로 변환**해 전송 → wire format(JSON 문자열)은 기존과 거의 동일.
- `birthDate`(YYYY-MM-DD)에 더해 또는 대신 **`age`(만 나이)** 전송 권장.
- 실제 변경 대상: `domain/chat/client/dto/ChatbotUserContext.java`의 `Profile` record(필드 12 → 8: 나이·성별·sido·sigungu·incomeType·householdType + boolean 4종).

---

## 3. 영향 범위

> Step 0 전수 조사 완료 — 실제 영향: **코드 9개 / 테스트 4개+ / 문서 9개**.
> 본 §3은 초기 추정 골격이며, 조사로 드러난 보정 사항을 v2에 반영했다(§9).
> 패키지 base는 `com.mozi.backend`.

### 3-1. 신규 생성

- `domain/region/` — `Region` 엔티티, `RegionRepository`, `RegionService`, `RegionController`, `RegionDto` (5개)
- `domain/user/entity/` — `IncomeType`, `HouseholdType` enum
- 시드 리소스 — REGION 시드 데이터(JSON 또는 SQL)

### 3-2. 수정 대상 (코드) — Step 0 확정

| 영역 | 파일 | 변경 내용 |
|---|---|---|
| User 엔티티 | `UserProfile` | 컬럼 6개 교체, enum 적용, `fullFor()`·`applyUpdate()` 시그니처·본문 수정 |
| User DTO | `UserProfileResponse` | `from()`·`empty()` 매핑 변경 |
| User DTO | `UserProfileUpdateRequest` | record 필드 6개 교체 |
| User Service | `UserProfileService` | REGION 검증 호출 추가 (본문 변경량은 작음 — 엔티티 위임 구조) |
| User Controller | `UserController` | DTO 변경 반영 (직접 필드 참조 없음, 변경 미미) |
| Welfare | `WelfareController` | `applyMyProfile` 파라미터 제거 + 생성자 호출 변경 + Javadoc 정리 |
| Welfare | `WelfareSearchCondition` | record에서 `applyMyProfile` 필드 제거 |
| Welfare | `WelfareService` | UserProfile import·필드 제거, boolean→STATUS 변환 메서드 전체 제거 |
| Chat | `ChatbotUserContext` | `Profile` record 필드 12→8, `from()` 매핑 변경 |
| Chat | `ChatService` | `loadUserContext()` 매핑 호출 변경 |
| Chat | `ChatbotRequest` | Javadoc만 갱신 |
| 보안 | `SecurityConfig` | `applyMyProfile` 관련 주석 제거 (사소) |
| 시드 | `UserSeedLoader` | `SEEDS`·`UserSeed` record·`fullFor()` 호출 신규 스키마로 재작성 |

- **무변경 확인**: `UserProfileRepository`(`findByUser_Id` 한 메서드뿐), `MockChatbotClient`(message만 사용).
- 추정에 있던 `UserProfileController` 별도 클래스는 **존재하지 않음** — `UserController`로 통합.

### 3-3. 수정 대상 (테스트) — Step 0 확정

- `UserProfileServiceTest` — `fullFor()` 호출 + assertion 6개 케이스
- `UserControllerIntegrationTest` — HTTP body·jsonPath assertion 8개 케이스
- `WelfareServiceTest` — `applyMyProfile` 관련 테스트 메서드 **3개 통째 삭제** + 클래스 Javadoc 갱신
- `ChatServiceTest` — 프로필 매핑 테스트 이름·assertion 갱신
- 신규: `RegionService`/`RegionController` 테스트
- **무변경/미미**: `ChatControllerIntegrationTest`(UserProfile 미의존), `WelfareControllerIntegrationTest`(기본값 동작이라 영향 적음 — 명시 호출 있으면 제거)

### 3-4. 수정 대상 (문서) — 9종

`ERD_REQUIREMENTS.md`, `API_SPEC.md`, `API_SPEC_DRAFT.md`, `CHATBOT_API_CONTRACT.md`, `CATEGORY_REFERENCE.md`, `PRD.md`(4-2, 4-4), `USER_FLOW.md`(3️⃣), `EXECUTION_PLAN.md`(진행 현황·Phase 4 기술), `FRONTEND_INTEGRATION_GUIDE.md`(§3-2 `applyMyProfile` 기술·프로필 필드 표).

> **`CATEGORY_REFERENCE.md` 처리 방침**: §5 부근의 boolean→STATUS 매핑 표(L147~153 부근)를 단순 삭제하지 말고
> "참고용 — 옵션 C 적용으로 검색 자동 매핑은 제거됨. 챗봇/사용자 명시 호출 시에만 활용" 으로 표기를 바꾼다.

### 3-5. DB 마이그레이션 주의 ⚠️ (Step 0 조사 반영 — 시점 변경)

`ddl-auto: update`는 다음을 **수행하지 못한다**:
- 컬럼 삭제·이름 변경 (`sido` → `sido_code` 시 옛 컬럼 잔존)
- **컬럼 타입 변경 — `VARCHAR` → `ENUM` 포함** (`income_type`/`household_type`이 해당)

따라서 Step 3에서 `UserProfile` 엔티티를 변경하는 순간 스키마가 어긋난다.
→ **개발 DB(`mozi_dev`) 재생성은 Step 8이 아니라 Step 3 시작 직전에 수행한다.**
방법: ① 시드를 끈 채 `ddl-auto: create`로 1회 부팅 후 `update`로 복귀, 또는 ② `mozi_dev` 스키마 DROP 후 재생성. 작업 전 백업 불필요(시드로 재현).
Step 1·2는 순수 추가(테이블 신규 생성)라 `update`가 정상 처리하므로 재생성 불필요.

---

## 4. 작업 원칙

### 4-1. 브랜치 전략

```
main (또는 develop)
  └── refactor/userprofile-region-redesign   ← 본 작업 전용 브랜치
        Step 1 커밋 → Step 2 커밋 → ... → Step 11 커밋
        전체 검증 통과 후 → main 으로 머지 (PR)
```

- 브랜치 하나에서 Step 단위로 커밋한다. 커밋 메시지에 Step 번호를 남긴다 (예: `refactor(step3): UserProfile 엔티티 컬럼 재설계`).
- 머지는 **모든 Step 완료 + 전체 테스트 통과 후 한 번에**. 중간 단계를 main에 머지하지 않는다.

### 4-2. 단계 진행 원칙

- **추가형 단계(Step 1, 2)**: 기존 코드를 건드리지 않는 순수 추가. 각 단계 끝에서 빌드·테스트가 그린 상태여야 한다.
- **변경형 단계(Step 3~9)**: 엔티티 변경이 컴파일 에러를 연쇄시킨다. 변경형 단계는 **작업 단위가 끝나는 시점에 컴파일·테스트 그린**을 기준으로 하며, 단위 내부에서 일시적으로 깨지는 것은 허용한다.
- **한 번에 한 Step만 진행한다.** 한 Step이 끝나면 빌드/테스트로 검증한 뒤 다음 Step으로 넘어간다.
- **각 Step은 "영향 조사 → 수정 계획 제시 → 승인 → 구현" 순서로 진행한다.** 조사 없이 곧바로 코드를 수정하지 않는다.
- 본 문서 §5에 명시된 "해당 Step의 검증 기준"까지만 책임진다. 계획서 범위를 벗어나는 변경은 하지 않으며, 불확실하면 추측하지 말고 먼저 질문한다.

---

## 5. 단계별 실행 계획

각 Step의 형식: **목표 / 영향 / 검증(✅)**.

### Step 0 — 브랜치 생성 + 영향 범위 전수 조사 ✅ 완료

- **목표**: 작업 브랜치를 만들고, §3의 추정 영향 범위를 실제 코드베이스와 대조해 확정한다. 코드는 수정하지 않는다.
- **결과**: 코드 9 / 테스트 4+ / 문서 9로 확정. 보정 사항은 v2(§9)에 반영됨.

### Step 1 — REGION 도메인 신규 (순수 추가)

- **목표**: `Region` 엔티티 + Repository + Service + Controller + DTO 생성. `GET /api/regions` 동작. 단 시드 데이터 적재는 Step 8에서.
- **영향**: 신규 파일만. 기존 코드 무변경.
- **✅ 검증**: 빌드 통과. 기존 테스트 전부 통과(영향 없음 확인). REGION 테이블 생성됨.

### Step 2 — IncomeType / HouseholdType Enum 신규 (순수 추가)

- **목표**: 두 Enum을 생성한다(상수명 + 한글 라벨). 아직 어떤 엔티티에도 연결하지 않는다.
- **영향**: 신규 파일만.
- **✅ 검증**: 빌드 통과. 기존 테스트 전부 통과.

### Step 3 — UserProfile 엔티티 변경 (+ DB 재생성)

- **선행 작업**: 이 Step 시작 **직전에 `mozi_dev` 스키마를 재생성**한다(§3-5). `ddl-auto`가 VARCHAR→ENUM 변환·컬럼 교체를 처리하지 못하므로 필수.
- **목표**: §2-1 최종 스키마로 `UserProfile` 엔티티 변경. 컬럼 6개 교체, Enum 타입 적용, `fullFor()`·`applyUpdate()` 시그니처·본문 동시 수정. `UserProfileRepository`는 변경 불필요(Step 0 확인).
- **영향**: 여기서부터 DTO/Service/Controller/`UserSeedLoader`에 컴파일 에러 연쇄 시작.
- **✅ 검증**: Step 4와 연속 진행. Step 4 종료 시점에 그린 확인.

### Step 4 — UserProfile DTO / Service / Controller + 시드 골격 변경

- **목표**: 엔티티 변경에 맞춰 `UserProfileResponse`(`from()`·`empty()` 포함), `UserProfileUpdateRequest`, `UserProfileService`(REGION 검증 포함), `UserController`를 수정한다. PUT 옵션 C 정책은 유지.
  추가로 **`UserSeedLoader`의 `fullFor()` 호출 골격을 컴파일이 통과하도록 임시 수정**한다 — 시드 값의 실제 재작성은 Step 8이지만, Step 3에서 시그니처가 바뀐 `fullFor()` 때문에 빌드가 깨지므로 골격은 여기서 맞춘다.
- **영향**: User 도메인 전반 + `UserSeedLoader`(골격).
- **✅ 검증**: 빌드 통과. User 도메인 테스트는 Step 9에서 손보므로 일시적 실패 허용. **컴파일은 통과**해야 함.

### Step 5 — GET /api/regions 엔드포인트 마무리

- **목표**: Step 1에서 만든 Region API를 UserProfile 흐름과 연결 점검(프로필 저장 시 region 코드 검증 경로 확인).
- **영향**: Region/User 도메인 경계.
- **✅ 검증**: `GET /api/regions` 정상 응답(시드 전이라 빈 배열 가능). 빌드 통과.

### Step 6 — Welfare 검색에서 applyMyProfile 제거

- **목표**: `WelfareController`/`WelfareSearchCondition`/`WelfareService`에서 `applyMyProfile` 파라미터, boolean→STATUS 변환, UserProfile 조회 로직을 **제거**한다. `SecurityConfig`의 관련 주석도 함께 제거.
- **영향**: Welfare 검색 도메인. `keyword`/`category`/`region`/`welfareType` 검색은 그대로 유지.
- **✅ 검증**: 빌드 통과. Welfare 검색 테스트는 Step 9에서 정리.

### Step 7 — 챗봇 연동 프로필 매핑 변경

- **목표**: `ChatbotUserContext`의 `Profile` record(12→8 필드)와 `from()` 매핑, `ChatService.loadUserContext()`를 §2-5에 맞춰 수정한다. boolean 4종, 코드/enum → 한글 라벨 변환, `birth_date` → `age` 변환. `ChatbotRequest`는 Javadoc만.
- **영향**: Chat 도메인. `MockChatbotClient`는 무변경(Step 0 확인).
- **✅ 검증**: 빌드 통과. `POST /api/chat` 수동 호출 시 정상 응답.

### Step 8 — 시드 데이터 (REGION + 가짜 사용자 7명)

- **목표**: REGION 마스터 시드 적재(행정표준코드 기반, 명칭은 크롤링 표기에 정렬). `UserSeedLoader`의 `SEEDS`·`UserSeed` record를 신규 스키마 값으로 재작성. USER_FLOW 시연 시나리오 A/B/C가 동작하도록 값 선정.
- **소득 미상 사용자 정책**: 기존 `incomeType`이 비어 있던 시드 사용자(예: senior07)는 `NULL`이 아니라 **`IncomeType.UNKNOWN`으로 적재**한다(§2-3).
- **영향**: 시드 컴포넌트(`UserSeedLoader`). DB 스키마는 Step 3에서 이미 재생성되어 있음.
- **✅ 검증**: 앱 부팅 시 REGION·가짜 사용자 정상 적재. `GET /api/regions` 실데이터 응답.

### Step 9 — 테스트 전면 수정

- **목표**: §3-3의 깨진 테스트를 모두 수정한다. `WelfareServiceTest`의 `applyMyProfile` 테스트 3개는 삭제. Region 도메인 테스트 신규 작성. User/Chat 테스트는 신규 스키마 기준으로 갱신.
- **영향**: 테스트 코드 전반.
- **✅ 검증**: `./gradlew test` **전체 통과**. Service 레이어 커버리지 70% 유지.

### Step 10 — 문서 동기화

- **목표**: §3-4의 문서 9종을 신규 설계에 맞춰 갱신한다. `CATEGORY_REFERENCE.md` 매핑 표는 §3-4 방침대로 "참고용" 표기로 변경. `EXECUTION_PLAN`에는 본 리팩토링을 이력으로 추가.
- **영향**: 문서만.
- **✅ 검증**: 문서 간 모순 없음. Swagger UI(`@Operation` 등) 갱신 반영.

### Step 11 — 통합 검증 + 머지

- **목표**: 전체 흐름 점검 후 `main`에 머지.
- **✅ 검증**: §6 최종 검증 체크리스트 전 항목 통과 → PR 머지.

---

## 6. 최종 검증 체크리스트 (Step 11)

- [ ] `./gradlew build` 성공
- [ ] `./gradlew test` 전체 통과, Service 레이어 커버리지 70% 이상
- [ ] `mozi_dev` 재생성 후 앱 부팅 시 USER_PROFILE·REGION 테이블 정상 생성
- [ ] REGION 시드 적재 확인, `GET /api/regions` 및 `?sido=` 정상 응답
- [ ] 가짜 사용자 7명 프로필이 신규 스키마로 적재됨 (소득 미상은 `UNKNOWN`)
- [ ] `PUT /api/users/me/profile`에서 enum·region 코드 정상 저장, 잘못된 값은 `VALIDATION_FAILED`
- [ ] `GET /api/welfares`에 `applyMyProfile` 파라미터가 더 이상 없음, keyword/category/region 검색 정상
- [ ] `POST /api/chat` 호출 시 챗봇에 전달되는 profile이 boolean 4종 + 라벨 변환 + age 형태
- [ ] USER_FLOW 시연 시나리오 A·B·C가 챗봇 흐름에서 동작
- [ ] 문서 9종 갱신 완료, 상호 모순 없음, Swagger UI 반영
- [ ] 모든 Step 커밋 완료 → `main` 머지 PR 생성

---

## 7. 롤백 정책

- 작업은 전용 브랜치에 격리되어 있으므로, 문제 발생 시 `main`은 영향받지 않는다.
- 특정 Step에서 막히면 해당 Step 커밋만 되돌리고(`git revert`/`reset`) 재시도한다.
- 머지 전 단계에서는 브랜치를 폐기하고 다시 시작하는 것도 안전한 선택지다.
- DB는 시드로 재현 가능하므로 별도 백업 불필요.

---

## 8. 부록: 변경 전후 한눈에 보기

| 항목 | Before (As-Is) | After (To-Be) |
|---|---|---|
| 거주지 | `sido`/`sigungu` 자유 텍스트 | `sido_code`/`sigungu_code` + REGION 마스터 |
| 소득유형 | `income_type` 자유 텍스트 | `IncomeType` Enum 5종 |
| 가구형태 | `household_type` 자유 텍스트 | `HouseholdType` Enum 5종 |
| boolean | 6종 (독거·장애·조손·다자녀·다문화·보훈) | 4종 (장애·다자녀·다문화·보훈) |
| 검색에서 프로필 사용 | `applyMyProfile`로 boolean 5종 자동 반영 | 사용 안 함 (검색은 query param만) |
| 프로필 용도 | 검색 필터링 + 챗봇 | 챗봇 컨텍스트 전용 |
| 신규 테이블 | - | `REGION` |
| 신규 API | - | `GET /api/regions` |
| 엔드포인트 수 | 16 | 17 |

---

## 9. 변경 이력

| 버전 | 변경 내용 |
|---|---|
| v1 | 최초 작성 (Phase 6). 옵션 C·REGION·Enum·boolean 6→4 확정. |
| v2 | **Step 0 영향 조사 결과 반영.** ① DB 재생성 시점을 Step 8 → **Step 3 직전**으로 이동, `VARCHAR→ENUM` 변환 불가 명시(§3-5, Step 3). ② 챗봇 전송 boolean 4종을 명시(§2-5). ③ `UserSeedLoader` 골격 수정을 Step 4 범위에 포함(Step 4). ④ `CATEGORY_REFERENCE.md` 매핑 표 "참고용" 처리 방침 명시, 문서 목록 8→9종 보정(§3-4). ⑤ 소득 미상 시드 사용자(senior07 등)를 `IncomeType.UNKNOWN`으로 적재 확정(§2-3, Step 8). ⑥ §3-2/§3-3을 Step 0 실제 조사 결과로 갱신. |
| **v3 (2026-05-17)** | **Step 1~10 ✅ 전부 완료.** Step 1 REGION 도메인 신규(229행 시드 적재), Step 2 IncomeType/HouseholdType Enum 5종씩, Step 3+4 UserProfile 스키마 재설계 + REGION 검증, Step 5 검증 테스트 보강(단위 4 + 통합 2), Step 6 applyMyProfile 폐기 + UserProfile 의존성 제거, Step 7 챗봇 페이로드 본격 매핑(라벨/age), Step 8 RegionSeedAdapter/Loader + 229행 적재, Step 9 정상 케이스 테스트 보강 5건, **Step 10 docs 9종 갱신 + FRONTEND_MIGRATION_NOTES + CHATBOT_MIGRATION_NOTES 신규 작성**. |
