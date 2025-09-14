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
    private static final String ISS_PASS_URL = "http://api.open-notify.org/iss-pass.json";
    
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
            String passUrl = ISS_PASS_URL + "?lat=" + latitude + "&lon=" + longitude + "&n=3";
            ResponseEntity<Map> response = restTemplate.getForEntity(passUrl, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseIssPassData(response.getBody());
            }
        } catch (Exception e) {
            log.error("ISS Pass API 호출 실패: {}", e.getMessage());
        }
        return Map.of("error", "ISS 관측 정보 조회 실패");
    }

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void fetchDailyAstronomyEvents() {
        try {
            log.info("천체 이벤트 데이터 수집 시작");
            
            List<AstronomyEvent> newEvents = collectAllAstronomyData();
            
            if (!newEvents.isEmpty()) {
                astronomyRepository.deleteAll();
                astronomyRepository.saveAll(newEvents);
                log.info("천체 이벤트 데이터 수집 완료: {} 개", newEvents.size());
            } else {
                log.warn("새로운 천체 데이터가 없어 기존 데이터 유지");
            }
        } catch (Exception e) {
            log.error("천체 이벤트 데이터 수집 실패", e);
        }
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
        if (!isValidKasiApiKey()) return new ArrayList<>();
        
        List<AstronomyEvent> events = new ArrayList<>();
        int currentYear = LocalDateTime.now().getYear();
        
        try {
            String url = kasiBaseUrl + "/" + kasiServiceName +
                "?serviceKey=" + kasiApiKey +
                "&solYear=" + currentYear +
                "&numOfRows=100";
            
            log.info("KASI API 호출: {}", url.replace(kasiApiKey, "***"));
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                events.addAll(parseKasiXmlData(response.getBody()));
                log.info("KASI 천문현상 데이터 {} 개 수집 완료", events.size());
            } else {
                log.warn("KASI API 응답 실패: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("KASI API 호출 실패: {}", e.getMessage(), e);
        }
        
        return events;
    }
    
    private List<AstronomyEvent> parseKasiXmlData(String xmlData) {
        List<AstronomyEvent> events = new ArrayList<>();
        
        try {
            // XML 파싱 (간단한 문자열 처리)
            if (xmlData.contains("<item>")) {
                String[] items = xmlData.split("<item>");
                
                for (int i = 1; i < items.length; i++) { // 첫 번째는 헤더
                    String itemXml = "<item>" + items[i].split("</item>")[0] + "</item>";
                    AstronomyEvent event = parseKasiXmlItem(itemXml);
                    if (event != null) {
                        events.add(event);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("KASI XML 데이터 파싱 실패: {}", e.getMessage());
        }
        
        return events;
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
        return kasiApiKey != null && !kasiApiKey.trim().isEmpty() && 
               kasiApiKey.length() > 10; // 실제 API 키는 32자 이상
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

    private Map<String, Object> parseIssPassData(Map<String, Object> passData) {
        try {
            List<Map<String, Object>> passes = (List<Map<String, Object>>) passData.get("response");
            if (passes == null || passes.isEmpty()) {
                return Map.of("message_key", "iss.no_passes");
            }
            
            Map<String, Object> nextPass = passes.get(0);
            long riseTime = ((Number) nextPass.get("risetime")).longValue();
            int duration = ((Number) nextPass.get("duration")).intValue();
            
            LocalDateTime passDateTime = LocalDateTime.ofEpochSecond(riseTime, 0, java.time.ZoneOffset.of("+09:00"));
            LocalDateTime now = LocalDateTime.now();
            
            return Map.of(
                "message_key", "iss.basic_opportunity",
                "time", passDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                "date", passDateTime.format(DateTimeFormatter.ofPattern("MM-dd")),
                "duration_minutes", duration / 60,
                "direction", determineDirection(passDateTime.getHour()),
                "is_today", passDateTime.toLocalDate().equals(now.toLocalDate()),
                "is_tomorrow", passDateTime.toLocalDate().equals(now.toLocalDate().plusDays(1))
            );
        } catch (Exception e) {
            log.error("ISS Pass 데이터 파싱 실패: {}", e.getMessage());
            return Map.of("error", "ISS 데이터 파싱 실패");
        }
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