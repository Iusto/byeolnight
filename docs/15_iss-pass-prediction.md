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

## 용어 정리

### 데이터 소스 & 표준

| 용어 | 설명 |
|------|------|
| **NORAD** | 북미항공우주방위사령부. 지구 궤도의 인공위성과 우주 파편을 추적·관리하는 기관. 각 위성에 고유 카탈로그 번호를 부여한다 (ISS = 25544) |
| **CelesTrak** | NORAD가 추적하는 위성의 궤도 데이터(TLE)를 무료로 배포하는 웹사이트. API 호출 제한이 없어 소규모 서비스에서 쓰기 적합하다 |
| **TLE (Two-Line Element Set)** | 인공위성의 궤도를 2줄의 숫자로 압축한 표준 형식. 이름 1줄 + 데이터 2줄로 구성되며, SGP4 알고리즘의 입력값으로 사용된다. 대기 항력 등으로 시간이 지나면 정확도가 떨어지기 때문에 주기적 갱신이 필요하다 |

### TLE 내부 필드

| 용어 | 설명 |
|------|------|
| **에포크 (Epoch)** | TLE가 측정된 기준 시각. 이 시점에서 멀어질수록 예측 오차가 커진다. 12시간마다 갱신하는 이유 |
| **궤도 경사각 (Inclination)** | 위성 궤도면이 적도면과 이루는 각도. ISS는 51.6°로, 남위 51.6°~북위 51.6° 사이를 지나간다. 한국(북위 33~38°)에서 관측 가능한 이유 |
| **이심률 (Eccentricity)** | 궤도가 원에 얼마나 가까운지를 나타내는 값. 0이면 완전한 원, 1에 가까우면 길쭉한 타원. ISS는 약 0.0007로 거의 원궤도 |
| **승교점 적경 (RAAN)** | 위성이 남반구에서 북반구로 올라가며 적도를 지나는 지점의 방향. 궤도면의 회전 위치를 결정한다 |
| **평균 운동 (Mean Motion)** | 위성이 하루에 지구를 도는 횟수. ISS는 약 15.5 rev/day → 90분에 1바퀴 |
| **BSTAR 항력계수** | 대기 항력에 의한 궤도 감쇠율. ISS는 저궤도(400km)라 대기 저항을 받아 서서히 고도가 낮아지며, 이 값으로 보정한다 |

### 궤도 예측 알고리즘

| 용어 | 설명 |
|------|------|
| **SGP4 (Simplified General Perturbations 4)** | NORAD가 개발한 궤도 전파 알고리즘. TLE를 입력받아 임의 시각의 위성 위치를 계산한다. 지구 중력장의 비균일성, 대기 항력, 달/태양 중력 등을 반영한다. 알고리즘 자체는 공개 표준이고, 이를 구현한 오픈소스가 여러 언어로 존재한다 |
| **SDP4 (Simplified Deep-space Perturbations 4)** | SGP4의 심우주 버전. 궤도 주기가 225분 이상인 고궤도 위성에 사용된다. ISS는 저궤도(90분)이므로 SGP4를 사용 |
| **predict4java** | SGP4/SDP4를 Java로 구현한 오픈소스 라이브러리 (`com.github.davidmoten:predict4java:1.3.1`). TLE 파싱, 궤도 전파, 관측자 기준 패스 예측까지 제공한다 |
| **궤도 전파 (Propagation)** | TLE의 에포크 시점 궤도를 미래(또는 과거) 시점으로 수학적으로 "외삽"하여 위성 위치를 예측하는 과정 |
| **ECI 좌표 (Earth-Centered Inertial)** | 지구 중심 관성 좌표계. SGP4가 위성 위치를 출력하는 좌표계로, 이후 관측자 기준 수평 좌표(방위각/고도각)로 변환된다 |

### 패스 예측 관련

