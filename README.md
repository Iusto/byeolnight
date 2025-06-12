# 🌌 별 헤는 밤 (Byeolnight) - 커뮤니티 & AI 통합 서비스

“기능만 되는 백엔드가 아닌, **운영 환경에서 살아남는 구조**를 설계합니다.”  
이 프로젝트는 인증/보안/확장성/로깅/클린 아키텍처를 모두 고려한 신입 백엔드 포트폴리오입니다.

---

## 🛠 기술 스택 및 선택 이유

| 분류 | 기술 | 이유 |
|------|------|------|
| Language | Java 21 | LTS 버전, Stream + Record + 최신 문법 |
| Framework | Spring Boot 3.2 | 보안/확장성/생산성 모두 확보 |
| ORM | Spring Data JPA | Repository 기반 추상화, 도메인 중심 설계 |
| DB | MySQL | 관계형 데이터 + 운영환경 범용성 |
| Infra | AWS S3, Redis | 파일 저장, 세션 관리, 캐싱 |
| CI/CD | GitHub Actions, Docker | 실무 수준 자동 배포 |
| Test | JUnit5 + Mockito | 서비스 계층 단위 테스트 |
| Frontend | React (예정) | REST API 연동 + WebSocket 소비자 관점 |
| API Doc | Swagger (OpenAPI 3.0) | 명세 기반 테스트, 자동 문서화 지원 |

---

## 🧱 프로젝트 아키텍처

- **DDD 기반 레이어드 구조**: `domain`, `service`, `controller`, `dto`, `infrastructure`
- **JWT 기반 인증 시스템**: Access / Refresh Token 분리 + Redis 저장
- **S3 Presigned URL 업로드 방식**: 직접 S3 업로드 가능, 서버 자원 낭비 없음
- **WebSocket 기반 실시간 채팅**: STOMP 지원 + 확장 고려한 구조 설계
- **Redis 기반 인증 코드 저장 및 토큰 관리**
- **Spring Security 권한 관리**: `@PreAuthorize`, `@Secured`, 관리자 페이지 분리

---

## 🔐 인증 및 보안 설계

- [x] ✅ 이메일 인증 코드 발송 및 검증 (`/auth/email/send`, `/verify`)
- [x] ✅ 휴대폰 인증 코드 발송 및 검증 (`/auth/phone/send`, `/verify`)
- [x] ✅ 회원가입 시 이메일/닉네임 중복 검사 및 비밀번호 정책 적용
- [x] ✅ 로그인 실패 횟수 누적 + 계정 잠금 처리 (`loginFailCount`, `accountLocked`)
- [x] ✅ 비밀번호 재확인 후 프로필 수정 가능 (`/users/profile`)
- [x] ✅ JWT 재발급 시 감사 로그 저장 (`AuditRefreshTokenLog`)
- [x] ✅ Soft Delete + 블라인드 처리 분리 (운영/보안 분리 설계)

---

## 📡 실시간 채팅 구조

- `/chat.send` → 전체 메시지 브로드캐스트
- `/chat.dm.{to}` → 1:1 메시지 전송
- 향후 Redis Pub/Sub으로 메시지 처리 모듈화 예정

---

## 📁 파일 업로드 (S3 Presigned 방식)

- `/api/files/presign` 호출 → S3로 직접 업로드 가능한 URL 발급
- 사용자 ID 기반 디렉토리 구분: `/uploads/{userId}/{uuid}.jpg`
- 허용 확장자 제한: `jpg`, `jpeg`, `png`, `gif`

---

## 🤖 AI 뉴스 자동 게시 기능 (2025.06 적용)

- Galaxy ML 프로젝트를 통해 얻은 데이터 전처리/모델 실험 경험을 바탕으로,
- **실제 커뮤니티 게시판에 자동으로 뉴스를 업로드하는 AI 기반 기능**을 설계 및 개발 중입니다.

