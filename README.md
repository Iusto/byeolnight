# 🌌 별 헤는 밤 (Byeolnight) - 우주 감성 커뮤니티 서비스

> “기능만 되는 백엔드가 아닌, 운영 환경에서 살아남는 구조를 설계합니다.”

---

## 👤 나는 어떤 백엔드 개발자인가?

- 문제를 **보안과 운영 관점으로 설계**하고,
- 에러 처리/토큰 관리/로그 설계까지 고려하는 **시스템 중심 개발자**입니다.
- 클린 아키텍처, 인증 로직, 실시간 연동, 테스트 설계까지 하나하나 **직접 구현**하며 학습했습니다.

---


## 🔧 기술 스택 및 선택 이유

| 분류 | 기술 | 선택 이유 |
|------|------|-----------|
| **Language** | **Java 21** | 최신 LTS 버전으로, `Record`, `Pattern Matching` 등 최신 문법을 활용해 DTO와 응답 객체를 간결하게 구성. Spring Boot 3.x와도 안정적으로 연동되며, 실무에서의 유지보수성과 호환성 모두 고려. |
| **Framework** | **Spring Boot 3.2** | 인증, 예외 처리, 트랜잭션, Swagger, WebSocket까지 모두 자체적으로 통합 가능하여 실무 백엔드 서비스 구성에 최적화. Spring Security, Validation, AOP 기반의 구조적 설계를 빠르게 구현 가능. |
| **ORM** | **JPA (Spring Data JPA)** | 게시글-댓글, 유저-추천, 유저-신고 등 복잡한 관계를 명확하게 표현 가능하며, Repository 추상화를 통해 테스트/유지보수가 용이. 연관관계 매핑 및 Lazy/Eager 전략 등을 직접 다루며 ORM 기반 도메인 설계 경험 확보. |
| **DB** | **MySQL** | 정합성과 무결성이 중요한 커뮤니티 서비스에 적합한 관계형 DB. 트랜잭션 처리와 인덱스 설계, FK 제약 등을 직접 다루며 실무 수준의 설계 경험을 반영. |
| **Infra** | **Redis**, **AWS S3** | Redis는 인증코드, 토큰 블랙리스트, 로그인 실패 횟수 등 휘발성 데이터 캐시에 사용. S3는 Presigned URL 기반의 이미지 업로드로 서버 부하 분산 및 보안 관리 구조 구현. |
| **Test** | **JUnit5**, **Mockito** | 단위 테스트 기반으로 `@WebMvcTest`와 `MockMvc` 조합을 사용하여 컨트롤러 및 인증 예외 상황 테스트 수행. 인증 실패, 권한 부족 등 실무 이슈 중심 테스트 설계. |
| **API 문서** | **Swagger (springdoc-openapi)** | 프론트엔드와의 연동 정확도 향상을 위해 실시간 API 명세 자동화. `@SecurityScheme`, `@Parameter` 등 상세 설정을 통해 인증 API 및 관리자 권한 API를 명확히 구분. |
| **CI/CD** | **GitHub Actions + Docker** | 코드 Push → 테스트 → Docker 이미지 → EC2 배포까지 자동화된 파이프라인 구축. 운영 환경에서 반복 배포 시 발생 가능한 실수 최소화 및 안정성 확보. |
| **Frontend** | **React + TailwindCSS** | 백엔드 API 소비자 관점에서의 흐름 검증 목적. 로그인, 게시글, 채팅, 이미지 업로드 등 실제 사용자 흐름을 테스트하며 API 설계에 대한 피드백 루프 확보. Tailwind를 통해 감성적 우주 커뮤니티 컨셉을 빠르게 UI로 구현. |