| 용어 | 설명 |
|------|------|
| **패스 (Pass)** | 위성이 관측자의 지평선 위로 올라왔다가 내려가는 한 번의 통과. ISS 기준 보통 3~10분 정도 지속된다 |
| **AOS (Acquisition of Signal)** | 패스 시작 시점. 위성이 관측자의 지평선 위로 올라오는 순간. 코드에서 `passTime.getStartTime()` |
| **LOS (Loss of Signal)** | 패스 종료 시점. 위성이 지평선 아래로 내려가는 순간. 코드에서 `passTime.getEndTime()` |
| **방위각 (Azimuth)** | 북쪽(0°)을 기준으로 시계 방향으로 잰 수평 방향 각도. 동쪽=90°, 남쪽=180°, 서쪽=270°. 코드에서 이를 8방향(N/NE/E/SE/S/SW/W/NW)으로 변환한다 |
| **고도각 (Elevation)** | 지평선(0°)부터 천정(90°)까지의 수직 각도. 최대 고도각이 높을수록 밝고 오래 보인다. 60° 이상이면 거의 머리 위를 지나가는 것 |
| **최대 고도각 (Max Elevation)** | 한 패스 동안 위성이 도달하는 가장 높은 고도각. 관측 품질(EXCELLENT/GOOD/FAIR/POOR)을 결정하는 핵심 지표 |
| **GroundStationPosition** | predict4java에서 관측자 위치(위도, 경도, 해발고도)를 표현하는 클래스. 코드에서 해발고도는 0으로 고정 |

### 캐싱 관련

| 용어 | 설명 |
|------|------|
| **AtomicReference** | Java의 원자적 참조 변수. 여러 스레드가 동시에 TLE를 읽고 쓸 때 동기화 없이 안전하게 접근할 수 있다 |
| **ConcurrentHashMap** | Java의 스레드 안전 해시맵. 패스 예측 결과를 저장하는 인메모리 캐시로 사용 중 |
| **1도 그리드 캐싱** | 위도/경도를 1도 단위로 반올림하여 같은 격자 안의 요청을 하나의 캐시 키로 합치는 전략. 한국 전체가 약 25개 키로 커버된다 |
| **volatile** | Java의 변수 가시성 보장 키워드. 한 스레드가 변경한 값을 다른 스레드가 즉시 읽을 수 있도록 한다. 위치 캐시처럼 단일 값을 여러 스레드가 공유할 때 사용 |
| **TTL (Time To Live)** | 캐시 데이터의 유효 기간. TLE 캐시는 12시간, 패스 예측 캐시는 2시간, 위치 캐시는 30초 |

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
┌──────────────────────┐     ┌─────────────────┐
│   IssService         │────▶│  wheretheiss.at  │  현재 고도/속도
│                      │     └─────────────────┘
│  ┌────────────────┐  │
│  │ Location Cache │  │  volatile 필드 (TTL 30초)
│  └────────────────┘  │
│  ┌────────────────┐  │
│  │ PassPredictor  │  │  SGP4 궤도 전파 + 패스 계산
│  │ (predict4java) │  │
│  └────────────────┘  │
│  ┌────────────────┐  │
│  │ Pass Cache     │  │  ConcurrentHashMap (1도 그리드, TTL 2시간)
│  └────────────────┘  │
└──────────────────────┘
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

ISS 기능은 3단계 로컬 캐싱을 적용하여 외부 API 호출을 최소화한다.

```
요청 흐름과 캐시 히트 포인트:

사용자 요청 ──→ fetchIssCurrentLocation()
                    │
                    ├─ [HIT] volatile 캐시 (30초 이내) → 즉시 반환
                    └─ [MISS] wheretheiss.at API 호출 → 캐시 갱신

              ──→ calculateNextIssPass()
                    │
                    ├─ [HIT] ConcurrentHashMap (2시간 이내, 같은 그리드) → 즉시 반환
                    └─ [MISS] SGP4 계산
                                │
                                ├─ TleFetchService.getIssTle()
                                │     ├─ [HIT] AtomicReference (12시간 이내) → 즉시 반환
                                │     └─ [MISS] CelesTrak API 호출 → 캐시 갱신
                                │
                                └─ predict4java SGP4 계산 → 캐시 저장
```

