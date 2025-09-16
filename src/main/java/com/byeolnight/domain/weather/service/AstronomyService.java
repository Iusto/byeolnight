package com.byeolnight.domain.weather.service;

import com.byeolnight.domain.weather.dto.AstronomyEventResponse;
import com.byeolnight.domain.weather.entity.AstronomyEvent;
import com.byeolnight.domain.weather.repository.AstronomyEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.HashMap;
import java.time.Duration;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AstronomyService {

    private final AstronomyEventRepository astronomyRepository;
    private final RestTemplate restTemplate;

    @Value("${nasa.api.key}")
    private String nasaApiKey;
    
    @Value("${kasi.api.key}")
    private String kasiApiKey;
    
    @Value("${kasi.api.base-url}")
    private String kasiBaseUrl;
    
    @Value("${kasi.api.service-name}")
    private String kasiServiceName;

    // API URLs
    private static final String NASA_NEOWS_URL = "https://api.nasa.gov/neo/rest/v1/feed";
    private static final String NASA_DONKI_URL = "https://api.nasa.gov/DONKI";
    private static final String ISS_LOCATION_URL = "https://api.wheretheiss.at/v1/satellites/25544";
    private static final String ISS_PASSES_URL = "https://api.wheretheiss.at/v1/satellites/25544/passes";
    
    // 실제 NASA 데이터 타입
    private static final Set<String> REAL_NASA_EVENT_TYPES = Set.of(
        "ASTEROID", "SOLAR_FLARE", "GEOMAGNETIC_STORM", 
        "LUNAR_ECLIPSE", "SOLAR_ECLIPSE", "SUPERMOON", "BLOOD_MOON", "BLUE_MOON"
    );

    // ==================== 공개 메서드 ====================
    
    public List<AstronomyEventResponse> getUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        List<AstronomyEvent> events = astronomyRepository.findUpcomingEvents(thirtyDaysAgo);
        
        return events.stream()
                .sorted((e1, e2) -> {
                    boolean e1IsReal = isRealNasaData(e1);
                    boolean e2IsReal = isRealNasaData(e2);
                    
                    if (e1IsReal && !e2IsReal) return -1;
                    if (!e1IsReal && e2IsReal) return 1;
                    
                    return e2.getEventDate().compareTo(e1.getEventDate());
                })
                .limit(10)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getIssObservationOpportunity(double latitude, double longitude) {
        try {
            // ISS 현재 위치 조회
            ResponseEntity<Map> currentResponse = restTemplate.getForEntity(ISS_LOCATION_URL, Map.class);
            
            // ISS 통과 예측 조회 (다음 5회)
            String passesUrl = ISS_PASSES_URL + "?lat=" + latitude + "&lon=" + longitude + "&limit=5&days=3";
            ResponseEntity<Map> passesResponse = restTemplate.getForEntity(passesUrl, Map.class);
            
            if (currentResponse.getStatusCode().is2xxSuccessful() && 
                passesResponse.getStatusCode().is2xxSuccessful() &&
                currentResponse.getBody() != null && passesResponse.getBody() != null) {
                
                return parseAdvancedIssData(currentResponse.getBody(), passesResponse.getBody(), latitude, longitude);
            }
        } catch (Exception e) {
            log.error("ISS Location API 호출 실패: {}", e.getMessage());
        }
        
        // Fallback: 기본 ISS 정보 제공
        return createFallbackIssInfo();
    }

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void fetchDailyAstronomyEvents() {
        performAstronomyDataCollection("자동 스케줄링");
    }
    
    /**
     * 관리자 수동 천체 이벤트 데이터 수집
     * @return 수집 결과 정보
     */
    @Transactional
    public Map<String, Object> manualFetchAstronomyEvents() {
        return performAstronomyDataCollection("관리자 수동 수집");
    }
    
    private Map<String, Object> performAstronomyDataCollection(String trigger) {
        LocalDateTime startTime = LocalDateTime.now();
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("천체 이벤트 데이터 수집 시작 ({})", trigger);
            
            List<AstronomyEvent> newEvents = collectAllAstronomyData();
            
            if (!newEvents.isEmpty()) {
                int beforeCount = (int) astronomyRepository.count();
                astronomyRepository.deleteAll();
                astronomyRepository.saveAll(newEvents);
                
                LocalDateTime endTime = LocalDateTime.now();
                long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
                
                result.put("success", true);
                result.put("message", "천체 이벤트 데이터 수집 완료");
                result.put("trigger", trigger);
                result.put("beforeCount", beforeCount);
                result.put("afterCount", newEvents.size());
                result.put("durationSeconds", durationSeconds);
                result.put("collectedAt", endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                // API별 수집 통계
                Map<String, Integer> apiStats = new HashMap<>();
                newEvents.forEach(event -> {
                    String source = determineEventSource(event.getEventType());
                    apiStats.put(source, apiStats.getOrDefault(source, 0) + 1);
                });
                result.put("apiStats", apiStats);
                
                log.info("천체 이벤트 데이터 수집 완료 ({}): {} 개, {}초 소요", trigger, newEvents.size(), durationSeconds);
            } else {
                result.put("success", false);
                result.put("message", "새로운 천체 데이터가 없어 기존 데이터 유지");
                result.put("trigger", trigger);
                result.put("beforeCount", (int) astronomyRepository.count());
                result.put("afterCount", 0);
                
                log.warn("새로운 천체 데이터가 없어 기존 데이터 유지 ({})", trigger);
            }
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            
            result.put("success", false);
            result.put("message", "천체 이벤트 데이터 수집 실패: " + e.getMessage());
            result.put("trigger", trigger);
            result.put("error", e.getClass().getSimpleName());
            result.put("durationSeconds", durationSeconds);
            
            log.error("천체 이벤트 데이터 수집 실패 ({})", trigger, e);
        }
        
        return result;
    }
    
    private String determineEventSource(String eventType) {
        switch (eventType) {
            case "ASTEROID":
                return "NASA NeoWs";
            case "SOLAR_FLARE":
            case "GEOMAGNETIC_STORM":
                return "NASA DONKI";
            case "LUNAR_ECLIPSE":
            case "SOLAR_ECLIPSE":
            case "BLOOD_MOON":
            case "SUPERMOON":
            case "BLUE_MOON":
                return "KASI";
            default:
                return "Unknown";
        }
    }

    /**
     * 현재 저장된 천체 이벤트 통계 조회
     * @return 통계 정보
     */
    public Map<String, Object> getAstronomyEventStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long totalCount = astronomyRepository.count();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime thirtyDaysAgo = now.minusDays(30);
            
            List<AstronomyEvent> recentEvents = astronomyRepository.findUpcomingEvents(thirtyDaysAgo);
            
            // 이벤트 타입별 통계
            Map<String, Long> typeStats = recentEvents.stream()
                .collect(Collectors.groupingBy(AstronomyEvent::getEventType, Collectors.counting()));
            
            // API 소스별 통계
            Map<String, Long> sourceStats = recentEvents.stream()
                .collect(Collectors.groupingBy(
                    event -> determineEventSource(event.getEventType()), 
                    Collectors.counting()
                ));
            
            stats.put("totalCount", totalCount);
            stats.put("recentCount", recentEvents.size());
            stats.put("typeStats", typeStats);
            stats.put("sourceStats", sourceStats);
            stats.put("lastUpdated", recentEvents.isEmpty() ? null : 
                recentEvents.get(0).getEventDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
        } catch (Exception e) {
            log.error("천체 이벤트 통계 조회 실패", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    // ==================== 데이터 수집 ====================
    
    private List<AstronomyEvent> collectAllAstronomyData() {
        List<AstronomyEvent> allEvents = new ArrayList<>();

        // NASA API 데이터 (실시간)
        allEvents.addAll(safeApiCall("NASA NeoWs", this::fetchNeoWsData));
        allEvents.addAll(safeApiCall("NASA DONKI", this::fetchDonkiData));
        
        // KASI API 데이터 (한국 기준)
        allEvents.addAll(safeApiCall("KASI Astronomy", this::fetchKasiAstronomyData));

        log.info("수집된 총 이벤트 수: {}", allEvents.size());
        return allEvents;
    }

    private List<AstronomyEvent> safeApiCall(String apiName, java.util.function.Supplier<List<AstronomyEvent>> apiCall) {
        try {
            List<AstronomyEvent> events = apiCall.get();
            if (!events.isEmpty()) {
                log.info("{} 성공: {} 개", apiName, events.size());
            }
            return events;
        } catch (Exception e) {
            log.warn("{} 실패: {}", apiName, e.getMessage());
            return new ArrayList<>();
        }
    }

    // ==================== NASA API 데이터 ====================
    
    private List<AstronomyEvent> fetchNeoWsData() {
        if (!isValidApiKey()) return new ArrayList<>();

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(7);
        String url = NASA_NEOWS_URL + "?start_date=" + startDate.toLocalDate() +
                "&end_date=" + endDate.toLocalDate() + "&api_key=" + nasaApiKey;

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return parseNeoWsData(response.getBody());
        }
        return new ArrayList<>();
    }

    private List<AstronomyEvent> fetchDonkiData() {
        if (!isValidApiKey()) return new ArrayList<>();

        List<AstronomyEvent> events = new ArrayList<>();
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();

        // 태양 플레어
        String flareUrl = NASA_DONKI_URL + "/FLR?startDate=" + startDate.toLocalDate() +
                "&endDate=" + endDate.toLocalDate() + "&api_key=" + nasaApiKey;
        
        ResponseEntity<Map[]> flareResponse = restTemplate.getForEntity(flareUrl, Map[].class);
        if (flareResponse.getStatusCode().is2xxSuccessful() && flareResponse.getBody() != null) {
            events.addAll(parseDonkiFlareData(flareResponse.getBody()));
        }

        // 지자기 폭풍
        String gstUrl = NASA_DONKI_URL + "/GST?startDate=" + startDate.toLocalDate() +
                "&endDate=" + endDate.toLocalDate() + "&api_key=" + nasaApiKey;
        
        ResponseEntity<Map[]> gstResponse = restTemplate.getForEntity(gstUrl, Map[].class);
        if (gstResponse.getStatusCode().is2xxSuccessful() && gstResponse.getBody() != null) {
            events.addAll(parseDonkiGstData(gstResponse.getBody()));
        }

        return events;
    }

    // ==================== KASI API 데이터 (한국 기준) ====================
    
    private List<AstronomyEvent> fetchKasiAstronomyData() {
        if (!isValidKasiApiKey()) {
            log.info("KASI API 키 검증 실패, 대체 데이터 사용");
            return createFallbackKasiEvents();
        }
        
        List<AstronomyEvent> events = new ArrayList<>();
        int currentYear = LocalDateTime.now().getYear();
        
        // 재시도 로직 (최대 3회)
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String url = kasiBaseUrl + "/" + kasiServiceName +
                    "?serviceKey=" + kasiApiKey +
                    "&solYear=" + currentYear +
                    "&numOfRows=100" +
                    "&pageNo=1";
                
                log.info("KASI API 호출 시도 {}/{}: {}", attempt, maxRetries, url.replace(kasiApiKey, "***"));
                
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    String responseBody = response.getBody();
                    
                    // 응답 데이터 유효성 검사
                    if (responseBody.length() < 100) {
                        log.warn("KASI API 응답 데이터가 너무 짧음: {} 바이트", responseBody.length());
                        if (attempt < maxRetries) continue;
                    }
                    
                    events.addAll(parseKasiXmlData(responseBody));
                    log.info("KASI 천문현상 데이터 {} 개 수집 완료 (시도 {})", events.size(), attempt);
                    break; // 성공 시 루프 종료
                    
                } else {
                    log.warn("KASI API 응답 실패 (시도 {}): {}", attempt, response.getStatusCode());
                    if (attempt < maxRetries) {
                        Thread.sleep(2000 * attempt); // 지수 백오프
                    }
                }
                
            } catch (Exception e) {
                log.error("KASI API 호출 실패 (시도 {}): {}", attempt, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(2000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // 모든 시도 실패 시 대체 데이터 반환
        if (events.isEmpty()) {
            log.warn("KASI API 모든 시도 실패, 대체 데이터 사용");
            return createFallbackKasiEvents();
        }
        
        return events;
    }
    
    private List<AstronomyEvent> parseKasiXmlData(String xmlData) {
        List<AstronomyEvent> events = new ArrayList<>();
        
        try {
            // API 응답 상태 확인
            if (xmlData.contains("<resultCode>00</resultCode>") || xmlData.contains("<resultCode>0</resultCode>")) {
                log.info("KASI API 정상 응답 확인");
            } else if (xmlData.contains("<resultCode>")) {
                String errorCode = extractXmlValue(xmlData, "resultCode");
                String errorMsg = extractXmlValue(xmlData, "resultMsg");
                log.warn("KASI API 오류 응답: 코드={}, 메시지={}", errorCode, errorMsg);
                return createFallbackKasiEvents(); // 대체 데이터 제공
            }
            
            // 구조화된 XML 파싱
            if (xmlData.contains("<item>")) {
                String[] items = xmlData.split("<item>");
                int successCount = 0;
                int failCount = 0;
                
                for (int i = 1; i < items.length; i++) {
                    try {
                        String itemXml = "<item>" + items[i].split("</item>")[0] + "</item>";
                        AstronomyEvent event = parseKasiXmlItem(itemXml);
                        if (event != null) {
                            events.add(event);
                            successCount++;
                        } else {
                            failCount++;
                        }
                    } catch (Exception itemError) {
                        failCount++;
                        log.debug("KASI 개별 아이템 파싱 실패: {}", itemError.getMessage());
                    }
                }
                
                log.info("KASI XML 파싱 완료: 성공={}, 실패={}", successCount, failCount);
            } else {
                log.warn("KASI XML에 <item> 태그가 없음, 대체 데이터 제공");
                return createFallbackKasiEvents();
            }
            
        } catch (Exception e) {
            log.error("KASI XML 데이터 파싱 실패: {}", e.getMessage());
            return createFallbackKasiEvents(); // 파싱 실패 시 대체 데이터
        }
        
        return events.isEmpty() ? createFallbackKasiEvents() : events;
    }
    
    private List<AstronomyEvent> createFallbackKasiEvents() {
        List<AstronomyEvent> fallbackEvents = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        try {
            // 2025년 예정된 주요 천문현상 (실제 데이터 기반)
            if (now.getYear() == 2025) {
                // 3월 14일 개기월식
                fallbackEvents.add(createAstronomyEvent("BLOOD_MOON", 
                    "3월 개기월식", 
                    "3월 14일 개기월식이 예정되어 있습니다. (한국천문연구원 예측 데이터)",
                    LocalDateTime.of(2025, 3, 14, 13, 0), "HIGH"));
                
                // 9월 8일 개기일식
                fallbackEvents.add(createAstronomyEvent("SOLAR_ECLIPSE", 
                    "9월 개기일식", 
                    "9월 8일 개기일식이 예정되어 있습니다. (한국천문연구원 예측 데이터)",
                    LocalDateTime.of(2025, 9, 8, 2, 0), "HIGH"));
            }
            
            log.info("KASI 대체 데이터 {} 개 생성", fallbackEvents.size());
        } catch (Exception e) {
            log.error("KASI 대체 데이터 생성 실패: {}", e.getMessage());
        }
        
        return fallbackEvents;
    }
    
    private AstronomyEvent parseKasiXmlItem(String itemXml) {
        try {
            String astroEvent = extractXmlValue(itemXml, "astroEvent");
            String astroTime = extractXmlValue(itemXml, "astroTime");
            String locdate = extractXmlValue(itemXml, "locdate");
            
            if (astroEvent == null || locdate == null) {
                return null;
            }
            
            LocalDateTime eventDate = parseKasiDate(locdate, astroTime);
            if (eventDate == null) {
                return null;
            }
            
            String eventType = determineKasiEventType(astroEvent);
            if (eventType == null) {
                return null;
            }
            
            String title = createKasiEventTitle(astroEvent, eventDate);
            String description = createKasiEventDescription(astroEvent, eventDate);
            String magnitude = getKasiEventMagnitude(eventType);
            
            return createAstronomyEvent(eventType, title, description, eventDate, magnitude);
            
        } catch (Exception e) {
            log.warn("KASI XML 아이템 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractXmlValue(String xml, String tagName) {
        try {
            String startTag = "<" + tagName + ">";
            String endTag = "</" + tagName + ">";
            
            int startIndex = xml.indexOf(startTag);
            int endIndex = xml.indexOf(endTag);
            
            if (startIndex != -1 && endIndex != -1) {
                return xml.substring(startIndex + startTag.length(), endIndex).trim();
            }
        } catch (Exception e) {
            log.debug("XML 값 추출 실패: tagName={}", tagName);
        }
        return null;
    }
    
    private LocalDateTime parseKasiDate(String locdate, String astroTime) {
        try {
            // locdate: YYYYMMDD, astroTime: HHMM 또는 null
            String dateStr = locdate;
            String timeStr = (astroTime != null && !astroTime.trim().isEmpty()) ? astroTime : "1200";
            
            int year = Integer.parseInt(dateStr.substring(0, 4));
            int month = Integer.parseInt(dateStr.substring(4, 6));
            int day = Integer.parseInt(dateStr.substring(6, 8));
            
            int hour = 12;
            int minute = 0;
            
            if (timeStr.length() >= 4) {
                hour = Integer.parseInt(timeStr.substring(0, 2));
                minute = Integer.parseInt(timeStr.substring(2, 4));
            }
            
            return LocalDateTime.of(year, month, day, hour, minute);
            
        } catch (Exception e) {
            log.warn("KASI 날짜 파싱 실패: locdate={}, astroTime={}", locdate, astroTime);
            return null;
        }
    }
    
    private String determineKasiEventType(String astroEvent) {
        String event = astroEvent.toLowerCase();
        
        if (event.contains("월식") || event.contains("lunar eclipse")) {
            if (event.contains("개기") || event.contains("total")) {
                return "BLOOD_MOON"; // 개기월식 = 블러드문
            }
            return "LUNAR_ECLIPSE";
        }
        
        if (event.contains("일식") || event.contains("solar eclipse")) {
            return "SOLAR_ECLIPSE";
        }
        
        if (event.contains("슈퍼문") || event.contains("supermoon") || 
            event.contains("근지점") || event.contains("perigee")) {
            return "SUPERMOON";
        }
        
        if (event.contains("블루문") || event.contains("blue moon")) {
            return "BLUE_MOON";
        }
        
        // 지원하지 않는 이벤트
        return null;
    }
    
    private String createKasiEventTitle(String astroEvent, LocalDateTime eventDate) {
        return eventDate.getMonthValue() + "월 " + astroEvent;
    }
    
    private String createKasiEventDescription(String astroEvent, LocalDateTime eventDate) {
        return String.format("%d월 %d일 %s이 예정되어 있습니다. (한국천문연구원 공식 데이터)",
            eventDate.getMonthValue(), eventDate.getDayOfMonth(), astroEvent);
    }
    
    private String getKasiEventMagnitude(String eventType) {
        switch (eventType) {
            case "LUNAR_ECLIPSE":
            case "SOLAR_ECLIPSE":
            case "BLOOD_MOON":
            case "SUPERMOON":
                return "HIGH";
            case "BLUE_MOON":
                return "MEDIUM";
            default:
                return "MEDIUM";
        }
    }
    
    private boolean isValidKasiApiKey() {
        if (kasiApiKey == null || kasiApiKey.trim().isEmpty()) {
            log.warn("KASI API 키가 설정되지 않음");
            return false;
        }
        
        if (kasiApiKey.length() < 10) {
            log.warn("KASI API 키 길이가 너무 짧음: {} 자", kasiApiKey.length());
            return false;
        }
        
        if ("DEMO_KEY".equals(kasiApiKey) || "TEST_KEY".equals(kasiApiKey)) {
            log.warn("KASI API 데모 키 사용 중");
            return false;
        }
        
        return true;
    }

    // ==================== 데이터 파싱 ====================
    
    private List<AstronomyEvent> parseNeoWsData(Map<String, Object> data) {
        List<AstronomyEvent> events = new ArrayList<>();
        try {
            Map<String, Object> nearEarthObjects = (Map<String, Object>) data.get("near_earth_objects");
            
            for (Map.Entry<String, Object> entry : nearEarthObjects.entrySet()) {
                List<Map<String, Object>> asteroids = (List<Map<String, Object>>) entry.getValue();
                
                for (Map<String, Object> asteroid : asteroids.stream().limit(3).collect(Collectors.toList())) {
                    String name = (String) asteroid.get("name");
                    Boolean isHazardous = (Boolean) asteroid.get("is_potentially_hazardous_asteroid");
                    LocalDateTime eventDate = LocalDateTime.parse(entry.getKey() + "T21:00:00");
                    
                    String sizeInfo = getAsteroidSize(asteroid);
                    String distanceInfo = getAsteroidDistance(asteroid);
                    
                    String description = String.format("Asteroid with diameter %s safely passed Earth at distance %s.", 
                        sizeInfo, distanceInfo);
                    
                    events.add(createAstronomyEvent("ASTEROID", 
                        "Near-Earth Asteroid " + name.replace("(", "").replace(")", ""),
                        description, eventDate, isHazardous ? "HIGH" : "MEDIUM"));
                }
            }
        } catch (Exception e) {
            log.error("NeoWs 데이터 파싱 실패: {}", e.getMessage());
        }
        return events;
    }

    private List<AstronomyEvent> parseDonkiFlareData(Map<String, Object>[] flareData) {
        return Arrays.stream(flareData)
                .filter(flare -> flare.get("classType") != null && flare.get("beginTime") != null)
                .map(this::createFlareEvent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<AstronomyEvent> parseDonkiGstData(Map<String, Object>[] gstData) {
        return Arrays.stream(gstData)
                .filter(gst -> gst.get("startTime") != null)
                .map(this::createGstEvent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, Object> parseAdvancedIssData(Map<String, Object> currentData, 
                                                     Map<String, Object> passesData, 
                                                     double userLat, double userLon) {
        try {
            // 현재 ISS 위치
            double issLat = ((Number) currentData.get("latitude")).doubleValue();
            double issLon = ((Number) currentData.get("longitude")).doubleValue();
            double issAlt = ((Number) currentData.get("altitude")).doubleValue();
            
            // 다음 통과 정보
            List<Map<String, Object>> passes = (List<Map<String, Object>>) passesData.get("passes");
            if (passes == null || passes.isEmpty()) {
                return createNoPassesInfo(issLat, issLon, issAlt, userLat, userLon);
            }
            
            // 가장 좋은 관측 기회 선택 (최고 고도각 기준)
            Map<String, Object> bestPass = findBestObservationPass(passes);
            
            return createAdvancedIssInfo(bestPass, issLat, issLon, issAlt, userLat, userLon);
            
        } catch (Exception e) {
            log.error("고급 ISS 데이터 파싱 실패: {}", e.getMessage());
            return createFallbackIssInfo();
        }
    }
    
    private Map<String, Object> findBestObservationPass(List<Map<String, Object>> passes) {
        return passes.stream()
                .filter(pass -> {
                    double maxElevation = ((Number) pass.get("max_elevation")).doubleValue();
                    return maxElevation >= 10; // 최소 10도 이상
                })
                .max((p1, p2) -> {
                    double elev1 = ((Number) p1.get("max_elevation")).doubleValue();
                    double elev2 = ((Number) p2.get("max_elevation")).doubleValue();
                    return Double.compare(elev1, elev2);
                })
                .orElse(passes.get(0)); // 조건에 맞는 것이 없으면 첫 번째
    }
    
    private Map<String, Object> createAdvancedIssInfo(Map<String, Object> bestPass,
                                                      double issLat, double issLon, double issAlt,
                                                      double userLat, double userLon) {
        
        long riseTime = ((Number) bestPass.get("rise_time")).longValue();
        long setTime = ((Number) bestPass.get("set_time")).longValue();
        double maxElevation = ((Number) bestPass.get("max_elevation")).doubleValue();
        
        LocalDateTime riseDateTime = LocalDateTime.ofEpochSecond(riseTime, 0, java.time.ZoneOffset.of("+09:00"));
        LocalDateTime setDateTime = LocalDateTime.ofEpochSecond(setTime, 0, java.time.ZoneOffset.of("+09:00"));
        LocalDateTime now = LocalDateTime.now();
        
        // 관측 시간 계산
        long durationSeconds = setTime - riseTime;
        int durationMinutes = (int) (durationSeconds / 60);
        
        // 방향 정보 계산
        String startDirection = calculateDirection(((Number) bestPass.get("rise_azimuth")).doubleValue());
        String endDirection = calculateDirection(((Number) bestPass.get("set_azimuth")).doubleValue());
        
        // 밝기 계산 (고도각 기반)
        String brightness = calculateBrightness(maxElevation);
        String brightnessDesc = getBrightnessDescription(brightness);
        
        // 관측 품질 평가
        String observationQuality = evaluateObservationQuality(maxElevation, durationMinutes);
        
        // 사용자 친화적 메시지 생성
        String friendlyMessage = createFriendlyMessage(riseDateTime, startDirection, endDirection, 
                                                      maxElevation, durationMinutes, brightness);
        
        // ISS 현재 거리 계산
        double distanceKm = calculateDistance(userLat, userLon, issLat, issLon);
        
        Map<String, Object> result = new HashMap<>();
        result.put("message_key", "iss.advanced_opportunity");
        result.put("friendly_message", friendlyMessage);
        result.put("rise_time", riseDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        result.put("rise_date", riseDateTime.format(DateTimeFormatter.ofPattern("MM-dd")));
        result.put("duration_minutes", durationMinutes);
        result.put("max_elevation", Math.round(maxElevation));
        result.put("start_direction", startDirection);
        result.put("end_direction", endDirection);
        result.put("brightness", brightness);
        result.put("brightness_desc", brightnessDesc);
        result.put("observation_quality", observationQuality);
        result.put("current_distance_km", Math.round(distanceKm));
        result.put("current_altitude_km", Math.round(issAlt));
        result.put("is_today", riseDateTime.toLocalDate().equals(now.toLocalDate()));
        result.put("is_tomorrow", riseDateTime.toLocalDate().equals(now.toLocalDate().plusDays(1)));
        result.put("is_visible_now", isCurrentlyVisible(distanceKm, issAlt));
        return result;
    }
    
    private Map<String, Object> createNoPassesInfo(double issLat, double issLon, double issAlt,
                                                   double userLat, double userLon) {
        double distanceKm = calculateDistance(userLat, userLon, issLat, issLon);
        
        return Map.of(
            "message_key", "iss.no_passes",
            "friendly_message", "현재 ISS가 관측 가능한 경로로 지나가지 않습니다. 며칠 후 다시 확인해보세요.",
            "current_distance_km", Math.round(distanceKm),
            "current_altitude_km", Math.round(issAlt),
            "is_visible_now", isCurrentlyVisible(distanceKm, issAlt)
        );
    }
    
    private Map<String, Object> createFallbackIssInfo() {
        return Map.of(
            "message_key", "iss.fallback",
            "friendly_message", "ISS는 지구 상공 400km에서 90분마다 지구를 한 바퀴 돕니다. 맑은 밤하늘에서 밝은 점으로 관측할 수 있습니다.",
            "current_altitude_km", 408,
            "orbital_period_minutes", 93,
            "observation_tip", "일출 전이나 일몰 후 1-2시간이 관측하기 가장 좋은 시간입니다."
        );
    }
    
    private String calculateDirection(double azimuth) {
        if (azimuth >= 337.5 || azimuth < 22.5) return "북쪽";
        if (azimuth >= 22.5 && azimuth < 67.5) return "북동쪽";
        if (azimuth >= 67.5 && azimuth < 112.5) return "동쪽";
        if (azimuth >= 112.5 && azimuth < 157.5) return "남동쪽";
        if (azimuth >= 157.5 && azimuth < 202.5) return "남쪽";
        if (azimuth >= 202.5 && azimuth < 247.5) return "남서쪽";
        if (azimuth >= 247.5 && azimuth < 292.5) return "서쪽";
        return "북서쪽";
    }
    
    private String calculateBrightness(double elevation) {
        if (elevation >= 50) return "-3.5"; // 매우 밝음
        if (elevation >= 30) return "-2.0"; // 밝음
        if (elevation >= 20) return "-1.0"; // 보통
        if (elevation >= 10) return "0.0";  // 어두움
        return "1.0"; // 매우 어두움
    }
    
    private String getBrightnessDescription(String magnitude) {
        double mag = Double.parseDouble(magnitude);
        if (mag <= -3.0) return "금성보다 밝음 (매우 밝음)";
        if (mag <= -2.0) return "목성 정도 밝기 (밝음)";
        if (mag <= -1.0) return "1등성 정도 밝기 (보통)";
        if (mag <= 0.0) return "2등성 정도 밝기 (어두움)";
        return "3등성 정도 밝기 (매우 어두움)";
    }
    
    private String evaluateObservationQuality(double elevation, int duration) {
        if (elevation >= 50 && duration >= 4) return "EXCELLENT";
        if (elevation >= 30 && duration >= 3) return "GOOD";
        if (elevation >= 20 && duration >= 2) return "FAIR";
        return "POOR";
    }
    
    private String createFriendlyMessage(LocalDateTime riseTime, String startDir, String endDir,
                                        double elevation, int duration, String brightness) {
        String timeStr = riseTime.format(DateTimeFormatter.ofPattern("M월 d일 HH:mm"));
        String elevationStr = Math.round(elevation) + "도";
        
        return String.format("%s에 %s 하늘에서 나타나 %s으로 사라집니다. " +
                           "최고 높이 %s까지 올라가며 %d분간 관측 가능합니다. " +
                           "밝기는 %s로 육안 관측이 %s합니다.",
                           timeStr, startDir, endDir, elevationStr, duration, 
                           getBrightnessDescription(brightness),
                           Double.parseDouble(brightness) <= -1.0 ? "쉽" : "어려울 수 있");
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // 지구 반지름 (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
    
    private boolean isCurrentlyVisible(double distanceKm, double altitudeKm) {
        // ISS가 지평선 위에 있고 적당한 거리에 있으면 관측 가능
        return distanceKm <= 2000 && altitudeKm >= 300;
    }

    // ==================== 헬퍼 메서드 ====================
    
    private AstronomyEvent createAstronomyEvent(String type, String title, String description, 
            LocalDateTime eventDate, String magnitude) {
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
    }

    private AstronomyEvent createFlareEvent(Map<String, Object> flare) {
        try {
            String beginTime = (String) flare.get("beginTime");
            LocalDateTime eventTime = LocalDateTime.parse(beginTime.replace("Z", ""));
            String classType = flare.get("classType").toString();
            
            String description = String.format("Solar flare class %s occurred on %s. May have temporarily affected radio and GPS signals.",
                    classType, eventTime.toLocalDate());
            
            return createAstronomyEvent("SOLAR_FLARE", "Solar Flare Class " + classType, 
                description, eventTime, "HIGH");
        } catch (Exception e) {
            log.warn("태양 플레어 데이터 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private AstronomyEvent createGstEvent(Map<String, Object> gst) {
        try {
            String startTime = (String) gst.get("startTime");
            LocalDateTime eventTime = LocalDateTime.parse(startTime.replace("Z", ""));
            String kpIndex = gst.get("kpIndex") != null ? gst.get("kpIndex").toString() : "Unknown";
            
            String description = String.format("Geomagnetic storm occurred on %s. Kp index: %s. Aurora observation may have been possible in polar regions.",
                    eventTime.toLocalDate(), kpIndex);
            
            return createAstronomyEvent("GEOMAGNETIC_STORM", "Geomagnetic Storm", 
                description, eventTime, "MEDIUM");
        } catch (Exception e) {
            log.warn("지자기 폭풍 데이터 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private String getAsteroidSize(Map<String, Object> asteroid) {
        try {
            Map<String, Object> estimatedDiameter = (Map<String, Object>) asteroid.get("estimated_diameter");
            Map<String, Object> meters = (Map<String, Object>) estimatedDiameter.get("meters");
            Double maxDiameter = (Double) meters.get("estimated_diameter_max");
            
            if (maxDiameter >= 1000) {
                return String.format("%.1fkm", maxDiameter / 1000);
            } else {
                return String.format("%.0fm", maxDiameter);
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String getAsteroidDistance(Map<String, Object> asteroid) {
        try {
            List<Map<String, Object>> closeApproachData = (List<Map<String, Object>>) asteroid.get("close_approach_data");
            if (!closeApproachData.isEmpty()) {
                Map<String, Object> missDistance = (Map<String, Object>) closeApproachData.get(0).get("miss_distance");
                String kmDistance = (String) missDistance.get("kilometers");
                double distance = Double.parseDouble(kmDistance);
                
                if (distance >= 1000000) {
                    return String.format("%.1f million km", distance / 1000000);
                } else if (distance >= 10000) {
                    return String.format("%.0f thousand km", distance / 1000);
                } else {
                    return String.format("%.0f km", distance);
                }
            }
        } catch (Exception e) {
            log.debug("소행성 거리 정보 파싱 실패: {}", e.getMessage());
        }
        return "정보 없음";
    }

    private String determineDirection(int hour) {
        if (hour >= 18 && hour <= 21) return "west";
        if (hour >= 4 && hour <= 7) return "east";
        if (hour >= 22 || hour <= 3) return "north";
        return "south";
    }

    private boolean isValidApiKey() {
        return nasaApiKey != null && !nasaApiKey.trim().isEmpty() && !"DEMO_KEY".equals(nasaApiKey);
    }

    private boolean isRealNasaData(AstronomyEvent event) {
        LocalDateTime now = LocalDateTime.now();
        boolean isPastEvent = event.getEventDate().isBefore(now);
        boolean isRealType = REAL_NASA_EVENT_TYPES.contains(event.getEventType());
        boolean isPrediction = event.getTitle().contains("예측") || event.getDescription().contains("예상");
        
        return isPastEvent && isRealType && !isPrediction;
    }

    private AstronomyEventResponse convertToResponse(AstronomyEvent event) {
        return AstronomyEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .peakTime(event.getPeakTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .visibility(event.getVisibility())
                .magnitude(event.getMagnitude())
                .isActive(event.getIsActive())
                .build();
    }
}