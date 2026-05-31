# [MOZI] 노인 맞춤형 통합 복지 안내 서비스 백엔드 실행 계획

## 📌 진행 원칙

1. **각 Phase는 끝나면 "동작하는" 상태**여야 한다 (다음 Phase로 가기 전 검증 필수)
2. **코드 작성 전 설계 먼저** — 큰 구조는 사용자 승인 후 구현
3. **각 Phase의 검증 기준(✅)을 통과해야 다음 Phase로 진행**
4. **의존성과 리스크를 명시** — 막힘 포인트를 미리 파악

---

## Phase 0: 사전 설계 (Pre-Design)
> ⚠️ 코드 작성 전 단계. 모든 산출물은 `docs/`에 저장.

### 작업 항목
- [x] PRD, USER_FLOW, ERD_REQUIREMENTS 문서 최종 검토
- [x] **ERD 최종안 작성** — `docs/ERD_REQUIREMENTS.md`의 To-Be 요구사항을 반영해 최종 ERD 도출
  - User / UserProfile 분리 vs 통합 결정
  - **Welfare 상속 매핑**: `WelfareCommon`(부모) + 4개 자식 (`Central`, `Local`, `Private`, **`Seoul` 별도 유지**)
  - 상속 전략: `JOINED` 권장
  - Category / Tag 정규화 구조 확정 (코드성 데이터의 출처별 컬럼명 통일 포함)
  - Bookmark N:M 매핑 테이블 확정 (`User` ↔ `WelfareCommon`)
  - **인덱스 전략** 명시 (`welfare_type`, `region_name`, FK들)
  - **챗봇 대화 이력(ChatLog) 저장 여부 결정** (MVP 권장: 저장 X)
- [x] **API 엔드포인트 목록 초안** 작성 (`docs/API_SPEC_DRAFT.md`)
  - 도메인별 메서드/경로/요청·응답 구조 표 형태
- [ ] **챗봇 서버 인터페이스 합의** — 챗봇 팀과 Request/Response 스펙 협의 (Phase 5 진입 시 본격 진행)
  - 챗봇 팀이 우리 백엔드의 호출 형태에 맞춰주는 방향으로 협의됨 → 우리가 외부 API 명세를 설계해 제안

### 검증 ✅
- ERD 다이어그램(Mermaid 또는 이미지)이 `docs/`에 존재
  - **`WelfareSeoul`이 별도 자식 엔티티로 표시되어 있는지 반드시 확인**
- API 엔드포인트 초안 표가 존재
- 사용자(본인)가 ERD와 API 초안을 검토하고 승인

### 의존성 / 리스크
- **리스크**: 챗봇 인터페이스 미확정 → Mock 스펙으로 가정하고 진행, Phase 5에서 실제와 다르면 리팩토링 필요

---

## Phase 1: 백엔드 기초 세팅 및 환경 구성

### 작업 항목
- [x] Spring Initializr로 프로젝트 생성 (Java 21, Spring Boot 3.5.14)
  - 의존성: Spring Web, Spring Data JPA, MySQL Driver, Spring Security, Lombok, Validation
- [x] Git 초기화 및 `.gitignore` 작성
  - `application-local.yml`, `application-prod.yml`, `.env`, `build/`, `.idea/` 등 등록
- [x] **MySQL 로컬 DB 설치 및 스키마 생성** (`mozi_dev`)
- [x] `application.yml` 기본 설정 + `application-local.yml` 분리 (DB 접속 정보는 local에)
- [x] `docs/` 폴더에 산출물(PRD, USER_FLOW, ERD_REQUIREMENTS, EXECUTION_PLAN, CLAUDE.md) 정리
- [x] **공통 컴포넌트 골격 작성**:
  - `global/common/ApiResponse<T>` — 통일 응답 포맷
  - `global/exception/BusinessException` — 비즈니스 예외 베이스
  - `global/exception/GlobalExceptionHandler` — `@RestControllerAdvice`
  - `global/common/BaseEntity` — JPA Auditing용 추상 엔티티
- [x] JPA Auditing 활성화 (`@EnableJpaAuditing`)
- [x] 헬스체크 엔드포인트 `/api/health` 작성 (서버 동작 확인용)

