# 이메일 인증 비동기 처리 개선

## 문제 상황

### 1. 동기 방식의 문제점
- 인증 코드 발송 API에서 SMTP 전송을 직접 수행
- 메일 서버 지연/장애 시 API 응답이 느려짐
- 동시 요청 증가 시 스레드 풀 고갈 위험

### 2. 보안 취약점
- Redis에 인증 코드 평문 저장
- Redis 접근 권한 유출 시 인증 우회 가능

### 3. 동시성 이슈
- 전송/인증 횟수 제한을 `get → set` 방식으로 처리
- 동시 요청 시 카운트 누락 가능 (race condition)

### 4. SMTP 타임아웃 미설정
- 메일 서버 문제 시 스레드가 장시간 블로킹될 가능성

---

## 해결 방안

### 설계 원칙
1. **인증 API는 항상 빠르게 응답** - 외부 I/O와 분리
2. **인증 코드는 해시로 저장** - Redis 유출에도 안전
3. **원자적 연산 사용** - 동시성 안전 보장
4. **재시도 정책은 워커가 담당** - API 코드와 분리

### 기술 선택

#### Redis Streams 대신 RBlockingQueue를 선택한 이유

**초기 계획: Redis Streams**
- 메시지 ID 기반 추적 가능
- Consumer Group 지원

**문제점 발견**
```java
// Redisson 3.37.0에서 Streams API 사용 시
StreamMessageId messageId = stream.add(data);
// ❌ Map<String, Object> 타입 필요
// ❌ 타입 안전성 부족
```

**최종 선택: RBlockingQueue**
```java
// ✅ 제네릭 지원으로 타입 안전
cacheService.enqueue("queue:mail", emailJob);
EmailJob job = cacheService.dequeue("queue:mail", timeout);

// ✅ Serializable 객체 직렬화 자동 처리
// ✅ 간단하고 명확한 API
```

**트레이드오프**
- ❌ 메시지 ID 추적 불가 (대신 jobId 사용)
- ❌ Consumer Group 미지원 (단일 워커로 충분)
- ✅ 타입 안전성 확보
- ✅ 코드 간결성 향상
- ✅ 프로젝트 규모에 적합

---

## 구현 내용

### 1. SMTP 타임아웃 설정 (5초)
```yaml
spring:
  mail:
    properties:
      mail:
        smtp:
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
```

### 2. SHA-256 해싱으로 인증 코드 보호
```java
private String hashCode(String email, String code) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    String combined = email + ":" + code + ":" + verificationSecret;
    return toHexString(digest.digest(combined.getBytes()));
}

// Redis 저장
cacheService.set("email:" + email, hashedCode, Duration.ofMinutes(5));

// 검증 시
String inputHash = hashCode(email, inputCode);
boolean valid = savedHash.equals(inputHash);
```

**보안 효과:**
- Redis 접근 권한 유출 시에도 원본 코드 알 수 없음
- 이메일별로 다른 해시 생성 (email을 salt로 사용)

### 3. 원자적 카운터로 Rate Limiting
```java
// ❌ 기존: Race Condition 발생 가능
Integer count = cacheService.get(key);
cacheService.set(key, count + 1, ttl);

// ✅ 개선: 원자적 증가
cacheService.incrementAndGet(key, Duration.ofMinutes(10));
```

**동시성 안전 보장:**
- Redis `INCR` 명령 사용 (atomic operation)
- 정확한 횟수 제한 가능

### 4. 비동기 메일 전송 (RBlockingQueue)

#### 인증 API 흐름
```java
public void sendCode(String email) {
    // 1. 코드 생성 및 해싱
    String code = generateCode();
    String hashedCode = hashCode(email, code);
    cacheService.set("email:" + email, hashedCode, Duration.ofMinutes(5));

    // 2. rate limit 증가
    incrementSendAttempts(email);

    // 3. 큐에 작업 추가 (즉시 반환)
    EmailJob job = EmailJob.builder()
        .email(email)
        .subject("[별 헤는 밤] 이메일 인증 코드")
        .htmlBody(createEmailTemplate(code))
        .build();

    cacheService.enqueue("queue:mail", job);
    // ✅ 즉시 응답 반환 (SMTP 대기 없음)
}
```

