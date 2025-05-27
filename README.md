# 🌌 별 헤는 밤 (Byeolnight)

> **AI, 실시간 통신, 인증 자동화**를 통합한 우주 커뮤니티 백엔드 시스템
> 단순 기능 구현이 아닌, 운영환경·보안·테스트·CI/CD까지 고려한 실무형 프로젝트

---

## 📌 프로젝트 개요

* **기간**: 2025.05 \~ 진행 중
* **개발 형태**: 100% 개인 설계 및 구현 (백엔드 중심)
* **목표**: 도메인 구조 기반의 커뮤니티 시스템에 AI 모델과 자동화 기능을 통합하여, 운영 가능한 백엔드 실전 시스템 구현
* **특징**:

  * 클린 아키텍처 기반 구조 설계
  * JWT + Redis 기반 인증 보안 체계
  * 실시간 채팅 및 자동화 게시 기능 탑재
  * GitHub Actions + Docker 기반 자동 배포

---

## ⚙️ 기술 스택 요약

| 범주       | 기술                              | 선택 이유 (자세한 근거는 하단 참조)                   |
| -------- | ------------------------------- | --------------------------------------- |
| Backend  | Java 21, Spring Boot 3.2        | 최신 Spring 생태계 적용 + Virtual Thread 기반 확보 |
| 보안/인증    | Spring Security, JWT, Redis     | 무상태 인증과 TTL 기반 세션 관리의 조합                |
| Infra    | Docker, GitHub Actions, AWS EC2 | 자동화 배포 및 운영환경 대응                        |
| Database | MySQL 8.0, Redis                | 확장성 + 세션 처리 최적화                         |
| 실시간      | WebSocket + STOMP               | 실시간 채팅 및 이벤트 처리 구조 구성                   |
| 문서화      | Swagger / OpenAPI 3.0           | 인증 포함 API 문서화 및 테스트 지원                  |
| 프론트      | React (REST 연동)                 | 클라이언트는 API 소비 전용 구조                     |
| 자동화/AI   | Python, Selenium, Galaxy ML     | 뉴스 크롤러 + CNN 분류 모델 연동                   |

---

## 🧩 핵심 기능 및 설계

| 영역    | 설계/기능                                                    |
| ----- | -------------------------------------------------------- |
| 인증/보안 | JWT + Redis TTL 기반 세션 구조, Spring Security 적용, BCrypt 암호화 |
| 게시판   | 게시글/댓글 CRUD, 페이징, Soft Delete, 게시판 Enum 기반 분기 처리         |
| 실시간   | WebSocket + STOMP 채팅방 구조, 비동기 이벤트 처리                     |
| 자동화   | Python 기반 뉴스 크롤러, Galaxy ML 연동 은하 이미지 자동 분류/업로드 예정       |
| 문서화   | Swagger 3.0, @SecurityRequirement 포함 API 문서 자동화          |
| 배포    | GitHub Actions → Docker 이미지 빌드 → EC2 + Nginx 배포 파이프라인 구축 |
| 테스트   | JUnit + Mockito 기반 단위/통합 테스트, 시나리오 자동화 진행 중              |

---

## 🏗️ 프로젝트 아키텍처

```bash
byeolnight/
├── domain/            # Entity, Repository (도메인 중심 구조)
├── application/       # Service 계층 (비즈니스 로직)
├── api/               # Controller 계층 (HTTP 진입점)
├── infrastructure/    # 보안, 설정, 외부 연동 (Redis, Email, AWS 등)
├── dto/               # Request / Response DTO
├── util/              # 공통 유틸리티
├── test/              # 테스트 코드
```

* **의존성 흐름**: Domain → Application → API → Infra
* 외부 시스템과의 의존은 모두 `infrastructure` 계층에 격리

---

## ⚒️ 기술 선택 근거 (요약)

* **Spring Boot 3.2 + Java 21**: Jakarta EE 10 대응, 최신 보안 및 Virtual Thread 기반 확보
* **JPA + QueryDSL**: 도메인 중심 설계 + 복잡한 검색 조건의 타입 안정성 확보
* **JWT + Redis**: 무상태 인증 + 세션 TTL 관리로 보안성과 확장성 동시 확보
* **Swagger + Postman**: 인증 포함 테스트 자동화 및 문서화 통합
* **Docker + GitHub Actions**: 수동 배포 제거, 실전형 CI/CD 체험
* **WebSocket**: REST 한계를 넘는 실시간 구조 구현 (채팅/알림)
* **클린 아키텍처**: 기능 확장 시 의존성 최소화 및 테스트 용이성 확보

> 전체 선택 근거 및 대안 비교는 [기술 선택 이유 문서](링크) 참고

---

## 🚀 실행 방법

```bash
git clone https://github.com/Iusto/byeolnight.git
cd byeolnight
docker-compose up -d
```

| 구성 요소    | 포트                                                                                             |
| -------- | ---------------------------------------------------------------------------------------------- |
| Backend  | 8080                                                                                           |
| Frontend | 3000                                                                                           |
| Redis    | 6379                                                                                           |
| Swagger  | [https://byeolnight.site/swagger-ui/index.html](https://byeolnight.site/swagger-ui/index.html) |

---

## 🧪 테스트 전략

* `@WebMvcTest`, `@DataJpaTest`, `@MockBean`을 활용한 계층별 단위 테스트 설계
* 만료 토큰, 잘못된 시그니처, 로그인 세션 만료 등 시나리오 커버 테스트
* CI 파이프라인 내 테스트 자동 실행 연동 예정

---

## 📈 향후 개발 계획

* 게시판 이미지 AWS S3 연동 및 정적 리소스 관리
* 관리자 기능 추가 (블랙리스트, 게시글 통계, 신고 처리 등)
* Galaxy ML 결과 기반 자동 태그 분류 및 사용자 추천 기능
* React 기반 프론트엔드 고도화 (현재는 연동 테스트 수준)

---

## 🙋 성장 포인트 및 회고

* **기술적 성장**: JWT 인증 구조 설계, Redis TTL 세션 제어, 실시간 메시징 및 자동화 크롤링 설계 경험 확보
* **비기술적 성장**: 배포 자동화, 예외 처리 통합, 운영환경 대응 설계에 대한 전방위적 고민
* **회고**:

  * Outven에서 얻은 구조 리팩토링의 교훈 → 실제 운영 가능한 설계로 확장
  * Galaxy ML에서의 AI 실험 → 자동 콘텐츠 생성 흐름으로 발전
  * 단순 구현보다 문제 해결과 구조화 중심 사고가 더욱 중요함을 체감

---

> 이 프로젝트는 단순한 기능 구현이 아니라, **서비스를 운영하는 데 필요한 구조, 인증, 보안, 배포, 테스트**를 전부 경험하고 증명하는 것을 목표로 한 실전 설계형 프로젝트입니다.
