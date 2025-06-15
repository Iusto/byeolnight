# 🌌 별 헤는 밤 (Byeolnight) - 커뮤니티 & AI 통합 서비스

“기능만 되는 백엔드가 아닌, **운영 환경에서 살아남는 구조**를 설계합니다.”  
이 프로젝트는 인증/보안/확장성/로깅/클린 아키텍처를 모두 고려한 신입 백엔드 포트폴리오입니다.

---

## 🛠 기술 스택 및 선택 이유

| 분류       | 기술                  | 선택 이유 |
|------------|-----------------------|-----------|
| Language   | Java 21               | LTS, 최신 문법(Stream, Record 등) |
| Framework  | Spring Boot 3.2       | 보안 + 확장성 + 생산성 |
| ORM        | Spring Data JPA       | 추상화된 Repository 기반 설계 |
| DB         | MySQL                 | 관계형 데이터의 안정성과 범용성 |
| Infra      | AWS S3, Redis         | 파일 저장 및 인증 코드/세션/토큰 관리 |
| CI/CD      | GitHub Actions, Docker | 실무 수준의 자동화된 배포 |
| Test       | JUnit5, Mockito       | 단위 테스트 검증 |
| API 문서   | Swagger (OpenAPI 3.0) | 명세 기반 문서화 |
| Frontend   | React (예정)          | REST + WebSocket 소비자 관점 개발 |

---

## 🧱 아키텍처 구성

- **도메인 중심 구조 분리**: `domain`, `service`, `dto`, `controller`, `infrastructure`
- **JWT 기반 인증 시스템**: Access / Refresh 분리, Redis 저장소 연동
- **Spring Security 권한 제어**: @PreAuthorize로 Role 기반 접근 통제
- **파일 업로드**: Presigned S3 방식
- **WebSocket 채팅**: STOMP + SockJS, 구조 분리
- **클린 아키텍처 흐름 정립**: 의존성 방향 및 계층 역할 명확화

---

## 🔐 인증 및 보안 설계

- 이메일/휴대폰 인증 시스템 (Gmail SMTP + CoolSMS)
- 로그인 실패 누적 시 계정 잠금 처리
- 비밀번호 재확인 후 민감 정보 수정
- Refresh 토큰 Redis 저장 + 블랙리스트 처리
- `@Valid`, 예외처리 전역 핸들링, 계정 상태 체크 (정지, 탈퇴 등)

---

## 📡 실시간 채팅 설계

- `/chat.send` → 공용 채널 브로드캐스트
- `/chat.dm.{to}` → 개인 DM 채널 라우팅
- Stomp.js 기반, 추후 Redis Pub/Sub 확장 가능

---

## 📁 S3 Presigned 업로드

- `/files/presign` 호출 → 클라이언트가 직접 업로드 가능
- UUID + User ID 기반 경로 분리
- 허용 확장자: jpg, jpeg, png, gif

---

## 🤖 AI 뉴스 자동 게시 (진행 중)

- 기존 Galaxy ML 모델 경험 → 실용 기능화
- 우주 뉴스 크롤링 + 텍스트 분류 모델 → 커뮤니티 자동 등록
- 6월 내 완성 및 배포 목표

---

## 🔧 관리자 기능 및 보안 강화 기능 업데이트 (2025-06-15)

### ✅ 관리자 기능 추가
- 관리자 전용 API 경로 `/api/admin/**` 구현
- `@PreAuthorize("hasRole('ADMIN')")` 기반 Role 인증 적용
- 주요 기능:
  - 전체 사용자 요약 정보 조회
  - 사용자 계정 상태 변경 (ACTIVE, SUSPENDED 등)
  - 관리자에 의한 강제 탈퇴 처리

### 🔐 로그인 보안 정책 강화
- 로그인 시 유저 상태(`UserStatus`) 확인 로직 추가
- `SUSPENDED`, `BANNED` 계정은 로그인 불가 처리
- 실패 시 `401 Unauthorized` 응답과 명확한 메시지 제공

### ✅ API 테스트 결과
- `/api/admin/test` → ADMIN 토큰으로 200 인증 성공
- `/api/admin/test` → USER 토큰으로 403 Forbidden 반환 확인
- 계정 상태 변경 성공 (e.g., SUSPENDED → 로그인 차단 테스트 통과)
- 관리자 권한으로 강제 탈퇴 API 테스트 완료

### 🛠️ 기타
- `UserStatus` 열거형 기반 상태 제어 로직 일원화
- `User` 엔티티 내 `changeStatus(UserStatus)` 도메인 메서드 추가
- Swagger에서 관리자 인증 자동 설정 (`@SecurityRequirement(name = "bearerAuth")`)
- `.yml` 설정으로 Spring Redis, JWT 인증 정상 동작 확인

---

## 🔄 API 명세 요약

### 🔑 Auth
- `POST /api/auth/signup` 회원가입
- `POST /api/auth/login` 로그인
- `POST /api/auth/logout` 로그아웃
- `POST /api/auth/token/refresh` 토큰 재발급
- `DELETE /api/auth/withdraw` 탈퇴
- `POST /api/auth/email/send` /verify
- `POST /api/auth/phone/send` /verify
- `POST /api/auth/password/reset-request` /reset

