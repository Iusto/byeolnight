# 03. 애플리케이션 구조

> 도메인 중심의 계층화 구조로 복잡성을 분산하고 유지보수성을 확보합니다.

## 🏛️ 전체 아키텍처 개요

```plaintext
[사용자] ──> [UI 계층 (Controller)] ──> [Application 계층] ──> [Domain 계층] ──> [Infrastructure]
```

| 계층                                    | 역할                         | 대표 예시                              |
| ------------------------------------- | -------------------------- | ---------------------------------- |
| **UI** (`controller`)                 | 외부 요청 진입점. DTO 수신/응답       | `UserController`, `PostController` |
| **Application** (`application`)       | 유스케이스 조합, 트랜잭션 처리          | `AuthFacade`, `MessageFacade`      |
| **Domain** (`domain`)                 | 도메인 모델, 엔티티, 도메인 서비스       | `User`, `Token`, `PostService` 등   |
| **Infrastructure** (`infrastructure`) | 기술 구현 (JPA, Redis, 외부 API) | `JpaUserRepository`, `S3Uploader`  |

## 🧭 설계 원칙

* **계층 간 의존성 방향 유지**: UI → Application → Domain → Infrastructure
* **도메인 독립성 보장**: Domain 계층은 외부 기술에 의존하지 않음
* **Application은 orchestration 중심**: 도메인 서비스 호출, 트랜잭션 단위 관리
* **Infrastructure는 DIP 기반으로 주입됨** (ex. `@Repository`, `@Component`)

## 📦 구조 예시

```
src/main/java/com/byeolnight
├── domain/
│   ├── entity/         # 핵심 엔티티
│   ├── repository/     # 저장소 인터페이스
│   └── service/        # 도메인 서비스 (비즈니스 로직)
├── application/
│   └── facade/         # 유스케이스 조합, 트랜잭션 단위 처리
├── ui/
│   └── controller/     # API 진입점, Request/Response 매핑
├── infrastructure/
│   ├── jpa/            # JPA 기반 Repository 구현
│   ├── s3/             # AWS S3 Presigned URL 처리
│   ├── redis/          # Redis TTL 및 분산 락 처리
│   └── external/       # 외부 API 연동 (NewsData, Vision 등)
```

> 도메인 계층의 복잡도는 기능 수가 아닌 도메인 수에 따라 나뉘며, 각 Bounded Context를 독립 유지하는 것을 우선합니다.

---

👉 다음 문서: [04. 핵심 도메인별 구조](./04_core-domains.md)