### 검증 ✅
- `./gradlew bootRun` 실행 시 포트 8080에서 에러 없이 구동
- DB 커넥션 로그 정상 출력
- `GET /api/health` 호출 시 통일 응답 포맷의 `200 OK` 반환

### 의존성 / 리스크
- **리스크**: MySQL 미설치 환경 → 로컬에 MySQL 또는 Docker로 띄우기

---

## Phase 2: 데이터베이스 ERD 설계 구현 (엔티티 + Repository)

### 작업 항목
- [x] **User 도메인 엔티티 구현**
  - `User` (인증 정보: email, password, role)
  - `UserProfile` (맞춤 정보: 거주지, 가구형태, 소득유형, 각종 boolean 플래그)
  - `User` ↔ `UserProfile` 1:1 매핑
  - `RefreshToken` (user_id FK, token_hash UK, expires_at) — User ↔ RefreshToken 1:N
  - User cascade 설정: `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` for UserProfile/Bookmark/RefreshToken

- [x] **Welfare 도메인 엔티티 구현 (상속 매핑) ⭐**
  - `WelfareCommon` (공통 부모 — id, title, summary, organization, target, applicationMethod, welfare_type 등)
  - 자식 엔티티 **4종 모두 별도로 구현**:
    - `WelfareCentral` — 복지로 중앙부처 데이터용
    - `WelfareLocal` — 복지로 지자체 데이터용
    - `WelfarePrivate` — 복지로 민간 데이터용
    - **`WelfareSeoul` — 서울복지포털 데이터용 (별도 자식, 통합 X)**
  - 상속 전략: `@Inheritance(strategy = InheritanceType.JOINED)`
  - 출처 구분: `welfare_type` 컬럼 활용

- [x] **Category / Tag 도메인 엔티티**
  - 콤마 분리 텍스트(`interest_theme_code`, `household_status_code`)를 N:M 매핑으로 정규화
  - 출처별 컬럼명은 이미 통일됨 (서울 데이터를 시드 단계에서 정규화 완료)
  - 자세한 코드 정의는 docs/CATEGORY_REFERENCE.md 참조
  - `Category` 엔티티 + `WelfareCategory` N:M 매핑 테이블

- [x] **Bookmark 엔티티** — User ↔ `WelfareCommon`(부모) N:M 해소 테이블

- [x] 각 도메인의 Repository 인터페이스 (`extends JpaRepository`)

- [x] **시드 데이터 적재 (Phase 2-4 완료)**
  - 4개 JSON 파일 → 각 자식 엔티티 매핑 (central/local/private/seoul) — 약 1,465 row 적재
  - Category 22행 (THEME 15 + STATUS 7)
  - 가짜 사용자 7명 (senior01~07@mozi.test, BCrypt 해싱)
  - 모든 출처 동일 컬럼명 사용 (서울 데이터 사전 정규화 완료)

### 검증 ✅
- 앱 실행 시 `ddl-auto: update`(또는 Flyway)로 테이블 정상 생성
  - **`WELFARE_COMMON` + 4개 자식 테이블(`WELFARE_CENTRAL`, `WELFARE_LOCAL`, `WELFARE_PRIVATE`, `WELFARE_SEOUL`)이 모두 생성되어 있는지 확인**
- MySQL Workbench/DataGrip에서 테이블 구조 확인
- 시드 스크립트 실행 후 4개 출처의 가짜 복지 데이터 + 가짜 사용자가 DB에 적재됨
- Repository 단위 테스트(`@DataJpaTest`)로 기본 CRUD 동작 확인

### 의존성 / 리스크
- **리스크 1**: 크롤링 데이터의 컬럼 길이가 예상보다 길 수 있음 → `TEXT` 또는 `LONGTEXT` 활용

---

## Phase 3: 인증 / 인가 시스템 구축
> ⚠️ 이 Phase가 Phase 4의 전제조건. 모든 사용자 데이터 접근에 인증이 필요함.

