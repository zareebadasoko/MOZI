# MOZI Backend Project

## 1. 프로젝트 개요

디지털 조작에 익숙하지 않은 노년층(65세 이상)이 대화형 챗봇과 상황별 필터링을 통해 맞춤형 복지 혜택을 쉽게 찾고 북마크할 수 있도록 지원하는 **백엔드 API 서비스**.
프론트엔드 화면에 종속되지 않는 **API-First 설계**를 지향한다.

### 프로젝트 컨텍스트
- **개발 규모**: 1인 개발 (백엔드 우선 → 이후 프론트엔드)
- **목적**: 졸업 프로젝트 + 1인 풀스택 서비스 완성 경험
- **배포 방식**: 최종 발표 시연용 임시 배포 (AWS, 실제 사용자 대상 운영 X)
- **데이터 출처**:
  - 복지로 사이트 크롤링 → 중앙부처 / 지자체 / 민간 복지 정보
  - 서울복지포털 사이트 크롤링 → 서울시 복지 정보
  - **두 출처는 서로 다른 사이트이며 데이터 스키마도 다름** (자세한 내용 ERD_REQUIREMENTS.md 참고)
- **유저 데이터**: 실제 유저 없음. 시연용 가짜 사용자(Seed Data) 사용

### 외부 시스템 연동
- **챗봇 서버**: 다른 팀원이 LLM + RAG로 개발 중 (별도 서버)
  - 통신: REST API
  - 우리 백엔드 역할: 사용자 메시지 + 프로필 정보를 챗봇 서버로 전달 → 응답을 프론트로 반환하는 **브릿지(Bridge)**
  - 챗봇 서버 스펙은 **개발 중**이므로, 우선 **Mock 서버**로 개발 후 실연동

---

## 2. 자주 쓰는 명령어 (Gradle 기반)

| 목적 | 명령어 |
|---|---|
| 개발 서버 실행 | `./gradlew bootRun` |
| 전체 테스트 실행 | `./gradlew test` |
| 특정 테스트만 실행 | `./gradlew test --tests "패키지.클래스명"` |
| 프로젝트 빌드 | `./gradlew build` |
| 빌드 청소 | `./gradlew clean` |
| 의존성 트리 확인 | `./gradlew dependencies` |

---

## 3. 폴더 구조 (Domain-Driven Design)

```
src/main/java/com/mozi/backend/
├── domain/                    # 핵심 비즈니스 로직 (도메인별 분리)
│   ├── user/                  # 사용자 인증, 프로필
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── dto/
│   ├── welfare/               # 복지 정보 (4개 출처별 자식 엔티티)
│   ├── bookmark/              # 북마크
│   ├── chat/                  # 챗봇 브릿지
│   └── category/              # 카테고리/태그
└── global/                    # 전역 설정
    ├── config/                # SecurityConfig, WebConfig 등
    ├── security/              # JWT, 인증 필터
    ├── exception/             # GlobalExceptionHandler, BusinessException
    ├── common/                # ApiResponse, BaseEntity
    └── util/

src/main/resources/
├── application.yml            # 공통 설정 (비밀 정보 X)
├── application-local.yml      # 로컬 개발용 (gitignore)
└── application-mock.yml       # 챗봇 Mock 모드 활성화 시

src/test/java/com/mozi/backend/  # 테스트 (운영 코드와 동일 패키지 구조)

seed-data/
└── welfare-crawled/
    ├── full/                  #데이터 원본 
    │   ├── central.json  #크기 :257KB
    │   ├── local.json    #크기 :1.9MB  
    │   ├── private.json  #크기 :51KB
    │   └── seoul.json    #크기 :23KB
    └── samples/               #원본 데이터 중 10건
        ├── central-sample.json      
        ├── local-sample.json
        ├── private-sample.json
        └── seoul-sample.json

docs/
├── PRD.md                     # 제품 명세
├── USER_FLOW.md               # 사용자 흐름
├── ERD_REQUIREMENTS.md        # DB 설계 요구사항 (As-Is, To-Be)
├── EXECUTION_PLAN.md          # 단계별 실행 계획
├── API_SPEC_DRAFT.md          # API 명세 초안 (Phase 0에서 작성)
├── ERROR_CODES.md             # 에러 코드 정의 (Phase 6에서 작성)
└── API_SPEC.md                # 최종 API 명세서 (Phase 6에서 자동 생성)
```

