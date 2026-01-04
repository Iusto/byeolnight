# 03. 애플리케이션 구조

> 도메인 중심의 계층화 구조로 복잡성을 분산하고 유지보수성을 확보합니다.

## 🏛️ 전체 아키텍처 개요

```plaintext
[사용자] ──> [UI 계층 (Controller)] ──> [Application 계층] ──> [Domain 계층] ──> [Infrastructure]
```

| 계층                                    | 역할                         | 대표 예시                              |
| ------------------------------------- | -------------------------- | ---------------------------------- |
| **UI** (`controller`)                 | 외부 요청 진입점. DTO 수신/응답       | `UserController`, `PostController` |
| **Application** (`service`)           | 비즈니스 로직, 트랜잭션 처리          | `AuthService`, `MessageService`    |
| **Domain** (`entity`)                 | 도메인 모델, 엔티티              | `User`, `Post`, `Message` 등        |
| **Infrastructure** (`infrastructure`) | 기술 구현 (JPA, Redis, 외부 API) | `RedissonConfig`, `S3Service`      |

## 🧭 설계 원칙

* **계층 간 의존성 방향 유지**: UI → Application → Domain → Infrastructure
* **도메인 독립성 보장**: Domain 계층은 외부 기술에 의존하지 않음
* **Service는 비즈니스 로직 중심**: 도메인 엔티티 조작, 트랜잭션 단위 관리
* **Infrastructure는 DIP 기반으로 주입됨** (ex. `@Repository`, `@Component`)

## 📦 구조 예시

```
src/main/java/com/byeolnight
├── entity/             # 도메인 엔티티 (도메인별 패키지 분리)
├── repository/         # 저장소 인터페이스 (도메인별 패키지 분리)
├── service/            # 비즈니스 로직 (도메인별 패키지 분리)
├── controller/         # API 진입점, Request/Response 매핑
├── dto/                # 데이터 전송 객체 (도메인별 패키지 분리)
├── config/             # Spring 설정 클래스
├── infrastructure/
│   ├── cache/          # Redis 캐시 서비스, 메시지 큐
│   ├── config/         # 인프라 설정 (Redisson, QueryDSL 등)
│   ├── security/       # JWT, OAuth2, 보안 필터
│   ├── exception/      # 글로벌 예외 처리
│   └── util/           # 유틸리티 클래스
```

> 각 도메인(auth, post, chat, message 등)별로 패키지를 분리하여 Bounded Context를 독립 유지하며, 도메인 간 의존성을 최소화합니다.

---

👉 다음 문서: [04. 핵심 도메인별 구조](./04_core-domains.md)