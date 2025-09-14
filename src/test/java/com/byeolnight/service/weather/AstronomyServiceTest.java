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
        ReflectionTestUtils.setField(astronomyService, "kasiApiKey", "TEST_KASI_KEY");
        ReflectionTestUtils.setField(astronomyService, "kasiBaseUrl", "https://apis.data.go.kr/B090041/openapi/service");
        ReflectionTestUtils.setField(astronomyService, "kasiServiceName", "AstroEventInfoService");
    }

    @Test
    @DisplayName("ISS 관측 기회 정보를 정상적으로 조회한다")
    void getIssObservationOpportunity_Success() {
        // Given
        Map<String, Object> mockIssResponse = Map.of(
            "response", List.of(
                Map.of(
                    "risetime", 1640995200L,
                    "duration", 300
                )
            )
        );
        
        when(restTemplate.getForEntity(contains("iss-pass.json"), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockIssResponse));

        // When
        Map<String, Object> result = astronomyService.getIssObservationOpportunity(37.5665, 126.9780);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("message_key");
        assertThat(result.get("message_key")).isEqualTo("iss.basic_opportunity");
    }

    @Test
    @DisplayName("NASA NeoWs API 응답을 올바르게 파싱한다")
    void parseNeoWsData_Success() {
        // Given
        Map<String, Object> mockNeoWsResponse = Map.of(
            "near_earth_objects", Map.of(
                "2024-01-15", List.of(
                    Map.of(
                        "name", "(2024 AA1) Test Asteroid",
                        "is_potentially_hazardous_asteroid", false,
                        "estimated_diameter", Map.of(
                            "meters", Map.of(
                                "estimated_diameter_max", 150.0
                            )
                        ),
                        "close_approach_data", List.of(
                            Map.of(
                                "miss_distance", Map.of(
                                    "kilometers", "1500000.0"
                                )
                            )
                        )
                    )
                )
            )
        );

        when(restTemplate.getForEntity(contains("neo/rest/v1/feed"), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockNeoWsResponse));
        when(restTemplate.getForEntity(contains("DONKI/FLR"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));
        when(restTemplate.getForEntity(contains("DONKI/GST"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));
        when(restTemplate.getForEntity(contains("AstroEventInfoService"), eq(String.class)))
            .thenReturn(ResponseEntity.ok("<response><body><items></items></body></response>"));

        // When
        astronomyService.fetchDailyAstronomyEvents();

        // Then
        verify(astronomyEventRepository).saveAll(argThat(events -> {
            List<AstronomyEvent> eventList = (List<AstronomyEvent>) events;
            return eventList.stream().anyMatch(event -> 
                event.getEventType().equals("ASTEROID") &&
                event.getTitle().contains("Test Asteroid") &&
                event.getDescription().contains("150m")
            );
        }));
    }

    @Test
    @DisplayName("NASA DONKI 태양플레어 데이터를 올바르게 파싱한다")
    void parseDonkiFlareData_Success() {
        // Given
        Map<String, Object>[] mockFlareResponse = new Map[] {
            Map.of(
                "classType", "C1.4",
                "beginTime", "2024-01-15T10:30:00Z",
                "peakTime", "2024-01-15T10:45:00Z"
            )
        };

        when(restTemplate.getForEntity(contains("neo/rest/v1/feed"), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("near_earth_objects", Map.of())));
        when(restTemplate.getForEntity(contains("DONKI/FLR"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(mockFlareResponse));
        when(restTemplate.getForEntity(contains("DONKI/GST"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));
        when(restTemplate.getForEntity(contains("AstroEventInfoService"), eq(String.class)))
            .thenReturn(ResponseEntity.ok("<response><body><items></items></body></response>"));

        // When
        astronomyService.fetchDailyAstronomyEvents();

        // Then
        verify(astronomyEventRepository).saveAll(argThat(events -> {
            List<AstronomyEvent> eventList = (List<AstronomyEvent>) events;
            return eventList.stream().anyMatch(event -> 
                event.getEventType().equals("SOLAR_FLARE") &&
                event.getTitle().contains("C1.4") &&
                event.getDescription().contains("Solar flare class C1.4")
            );
        }));
    }

    @Test
    @DisplayName("KASI API XML 응답을 올바르게 파싱한다")
    void parseKasiXmlData_Success() {
        // Given
        String mockKasiXmlResponse = """
            <response>
                <body>
                    <items>
                        <item>
                            <astroEvent>월식</astroEvent>
                            <astroTime>0700</astroTime>
                            <locdate>20250314</locdate>
                        </item>
                        <item>
                            <astroEvent>슈퍼문</astroEvent>
                            <astroTime>2327</astroTime>
                            <locdate>20250113</locdate>
                        </item>
                    </items>
                </body>
            </response>
            """;

        when(restTemplate.getForEntity(contains("neo/rest/v1/feed"), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("near_earth_objects", Map.of())));
        when(restTemplate.getForEntity(contains("DONKI/FLR"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));
        when(restTemplate.getForEntity(contains("DONKI/GST"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));
        when(restTemplate.getForEntity(contains("AstroEventInfoService"), eq(String.class)))
            .thenReturn(ResponseEntity.ok(mockKasiXmlResponse));

        // When
        astronomyService.fetchDailyAstronomyEvents();

        // Then
        verify(astronomyEventRepository).saveAll(argThat(events -> {
            List<AstronomyEvent> eventList = (List<AstronomyEvent>) events;
            return eventList.stream().anyMatch(event -> 
                (event.getEventType().equals("LUNAR_ECLIPSE") || event.getEventType().equals("SUPERMOON")) &&
                event.getDescription().contains("한국천문연구원 공식 데이터")
            );
        }));
    }

    @Test
    @DisplayName("KASI API에서 블러드문 이벤트를 올바르게 분류한다")
    void parseKasiBloodMoonEvent_Success() {
        // Given
        String mockKasiXmlResponse = """
            <response>
                <body>
                    <items>
                        <item>
                            <astroEvent>개기월식</astroEvent>
                            <astroTime>0700</astroTime>
                            <locdate>20250314</locdate>
                        </item>
                    </items>
                </body>
            </response>
            """;

        when(restTemplate.getForEntity(contains("neo/rest/v1/feed"), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("near_earth_objects", Map.of())));
        when(restTemplate.getForEntity(contains("DONKI/FLR"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));
        when(restTemplate.getForEntity(contains("DONKI/GST"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));
        when(restTemplate.getForEntity(contains("AstroEventInfoService"), eq(String.class)))
            .thenReturn(ResponseEntity.ok(mockKasiXmlResponse));

        // When
        astronomyService.fetchDailyAstronomyEvents();

        // Then
        verify(astronomyEventRepository).saveAll(argThat(events -> {
            List<AstronomyEvent> eventList = (List<AstronomyEvent>) events;
            return eventList.stream().anyMatch(event -> 
                event.getEventType().equals("BLOOD_MOON") &&
                event.getTitle().contains("개기월식")
            );
        }));
    }

    @Test
    @DisplayName("API 호출 실패 시 빈 리스트를 반환한다")
    void apiCallFailure_ReturnsEmptyList() {
        // Given - 모든 API 호출에 대한 Mock 설정
        when(restTemplate.getForEntity(contains("neo/rest/v1/feed"), eq(Map.class)))
            .thenThrow(new RuntimeException("NeoWs API 호출 실패"));
        when(restTemplate.getForEntity(contains("DONKI/FLR"), eq(Map[].class)))
            .thenThrow(new RuntimeException("DONKI FLR API 호출 실패"));
        when(restTemplate.getForEntity(contains("DONKI/GST"), eq(Map[].class)))
            .thenThrow(new RuntimeException("DONKI GST API 호출 실패"));
        when(restTemplate.getForEntity(contains("AstroEventInfoService"), eq(String.class)))
            .thenReturn(ResponseEntity.ok("<response><body><items></items></body></response>"));

        // When & Then - 예외가 발생하지 않고 처리됨
        assertThatCode(() -> astronomyService.fetchDailyAstronomyEvents())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("KASI API 키가 비어있을 때 해당 데이터를 수집하지 않는다")
    void emptyKasiApiKey_SkipsKasiData() {
        // Given - KASI API 키를 비우기
        ReflectionTestUtils.setField(astronomyService, "kasiApiKey", "");

        when(restTemplate.getForEntity(contains("neo/rest/v1/feed"), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("near_earth_objects", Map.of())));
        when(restTemplate.getForEntity(contains("DONKI/FLR"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));
        when(restTemplate.getForEntity(contains("DONKI/GST"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));

        // When
        astronomyService.fetchDailyAstronomyEvents();

        // Then - KASI API는 호출되지 않음
        verify(restTemplate, never()).getForEntity(contains("AstroEventInfoService"), eq(String.class));
    }

    @Test
    @DisplayName("최근 30일 내 천체 이벤트만 조회한다")
    void getUpcomingEvents_FiltersByDate() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        
        List<AstronomyEvent> mockEvents = List.of(
            createMockEvent("ASTEROID", now.minusDays(5)),
            createMockEvent("LUNAR_ECLIPSE", now.minusDays(35))
        );
        
        when(astronomyEventRepository.findUpcomingEvents(any(LocalDateTime.class)))
            .thenReturn(mockEvents);

        // When
        var result = astronomyService.getUpcomingEvents();

        // Then
        verify(astronomyEventRepository).findUpcomingEvents(argThat(date -> 
            date.isBefore(now) && date.isAfter(thirtyDaysAgo.minusMinutes(1))
        ));
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("ISS Pass API 응답이 비어있을 때 적절한 메시지를 반환한다")
    void getIssObservationOpportunity_NoPassesAvailable() {
        // Given
        Map<String, Object> mockEmptyResponse = Map.of(
            "response", List.of()
        );
        
        when(restTemplate.getForEntity(contains("iss-pass.json"), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockEmptyResponse));

        // When
        Map<String, Object> result = astronomyService.getIssObservationOpportunity(37.5665, 126.9780);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("message_key");
        assertThat(result.get("message_key")).isEqualTo("iss.no_passes");
    }

    @Test
    @DisplayName("5가지 천체 이벤트 타입이 모두 지원된다")
    void supportsFiveAstronomyEventTypes() {
        // Given
        String mockKasiXmlResponse = """
            <response>
                <body>
                    <items>
                        <item>
                            <astroEvent>월식</astroEvent>
                            <astroTime>0700</astroTime>
                            <locdate>20250314</locdate>
                        </item>
                        <item>
                            <astroEvent>일식</astroEvent>
                            <astroTime>1148</astroTime>
                            <locdate>20250329</locdate>
                        </item>
                        <item>
                            <astroEvent>슈퍼문</astroEvent>
                            <astroTime>2327</astroTime>
                            <locdate>20250113</locdate>
                        </item>
                        <item>
                            <astroEvent>개기월식</astroEvent>
                            <astroTime>0700</astroTime>
                            <locdate>20250314</locdate>
                        </item>
                        <item>
                            <astroEvent>블루문</astroEvent>
                            <astroTime>1845</astroTime>
                            <locdate>20250531</locdate>
                        </item>
                    </items>
                </body>
            </response>
            """;

        when(restTemplate.getForEntity(contains("neo/rest/v1/feed"), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("near_earth_objects", Map.of())));
        when(restTemplate.getForEntity(contains("DONKI/FLR"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));
        when(restTemplate.getForEntity(contains("DONKI/GST"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));
        when(restTemplate.getForEntity(contains("AstroEventInfoService"), eq(String.class)))
            .thenReturn(ResponseEntity.ok(mockKasiXmlResponse));

        // When
        astronomyService.fetchDailyAstronomyEvents();

        // Then - 5가지 이벤트 타입이 모두 처리됨
        verify(astronomyEventRepository).saveAll(argThat(events -> {
            List<AstronomyEvent> eventList = (List<AstronomyEvent>) events;
            return eventList.stream().anyMatch(event -> event.getEventType().equals("LUNAR_ECLIPSE")) &&
                   eventList.stream().anyMatch(event -> event.getEventType().equals("SOLAR_ECLIPSE")) &&
                   eventList.stream().anyMatch(event -> event.getEventType().equals("SUPERMOON")) &&
                   eventList.stream().anyMatch(event -> event.getEventType().equals("BLOOD_MOON")) &&
                   eventList.stream().anyMatch(event -> event.getEventType().equals("BLUE_MOON"));
        }));
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