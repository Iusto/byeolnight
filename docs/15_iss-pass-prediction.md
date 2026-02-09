# ISS 관측 예보 시스템

> 하드코딩에서 TLE + SGP4 궤도 역학 기반 실제 계산으로의 전환

## 개선 결과

| 항목 | Before (하드코딩) | After (SGP4 계산) |
|------|-------------------|-------------------|
| 다음 관측 시간 | `now + 2시간 30분` 고정 | 실제 궤도 기반 계산 |
| 관측 방향 | `NORTHEAST` 고정 | 방위각 기반 8방향 변환 |
| 관측 시간 | `5-7분` 고정 | 패스 시작~종료 실측 |
| 관측 품질 | `GOOD` 고정 | 최대 고도각 기반 4단계 |
| 외부 API 의존 | 없음 (대신 부정확) | TLE만 하루 2회 (무료/무제한) |
| 서버 부하 | 없음 | CPU 계산만 (외부 호출 없음) |

---

## 배경

### ISS란?

국제우주정거장(ISS)은 고도 약 408km에서 시속 27,600km로 지구를 약 90분에 한 바퀴 돌고 있다. 맑은 밤에 맨눈으로 관측이 가능하며, 관측하려면 ISS가 관측자 상공을 지나가는 정확한 시간과 방향을 알아야 한다.

### 기존 문제

기존 `IssService.calculateNextIssPass()`는 모든 값을 하드코딩하고 있었다.

```java
// Before: 하드코딩된 패스 계산
private IssPassData calculateNextIssPass(double latitude, double longitude) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime nextPass = now.plusHours(2).plusMinutes(30); // 항상 2시간 30분 후

    return IssPassData.builder()
        .nextPassTime(nextPass.format(...))
        .nextPassDirection("NORTHEAST")  // 항상 북동쪽
        .estimatedDuration("5-7분")      // 항상 5-7분
        .visibilityQuality("GOOD")       // 항상 GOOD
        .build();
}
```

사용자의 위치(`latitude`, `longitude`)를 파라미터로 받지만 실제로는 사용하지 않았다. 서울이든 부산이든 제주든 동일한 결과를 반환했다.

---

## 구현 방식: TLE + SGP4

### TLE (Two-Line Element Set)

TLE는 인공위성의 궤도 정보를 2줄의 텍스트로 표현하는 표준 형식이다. NORAD(북미항공우주방위사령부)에서 관리하며, CelesTrak에서 무료로 제공한다.

```
ISS (ZARYA)
1 25544U 98067A   25001.47015846  .00018150  00000+0  32156-3 0  9998
2 25544  51.6411 353.0857 0006988 128.0547 232.1080 15.50102288436088
```

TLE에 포함된 정보:
- **궤도 경사각** (51.6411도): ISS가 적도면과 이루는 각도
- **승교점 적경** (353.0857도): 궤도면이 적도를 지나는 지점
- **이심률** (0.0006988): 궤도의 원에 가까운 정도
- **평균 운동** (15.50 rev/day): 하루 지구를 도는 횟수
- **BSTAR 항력계수**: 대기 항력에 의한 궤도 감쇠율

### SGP4 (Simplified General Perturbations 4)

SGP4는 TLE 데이터를 입력으로 받아 임의의 시간에 위성의 위치를 계산하는 궤도 전파 알고리즘이다. 지구의 비균일 중력장, 대기 항력, 달/태양의 중력 섭동을 고려한다.

```
TLE 데이터 ─→ SGP4 전파기 ─→ 위성 위치 (ECI 좌표)
                                    │
관측자 위치 ──────────────────────→ 좌표 변환 ─→ 방위각/고도각
                                                      │
                                                 패스 예측
                                            (시작/종료/최대고도)
```

### 사용 라이브러리