### 작업 항목
- [x] Spring Security 설정 (`SecurityConfig`)
- [x] **JWT 유틸 클래스** (토큰 생성, 검증, 파싱)
- [x] **JWT 인증 필터** — 요청 헤더의 `Authorization: Bearer ...`에서 토큰 추출 후 인증 컨텍스트 등록
- [x] **회원가입 API**: `POST /api/auth/signup`
  - 이메일/비밀번호 검증, 중복 체크, BCrypt 해싱 후 저장
  - 가입 즉시 access + refresh 발급 (RefreshToken row 신규)
- [x] **로그인 API**: `POST /api/auth/login`
  - 이메일/비밀번호 검증 → access + refresh 발급 (RefreshToken row 신규)
- [x] **토큰 갱신 API**: `POST /api/auth/refresh`
  - Refresh Token Rotation 구현 — 이전 token_hash row 삭제 후 새 access + refresh 발급
  - 위조/만료 시 401 `INVALID_REFRESH_TOKEN`
- [x] **로그아웃 API**: `POST /api/auth/logout`
  - 해당 user의 RefreshToken row 일괄 삭제 (다중 기기 일괄 로그아웃)
- [x] JWT 만료시간 정책: access 1h / refresh 7d
- [x] 만료된 RefreshToken row 정리는 MVP에서 진행하지 않음 (누적 허용). v2에서 스케줄러로 정리 검토.
- [x] 인증 필요 / 공개 엔드포인트 구분 (SecurityConfig의 permitAll/authenticated)
- [x] **테스트 작성**:
  - 회원가입 정상 플로우, 중복 이메일 예외
  - 로그인 정상 플로우, 잘못된 비밀번호 예외
  - 보호된 엔드포인트에 토큰 없이 접근 → 401

### 검증 ✅
- 가입 → 로그인 → JWT 수신 → 보호된 API 호출 시 200
- 토큰 없이 보호된 API 호출 시 401 + 통일된 에러 JSON 응답
- 잘못된 토큰으로 호출 시 401
- `./gradlew test`로 전체 테스트 통과

### 의존성 / 리스크
- **리스크**: JWT Secret 관리 → `application.yml`에 하드코딩 금지, 환경 변수로 주입

---

## Phase 4: 핵심 비즈니스 로직 (User Profile / Welfare / Bookmark)

### 작업 항목

#### 4-1. UserProfile / User API
- [x] `GET /api/users/me/profile` — 내 프로필 조회
  - 응답에 `isCompleted: boolean` 포함 (한 번이라도 저장됐는지)
- [x] `PUT /api/users/me/profile` — 내 프로필 수정 (없으면 생성)
  - 요청에 없는 필드는 변경 X. 명시적 `null`도 무변경(옵션 C 정책 — Jackson record 한계로 absent vs explicit null 구분 불가, 클리어 동작 미지원)
- [x] `PUT /api/users/me/password` — 비밀번호 변경
  - 현재 비밀번호 BCrypt 검증 → 새 비밀번호 해싱 후 갱신
  - 해당 user의 RefreshToken row 일괄 삭제 (다중 기기 무효화)
  - 응답 메시지에 "다른 기기는 다시 로그인이 필요해요" 안내 포함
- [x] `DELETE /api/users/me` — 회원 탈퇴 (Hard Delete + Cascade)
  - User + UserProfile + Bookmark + RefreshToken 일괄 삭제
- [x] DTO 분리 (Entity 직접 노출 금지)
- [x] 단위/통합 테스트 작성

#### 4-2. Welfare 통합 조회 API
- [x] `GET /api/welfares` — 키워드 + 카테고리 + 지역 기반 동적 필터링
  - QueryParam: `keyword`, `category`, `region`, `welfareType`, `page`, `size`
  - **키워드 검색 대상 컬럼**: `WelfareCommon.title`, `WelfareCommon.summary` (LIKE %keyword%)
  - 로그인 사용자: 본인 북마크만 자동 반영 (`isBookmarked`)
  - 비로그인: 동일 query params 적용, isBookmarked 모두 false
  - ⚠️ **Step 6(USER_PROFILE_REDESIGN, 2026-05-17) 폐기**: 기존 `applyMyProfile` 파라미터 + boolean→STATUS 자동 매핑 정책은 제거됨. 본인 맞춤 추천은 챗봇 흐름이 전담.
  - **응답: 4개 출처 모두 통합된 결과** (welfare_type으로 출처 구분)
  - `WelfareSummaryDto`에 `isBookmarked` 필드 포함 (로그인 시 본인 기준 / 비로그인 시 false)