#### 워커 처리 흐름
```java
@Scheduled(fixedDelay = 1000)
public void processEmailJobs() {
    // 1초마다 큐에서 작업 가져오기
    EmailJob job = cacheService.dequeue("queue:mail", Duration.ofSeconds(1));

    if (job != null) {
        try {
            gmailEmailService.sendHtml(job.getEmail(), job.getSubject(), job.getHtmlBody());
            // 성공 시 완료
        } catch (Exception e) {
            // 실패 시 재시도 (최대 5회)
            if (job.getAttempt() < 5) {
                cacheService.enqueue("queue:mail", job.withRetry(e.getMessage()));
            } else {
                cacheService.enqueue("queue:mail:dlq", job.withFinalFailure(e.getMessage()));
            }
        }
    }
}
```

### 5. DLQ (Dead Letter Queue)
- 5회 재시도 후에도 실패한 작업은 `queue:mail:dlq`로 이동
- 관리자가 수동으로 확인 가능
- 실패 원인 및 시간 기록

---

## 구현 전/후 비교

### AS-IS (동기 방식)
```
[Client] → [API] → [SMTP 전송 (5~10초)] → [Response]
             ↓
      [Redis 평문 저장]
      [get → set 카운터]
```

**문제:**
- API 응답 느림 (SMTP 대기)
- Redis 평문 노출 위험
- Race Condition 가능

### TO-BE (비동기 방식)
```
[Client] → [API] → [해시 저장 + 큐 추가] → [Response (즉시)]
                           ↓
                    [RBlockingQueue]
                           ↓
                      [EmailWorker]
                           ↓
                    [SMTP 전송 (백그라운드)]
                           ↓
                    [실패 시 DLQ]
```

**개선:**
- ✅ API 응답 속도 향상 (100ms 이내)
- ✅ SHA-256 해싱으로 보안 강화
- ✅ 원자적 카운터로 정확한 제한
- ✅ 재시도 로직 분리
- ✅ 타입 안전성 확보 (Serializable DTO)

---

## 성능 개선 효과

### API 응답 시간
- **AS-IS:** 평균 5~10초 (SMTP 대기)
- **TO-BE:** 평균 50~100ms (큐 추가만)

### 동시 처리 능력
- **AS-IS:** 스레드 풀 크기에 제한됨 (예: 200 TPS)
- **TO-BE:** 큐 기반으로 무제한 수용 (큐 크기만큼)

### 안정성
- **AS-IS:** SMTP 장애 시 API 응답 지연
- **TO-BE:** SMTP 장애와 API 응답 완전 분리

---

## 운영 고려사항

### 모니터링 포인트
```java
// 큐 크기 확인
int queueSize = cacheService.getQueueSize("queue:mail");
int dlqSize = cacheService.getQueueSize("queue:mail:dlq");

// DLQ에 작업이 쌓이면 알림 필요
if (dlqSize > 10) {
    log.error("DLQ에 실패 작업 누적: {}", dlqSize);
}
```

### 장애 복구
1. **워커 다운 시:** 큐에 작업이 쌓이지만 유실되지 않음
2. **Redis 다운 시:** 인증 코드/큐 모두 유실 (휘발성)
3. **SMTP 장애 시:** DLQ에 누적 후 수동 재처리

### 확장 가능성
- 현재: 단일 워커로 충분
- 미래: 여러 워커 인스턴스로 수평 확장 가능 (RBlockingQueue는 자동 분산)

---

## 결론

**핵심 개선사항:**
1. ✅ 비동기 처리로 API 응답 속도 10배 이상 개선
2. ✅ SHA-256 해싱으로 보안 강화
3. ✅ 원자적 카운터로 정확한 rate limiting
4. ✅ RBlockingQueue로 타입 안전성 확보
5. ✅ DLQ 패턴으로 실패 작업 추적

**기술 선택의 이유:**
- Redis Streams 대신 RBlockingQueue 선택 → 타입 안전성 우선
- Kafka/RabbitMQ 미사용 → 프로젝트 규모에 적합한 선택
- 단일 워커 구조 → 복잡도 최소화, 필요 시 확장 가능

**트레이드오프 수용:**
- 메시지 ID 추적 불가 → jobId로 대체
- Consumer Group 미지원 → 현재 규모에 불필요
- Redis 휘발성 → 이메일 재전송으로 해결 가능

이 구조는 개인 사이드 프로젝트 규모에 최적화되어 있으며, 향후 트래픽 증가 시 워커 확장으로 대응 가능합니다.
