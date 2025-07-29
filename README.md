# 🌌 별 헤는 밤 (Byeolnight) - 우주 감성 커뮤니티 서비스

> "도메인 문제를 명확히 정의하고, 구조로 해결합니다."

**Byeolnight**는 우주를 테마로 한 커뮤니티 플랫폼이자, 복잡한 비즈니스 요구사항을 "도메인 중심 아키텍처"로 설계한 백엔드 프로젝트입니다. 단순한 기능 구현을 넘어, 운영 환경에서 살아남는 실전 구조를 목표로 합니다.

---

## 🧭 목차

1. [설계 철학](docs/01_design-philosophy.md)
2. [도메인 모델 개요](docs/02_domain-model.md)
3. [애플리케이션 구조](docs/03_architecture.md)
4. [핵심 기능별 도메인 설명](docs/04_core-domains.md)
5. [성능 최적화 전략](docs/05_optimizations.md)
6. [기술 스택](docs/06_tech-stack.md)
7. [테스트 전략](docs/07_testing.md)
8. [운영 및 배포](docs/08_deployment.md)
9. [향후 계획](docs/09_roadmap.md)
10. [기여 방법](docs/10_contributing.md)

---

## 🔧 빠른 실행 방법

### 1. 환경 요구 사항

* Java 21+
* Docker / Docker Compose
* Git

### 2. 실행

```bash
# 1. 레포 클론 및 이동
$ git clone https://github.com/Iusto/byeolnight.git
$ cd byeolnight

# 2. 환경 변수 설정
$ cp .env.example .env
# → 이메일, DB, AWS 등 실제 값으로 수정

# 3. 로컬 백엔드 실행
$ ./run-local.bat            # Windows
# 또는
$ docker-compose -f docker-compose.local.yml up -d
$ ./gradlew bootRun --args='--spring.profiles.active=local'

# 4. 프론트 실행
$ cd byeolnight-frontend && npm install && npm run dev

# 5. 접속 주소
- 프론트: http://localhost:5173
- 백엔드: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
```

---

## 📂 디렉토리 구조 요약

```
byeolnight
├── docs/                    # 설계 및 문서 파일
├── src/
│   └── main/java/com/byeolnight/
│       ├── domain/          # 도메인 중심 구조
│       │   ├── entity/      # 엔티티 (예: User, Post, Message)
│       │   ├── repository/  # 저장소 인터페이스
│       │   └── service/     # 도메인 서비스
│       ├── application/     # 유스케이스 단위 서비스
│       ├── ui/              # Controller (외부 요청 진입점)
│       ├── infrastructure/  # 외부 API, Redis, JPA 구현체
│       └── config/          # 설정 파일
├── test/                    # 단위 및 통합 테스트
└── byeolnight-frontend/     # React 기반 프론트엔드
```

---

## 📎 추가 문서

* [🧠 DDD와 도메인 중심 설계 개요](docs/01_design-philosophy.md)
* [🧱 도메인별 상세 구조](docs/04_core-domains.md)
* [📡 운영환경 대응 전략](docs/08_deployment.md)

---

> “Byeolnight는 단순한 커뮤니티가 아니라, 복잡한 운영 시나리오를 구조로 해결하는 실전 백엔드 설계의 집약체입니다.”

---

[GitHub](https://github.com/Iusto) • [Email](mailto:iusto@naver.com) • [LinkedIn](https://www.linkedin.com/in/jeonggyu-kim-711289343/)
