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
        // Given - ISS 위치 API 응답 구조에 맞게 수정
        Map<String, Object> mockResponse = Map.of(
            "altitude", 408.5,
            "velocity", 27600.0,
            "latitude", 37.5,
            "longitude", 126.9
        );
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockResponse));

        // When
        Map<String, Object> result = astronomyService.getIssObservationOpportunity(37.5665, 126.9780);

        // Then - ISS 관측 기회 정보가 포함되어 있는지 확인
        assertThat(result).containsKey("message_key");
        assertThat(result.get("message_key")).isIn("iss.detailed_status", "iss.fallback");
    }

    @Test
    @DisplayName("Spring Context와 함께 천체 데이터 수집이 정상 동작한다")
    void performAstronomyDataCollection_WithSpringContext_Success() {
        // When & Then
        assertThatCode(() -> astronomyService.fetchDailyAstronomyEvents())
            .doesNotThrowAnyException();
    }
}