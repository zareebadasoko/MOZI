# MOZI 백엔드

> 노인 맞춤형 통합 복지 안내 서비스의 **Spring Boot API 서버**.
> 졸업 프로젝트 / 1인 풀스택 개발 / 시연용 임시 배포.

본 디렉터리는 `moji_project` 모노레포의 백엔드 하위 프로젝트다. 프론트엔드는 `../frontend_project/`에서 별도 개발 예정.

---

## 🚀 빠른 시작

### 1) 사전 요구사항

| 항목 | 버전 |
|---|---|
| Java | 21+ (Temurin 권장) |
| MySQL | 8.0+ (로컬 또는 Docker) |
| Gradle | Wrapper 포함 (`./gradlew`) |

### 2) DB 준비

```sql
CREATE DATABASE mozi_dev DEFAULT CHARACTER SET utf8mb4;
CREATE USER 'mozi'@'localhost' IDENTIFIED BY '<password>';
GRANT ALL PRIVILEGES ON mozi_dev.* TO 'mozi'@'localhost';
```

### 3) `application-local.yml` 작성 (gitignore 대상)

`src/main/resources/application-local.yml`을 직접 생성:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mozi_dev?serverTimezone=UTC
    username: mozi
    password: <password>

mozi:
  seed:
    enabled: true  # 첫 부팅 시 시드 적재
  jwt:
    secret: <충분히-긴-랜덤-문자열-256bit-이상>
  chatbot:
    mock-enabled: true  # 챗봇 서버 없이 단독 시연 모드
```

> ⚠️ 본 파일은 **반드시 gitignore**. 비밀 정보가 들어간다.

### 4) 부팅

```bash
./gradlew bootRun
```

성공 시:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- 헬스체크: http://localhost:8080/api/health

---

## 📁 디렉터리 구조

```
backend_project/
├── src/
│   ├── main/java/com/mozi/backend/
│   │   ├── domain/        # user, welfare, bookmark, category, chat
│   │   ├── global/        # config, security, exception, common
│   │   └── BackendApplication.java
│   └── main/resources/
│       ├── application.yml         # 공통 (비밀 정보 X)
│       ├── application-local.yml   # 로컬용 (gitignore)
│       └── application-mock.yml    # 챗봇 Mock 모드
├── src/test/java/...    # 단위/통합 테스트
├── seed-data/           # 복지 크롤링 시드 (수정 금지)
├── docs/                # 명세서
├── build.gradle
├── CLAUDE.md            # Claude Code 작업용 컨벤션
└── README.md            # 본 문서
```

---

## 📚 문서

| 문서 | 용도 |
|---|---|
| [docs/PRD.md](docs/PRD.md) | 제품 명세 |
| [docs/USER_FLOW.md](docs/USER_FLOW.md) | 사용자 시나리오 |
| [docs/ERD_REQUIREMENTS.md](docs/ERD_REQUIREMENTS.md) | DB 설계 요구사항 |
| [docs/EXECUTION_PLAN.md](docs/EXECUTION_PLAN.md) | 단계별 실행 계획 (Phase 0~7) |
| [docs/API_SPEC.md](docs/API_SPEC.md) | **확정 API 명세** (16 엔드포인트) |
| [docs/ERROR_CODES.md](docs/ERROR_CODES.md) | 에러 코드 + 프론트 노출 정책 |
| [docs/FRONTEND_INTEGRATION_GUIDE.md](docs/FRONTEND_INTEGRATION_GUIDE.md) | **프론트 작업의 출발점** |
| [docs/CHATBOT_API_CONTRACT.md](docs/CHATBOT_API_CONTRACT.md) | 챗봇 서버 ↔ MOZI 백엔드 외부 계약서 |
| [docs/CATEGORY_REFERENCE.md](docs/CATEGORY_REFERENCE.md) | 카테고리 코드 마스터 |

---

## 🧪 테스트

```bash
# 전체
./gradlew test

# 특정 클래스
./gradlew test --tests "com.mozi.backend.global.config.CorsIntegrationTest"
```

빌드 + 테스트:
```bash
./gradlew build
```

---

## 🛠️ 자주 쓰는 명령어

| 목적 | 명령 |
|---|---|
| 부팅 | `./gradlew bootRun` |
| 빌드 | `./gradlew build` |
| 테스트 | `./gradlew test` |
| 빌드 청소 | `./gradlew clean` |
| 의존성 확인 | `./gradlew dependencies` |

---

## 🔌 외부 시스템

### 챗봇 서버 (LLM + RAG)
- 별도 팀이 개발 중. MOZI 백엔드는 **브릿지**로 동작.
- 챗봇 서버 미준비 시 `mozi.chatbot.mock-enabled: true`로 단독 시연 가능.
- 외부 API 계약: [docs/CHATBOT_API_CONTRACT.md](docs/CHATBOT_API_CONTRACT.md)

### MySQL
- 로컬 개발은 단일 인스턴스. 시연 배포 시 RDS 검토 (Phase 7).

---

## 🔐 환경 변수 / 비밀 관리

| 키 | 용도 | 위치 |
|---|---|---|
| `spring.datasource.url` | DB 접속 | `application-local.yml` |
| `spring.datasource.username` | DB 사용자 | 동상 |
| `spring.datasource.password` | DB 비밀번호 | 동상 |
| `mozi.jwt.secret` | JWT 서명 키 (256bit+) | 동상 또는 env `MOZI_JWT_SECRET` |
| `mozi.chatbot.api-key` | 챗봇 서버 API Key | env `CHATBOT_API_KEY` |
| `mozi.chatbot.base-url` | 챗봇 서버 URL | env `CHATBOT_BASE_URL` |

**원칙**: 비밀 정보는 코드/yml에 하드코딩 금지. `application-local.yml` 또는 환경 변수만 사용.

---

## 📌 진행 현황 (Phase 0~7)

상세는 [docs/EXECUTION_PLAN.md](docs/EXECUTION_PLAN.md) 참조.

| Phase | 상태 | 비고 |
|---|---|---|
| 0. 사전 설계 | ✅ | |
| 1. 기초 세팅 | ✅ | |
| 2. ERD/엔티티 | ✅ | 시드 1,465 row + 가짜 사용자 7명 |
| 3. 인증/인가 | ✅ | JWT + Refresh Token Rotation |
| 4. 핵심 비즈니스 | ✅ | User/Welfare/Bookmark/Category — 10 엔드포인트 |
| 5. 챗봇 브릿지 | ✅ | Mock 우선 + 외부 계약서 작성 |
| 6. 문서화/연계 | ✅ | Swagger UI + 4종 문서 |
| 7. AWS 배포 | ⏳ | 백엔드·프론트 완성 후 |

---

## 🤝 컨벤션

작업 시 [CLAUDE.md](CLAUDE.md) 참조. 핵심:
- 응답: `ApiResponse<T>` 통일
- 예외: `BusinessException` 상속 + `@RestControllerAdvice` 전역 처리
- 엔티티: API 응답 직접 노출 금지 → 항상 DTO
- 커밋: Conventional Commits (`feat:`, `fix:`, `docs:` 등)
- 브랜치: `main` 직접 푸시 금지 → `feature/*`

---

## 📞 참고

- Spring Boot 3.5.14 / Java 21 / Spring Data JPA / Spring Security
- JWT: `io.jsonwebtoken:jjwt 0.12.6`
- API 문서 자동화: `org.springdoc:springdoc-openapi 2.8.6`
- MySQL Driver: `com.mysql:mysql-connector-j`
