# 🌌 별 헤는 밤 (Byeolnight)

> **AI + 실시간 통신 + 인증 자동화**가 통합된 우주 커뮤니티 플랫폼  
> 기술 스택 과시가 아닌, **구조적 문제 해결과 운영 환경 대응에 집중한 백엔드 중심 설계**

---

## 1. 프로젝트 개요

| 항목 | 설명 |
|------|------|
| 개발 기간 | 2025.05 ~ 진행 중 |
| 개발 형태 | 100% 개인 설계 및 구현 (백엔드 중심 + 프론트 간단 연동) |
| 핵심 목적 | 기존 Outven(도메인 설계 중심) + Galaxy ML(AI 이미지 분류) 기반 확장형 프로젝트 |
| 주요 특징 | JWT 인증, Redis 세션, WebSocket 실시간 채팅, 자동 뉴스 게시, 클린 아키텍처, CI/CD |

---

## 2. 기술 스택 및 아키텍처

| 영역 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.2, Spring Security, JPA, WebSocket |
| Infra | Docker, GitHub Actions, AWS EC2, Nginx |
| Database | MySQL 8.0, Redis |
| Testing | JUnit5, Mockito |
| Documentation | Swagger / OpenAPI 3.0 |
| Frontend | React (REST API 연동) |
| 기타 | Python (뉴스 크롤러) |

**구성 아키텍처:**  
클라이언트(React) → Nginx API Gateway → Spring API 서버 → MySQL/Redis  
**배포 흐름:** GitHub Actions → Docker 이미지 → EC2 + Nginx + HTTPS 구성

---

## 3. 주요 기능 요약

| 기능군 | 설명 |
|--------|------|
| 인증/보안 | JWT + Redis TTL 세션, BCrypt 암호화, Global 예외 처리 |
| 게시판 기능 | 게시글/댓글 CRUD, 페이지네이션, Soft Delete 구조 |
| 실시간 채팅 | WebSocket + STOMP 기반 채팅방 |
| AI 연동 | 은하 이미지 자동 분류 (Galaxy ML 연계 예정), 뉴스 자동 게시 |
| 문서화 | Swagger 기반 API 자동 문서화 |
| 배포 | GitHub Actions + Docker 기반 자동화 배포 |
| 테스트 | JUnit 단위 테스트, Mockito 기반 서비스 계층 테스트 |

---

## 4. 폴더 및 계층 구조

```bash
byeolnight/
├── domain/            # Entity, Repository
├── application/       # 서비스 계층 (비즈니스 로직)
├── api/               # Controller 계층
├── infrastructure/    # Config, Security, Redis 등 외부 연동
├── dto/               # Request / Response DTO
├── test/              # JUnit, Mockito 테스트 코드
└── util/              # 공통 유틸리티
```

→ **도메인-애플리케이션-인프라-API** 계층 분리를 통해 의존성 방향 명확화 및 유지보수성 향상

---

## 5. 기술 선택 이유와 설계 철학

| 선택 항목 | 이유 |
|-----------|------|
| **클린 아키텍처 적용** | 기능 확장 시 영향 범위 최소화, 테스트 용이성 확보 |
| **JWT + Redis 세션** | 무상태 인증과 세션 TTL 관리 간 균형 확보 |
| **Docker 기반 배포** | 실서비스와 유사한 환경에서 운영 검증 가능 |
| **GitHub Actions 도입** | 수동 반복 작업 제거, 실전형 DevOps 경험 확보 |
| **React 연동** | API 소비 관점에서 설계 적합성 직접 검증 |
| **크롤러 자동화** | 콘텐츠 지속성을 확보하는 운영 자동화 기능 반영 |
| **WebSocket 도입** | 단순 기능 구현을 넘은 실시간 통신의 실제 적용 사례 확보 |

> 단순히 최신 기술을 나열하는 것이 아닌, **운영/테스트/유지보수**를 고려한 설계 철학 기반 기술 선정

---

## 6. 실행 방법

```bash
git clone https://github.com/Iusto/byeolnight.git
cd byeolnight
cp .env.example .env
docker-compose up -d
```

| 항목 | 값 |
|------|------|
| 백엔드 포트 | 8080 (Spring Boot) |
| 프론트 포트 | 3000 (React) |
| Redis 포트 | 6379 |
| API 문서 | `https://byeolnight.site/swagger-ui/index.html` |

---

## 7. 향후 개선 예정 사항

- DTO → Entity 간 매핑 구조 보완 및 Validation 강화
- 게시판 이미지 업로드 기능 → AWS S3 연동 예정
- 관리자 기능 + 알림 시스템 도입
- Swagger 문서 자동화 고도화 (테스트 코드 연동)
- Galaxy ML 결과 기반 **태그 자동 생성 로직 통합**

---

## 8. 개발자로서 성장 포인트

- **기술 포인트**: 아키텍처 설계, 실시간 시스템 구성, 인증/보안 구조 직접 구현
- **비기술 포인트**: 자동화된 운영 구조 구성 경험, 콘텐츠 유지 시스템 설계 경험
- **이전 프로젝트와의 차별성**:
  - Outven: DB 중심 구조 설계 및 서비스 분리 개념 이해
  - Galaxy ML: AI 모델의 실전 활용 가능성을 백엔드에 직접 녹여냄
- **지향점**: 단순 구현을 넘은 **서비스 전체 흐름을 아우르는 통합적 사고와 실전 대응력**

---

> 📌 이 프로젝트는 단지 기능 구현이 아니라, **서비스 운영과 유지보수, 테스트 자동화, 인프라 구성**까지 전체 개발 생명주기를 스스로 구축해 본 실전형 프로젝트입니다.