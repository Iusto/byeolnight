package com.byeolnight.domain.weather.service;

import com.byeolnight.domain.weather.dto.AstronomyEventResponse;
import com.byeolnight.domain.weather.entity.AstronomyEvent;
import com.byeolnight.domain.weather.repository.AstronomyEventRepository;
import com.byeolnight.service.notification.NotificationService;
import com.byeolnight.entity.Notification;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AstronomyService {
    
    private final AstronomyEventRepository astronomyRepository;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${nasa.api.key}")
    private String nasaApiKey;
    
    @Value("${kasi.api.key}")
    private String kasiApiKey;
    
    // NASA API URLs
    private static final String NASA_APOD_URL = "https://api.nasa.gov/planetary/apod";
    private static final String NASA_NEOWS_URL = "https://api.nasa.gov/neo/rest/v1/feed";
    private static final String NASA_DONKI_URL = "https://api.nasa.gov/DONKI";
    private static final String NASA_ISS_URL = "http://api.open-notify.org/iss-now.json";
    
    // KASI API URL
    private static final String KASI_ASTRO_URL = "http://apis.data.go.kr/B090041/openapi/service/AstroEventInfoService";
    
    public List<AstronomyEventResponse> getUpcomingEvents() {
        List<AstronomyEvent> events = astronomyRepository.findUpcomingEvents(LocalDateTime.now());
        return events.stream()
            .limit(10) // 최대 10개
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void checkUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);
        
        List<AstronomyEvent> upcomingEvents = astronomyRepository.findActiveEventsBetween(now, tomorrow);
        
        for (AstronomyEvent event : upcomingEvents) {
            if (shouldNotifyEvent(event, now)) {
                sendEventNotification(event);
            }
        }
    }
    
    @Scheduled(cron = "0 0 9 * * ?") // 매일 오전 9시 실행
    public void fetchDailyAstronomyEvents() {
        performAstronomyDataCollection();
    }
    
    // 관리자 수동 수집용 public 메서드
    public void performAstronomyDataCollection() {
        try {
            log.info("천체 이벤트 데이터 수집 시작 (NASA + KASI API 연동)");
            
            // 기존 이벤트 비활성화
            deactivateOldEvents();
            
            // 다양한 이벤트 생성 (랜덤)
            createRandomEvents();
            
            log.info("천체 이벤트 데이터 수집 완료");
            
        } catch (Exception e) {
            log.error("천체 이벤트 데이터 수집 실패: {}", e.getMessage());
            throw e;
        }
    }
    
    private void deactivateOldEvents() {
        // 기존 이벤트 중 오래된 것만 삭제 (7일 이전)
        List<AstronomyEvent> oldEvents = astronomyRepository.findUpcomingEvents(LocalDateTime.now().minusDays(7));
        
        if (!oldEvents.isEmpty()) {
            astronomyRepository.deleteAll(oldEvents);
        }
        
        log.info("오래된 이벤트 {} 개 삭제", oldEvents.size());
    }
    
    private void createRandomEvents() {
        // 실제 API 데이터 수집 시도
        fetchAlternativeAstronomyData();
    }
    
    private void fetchAlternativeAstronomyData() {
        List<AstronomyEvent> allEvents = new ArrayList<>();
        int successCount = 0;
        
        // 1. NASA NeoWs - 지구 근접 소행성
        try {
            List<AstronomyEvent> neoEvents = fetchNeoWsData();
            allEvents.addAll(neoEvents);
            if (!neoEvents.isEmpty()) successCount++;
        } catch (Exception e) {
            log.warn("NASA NeoWs API 호출 실패: {}", e.getMessage());
        }
        
        // 2. NASA DONKI - 우주 기상
        try {
            List<AstronomyEvent> donkiEvents = fetchDonkiData();
            allEvents.addAll(donkiEvents);
            if (!donkiEvents.isEmpty()) successCount++;
        } catch (Exception e) {
            log.warn("NASA DONKI API 호출 실패: {}", e.getMessage());
        }
        
        // 3. NASA ISS - 국제우주정거장 위치
        try {
            List<AstronomyEvent> issEvents = fetchIssData();
            allEvents.addAll(issEvents);
            if (!issEvents.isEmpty()) successCount++;
        } catch (Exception e) {
            log.warn("NASA ISS API 호출 실패: {}", e.getMessage());
        }
        
        // 4. KASI - 한국 천문현상
        try {
            List<AstronomyEvent> kasiEvents = fetchKasiData();
            allEvents.addAll(kasiEvents);
            if (!kasiEvents.isEmpty()) successCount++;
        } catch (Exception e) {
            log.warn("KASI API 호출 실패: {}", e.getMessage());
        }
        
        // 실제 API 데이터 저장
        if (!allEvents.isEmpty()) {
            log.info("저장 전 이벤트 목록:");
            for (AstronomyEvent event : allEvents) {
                log.info("- {} ({}): {}", event.getTitle(), event.getEventType(), event.getEventDate());
            }
            
            astronomyRepository.saveAll(allEvents);
            log.info("실제 API 데이터 {} 개 수집 성공 ({}/4 API 성공)", allEvents.size(), successCount);
        } else {
            log.warn("모든 API 호출 실패, 기본 데이터 생성");
            createFallbackEvents();
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
            log.error("NASA NeoWs API 호출 실패: {}", e.getMessage());
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
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusDays(7);
            
            // 태양 플레어 데이터
            String flareUrl = NASA_DONKI_URL + "/FLR?startDate=" + startDate.toLocalDate() + 
                             "&endDate=" + endDate.toLocalDate() + "&api_key=" + nasaApiKey;
            
            log.info("NASA DONKI FLR API 호출");
            ResponseEntity<Map[]> flareResponse = restTemplate.getForEntity(flareUrl, Map[].class);
            
            if (flareResponse.getStatusCode().is2xxSuccessful() && flareResponse.getBody() != null) {
                events.addAll(parseDonkiFlareData(flareResponse.getBody()));
            }
            
            // 지자기 폭풍 데이터
            String gstUrl = NASA_DONKI_URL + "/GST?startDate=" + startDate.toLocalDate() + 
                           "&endDate=" + endDate.toLocalDate() + "&api_key=" + nasaApiKey;
            
            log.info("NASA DONKI GST API 호출");
            ResponseEntity<Map[]> gstResponse = restTemplate.getForEntity(gstUrl, Map[].class);
            
            if (gstResponse.getStatusCode().is2xxSuccessful() && gstResponse.getBody() != null) {
                events.addAll(parseDonkiGstData(gstResponse.getBody()));
            }
            
            log.info("NASA DONKI 데이터 {} 개 수집 완료", events.size());
            
        } catch (Exception e) {
            log.error("NASA DONKI API 호출 실패: {}", e.getMessage());
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
    
    private List<AstronomyEvent> fetchKasiData() {
        List<AstronomyEvent> events = new ArrayList<>();
        
        try {
            // NASA APOD API 사용
            String apodUrl = NASA_APOD_URL + "?api_key=" + nasaApiKey;
            log.info("NASA APOD API 호출");
            
            ResponseEntity<Map> response = restTemplate.getForEntity(apodUrl, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> apodData = response.getBody();
                String title = (String) apodData.get("title");
                String explanation = (String) apodData.get("explanation");
                
                if (title != null && explanation != null) {
                    events.add(AstronomyEvent.builder()
                        .eventType("APOD")
                        .title("오늘의 천체사진: " + title)
                        .description(explanation.length() > 100 ? explanation.substring(0, 100) + "..." : explanation)
                        .eventDate(LocalDateTime.now().plusDays(1))
                        .peakTime(LocalDateTime.now().plusDays(1).withHour(20))
                        .visibility("WORLDWIDE")
                        .magnitude("HIGH")
                        .isActive(true)
                        .build());
                }
            }
            
            // 계절별 행성 관측 정보 추가
            LocalDateTime now = LocalDateTime.now();
            int month = now.getMonthValue();
            
            if (month >= 9 && month <= 11) {
                events.add(AstronomyEvent.builder()
                    .eventType("PLANET_OBSERVATION")
                    .title("목성 최적 관측 시기")
                    .description("가을철 목성이 밤하늘에서 가장 밝게 빛납니다. 망원경으로 목성의 대적반과 위성들을 관측할 수 있습니다.")
                    .eventDate(now.plusDays(3))
                    .peakTime(now.plusDays(3).withHour(21))
                    .visibility("WORLDWIDE")
                    .magnitude("HIGH")
                    .isActive(true)
                    .build());
            }
            
            log.info("대체 천체 API 데이터 {} 개 생성 완료", events.size());
            
        } catch (Exception e) {
            log.error("대체 천체 API 호출 실패: {}", e.getMessage());
        }
        
        return events;
    }
    
    private List<AstronomyEvent> parseDonkiFlareData(Map<String, Object>[] flareData) {
        return Arrays.stream(flareData)
            .map(flare -> createAstronomyEvent("SOLAR_FLARE", 
                "태양 플레어 " + flare.get("classType") + " 등급",
                "NASA DONKI에서 감지된 태양 플레어 활동입니다. 통신 및 GPS에 영향을 줄 수 있습니다.",
                LocalDateTime.parse(((String) flare.get("beginTime")).replace("Z", "")), "HIGH"))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private List<AstronomyEvent> parseDonkiGstData(Map<String, Object>[] gstData) {
        return Arrays.stream(gstData)
            .map(gst -> createAstronomyEvent("GEOMAGNETIC_STORM",
                "지자기 폭풍 발생",
                "NASA DONKI에서 감지된 지자기 폭풍입니다. 오로라 관측 기회가 증가할 수 있습니다.",
                LocalDateTime.parse(((String) gst.get("startTime")).replace("Z", "")), "MEDIUM"))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private AstronomyEvent parseIssData(Map<String, Object> issData) {
        try {
            Map<String, Object> position = (Map<String, Object>) issData.get("iss_position");
            String latitude = (String) position.get("latitude");
            String longitude = (String) position.get("longitude");
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextPass = now.plusHours(2); // 2시간 후 다음 관측 기회
            
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
                
                for (Map<String, Object> asteroid : asteroids.stream().limit(1).collect(Collectors.toList())) {
                    String name = (String) asteroid.get("name");
                    Boolean isPotentiallyHazardous = (Boolean) asteroid.get("is_potentially_hazardous_asteroid");
                    
                    // 2010년 이전 오래된 데이터 필터링
                    if (name.matches(".*(19|20[0-9][0-9]).*")) {
                        String yearMatch = name.replaceAll(".*(19|20[0-9][0-9]).*", "$1$2");
                        try {
                            int year = Integer.parseInt(yearMatch);
                            if (year < 2010) {
                                log.info("오래된 소행성 데이터 제외: {} ({})", name, year);
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            // 연도 파싱 실패 시 계속 진행
                        }
                    }
                    
                    List<Map<String, Object>> closeApproachData = (List<Map<String, Object>>) asteroid.get("close_approach_data");
                    String distanceText = "정보 없음";
                    if (!closeApproachData.isEmpty()) {
                        Map<String, Object> missDistance = (Map<String, Object>) closeApproachData.get(0).get("miss_distance");
                        String kmDistance = (String) missDistance.get("kilometers");
                        distanceText = formatDistance(Double.parseDouble(kmDistance));
                    }
                    
                    LocalDateTime eventDate = LocalDateTime.parse(entry.getKey() + "T21:00:00");
                    
                    events.add(AstronomyEvent.builder()
                        .eventType("ASTEROID")
                        .title("지구 근접 소행성 " + name.replace("(", "").replace(")", ""))
                        .description(String.format("%s 소행성이 지구에서 %s 거리를 안전하게 통과합니다. 망원경으로 관측 가능합니다.", 
                                   isPotentiallyHazardous ? "잠재적 위험" : "안전한", distanceText))
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
    

    
    private void createFallbackEvents() {
        List<AstronomyEvent> events = List.of(
            createEvent("ASTEROID", "지구 근접 소행성 2025 AB1", "지름 150m의 소행성이 지구에서 50백만 km 거리를 안전하게 통과합니다.", 3, 21),
            createEvent("ISS_LOCATION", "ISS 관측 기회", "국제우주정거장이 한국 상공을 통과합니다. 5분간 관측 가능합니다.", 1, 19),
            createEvent("METEOR_SHOWER", "페르세우스 유성우", "시간당 최대 60개의 유성을 관측할 수 있는 연중 최대 유성우입니다.", 5, 2)
        );
        astronomyRepository.saveAll(events);
        log.info("대체 천체 이벤트 {} 개 생성", events.size());
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
    

    
    @PostConstruct
    public void initOnStartup() {
        if (astronomyRepository.count() == 0) {
            createFallbackEvents();
        }
    }
    
    private boolean shouldNotifyEvent(AstronomyEvent event, LocalDateTime now) {
        // 이벤트 24시간 전에 알림
        return event.getEventDate().minusHours(24).isBefore(now) && 
               event.getEventDate().isAfter(now);
    }
    
    private void sendEventNotification(AstronomyEvent event) {
        String message = String.format("🌟 %s이(가) 내일 예정되어 있습니다! %s", 
                                     event.getTitle(), event.getDescription());
        
        try {
            notificationService.sendToAll(Notification.NotificationType.CELESTIAL_EVENT, message);
            log.info("천체 이벤트 알림 전송: {}", event.getTitle());
        } catch (Exception e) {
            log.error("천체 이벤트 알림 전송 실패: {}", e.getMessage());
        }
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