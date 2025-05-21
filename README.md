# 🌌 별 헤는 밤

**AI 기반 은하 분류 + 커뮤니티 기능을 결합한 우주 이미지 커뮤니티 프로젝트**

---

## 🚀 프로젝트 소개

> “별 헤는 밤”은 사용자가 은하 이미지를 업로드하면, AI가 자동으로 분류해주고  
> 이를 기반으로 자유롭게 기록하고 토론할 수 있는 감성 + 기술 융합 커뮤니티입니다.

---

## 📚 주요 기능

- ✅ 회원가입 / 로그인 (JWT 기반 인증)
- ✅ 은하 게시글 CRUD (제목, 설명, 이미지, 분류 결과 포함)
- ✅ Swagger(OpenAPI 3) API 문서 자동화
- ✅ Spring Security + JWT 인증 필터 적용
- ✅ Docker + docker-compose 로 통합 배포
- ✅ GitHub Actions 기반 CI 파이프라인 구성

---

## 🛠 기술 스택

| 영역 | 기술 |
|------|------|
| Language | Java 21 |
| Backend | Spring Boot 3.2, Spring Security, Spring Data JPA |
| Auth | JWT (JJWT), BCrypt |
| Infra | MySQL, Redis, Docker, GitHub Actions |
| API Docs | SpringDoc OpenAPI 3 + Swagger UI |
| Test | JUnit 5 + Mockito (예정) |

---

## 🧪 실행 방법

### 1. Gradle Build

```bash
./gradlew build
```

### 2. Docker로 실행

```bash
docker-compose up --build
```

### 3. Swagger UI

[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## 📁 디렉토리 구조

```bash
src/
├── main/java/com/byeolnight
│   ├── domain/              # 엔티티
│   ├── application/         # 서비스 레이어
│   ├── infrastructure/      # 리포지토리, 보안 구성
│   └── api/controller/      # REST 컨트롤러
├── resources/
│   └── application.yml
```

---

## ✨ 향후 계획

- ✅ 단위 테스트 작성
- ✅ React 프론트엔드 연동
- ✅ EC2 배포 및 운영 리포트 정리

---

## 👨‍💻 개발자

| 이름 | GitHub |
|------|--------|
| 김정규 | [github.com/Iusto](https://github.com/Iusto) |