---

## 4. 코딩 규칙 (Coding Conventions)

### 4-1. API 응답 규격
- 모든 REST API는 통일된 JSON 포맷으로 반환
- 성공/실패 모두 `ApiResponse<T>` 래퍼 사용
  ```json
  { "status": "SUCCESS", "message": "...", "data": { ... } }
  { "status": "ERROR", "message": "...", "errorCode": "USER_NOT_FOUND", "timestamp": "ISO-8601" }
  ```
- Validation 실패 시 `errorCode: "VALIDATION_FAILED"` + `fields` 객체로 필드별 에러 분리 반환 (자세한 형식은 `docs/API_SPEC_DRAFT.md` §1-3 참조)

### 4-2. 예외 처리
- 개별 Controller에서 `try-catch` 남발 금지
- `@RestControllerAdvice`로 글로벌 예외 처리
- 비즈니스 예외는 `BusinessException` 상속 후 도메인별 커스텀 예외 정의

### 4-3. JPA 규칙
- 엔티티 생성/수정 시간은 **JPA Auditing**(`@CreatedDate`, `@LastModifiedDate`)로 자동화
- 양방향 연관관계 매핑 시 **무한 루프(순환 참조) 주의** — `toString()`, JSON 직렬화에서 한쪽을 제외
- **Entity는 API 응답에 직접 노출 금지** — 항상 DTO로 변환
- **Setter 사용 자제** — 빌더 패턴 또는 정적 팩토리 메서드 권장
- **N+1 쿼리 주의** — 연관관계 조회 시 `fetch join` 또는 `@EntityGraph` 사용

### 4-4. Welfare 도메인 핵심 규칙 ⭐
> ⚠️ **이 부분은 ERD 설계의 핵심이므로 반드시 준수**

- **상속 매핑 전략**: `JOINED` 권장 (`WelfareCommon` 부모 + 4개 자식)
- **자식 엔티티 4종**: `WelfareCentral`, `WelfareLocal`, `WelfarePrivate`, `WelfareSeoul`
- **서울복지포털 데이터(`WelfareSeoul`)는 별도 자식으로 유지**
  - 출처가 다른 사이트이고 데이터 스키마가 다르기 때문
  - `WelfareLocal`에 통합 X
- 출처 구분: 부모 엔티티의 `welfare_type` 컬럼 사용
- 통합 조회 시: `WelfareCommon` 기준으로 조회 후, 상세 조회 시 자식 fetch join

### 4-5. 작업 순서 (반드시 지킬 것)
1. PRD/USER_FLOW/ERD_REQUIREMENTS 문서를 먼저 읽고 맥락 확보
2. **코드 작성 전 아키텍처와 ERD 설계를 먼저 제안하고 사용자 승인을 받는다**
3. 승인된 설계대로 단계별로 구현
4. 각 단계마다 검증(테스트 또는 수동 확인) 후 다음 단계 진행

### 4-6. 주석 정책 (학습 목적 강화) ⭐
> 본 프로젝트는 1인 학습 프로젝트. 작성자(사용자)가 코드를 직접 이해할 수 있도록 주석을 적극 작성한다.

#### 1. 클래스/메서드 위에는 JavaDoc 필수
모든 `public` 클래스, `public` 메서드, **핵심 private 메서드**(비즈니스 로직 포함) 위에 JavaDoc 작성.

