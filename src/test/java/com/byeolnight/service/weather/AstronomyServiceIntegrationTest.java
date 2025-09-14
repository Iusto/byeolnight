package com.byeolnight.service.weather;

import com.byeolnight.common.TestMockConfig;
import com.byeolnight.domain.weather.entity.AstronomyEvent;
import com.byeolnight.domain.weather.repository.AstronomyEventRepository;
import com.byeolnight.domain.weather.service.AstronomyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
    "nasa.api.key=TEST_API_KEY"
})
@Import({AstronomyService.class, TestMockConfig.class})
class AstronomyServiceIntegrationTest {

    @Autowired
    private AstronomyService astronomyService;

    @MockBean
    private AstronomyEventRepository astronomyEventRepository;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    @DisplayName("Spring Context와 함께 ISS 위치 정보를 정상적으로 조회한다")
    void getIssLocation_WithSpringContext_Success() {
        // Given - ISS Pass API 응답 구조에 맞게 수정
        Map<String, Object> mockResponse = Map.of(
            "response", List.of(
                Map.of(
                    "risetime", 1640995200L,
                    "duration", 300
                )
            )
        );
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockResponse));

        // When
        Map<String, Object> result = astronomyService.getIssObservationOpportunity(37.5665, 126.9780);

        // Then - ISS 관측 기회 정보가 포함되어 있는지 확인
        assertThat(result).containsKey("message_key");
        assertThat(result.get("message_key")).isIn("iss.basic_opportunity", "iss.detailed_opportunity", "iss.no_passes");
    }

    @Test
    @DisplayName("Spring Context와 함께 천체 데이터 수집이 정상 동작한다")
    void performAstronomyDataCollection_WithSpringContext_Success() {
        // Given
        when(restTemplate.getForEntity(contains("neo/rest/v1/feed"), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("near_earth_objects", Map.of())));
        when(restTemplate.getForEntity(contains("DONKI/FLR"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));
        when(restTemplate.getForEntity(contains("DONKI/GST"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));
        when(restTemplate.getForEntity(contains("planetary/apod"), eq(Map[].class)))
            .thenReturn(ResponseEntity.ok(new Map[0]));

        // When & Then
        assertThatCode(() -> astronomyService.fetchDailyAstronomyEvents())
            .doesNotThrowAnyException();
    }
}