- [x] `GET /api/welfares/{id}` — 상세 조회
  - 부모(`WelfareCommon`) + 해당 자식 엔티티 정보 포함
  - 자식 종류별 nullable detail 필드 (centralDetail/localDetail/privateDetail/seoulDetail) 중 1개만 채워짐
  - `isBookmarked` 필드 포함
- [x] 페이지네이션 적용 (`Pageable`) — 커스텀 `PageResponse<T>` 래퍼 (`items/page/size/totalCount/hasNext`)
- [x] **N+1 쿼리 점검** — 검색 결과 categories와 isBookmarked 모두 1회 IN 쿼리로 배치 lookup, `@EntityGraph(category)` 적용
- [x] 단위/통합 테스트 작성 (4개 출처 모두 + isBookmarked 동작 검증, Step 6에서 applyMyProfile 테스트 3건 삭제)
- [x] `GET /api/categories?type=THEME|STATUS` — 카테고리 마스터 조회 (type 필수, 22행 시드 기반)

#### 4-3. Bookmark API
- [x] `POST /api/bookmarks/{welfareId}` — 북마크 추가 (idempotent, 응답 `{bookmarkId}`)
- [x] `DELETE /api/bookmarks/{welfareId}` — 북마크 삭제 (응답 `{deleted: true}`, WELFARE_NOT_FOUND/BOOKMARK_NOT_FOUND 분리)
- [x] `GET /api/bookmarks` — 내 북마크 목록 조회 (페이지네이션, PageResponse<WelfareSummaryDto>, isBookmarked=true 고정, createdAt DESC)
- [x] 중복 북마크 시도 시 idempotent 처리 — 두 번 모두 200 + 같은 bookmarkId (API_SPEC 정합)
- [x] 단위/통합 테스트 작성 — BookmarkServiceTest 10개 + BookmarkControllerIntegrationTest 11개

### 검증 ✅
- Postman 또는 Swagger로 모든 엔드포인트 정상 호출 확인
- 통일 응답 포맷으로 200/4xx 응답 일관성 확인
- 시드 데이터 기반으로 가짜 사용자 A의 프로필 조건에 따라 다른 복지 리스트 반환됨을 확인
- **4개 출처(중앙/지자체/민간/서울)의 데이터가 모두 통합 조회 결과에 포함되는지 확인**
- ~~`applyMyProfile=false`로 본인 조건 무시 동작 확인~~ — Step 6에서 폐기. 검색은 항상 query params만 사용.
- 키워드 검색이 title + summary에서만 동작하는지 확인 (targetAudience 미포함)
- `./gradlew test` 전체 통과

### 의존성 / 리스크
- **리스크 1**: 동적 쿼리 복잡도 → QueryDSL 도입 검토 (`build.gradle` 추가는 사전 승인)
- **리스크 2**: 4개 자식 엔티티 통합 조회 시 N+1 → fetch join으로 해결
- **리스크 3**: 자식 엔티티별 필드가 달라 응답 DTO 설계 까다로움 → 자식별 별도 DTO + 부모 DTO 합성 패턴 권장

---

## Phase 5: 외부 통신 브릿지 (챗봇 연동)

### 작업 항목
- [x] **챗봇 통신 인터페이스 정의** (인터페이스 + 구현체)
  - `ChatbotClient` 인터페이스
  - `MockChatbotClient` Mock 구현체 (시드 기반 결정적 무작위 추천)
  - 실 구현체(`RestChatbotClient`)는 챗봇 팀 서버 준비 후 별도 작업으로 추가 — `ChatbotClientConfig` 스켈레톤 + TODO 주석으로 준비