```java
/**
 * (한 줄 요약: 무엇을 하는 메서드/클래스인가)
 *
 * (2~3줄 상세 설명: 왜 필요한지, 어떤 비즈니스 규칙이 적용되는지)
 *
 * @param 파라미터명 의미와 제약
 * @return 반환값 의미
 * @throws 발생 가능한 예외와 발생 조건
 */
```

빠뜨리면 안 되는 항목:
- 메서드의 목적
- 입력값 (각 파라미터의 의미와 제약)
- 출력값 (반환값의 의미)
- 발생 가능한 예외 종류와 조건

#### 2. 메서드 내부는 핵심만 한 줄 주석
모든 줄에 주석 X. 다음 경우에만 `//` 주석 작성:
- 비즈니스 규칙이 적용되는 부분
- 왜 이렇게 했는지 직관적이지 않은 코드
- 외부 시스템과 통신하는 지점
- 보안/검증 로직

```java
// 비밀번호 평문 저장 방지: BCrypt로 해싱
String hashed = passwordEncoder.encode(password);
```

#### 3. 하지 말 것
- ❌ 자명한 코드에 주석 (`// userId를 받는다` 같은 거)
- ❌ 변수명만으로 알 수 있는 내용 반복
- ❌ 한국어/영어 혼용 → 주석은 **한국어로 통일**

#### 4. 클래스 말미 요약
각 클래스 끝(닫는 `}` 직후)에 1~2문장 단일 주석 블록 추가:
- "이 클래스의 역할은 X이고, 이런 흐름으로 동작한다"
- 처음 읽을 때 막힐 만한 부분에 대한 보조 설명

---

## 5. 절대 하지 말 것 (Out of Scope & Restrictions)

### 5-1. 아키텍처 위반 금지
- ❌ **UI 렌더링 금지**: Thymeleaf, JSP 등 서버 사이드 렌더링 사용 금지. 오직 **JSON API**만 서빙
- ❌ **로컬 파일 저장 금지**: 이미지 등은 클라우드 스토리지(S3) 전제로 설계
- ❌ **출처가 다른 데이터를 한 테이블에 통합 금지**: 특히 `WelfareSeoul`을 `WelfareLocal`에 병합하는 것은 절대 금지 (데이터 스키마가 다름. 자세한 내용은 `docs/ERD_REQUIREMENTS.md` 섹션 3-2 참고)

### 5-2. 과잉 개발 금지
- ❌ 지시받지 않은 기능을 선제적으로 개발하지 않는다
  - 예: 자체 회원 간 커뮤니티, 댓글/리뷰, 복잡한 관리자 대시보드, 푸시 알림 등
- ❌ 챗봇 자체 구현 금지 (외부 챗봇 서버와 통신만 담당)

### 5-3. 의존성 관리
- ❌ `build.gradle`에 새 라이브러리 추가 시 **반드시 사전에 이유 명시 + 사용자 허락**
- ❌ 비밀 정보(DB 비밀번호, JWT Secret, API Key)를 코드/yml에 하드코딩 금지

### 5-4. 데이터 보호
- ❌ 가짜 유저 데이터지만 비밀번호는 반드시 BCrypt 해싱 후 저장
- ❌ 운영 코드에 `System.out.println` 또는 무분별한 로그로 사용자 데이터 출력 금지

### 5-5. 시드 데이터 보호
- ❌ `seed-data/welfare-crawled/` 원본 크롤링 데이터 수정/삭제 금지 (재크롤링 비용 큼)
- ❌ `.git/` 직접 수정 금지

### 5-6. 비밀번호, API KEY 등 보안
- ❌ CLAUDE.md, ./docs 등 깃 추적 및 깃허브에 올라가는 문서에 비밀번호, API KEY, 토큰 등 민감 정보 게시 금지
- ❌ 운영/개발 문서에 보안 관련 민감 정보 하드코딩 금지 


---

## 6. 테스트 규칙

