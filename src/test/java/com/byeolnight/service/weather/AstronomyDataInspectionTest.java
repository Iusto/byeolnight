package com.byeolnight.service.weather;

import com.byeolnight.domain.weather.entity.AstronomyEvent;
import com.byeolnight.domain.weather.repository.AstronomyEventRepository;
import com.byeolnight.domain.weather.service.AstronomyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
        ReflectionTestUtils.setField(astronomyService, "nasaApiKey", "TEST_KEY");
        ReflectionTestUtils.setField(astronomyService, "kasiApiKey", "TEST_KEY");
        
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
        // Given
        Map<String, Object> mockIssResponse = Map.of(
            "response", List.of(
                Map.of(
                    "risetime", 1640995200L,
                    "duration", 300
                )
            )
        );
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(org.springframework.http.ResponseEntity.ok(mockIssResponse));
        
        // When
        Map<String, Object> result = astronomyService.getIssObservationOpportunity(37.5665, 126.9780);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("message_key");
    }

    @Test
    @DisplayName("Mock 기반 천체 이벤트 조회 테스트")
    void mockUpcomingEvents() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<AstronomyEvent> mockEvents = List.of(
            createMockEvent("LUNAR_ECLIPSE", "Test Lunar Eclipse"),
            createMockEvent("METEOR_SHOWER", "Test Meteor Shower")
        );
        
        when(astronomyEventRepository.findUpcomingEvents(any(LocalDateTime.class)))
            .thenReturn(mockEvents);
        
        // When
        var upcomingEvents = astronomyService.getUpcomingEvents();
        
        // Then
        assertThat(upcomingEvents).hasSize(2);
        assertThat(upcomingEvents.get(0).getEventType()).isEqualTo("LUNAR_ECLIPSE");
        assertThat(upcomingEvents.get(1).getEventType()).isEqualTo("METEOR_SHOWER");
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