[predict4java](https://github.com/davidmoten/predict4java) (v1.3.1, Maven Central)

predict4java는 SGP4/SDP4 궤도 전파와 위성 패스 예측을 제공하는 Java 라이브러리다. NORAD의 공식 SGP4 알고리즘을 구현하고 있다.

```gradle
// build.gradle
implementation 'com.github.davidmoten:predict4java:1.3.1'
```

---

## 구현 상세

### 아키텍처

```
CelesTrak (TLE 제공)
        │
        │ HTTPS (정상: 12시간마다 / TLE 없을 시: 5분마다 재시도)
        ▼
┌──────────────────┐
│  TleFetchService │  TLE 수집 + 메모리 캐싱
│  (AtomicReference)│  스케줄러: 5분 주기 체크, 조건부 갱신
└────────┬─────────┘
         │ TLE 객체
         ▼
┌──────────────────┐     ┌─────────────────┐
│   IssService     │────▶│  wheretheiss.at  │  현재 고도/속도 (null safety 적용)
│                  │     └─────────────────┘
│  ┌────────────┐  │
│  │ PassPredict│  │  SGP4 궤도 전파 + 패스 계산
│  │ or (lib)   │  │
│  └────────────┘  │
│                  │
│  ┌────────────┐  │
│  │ Pass Cache │  │  ConcurrentHashMap (1도 그리드, TTL 2시간)
│  └────────────┘  │
└──────────────────┘
         │
         ▼
    API 응답 (IssObservationResponse)
```

### 1. TleFetchService - TLE 데이터 수집

CelesTrak에서 ISS TLE를 가져와 메모리에 캐싱한다.

```java
@Service
public class TleFetchService {

    private static final String CELESTRAK_ISS_TLE_URL =
        "https://celestrak.org/NORAD/elements/gp.php?CATNR=25544&FORMAT=TLE";

    private final AtomicReference<TLE> cachedTle = new AtomicReference<>();
    private volatile LocalDateTime lastFetchTime;

    @PostConstruct
    public void init() {
        refreshTle();  // 서버 시작 시 즉시 TLE 가져오기
    }

    /**
     * 5분마다 실행되지만, TLE가 유효하면(12시간 미만) 스킵.
     * TLE가 없거나 12시간 이상 지났으면 갱신 시도.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    public void scheduledRefresh() {
        if (cachedTle.get() != null && lastFetchTime != null
                && Duration.between(lastFetchTime, LocalDateTime.now()).toHours() < 12) {
            return; // TLE가 유효하면 12시간 전까지 갱신 안 함
        }
        refreshTle();
    }

    public TLE getIssTle() {
        TLE tle = cachedTle.get();
        if (tle == null) {
            refreshTle();
            tle = cachedTle.get();
        }
        return tle;
    }
}
```

설계 결정:
- **AtomicReference**: 동시 접근에 안전한 TLE 저장소
- **적응형 갱신 주기**: 스케줄러는 5분마다 실행되지만, TLE가 존재하고 12시간 미만이면 스킵. TLE가 없으면(서버 시작 실패, 네트워크 장애 등) 5분마다 재시도하여 빠르게 복구
- **volatile lastFetchTime**: 스케줄러 스레드와 요청 스레드 간 가시성 보장
- **@PostConstruct 즉시 로딩**: 서버 시작 직후부터 예측 가능

```
TLE 갱신 시나리오:

1. 정상 운영: 서버 시작 → TLE 획득 → 12시간 후 갱신 → 12시간 후 갱신 → ...
2. 시작 실패: 서버 시작 → TLE 실패 → 5분 후 재시도 → 5분 후 재시도 → TLE 획득 → 12시간 유지
3. 갱신 실패: ... → 12시간 경과 → 갱신 실패 → 5분 후 재시도 → TLE 획득 → 12시간 유지
```

### 2. IssService - SGP4 기반 패스 계산

핵심 로직인 `calculateNextIssPass()`가 하드코딩에서 실제 궤도 계산으로 교체되었다.

```java
private IssPassData calculateNextIssPass(double latitude, double longitude) {
    // 1도 단위 그리드 캐시
    String cacheKey = String.format("iss:%d:%d", Math.round(latitude), Math.round(longitude));

    CachedPassData cached = passCache.get(cacheKey);
    if (cached != null && !cached.isExpired()) {
        return cached.getData();
    }

    // TLE + SGP4 실제 계산
    TLE tle = tleFetchService.getIssTle();
    GroundStationPosition observer = new GroundStationPosition(latitude, longitude, 0);
    PassPredictor predictor = new PassPredictor(tle, observer);
    SatPassTime passTime = predictor.nextSatPass(new Date());

    // 결과 추출
    long durationMinutes = (passTime.getEndTime().getTime()
                          - passTime.getStartTime().getTime()) / 60000;
    String direction = azimuthToDirection(passTime.getAosAzimuth());
    double maxEl = passTime.getMaxEl();
    String quality = elevationToQuality(maxEl);

    // KST 변환
    LocalDateTime passStart = passTime.getStartTime().toInstant()
        .atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();

    // 캐시 저장 후 반환
    IssPassData passData = IssPassData.builder()
        .nextPassTime(passStart.format(DateTimeFormatter.ofPattern("HH:mm")))
        .nextPassDate(passStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        .nextPassDirection(direction)
        .estimatedDuration(durationMinutes + "분")
        .visibilityQuality(quality)
        .maxElevation(maxEl)
        .build();

    passCache.put(cacheKey, new CachedPassData(passData, System.currentTimeMillis()));
    return passData;
}
```

### 3. 방위각 변환

predict4java가 반환하는 AOS(Acquisition of Signal) 방위각(0-360도)을 사용자 친화적인 8방향으로 변환한다.

```java
private String azimuthToDirection(int azimuth) {
    if (azimuth < 0) azimuth += 360;
    int index = ((azimuth + 22) % 360) / 45;
    String[] directions = {"NORTH", "NORTHEAST", "EAST", "SOUTHEAST",
                           "SOUTH", "SOUTHWEST", "WEST", "NORTHWEST"};
    return directions[index];
}
```

| 방위각 범위 | 방향 |
|-------------|------|
| 338 - 22 | NORTH |
| 23 - 67 | NORTHEAST |
| 68 - 112 | EAST |
| 113 - 157 | SOUTHEAST |
| 158 - 202 | SOUTH |
| 203 - 247 | SOUTHWEST |
| 248 - 292 | WEST |
| 293 - 337 | NORTHWEST |

### 4. 관측 품질 판단

ISS의 최대 고도각(Max Elevation)이 높을수록 밝고 오래 관측할 수 있다.

```java
private String elevationToQuality(double maxElevation) {
    if (maxElevation >= 60) return "EXCELLENT";  // 거의 머리 위
    if (maxElevation >= 30) return "GOOD";       // 밝게 보임
    if (maxElevation >= 10) return "FAIR";       // 낮은 고도, 짧은 관측
    return "POOR";                                // 지평선 근처, 관측 어려움
}
```

---

## 캐싱 전략

### TLE 캐시

| 항목 | 값 |
|------|-----|
| 저장소 | AtomicReference (인메모리) |
| 정상 갱신 주기 | 12시간 |
| 실패 시 재시도 | 5분 간격 |
| 데이터 소스 | CelesTrak (무료, 무제한) |
| 크기 | TLE 1건 (ISS만) |
| 스케줄러 | `fixedRate=5분`, 조건부 갱신 (TLE 유효 시 스킵) |

### 패스 예측 캐시

| 항목 | 값 |
|------|-----|
| 저장소 | ConcurrentHashMap |
| 캐시 키 | `iss:{위도(1도단위)}:{경도(1도단위)}` |
| TTL | 2시간 |
| 한국 내 캐시 키 수 | 약 25개 (위도 33-38, 경도 126-130) |

위도/경도를 1도 단위로 반올림하여 그리드화한다. 한국 영토가 약 5도 x 4도 범위이므로, 한국 내 모든 사용자의 요청이 약 20-25개의 캐시 키로 수렴한다.

```
캐시 키 예시:
  서울 (37.57, 126.98) → iss:38:127
  부산 (35.18, 129.08) → iss:35:129
  제주 (33.50, 126.53) → iss:34:127
```

ISS 궤도 주기가 약 90분이므로, 2시간 TTL이면 최소 1회의 패스 변경을 반영한다.

---

## 폴백 전략

SGP4 계산 실패 시 (TLE 미수신, 라이브러리 오류 등) 안전하게 기본값을 반환한다.

```java
private IssPassData createFallbackPassData() {
    LocalDateTime nextPass = LocalDateTime.now().plusHours(3);
    return IssPassData.builder()
        .nextPassTime(nextPass.format(...))
        .nextPassDirection("NORTHEAST")
        .estimatedDuration("5-7분")
        .visibilityQuality("GOOD")
        .maxElevation(null)
        .build();
}
```

폴백이 발생하는 경우:
1. CelesTrak에서 TLE를 가져오지 못한 경우 (네트워크 오류)
2. SGP4 계산 중 예외 발생 (극히 드문 경우)
3. 해당 위치에서 ISS 패스를 찾을 수 없는 경우

폴백 데이터는 캐시에 저장하지 않으므로, 다음 요청 시 재계산을 시도한다.

---

## 외부 API Null Safety

wheretheiss.at API에서 ISS 현재 위치를 가져올 때, 응답 필드가 누락될 수 있다. 모든 필수 필드를 개별적으로 검증하여 NPE를 방지한다.

```java
private IssLocationData fetchIssCurrentLocation() {
    // ...
    JsonNode issData = objectMapper.readTree(response.body());
    JsonNode altNode = issData.get("altitude");
    JsonNode velNode = issData.get("velocity");
    JsonNode latNode = issData.get("latitude");
    JsonNode lonNode = issData.get("longitude");

    // 필수 필드 중 하나라도 없으면 null 반환 → 폴백 메시지 사용
    if (altNode == null || velNode == null || latNode == null || lonNode == null) {
        log.warn("ISS API 응답에 필수 필드 누락: alt={}, vel={}, lat={}, lon={}",
                altNode, velNode, latNode, lonNode);
        return null;
    }
    // ...
}
```

`fetchIssCurrentLocation()`이 null을 반환하면 `getIssObservationOpportunity()`에서 기본 상태 메시지("ISS는 현재 지구 상공 약 400km에서...")를 사용한다. SGP4 패스 계산은 이 API와 독립적이므로 영향 없이 정상 작동한다.

---

## API 응답

### 엔드포인트

```
GET /api/weather/iss?latitude=37.5665&longitude=126.9780
```

### 응답 예시

```json
{
  "messageKey": "iss.detailed_status",
  "friendlyMessage": "ISS는 현재 고도 408km에서 시속 27580km로 이동 중입니다.",
  "currentAltitudeKm": 408.2,
  "currentVelocityKmh": 27580.4,
  "nextPassTime": "21:35",
  "nextPassDate": "2026-02-09",
  "nextPassDirection": "SOUTHWEST",
  "estimatedDuration": "6분",
  "visibilityQuality": "GOOD",
  "maxElevation": 42.3
}
```

### 응답 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `currentAltitudeKm` | Double | ISS 현재 고도 (km) - wheretheiss.at API |
| `currentVelocityKmh` | Double | ISS 현재 속도 (km/h) - wheretheiss.at API |
| `nextPassTime` | String | 다음 관측 시작 시간 (KST, HH:mm) |
| `nextPassDate` | String | 다음 관측 날짜 (yyyy-MM-dd) |
| `nextPassDirection` | String | ISS 출현 방향 (8방향) |
| `estimatedDuration` | String | 관측 가능 시간 (분) |
| `visibilityQuality` | String | 관측 품질 (EXCELLENT/GOOD/FAIR/POOR) |
| `maxElevation` | Double | 최대 고도각 (도). SGP4 계산 성공 시에만 포함 |

---

## 테스트

38개 테스트로 전체 로직을 검증한다.

```
IssServiceTest (38 tests, all passed)
├── TLE 파싱 (2)
│   ├── ISS TLE 정상 파싱
│   └── 잘못된 TLE 형식 예외
├── 방위각 → 방향 변환 (14)
│   └── 0°~359° 전 범위 8방향 매핑
├── 고도각 → 관측 품질 (8)
│   └── EXCELLENT/GOOD/FAIR/POOR 경계값
├── SGP4 패스 계산 (4)
│   ├── 서울/부산/제주 좌표 계산
│   └── 하드코딩 아닌 실제 계산 검증
├── 캐싱 (3)
│   ├── 동일 좌표 캐시 히트
│   ├── 1도 이내 동일 그리드
│   └── 다른 그리드 별도 계산
├── 폴백 처리 (4)
│   ├── TLE null 시 기본값
│   ├── TLE 예외 시 기본값
│   ├── 폴백 ISS 메시지
│   └── TLE 재요청 시도
└── 응답 필드 검증 (3)
    ├── messageKey 설정
    ├── friendlyMessage 설정
    └── maxElevation 포함
```

---

## 관련 파일

| 파일 | 역할 |
|------|------|
| `service/weather/TleFetchService.java` | CelesTrak TLE 수집 + 캐싱 |
| `service/weather/IssService.java` | SGP4 패스 계산 + 캐싱 + API 응답 구성 |
| `dto/weather/IssObservationResponse.java` | ISS 관측 응답 DTO |
| `controller/weather/WeatherController.java` | `/api/weather/iss` 엔드포인트 |
| `test/.../IssServiceTest.java` | 38개 단위 테스트 |