| 분류 | 기술 | 선택 이유 |
|------|------|-----------|
| Language | Java 21 | LTS 버전 + `Record`, `Pattern Matching` 등 최신 문법 사용 |
| Framework | Spring Boot 3.2 | 인증, 예외, 트랜잭션, Swagger까지 실무 수준 제공 |
| ORM | JPA | 추상화된 Repository 패턴 기반 설계 |
| DB | MySQL | 관계형 구조로 데이터 무결성과 정합성 유지 |
| Infra | Redis, AWS S3 | 인증코드 캐싱, 토큰 저장, 이미지 저장 분리 구조 |
| Test | JUnit5, Mockito | 단위 테스트 / 인증 실패 테스트 커버 |
| API 문서 | Swagger | 실시간 API 명세 자동 생성 |
| CI/CD | GitHub Actions + Docker | 코드 Push → 자동 배포 구조 구성 |
| Frontend | React + Tailwind (연동용) | 백엔드 API 소비자 입장에서 설계 검증 목적 |

---

## 🧱 아키텍처 구성

- 도메인 중심 구조 분리: `domain`, `dto`, `service`, `controller`, `infra`
- 클린 아키텍처 흐름 정립: **의존성 흐름 = Controller → Service → Repository**
- JWT 기반 인증/인가 시스템
- 인증 실패 5/10/15회 단계별 대응 (잠금/경고/IP 차단)
- Presigned URL 기반 이미지 업로드 (S3)
- Redis 기반 인증코드 + 토큰 + 블랙리스트 저장
- WebSocket 실시간 채팅 (STOMP + SockJS)

---

## 🔐 인증 및 보안 설계


### ✅ 왜 JWT를 선택했는가?

`별 헤는 밤`은 로그인/회원가입, 게시글 작성/추천/신고, 실시간 채팅, 관리자 기능 등에서 사용자 인증이 필요한 구조입니다. 프론트엔드는 React 기반 SPA 구조이며, REST API와의 통신에서 **반복적으로 인증이 필요한 요청**이 많고, 서버는 Stateless하게 확장 가능한 구조가 필요했습니다.

#### 📌 JWT가 적합했던 이유

| 고려 요소 | 설명 |
|-----------|------|
| **무상태 인증 구조** | 서버가 인증 상태를 저장하지 않고, 클라이언트가 토큰을 보관하는 구조가 RESTful API와 자연스럽게 맞물림 |
| **확장성** | EC2, Docker 등 수평 확장을 고려할 때, 세션 기반은 Redis 공유 등 복잡한 동기화가 필요하나 JWT는 서버 확장에 유리 |
| **프론트엔드 연동 용이성** | React와 통신 시 쿠키 기반 세션은 CORS 이슈 발생 가능. JWT는 Authorization 헤더로 명확하게 전달되어 구현 단순 |
| **로그아웃/폐기 처리** | 일반적으로 JWT는 로그아웃 시 토큰 폐기가 어려운 단점이 있으나, Redis 기반 블랙리스트 구조로 이를 해결 |
| **보안 설계** | 토큰 유효시간 설정 + Refresh 토큰 분리 + Redis TTL + 로그인 실패 누적 잠금 정책을 통해 JWT의 단점을 구조적으로 보완 |
| **구현 난이도와 유지보수** | 세션 동기화, 쿠키 관리, OAuth2 플로우 등보다 상대적으로 단순하면서도 실무에서 많이 쓰이는 구조로, 구현과 디버깅이 명확함 |

#### 💡 결론

> 쿠키/세션 기반 인증은 서버 상태 공유 및 유지에 따른 복잡도가 높고, OAuth2는 구조가 과도하게 복잡했습니다. 반면, JWT는 무상태 구조로 반복적인 API 호출과 프론트엔드 연동에 적합하며, Redis 기반의 보완 설계를 통해 단점을 해소할 수 있었기 때문에 `별 헤는 밤` 프로젝트에 가장 현실적이고 확장 가능한 선택이라고 판단했습니다.


- ✅ 이메일 인증 (Gmail SMTP)
- ✅ 전화번호 인증 (CoolSMS)
- ✅ JWT Access/Refresh 구조 + Redis 저장소 + 블랙리스트
- ✅ 로그인 실패 → 5회 경고 / 10회 계정잠금 / 15회 IP 차단
- ✅ 비밀번호 재확인 후 프로필 수정
- ✅ 관리자에 의한 사용자 제재, 로그 기록
- ✅ 전역 예외 처리 핸들러 적용 (`@ControllerAdvice`)
- ✅ Swagger에 관리자 Role 전용 인증 설정 적용

---

## 🧪 테스트 전략

