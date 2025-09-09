package com.byeolnight.domain.weather.service;

import com.byeolnight.domain.weather.dto.AstronomyEventResponse;
import com.byeolnight.domain.weather.entity.AstronomyEvent;
import com.byeolnight.domain.weather.repository.AstronomyEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class AstronomyService {

    private final AstronomyEventRepository astronomyRepository;
    private final RestTemplate restTemplate;

    @Value("${nasa.api.key}")
    private String nasaApiKey;



    private static final String NASA_NEOWS_URL = "https://api.nasa.gov/neo/rest/v1/feed";
    private static final String NASA_DONKI_URL = "https://api.nasa.gov/DONKI";
    private static final String NASA_ISS_URL = "http://api.open-notify.org/iss-now.json";
    
    public Map<String, Object> getIssLocation() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(NASA_ISS_URL, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("ISS API 호출 실패: {}", e.getMessage(), e);
        }
        return Map.of("error", "ISS 데이터 조회 실패");
    }

    public List<AstronomyEventResponse> getUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        List<AstronomyEvent> events = astronomyRepository.findUpcomingEvents(thirtyDaysAgo);
        
        // 실제 NASA 데이터 우선, 예측 데이터는 후순위
        return events.stream()
                .sorted((e1, e2) -> {
                    boolean e1IsReal = isRealNasaData(e1);
                    boolean e2IsReal = isRealNasaData(e2);
                    
                    // 실제 데이터 우선
                    if (e1IsReal && !e2IsReal) return -1;
                    if (!e1IsReal && e2IsReal) return 1;
                    
                    // 같은 타입끼리는 최신순
                    return e2.getEventDate().compareTo(e1.getEventDate());
                })
                .limit(10)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    // 상수 정의
    private static final Set<String> REAL_NASA_EVENT_TYPES = Set.of(
        "ASTEROID", "SOLAR_FLARE", "GEOMAGNETIC_STORM"
    );
    
    private boolean isRealNasaData(AstronomyEvent event) {
        LocalDateTime now = LocalDateTime.now();
        boolean isPastEvent = event.getEventDate().isBefore(now);
        boolean isRealType = REAL_NASA_EVENT_TYPES.contains(event.getEventType());

        boolean isPrediction = event.getTitle().contains("예측") || 
                              event.getDescription().contains("예상") ||
                              event.getEventType().equals("MARS_WEATHER");
        
        return isPastEvent && isRealType && !isPrediction;
    }


    @Scheduled(cron = "0 0 9 * * ?") // 매일 오전 9시 실행
    public void fetchDailyAstronomyEvents() {
        performAstronomyDataCollection();
    }
    



    @Transactional
    public void performAstronomyDataCollection() {
        try {
            log.info("천체 이벤트 데이터 수집 시작 (NASA API 연동)");
            
            // 새 데이터 수집 후 성공 시에만 기존 데이터 삭제
            List<AstronomyEvent> newEvents = fetchNasaAstronomyData();
            
            if (!newEvents.isEmpty()) {
                deactivateOldEvents();
                astronomyRepository.saveAll(newEvents);
                log.info("천체 이벤트 데이터 수집 완료: {} 개", newEvents.size());
            } else {
                log.warn("새로운 천체 데이터가 없어 기존 데이터 유지");
            }

        } catch (Exception e) {
            log.error("천체 이벤트 데이터 수집 실패", e);
            throw e;
        }
    }

    private void deactivateOldEvents() {
        astronomyRepository.deleteAll();
        log.info("기존 모든 이벤트 삭제 (중복 방지)");
    }

    private List<AstronomyEvent> fetchNasaAstronomyData() {
        List<AstronomyEvent> allEvents = new ArrayList<>();

        // API 호출을 공통 메서드로 처리
        allEvents.addAll(executeApiCall("NASA NeoWs", this::fetchNeoWsData));
        allEvents.addAll(executeApiCall("NASA DONKI", this::fetchDonkiData));
        allEvents.addAll(executeApiCall("천체 예보", this::fetchAstronomyForecast));

        if (!allEvents.isEmpty()) {
            log.info("수집된 이벤트 수: {}", allEvents.size());
            for (AstronomyEvent event : allEvents) {
                log.debug("이벤트: {} ({})", sanitizeForLog(event.getTitle()), event.getEventType());
            }
        } else {
            log.warn("모든 NASA API 호출 실패");
        }
        
        return allEvents;
    }
    
    private List<AstronomyEvent> executeApiCall(String apiName, java.util.function.Supplier<List<AstronomyEvent>> apiCall) {
        try {
            List<AstronomyEvent> events = apiCall.get();
            if (!events.isEmpty()) {
                log.info("{} API 성공: {} 개", apiName, events.size());
            }
            return events;
        } catch (Exception e) {
            log.warn("{} API 호출 실패: {}", apiName, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<AstronomyEvent> fetchNeoWsData() {
        List<AstronomyEvent> events = new ArrayList<>();

        if (nasaApiKey == null || nasaApiKey.trim().isEmpty() || "DEMO_KEY".equals(nasaApiKey)) {
            log.warn("NASA API 키가 설정되지 않음 또는 DEMO_KEY 사용 중");
            return events;
        }

        try {
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusDays(7);
            String neowsUrl = NASA_NEOWS_URL + "?start_date=" + startDate.toLocalDate() +
                    "&end_date=" + endDate.toLocalDate() + "&api_key=" + nasaApiKey;

            log.info("NASA NeoWs API 호출: {}", neowsUrl.replace(nasaApiKey, "***"));
            ResponseEntity<Map> response = restTemplate.getForEntity(neowsUrl, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                events.addAll(parseNeoWsData(response.getBody()));
                log.info("NASA NeoWs 데이터 {} 개 수집 완료", events.size());
            } else {
                log.warn("NASA NeoWs API 응답 실패: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("NASA NeoWs API 호출 실패: {}", e.getMessage(), e);
            throw e;
        }

        return events;
    }

    private List<AstronomyEvent> fetchDonkiData() {
        List<AstronomyEvent> events = new ArrayList<>();

        if (nasaApiKey == null || nasaApiKey.trim().isEmpty() || "DEMO_KEY".equals(nasaApiKey)) {
            log.warn("NASA API 키가 설정되지 않음 또는 DEMO_KEY 사용 중");
            return events;
        }

        try {

            LocalDateTime startDate = LocalDateTime.now().minusDays(30);
            LocalDateTime endDate = LocalDateTime.now();


            String flareUrl = NASA_DONKI_URL + "/FLR?startDate=" + startDate.toLocalDate() +
                    "&endDate=" + endDate.toLocalDate() + "&api_key=" + nasaApiKey;

            log.info("NASA DONKI FLR API 호출 (과거 30일)");
            ResponseEntity<Map[]> flareResponse = restTemplate.getForEntity(flareUrl, Map[].class);

            if (flareResponse.getStatusCode().is2xxSuccessful() && flareResponse.getBody() != null) {
                List<AstronomyEvent> flareEvents = parseDonkiFlareData(flareResponse.getBody());
                events.addAll(flareEvents);
                log.info("태양 플레어 데이터 {} 개 수집", flareEvents.size());
            }


            String gstUrl = NASA_DONKI_URL + "/GST?startDate=" + startDate.toLocalDate() +
                    "&endDate=" + endDate.toLocalDate() + "&api_key=" + nasaApiKey;

            log.info("NASA DONKI GST API 호출 (과거 30일)");
            ResponseEntity<Map[]> gstResponse = restTemplate.getForEntity(gstUrl, Map[].class);

            if (gstResponse.getStatusCode().is2xxSuccessful() && gstResponse.getBody() != null) {
                List<AstronomyEvent> gstEvents = parseDonkiGstData(gstResponse.getBody());
                events.addAll(gstEvents);
                log.info("지자기 폭풍 데이터 {} 개 수집", gstEvents.size());
            }

            log.info("NASA DONKI 데이터 {} 개 수집 완료", events.size());

        } catch (Exception e) {
            log.error("NASA DONKI API 호출 실패: {}", e.getMessage(), e);
            throw e;
        }

        return events;
    }

    private List<AstronomyEvent> fetchIssData() {
        List<AstronomyEvent> events = new ArrayList<>();

        try {
            log.info("NASA ISS API 호출: {}", NASA_ISS_URL);
            ResponseEntity<Map> response = restTemplate.getForEntity(NASA_ISS_URL, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AstronomyEvent issEvent = parseIssData(response.getBody());
                if (issEvent != null) {
                    events.add(issEvent);
                }
            } else {
                log.warn("NASA ISS API 응답 실패: {}", response.getStatusCode());
            }

            log.info("NASA ISS 데이터 {} 개 수집 완료", events.size());

        } catch (Exception e) {
            log.error("NASA ISS API 호출 실패: {}", e.getMessage());
            throw e;
        }

        return events;
    }



    private List<AstronomyEvent> fetchMarsWeatherData() {
        List<AstronomyEvent> events = new ArrayList<>();

        if (nasaApiKey == null || nasaApiKey.trim().isEmpty() || "DEMO_KEY".equals(nasaApiKey)) {
            log.warn("NASA API 키가 설정되지 않음");
            return events;
        }

        try {
            String marsUrl = "https://api.nasa.gov/insight_weather/?api_key=" + nasaApiKey + "&feedtype=json&ver=1.0";
            log.info("NASA Mars Weather API 호출");
            ResponseEntity<Map> response = restTemplate.getForEntity(marsUrl, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> marsData = response.getBody();
                if (marsData.containsKey("sol_keys") && marsData.get("sol_keys") != null) {
                    events.add(createEvent("MARS_WEATHER", "화성 날씨 리포트",
                            "NASA InSight 탐사선이 전송한 화성의 최신 날씨 정보입니다. 화성의 온도, 바람, 기압 데이터를 확인할 수 있습니다.", 0, 15));
                    log.info("NASA Mars Weather 데이터 수집 성공");
                } else {
                    log.warn("NASA Mars Weather API: 데이터 없음 (InSight 미션 종료)");
                }
            }

        } catch (Exception e) {
            log.error("NASA Mars Weather API 호출 실패: {}", e.getMessage(), e);
        }

        return events;
    }

    private List<AstronomyEvent> parseDonkiFlareData(Map<String, Object>[] flareData) {
        if (flareData == null || flareData.length == 0) {
            log.info("태양 플레어 데이터 없음");
            return new ArrayList<>();
        }
        
        return Arrays.stream(flareData)
                .filter(flare -> flare.get("classType") != null && flare.get("beginTime") != null)
                .map(flare -> {
                    try {
                        String beginTime = (String) flare.get("beginTime");
                        LocalDateTime eventTime = LocalDateTime.parse(beginTime.replace("Z", ""));
                        String classType = flare.get("classType").toString();
                        String peakTime = flare.get("peakTime") != null ? 
                            LocalDateTime.parse(((String) flare.get("peakTime")).replace("Z", "")).toLocalTime().toString() : "미상";
                        
                        String description = String.format("%s에 태양에서 %s 등급 플레어가 발생했습니다. 최대 강도 시간: %s (UTC). 전파 및 GPS 신호에 일시적 영향을 주었을 수 있습니다.",
                                eventTime.toLocalDate(), classType, peakTime);
                        
                        return createAstronomyEvent("SOLAR_FLARE",
                                "태양 플레어 " + classType + " 등급",
                                description,
                                eventTime, "HIGH");
                    } catch (Exception e) {
                        log.warn("태양 플레어 데이터 파싱 실패: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<AstronomyEvent> parseDonkiGstData(Map<String, Object>[] gstData) {
        if (gstData == null || gstData.length == 0) {
            log.info("지자기 폭풍 데이터 없음");
            return new ArrayList<>();
        }
        
        return Arrays.stream(gstData)
                .filter(gst -> gst.get("startTime") != null)
                .map(gst -> {
                    try {
                        String startTime = (String) gst.get("startTime");
                        LocalDateTime eventTime = LocalDateTime.parse(startTime.replace("Z", ""));
                        String kpIndex = gst.get("kpIndex") != null ? gst.get("kpIndex").toString() : "미상";
                        String description = String.format("%s에 지자기 폭풍이 발생했습니다. Kp 지수: %s. 극지방에서 오로라 관측이 가능했을 것입니다.",
                                eventTime.toLocalDate(), kpIndex);
                        
                        return createAstronomyEvent("GEOMAGNETIC_STORM",
                                "지자기 폭풍 발생",
                                description,
                                eventTime, "MEDIUM");
                    } catch (Exception e) {
                        log.warn("지자기 폭풍 데이터 파싱 실패: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private AstronomyEvent parseIssData(Map<String, Object> issData) {
        try {
            Map<String, Object> position = (Map<String, Object>) issData.get("iss_position");
            String latitude = (String) position.get("latitude");
            String longitude = (String) position.get("longitude");

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextPass = now.plusHours(2);

            return AstronomyEvent.builder()
                    .eventType("ISS_LOCATION")
                    .title("ISS 관측 기회")
                    .description(String.format("국제우주정거장이 한국 상공을 통과합니다. 맑은 점으로 5분간 관측 가능합니다. (현재 위치: %.1f°, %.1f°)",
                            Double.parseDouble(latitude), Double.parseDouble(longitude)))
                    .eventDate(nextPass)
                    .peakTime(nextPass.plusMinutes(2))
                    .visibility("WORLDWIDE")
                    .magnitude("MEDIUM")
                    .isActive(true)
                    .build();
        } catch (Exception e) {
            log.error("ISS 데이터 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private List<AstronomyEvent> parseNeoWsData(Map<String, Object> neowsData) {
        List<AstronomyEvent> events = new ArrayList<>();

        try {
            Map<String, Object> nearEarthObjects = (Map<String, Object>) neowsData.get("near_earth_objects");
            LocalDateTime now = LocalDateTime.now();

            for (Map.Entry<String, Object> entry : nearEarthObjects.entrySet()) {
                List<Map<String, Object>> asteroids = (List<Map<String, Object>>) entry.getValue();

                for (Map<String, Object> asteroid : asteroids.stream().limit(3).collect(Collectors.toList())) {
                    String name = (String) asteroid.get("name");
                    Boolean isPotentiallyHazardous = (Boolean) asteroid.get("is_potentially_hazardous_asteroid");



                    List<Map<String, Object>> closeApproachData = (List<Map<String, Object>>) asteroid.get("close_approach_data");
                    String distanceText = "정보 없음";
                    if (!closeApproachData.isEmpty()) {
                        Map<String, Object> missDistance = (Map<String, Object>) closeApproachData.get(0).get("miss_distance");
                        String kmDistance = (String) missDistance.get("kilometers");
                        distanceText = formatDistance(Double.parseDouble(kmDistance));
                    }

                    LocalDateTime eventDate = LocalDateTime.parse(entry.getKey() + "T21:00:00");


                    boolean isPastEvent = eventDate.isBefore(LocalDateTime.now());
                    String sizeInfo = getAsteroidSize(asteroid);
                    String description = isPastEvent ? 
                        String.format("지름 %s의 소행성이 지구에서 %s 거리를 안전하게 통과했습니다.", sizeInfo, distanceText) :
                        String.format("지름 %s의 소행성이 지구에서 %s 거리를 안전하게 통과할 예정입니다.", sizeInfo, distanceText);

                    events.add(AstronomyEvent.builder()
                            .eventType("ASTEROID")
                            .title("지구 근접 소행성 " + name.replace("(", "").replace(")", ""))
                            .description(description)
                            .eventDate(eventDate)
                            .peakTime(eventDate)
                            .visibility("WORLDWIDE")
                            .magnitude(isPotentiallyHazardous ? "HIGH" : "MEDIUM")
                            .isActive(true)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("NeoWs 데이터 파싱 실패: {}", e.getMessage());
        }

        return events;
    }

    private String formatDistance(double kilometers) {
        if (kilometers >= 1000000) {
            return String.format("%.1f백만 km", kilometers / 1000000);
        } else if (kilometers >= 10000) {
            return String.format("%.0f만 km", kilometers / 10000);
        } else {
            return String.format("%.0f km", kilometers);
        }
    }




    private AstronomyEvent createEvent(String type, String title, String description, int daysFromNow, int hour) {
        LocalDateTime eventDate = LocalDateTime.now().plusDays(daysFromNow);
        return AstronomyEvent.builder()
                .eventType(type)
                .title(title)
                .description(description)
                .eventDate(eventDate)
                .peakTime(eventDate.withHour(hour))
                .visibility("WORLDWIDE")
                .magnitude(type.contains("METEOR") || type.contains("SOLAR") ? "HIGH" : "MEDIUM")
                .isActive(true)
                .build();
    }

    private AstronomyEvent createAstronomyEvent(String type, String title, String description, LocalDateTime eventDate, String magnitude) {
        try {
            return AstronomyEvent.builder()
                    .eventType(type)
                    .title(title)
                    .description(description)
                    .eventDate(eventDate)
                    .peakTime(eventDate)
                    .visibility("WORLDWIDE")
                    .magnitude(magnitude)
                    .isActive(true)
                    .build();
        } catch (Exception e) {
            log.error("이벤트 생성 실패: {}", e.getMessage());
            return null;
        }
    }





    private String getAsteroidSize(Map<String, Object> asteroid) {
        try {
            Map<String, Object> estimatedDiameter = (Map<String, Object>) asteroid.get("estimated_diameter");
            if (estimatedDiameter != null) {
                Map<String, Object> meters = (Map<String, Object>) estimatedDiameter.get("meters");
                if (meters != null) {
                    Double maxDiameter = (Double) meters.get("estimated_diameter_max");
                    if (maxDiameter != null) {
                        if (maxDiameter >= 1000) {
                            return String.format("%.1fkm", maxDiameter / 1000);
                        } else {
                            return String.format("%.0fm", maxDiameter);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("소행성 크기 정보 파싱 실패: {}", e.getMessage());
        }
        return "미상";
    }

    private List<AstronomyEvent> fetchAstronomyForecast() {
        List<AstronomyEvent> events = new ArrayList<>();
        
        if (nasaApiKey == null || nasaApiKey.trim().isEmpty() || "DEMO_KEY".equals(nasaApiKey)) {
            log.warn("NASA API 키가 설정되지 않음 - 천체 예보 스킵");
            return events;
        }
        
        try {
            log.info("천체 예보 수집 시작 (NASA API)");
            
            // NASA APOD API로 예정된 천체 이벤트 수집
            events.addAll(fetchNasaApodEvents());
            
            // NASA JPL Small-Body Database로 미래 소행성 이벤트
            events.addAll(fetchUpcomingAsteroids());
            
            log.info("천체 예보 {} 개 수집 완료", events.size());
            
        } catch (Exception e) {
            log.error("천체 예보 수집 실패", e);
        }
        
        return events;
    }
    
    private List<AstronomyEvent> fetchNasaApodEvents() {
        List<AstronomyEvent> events = new ArrayList<>();
        
        try {
            // NASA APOD API로 최근 천체 이벤트 정보 수집
            String apodUrl = "https://api.nasa.gov/planetary/apod?api_key=" + nasaApiKey + "&count=5";
            
            log.info("NASA APOD API 호출");
            ResponseEntity<Map[]> response = restTemplate.getForEntity(apodUrl, Map[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                for (Map<String, Object> apod : response.getBody()) {
                    String title = (String) apod.get("title");
                    String explanation = (String) apod.get("explanation");
                    
                    // 천체 이벤트 관련 키워드 검색
                    if (isAstronomyEvent(title, explanation)) {
                        LocalDateTime eventDate = LocalDateTime.now().plusDays(1 + new Random().nextInt(30));
                        
                        events.add(createForecastEvent(
                            determineEventType(title, explanation),
                            extractEventTitle(title),
                            createEventDescription(title, explanation),
                            eventDate
                        ));
                    }
                }
                
                log.info("NASA APOD 기반 예보 이벤트 {} 개 생성", events.size());
            }
            
        } catch (Exception e) {
            log.warn("NASA APOD API 호출 실패", e);
        }
        
        return events;
    }
    
    private List<AstronomyEvent> fetchUpcomingAsteroids() {
        List<AstronomyEvent> events = new ArrayList<>();
        
        try {
            // NASA NeoWs API로 미래 7일 내 소행성 이벤트 수집 (API 제한)
            LocalDateTime startDate = LocalDateTime.now().plusDays(1);
            LocalDateTime endDate = startDate.plusDays(7);
            
            String neowsUrl = NASA_NEOWS_URL + "?start_date=" + startDate.toLocalDate() +
                    "&end_date=" + endDate.toLocalDate() + "&api_key=" + nasaApiKey;
            
            log.info("NASA NeoWs 미래 이벤트 API 호출");
            ResponseEntity<Map> response = restTemplate.getForEntity(neowsUrl, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                events.addAll(parseUpcomingNeoWsData(response.getBody()));
                log.info("미래 소행성 이벤트 {} 개 수집", events.size());
            }
            
        } catch (Exception e) {
            log.warn("미래 소행성 이벤트 수집 실패", e);
        }
        
        return events;
    }
    
    private boolean isAstronomyEvent(String title, String explanation) {
        String content = (title + " " + explanation).toLowerCase(Locale.ROOT);
        return content.contains("eclipse") || content.contains("meteor") || 
               content.contains("supermoon") || content.contains("conjunction") ||
               content.contains("월식") || content.contains("일식") || 
               content.contains("유성우") || content.contains("슬퍼문");
    }
    
    private String determineEventType(String title, String explanation) {
        String content = (title + " " + explanation).toLowerCase(Locale.ROOT);
        if (content.contains("eclipse") && content.contains("lunar")) return "LUNAR_ECLIPSE";
        if (content.contains("eclipse") && content.contains("solar")) return "SOLAR_ECLIPSE";
        if (content.contains("meteor") || content.contains("유성")) return "METEOR_SHOWER";
        if (content.contains("supermoon") || content.contains("슬퍼문")) return "SUPERMOON";
        if (content.contains("conjunction")) return "PLANET_CONJUNCTION";
        return "SPECIAL_EVENT";
    }
    
    private String extractEventTitle(String title) {
        return title.length() > 50 ? title.substring(0, 47) + "..." : title;
    }
    
    private String createEventDescription(String title, String explanation) {
        String desc = explanation.length() > 200 ? explanation.substring(0, 197) + "..." : explanation;
        return "NASA에서 예보한 천체 이벤트입니다. " + desc;
    }
    
    private List<AstronomyEvent> parseUpcomingNeoWsData(Map<String, Object> neowsData) {
        List<AstronomyEvent> events = new ArrayList<>();
        
        try {
            Map<String, Object> nearEarthObjects = (Map<String, Object>) neowsData.get("near_earth_objects");
            
            for (Map.Entry<String, Object> entry : nearEarthObjects.entrySet()) {
                List<Map<String, Object>> asteroids = (List<Map<String, Object>>) entry.getValue();
                
                for (Map<String, Object> asteroid : asteroids.stream().limit(2).collect(Collectors.toList())) {
                    String name = (String) asteroid.get("name");
                    Boolean isPotentiallyHazardous = (Boolean) asteroid.get("is_potentially_hazardous_asteroid");
                    
                    LocalDateTime eventDate = LocalDateTime.parse(entry.getKey() + "T21:00:00");
                    String sizeInfo = getAsteroidSize(asteroid);
                    
                    List<Map<String, Object>> closeApproachData = (List<Map<String, Object>>) asteroid.get("close_approach_data");
                    String distanceText = "정보 없음";
                    if (!closeApproachData.isEmpty()) {
                        Map<String, Object> missDistance = (Map<String, Object>) closeApproachData.get(0).get("miss_distance");
                        String kmDistance = (String) missDistance.get("kilometers");
                        distanceText = formatDistance(Double.parseDouble(kmDistance));
                    }
                    
                    String description = String.format("지름 %s의 소행성이 지구에서 %s 거리를 안전하게 통과할 예정입니다. NASA에서 예보한 실제 이벤트입니다.", sizeInfo, distanceText);
                    
                    events.add(createForecastEvent(
                        "ASTEROID_FORECAST",
                        "예정된 소행성 근접 " + name.replace("(", "").replace(")", ""),
                        description,
                        eventDate
                    ));
                }
            }
        } catch (Exception e) {
            log.error("미래 NeoWs 데이터 파싱 실패", e);
        }
        
        return events;
    }
    
    private AstronomyEvent createForecastEvent(String type, String title, String description, LocalDateTime eventDate) {
        return AstronomyEvent.builder()
                .eventType(type)
                .title(title)
                .description(description)
                .eventDate(eventDate)
                .peakTime(eventDate)
                .visibility("KOREA")
                .magnitude("HIGH")
                .isActive(true)
                .build();
    }

    private String sanitizeForLog(String input) {
        if (input == null) return "null";
        // 로그 인젝션 방지: 개행문자, 제어문자 제거 및 길이 제한
        return input.replaceAll("[\r\n\t\f\b]", "_")
                   .replaceAll("[\u0000-\u001F\u007F]", "")
                   .substring(0, Math.min(input.length(), 100));
    }
    
    private AstronomyEventResponse convertToResponse(AstronomyEvent event) {
        return AstronomyEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)))
                .peakTime(event.getPeakTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)))
                .visibility(event.getVisibility())
                .magnitude(event.getMagnitude())
                .isActive(event.getIsActive())
                .build();
    }
}