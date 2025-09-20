# 🌌 별 헤는 밤 (Byeolnight)

> **운영 환경에서 살아남는 백엔드 구조**를 중심으로 한 우주 테마 커뮤니티 플랫폼

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3.1-blue.svg)](https://reactjs.org/)
[![CI/CD](https://github.com/Iusto/byeolnight/actions/workflows/ci.yml/badge.svg)](https://github.com/Iusto/byeolnight/actions)
[![Security](https://github.com/Iusto/byeolnight/actions/workflows/code-quality.yml/badge.svg)](https://github.com/Iusto/byeolnight/actions)

## 📋 목차
- [⚡ 빠른 시작](#-빠른-시작)
- [🎯 프로젝트 개요](#-프로젝트-개요)
- [🌟 핵심 특징](#-핵심-특징)
- [🏗️ 아키텍처](#️-아키텍처)
- [📚 상세 문서](#-상세-문서)
- [🔧 개발자 정보](#-개발자-정보)

## ⚡ 빠른 시작

### 전제조건
- Java 21+, Node.js 18+, Docker & Docker Compose
- AWS 계정 (S3, CloudFront 설정 필요)
- 외부 API 키 (Gmail, OpenWeatherMap, NASA 등)

### 1분 설치
```bash
# 1. 프로젝트 클론
git clone https://github.com/Iusto/byeolnight.git
cd byeolnight

# 2. Docker로 전체 실행 (가장 간단)
chmod +x deploy.sh && ./deploy.sh

# 3. 접속
open http://localhost
```

### 로컬 개발 환경
```bash
# Config Server 시작
cd config-server && gradlew bootRun

# 백엔드 시작
gradlew bootRun --args='--spring.profiles.active=local'

# 프론트엔드 시작
cd byeolnight-frontend && pnpm install && pnpm run dev
```

### 주요 URL
- **애플리케이션**: http://localhost:5173
- **API 문서**: http://localhost:8080/swagger-ui.html
- **Config Server**: http://localhost:8888

---

## 🎯 프로젝트 개요

**별 헤는 밤**은 단순한 CRUD를 넘어서 **실제 운영 환경에서 필요한 보안, 성능, 확장성**을 고려하여 설계된 우주 테마 커뮤니티 플랫폼입니다.

### 핵심 가치
- 🔐 **운영급 보안**: JWT + Redis 인증, OAuth2 소셜 로그인, 파일 업로드 보안
- ⚡ **성능 최적화**: 데이터베이스 인덱싱, Redis 캐싱, S3 직접 업로드
- 🔄 **실시간 처리**: WebSocket 기반 채팅, 하트비트 + 재연결 로직
- 🧪 **테스트 기반**: JUnit 5 + Mockito 기반 단위/통합 테스트
- 🌌 **외부 API 연동**: 다양한 외부 서비스 통합

---

## 🌟 핵심 특징

### 🔐 운영급 보안
- **JWT + Redis 인증**: 토큰 자동 갱신, 블랙리스트 관리
- **OAuth2 소셜 로그인**: Google, Kakao, Naver 연동
- **파일 업로드 보안**: S3 Presigned URL + CloudFront 조합
- **동시성 제어**: Redisson 분산 락 구현

### 📁 파일 업로드/조회 플로우

#### 📤 **업로드 플로우**
```
1. Client → Server: 파일명/타입 전송
2. Server: 확장자/용량 검증 (10MB, jpg/png/gif 등)
3. Server: S3 Presigned URL 생성 (5분 유효)
4. Client → S3: Presigned URL로 직접 업로드
5. Server: Google Vision API로 이미지 검열
6. 부적절한 이미지 → S3에서 자동 삭제
```

#### 📥 **조회/다운로드 플로우**
```
Client → CloudFront → S3 (OAI 인증)
- S3 직접 접근 완전 차단
- CloudFront CDN을 통한 빠른 이미지 로딩
- Signed URL로 보안 강화 (선택적)
```

#### 🔒 **보안 특징**
- **서버 부하 없음**: 클라이언트가 S3에 직접 업로드
- **자동 검열**: Google Vision API 기반 부적절한 이미지 차단
- **접근 제어**: S3 버킷 비공개 + CloudFront OAI만 허용
- **용량 제한**: 10MB 제한으로 서버 리소스 보호

### ⚡ 성능 최적화
- **데이터베이스 인덱싱**: QueryDSL 기반 최적화된 쿼리
- **S3 직접 업로드**: Presigned URL로 서버 부하 분산
- **CloudFront CDN**: 전 세계 엣지 캐싱으로 빠른 이미지 로딩
- **Redis 캐싱**: 세션 및 데이터 캐싱 레이어
- **테스트 최적화**: 공통 Mock 설정으로 코드 중복 제거

### 🔄 실시간 처리
- **WebSocket 연결**: Native STOMP + 하트비트 + 재연결 로직
- **실시간 채팅**: HttpOnly 쿠키 기반 인증
- **브라우저 알림**: Notification API 활용

### 🌌 외부 API 연동
- **다양한 외부 서비스**: 이메일, 파일 저장, 소셜 로그인 등
- **API 통합**: RESTful API 기반 외부 서비스 연동
- **설정 관리**: Spring Cloud Config로 API 키 중앙 관리

### 🤖 콘텐츠 관리
- **자동화된 콘텐츠**: 스케줄러 기반 콘텐츠 수집
- **데이터 처리**: 외부 API 데이터 가공 및 저장
- **콘텐츠 분류**: 카테고리별 콘텐츠 관리

### 📝 커뮤니티 기능
- **게시글/댓글**: CRUD, 좋아요, 신고, 계층형 댓글
- **Toast UI 에디터**: 색상 변경, 텍스트 정렬, 이미지 검열
- **이미지 업로드**: 파일 선택 + 클립보드 붙여넣기 지원

### 🛍️ 게임화 요소
- **포인트 시스템**: 사용자 활동 기반 포인트 적립
- **아이템 상점**: 다양한 테마 아이콘 구매 시스템
- **성취 시스템**: 활동 기반 성취 달성

> 📚 **상세 기능 설명**: [FEATURES.md](./docs/FEATURES.md) 참고

---

## 🏗️ 아키텍처

### 시스템 구성
```
React 18 (Frontend) ↔️ Spring Boot (Backend) ↔️ MySQL 8 (Database)
                           │
                    ┌─────┼─────┐
                    │           │
            Config Server   Redis Cache
                    │           │
            (Encrypted)   (Auth/Lock)
                    │
                AWS S3 + CloudFront
                (File Storage + CDN)
                    │
            External APIs (8개 서비스)
```

### 핵심 기술 스택
- **Backend**: Java 21 + Spring Boot 3.2.4 + Spring Security + JPA + QueryDSL
- **Frontend**: React 18 + TypeScript + Vite + TailwindCSS
- **Database**: MySQL 8.0 + Redis 7.0 (Redisson)
- **Infrastructure**: Docker + GitHub Actions + AWS S3 + CloudFront
- **External APIs**: Gmail, NewsData, Google Vision, OpenAI, Claude, NASA APIs

> 🔧 **상세 기술 스택**: [TECH-STACK.md](./docs/TECH-STACK.md) 참고

---

## 📊 기술 지표

### 주요 기술 구현
- **데이터베이스**: QueryDSL + JPA 기반 최적화된 쿼리
- **WebSocket**: STOMP 프로토콜 기반 실시간 통신
- **파일 업로드**: S3 Presigned URL 직접 업로드
- **테스트**: JUnit 5 + Mockito 기반 테스트 자동화
- **캐싱**: Redis 기반 세션 및 데이터 캐싱

### 코드 품질
- **테스트**: 단위/통합 테스트 자동화
- **CI/CD**: GitHub Actions 기반 6개 워크플로우
- **보안**: CodeQL 스캔, OWASP 의존성 검사

> 📊 **상세 기술 정보**: [TECH-STACK.md](./docs/TECH-STACK.md) 참고

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
- [⏰ 스케줄러 테스트 가이드](./docs/12_scheduler-testing.md)
- [📊 데이터베이스 설계](./docs/11_database-design.md)

### 🚀 운영 및 개발
- [📦 배포 가이드](./docs/08_deployment.md)
- [📊 데이터베이스 설계](./docs/09_database-design.md)
- [⏰ 스케줄러 테스트 가이드](./docs/10_scheduler-testing.md)
- [🔄 소셜 계정 탈퇴 복구 시스템](docs/11_social-account-recovery.md)

### 🔧 트러블슈팅
- [🌙 JWT Config Server 암호화 문제 해결기](./docs/troubleshooting/jwt-config-server-issue.md)

---

## 🔍 주요 해결 과제

### 실제 개발 과정에서 겪은 기술적 도전
- **파일 업로드 보안**: S3 Presigned URL + CloudFront 조합으로 보안 강화
- **중앙화된 설정 관리**: Spring Cloud Config Server로 환경별 설정 암호화 관리
- **JWT 토큰 관리**: 자동 갱신 로직으로 사용자 경험 개선
- **WebSocket 연결 안정성**: 하트비트 + 재연결 로직으로 안정성 확보
- **브라우저 호환성**: Notification API 타입 체크로 다양한 환경 지원
- **동시성 제어**: Redisson 분산 락으로 동시성 문제 해결
- **소셜 계정 관리**: 탈퇴 후 복구 시스템으로 사용자 편의성 향상
- **이메일 인증**: HTML 템플릿 + 영숫자 코드로 보안성 강화
- **컴포넌트 재사용**: ImageUploader 컴포넌트로 코드 중복 제거
- **에디터 커스터마이징**: Toast UI Editor 툴바 확장
- **패키지 관리**: npm에서 pnpm으로 전환하여 성능 개선
- **Config Server 문제**: JWT 시크릿 암호화 이슈 해결

### 주요 기술적 해결 과제

#### 🛡️ 보안 강화
- **파일 시스템 보안**: S3 + CloudFront 하이브리드 구조로 직접 접근 차단
- **인증 보안**: JWT + Redis 기반 토큰 관리 시스템

#### ⚙️ 운영 효율성
- **설정 관리**: Spring Cloud Config Server로 중앙화된 설정 관리
- **CI/CD 자동화**: GitHub Actions 기반 자동 배포 파이프라인

#### 🚀 성능 개선
- **데이터베이스**: QueryDSL + 인덱스 최적화로 쿼리 성능 향상
- **캐싱**: Redis 기반 다층 캐싱 전략

#### 🔐 사용자 경험
- **이메일 인증**: HTML 템플릿 + 재시도 로직으로 안정성 확보
- **소셜 계정**: 탈퇴 후 복구 시스템으로 사용자 편의성 향상

#### 🛠️ 개발 생산성
- **API 문서화**: Swagger UI 기반 자동 문서 생성
- **컴포넌트 재사용**: 공통 컴포넌트로 코드 중복 제거
- **테스트 최적화**: 공통 Mock 설정으로 테스트 코드 간소화
- **패키지 관리**: pnpm으로 의존성 관리 효율화

---

## 📈 프로젝트 구조

### 아키텍처 구성
- **마이크로서비스**: Config Server + Main Application
- **핵심 도메인**: 인증, 게시글, 댓글, 채팅, 쪽지, 알림, 상점, 인증서, 콘텐츠, 관리자 등
- **API 문서화**: Swagger UI 기반 완전 문서화

### 기술 스택
- **Backend**: Java 21 + Spring Boot 3.2.4 + JPA + QueryDSL + Redis
- **Frontend**: React 18 + TypeScript + Vite + TailwindCSS
- **Database**: MySQL 8.0 + Redis (Redisson)
- **Infrastructure**: Docker + GitHub Actions + AWS S3 + CloudFront

### 보안 및 인증
- **JWT + Redis**: Access/Refresh Token 관리
- **OAuth2**: Google, Kakao, Naver 소셜 로그인
- **파일 보안**: S3 Presigned URL + CloudFront
- **데이터 보존**: 개인정보보호법 준수 데이터 정리

### 외부 연동
- **이메일**: Gmail SMTP
- **파일 저장**: AWS S3 + CloudFront
- **이미지 검열**: Google Vision API
- **소셜 로그인**: Google, Kakao, Naver OAuth2
- **기타**: NewsData, OpenAI, Claude, OpenWeatherMap, NASA APIs

### 자동화 및 모니터링
- **CI/CD**: GitHub Actions 기반 6개 워크플로우
- **스케줄링**: Spring Scheduler 기반 자동 작업
- **테스트**: JUnit 5 + Mockito + TestMockConfig
- **보안 검사**: CodeQL + OWASP 의존성 검사

---

### 기여 가이드
- **코드 스타일**: Google Java Style Guide + Prettier (Frontend)
- **커밋 컨벤션**: Conventional Commits
- **PR 규칙**: 제목 컨벤션, 크기 제한 (500줄), 자동 리뷰어 할당
- **테스트**: 새로운 기능에 대한 테스트 코드 필수

### 라이선스 및 연락처
- **라이선스**: MIT License
- **문의**: GitHub Issues 또는 이메일
- **보안 이슈**: 비공개 이메일로 연락

---

<div align="center">

**🌟 별 헤는 밤에서 우주의 신비를 함께 탐험해보세요! 🌟**

</div>