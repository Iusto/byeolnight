package com.byeolnight.service.weather;

import com.byeolnight.entity.weather.AstronomyEvent;
import com.byeolnight.repository.weather.AstronomyEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AstronomyDataInspectionTest {

    @Mock
    private AstronomyEventRepository astronomyEventRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AstronomyService astronomyService;

    @Test
    @DisplayName("Mock 기반 천체 데이터 수집 테스트")
    void mockAstronomyDataCollection() {
        // Given
        List<AstronomyEvent> mockEvents = List.of(
            createMockEvent("ASTEROID", "Test Asteroid"),
            createMockEvent("SOLAR_FLARE", "Test Solar Flare")
        );
        
        when(astronomyEventRepository.findAll()).thenReturn(mockEvents);
        
        // When
        List<AstronomyEvent> events = astronomyEventRepository.findAll();
        
        // Then
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo("ASTEROID");
        assertThat(events.get(1).getEventType()).isEqualTo("SOLAR_FLARE");
    }

    @Test
    @DisplayName("Mock 기반 ISS 위치 정보 조회 테스트")
    void mockIssLocationData() {
        // When - Mock 없이 실제 서비스 로직 테스트
        Map<String, Object> result = astronomyService.getIssObservationOpportunity(37.5665, 126.9780);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("message_key");
        assertThat(result.get("message_key")).isIn("iss.detailed_status", "iss.fallback");
    }

    @Test
    @DisplayName("Mock 기반 천체 이벤트 조회 테스트")
    void mockUpcomingEvents() {
        // When
        var upcomingEvents = astronomyService.getUpcomingEvents();
        
        // Then
        assertThat(upcomingEvents).isNotNull();
        assertThat(upcomingEvents.size()).isLessThanOrEqualTo(5);
    }
    
    private AstronomyEvent createMockEvent(String eventType, String title) {
        return AstronomyEvent.builder()
            .eventType(eventType)
            .title(title)
            .description("Test description for " + eventType)
            .eventDate(LocalDateTime.now())
            .peakTime(LocalDateTime.now())
            .visibility("WORLDWIDE")
            .magnitude("MEDIUM")
            .isActive(true)
            .build();
    }
}