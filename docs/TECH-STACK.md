# 🔧 기술 스택 상세

> 별 헤는 밤에서 사용된 기술 스택과 선택 배경에 대한 상세한 설명입니다.

## 📋 목차
- [Backend (핵심 역량)](#backend-핵심-역량)
- [Testing & Quality Assurance](#testing--quality-assurance)
- [Frontend (사용자 경험)](#frontend-사용자-경험)
- [Infrastructure & DevOps](#infrastructure--devops)
- [External APIs & Services](#external-apis--services)
- [개발 도구 및 환경](#개발-도구-및-환경)

---

## Backend (핵심 역량)

### 🚀 Java 21 + Spring Boot 3.2.4
**선택 배경**: 최신 LTS 기반 안정성, Virtual Thread 지원으로 동시성 처리 개선
- **Virtual Thread**: 경량 스레드로 동시성 처리 성능 향상
- **Record 클래스**: 불변 데이터 클래스로 코드 간소화
- **Pattern Matching**: 코드 가독성 및 안전성 향상
- **Text Blocks**: 다중 라인 문자열 처리 개선

### ⚙️ Spring Cloud Config Server
**선택 배경**: 환경별 설정 분산 문제 해결, 민감 정보 암호화로 보안 강화
- **중앙화된 설정 관리**: 모든 환경 설정을 중앙에서 관리
- **암호화 지원**: 민감한 정보 (DB 비밀번호, API 키 등) 암호화 저장
- **동적 설정 변경**: 애플리케이션 재시작 없이 설정 변경 가능
- **버전 관리**: Git 기반 설정 파일 버전 관리
- **환경별 프로파일**: local, dev, prod 환경별 설정 분리

### 🔐 Spring Security + JWT
**선택 배경**: 세션 기반 인증의 확장성 한계 극복, 무상태 인증으로 로드밸런싱 최적화
- **JWT 토큰**: 무상태 인증으로 서버 확장성 향상
- **HttpOnly 쿠키**: XSS 공격 방지
- **Refresh Token**: 보안과 사용자 편의성 균형
- **OAuth2 통합**: Google, Kakao, Naver 소셜 로그인
- **CORS 설정**: 프론트엔드와의 안전한 통신

### 🗄️ Spring Data JPA + QueryDSL
**선택 배경**: N+1 쿼리 문제 해결, 타입 안전 쿼리로 런타임 오류 방지
- **JPA**: 객체-관계 매핑으로 개발 생산성 향상
- **QueryDSL**: 타입 안전한 쿼리 작성
- **N+1 문제 해결**: @EntityGraph, Fetch Join 활용
- **페이지네이션**: 효율적인 대용량 데이터 처리
- **Auditing**: 생성/수정 시간 자동 관리

### 💾 MySQL 8.0 + Redis 7.0
**선택 배경**: 관계형 데이터 무결성 + 고성능 캐싱, 분산 락으로 동시성 제어
- **MySQL 8.0**: 
  - JSON 데이터 타입 지원
  - 향상된 성능 및 보안
  - 복합 인덱스 최적화
- **Redis 7.0**:
  - 세션 저장소
  - 캐시 레이어
  - 분산 락 구현
  - 실시간 데이터 저장

### 🔒 Redisson
**선택 배경**: 단순 Redis 클라이언트의 분산 락 한계 극복, 공정성과 재진입 지원
- **분산 락**: 멀티 인스턴스 환경에서 동시성 제어
- **공정성 보장**: FIFO 순서로 락 획득
- **재진입 지원**: 같은 스레드에서 락 재획득 가능
- **자동 해제**: 락 보유자 장애 시 자동 해제
- **다양한 락 타입**: ReadWriteLock, Semaphore 등

### ☁️ AWS S3 + CloudFront
**선택 배경**: SSRF 취약점 해결, 서버 부하 분산 및 글로벌 CDN으로 성능 향상
- **S3 Presigned URL**: 서버 부하 없는 직접 업로드
- **CloudFront Signed URL**: 보안이 강화된 파일 조회
- **OAC (Origin Access Control)**: S3 직접 접근 차단
- **글로벌 CDN**: 전 세계 빠른 파일 전송
- **SSRF 방지**: 서버 사이드 요청 위조 공격 차단

### 🔌 Native WebSocket (STOMP) + HttpOnly 쿠키
**선택 배경**: Socket.IO 의존성 제거, XSS 공격 방지를 위한 쿠키 보안
- **STOMP 프로토콜**: 메시지 브로커 패턴으로 확장성 향상
- **HttpOnly 쿠키**: JavaScript 접근 불가로 XSS 방지
- **하트비트**: 연결 상태 모니터링
- **재연결 로직**: 네트워크 장애 시 자동 재연결
- **브로드캐스트**: 전체 사용자 대상 메시지 전송

### ⏰ Spring Scheduling
**선택 배경**: 외부 크론 서버 불필요, 애플리케이션 내장으로 운영 복잡도 감소
- **@Scheduled**: 간단한 스케줄링 설정
- **Cron 표현식**: 복잡한 스케줄링 지원
- **비동기 처리**: @Async로 성능 최적화
- **예외 처리**: 스케줄링 작업 예외 처리
- **모니터링**: 스케줄링 작업 실행 로그

### 📚 Swagger UI
**선택 배경**: API 문서 수동 관리 부담 해소, 자동 생성으로 문서-코드 동기화 보장
- **자동 문서 생성**: 코드 기반 API 문서 자동 생성
- **인터랙티브 UI**: 브라우저에서 직접 API 테스트 가능
- **스키마 정의**: 요청/응답 모델 자동 문서화
- **인증 지원**: JWT 토큰 기반 API 테스트
- **다운로드**: OpenAPI 3.0 스펙 다운로드 가능

---

## Testing & Quality Assurance

### 🧪 JUnit 5 + Mockito
**123개 테스트 100% 통과 ✅**
- **JUnit 5**: 최신 테스트 프레임워크
- **Mockito**: Mock 객체 생성 및 검증
- **@ExtendWith**: 테스트 확장 기능
- **@ParameterizedTest**: 매개변수화된 테스트
- **@TestMethodOrder**: 테스트 실행 순서 제어

### 🎭 TestMockConfig
**선택 배경**: 공통 Mock 설정으로 테스트 코드 93% 감소
- **중앙화된 Mock 설정**: 모든 테스트에서 공통 Mock 사용
- **@MockBean**: Spring Boot 테스트용 Mock Bean
- **Lenient Mock**: 엄격한 검증 완화로 안정성 향상
- **테스트 격리**: 각 테스트 간 독립성 보장
- **설정 재사용**: Mock 설정 코드 중복 제거

### ✅ AssertJ
**선택 배경**: 유창한 API 기반 테스트 검증
- **Fluent API**: 읽기 쉬운 테스트 코드
- **다양한 Assertion**: 컬렉션, 예외, 날짜 등 다양한 검증
- **커스텀 Assertion**: 도메인 특화 검증 로직
- **에러 메시지**: 명확한 실패 메시지 제공
- **IDE 지원**: 자동완성 및 타입 안전성

### 🏗️ Spring Boot Test
**선택 배경**: 통합 테스트 및 슬라이스 테스트
- **@SpringBootTest**: 전체 애플리케이션 컨텍스트 테스트
- **@WebMvcTest**: 웹 레이어 슬라이스 테스트
- **@DataJpaTest**: JPA 레이어 슬라이스 테스트
- **@TestConfiguration**: 테스트 전용 설정
- **TestContainers**: 실제 데이터베이스 테스트

---

## Frontend (사용자 경험)

### ⚛️ React 18 + TypeScript
**선택 배경**: 컴포넌트 기반 UI, 타입 안전성
- **React 18**: 
  - Concurrent Features
  - Automatic Batching
  - Suspense 개선
- **TypeScript**:
  - 정적 타입 검사
  - IDE 지원 향상
  - 런타임 오류 방지
  - 인터페이스 정의

### ⚡ Vite + TailwindCSS
**선택 배경**: 빠른 개발 서버, 유틸리티 우선 스타일링
- **Vite**:
  - 빠른 HMR (Hot Module Replacement)
  - ES 모듈 기반 번들링
  - 플러그인 생태계
- **TailwindCSS**:
  - 유틸리티 우선 CSS
  - 반응형 디자인
  - 다크 모드 지원
  - 커스텀 테마

### 🛣️ React Router DOM v7
**선택 배경**: 클라이언트 사이드 라우팅
- **중첩 라우팅**: 복잡한 UI 구조 지원
- **Lazy Loading**: 코드 스플리팅으로 성능 최적화
- **Protected Routes**: 인증 기반 라우트 보호
- **History API**: 브라우저 히스토리 관리
- **동적 라우팅**: 매개변수 기반 라우팅

### 🌐 Axios + Native WebSocket/STOMP
**선택 배경**: HTTP 통신 및 WebSocket 연결
- **Axios**:
  - HTTP 클라이언트
  - 인터셉터 지원
  - 요청/응답 변환
  - 에러 처리
- **WebSocket/STOMP**:
  - 실시간 양방향 통신
  - 메시지 브로커 패턴
  - 자동 재연결
  - 하트비트

### 🌍 React i18next
**선택 배경**: 다국어 지원 (한국어, 영어, 일본어)
- **네임스페이스**: 번역 파일 분리 관리
- **Lazy Loading**: 필요한 언어만 로드
- **Pluralization**: 복수형 처리
- **Interpolation**: 동적 값 삽입
- **Context**: 문맥에 따른 번역

### ✏️ Toast UI Editor
**선택 배경**: 마크다운 기반 에디터, 커스텀 툴바
- **WYSIWYG + Markdown**: 두 가지 편집 모드
- **커스텀 툴바**: 색상 변경, 텍스트 정렬 추가
- **플러그인**: 다양한 확장 기능
- **이미지 업로드**: 드래그 앤 드롭 지원
- **실시간 미리보기**: 작성 중 미리보기

### 📷 ImageUploader 컴포넌트
**선택 배경**: 재사용 가능한 이미지 업로드 시스템, 클립보드 지원
- **재사용성**: 여러 컴포넌트에서 공통 사용
- **클립보드 지원**: Ctrl+V로 이미지 붙여넣기
- **진행률 표시**: 업로드 진행률 실시간 표시
- **에러 처리**: 사용자 친화적 에러 메시지
- **미리보기**: 업로드 전 이미지 미리보기

---

## Infrastructure & DevOps

### 🐳 Docker + Docker Compose
**선택 배경**: 컨테이너화된 배포
- **일관된 환경**: 개발/테스트/운영 환경 일치
- **격리**: 애플리케이션 간 격리
- **확장성**: 수평 확장 용이
- **배포 자동화**: 컨테이너 기반 배포
- **Multi-stage Build**: 최적화된 이미지 생성

### 🔄 GitHub Actions
**선택 배경**: 5개 워크플로우 자동화
1. **CI 테스트**: 코드 푸시 시 자동 테스트
2. **자동 배포**: master 브랜치 배포
3. **코드 품질**: CodeQL 보안 스캔
4. **PR 검증**: Pull Request 검증
5. **성능 테스트**: 주기적 성능 측정

### 📦 GitHub Container Registry
**선택 배경**: Docker 이미지 저장소
- **통합 관리**: GitHub와 통합된 컨테이너 레지스트리
- **보안**: 프라이빗 이미지 저장
- **버전 관리**: 이미지 태그 관리
- **자동화**: CI/CD 파이프라인 통합
- **비용 효율**: GitHub 패키지와 통합 과금

### 🌐 Nginx
**선택 배경**: 리버스 프록시, SSL 터미네이션
- **리버스 프록시**: 백엔드 서버 로드 밸런싱
- **SSL 터미네이션**: HTTPS 처리
- **정적 파일 서빙**: 프론트엔드 파일 서빙
- **압축**: Gzip 압축으로 전송 최적화
- **캐싱**: 정적 리소스 캐싱

### 🔒 Let's Encrypt
**선택 배경**: 무료 SSL 인증서 자동 갱신
- **무료 SSL**: 비용 절약
- **자동 갱신**: 인증서 만료 걱정 없음
- **도메인 검증**: 자동 도메인 소유권 검증
- **와일드카드**: 서브도메인 지원
- **브라우저 호환**: 모든 주요 브라우저 지원

---

## External APIs & Services

### 📧 Gmail SMTP
**선택 배경**: 이메일 인증 시스템
- **높은 전달률**: Gmail의 높은 이메일 전달률
- **보안**: OAuth2 인증
- **HTML 템플릿**: 풍부한 이메일 디자인
- **대용량**: 일일 전송 한도 충분
- **모니터링**: 전송 상태 추적

### 📰 NewsData.io
**선택 배경**: 우주/과학 뉴스 데이터 수집
- **실시간 뉴스**: 최신 뉴스 실시간 수집
- **다양한 소스**: 전 세계 뉴스 소스
- **카테고리 필터**: 과학/기술 카테고리 필터
- **언어 지원**: 다국어 뉴스 지원
- **API 안정성**: 높은 가용성

### 👁️ Google Vision API
**선택 배경**: 이미지 콘텐츠 검증 및 부적절 콘텐츠 차단
- **AI 기반 분석**: 고정확도 이미지 분석
- **다양한 카테고리**: 성인, 폭력, 의료 등 감지
- **실시간 처리**: 빠른 이미지 분석
- **확장성**: 대용량 이미지 처리
- **정확도**: 높은 감지 정확도

### 🤖 Claude/OpenAI API
**선택 배경**: AI 기반 콘텐츠 요약 및 토론 주제 생성
- **Claude API**:
  - 긴 텍스트 처리 우수
  - 한국어 지원
  - 안전한 AI 응답
- **OpenAI API**:
  - GPT 모델 활용
  - 창의적 콘텐츠 생성
  - 다양한 모델 선택

### 🌤️ OpenWeatherMap API
**선택 배경**: 실시간 날씨 데이터 및 별 관측 조건 분석
- **정확한 날씨**: 높은 정확도의 날씨 정보
- **글로벌 커버리지**: 전 세계 날씨 데이터
- **다양한 데이터**: 온도, 습도, 구름량, 시정 등
- **예보**: 시간별/일별 예보
- **API 안정성**: 높은 가용성

### 🚀 NASA Open APIs
**선택 배경**: 실제 우주 데이터 제공
- **NeoWs API**: 지구 근접 소행성 정보
- **DONKI API**: 우주 기상 정보
- **ISS API**: 국제우주정거장 위치
- **무료 제공**: NASA에서 무료 제공
- **신뢰성**: 공식 우주 기관 데이터

---

## 개발 도구 및 환경

### 💻 개발 환경
- **IDE**: IntelliJ IDEA Ultimate, VS Code
- **Java**: OpenJDK 21
- **Node.js**: 18.x LTS
- **Package Manager**: pnpm (70% 빠른 설치, 50% 적은 디스크 사용)
- **Git**: 버전 관리 및 협업

### 🔧 빌드 도구
- **Gradle**: Java 프로젝트 빌드
- **Vite**: 프론트엔드 빌드
- **Docker**: 컨테이너 빌드
- **GitHub Actions**: CI/CD 파이프라인

### 📊 모니터링 및 로깅
- **Spring Boot Actuator**: 애플리케이션 모니터링
- **Logback**: 로깅 프레임워크
- **Micrometer**: 메트릭 수집
- **Swagger UI**: API 문서 및 테스트

### 🛡️ 보안 도구
- **CodeQL**: 정적 코드 분석
- **Dependabot**: 의존성 취약점 검사
- **OWASP**: 보안 가이드라인 준수
- **SonarQube**: 코드 품질 분석

---

## 🔗 관련 문서

- [🚀 상세 기능 설명](./FEATURES.md)
- [🏗️ 아키텍처 가이드](./ARCHITECTURE.md)
- [📊 성능 최적화](./PERFORMANCE.md)
- [🧪 테스트 가이드](./TESTING.md)
- [📦 배포 가이드](./DEPLOYMENT.md)