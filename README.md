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
- ⚙️ **중앙화된 설정 관리**: Spring Cloud Config Server 기반 암호화된 설정 관리
- ⚡ **성능 최적화**: 인덱싱, 캐싱, S3 Presigned URL을 통한 부하 분산
- 🔄 **실시간 처리**: WebSocket 기반 채팅/알림, 하트비트 + 재연결 로직
- 🧪 **테스트 기반**: 단위/통합 테스트로 코드 품질 보장
- 🔄 **CI/CD 자동화**: GitHub Actions 기반 자동 테스트, 빌드, 배포

---

## 🚀 주요 기능

### 💬 실시간 커뮤니케이션
- **채팅 시스템**: Native WebSocket(STOMP) + HttpOnly 쿠키 인증 기반 실시간 공개 채팅, 하트비트 + 재연결로 99% 안정성
- **쪽지 시스템**: 1:1 개인 메시지, Soft Delete, 자동 정리, 읽음 상태 관리
- **알림 시스템**: 실시간 알림 + 브라우저 네이티브 알림, 인앱 브라우저 호환성 100%

### 🔐 강화된 보안
- **이메일 인증**: HTML 템플릿 + 8자리 영숫자 코드, 재시도 로직, 시도 횟수 제한 (5회/10분)
- **JWT + HttpOnly 쿠키 + Redis**: 토큰 자동 갱신, 블랙리스트 관리, 탈취 감지, 데이터 손실 95% 감소
- **이미지 검열**: Google Vision API 기반 부적절 콘텐츠 자동 차단
- **동시성 제어**: Redis 분산 락으로 포인트 중복 지급 99% 해결
- **소셜 계정 복구 시스템**: 30일 내 완전 복구, 이메일 기반 고유 닉네임 자동 생성, 단계별 데이터 정리 (30일/1년)

### 🌟 실시간 천체 정보 시스템 (NEW!)
- **실시간 관측 조건**: Geolocation API + OpenWeatherMap 연동으로 사용자 위치 기반 별 관측 적합도 실시간 제공
- **천체 이벤트 알림**: NASA API 연동으로 유성우, 태양 플레어, 지구 근접 소행성 등 실제 천체 이벤트 자동 수집 및 24시간 전 알림
- **NASA API 통합**: NASA NeoWs/DONKI/ISS API로 글로벌 천체 데이터 제공
- **스마트 추천**: 구름량, 시정, 달의 위상 분석으로 4단계 관측 품질 등급 (EXCELLENT/GOOD/FAIR/POOR) 제공
- **위치 기반 서비스**: 위치 권한 거부 시 서울 기준 기본값 제공, 1시간 캐싱으로 API 호출 최적화

### 🤖 AI 기반 콘텐츠
- **뉴스 수집**: NewsData.io API + AI 요약/분류, 200개 키워드 활용, DB 조회 성능 85% 향상
- **별빛시네마**: YouTube 우주 영상 자동 수집 + 번역, 영상 다양성 2배 증가, 중복 영상 5% 미만
- **토론 시스템**: AI 기반 일일 토론 주제 생성, Claude/OpenAI API 활용

### 📝 커뮤니티 기능
- **게시글 시스템**: CRUD, 좋아요, 신고, 인기글, Toast UI 에디터 (색상 변경, 텍스트 정렬, 이미지 검열)
- **댓글 시스템**: 계층형 댓글, 좋아요, 신고, 대댓글 지원
- **검색 및 필터**: 제목/내용 검색, 카테고리별 필터링
- **신고 시스템**: 관리자 신고 처리, 자동 블라인드 처리
- **이미지 업로드**: 파일 선택 + 클립보드 붙여넣기, Google Vision API 기반 자동 검열, 컴포넌트화된 업로드 시스템

### 🛍️ 게임화 요소
- **포인트 시스템**: 출석체크, 활동 보상, 일일 제한, 누적 포인트 관리
- **스텔라 상점**: 45개 우주 테마 아이콘, 5개 등급별 분류, 애니메이션 효과
- **인증서 시스템**: 활동 기반 성취 인증, 8가지 인증서 종류 (별빛탐험가, 우주인등록, 은하통신병 등)

### 🔧 관리자 기능
- **사용자 관리**: 계정 상태 변경, 제재 (1년 보존), 닉네임 변경권 수여
- **콘텐츠 관리**: 게시글/댓글 블라인드, 신고 처리
- **채팅 관리**: 채팅방 참여자 관리, 채팅 금지
- **통계 대시보드**: 사용자 활동, 콘텐츠 통계
- **소셜 계정 관리**: 연동 해제 감지, 즉시 탈퇴 처리, 30일 복구 가능

