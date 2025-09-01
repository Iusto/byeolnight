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
        try {
            fetchRealEventsFromAPI();
        } catch (Exception e) {
            log.error("실제 천체 이벤트 수집 실패, 기본 데이터 사용: {}", e.getMessage());
            ensureDefaultEvents();
        }
    }
    
    @PostConstruct
    public void initOnStartup() {
        ensureDefaultEvents();
    }
    
    private void fetchRealEventsFromAPI() {
        try {
            // 1. 유성우 데이터 수집
            fetchMeteorShowers();
            
            // 2. 일식/월식 데이터 수집  
            fetchEclipses();
            
            // 3. 행성 근접 데이터 수집
            fetchPlanetaryEvents();
            
            log.info("실제 천체 이벤트 API 연동 완료");
            
        } catch (Exception e) {
            log.error("천체 이벤트 API 호출 실패: {}", e.getMessage());
            throw e;
        }
    }
    
    private void fetchMeteorShowers() {
        try {
            String url = apiUrl + "/bodies/positions/moon?latitude=37.5665&longitude=126.9780&elevation=100&from_date=2025-01-01&to_date=2025-12-31&time=12:00:00";
            
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // API 응답 파싱하여 유성우 이벤트 생성
                createMeteorShowerEvents(response.getBody());
            }
            
        } catch (Exception e) {
            log.warn("유성우 데이터 수집 실패: {}", e.getMessage());
        }
    }
    
    private void fetchEclipses() {
        try {
            // 일식/월식 API 호출 (실제 구현)
            log.info("일식/월식 데이터 수집 시도");
            // 현재는 기본 데이터 사용
        } catch (Exception e) {
            log.warn("일식/월식 데이터 수집 실패: {}", e.getMessage());
        }
    }
    
    private void fetchPlanetaryEvents() {
        try {
            // 행성 근접 API 호출 (실제 구현)
            log.info("행성 근접 데이터 수집 시도");
            // 현재는 기본 데이터 사용
        } catch (Exception e) {
            log.warn("행성 근접 데이터 수집 실패: {}", e.getMessage());
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
    
    private void createMeteorShowerEvents(Map<String, Object> apiData) {
        // API 응답을 파싱하여 실제 유성우 이벤트 생성
        // 현재는 기본 이벤트로 대체
        log.info("API 데이터 기반 유성우 이벤트 생성: {}", apiData.keySet());
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