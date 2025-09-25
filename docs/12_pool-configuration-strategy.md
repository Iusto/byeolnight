# 🏊‍♂️ 풀 설정 전략 및 근본적 이유

## 📊 **비즈니스 요구사항 기반 풀 설계**

### 🎯 **예상 사용자 규모**
- **동시 접속자**: 100명 (피크 타임)
- **일일 활성 사용자**: 500명
- **이미지 업로드**: 시간당 50건
- **외부 API 호출**: 분당 200건

---

## 🧵 **쓰레드풀 설계 (AsyncConfig)**

### **현재 문제점**
```java
executor.setCorePoolSize(5);   // 근거 없음
executor.setMaxPoolSize(10);   // 근거 없음
executor.setQueueCapacity(25); // 근거 없음
```

### **근본적 이유 기반 설계**

#### **이미지 검증 쓰레드풀**
- **용도**: Google Vision API 호출 (I/O 집약적)
- **API 제한**: 분당 600건 (Google Vision)
- **평균 응답시간**: 2-3초
- **동시 업로드**: 시간당 50건 = 분당 1건 미만

**권장 설정**:
```java
// 이미지 검증은 I/O 집약적이므로 코어 수보다 많게 설정
executor.setCorePoolSize(3);    // 기본 3개 (평상시 충분)
executor.setMaxPoolSize(8);     // 최대 8개 (피크 타임 대응)
executor.setQueueCapacity(20);  // 대기열 20개 (버퍼)
executor.setKeepAliveSeconds(60); // 유휴 쓰레드 1분 후 정리
```

**계산 근거**:
- 평균 처리시간 3초 × 분당 1건 = 동시 처리 필요 쓰레드 1개
- 피크 타임 5배 증가 고려 = 5개
- 여유분 60% 추가 = 8개

---

## 🔗 **Redis 커넥션풀 설계 (RedissonConfig)**

### **현재 문제점**
```java
.setConnectionMinimumIdleSize(2)  // 근거 없음
.setConnectionPoolSize(5)         // 근거 없음
config.useSingleServer().setPassword() // 설정 체이닝 오류
```

### **근본적 이유 기반 설계**

#### **Redis 사용 용도**
- JWT 토큰 저장/검증
- 세션 관리
- 분산락 (동시성 제어)
- 캐싱

**권장 설정**:
```java
.setConnectionMinimumIdleSize(5)   // 기본 연결 5개 유지
.setConnectionPoolSize(15)         // 최대 15개 (동시 접속자 100명 기준)
.setIdleConnectionTimeout(30000)   // 30초 (빠른 정리)
.setConnectTimeout(5000)           // 5초 (빠른 실패)
```

**계산 근거**:
- 동시 접속자 100명 × 평균 Redis 요청 0.15회/초 = 15 TPS
- 커넥션당 처리 능력 고려하여 15개 설정

---

## 🌐 **HTTP 커넥션풀 설계 (RestTemplateConfig)**

### **현재 문제점**
```java
factory.setConnectTimeout(10000); // 하드코딩
factory.setReadTimeout(15000);    // 하드코딩
// 커넥션 풀 설정 없음
```

### **근본적 이유 기반 설계**

#### **외부 API별 특성**
- **Google Vision**: 2-5초 응답
- **OpenAI**: 3-10초 응답  
- **NASA API**: 1-3초 응답
- **이메일 SMTP**: 5-15초 응답

**권장 설정**:
```java
// Apache HttpClient 기반 커넥션 풀
PoolingHttpClientConnectionManager connectionManager = 
    new PoolingHttpClientConnectionManager();
connectionManager.setMaxTotal(50);        // 전체 최대 50개
connectionManager.setDefaultMaxPerRoute(10); // 호스트당 10개

// 타임아웃 설정 (환경별 주입)
@Value("${app.http.connect-timeout:5000}")
private int connectTimeout;
@Value("${app.http.read-timeout:30000}")  
private int readTimeout;
```

**계산 근거**:
- 외부 API 8개 × 호스트당 10개 = 80개 이론치
- 실제 동시 호출 빈도 고려하여 50개로 조정

---

## 🗄️ **데이터베이스 커넥션풀 설계 (HikariCP)**

### **현재 상태**: Spring Boot 기본값 사용 (최적화 안됨)

### **1단계: 실제 사용 패턴 분석**

