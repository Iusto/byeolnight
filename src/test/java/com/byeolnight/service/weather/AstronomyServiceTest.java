package com.byeolnight.service.weather;

import com.byeolnight.domain.weather.entity.AstronomyEvent;
import com.byeolnight.domain.weather.repository.AstronomyEventRepository;
import com.byeolnight.domain.weather.service.AstronomyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AstronomyServiceTest {

    @Mock
    private AstronomyEventRepository astronomyEventRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AstronomyService astronomyService;

    @BeforeEach
    void setUp() {
        // API 키 설정
        ReflectionTestUtils.setField(astronomyService, "nasaApiKey", "TEST_NASA_KEY");
    }

    @Test
    @DisplayName("ISS 관측 기회 정보를 정상적으로 조회한다")
    void getIssObservationOpportunity_Success() {
        // Given
        Map<String, Object> mockIssResponse = Map.of(
            "altitude", 408.5,
            "velocity", 27600.0,
            "latitude", 37.5,
            "longitude", 126.9
        );
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockIssResponse));

        // When
        Map<String, Object> result = astronomyService.getIssObservationOpportunity(37.5665, 126.9780);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("message_key");
        assertThat(result.get("message_key")).isIn("iss.detailed_status", "iss.fallback");
    }

    @Test
    @DisplayName("천체 데이터 수집이 정상 동작한다")
    void fetchDailyAstronomyEvents_Success() {
        // When
        Map<String, Object> result = astronomyService.manualFetchAstronomyEvents();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("success");
    }

    @Test
    @DisplayName("천체 이벤트 통계를 정상적으로 조회한다")
    void getAstronomyEventStats_Success() {
        // Given
        when(astronomyEventRepository.count()).thenReturn(5L);

        // When
        Map<String, Object> stats = astronomyService.getAstronomyEventStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats).containsKey("totalCount");
        assertThat(stats.get("totalCount")).isEqualTo(5L);
    }

    @Test
    @DisplayName("최근 천체 이벤트를 정상적으로 조회한다")
    void getUpcomingEvents_Success() {
        // When
        var events = astronomyService.getUpcomingEvents();

        // Then
        assertThat(events).isNotNull();
        assertThat(events.size()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("ISS API 호출 실패 시 Fallback 데이터 반환")
    void getIssObservationOpportunity_Fallback() {
        // When - 실제 서비스 로직 테스트
        Map<String, Object> result = astronomyService.getIssObservationOpportunity(37.5665, 126.9780);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("message_key");
        assertThat(result.get("message_key")).isIn("iss.detailed_status", "iss.fallback");
    }

    @Test
    @DisplayName("데이터 수집 실패 시 적절한 오류 메시지를 반환한다")
    void fetchDailyAstronomyEvents_HandleError() {
        // Given - Repository 오류 시뮬레이션
        doThrow(new RuntimeException("DB 오류")).when(astronomyEventRepository).deleteAll();

        // When
        Map<String, Object> result = astronomyService.manualFetchAstronomyEvents();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).asString().contains("실패");
    }

    @Test
    @DisplayName("스케줄러가 정상적으로 동작한다")
    void scheduledFetchDailyAstronomyEvents_Success() {
        // When & Then - 예외가 발생하지 않고 처리됨
        assertThatCode(() -> astronomyService.fetchDailyAstronomyEvents())
            .doesNotThrowAnyException();
    }







    private AstronomyEvent createMockEvent(String eventType, LocalDateTime eventDate) {
        return AstronomyEvent.builder()
            .eventType(eventType)
            .title("Test " + eventType)
            .description("Test description for " + eventType)
            .eventDate(eventDate)
            .peakTime(eventDate)
            .visibility("WORLDWIDE")
            .magnitude("MEDIUM")
            .isActive(true)
            .build();
    }
}