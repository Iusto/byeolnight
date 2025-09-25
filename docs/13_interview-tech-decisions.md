# 🎤 면접 대비: 기술 선택 이유 및 대안 검토

> "왜 이 기술을 선택했나요? 다른 방법은 없었나요?" 질문에 대한 구체적인 답변 가이드

## 📋 목차
- [커넥션풀 & 쓰레드풀](#커넥션풀--쓰레드풀)
- [Redisson vs Redis](#redisson-vs-redis)
- [JWT vs Session](#jwt-vs-session)
- [S3 Presigned URL vs 서버 업로드](#s3-presigned-url-vs-서버-업로드)
- [WebSocket vs HTTP 폴링](#websocket-vs-http-폴링)
- [Spring Cloud Config vs 환경변수](#spring-cloud-config-vs-환경변수)
- [QueryDSL vs JPA Repository](#querydsl-vs-jpa-repository)
- [React vs Vue/Angular](#react-vs-vueangular)

---

## 커넥션풀 & 쓰레드풀

### 🤔 **예상 질문**
> "커넥션풀과 쓰레드풀을 왜 도입했나요? 다른 방법은 없었나요?"

### 💡 **답변 전략**

#### **1. 문제 상황부터 설명**
```
🚨 실제 겪었던 문제:
- 이미지 업로드 시 Google Vision API 호출로 3-5초 대기
- 동시 업로드 시 메인 쓰레드 블로킹으로 전체 서비스 응답 지연
- Redis 연결 부족으로 포인트 지급 실패 (connection timeout)
- HTTP 연결 재사용 없이 매번 새 연결 생성으로 성능 저하
```

#### **2. 검토했던 대안들**
```
🔍 다른 방법들을 검토했지만...

1️⃣ 동기 처리 유지
   ❌ 사용자가 이미지 업로드 시 5초간 대기 (UX 최악)
   
2️⃣ 단순 @Async만 사용  
   ❌ 쓰레드 무제한 생성으로 메모리 부족 위험
   
3️⃣ Redis 연결을 매번 새로 생성
   ❌ 연결 오버헤드로 성능 저하
   
4️⃣ 메시지 큐 (RabbitMQ, Kafka)
   ❌ 단순 이미지 검증에 오버엔지니어링
```

#### **3. 풀 패턴 선택 이유**
```
💡 왜 풀(Pool) 패턴?

✅ 리소스 재사용 = 성능 + 안정성
✅ 제한된 리소스로 메모리 보호
✅ 가장 단순하면서도 효과적인 해결책
✅ 복잡한 아키텍처 변경 없이 즉시 적용 가능
```

#### **4. 구체적인 개선 효과**
```
📈 실제 개선 결과:

쓰레드풀 도입:
- 이미지 업로드 응답시간: 5초 → 200ms (비동기 처리)
- 동시 처리 능력: 1개 → 8개 (안정적)

커넥션풀 도입:  
- Redis 연결 실패율: 15% → 0%
- 평균 응답시간: 500ms → 50ms (연결 재사용)
```

---

## Redisson vs Redis

### 🤔 **예상 질문**
> "Redis만 써도 되는데 왜 Redisson을 추가로 도입했나요?"

### 💡 **답변 전략**

#### **1. 구체적인 문제 상황**
```
🚨 Redis만으로는 해결 안 되는 문제:
- 포인트 지급 시 동시성 문제 (race condition)
- 출석 체크 중복 처리 문제
- 아이콘 구매 시 중복 결제 위험
```

#### **2. Redis vs Redisson 비교**
```
📊 기능 비교:

Redis (RedisTemplate):
✅ 단순 SET/GET
❌ 분산락 구현 복잡 (SETNX + 만료시간 수동 관리)
❌ 데드락 위험
❌ 재진입 락 미지원

Redisson:
✅ 분산락 자동 관리 (tryLock, unlock)
✅ 데드락 방지 (자동 해제)
✅ 재진입 락 지원
✅ 공정성 보장 (FIFO)
✅ 분산 컬렉션 (RMapCache 등)
```

#### **3. 실제 사용 사례**
```java
// Redis만으로 분산락 구현 시 (복잡함)
String lockKey = "lock:user:" + userId;
String lockValue = UUID.randomUUID().toString();
Boolean acquired = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10));
// 수동으로 해제, 데드락 위험 등...

// Redisson 사용 시 (간단함)
RLock lock = redissonClient.getLock("lock:user:" + userId);
try {
    if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
        // 비즈니스 로직
    }
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock(); // 자동 안전 해제
    }
}
```

#### **4. 왜 다른 대안 안 썼는지**
```
🤷♂️ 다른 분산락 방법들:

1️⃣ 데이터베이스 락
   ❌ 성능 저하, 데드락 위험 높음
   
2️⃣ Zookeeper
   ❌ 오버엔지니어링, 운영 복잡도 증가
   
3️⃣ 직접 구현
   ❌ 버그 위험, 검증된 라이브러리 사용이 안전
```

---

## JWT vs Session

### 🤔 **예상 질문**
> "세션 방식도 있는데 왜 JWT를 선택했나요?"

### 💡 **답변 전략**

#### **1. 확장성 문제**
```
🚨 세션 방식의 한계:
- 서버 확장 시 세션 공유 문제
- 로드밸런서 설정 복잡 (sticky session)
- 메모리 사용량 증가
```

#### **2. JWT 선택 이유**
```
✅ JWT의 장점:
- 무상태(stateless) 인증
- 서버 확장성 우수
- 마이크로서비스 아키텍처 적합
- 모바일 앱 지원 용이
```

#### **3. JWT 단점 해결 방법**
```
❌ JWT 단점과 해결책:

단점: 토큰 크기가 큼
해결: 필요한 정보만 포함 (userId, role만)

단점: 토큰 무효화 어려움  
해결: Redis 블랙리스트 + Refresh Token 패턴

단점: 보안 위험
해결: HttpOnly 쿠키 + CSRF 토큰
```

---

## S3 Presigned URL vs 서버 업로드

### 🤔 **예상 질문**
> "서버에서 파일을 받아서 S3에 업로드하면 되는데 왜 Presigned URL을 썼나요?"

### 💡 **답변 전략**

#### **1. 서버 부하 문제**
```
🚨 서버 업로드 방식의 문제:
- 10MB 파일 업로드 시 서버 메모리 사용
- 동시 업로드 시 서버 부하 급증
- 네트워크 대역폭 2배 사용 (클라이언트→서버→S3)
```

#### **2. Presigned URL 장점**
```
✅ 개선 효과:
- 서버 부하 0% (클라이언트가 S3에 직접 업로드)
- 업로드 속도 향상 (직접 연결)
- 서버 리소스 절약
- 확장성 우수
```

#### **3. 보안 우려 해결**
```
🔒 보안 강화 방법:
- Presigned URL 5분 제한
- 파일 크기 제한 (10MB)
- 확장자 검증
- Google Vision API로 이미지 검열
- CloudFront OAI로 S3 직접 접근 차단
```

---

## WebSocket vs HTTP 폴링

### 🤔 **예상 질문**
> "실시간 통신에 HTTP 폴링이나 Server-Sent Events도 있는데 왜 WebSocket을 선택했나요?"

### 💡 **답변 전략**

#### **1. 각 방식의 특징**
```
📊 실시간 통신 방식 비교:

HTTP 폴링:
❌ 불필요한 요청 많음 (빈 응답)
❌ 서버 리소스 낭비
❌ 실시간성 부족

Server-Sent Events:
✅ 서버→클라이언트 단방향
❌ 클라이언트→서버 별도 HTTP 필요
❌ 브라우저 연결 수 제한

WebSocket:
✅ 양방향 실시간 통신
✅ 연결 유지로 오버헤드 최소
✅ 채팅에 최적화
```

#### **2. 실제 사용 사례**
```
💬 채팅 시스템 요구사항:
- 실시간 메시지 전송/수신
- 사용자 입장/퇴장 알림
- 관리자 공지사항 브로드캐스트
- 연결 상태 모니터링

→ 양방향 통신이 필수적
```

#### **3. WebSocket 단점 해결**
```
❌ WebSocket 단점과 해결책:

단점: 연결 불안정
해결: 하트비트 + 자동 재연결 로직

단점: 프록시/방화벽 문제
해결: Socket.IO 대신 Native WebSocket + fallback

단점: 인증 복잡
해결: HttpOnly 쿠키 기반 인증
```

---

## Spring Cloud Config vs 환경변수

### 🤔 **예상 질문**
> "환경변수나 application.yml로도 설정 관리가 되는데 왜 Config Server를 도입했나요?"

### 💡 **답변 전략**

#### **1. 설정 관리의 복잡성**
```
🚨 기존 방식의 문제:
- 환경별 설정 파일 관리 복잡
- 민감 정보 평문 저장 위험
- 설정 변경 시 재배포 필요
- API 키 등 민감 정보 Git 커밋 위험
```

#### **2. Config Server 도입 효과**
```
✅ 개선 사항:
- 중앙화된 설정 관리
- 암호화된 민감 정보 저장
- 런타임 설정 변경 가능 (재시작 불필요)
- Git 기반 버전 관리
- 환경별 프로파일 분리
```

#### **3. 실제 적용 사례**
```yaml
# 암호화된 설정 예시
spring:
  datasource:
    password: '{cipher}4f645acb62e6302d47f02b2d3b88c8cd...'
app:
  security:
    jwt:
      secret: '{cipher}38fd22c9b913f2d6069ebb2803ae04a4...'
```

---

## QueryDSL vs JPA Repository

### 🤔 **예상 질문**
> "JPA Repository만 써도 되는데 왜 QueryDSL을 추가로 도입했나요?"

### 💡 **답변 전략**

#### **1. 복잡한 쿼리의 한계**
```
🚨 JPA Repository 한계:
- 복잡한 조건문 처리 어려움
- 동적 쿼리 작성 복잡
- 컴파일 타임 검증 불가
- N+1 문제 해결 어려움
```

#### **2. QueryDSL 도입 이유**
```
✅ QueryDSL 장점:
- 타입 안전한 쿼리 작성
- 컴파일 타임 오류 검출
- 동적 쿼리 작성 용이
- IDE 자동완성 지원
- 복잡한 조인 쿼리 최적화
```

#### **3. 실제 사용 사례**
```java
// JPA Repository (복잡한 조건 처리 어려움)
@Query("SELECT p FROM Post p WHERE " +
       "(:category IS NULL OR p.category = :category) AND " +
       "(:keyword IS NULL OR p.title LIKE %:keyword%)")
List<Post> findPosts(@Param("category") String category, 
                    @Param("keyword") String keyword);

// QueryDSL (타입 안전, 동적 쿼리)
public List<Post> findPosts(String category, String keyword) {
    return queryFactory
        .selectFrom(post)
        .where(
            categoryEq(category),
            titleContains(keyword)
        )
        .orderBy(post.createdAt.desc())
        .fetch();
}
```

---

## React vs Vue/Angular

### 🤔 **예상 질문**
> "Vue나 Angular도 있는데 왜 React를 선택했나요?"

### 💡 **답변 전략**

#### **1. 생태계와 커뮤니티**
```
📊 프레임워크 비교:

React:
✅ 가장 큰 생태계
✅ 풍부한 라이브러리
✅ 높은 채용 수요
✅ 유연한 아키텍처

Vue:
✅ 학습 곡선 완만
❌ 상대적으로 작은 생태계

Angular:
✅ 엔터프라이즈급 기능
❌ 높은 학습 곡선
❌ 무거운 프레임워크
```

#### **2. 프로젝트 특성에 맞는 선택**
```
🎯 프로젝트 요구사항:
- 실시간 채팅 (WebSocket 연동)
- 복잡한 상태 관리
- 다양한 외부 라이브러리 연동
- 빠른 개발 속도

→ React의 유연성과 생태계가 적합
```

---

## 💡 **면접 답변 핵심 전략**

### 🎯 **답변 구조**
1. **문제 상황** → 실제 겪었던 구체적인 문제
2. **대안 검토** → 다른 방법들을 왜 선택하지 않았는지
3. **선택 이유** → 해당 기술의 장점과 프로젝트 적합성
4. **개선 효과** → 수치화된 결과나 구체적인 개선 사항

### 🔑 **핵심 메시지**
- **"적정 기술 선택"**: 과도한 엔지니어링보다는 문제에 딱 맞는 기술
- **"실제 경험 기반"**: 이론이 아닌 실제 겪은 문제와 해결 과정
- **"트레이드오프 인식"**: 모든 기술의 장단점을 이해하고 선택
- **"확장성 고려"**: 현재뿐만 아니라 미래 확장성까지 고려

### 📝 **추가 준비사항**
- 각 기술의 **대안들**과 **트레이드오프** 숙지
- **구체적인 수치**나 **개선 사례** 준비
- **실패 경험**과 **해결 과정**도 함께 준비
- **최신 트렌드**와 **기술 발전 방향** 이해

---

## 🔗 **관련 문서**
- [🔧 기술 스택 상세](./06_TECH-STACK.md)
- [🚀 성능 최적화 전략](./05_optimizations.md)
- [🏊♂️ 풀 설정 전략](./12_pool-configuration-strategy.md)
- [🏗️ 아키텍처 가이드](./03_architecture.md)