- **Service 레이어 핵심 로직**: 단위 테스트(Mockito) 필수
- **Controller**: 통합 테스트(`@SpringBootTest` 또는 `@WebMvcTest`) 작성
- **테스트 코드 위치**: `src/test/java/` 하위에 운영 코드와 동일 패키지 구조
- **테스트 명명**: `메서드명_상황_기대결과`
  - 예: `findWelfare_노인복지조회_정상반환()`
  - 예: `signup_중복이메일_예외발생()`
- **새 기능 작성 후**: 반드시 `./gradlew test` 통과 확인 후 커밋
- **MVP 단계 커버리지 목표**: Service 레이어 70% 이상

---

## 7. Git 규칙

- **커밋 메시지**: Conventional Commits 형식
  - `feat:` 새 기능
  - `fix:` 버그 수정
  - `refactor:` 리팩토링
  - `docs:` 문서 수정
  - `test:` 테스트 추가/수정
  - `chore:` 설정/빌드 등 부수 작업
- 예시: `feat(welfare): 카테고리별 복지 조회 API 구현`
- **브랜치**: `main`에 직접 푸시 금지. 작업은 `feature/*` 브랜치에서
- Claude Code가 커밋 생성 시 메시지를 사용자가 검토 후 승인

---

## 8. 환경 변수 및 비밀 관리

- DB 접속 정보, JWT Secret, 외부 API URL/Key는 `application.yml`에 직접 작성 금지
- 다음 중 하나로 주입:
  1. `application-local.yml` (개발자 로컬용, **반드시 gitignore**)
  2. 환경 변수 (`${ENV_VAR}` 형태로 참조)
- `.gitignore`에 등록할 파일:
  - `application-local.yml`
  - `application-prod.yml`
  - `.env`
- 시연용 시드 데이터에 포함된 **가짜 사용자 비밀번호도 평문 저장 금지** (BCrypt 해싱)

---

## 9. AWS 배포 관련 (참고)

- 실제 배포는 **백엔드/프론트 완성 이후**에 학습 + 진행 예정
- 현 단계에서는 배포 코드를 작성하지 않는다 (Dockerfile, GitHub Actions, ECS 설정 등)
- 단, **배포 가능한 형태로 설계**는 유지: 환경 변수 분리, 클라우드 스토리지 전제 등

---

## 10. 참고 문서

| 문서 | 경로 | 용도 |
|---|---|---|
| 제품 명세 (PRD) | `./docs/PRD.md` | 무엇을 만들 것인가 |
| 사용자 흐름 | `./docs/USER_FLOW.md` | 어떤 시나리오를 다룰 것인가 |
| DB 설계 요구사항 | `./docs/ERD_REQUIREMENTS.md` | 어떻게 데이터를 설계할 것인가 |
| 실행 계획 | `./docs/EXECUTION_PLAN.md` | 어떤 순서로 만들 것인가 |
| API 명세 초안 | `./docs/API_SPEC_DRAFT.md` | Phase 0에서 작성 |
| 에러 코드 정의 | `./docs/ERROR_CODES.md` | Phase 6에서 작성 |
| 최종 API 명세서 | `./docs/API_SPEC.md` | Phase 6에서 자동 생성 |

---

## 11. 시드 데이터 위치
- 원본 데이터: `seed-data/welfare-crawled/full`
  - `central.json` — 복지로 중앙부처
  - `local.json` — 복지로 지자체
  - `private.json` — 복지로 민간
  - `seoul.json` — 서울복지포털 (**스키마가 위 3개와 일부 다름**)
- 샘플 데이터: `seed-data/welfare-crawled/samples/` (원본 데이터 내용 중 10건)
- 코드 작성 시: 샘플로 스키마 파악 → 적재 코드는 원본 폴더
- 시드 적재 스크립트: Phase 2에서 `src/main/resources/db/seed/` 또는 `BootstrapRunner` 클래스로 작성
- 가짜 사용자 시드: 5~10명, Phase 2에서 함께 생성 (다양한 프로필 조건 커버)