### 구성 흐름:
1. **뉴스 크롤링**: 우주 관련 뉴스 수집 (키워드 기반)
2. **텍스트 분류기**: 단순 키워드 필터링이 아닌, 머신러닝 모델 기반으로 우주 관련성 판별
3. **자동 게시**: Spring API 연동 → 게시글 자동 업로드

> Galaxy ML은 단독 실용성이 부족하다고 판단하여, "AI 모델 실험 경험"으로 정리하고,  
> 커뮤니티에 실질적으로 기여할 수 있는 기능으로 전환하여 적용 중입니다.

---

## 🔄 API 명세 요약

### 🔑 Auth
- `POST /auth/signup` 회원가입
- `POST /auth/login` 로그인
- `POST /auth/logout` 로그아웃
- `POST /auth/token/refresh` JWT 재발급
- `POST /auth/withdraw` 회원 탈퇴
- `POST /auth/email/send` 이메일 인증코드 전송
- `POST /auth/email/verify` 이메일 인증코드 확인
- `POST /auth/phone/send` 휴대폰 인증코드 전송
- `POST /auth/phone/verify` 휴대폰 인증코드 확인
- `POST /auth/password/reset-request` 비밀번호 초기화 요청
- `POST /auth/password/reset` 비밀번호 재설정

### 👤 User
- `GET /users/me` 내 정보 조회
- `PUT /users/profile` 프로필 수정

### 📄 Post
- `GET /posts/{id}` 게시글 상세
- `GET /posts` 게시글 목록
- `POST /posts` 게시글 작성
- `PUT /posts/{id}` 게시글 수정
- `DELETE /posts/{id}` 게시글 삭제
- `POST /posts/posts/{postId}/like` 추천
- `POST /posts/{postId}/report` 신고

### 💬 Comment
- `POST /comments` 댓글 작성
- `GET /comments/post/{postId}` 댓글 목록
- `PUT /comments/{id}` 댓글 수정
- `DELETE /comments/{id}` 댓글 삭제

### 📁 File (S3)
- `POST /files/presign` 업로드용 URL 요청

### 👮 Admin
- `GET /admin/users` 전체 사용자 조회 (관리자 권한)

---

## ✅ 구현 기능 체크리스트

| 기능 | 구현 여부 |
|------|-----------|
| 이메일 인증 | ✅ 완료 |
| 휴대폰 인증 | ✅ 완료 |
| 회원가입 / 로그인 / 로그아웃 | ✅ 완료 |
| JWT Access / Refresh 분리 | ✅ 완료 |
| Refresh Token Redis 저장 | ✅ 완료 |
| Refresh 재발급 시 감사 로그 저장 | ✅ 완료 |
| 게시글 작성 / 수정 / 삭제 (Soft Delete) | ✅ 완료 |
| 댓글 작성 / 수정 / 삭제 | ✅ 완료 |
| 추천 / 중복 추천 방지 | ✅ 완료 |
| 게시글 신고 / 블라인드 처리 | ✅ 완료 |
| S3 Presigned 업로드 | ✅ 완료 |
| 관리자 유저 목록 조회 | ✅ 완료 |
| 닉네임 변경 6개월 제한 | ✅ 완료 |
| 로그인 실패 횟수 누적 / 계정 잠금 | ✅ 완료 |
| WebSocket 채팅 | ✅ 완료 (SimpleBroker 기반) |
| Swagger 문서화 | ✅ 완료 |
| 뉴스 자동 크롤링 + 게시글 등록 | 🔄 개발 중 (6월 배포 목표) |

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

#### 📂 `.env` 파일 구성

- 보안상 이유로 `.env` 파일은 GitHub에 포함되지 않습니다.
- 아래와 같이 `.env.example` 파일을 참고하여 `.env` 파일을 작성하세요:

```bash
cp .env.example .env
```

- 필요한 환경변수 예시 (`.env.example` 참고):

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