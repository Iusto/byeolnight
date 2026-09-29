package com.byeolnight.service.weather;

import com.byeolnight.config.WeatherCityConfig;
import com.byeolnight.dto.external.weather.OpenWeatherResponse;
import com.byeolnight.dto.weather.WeatherResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.byeolnight.infrastructure.util.CoordinateUtils.generateCacheKey;

/**
 * 날씨 데이터 스케줄 수집 서비스
 * - 30분마다 주요 도시 날씨 수집
 * - 로컬 캐시에 저장
 * - OpenWeatherClient의 공통 Rate Limiter를 통해 호출 속도 제한
 */
@Slf4j
@Service
public class WeatherScheduler {

    private final WeatherLocalCacheService cacheService;
    private final WeatherCityConfig cityConfig;
    private final OpenWeatherClient openWeatherClient;
    private final MeterRegistry meterRegistry;
    private final ObservationScoreService observationScoreService;
    private final Sleeper sleeper;

    static final int MAX_ATTEMPTS = 2;
    static final long RETRY_DELAY_MILLIS = 1_000;

    public WeatherScheduler(WeatherLocalCacheService cacheService,
                            WeatherCityConfig cityConfig,
                            OpenWeatherClient openWeatherClient,
                            MeterRegistry meterRegistry,
                            ObservationScoreService observationScoreService) {
        this(cacheService, cityConfig, openWeatherClient, meterRegistry, observationScoreService, Thread::sleep);
    }

    WeatherScheduler(WeatherLocalCacheService cacheService,
                     WeatherCityConfig cityConfig,
                     OpenWeatherClient openWeatherClient,
                     MeterRegistry meterRegistry,
                     ObservationScoreService observationScoreService,
                     Sleeper sleeper) {
        this.cacheService = cacheService;
        this.cityConfig = cityConfig;
        this.openWeatherClient = openWeatherClient;
        this.meterRegistry = meterRegistry;
        this.observationScoreService = observationScoreService;
        this.sleeper = sleeper;
    }

    /**
     * 30분마다 주요 도시 날씨 수집
     * - 초기 지연: 10초
     * - 반복 간격: 30분
     */
    @Scheduled(initialDelay = 10_000, fixedRate = 1_800_000) // 10초 후 시작, 30분 간격
    public void collectWeatherData() {
        log.info("===== 날씨 데이터 수집 시작 =====");
        int successCount = 0;
        int failCount = 0;

        for (WeatherCityConfig.City city : cityConfig.getCities()) {
            try {
                WeatherResponse weather = fetchWeatherDataWithRetry(city);
                String cacheKey = generateCacheKey(city.latitude(), city.longitude());
                cacheService.put(cacheKey, weather);
                successCount++;
                meterRegistry.counter("weather.scheduler.refresh.success").increment();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("날씨 수집 스케줄이 중단되었습니다: city={}", city.name());
                break;
            } catch (Exception e) {
                log.error("날씨 수집 실패 - 기존 캐시를 유지합니다: city={}, error={}", city.name(), e.getMessage());
                failCount++;
                meterRegistry.counter("weather.scheduler.refresh.failure").increment();
            }
        }

        log.info("===== 날씨 데이터 수집 완료 ===== 성공: {}, 실패: {}", successCount, failCount);
    }

    /**
     * 외부 API 호출하여 날씨 데이터 수집
     */
    private WeatherResponse fetchWeatherDataWithRetry(WeatherCityConfig.City city) throws InterruptedException {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return fetchWeatherData(city);
            } catch (RuntimeException e) {
                lastFailure = e;
                if (attempt < MAX_ATTEMPTS) {
                    meterRegistry.counter("weather.scheduler.refresh.retry").increment();
                    log.warn("날씨 수집 재시도 대기: city={}, attempt={}/{}, delayMs={}, error={}",
                            city.name(), attempt + 1, MAX_ATTEMPTS, RETRY_DELAY_MILLIS, e.getMessage());
                    sleeper.sleep(RETRY_DELAY_MILLIS);
                }
            }
        }
        throw lastFailure == null ? new IllegalStateException("날씨 수집에 실패했습니다") : lastFailure;
    }

    private WeatherResponse fetchWeatherData(WeatherCityConfig.City city) {
        OpenWeatherResponse apiResponse = openWeatherClient.fetch(city.latitude(), city.longitude());
        String moonPhase = getMoonPhaseIcon();
        ObservationScoreService.ObservationScore score = observationScoreService.calculate(
                apiResponse.getCloudCover(), apiResponse.getVisibilityKm(), moonPhase);
        String successfulAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        return WeatherResponse.builder()
                .location(city.name()) // 설정된 한글 이름 사용
                .latitude(city.latitude())
                .longitude(city.longitude())
                .cloudCover(apiResponse.getCloudCover())
                .visibility(apiResponse.getVisibilityKm())
                .moonPhase(moonPhase)
                .observationScore(score.totalScore())
                .cloudScore(score.cloudScore())
                .visibilityScore(score.visibilityScore())
                .moonScore(score.moonScore())
                .observationQuality(score.quality())
                .recommendation(score.quality())
                .observationTime(successfulAt)
                .dataStatus(WeatherResponse.DataStatus.FRESH)
                .lastSuccessfulAt(successfulAt)
                .build();
    }

    private static double toJulian(LocalDateTime dtUtc) {
        int Y = dtUtc.getYear(), M = dtUtc.getMonthValue(), D = dtUtc.getDayOfMonth();
        int A = (14 - M) / 12;
        Y = Y + 4800 - A;
        M = M + 12 * A - 3;
        long JDN = D + (153L * M + 2) / 5 + 365L * Y + Y / 4 - Y / 100 + Y / 400 - 32045;
        double frac = (dtUtc.getHour() - 12) / 24.0 + dtUtc.getMinute() / 1440.0 + dtUtc.getSecond() / 86400.0;
        return JDN + frac;
    }

    private static double moonPhaseFraction(LocalDateTime nowUtc) {
        final double SYNODIC = 29.530588853;
        final double NEWMOON_JDN = 2451550.1;
        double j = toJulian(nowUtc);
        double cycles = (j - NEWMOON_JDN) / SYNODIC;
        return cycles - Math.floor(cycles);
    }

    private static String getMoonPhase(double f) {
        if (f < 0.03 || f > 0.97) return "🌑";
        if (f < 0.22) return "🌒";
        if (Math.abs(f - 0.25) < 0.03) return "🌓";
        if (f < 0.47) return "🌔";
        if (Math.abs(f - 0.50) < 0.03) return "🌕";
        if (f < 0.72) return "🌖";
        if (Math.abs(f - 0.75) < 0.03) return "🌗";
        return "🌘";
    }

    private String getMoonPhaseIcon() {
        LocalDateTime nowUtc = LocalDateTime.now(java.time.ZoneOffset.UTC);
        double f = moonPhaseFraction(nowUtc);
        return getMoonPhase(f);
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