| 대상 | 방식 |
|------|------|
| 유저 서비스 | 단위 테스트 (비밀번호 검증, 상태 변경, 로그인 실패 누적 등) |
| 게시글 서비스 | 단위 + 예외 테스트 (삭제, 추천, 블라인드) |
| 파일 업로드 | Mock S3Service 테스트 |
| 인증 API | `MockMvc` + `@WebMvcTest` 컨트롤러 테스트 |
| 통합 테스트 | Docker 기반 통합환경에서 수동 검증 가능 |

---

## 📡 실시간 채팅 설계

- `WebSocketConfig` + `StompEndpointRegistry` 구성
- `/chat.send` → 공용 채팅 브로드캐스트
- 로그인 여부에 따라 입력창 제한 / 메시지 읽기 허용
- 추후 Redis Pub/Sub 확장 고려 설계

---

## 🗂 파일 업로드 (S3)

- `/api/files/presign` 호출 → S3 Presigned URL 발급
- 클라이언트가 직접 업로드 → 서버 부하 분산
- S3 저장 경로: `/userId/uuid-filename.jpg`

---

## 👮 관리자 기능

| 기능 | 설명 |
|------|------|
| 사용자 목록 조회 | `/api/admin/users` |
| 계정 상태 변경 | `/api/admin/users/{id}/status` |
| 계정 잠금 | `/api/admin/users/{id}/lock` |
| 강제 탈퇴 | `/api/admin/users/{id}` |
| 로그인 기록 열람 | `AuditLoginLog` 저장소 (Swagger 미기재, DB 직접 확인) |
| IP 차단 처리 | `/blocked:ip:{IP}` 키 → Redis 등록 후 차단 |

---

## 🔄 API 명세 요약 (Swagger 연동 완료)

### 🔑 Auth
- `POST /auth/signup` 회원가입
- `POST /auth/login` 로그인
- `POST /auth/logout` 로그아웃
- `POST /auth/email/send` /verify
- `POST /auth/phone/send` /verify
- `POST /auth/token/refresh`
- `POST /auth/password/reset-request`
- `DELETE /auth/withdraw`

### 👤 User
- `GET /users/me`
- `PUT /users/profile`

### 📄 Post
- `GET /public/posts`
- `POST /member/posts`
- `PUT /member/posts/{id}`
- `POST /posts/{postId}/like`
- `POST /posts/{postId}/report`

### 💬 Comment
- `POST /comments`
- `GET /comments/post/{postId}`

### 👮 Admin
- `GET /admin/users`
- `PATCH /admin/users/{id}/lock`
- `PATCH /admin/users/{id}/status`
- `DELETE /admin/users/{id}`
- ⛔ `관리자 IP 차단` API는 현재 미공개, 내부 Redis 처리 방식

---

## 🧠 회고

- 단순 구현보다, "운영 가능한 구조"를 설계하는 것에 초점을 맞췄습니다.
- 인증 로직, 보안 정책, 파일 저장, 실시간 채팅 등 실무 백엔드의 핵심 요소를 모두 구현했습니다.
- 이 경험을 통해 **"일을 설계할 줄 아는 신입 개발자"**로 성장했다고 생각합니다.

---

## 🐳 Docker 실행 가이드

```bash
# 프로젝트 루트에서 실행
docker-compose up --build
```

| 구성 | 포트 |
|------|------|
| Spring Boot | 8080 |
| MySQL       | 3306 |
| Redis       | 6379 |

---

## 📁 .env 설정 예시

```env
DB_URL=jdbc:mysql://mysql:3306/byeolnight
MAIL_USERNAME=...
REDIS_HOST=redis
CLOUD_AWS_ACCESS_KEY=...
COOLSMS_API_KEY=...
```

---

## ✅ 향후 계획

- [ ] 뉴스 자동 분류 및 게시 기능 (7월 예정)
- [ ] 관리자 로그 열람 Swagger 명세화
- [ ] 비밀번호 변경 기능 추가 예정
- [ ] 프론트엔드 쪽지 기능 논의 중

---

👨‍💻 감사합니다. "실제로 배포하고 운영할 수 있는 백엔드"를 추구하는 개발자입니다.