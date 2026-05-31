# Category Reference (시드 데이터용)

> Phase 2-3 / Phase 4 시드 적재 시 이 문서 기반으로 Category 테이블 초기화.
> 출처별 컬럼명 차이 통일 매핑 정보 포함.

---

## 1. THEME (관심 주제) — 15종

크롤링 데이터의 `interest_theme_code` 필드에 매핑되는 코드 목록.

| code | name |
|---|---|
| THM001 | 서민금융 |
| THM002 | 안전·위기 |
| THM003 | 신체건강 |
| THM004 | 임신·출산 |
| THM005 | 일자리 |
| THM006 | 주거 |
| THM007 | 교육 |
| THM008 | 에너지 |
| THM009 | 생활지원 |
| THM010 | 문화·여가 |
| THM011 | 정신건강 |
| THM012 | 보육 |
| THM013 | 입양·위탁 |
| THM014 | 보호·돌봄 |
| THM015 | 법률 |

---

## 2. STATUS (가구 상황) — 7종

크롤링 데이터의 `household_status_code` 필드에 매핑되는 코드 목록.

| code | name | 비고 |
|---|---|---|
| STS001 | 보훈대상자 | |
| STS002 | 저소득 | |
| STS003 | 장애인 | |
| STS004 | 한부모·조손 | |
| STS005 | 다자녀 | |
| STS006 | 다문화·탈북민 | |
| STS007 | 퇴직자 | 신중년사회공헌사업, 중장년 가치동행 일자리 등 특수 사업 한정 |

> ⚠️ **주의**: STS007 "퇴직자"는 일반 코드 6종(STS001~STS006)과 달리 특정 사업(중장년 일자리)에서만 사용되는 특수 케이스. 시드 적재 시 출처 데이터에 따라 다르게 매핑될 수 있음.

---

## 3. 출처별 컬럼명 매핑

> ✅ **MVP 결정**: 서울 데이터의 컬럼명을 시드 단계에서 정규화했음 (2026-05-08).
> 시드 적재 시점에는 모든 출처가 동일 컬럼명 사용 (분기 불필요).

| 의미 | 표준 컬럼명 | 변경 이력 |
|---|---|---|
| 관심 주제 | `interest_theme_code` | 서울 원본은 `INTRS_THEMA_CD`였으나 18건 수동 정규화 |
| 가구 상황 | `household_status_code` | 서울 원본은 `FMLY_CIRC_CD`였으나 18건 수동 정규화 |

원본 보존: `seed-data/welfare-crawled/full/seoul-original.json` 

---

## 4. 데이터 형태 — 콤마(,) 구분 문자열

각 컬럼은 **콤마로 구분된 문자열**로 원본 JSON에 저장됨. 한 복지에 여러 코드가 매핑될 수 있음 (N:M 관계의 근거).

### 예시 1: 복지로 중앙부처 데이터
```json
{
  "ID": "WLF00005442",
  "title": "긴급돌봄 지원사업",
  "interest_theme_code": "보호·돌봄,안전·위기",
  "household_status_code": ""
}
```
→ 매핑: WelfareCategory에 (WLF00005442, THM014), (WLF00005442, THM002) 두 행 추가
→ household_status_code가 빈 문자열인 경우 STATUS 매핑 없음

### 예시 2: 서울복지포털 데이터
```json
{
  "ID": "SEL00000001",
  "title": "고령장애인 활동지원 사업",
  "interest_theme_code": "보호·돌봄,생활지원",
  "household_status_code": "장애인"
}
```
→ 매핑: WelfareCategory에 3개 행 추가
- (SEL00000001, THM014) — 보호·돌봄
- (SEL00000001, THM009) — 생활지원
- (SEL00000001, STS003) — 장애인

### 예시 3: 단일 코드
```json
{
  "interest_theme_code": "서민금융",
  "household_status_code": "보훈대상자"
}
```
→ 매핑: 각각 1개 행씩

---

## 5. Phase 4 시드 어댑터 처리 정책

### 5-1. 매핑 알고리즘

```
1. JSON 원본의 콤마 분리 문자열을 읽어 split(",")로 배열화
2. 각 항목을 trim() 처리 (앞뒤 공백 제거)
3. 빈 문자열은 skip
4. Category 테이블에서 name 기준으로 lookup → category_id 획득
5. WelfareCategory 매핑 테이블에 (welfare_id, category_id) 행 추가
6. 매칭 안 되는 코드는 WARN 로그 남기고 skip
   - 예: 오타 ("보호.돌봄" → 매칭 실패)
   - 예: 신규 코드 (Category 테이블에 없음)
```

### 5-2. 출처별 분기 → 불필요

서울 데이터의 컬럼명을 정규화했으므로 출처별 분기 없이 단일 로직으로 처리:

```java
String themeRawValue = json.get("interest_theme_code");
String statusRawValue = json.get("household_status_code");
```

### 5-3. Category 테이블 초기화 순서

Phase 2-3 또는 Phase 4 진입 시 다음 순서:

```
1. Category 테이블에 THEME 15종 + STATUS 7종 = 총 22행 시드 적재 (정적)
2. Welfare 4종 시드 적재 (4개 JSON 파일 → 자식 엔티티별 적재)
3. WelfareCategory 매핑 적재 (Welfare 적재 시 동시에 처리)
```

---

## 6. UserProfile 도메인과의 관계 (참고)

> ⚠️ **2026-05-17 USER_PROFILE_REDESIGN Step 3·6 이후**: UserProfile boolean은 **4종**으로 축소되었고,
> 검색에서 boolean→STATUS 자동 매핑(`applyMyProfile`) 정책은 **폐기**되었다. 본 표는 **참고용**으로만 유지 — 자동 매핑 동작 없음.

UserProfile 엔티티의 boolean 플래그 4종(Step 3 재설계)은 **STATUS 카테고리와 의미적으로 동일**하지만, **별도로 관리**한다.

| UserProfile 필드 | 의미적으로 매칭되는 STATUS 코드 |
|---|---|
| `isVeteran` | STS001 보훈대상자 |
| (소득 관련 — `incomeType` enum) | STS002 저소득 |
| `isDisabled` | STS003 장애인 |
| (`householdType == GRANDPARENT_GRANDCHILD`) | STS004 한부모·조손 (Step 3에서 boolean 흡수) |
| `isMultiChild` | STS005 다자녀 |
| `isMulticulturalNorthDefector` | STS006 다문화·탈북민 |
| (없음) | STS007 퇴직자 |

> 💡 **이유**: UserProfile의 boolean은 사용자의 "내 상태"를 표현하기 위한 것이고, Category의 STATUS는 복지에 매핑되는 카테고리 분류용. 같은 의미라도 도메인이 달라 각자 관리.

> ❌ **자동 매핑 폐기 이력 (Step 6)**: Phase 4 시점에는 `applyMyProfile=true`로 보낸 사용자 프로필의 boolean을 자동 STATUS 코드로 변환해 검색 조건에 포함시켰으나, USER_PROFILE_REDESIGN 옵션 C 채택으로 본 정책이 폐기되었다. 검색에서는 사용자가 명시한 query param(`category=STS00X`)만 사용한다. 본인 프로필 기반 추천은 **챗봇 흐름**(`POST /api/chat`)이 전담.

---

## 7. 향후 확장 (v2 후보)

- **신규 코드 발견 시**: Category 테이블에 행 추가 + 시드 어댑터 재실행
- **퇴직자 등 특수 케이스 추가**: 비고란에 명시
- **동의어 처리**: "보호 돌봄" vs "보호·돌봄" 같은 가운뎃점 변형 처리 (현재는 정확 일치만)