#### **Repository 분석 결과 (25개 Repository)**
- **복잡한 쿼리**: QueryDSL 동적 쿼리, 조인, 페이징
- **주요 도메인**: 게시글(5), 사용자(3), 채팅(3), 댓글(3), 로그(5) 등
- **트랜잭션 패턴**: 포인트 지급, 파일 업로드, 게시글 작성

#### **실제 부하 계산**
```
🔍 사용량 추정:
- 동시 접속자: 100명
- 페이지 조회율: 사용자당 분당 3페이지  
- DB 쿼리/페이지: 평균 5개
- 기본 TPS: 100 × 3 × 5 ÷ 60 = 25 TPS
- 피크 TPS: 25 × 3 = 75 TPS
```

### **2단계: Little's Law 적용**

#### **수학적 계산**
```
필요 커넥션 수 = TPS × 평균 응답시간(초)

평상시: 25 TPS × 0.05초 = 1.25개
피크시: 75 TPS × 0.05초 = 3.75개

안전 계수 적용:
- 기본: 4개 (피크시 기준)
- 버퍼 200%: 4 × 3 = 12개  
- 최대 300%: 4 × 5 = 20개
```

### **3단계: 환경별 설정**

#### **로컬 개발 환경**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10      # 개발용 적당한 크기
      minimum-idle: 2            # 최소한만 유지
      idle-timeout: 600000       # 10분 (개발 중 대기)
      connection-timeout: 30000  # 30초 (여유있게)
      max-lifetime: 1800000      # 30분
      leak-detection-threshold: 60000 # 1분 (개발 중 누수 감지)
```

#### **운영 환경**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # 피크 대응
      minimum-idle: 5            # 기본 연결 유지
      idle-timeout: 300000       # 5분 (메모리 절약)
      connection-timeout: 20000  # 20초 (빠른 실패)
      max-lifetime: 1200000      # 20분 (연결 갱신)
      leak-detection-threshold: 30000 # 30초 (빠른 감지)
```

### **4단계: 설정값 근거**

| 설정 | 로컬 | 운영 | 근거 |
|------|------|------|------|
| **maximum-pool-size** | 10 | 20 | Little's Law: 75 TPS × 0.05초 × 5배 여유 |
| **minimum-idle** | 2 | 5 | 기본 연결 유지 (콜드 스타트 방지) |
| **idle-timeout** | 10분 | 5분 | 개발=여유, 운영=메모리 절약 |
| **connection-timeout** | 30초 | 20초 | 개발=디버깅 여유, 운영=빠른 실패 |
| **max-lifetime** | 30분 | 20분 | 연결 갱신 주기 (DB 재시작 대응) |
| **leak-detection** | 1분 | 30초 | 누수 감지 민감도 |

---

## 📈 **모니터링 및 튜닝 지표**

### **측정해야 할 지표**
- **쓰레드풀**: 활성 쓰레드 수, 큐 대기 시간
- **Redis**: 커넥션 사용률, 응답 시간
- **HTTP**: 커넥션 풀 사용률, 타임아웃 발생률
- **DB**: 커넥션 풀 사용률, 대기 시간, 누수 감지

### **HikariCP 모니터링 메트릭**
```
# Actuator 엔드포인트
/actuator/metrics/hikaricp.connections.active
/actuator/metrics/hikaricp.connections.idle  
/actuator/metrics/hikaricp.connections.pending
/actuator/metrics/hikaricp.connections.timeout
/actuator/metrics/hikaricp.connections.usage
```

### **알람 임계값**
- 쓰레드풀 사용률 > 80%
- 커넥션풀 사용률 > 85%
- 평균 응답시간 > 기준치 2배

---

## 🔧 **단계별 적용 계획**

### **1단계**: 설정 외부화
- 하드코딩된 값들을 Config Server로 이동
- 환경별 다른 설정 적용

### **2단계**: 근본적 이유 기반 재설정
- 비즈니스 요구사항 기반 값 산정
- 성능 테스트를 통한 검증

### **3단계**: 모니터링 구축
- 풀 사용률 메트릭 수집
- 알람 및 대시보드 구축

### **4단계**: 지속적 튜닝
- 실제 사용 패턴 기반 조정
- 계절성/이벤트 대응 설정

---

## 💡 **핵심 원칙**

1. **측정 가능한 근거**: 모든 설정값은 비즈니스 요구사항에서 도출
2. **환경별 차별화**: 로컬/개발/운영 환경별 다른 설정
3. **모니터링 기반 튜닝**: 실제 사용 패턴 기반 지속적 개선
4. **Graceful Degradation**: 리소스 부족 시 우아한 성능 저하