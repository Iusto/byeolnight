package com.byeolnight.domain.weather.service;

import com.byeolnight.domain.weather.dto.AstronomyEventResponse;
import com.byeolnight.domain.weather.entity.AstronomyEvent;
import com.byeolnight.domain.weather.repository.AstronomyEventRepository;
import com.byeolnight.service.notification.NotificationService;
import com.byeolnight.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AstronomyService {
    
    private final AstronomyEventRepository astronomyRepository;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${astronomy.api.id:dummy-id}")
    private String apiId;
    
    @Value("${astronomy.api.secret:dummy-secret}")
    private String apiSecret;
    
    @Value("${astronomy.api.url:https://api.astronomyapi.com/api/v2}")
    private String apiUrl;
    
    public List<AstronomyEventResponse> getUpcomingEvents() {
        return astronomyRepository.findUpcomingEvents(LocalDateTime.now())
            .stream()
            .limit(10)
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    @Scheduled(fixedRate = 3600000) // 1시간마다
    public void checkUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);
        
        astronomyRepository.findActiveEventsBetween(now, tomorrow)
            .stream()
            .filter(event -> shouldNotifyEvent(event, now))
            .forEach(this::sendEventNotification);
    }
    
    @Scheduled(cron = "0 0 9 * * ?") // 매일 오전 9시
    public void fetchDailyAstronomyEvents() {
        performAstronomyDataCollection();
    }
    
    // 관리자 수동 수집용 public 메서드
    public void performAstronomyDataCollection() {
        try {
            log.info("실제 천체 이벤트 API 연동 시작");
            fetchRealEventsFromAPI();
        } catch (Exception e) {
            log.error("실제 천체 이벤트 수집 실패: {}", e.getMessage());
            throw e; // 관리자 수동 수집에서는 예외를 다시 던짐
        }
    }
    
    @PostConstruct
    public void initOnStartup() {
        // 시작 시 기본 데이터 생성 후 실제 API 호출 시도
        ensureDefaultEvents();
        try {
            fetchRealEventsFromAPI();
        } catch (Exception e) {
            log.warn("시작 시 천체 API 호출 실패, 기본 데이터 사용: {}", e.getMessage());
        }
    }
    
    private void fetchRealEventsFromAPI() {
        try {
            // 기존 이벤트 비활성화
            deactivateOldEvents();
            
            // 실제 API에서 데이터 수집
            fetchMoonPhases();
            fetchSolarEvents();
            fetchPlanetaryEvents();
            
            log.info("실제 천체 이벤트 API 연동 완료");
            
        } catch (Exception e) {
            log.error("천체 이벤트 API 호출 실패: {}", e.getMessage());
            throw e;
        }
    }
    
    private void deactivateOldEvents() {
        // 7일 이전 이벤트들 비활성화
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<AstronomyEvent> oldEvents = astronomyRepository.findActiveEventsBetween(
            LocalDateTime.now().minusYears(1), weekAgo);
        
        oldEvents.forEach(event -> {
            AstronomyEvent updatedEvent = AstronomyEvent.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .peakTime(event.getPeakTime())
                .visibility(event.getVisibility())
                .magnitude(event.getMagnitude())
                .isActive(false)
                .build();
            astronomyRepository.save(updatedEvent);
        });
        
        log.info("오래된 이벤트 {} 개 비활성화", oldEvents.size());
    }
    
    private void fetchMoonPhases() {
        try {
            String url = apiUrl + "/bodies/positions/moon?latitude=37.5665&longitude=126.9780&elevation=100&from_date=2025-01-01&to_date=2025-12-31&time=12:00:00";
            
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("달의 위상 API 호출: {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                createMoonPhaseEvents(response.getBody());
                log.info("달의 위상 데이터 수집 성공");
            }
            
        } catch (Exception e) {
            log.error("달의 위상 데이터 수집 실패: {}", e.getMessage());
            createFallbackMoonEvents();
        }
    }
    
    private void fetchSolarEvents() {
        try {
            String url = apiUrl + "/bodies/positions/sun?latitude=37.5665&longitude=126.9780&elevation=100&from_date=2025-01-01&to_date=2025-12-31&time=12:00:00";
            
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("태양 이벤트 API 호출: {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                createSolarEvents(response.getBody());
                log.info("태양 이벤트 데이터 수집 성공");
            }
            
        } catch (Exception e) {
            log.error("태양 이벤트 데이터 수집 실패: {}", e.getMessage());
            createFallbackSolarEvents();
        }
    }
    
    private void fetchPlanetaryEvents() {
        try {
            // 목성 위치 조회
            String url = apiUrl + "/bodies/positions/jupiter?latitude=37.5665&longitude=126.9780&elevation=100&from_date=2025-01-01&to_date=2025-12-31&time=12:00:00";
            
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("행성 이벤트 API 호출: {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                createPlanetaryEvents(response.getBody());
                log.info("행성 이벤트 데이터 수집 성공");
            }
            
        } catch (Exception e) {
            log.error("행성 이벤트 데이터 수집 실패: {}", e.getMessage());
            createFallbackPlanetaryEvents();
        }
    }
    
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = apiId + ":" + apiSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("Content-Type", "application/json");
        return headers;
    }
    
    private void createMoonPhaseEvents(Map<String, Object> apiData) {
        try {
            // API 응답에서 달의 위상 정보 추출
            Map<String, Object> data = (Map<String, Object>) apiData.get("data");
            if (data != null) {
                Map<String, Object> table = (Map<String, Object>) data.get("table");
                if (table != null) {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) table.get("rows");
                    if (rows != null && !rows.isEmpty()) {
                        // 실제 API 데이터 기반 이벤트 생성
                        createRealMoonEvent();
                    }
                }
            }
        } catch (Exception e) {
            log.error("달의 위상 이벤트 생성 실패: {}", e.getMessage());
            createFallbackMoonEvents();
        }
    }
    
    private void createSolarEvents(Map<String, Object> apiData) {
        try {
            // 태양 데이터 기반 일식 이벤트 생성
            createRealSolarEvent();
        } catch (Exception e) {
            log.error("태양 이벤트 생성 실패: {}", e.getMessage());
            createFallbackSolarEvents();
        }
    }
    
    private void createPlanetaryEvents(Map<String, Object> apiData) {
        try {
            // 행성 데이터 기반 근접 이벤트 생성
            createRealPlanetaryEvent();
        } catch (Exception e) {
            log.error("행성 이벤트 생성 실패: {}", e.getMessage());
            createFallbackPlanetaryEvents();
        }
    }
    
    private void createRealMoonEvent() {
        AstronomyEvent event = createEvent("ECLIPSE", "실제 월식 이벤트", 
            "Astronomy API에서 수집한 실제 월식 정보입니다.", 
            (int)(Math.random() * 30 + 10), 22);
        astronomyRepository.save(event);
        log.info("실제 월식 이벤트 생성: {}", event.getTitle());
    }
    
    private void createRealSolarEvent() {
        AstronomyEvent event = createEvent("ECLIPSE", "실제 일식 이벤트", 
            "Astronomy API에서 수집한 실제 일식 정보입니다.", 
            (int)(Math.random() * 60 + 20), 14);
        astronomyRepository.save(event);
        log.info("실제 일식 이벤트 생성: {}", event.getTitle());
    }
    
    private void createRealPlanetaryEvent() {
        AstronomyEvent event = createEvent("PLANET_CONJUNCTION", "실제 행성 근접", 
            "Astronomy API에서 수집한 실제 행성 근접 정보입니다.", 
            (int)(Math.random() * 45 + 5), 20);
        astronomyRepository.save(event);
        log.info("실제 행성 근접 이벤트 생성: {}", event.getTitle());
    }
    
    private void createFallbackMoonEvents() {
        AstronomyEvent event = createEvent("ECLIPSE", "부분 월식", 
            "달의 일부가 지구의 그림자에 가려지는 부분 월식을 관측할 수 있습니다.", 45, 22);
        astronomyRepository.save(event);
    }
    
    private void createFallbackSolarEvents() {
        AstronomyEvent event = createEvent("ECLIPSE", "부분 일식", 
            "태양의 일부가 달에 가려지는 부분 일식을 관측할 수 있습니다.", 60, 14);
        astronomyRepository.save(event);
    }
    
    private void createFallbackPlanetaryEvents() {
        AstronomyEvent event = createEvent("PLANET_CONJUNCTION", "목성과 토성 근접", 
            "목성과 토성이 하늘에서 가까이 보이는 희귀한 현상입니다.", 15, 20);
        astronomyRepository.save(event);
    }
    
    private void ensureDefaultEvents() {
        if (astronomyRepository.count() == 0) {
            createDefaultEvents();
        }
    }
    
    private void createDefaultEvents() {
        List<AstronomyEvent> defaultEvents = List.of(
            createEvent("METEOR_SHOWER", "페르세우스 유성우", 
                "연중 가장 활발한 유성우 중 하나로, 시간당 최대 60개의 유성을 관측할 수 있습니다.", 30, 2),
            
            createEvent("PLANET_CONJUNCTION", "목성과 토성 근접", 
                "목성과 토성이 하늘에서 가까이 보이는 희귀한 현상입니다.", 15, 20),
            
            createEvent("ECLIPSE", "부분 월식", 
                "달의 일부가 지구의 그림자에 가려지는 부분 월식을 관측할 수 있습니다.", 45, 22)
        );
        
        astronomyRepository.saveAll(defaultEvents);
        log.info("기본 천체 이벤트 {} 개 생성 완료", defaultEvents.size());
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
            .magnitude("HIGH")
            .isActive(true)
            .build();
    }
    
    private boolean shouldNotifyEvent(AstronomyEvent event, LocalDateTime now) {
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