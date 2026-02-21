package com.byeolnight.service.weather;

import com.byeolnight.dto.weather.IssObservationResponse;
import com.github.amsacode.predict4java.GroundStationPosition;
import com.github.amsacode.predict4java.PassPredictor;
import com.github.amsacode.predict4java.SatPassTime;
import com.github.amsacode.predict4java.SatPos;
import com.github.amsacode.predict4java.TLE;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class IssService {

    private final TleFetchService tleFetchService;
    private final MeterRegistry meterRegistry;

    // ISS 궤도 속도는 거의 일정 (~27,600 km/h)
    private static final double ISS_ORBITAL_VELOCITY_KMH = 27600.0;

    // 패스 예측 캐시: 위도/경도 1도 그리드 → 계산 결과 (TTL 2시간)
    private final Map<String, CachedPassData> passCache = new ConcurrentHashMap<>();
    private static final long PASS_CACHE_TTL_MS = TimeUnit.HOURS.toMillis(2);

    public IssService(TleFetchService tleFetchService, MeterRegistry meterRegistry) {
        this.tleFetchService = tleFetchService;
        this.meterRegistry = meterRegistry;
    }

    public IssObservationResponse getIssObservationOpportunity(double latitude, double longitude) {
        try {
            IssPassData nextPass = calculateNextIssPass(latitude, longitude);
            Double altitudeKm = calculateCurrentAltitude(latitude, longitude);

            return IssObservationResponse.builder()
                .messageKey("iss.detailed_status")
                .friendlyMessage("")
                .currentAltitudeKm(altitudeKm != null ? altitudeKm : 408.0)
                .currentVelocityKmh(ISS_ORBITAL_VELOCITY_KMH)
                .nextPassTime(nextPass.getNextPassTime())
                .nextPassDate(nextPass.getNextPassDate())
                .nextPassDirection(nextPass.getNextPassDirection())
                .estimatedDuration(nextPass.getEstimatedDuration())
                .visibilityQuality(nextPass.getVisibilityQuality())
                .maxElevation(nextPass.getMaxElevation())
                .build();

        } catch (Exception e) {
            log.error("ISS 관측 정보 조회 실패: {}", e.getMessage());
            return createFallbackIssInfo();
        }
    }

    /**
     * SGP4로 ISS 현재 고도를 계산 (외부 API 의존 없음)
     */
    private Double calculateCurrentAltitude(double latitude, double longitude) {
        try {
            TLE tle = tleFetchService.getIssTle();
            if (tle == null) return null;

            GroundStationPosition observer = new GroundStationPosition(latitude, longitude, 0);
            PassPredictor predictor = new PassPredictor(tle, observer);
            SatPos satPos = predictor.getSatPos(new Date());

            return satPos.getAltitude();
        } catch (Exception e) {
            log.warn("ISS 고도 계산 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * TLE + SGP4를 사용하여 실제 ISS 패스를 계산.
     * 위도/경도를 1도 단위로 그리드화하여 캐싱 (한국 내 사용자는 대부분 같은 캐시 히트).
     */
    private IssPassData calculateNextIssPass(double latitude, double longitude) {
        // 1도 단위 그리드 캐시 키
        String cacheKey = String.format("iss:%d:%d", Math.round(latitude), Math.round(longitude));

        // 캐시 확인 (만료되지 않은 경우)
        CachedPassData cached = passCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("ISS 패스 캐시 HIT: {}", cacheKey);
            meterRegistry.counter("cache.iss.hit").increment();
            return cached.getData();
        }

        // TLE 기반 실제 계산
        meterRegistry.counter("cache.iss.miss").increment();
        try {
            TLE tle = tleFetchService.getIssTle();
            if (tle == null) {
                log.warn("TLE 데이터 없음, 폴백 사용");
                return createFallbackPassData();
            }

            GroundStationPosition observer = new GroundStationPosition(latitude, longitude, 0);
            PassPredictor predictor = new PassPredictor(tle, observer);
            SatPassTime passTime = predictor.nextSatPass(new Date());

            if (passTime == null) {
                log.warn("다음 ISS 패스를 찾을 수 없음: lat={}, lon={}", latitude, longitude);
                return createFallbackPassData();
            }

            // 패스 시작/종료 시간으로 지속 시간 계산
            Date startTime = passTime.getStartTime();
            Date endTime = passTime.getEndTime();
            long durationMinutes = (endTime.getTime() - startTime.getTime()) / 60000;

            // 방위각 → 방향 문자열 변환
            String direction = azimuthToDirection(passTime.getAosAzimuth());

            // 최대 고도각 기반 관측 품질
            double maxEl = passTime.getMaxEl();
            String quality = elevationToQuality(maxEl);

            // 시간대 변환 (KST)
            LocalDateTime passStart = startTime.toInstant()
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toLocalDateTime();

            IssPassData passData = IssPassData.builder()
                .nextPassTime(passStart.format(DateTimeFormatter.ofPattern("HH:mm")))
                .nextPassDate(passStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .nextPassDirection(direction)
                .estimatedDuration(durationMinutes + "분")
                .visibilityQuality(quality)
                .maxElevation(maxEl)
                .build();

            // 캐시 저장
            passCache.put(cacheKey, new CachedPassData(passData, System.currentTimeMillis()));
            log.info("ISS 패스 계산 완료: {} → {} 방향 {}°, {}분간, 최대고도 {}°",
                    cacheKey, passStart.format(DateTimeFormatter.ofPattern("HH:mm")),
                    passTime.getAosAzimuth(), durationMinutes, String.format("%.1f", maxEl));

            return passData;

        } catch (Exception e) {
            log.error("SGP4 패스 계산 실패: {}", e.getMessage(), e);
            return createFallbackPassData();
        }
    }

    /**
     * 방위각(0-360)을 8방향 문자열로 변환
     */
    private String azimuthToDirection(int azimuth) {
        if (azimuth < 0) azimuth += 360;
        int index = ((azimuth + 22) % 360) / 45;
        String[] directions = {"NORTH", "NORTHEAST", "EAST", "SOUTHEAST",
                               "SOUTH", "SOUTHWEST", "WEST", "NORTHWEST"};
        return directions[index];
    }

    /**
     * 최대 고도각 기반 관측 품질 판단
     */
    private String elevationToQuality(double maxElevation) {
        if (maxElevation >= 60) return "EXCELLENT";
        if (maxElevation >= 30) return "GOOD";
        if (maxElevation >= 10) return "FAIR";
        return "POOR";
    }

    private IssPassData createFallbackPassData() {
        LocalDateTime nextPass = LocalDateTime.now().plusHours(3);
        return IssPassData.builder()
            .nextPassTime(nextPass.format(DateTimeFormatter.ofPattern("HH:mm")))
            .nextPassDate(nextPass.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            .nextPassDirection("NORTHEAST")
            .estimatedDuration("5-7분")
            .visibilityQuality("GOOD")
            .maxElevation(null)
            .build();
    }

    private IssObservationResponse createFallbackIssInfo() {
        IssPassData fallbackPass = createFallbackPassData();

        return IssObservationResponse.builder()
            .messageKey("iss.fallback")
            .friendlyMessage("")
            .currentAltitudeKm(408.0)
            .currentVelocityKmh(ISS_ORBITAL_VELOCITY_KMH)
            .nextPassTime(fallbackPass.getNextPassTime())
            .nextPassDate(fallbackPass.getNextPassDate())
            .nextPassDirection(fallbackPass.getNextPassDirection())
            .estimatedDuration(fallbackPass.getEstimatedDuration())
            .visibilityQuality(fallbackPass.getVisibilityQuality())
            .maxElevation(null)
            .build();
    }

    @Getter
    @Builder
    private static class IssPassData {
        private final String nextPassTime;
        private final String nextPassDate;
        private final String nextPassDirection;
        private final String estimatedDuration;
        private final String visibilityQuality;
        private final Double maxElevation;
    }

    @Getter
    private static class CachedPassData {
        private final IssPassData data;
        private final long cachedAt;

        CachedPassData(IssPassData data, long cachedAt) {
            this.data = data;
            this.cachedAt = cachedAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - cachedAt > PASS_CACHE_TTL_MS;
        }
    }
}