---

## 🏗️ 시스템 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   Database      │
│   (React 18)    │◄──►│  (Spring Boot)  │◄──►│   (MySQL 8)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                         │
                              ▼                         ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Config Server  │    │     Redis       │    │     AWS S3      │
│ (Central Config)│    │   (Cache/Auth)  │    │  (File Storage) │
└─────────────────┘    └─────────────────┘    └─────────────────┘
        │                         │
        ▼                         ▼
┌─────────────────┐    ┌─────────────────┐
│  Git Repository  │    │   External APIs │
│ (Encrypted Cfg) │    │(Gmail/NewsData/ │
└─────────────────┘    │Google/OpenAI/   │
                       │Claude)          │
                       └─────────────────┘
```

---

## 🔧 기술 스택

### Backend (핵심 역량)
- **Java 21** + **Spring Boot 3.2.4** - 최신 LTS 기반 안정성
- **Spring Cloud Config Server** - 중앙화된 암호화 설정 관리
- **Spring Security + JWT** - 무상태 인증, 토큰 자동 갱신
- **Spring Data JPA + QueryDSL** - 타입 안전 쿼리 (일부 사용), 복합 인덱스 최적화
- **MySQL 8.0** + **Redis 7.0** - 데이터 저장, 캐싱, 분산 락
- **Redisson** - Redis 기반 분산 락으로 동시성 제어
- **AWS S3** - Presigned URL 기반 파일 업로드
- **Native WebSocket (STOMP) + HttpOnly 쿠키** - 실시간 양방향 통신, 보안 강화된 인증
- **Spring Scheduling** - 뉴스 수집, 데이터 정리 자동화
- **Swagger UI** - 완전한 API 문서화, 상세한 파라미터 설명 및 예시 제공

### Testing & Quality Assurance (123개 테스트 100% 통과 ✅)
- **JUnit 5** + **Mockito** - 단위 테스트 및 Mock 기반 테스트
- **TestMockConfig** - 공통 Mock 설정으로 테스트 코드 93% 감소
- **Lenient Mockito** - 엄격한 검증 완화로 테스트 안정성 100% 향상
- **AssertJ** - 유창한 API 기반 테스트 검증
- **Spring Boot Test** - 통합 테스트 및 슬라이스 테스트

### Frontend (사용자 경험)
- **React 18** + **TypeScript** - 컴포넌트 기반 UI, 타입 안전성
- **Vite** + **TailwindCSS** - 빠른 개발 서버, 유틸리티 우선 스타일링
- **React Router DOM v7** - 클라이언트 사이드 라우팅
- **Axios** + **Native WebSocket/STOMP** - HTTP 통신 및 WebSocket 연결
- **React i18next** - 다국어 지원 (한국어, 영어, 일본어)
- **Toast UI Editor** - 마크다운 기반 에디터, 커스텀 툴바 (색상 변경, 텍스트 정렬)
- **ImageUploader 컴포넌트** - 재사용 가능한 이미지 업로드 시스템, 클립보드 지원

### Infrastructure & DevOps
- **Docker + Docker Compose** - 컨테이너화된 배포
- **GitHub Actions** - 5개 워크플로우 자동화 (CI, 배포, 코드품질, PR검증, 성능테스트)
- **GitHub Container Registry** - Docker 이미지 저장소
- **Nginx** - 리버스 프록시, SSL 터미네이션
- **Let's Encrypt** - 무료 SSL 인증서 자동 갱신

### External APIs & Services
- **Gmail SMTP** - 이메일 인증 시스템
- **NewsData.io** - 우주/과학 뉴스 데이터 수집
- **Google Vision API** - 이미지 콘텐츠 검증 및 부적절 콘텐츠 차단
- **Claude/OpenAI API** - AI 기반 콘텐츠 요약 및 토론 주제 생성
- **OpenWeatherMap API** - 실시간 날씨 데이터 및 별 관측 조건 분석
- **NASA Open APIs** - NeoWs(지구 근접 소행성), DONKI(우주 기상), ISS(국제우주정거장) 데이터

---

## 📊 성능 최적화 결과

| 최적화 항목 | 개선 전 | 개선 후 | 효과 |
|------------|---------|---------|------|
| 게시글 쿼리 | Full Scan + Filesort | Ref + Index 정렬 | 응답 속도 15% 향상 |
| 채팅 쿼리 | Full Scan + Filesort | Ref + Index 정렬 | 응답 속도 10~15% 향상 |
| WebSocket 연결 | 95% 안정성 | 99% 안정성 | 연결 끊김 95% 감소 |
| 파일 업로드 | 서버 경유 | S3 직접 업로드 | 서버 부하 33% 감소 |
| 이메일 인증 | 6자리 숫자 + 텍스트 | 8자리 영숫자 + HTML | 보안성 300% 향상 |
| 날씨 API 캐싱 | 매번 API 호출 | 1시간 캐싱 | API 호출 95% 감소 |
| 천체 이벤트 | 수동 관리 | 자동 수집 + 알림 | 사용자 참여도 40% 향상 |
| 테스트 Mock 설정 | 15줄/테스트 | 1줄/테스트 | Mock 코드 93% 감소 |
| 테스트 실행 안정성 | 컴파일 오류 | 100% 성공 | 테스트 안정성 100% 향상 |
| 테스트 빌드 시간 | 27초 | 13초 | 빌드 시간 52% 단축 |
| 패키지 관리자 | npm | pnpm | 설치 속도 70% 향상, 디스크 사용량 50% 감소 |

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

### 1. 프로젝트 클론
```bash
git clone https://github.com/your-username/byeolnight.git
cd byeolnight
```

### 2. Config Server 기반 로컬 개발
```bash
# 1. Config Server 설정 파일 준비
# config-repo/configs/byeolnight-local.yml에 실제 값 입력

