package com.byeolnight.service.weather;

import com.byeolnight.dto.weather.WeatherResponse;
import com.byeolnight.infrastructure.util.CoordinateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 날씨 서비스
 * - 로컬 캐시에서 날씨 데이터 제공
 * - 30분마다 스케줄러가 주요 도시 날씨 자동 수집
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherLocalCacheService localCacheService;

    /**
     * 별관측 날씨 조회
     * - 로컬 캐시에서만 조회 (스케줄러가 30분마다 갱신)
     * - 캐시에 없으면 fallback 응답 반환
     */
    public WeatherResponse getObservationConditions(Double latitude, Double longitude) {
        // 좌표 반올림 & 캐시 키 생성
        String cacheKey = CoordinateUtils.generateCacheKey(latitude, longitude);

        // 로컬 캐시 확인
        Optional<WeatherResponse> cached = localCacheService.get(cacheKey);
        if (cached.isPresent()) {
            log.debug("캐시에서 날씨 반환: cacheKey={}", cacheKey);
            return cached.get();
        }

        // 캐시에 없으면 fallback 응답 (스케줄러가 곧 수집할 예정)
        log.warn("캐시 MISS: cacheKey={} - fallback 응답 반환", cacheKey);
        return createFallbackResponse(latitude, longitude);
    }

    /**
     * Fallback 응답 생성
     * - 스케줄러가 아직 해당 지역 데이터를 수집하지 않았을 때
     */
    private WeatherResponse createFallbackResponse(Double latitude, Double longitude) {
        return WeatherResponse.builder()
                .location("알 수 없음")
                .latitude(latitude)
                .longitude(longitude)
                .cloudCover(50.0)
                .visibility(10.0)
                .moonPhase("🌙")
                .observationQuality("UNKNOWN")
                .recommendation("UNKNOWN")
                .observationTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }
}
