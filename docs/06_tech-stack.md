# 06. 기술 스택 상세

> 각 기술 선택의 이유와 실제 적용 방식을 설명합니다.

## 🎯 Backend (핵심 역량)

### Core Framework
- **Java 21 (LTS)** - 최신 LTS 버전으로 성능과 안정성 확보
- **Spring Boot 3.2.4** - 빠른 개발과 운영 환경 최적화
- **Spring Security + JWT** - 무상태 인증으로 확장성과 보안성 동시 확보

### Data Layer
- **Spring Data JPA + QueryDSL 5.0.0** - 복잡한 연관관계와 타입 안전 쿼리 처리
- **MySQL 8.0** - 대용량 데이터 처리와 트랜잭션 안정성
- **Redis 7.0 + Redisson 3.37.0** - 토큰 관리, 분산 락, 캐싱으로 성능 최적화

### External Services
- **AWS S3 (SDK v2.25.17)** - Presigned URL로 서버 부하 없는 파일 업로드
- **Gmail SMTP** - HTML 템플릿 기반 이메일 인증 시스템
- **NewsData.io API** - 우주/과학 뉴스 데이터 수집 및 AI 요약
- **YouTube Data API** - 우주 관련 영상 자동 수집 및 번역
- **OpenAI GPT API** - 뉴스 번역, 요약, AI 분석 및 토론 주제 생성
- **OAuth2 소셜 로그인** - Google, Kakao, Naver 소셜 로그인 연동
- **Google Vision API** - 이미지 업로드 시 유해 콘텐츠 자동 검열

### Real-time & Configuration
- **WebSocket (STOMP)** - 실시간 채팅과 알림을 위한 양방향 통신
- **Spring Cloud Config** - 중앙화된 암호화 설정 관리 시스템

### Documentation & Testing
- **Swagger (OpenAPI 3)** - API 문서 자동화로 프론트엔드 연동 효율화
- **JUnit 5, Mockito** - 단위/통합 테스트로 코드 품질 보장

## 🎨 Frontend (도구 활용)

### Core Framework
- **React 18.3.1** - 컴포넌트 기반 UI 구성과 상태 관리
- **TypeScript 5.8.3** - 타입 안정성으로 런타임 오류 방지
- **Vite 6.3.5** - 빠른 개발 서버와 번들링 최적화

### Styling & UI
- **TailwindCSS 3.4.1** - 빠른 스타일링과 일관된 디자인 시스템
- **Toast UI Editor 3.1.0** - 마크다운 기반 리치 텍스트 에디터
- **React Markdown 10.1.0** - 마크다운 렌더링 및 HTML 변환

### Communication & State
- **Axios 1.7.9** - HTTP 요청 인터셉터와 JWT 자동 갱신
- **Native WebSocket + STOMP 7.1.1** - 순수 WebSocket 기반 실시간 통신
- **React Router DOM 7.6.2** - SPA 라우팅과 페이지 네비게이션

### Utilities
- **Day.js** - 경량 날짜 처리 라이브러리
- **jwt-decode** - JWT 토큰 파싱과 만료 시간 체크

## 🚀 DevOps & Infrastructure

### Containerization
- **Docker & Docker Compose** - 개발/운영 환경 일치와 배포 자동화
- **GitHub Actions** - 코드 푸시 시 자동 빌드/테스트/배포

### Cloud Infrastructure
- **AWS EC2** - 확장 가능한 클라우드 인프라
- **AWS S3** - 정적 파일 저장 및 CDN 역할

## 🔧 기술 선택 이유

### 왜 Spring Boot 3.x를 선택했나?
- **Virtual Threads 지원**: Java 21의 가상 스레드로 동시성 처리 개선
- **Native Image 지원**: GraalVM을 통한 빠른 시작 시간과 메모리 효율성
- **보안 강화**: Spring Security 6.x의 최신 보안 기능 활용

### 왜 Redis를 선택했나?
- **토큰 관리**: JWT Refresh Token의 TTL 관리와 블랙리스트 처리
- **분산 락**: 포인트 적립 등 동시성 문제 해결
- **캐싱**: 자주 조회되는 데이터의 성능 최적화

### 왜 WebSocket(STOMP)을 선택했나?
- **실시간성**: HTTP 폴링 대비 성능 최적화
- **양방향 통신**: 채팅과 알림을 위한 서버 → 클라이언트 푸시
- **연결 관리**: 하트비트와 재연결 로직으로 안정성 확보

### 왜 S3 Presigned URL을 선택했나?
- **서버 부하 분산**: 파일 업로드를 클라이언트에서 직접 처리
- **보안**: 임시 URL로 접근 권한 제어
- **확장성**: 대용량 파일 처리 시 서버 리소스 절약

---

👉 다음 문서: [07. 테스트 전략](./07_testing.md)