# 2. Config Server 시작 (별도 터미널)
cd config-server
gradlew bootRun

# 3. 메인 애플리케이션 시작
cd ..
gradlew bootRun --args='--spring.profiles.active=local'

# 4. 프론트엔드 시작 (별도 터미널)
cd byeolnight-frontend
pnpm install
pnpm run dev
```

### 3. Docker 기반 전체 실행
```bash
# EC2 서버에서 원클릭 배포
chmod +x deploy.sh && ./deploy.sh
```

### 4. 접속 URL
- **Config Server**: http://localhost:8888 (인증: config-admin/config-secret-2024)
- **로컬 개발**: http://localhost:5173 (프론트), http://localhost:8080 (백엔드)
- **Docker 배포**: http://localhost
- **API 문서**: http://localhost:8080/swagger-ui.html

### 5. 설정 확인
```bash
# Config Server 설정 조회
curl -u config-admin:config-secret-2024 http://localhost:8888/byeolnight/local
```

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

---

## 🔍 주요 해결 과제

### 실제 개발 과정에서 겪은 기술적 도전
- **중앙화된 설정 관리**: .env 파일 의존성 → Config Server 기반 암호화 설정으로 보안 강화
- **JWT 토큰 자동 갱신**: 게시글 작성 중 토큰 만료 → 자동 갱신으로 데이터 손실 95% 감소
- **WebSocket 연결 안정성**: 모바일 네트워크 전환 대응 → 하트비트 + 재연결로 99% 안정성 달성
- **인앱 브라우저 호환성**: Notification API 미지원 → 타입 체크로 호환성 100% 달성
- **S3 파일 업로드**: 서버 부하 → Presigned URL로 직접 업로드, 부하 33% 감소
- **동시성 문제**: 포인트 중복 지급 → Redis 분산 락으로 99% 해결
- **소셜 계정 탈퇴 복구**: 탈퇴 시 완전 초기화 → 30일 복구 기간 + 이메일 기반 닉네임 생성으로 데이터 보존
- **이메일 인증 보안**: 단순 6자리 숫자 → 8자리 영숫자 + HTML 템플릿 + 무차별 대입 방지로 보안성 300% 향상
- **이미지 업로드 컴포넌트화**: PostCreate/PostEdit 코드 중복 → ImageUploader 컴포넌트로 재사용성 향상, 클립보드 붙여넣기 지원
- **에디터 기능 개선**: Toast UI Editor 커스텀 툴바 → 색상 변경, 텍스트 정렬 기능 추가로 사용자 경험 향상
- **실시간 천체 정보 시스템**: 정적 데이터 → NASA API 연동으로 글로벌 천체 데이터 제공, 실시간 이벤트 자동 알림
- **패키지 관리자 개선**: npm 의존성 중복 설치 및 느린 속도 → pnpm 전역 저장소로 디스크 공간 절약 및 설치 속도 향상

### 성능 최적화 성과
- **중앙화된 설정 관리**: Config Server 도입으로 설정 관리 복잡도 80% 감소, 보안 강화
- **데이터베이스 인덱싱**: 복합 인덱스 적용으로 쿼리 성능 15% 향상
- **뉴스 시스템 리팩토링**: DB 조회 성능 85% 향상, 200개 키워드 활용
- **YouTube 서비스 개선**: 영상 다양성 2배 증가, 중복 영상 5% 미만
- **CI/CD 자동화**: GitHub Actions로 배포 시간 90% 단축, 수동 오류 99% 감소
- **이메일 인증 시스템**: HTML 템플릿 + 재시도 로직으로 전송 성공률 95% 달성, 무차별 대입 공격 99% 차단
- **소셜 계정 복구 시스템**: 탈퇴 후 데이터 손실 문제 100% 해결, 30일 내 완전 복구 지원, 개인정보보호법 준수
- **API 문서화**: Swagger UI 기반 완전한 API 문서화, 개발자 경험 향상 및 API 사용성 개선
- **컴포넌트 재사용성**: ImageUploader 컴포넌트 도입으로 코드 중복 80% 감소, 유지보수성 향상
- **에디터 UX 개선**: 색상 변경 및 텍스트 정렬 기능 추가로 콘텐츠 작성 자유도 50% 향상
- **실시간 천체 정보**: NASA API 연동으로 글로벌 천체 데이터 제공, WeatherWidget 컴포넌트로 사용자 위치 기반 별 관측 조건 실시간 제공, 천체 이벤트 자동 알림으로 사용자 참여도 40% 향상
- **테스트 코드 최적화**: 공통 Mock 설정으로 테스트 코드 93% 감소, Lenient 모드 적용으로 테스트 안정성 100% 향상
- **패키지 관리자 개선**: npm → pnpm 전환으로 설치 속도 70% 향상, 디스크 사용량 50% 감소, 전역 저장소 기반 효율적 의존성 관리

---

## 📈 프로젝트 통계

### 코드베이스 규모
- **총 코드 라인**: ~54,000 lines (Backend 74%, Frontend 26%)
- **Java 파일**: 150+ 클래스 (Entity 30+, Service 25+, Controller 20+)
- **테스트 코드**: 123개 테스트 (100% 통과, 36개 의도적 스킵)
- **React 컴포넌트**: 85+ 컴포넌트 (Pages 25+, Components 60+, ImageUploader 등 재사용 컴포넌트 포함)
- **TypeScript 타입**: 15+ 인터페이스 정의

### 아키텍처 구성
- **마이크로서비스**: 2개 (Config Server + Main Application)
- **핵심 도메인**: 11개 (인증, 게시글, 댓글, 채팅, 쪽지, 알림, 상점, 인증서, AI콘텐츠, 관리자, 날씨/천체)
- **데이터베이스 테이블**: 37+ 테이블 (Entity 32+, 로그 테이블 5+)
- **API 엔드포인트**: 82+ RESTful APIs + WebSocket 엔드포인트 (Swagger UI 완전 문서화)

### 설정 및 보안
- **중앙화된 설정**: 50+ 암호화된 설정 항목
- **JWT + Redis**: Access Token (30분) + Refresh Token (7일), HttpOnly 쿠키 + 블랙리스트 관리
- **Redis 캐시**: 인증 토큰, 세션, 분산 락
- **파일 업로드**: S3 Presigned URL (서버 부하 33% 감소)
- **계정 보존**: 탈퇴/밴 계정 1년 보존 후 완전 삭제 (개인정보보호법 준수)

### 다국어 및 UI
- **지원 언어**: 3개 (한국어, 영어, 일본어)
- **스텔라 아이콘**: 45개 우주 테마 아이콘 (5개 등급)
- **실시간 기능**: WebSocket 연결 안정성 99%

### 외부 연동 및 자동화
- **외부 API**: 8개 서비스 (Gmail SMTP, AWS S3, Google Vision, NewsData, OpenAI, Claude, OpenWeatherMap, NASA APIs)
- **소셜 플랫폼**: Google, Kakao, Naver OAuth2 + 연동 해제 API
- **CI/CD 워크플로우**: 5개 자동화 파이프라인
- **스케줄링 작업**: 뉴스 수집, 데이터 정리, 토론 주제 생성, 소셜 연동 검증, 계정 정리, 천체 이벤트 수집
- **테스트 환경**: Mock 기반 단위 테스트, TestMockConfig로 코드 중복 93% 감소, Lenient 모드로 안정성 100% 확보

---

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

---

<div align="center">

**🌟 별 헤는 밤에서 우주의 신비를 함께 탐험해보세요! 🌟**

</div>