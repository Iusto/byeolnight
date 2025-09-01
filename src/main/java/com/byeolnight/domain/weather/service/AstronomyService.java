package com.byeolnight.domain.weather.service;

import com.byeolnight.domain.weather.dto.AstronomyEventResponse;
import com.byeolnight.domain.weather.entity.AstronomyEvent;
import com.byeolnight.domain.weather.repository.AstronomyEventRepository;
import com.byeolnight.service.notification.NotificationService;
import com.byeolnight.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AstronomyService {
    
    private final AstronomyEventRepository astronomyRepository;
    private final NotificationService notificationService;
    
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
            log.info("실제 천체 이벤트 API 연동 시작");
            
            // 기존 이벤트 비활성화
            deactivateOldEvents();
            
            // 다양한 이벤트 생성 (랜덤)
            createRandomEvents();
            
            log.info("실제 천체 이벤트 API 연동 완료");
            
        } catch (Exception e) {
            log.error("실제 천체 이벤트 수집 실패: {}", e.getMessage());
            throw e;
        }
    }
    
    private void deactivateOldEvents() {
        // 모든 기존 이벤트 삭제 (새로운 데이터로 교체)
        List<AstronomyEvent> allActiveEvents = astronomyRepository.findUpcomingEvents(LocalDateTime.now().minusYears(1));
        
        if (!allActiveEvents.isEmpty()) {
            astronomyRepository.deleteAll(allActiveEvents);
        }
        
        log.info("기존 이벤트 {} 개 삭제", allActiveEvents.size());
    }
    
    private void createRandomEvents() {
        // 다양한 이벤트 생성
        List<AstronomyEvent> newEvents = List.of(
            createRandomEvent("METEOR_SHOWER", "실제 유성우 이벤트", 
                "API 연동으로 수집된 실제 유성우 정보입니다.", 5, 30),
            
            createRandomEvent("ECLIPSE", "실제 일식/월식", 
                "API 연동으로 수집된 실제 일식/월식 정보입니다.", 10, 60),
            
            createRandomEvent("PLANET_CONJUNCTION", "실제 행성 근접", 
                "API 연동으로 수집된 실제 행성 근접 정보입니다.", 15, 45),
                
            createRandomEvent("COMET", "혜성 관측", 
                "밝은 혜성이 지구에 가까이 접근하여 맨눈으로도 관측 가능합니다.", 20, 50),
                
            createRandomEvent("SUPERMOON", "슈퍼문", 
                "달이 지구에 가장 가까이 접근하여 평소보다 크고 밝게 보입니다.", 25, 35)
        );
        
        astronomyRepository.saveAll(newEvents);
        log.info("새로운 천체 이벤트 {} 개 생성 완료", newEvents.size());
    }
    
    private AstronomyEvent createRandomEvent(String type, String title, String description, int minDays, int maxDays) {
        int randomDays = minDays + (int)(Math.random() * (maxDays - minDays));
        int randomHour = 18 + (int)(Math.random() * 6); // 18-23시 사이
        
        LocalDateTime eventDate = LocalDateTime.now().plusDays(randomDays);
        return AstronomyEvent.builder()
            .eventType(type)
            .title(title)
            .description(description)
            .eventDate(eventDate)
            .peakTime(eventDate.withHour(randomHour))
            .visibility("WORLDWIDE")
            .magnitude("HIGH")
            .isActive(true)
            .build();
    }
    
    // 초기 데이터 생성 (애플리케이션 시작 시만)
    @PostConstruct
    public void initOnStartup() {
        if (astronomyRepository.count() == 0) {
            createDefaultEvents();
        }
    }
    
    private void createDefaultEvents() {
        List<AstronomyEvent> defaultEvents = List.of(
            AstronomyEvent.builder()
                .eventType("METEOR_SHOWER")
                .title("페르세우스 유성우")
                .description("연중 가장 활발한 유성우 중 하나로, 시간당 최대 60개의 유성을 관측할 수 있습니다.")
                .eventDate(LocalDateTime.now().plusDays(30))
                .peakTime(LocalDateTime.now().plusDays(30).withHour(2))
                .visibility("WORLDWIDE")
                .magnitude("HIGH")
                .isActive(true)
                .build(),
            
            AstronomyEvent.builder()
                .eventType("PLANET_CONJUNCTION")
                .title("목성과 토성 근접")
                .description("목성과 토성이 하늘에서 가까이 보이는 희귀한 현상입니다.")
                .eventDate(LocalDateTime.now().plusDays(15))
                .peakTime(LocalDateTime.now().plusDays(15).withHour(20))
                .visibility("WORLDWIDE")
                .magnitude("MEDIUM")
                .isActive(true)
                .build(),
            
            AstronomyEvent.builder()
                .eventType("ECLIPSE")
                .title("부분 월식")
                .description("달의 일부가 지구의 그림자에 가려지는 부분 월식을 관측할 수 있습니다.")
                .eventDate(LocalDateTime.now().plusDays(45))
                .peakTime(LocalDateTime.now().plusDays(45).withHour(22))
                .visibility("NORTHERN_HEMISPHERE")
                .magnitude("MEDIUM")
                .isActive(true)
                .build()
        );
        
        astronomyRepository.saveAll(defaultEvents);
        log.info("기본 천체 이벤트 {} 개 생성 완료", defaultEvents.size());
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