### 1. TLE 캐시

| 항목 | 값 |
|------|-----|
| 저장소 | `AtomicReference<TLE>` (인메모리) |
| 정상 갱신 주기 | 12시간 |
| 실패 시 재시도 | 5분 간격 |
| 데이터 소스 | CelesTrak (무료, 무제한) |
| 크기 | TLE 1건 (ISS만) |
| 스케줄러 | `fixedRate=5분`, 조건부 갱신 (TLE 유효 시 스킵) |

설계 결정:

- **TTL 12시간**: CelesTrak은 ISS TLE를 하루 수회 업데이트한다. TLE는 에포크(측정 시점)에서 멀어질수록 예측 오차가 커지는데, ISS 저궤도(400km) 기준 12시간 이내면 위치 오차가 수 km 이내로 패스 예측 정확도에 실질적 영향이 없다. 더 자주 갱신해도 CelesTrak 데이터 자체가 수시간 단위로 바뀌므로 실익이 없다
- **AtomicReference**: TLE는 서버 전체에서 1건만 유지하므로 Map 구조가 필요 없다. `AtomicReference`로 lock-free 읽기/쓰기를 보장하면서 오버헤드를 최소화한다
- **적응형 재시도 (5분)**: 서버 시작 시 CelesTrak이 다운되어 있을 수 있다. 12시간을 기다리는 대신 5분마다 재시도하여 빠르게 복구한다. TLE가 확보되면 자동으로 12시간 주기로 전환된다

### 2. 패스 예측 캐시

| 항목 | 값 |
|------|-----|
| 저장소 | `ConcurrentHashMap<String, CachedPassData>` |
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

설계 결정:

- **TTL 2시간**: ISS 궤도 주기가 약 90분이므로, 2시간이면 최소 1회의 새로운 패스가 발생한다. 너무 짧으면 SGP4 계산이 빈번해지고, 너무 길면 이미 지나간 패스를 보여줄 수 있다. 2시간은 "다음 패스"가 변경되는 시점을 적절히 반영하는 균형점이다
- **1도 그리드**: 위도 1도는 약 111km, 경도 1도는 한국 위도 기준 약 88km에 해당한다. 같은 그리드 안의 두 지점에서 ISS 패스 시각 차이는 수 초~수십 초 이내로, 사용자가 인지할 수 없는 수준이다. 더 세밀한 그리드(0.1도)로 하면 캐시 키가 2,500개로 늘어나 캐시 히트율이 급락한다
- **ConcurrentHashMap**: 위치별로 키가 여러 개 필요하고, 서블릿 스레드에서 동시 접근이 발생하므로 스레드 안전한 Map이 필요하다. 읽기 위주 워크로드에서 `ConcurrentHashMap`이 `synchronized Map`보다 성능이 좋다

### 3. ISS 위치 캐시

| 항목 | 값 |
|------|-----|
| 저장소 | `volatile IssLocationData` + `volatile long` (인메모리) |
| TTL | 30초 |
| 데이터 소스 | wheretheiss.at API |
| 크기 | 1건 (고도, 속도, 위도, 경도) |

ISS 현재 위치(고도/속도)를 반환하는 `fetchIssCurrentLocation()`에 30초 TTL 로컬 캐싱을 적용한다.

```java
private volatile IssLocationData cachedLocation;
private volatile long locationCachedAt;
private static final long LOCATION_CACHE_TTL_MS = TimeUnit.SECONDS.toMillis(30);

private IssLocationData fetchIssCurrentLocation() {
    // 캐시 확인 (30초 TTL)
    IssLocationData cached = cachedLocation;
    if (cached != null && System.currentTimeMillis() - locationCachedAt < LOCATION_CACHE_TTL_MS) {
        return cached;  // 30초 이내 → 외부 호출 없이 반환
    }

    try {
        // wheretheiss.at API 호출
        // ... 성공 시 캐시 갱신
        cachedLocation = location;
        locationCachedAt = System.currentTimeMillis();
        return location;
    } catch (Exception e) {
        return cached;  // 실패 시 이전 캐시 반환 (없으면 null)
    }
}
```