- [x] **Mock/실 구현 토글**: `@ConditionalOnProperty(mozi.chatbot.mock-enabled)`로 분기. Mock + 실 모두 없으면 부팅 실패 (의도된 안전망)
- [x] **챗봇 연동 API**: `POST /api/chat`
  - Request: `{ message, conversationId? }`
  - 백엔드 처리:
    1. JWT에서 userId 추출 (인증 필수)
    2. UserProfile 조회 (없으면 anonymous context)
    3. `conversationId`가 없으면 UUID v4 발급 (백엔드 책임), 있으면 그대로 전달
    4. 챗봇 서버에 `{ message, conversationId, user: {userId, profile} }` 전달
    5. 챗봇 응답 `recommendedWelfareIds`를 4-2/4-3 N+1 회피 패턴으로 hydrate → `WelfareSummaryDto[]`
    6. `{ reply, welfares, conversationId }` 통합 응답 반환
  - **conversationId 저장 책임**: MOZI 백엔드는 저장 X (브릿지). 챗봇 서버가 자체 보관.
  - 클라이언트는 `sessionStorage`에 conversationId 보관 (페이지 이동/복귀 시 유지, 창 닫기/로그아웃 시 자동/수동 삭제)
- [x] **타임아웃 설정** (8초) → `ChatbotTimeoutException` → 504 `CHATBOT_TIMEOUT`
- [x] **에러 처리**: 3종 도메인 예외로 분리 — TIMEOUT(504) / UNAVAILABLE(503) / INVALID_RESPONSE(502)
- [x] **외부 API 명세 문서 작성**: `docs/CHATBOT_API_CONTRACT.md` 신규 — 챗봇 팀이 우리 명세에 맞춰 구현
- [x] 단위/통합 테스트 — MockChatbotClientTest 5개 + ChatServiceTest 10개 + ChatControllerIntegrationTest 8개

### 검증 ✅
- Mock 서버 활성화 상태에서 `POST /api/chat` 호출 → 더미 추천 복지 리스트 반환
- 챗봇 서버 timeout/장애 시 500이 아닌 친화적 에러 메시지 반환
- (실 챗봇 서버 준비 완료 시) 실연동 테스트 통과

### 의존성 / 리스크
- **리스크 1**: 챗봇 서버 미완성 → Mock으로 발표까지 진행 가능하도록 준비
- **리스크 2**: 챗봇 서버 응답 스펙 변경 → 인터페이스 분리로 영향 최소화
- **리스크 3**: 추천 복지 ID가 우리 DB에 없는 경우 → 누락된 ID는 응답에서 제외하고 로깅

---

## Phase 6: 문서화 및 프론트엔드 연계 준비

### 작업 항목
- [ ] **Springdoc(OpenAPI) 도입** — Swagger UI 자동 생성
  - `build.gradle`에 `springdoc-openapi-starter-webmvc-ui` 추가 (사전 승인)
  - `@Operation`, `@Schema` 어노테이션으로 명세 보강
- [ ] **API_SPEC.md 자동 생성/수동 정리** — Markdown 표 형식
  - Claude Code에게 "전체 컨트롤러 스캔 후 API 명세 표로 추출" 요청
- [ ] **CORS 설정**
  - 허용 Origin: `http://localhost:3000` (프론트 개발), 추후 배포 도메인 추가
  - 허용 Method: GET, POST, PUT, DELETE, OPTIONS
  - 허용 Header: `Authorization`, `Content-Type`
- [ ] **에러 핸들링 고도화**
  - 모든 예외 케이스에 대해 통일된 에러 코드 + 사용자 친화 메시지 매핑
  - `VALIDATION_FAILED` 응답에 `fields` 객체 포함 (필드별 에러 메시지)
  - 에러 코드 표 정리 (`docs/ERROR_CODES.md`)
- [ ] **로깅 정책 정리** — 요청/응답, 예외, 외부 호출 로그 레벨 통일
- [ ] **README.md 작성** — 프로젝트 실행 방법, API 문서 링크, 환경 변수 설명

### 검증 ✅
- Swagger UI(`/swagger-ui.html`) 접속 시 모든 API 명세 확인 가능
- 프론트엔드 개발자(본인)가 `docs/API_SPEC.md`만 보고 연동 시작 가능
- CORS 설정으로 로컬 프론트에서 API 호출 시 에러 없음

