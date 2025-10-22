# 🌌 별 헤는 밤 (Byeolnight)

> 실시간 채팅, 게시판, 우주 콘텐츠를 즐기는 커뮤니티 플랫폼

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.2.3-green.svg)](https://spring.io/projects/spring-security)
[![JPA](https://img.shields.io/badge/Spring%20Data%20JPA-3.2.4-green.svg)](https://spring.io/projects/spring-data-jpa)
[![QueryDSL](https://img.shields.io/badge/QueryDSL-5.0.0-blue.svg)](http://www.querydsl.com/)
[![React](https://img.shields.io/badge/React-18.3.1-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.2.2-blue.svg)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-5.2.0-purple.svg)](https://vitejs.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-24.0-blue.svg)](https://www.docker.com/)
[![CI/CD](https://github.com/Iusto/byeolnight/actions/workflows/ci.yml/badge.svg)](https://github.com/Iusto/byeolnight/actions)
[![Security](https://github.com/Iusto/byeolnight/actions/workflows/code-quality.yml/badge.svg)](https://github.com/Iusto/byeolnight/actions)

---

## ⚡ Quick Start

Docker만 있으면 3줄로 실행 가능합니다:

```bash
git clone https://github.com/Iusto/byeolnight.git
cd byeolnight
chmod +x deploy.sh && ./deploy.sh
```

**접속**: http://localhost

> ⚠️ **주의**: Mock 데이터로 실행됩니다. 실제 외부 API 연동은 [배포 가이드](./docs/08_deployment.md) 참고

---

## 🎯 주요 기능

### 💬 실시간 소통
- **WebSocket 채팅**: 하트비트 + 자동 재연결로 안정적인 실시간 대화
- **쪽지 시스템**: 1:1 개인 메시지 전송
- **브라우저 알림**: 새 메시지 도착 시 데스크톱 알림

### 📝 커뮤니티
- **게시글/댓글**: 좋아요, 신고, 계층형 댓글 지원
- **Toast UI 에디터**: 색상, 정렬, 이미지 업로드 (클립보드 붙여넣기 지원)
- **이미지 검열**: Google Vision API로 부적절한 이미지 자동 차단

### 🔐 인증/보안
- **소셜 로그인**: Google, Kakao, Naver OAuth2 연동
- **JWT + Redis**: Access/Refresh Token 자동 갱신 + 블랙리스트
- **이메일 인증**: HTML 템플릿 기반 인증 코드

### 🌌 우주 콘텐츠
- **NASA APOD**: 매일 업데이트되는 천문 사진
- **우주 뉴스**: 자동 수집 및 분류
- **날씨 정보**: 실시간 날씨 + 별 관측 가능 여부

---

## 🏗️ 아키텍처

![아키텍처 다이어그램](./docs/다이어그램.png)

### 계층별 구성

**클라이언트 계층:**
- React 18 + TypeScript (SPA)
- WebSocket (STOMP) 실시간 통신

**애플리케이션 계층:**
- Spring Boot 3.2.4 (REST API)
- Spring Security 6.2.3 (JWT + OAuth2)
- Spring Cloud Config Server (중앙 설정 관리)

**데이터 계층:**
- MySQL 8.0 (주 데이터베이스)
- Redis 7.0 (캐시 + 세션 + 분산 락)

**외부 연동:**
- AWS S3 + CloudFront (파일 저장/CDN)
- OAuth2 Provider (Google, Kakao, Naver)
- External APIs (Gmail, Vision, NASA 등)

---

## 🔧 핵심 구현

### ⚡ 성능 최적화
- **S3 Presigned URL**: 클라이언트 직접 업로드로 서버 부하 제로
- **CloudFront CDN**: 전 세계 엣지 캐싱
- **Redis 캐싱**: 세션 + 데이터 캐싱
- **QueryDSL**: 동적 쿼리 최적화
- **커넥션 풀 튜닝**: HikariCP, Redis, HTTP 풀 최적화

### 🔒 보안
- **JWT + Redis**: Token 자동 갱신 + 블랙리스트
- **OAuth2**: 3사 소셜 로그인 통합
- **S3 + CloudFront OAI**: 파일 직접 접근 차단
- **Redisson 분산 락**: 동시성 제어
- **Google Vision API**: 이미지 자동 검열

### 🔄 실시간 처리
- **WebSocket (STOMP)**: 하트비트 + 자동 재연결
- **브라우저 알림**: Notification API 통합

### 🛠️ 개발 인프라
- **Spring Cloud Config**: 중앙 설정 관리 + 암호화
- **GitHub Actions**: 6개 워크플로우 (CI/CD, 보안 스캔)
- **Swagger UI**: 자동 API 문서화
- **Docker Compose**: 원클릭 배포

---

## 📚 문서

### 시작하기
- [⚡ Quick Start](#-quick-start) - 3줄로 실행하기
- [💻 로컬 개발 환경](#-로컬-개발) - 개발 서버 실행 방법
- [📦 배포 가이드](./docs/08_deployment.md) - 프로덕션 배포

### 설계 문서
- [📋 설계 철학 및 DDD](./docs/01_design-philosophy.md)
- [🧱 도메인 모델](./docs/02_domain-model.md)
- [🏛️ 애플리케이션 구조](./docs/03_architecture.md)
- [🎯 핵심 도메인별 구조](./docs/04_core-domains.md)

### 기술 상세
- [🔧 기술 스택 상세](./docs/06_tech-stack.md)
- [📊 성능 최적화 전략](./docs/PERFORMANCE.md)
- [📁 이미지 업로드 파이프라인](./docs/14_image-upload-pipeline.md)
- [🏊 풀 설정 전략](./docs/12_pool-configuration-strategy.md)
- [🧪 테스트 전략](./docs/07_testing.md)
- [📊 데이터베이스 설계](./docs/09_database-design.md)

### 트러블슈팅
- [🌙 JWT Config Server 암호화 문제](./docs/troubleshooting/jwt-config-server-issue.md)
- [🔄 소셜 계정 탈퇴 복구](./docs/11_social-account-recovery.md)
- [⏰ 스케줄러 테스트](./docs/10_scheduler-testing.md)
- [🎤 면접 대비: 기술 선택 이유](./docs/13_interview-tech-decisions.md)

---

## 🛠️ 기술 스택

### Backend
- **Core**: Java 21, Spring Boot 3.2.4, Spring Security 6.2.3
- **Data**: Spring Data JPA, QueryDSL 5.0.0, MySQL 8.0
- **Cache & Session**: Redis 7.0, Redisson (분산 락)
- **Auth**: JWT, OAuth2 (Google, Kakao, Naver)
- **API Docs**: Swagger UI (SpringDoc OpenAPI)
- **Config**: Spring Cloud Config Server (암호화 지원)

### Frontend
- **Core**: React 18.3.1, TypeScript 5.2.2
- **Build**: Vite 5.2.0
- **Styling**: TailwindCSS 3.4.1
- **Editor**: Toast UI Editor 3.2.2
- **HTTP**: Axios
- **WebSocket**: STOMP over SockJS

### Infrastructure
- **Container**: Docker 24.0, Docker Compose
- **CI/CD**: GitHub Actions (6개 워크플로우)
- **Storage**: AWS S3 (Presigned URL)
- **CDN**: AWS CloudFront (OAI)
- **Package Manager**: Gradle 8.7, pnpm 8.15

### External APIs
- **Email**: Gmail SMTP
- **Image Moderation**: Google Vision API
- **AI**: OpenAI GPT-4, Claude 3
- **Space**: NASA APOD API
- **News**: NewsData.io API
- **Weather**: OpenWeatherMap API

> 📚 **상세 정보**: [기술 스택 상세 문서](./docs/06_tech-stack.md)

---

## 💻 로컬 개발

### 전제조건
- Java 21+
- Node.js 18+
- Docker & Docker Compose
- pnpm (권장)

### 개발 서버 실행

```bash
# 1. Config Server 시작
cd config-server
./gradlew bootRun

# 2. 백엔드 시작 (새 터미널)
./gradlew bootRun --args='--spring.profiles.active=local'

# 3. 프론트엔드 시작 (새 터미널)
cd byeolnight-frontend
pnpm install
pnpm run dev
```

### 주요 엔드포인트
- **프론트엔드**: http://localhost:5173
- **백엔드 API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Config Server**: http://localhost:8888

---

## 🤝 기여하기

기여를 환영합니다! 자세한 내용은 [CONTRIBUTING.md](./CONTRIBUTING.md)를 참고해주세요.

### 빠른 시작
```bash
# 1. develop 브랜치에서 시작
git checkout develop
git checkout -b feature/your-feature

# 2. 작업 후 커밋 (컨벤션 준수)
git commit -m "feat(scope): 기능 설명"

# 3. develop에 PR 생성
git push origin feature/your-feature
```

### 필수 규칙
- ✅ **커밋 컨벤션**: `type(scope): subject` 형식 준수
- ✅ **브랜치 전략**: develop → main 순서 엄수
- ✅ **품질 게이트**: 모든 테스트 통과 필수
- ❌ **main 직접 푸시 금지**

---

## 📄 라이선스 & 문의

**라이선스**: MIT License  
**문의**: GitHub Issues  
**보안 이슈**: 비공개 이메일로 연락

---

<div align="center">

**🌟 별 헤는 밤에서 우주의 신비를 함께 탐험해보세요! 🌟**

</div>
