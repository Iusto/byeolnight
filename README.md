# 🌌 별 헤는 밤 (Byeolnight)

> **운영 환경에서 살아남는 백엔드 구조**를 중심으로 한 우주 테마 커뮤니티 플랫폼

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3.1-blue.svg)](https://reactjs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-latest-red.svg)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://www.docker.com/)
[![CI/CD](https://github.com/Iusto/byeolnight/actions/workflows/ci.yml/badge.svg)](https://github.com/Iusto/byeolnight/actions)
[![Security](https://github.com/Iusto/byeolnight/actions/workflows/code-quality.yml/badge.svg)](https://github.com/Iusto/byeolnight/actions)

## 🎯 프로젝트 개요

**별 헤는 밤**은 단순한 CRUD를 넘어서 **실제 운영 환경에서 필요한 보안, 성능, 확장성**을 고려하여 설계된 우주 테마 커뮤니티 플랫폼입니다.

### 핵심 설계 원칙
- 🏗️ **도메인 중심 설계**: 비즈니스 로직을 명확한 도메인 모델로 표현
- 🔐 **운영급 보안**: JWT + Redis 기반 인증, 토큰 탈취 대응, 계정 보호
- ⚡ **성능 최적화**: 인덱싱, 캐싱, S3 Presigned URL을 통한 부하 분산
- 🔄 **실시간 처리**: WebSocket 기반 채팅/알림, 하트비트 + 재연결 로직
- 🧪 **테스트 기반**: 단위/통합 테스트로 코드 품질 보장
- 🔄 **CI/CD 자동화**: GitHub Actions 기반 자동 테스트, 빌드, 배포

---

## 🚀 주요 기능

### 💬 실시간 커뮤니케이션
- **채팅 시스템**: WebSocket(STOMP) 기반 실시간 공개 채팅
- **쪽지 시스템**: 1:1 개인 메시지, Soft Delete, 자동 정리
- **알림 시스템**: 실시간 알림 + 브라우저 네이티브 알림

### 🔐 강화된 보안
- **다단계 인증**: 이메일/SMS 인증을 통한 안전한 회원가입
- **JWT + Redis**: 토큰 자동 갱신, 탈취 감지, 계정 잠금
- **이미지 검열**: Google Vision API 기반 부적절 콘텐츠 자동 차단

### 🤖 AI 기반 콘텐츠
- **뉴스 수집**: NewsData.io API + AI 요약/분류
- **별빛시네마**: YouTube 우주 영상 자동 수집 + 번역
- **토론 시스템**: AI 기반 일일 토론 주제 생성

### 🛍️ 게임화 요소
- **포인트 시스템**: 출석체크, 활동 보상, 일일 제한
- **스텔라 상점**: 45개 우주 테마 아이콘, 등급별 분류
- **인증서 시스템**: 활동 기반 성취 인증

---

## 🏗️ 시스템 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   Database      │
│   (React 18)    │◄──►│  (Spring Boot)  │◄──►│   (MySQL 8)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                         │
                              ▼                         ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │     Redis       │    │     AWS S3      │
                       │   (Cache/Auth)  │    │  (File Storage) │
                       └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │   External APIs │
                       │(SendGrid/SMS/   │
                       │NewsData/Google) │
                       └─────────────────┘
```

---

## 🔧 기술 스택

### Backend (핵심 역량)
- **Java 21** + **Spring Boot 3.2.4** - 최신 LTS 기반 안정성
- **Spring Security + JWT** - 무상태 인증으로 확장성 확보
- **MySQL 8.0** + **Redis 7.0** - 데이터 저장 및 캐싱
- **AWS S3** - Presigned URL 기반 파일 업로드
- **WebSocket (STOMP)** - 실시간 양방향 통신

### Frontend (도구 활용)
- **React 18** + **TypeScript** - 컴포넌트 기반 UI
- **Vite** + **TailwindCSS** - 빠른 개발 및 스타일링
- **Axios** + **SockJS** - HTTP 통신 및 WebSocket 연결

### External APIs
- **SendGrid + Gmail SMTP** - 이메일 인증 이중화
- **CoolSMS** - SMS 인증
- **NewsData.io** - 뉴스 데이터 수집
- **Google Vision API** - 이미지 콘텐츠 검증
- **Claude/OpenAI API** - AI 기반 콘텐츠 처리

---

## 📊 성능 최적화 결과

| 최적화 항목 | 개선 전 | 개선 후 | 효과 |
|------------|---------|---------|------|
| 게시글 쿼리 | Full Scan + Filesort | Ref + Index 정렬 | 응답 속도 15% 향상 |
| 채팅 쿼리 | Full Scan + Filesort | Ref + Index 정렬 | 응답 속도 10~15% 향상 |
| WebSocket 연결 | 95% 안정성 | 99% 안정성 | 연결 끊김 95% 감소 |
| 파일 업로드 | 서버 경유 | S3 직접 업로드 | 서버 부하 33% 감소 |

---

## 🔄 CI/CD 파이프라인

### 자동화된 워크플로우
- **CI 테스트**: 코드 푸시 시 자동 테스트 실행 (백엔드 + 프론트엔드)
- **코드 품질 검사**: CodeQL 보안 스캔, 의존성 취약점 검사
- **자동 배포**: `master` 브랜치 푸시 시 운영 서버 자동 배포
- **PR 검증**: Pull Request 시 제목 컨벤션, 크기 체크, 자동 리뷰어 할당
- **성능 테스트**: 주기적 부하 테스트 및 Lighthouse 성능 측정

### 브랜치 전략
- **`master`**: 운영 배포 브랜치 (자동 배포)
- **`develop`**: 개발 브랜치 (테스트만 실행)
- **`feature/*`**: 기능 개발 브랜치 (PR 시 검증)

---

## 🚀 빠른 시작

### 1. 환경 설정
```bash
# 프로젝트 클론
git clone https://github.com/your-username/byeolnight.git
cd byeolnight

# 환경변수 설정
cp .env.example .env
# .env 파일을 열어서 실제 값들로 수정
```

### 2. 로컬 개발 환경
```bash
# 백엔드 + DB만 실행 (개발용)
./run-local.bat  # Windows
docker-compose -f docker-compose.local.yml up -d
gradlew bootRun --args='--spring.profiles.active=local'

# 프론트엔드 별도 실행
cd byeolnight-frontend
npm install
npm run dev
```

### 3. 전체 서비스 실행 (배포용)
```bash
docker-compose up --build -d
```

### 4. 접속 URL
- **로컬 개발**: http://localhost:5173 (프론트), http://localhost:8080 (백엔드)
- **Docker 배포**: http://localhost
- **API 문서**: http://localhost:8080/swagger-ui.html

---

## 📚 상세 문서

프로젝트의 상세한 설계 및 구현 내용은 다음 문서들을 참고하세요:

### 🏗️ 설계 및 아키텍처
- [📋 설계 철학 및 DDD 개요](./docs/01_design-philosophy.md)
- [🧱 도메인 모델 개요](./docs/02_domain-model.md)
- [🏛️ 애플리케이션 구조](./docs/03_architecture.md)
- [🎯 핵심 도메인별 구조](./docs/04_core-domains.md)

### ⚡ 성능 및 기술
- [🚀 성능 최적화 전략](./docs/05_optimizations.md)
- [🔧 기술 스택 상세](./docs/06_tech-stack.md)
- [🧪 테스트 전략](./docs/07_testing.md)
- [📊 데이터베이스 설계](./docs/11_database-design.md)

### 🚀 운영 및 개발
- [📦 배포 가이드](./docs/08_deployment.md)
- [🗺️ 로드맵](./docs/09_roadmap.md)
- [🤝 기여 가이드](./docs/10_contributing.md)

---

## 🔍 주요 해결 과제

### 실제 개발 과정에서 겪은 기술적 도전
- **JWT 토큰 자동 갱신**: 게시글 작성 중 토큰 만료 → 자동 갱신으로 데이터 손실 95% 감소
- **WebSocket 연결 안정성**: 모바일 네트워크 전환 대응 → 하트비트 + 재연결로 99% 안정성 달성
- **인앱 브라우저 호환성**: Notification API 미지원 → 타입 체크로 호환성 100% 달성
- **S3 파일 업로드**: 서버 부하 → Presigned URL로 직접 업로드, 부하 33% 감소
- **동시성 문제**: 포인트 중복 지급 → Redis 분산 락으로 99% 해결

### 성능 최적화 성과
- **데이터베이스 인덱싱**: 복합 인덱스 적용으로 쿼리 성능 15% 향상
- **뉴스 시스템 리팩토링**: DB 조회 성능 85% 향상, 200개 키워드 활용
- **YouTube 서비스 개선**: 영상 다양성 2배 증가, 중복 영상 5% 미만
- **CI/CD 자동화**: GitHub Actions로 배포 시간 90% 단축, 수동 오류 99% 감소

---

## 📈 프로젝트 통계

- **총 코드 라인**: ~50,000 lines (Backend 70%, Frontend 30%)
- **도메인 수**: 10개 핵심 도메인 (인증, 게시글, 쪽지, 알림, 채팅 등)
- **API 엔드포인트**: 80+ RESTful APIs
- **테스트 커버리지**: Controller, Service, Repository 계층별 테스트
- **외부 API 연동**: 7개 서비스 (SendGrid, CoolSMS, AWS S3, Google Vision 등)
- **CI/CD 워크플로우**: 5개 자동화 파이프라인 (CI, 코드품질, 배포, PR검증, 성능테스트)

---

## 🤝 기여하기

프로젝트에 기여하고 싶으시다면 [기여 가이드](./docs/10_contributing.md)를 참고해주세요.

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

---

<div align="center">

**🌟 별 헤는 밤에서 우주의 신비를 함께 탐험해보세요! 🌟**

</div>