---

## Phase 7: AWS 임시 배포 (졸업 발표 직전)
> ⚠️ 이 Phase는 백엔드 완성 + 프론트엔드 어느 정도 진행 후 학습/실행

### 작업 항목 (개요만, 상세는 학습 후 보완)
- [ ] AWS EC2 인스턴스 생성 (프리티어)
- [ ] MySQL: RDS 또는 EC2 내 설치
- [ ] 환경 변수 주입 방식 결정 (Systems Manager Parameter Store 또는 .env 파일)
- [ ] 보안 그룹 설정 (8080, 22, 3306 등)
- [ ] (선택) Nginx 리버스 프록시 + HTTPS (Let's Encrypt)
- [ ] 배포 자동화는 진행하지 않음 — 수동 배포

### 검증 ✅
- 외부에서 `https://...` 또는 `http://...:8080/api/health` 접근 시 정상 응답
- 발표 시연 동안 안정적으로 동작

---

## 📊 진행 현황 추적

| Phase | 상태 | 완료일 | 비고 |
|---|---|---|---|
| 0. 사전 설계 | ✅ | 2026-04 | 챗봇 인터페이스 합의만 Phase 5 시작 시점으로 연기 |
| 1. 기초 세팅 | ✅ | 2026-04 | 공통 컴포넌트 + 헬스체크 |
| 2. ERD 구현 | ✅ | 2026-05 | 4개 자식 엔티티 (서울 별도) + 시드 1,465 row + 7 가짜 사용자 |
| 3. 인증/인가 | ✅ | 2026-05 | JWT + Refresh Token Rotation, 4개 auth 엔드포인트 |
| 4. 핵심 비즈니스 | ✅ | 2026-05 | User account 4 + Welfare 2 + Category 1 + Bookmark 3 = 10 엔드포인트 |
| 5. 챗봇 연동 | ✅ | 2026-05 | Mock 구현 + 외부 API 계약서 완료. 실 챗봇 서버 연동(`RestChatbotClient`)은 챗봇 팀 완성 후 별도 작업 |
| 6. 문서화/연계 | ✅ | 2026-05 | Springdoc + Swagger UI + CORS(5173/3000) + ERROR_CODES + API_SPEC + FRONTEND_INTEGRATION_GUIDE + README |
| **USER_PROFILE_REDESIGN** | **✅** | **2026-05-17** | **Step 1~10: REGION 마스터(229행) 신규, UserProfile 스키마 재설계(sidoCode/sigunguCode + Enum + boolean 6→4), applyMyProfile 폐기, 챗봇 페이로드 본격 매핑(라벨/age 변환), INVALID_REGION_CODE 신규, FRONTEND_MIGRATION_NOTES/CHATBOT_MIGRATION_NOTES 작성. 근거: `docs/USER_PROFILE_REDESIGN_PLAN.md`** |
| 7. AWS 배포 | ⏳ | - | 백/프론트 완성 후 |

> 상태 표기: ⏳ 대기 / 🚧 진행 중 / ✅ 완료 / ⚠️ 블로킹

---

## 🚧 전체 리스크 관리

| 리스크 | 영향 | 대응 |
|---|---|---|
| 챗봇 서버 인터페이스 미확정 | Phase 5 지연 | Mock으로 우선 개발, 인터페이스 분리 설계 |
| 챗봇 서버 완성 지연 | 발표 시연 불가 | Mock 서버로 시연 가능하도록 유지 |
| 크롤링 데이터의 비정형성 | Phase 2 시드 실패 | 시드 스크립트에 데이터 검증/정제 로직 포함 |
| 출처별 데이터 스키마 차이 | Phase 2/4 복잡도 증가 | 4개 자식 엔티티로 분리 설계 + 컬럼명은 시드 데이터 사전 정규화로 통일 |
| 1인 개발 일정 압박 | 전체 지연 | "안 만들 것" 목록 엄격히 준수, 욕심 자제 |
| AWS 배포 학습 부족 | Phase 7 실패 | 백/프론트 완성 후 충분한 학습 시간 확보, 안 되면 로컬 시연으로 대체 |