### 👤 User
- `GET /api/users/me` 내 정보
- `PUT /api/users/profile` 프로필 수정

### 📄 Post
- `GET /api/public/posts` 목록
- `GET /api/public/posts/{id}` 상세
- `POST /api/member/posts` 작성
- `PUT /api/member/posts/{id}` 수정
- `DELETE /api/member/posts/{id}` 삭제
- `POST /api/member/posts/posts/{postId}/like` 추천
- `POST /api/posts/{postId}/report` 신고

### 💬 Comment
- `POST /api/comments` 댓글 수정
- `GET /api/comments/post/{postId}` 게시글 댓글 목록 조회
- `PUT /api/comments/{id}` 댓글 생성
- `DELETE /api/comments/{id}` 댓글 삭제

### 📁 File (S3)
- `POST /api/files/presign`

### 👮 Admin
- `GET /api/admin/users` 전체 사용자 조회
- `GET /api/admin/test` 관리자 인증
- `PATCH /api/admin/users/{id}/lock` 사용자 계정 잠금
- `PATCH /api/admin/users/{userId}/status` 회원 상태 변경
- `DELETE /api/admin/users/{userId}` 회원 강제 탈퇴

---

## ✅ 구현 기능 체크리스트

| 항목 | 상태        |
|------|-----------|
| 회원가입/로그인 | ✅         |
| 이메일/휴대폰 인증 | ✅         |
| JWT Access/Refresh + 블랙리스트 | ✅         |
| 닉네임 6개월 제한 변경 | ✅         |
| 프로필 수정 시 비밀번호 확인 | ✅         |
| 게시글 CRUD + Soft Delete | ✅         |
| 댓글 CRUD | ✅         |
| 게시글 추천/신고 + 블라인드 처리 | ✅         |
| 관리자 페이지 기능 | ✅         |
| 계정 잠금 정책 | ✅         |
| 실시간 채팅 구조 설계 | ✅         |
| Swagger 문서화 | ✅         |
| 뉴스 자동 게시 기능 | ⏳ (7월 예정) |

---

## 🧠 회고 및 기술 선택 기준

- **왜 직접 구현했는가?**  
  직접 경험을 통해 운영 환경에서의 보안/확장성/에러처리 관점을 내재화하기 위함입니다.  
  단순한 기능 구현이 아닌, **시스템을 책임지는 백엔드**가 되기 위한 설계입니다.

- **어떤 경험을 얻었는가?**
  - Redis 캐시, 토큰 저장, 인증 시스템 분리 설계 경험
  - 인증/보안 정책 설계 (계정 잠금, 감사 로그, 닉네임 제한 등)
  - Spring Security & Swagger 통합 경험
  - Presigned URL을 통한 안전한 파일 업로드 구조 설계
  - ML 모델 설계 경험 → 실용적 뉴스 자동화 기능으로 확장 전환

---

## 🧪 테스트 코드 계획 (작성 중)

- UserService 테스트
- 로그인 / 실패 → 계정 잠금 흐름
- 게시글 좋아요/신고 관련 예외 흐름 테스트
- S3Service mock 테스트

---

## 🚀 예정된 작업

- [ ] React 기반 UI 구성
- [ ] Axios/React-Query 연동
- [ ] 채팅 연동 (SockJS + Stomp.js)
- [ ] 도메인 + Docker 배포

---

👨‍💻 감사합니다. “운영 가능한 실전 백엔드”를 계속 만들어 가고 있습니다.

---

## 🐳 Docker 기반 실행 가이드

이 프로젝트는 **도커를 통해 백엔드 전체 환경을 재현**할 수 있도록 구성되어 있습니다.

### 1️⃣ 실행 준비

#### 📂 환경 변수 파일 설정 (예: `config.template`)

- 보안상 이유로 `.env` 파일은 GitHub에 포함되지 않습니다.
- 아래 내용을 참고하여 실제 실행에 필요한 환경변수 파일을 직접 생성하세요 (예: `.env`, `config.env` 등):

```env
# DB
DB_URL=jdbc:mysql://mysql:3306/byeolnight
DB_USERNAME=root
DB_PASSWORD=password

# MAIL
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_FROM=your_email@gmail.com

# REDIS
REDIS_HOST=redis
REDIS_PORT=6379

# COOLSMS
COOLSMS_API_KEY=...
COOLSMS_API_SECRET=...
COOLSMS_SENDER=01012345678

# AWS
CLOUD_AWS_S3_BUCKET=your-bucket-name
CLOUD_AWS_ACCESS_KEY=...
CLOUD_AWS_SECRET_KEY=...
```

---

### 2️⃣ 실행 방법

```bash
# 프로젝트 루트에서 다음 명령어 실행
docker-compose up --build
```

- 위 명령어 하나로 아래 환경이 자동 구성됩니다:
  - Spring Boot 앱 (포트: 8080)
  - MySQL 8.0 (포트: 3306)
  - Redis (포트: 6379)

> 데이터베이스는 `spring.jpa.hibernate.ddl-auto=create` 설정에 따라 **자동 생성**됩니다.

---

### 3️⃣ 도커 설치 외에는 필요 없음

- Java, MySQL, Redis 등은 **모두 Docker로 격리 실행**되므로
- 타 컴퓨터에서도 Docker만 설치하면 동일한 환경을 재현할 수 있습니다.

---