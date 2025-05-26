
# 🌌 별 헤는 밤 (Byeolnight)

> **AI + 실시간 통신 + 인증 자동화**가 통합된 우주 커뮤니티 플랫폼  
> 기술 스택 과시가 아닌, **구조적 문제 해결과 운영 대응력 확보를 목표로 한 실전형 백엔드 프로젝트**

---

## 📌 프로젝트 개요

- **기간**: 2025.05 ~ 진행 중
- **개발 형태**: 100% 개인 설계 및 구현 (백엔드 중심 + 간단한 프론트 연동)
- **목표**: 기존 Outven(도메인 중심 구조)과 Galaxy ML(은하 이미지 분류 AI) 기반의 확장형 커뮤니티 서비스
- **핵심 가치**: 인증/보안 구조 설계, 실시간 통신 시스템 도입, 운영 자동화 및 테스트 기반 개발

---

## ⚙️ 핵심 기술 스택 및 아키텍처

| 범주 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.2, Spring Security, JPA |
| Infra | Docker, GitHub Actions, AWS EC2, Nginx |
| Database | MySQL 8.0, Redis |
| 실시간 통신 | WebSocket + STOMP |
| Testing | JUnit5, Mockito |
| 문서화 | Swagger / OpenAPI 3.0 |
| Frontend | React (REST API 연동) |
| AI/자동화 | Python (뉴스 크롤러), Galaxy ML (CNN 분류 모델) |

> 클라이언트(React) → Nginx Gateway → Spring API 서버 → MySQL/Redis  
> CI/CD: GitHub Actions → Docker 이미지 빌드/배포 → EC2 + Nginx 구성

---

## 🧩 핵심 설계 및 기능

| 영역 | 설명 |
|------|------|
| 인증/보안 | JWT + Redis TTL 기반 세션 관리, Spring Security, BCrypt 암호화, 전역 예외 처리 |
| 게시판 | 게시글/댓글 CRUD, 페이징, Soft Delete, Enum 기반 게시판 분기 처리 |
| 실시간 채팅 | WebSocket + STOMP 프로토콜 기반 채팅방 구조 |
| AI 연동 | Galaxy ML 기반 은하 이미지 자동 분류 및 태깅 예정 |
| 자동화 | Python 크롤러를 활용한 우주 뉴스 자동 게시 시스템 |
| 문서화 | Swagger 3.0 기반 API 문서 자동화, @SecurityRequirement 등 보안 요구 포함 |
| 배포 | Docker + GitHub Actions 기반 자동화 배포 구조 구축 |
| 테스트 | JUnit + Mockito 기반 단위 및 서비스 계층 테스트

---

## 🏗️ 폴더 구조 및 아키텍처

```bash
byeolnight/
├── domain/            # Entity, Repository
├── application/       # Service 계층 (비즈니스 로직)
├── api/               # Controller 계층
├── infrastructure/    # 보안, 설정, 외부 연동 (Redis, Email, AWS 등)
├── dto/               # Request / Response DTO
├── test/              # 테스트 코드
└── util/              # 공통 유틸
```

- 도메인 주도 기반의 계층 분리 (Domain → Application → API → Infra)
- 인증, 보안, 예외, 외부 API 등은 `infrastructure` 레이어에 집중 배치

---

## ⚒️ 기술 선택 근거

| 기술 | 선택 이유 |
|------|-----------|
| JWT + Redis | 무상태 인증(JWT)과 상태 기반 세션 TTL 관리(Redis)를 조합하여 확장성과 보안성을 동시에 확보 |
| WebSocket | 단방향 API를 넘은 실시간 이벤트 시스템 구현 (e.g. 채팅, 알림) |
| Docker + GitHub Actions | 수동 배포 제거, 실전형 CI/CD 구조 체험 |
| Swagger + Security Requirement | 보안 인증 포함 API 문서화, 테스트 기반 검증 효율화 |
| Clean Architecture | 기능 확장 시 의존성 최소화, 테스트/유지보수 편의성 확보 |
| Python 크롤러 | 커뮤니티 콘텐츠 자동화 → 운영 지속성 확보

---

## 🚀 실행 방법

```bash
git clone https://github.com/Iusto/byeolnight.git
cd byeolnight
cp .env.example .env
docker-compose up -d
```

| 구성 요소 | 포트 |
|-----------|------|
| Backend | 8080 |
| Frontend | 3000 |
| Redis | 6379 |
| Swagger 문서 | `https://byeolnight.site/swagger-ui/index.html` |

---

## 🧪 테스트 전략

- `@WebMvcTest`, `@DataJpaTest`, `Mockito`를 통한 계층별 단위 테스트 작성
- 예외/경계 케이스 중심 시나리오 테스트 강화 중
- GitHub Actions Workflow 내 자동 테스트 실행 연동 예정

---

## 🛠 향후 개발 계획

- DTO → Entity 간 변환 최적화 및 Validation 어노테이션 강화
- 게시판 이미지 → AWS S3 업로드 기능 연동
- 관리자 기능 추가: 블랙리스트, 통계, 알림 시스템
- Galaxy ML 모델 결과 기반 자동 태그 생성 로직 통합

---

## 🙋 개발자로서 성장 포인트

- **기술적**: 백엔드 설계, 인증/보안 구조 설계, 실시간 통신 기반 시스템 설계
- **비기술적**: 자동화 중심의 운영 시스템 설계, 기술 선정과 유지보수성 고려 사고방식 확립
- **이전 프로젝트와의 차별점**:
  - `Outven`: 도메인 중심 CRUD → 구조/보안 위주 리팩토링 학습
  - `Galaxy ML`: AI 모델을 실제 서비스 기능으로 연결하는 백엔드 실전 경험

---

> 📌 이 프로젝트는 단순 기능 구현이 아니라, **"운영, 보안, 테스트, 자동화"를 아우르는 실무형 백엔드 역량**을 검증하기 위한 실전 설계 프로젝트입니다.