설계 결정:

- **TTL 30초**: ISS는 시속 27,600km로 이동하므로 30초 동안 약 230km 이동한다. 고도/속도 표시 용도로는 30초 오차가 무시할 수 있는 수준이다
- **volatile 필드**: 패스 예측 캐시(`ConcurrentHashMap`)와 달리 키가 1개뿐이므로 `volatile` 필드로 충분하다. `ConcurrentHashMap`보다 메모리/오버헤드가 적다
- **Graceful degradation**: API 실패 시 `null` 대신 이전 캐시 데이터를 반환하여, 일시적 네트워크 오류에도 사용자에게 데이터를 보여줄 수 있다
- **프론트엔드 연동**: 프론트엔드가 5분(`refetchInterval: 5 * 60 * 1000`)마다 자동 갱신하므로, 동시 접속 사용자가 많아도 30초당 최대 1회만 외부 API를 호출한다

```
캐시 효과 (동시 접속 100명, 5분 내):
  Before: 100회 외부 API 호출
  After:  최대 10회 외부 API 호출 (5분 ÷ 30초 = 10)
```

### k6 부하테스트 실측 결과

별도 EC2(t3.micro)에서 k6를 실행하여 ISS 캐싱 성능을 검증했다.

| 지표 | 결과 |
|------|------|
| 총 요청 | 290,792건 |
| 에러율 | 0% |
| 평균 응답시간 | 24.05ms (캐시 히트) |
| p(95) 응답시간 | 51.15ms (**임계값 50ms FAIL**) |
| 초당 처리량 | 1,938 req/s |
| Actuator 캐시 검증 | hit 290,778 / miss 14 (적중률 99.995%) |

miss 14건은 TLE 갱신 주기(12시간)와 패스 예측 캐시 TTL(2시간) 만료 시점에 발생한 정상적인 캐시 미스다. 캐시 미스 시 SGP4 계산이 실행되어 p(95)가 임계값을 초과한다. 개선 방향은 서버 시작 시 주요 도시 그리드 사전 계산(Proactive) 또는 임계값 현실화(60ms)다.

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

wheretheiss.at API에서 ISS 현재 위치를 가져올 때, 응답 필드가 누락되거나 API 호출이 실패할 수 있다. 3단계 방어 전략을 적용한다.

```
방어 단계:

1단계: 캐시 히트 (30초 TTL) → 외부 호출 자체를 하지 않음
2단계: API 응답 필드 검증 → 필드 누락 시 이전 캐시 데이터 반환
3단계: API 호출 실패 → 이전 캐시 데이터 반환 (없으면 null → 폴백 메시지)
```

```java
private IssLocationData fetchIssCurrentLocation() {
    // 1단계: 캐시 확인
    IssLocationData cached = cachedLocation;
    if (cached != null && System.currentTimeMillis() - locationCachedAt < LOCATION_CACHE_TTL_MS) {
        return cached;
    }

    try {
        // ...
        // 2단계: 응답 필드 검증
        if (altNode == null || velNode == null || latNode == null || lonNode == null) {
            return cached;  // 이전 캐시 반환 (null일 수 있음)
        }
        // ...
    } catch (Exception e) {
        // 3단계: 호출 실패 시 이전 캐시 반환
        return cached;
    }
}
```

`fetchIssCurrentLocation()`이 최종적으로 null을 반환하면(캐시도 없는 초기 상태) `getIssObservationOpportunity()`에서 기본 상태 메시지("ISS는 현재 지구 상공 약 400km에서...")를 사용한다. SGP4 패스 계산은 이 API와 독립적이므로 영향 없이 정상 작동한다.

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
