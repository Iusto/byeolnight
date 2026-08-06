package com.byeolnight.service.weather;

import com.byeolnight.dto.weather.WeatherResponse;
import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("날씨 로컬 캐시 서비스")
class WeatherLocalCacheServiceTest {

    /** 테스트에서 시간을 임의로 진행시키기 위한 가짜 시계 */
    private static class FakeTicker implements Ticker {
        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }

    private WeatherResponse weather(String location) {
        return WeatherResponse.builder()
                .location(location)
                .latitude(37.57)
                .longitude(126.98)
                .cloudCover(10.0)
                .visibility(10.0)
                .moonPhase("🌕")
                .observationQuality("EXCELLENT")
                .recommendation("EXCELLENT")
                .observationTime("2026-08-03 00:00")
                .dataStatus(WeatherResponse.DataStatus.FRESH)
                .lastSuccessfulAt("2026-08-03 00:00")
                .build();
    }

    @Nested
    @DisplayName("만료 전략")
    class Expiration {

        @Test
        @DisplayName("TTL 이내에는 캐시된 값을 반환한다")
        void returnsValueBeforeTtl() {
            FakeTicker ticker = new FakeTicker();
            WeatherLocalCacheService cache = new WeatherLocalCacheService(ticker);
            cache.put("wx:37.57:126.98", weather("서울"));

            ticker.advance(WeatherLocalCacheService.FRESH_TTL.minusMinutes(1));

            Optional<WeatherResponse> result = cache.get("wx:37.57:126.98");
            assertThat(result).isPresent();
            assertThat(result.get().getLocation()).isEqualTo("서울");
            assertThat(result.get().getDataStatus()).isEqualTo(WeatherResponse.DataStatus.FRESH);
        }

        @Test
        @DisplayName("신선도 TTL이 지나면 마지막 성공 데이터를 STALE로 반환한다")
        void returnsStaleAfterFreshTtl() {
            FakeTicker ticker = new FakeTicker();
            WeatherLocalCacheService cache = new WeatherLocalCacheService(ticker);
            cache.put("wx:37.57:126.98", weather("서울"));

            ticker.advance(WeatherLocalCacheService.FRESH_TTL.plusMinutes(1));

            Optional<WeatherResponse> result = cache.get("wx:37.57:126.98");
            assertThat(result).isPresent();
            assertThat(result.get().getDataStatus()).isEqualTo(WeatherResponse.DataStatus.STALE);
            assertThat(result.get().getLastSuccessfulAt()).isEqualTo("2026-08-03 00:00");
        }

        @Test
        @DisplayName("stale 보존 시간이 지나면 캐시에서 제거한다")
        void expiresAfterStaleRetention() {
            FakeTicker ticker = new FakeTicker();
            WeatherLocalCacheService cache = new WeatherLocalCacheService(ticker);
            cache.put("wx:37.57:126.98", weather("서울"));

            ticker.advance(WeatherLocalCacheService.STALE_RETENTION.plusMinutes(1));

            assertThat(cache.get("wx:37.57:126.98")).isEmpty();
        }

        @Test
        @DisplayName("TTL은 스케줄 주기(30분)보다 길어야 갱신 직전 만료를 피할 수 있다")
        void ttlCoversSchedulerInterval() {
            assertThat(WeatherLocalCacheService.FRESH_TTL)
                    .isGreaterThan(Duration.ofMinutes(30));
        }
    }

    @Nested
    @DisplayName("크기 제한")
    class SizeLimit {

        @Test
        @DisplayName("상한을 초과해 적재해도 항목 수가 상한 근처로 유지된다")
        void evictsBeyondMaxEntries() {
            WeatherLocalCacheService cache = new WeatherLocalCacheService();

            long overflow = WeatherLocalCacheService.MAX_ENTRIES + 2_000;
            for (long i = 0; i < overflow; i++) {
                cache.put("wx:key:" + i, weather("지역" + i));
            }

            long size = (long) cache.getStats().get("size");
            assertThat(size)
                    .as("온디맨드 적재로 인한 무제한 증가가 방지되어야 한다")
                    .isLessThanOrEqualTo(WeatherLocalCacheService.MAX_ENTRIES);
        }
    }

    @Nested
    @DisplayName("통계")
    class Stats {

        @Test
        @DisplayName("적중/미스 횟수를 집계한다")
        void recordsHitAndMiss() {
            WeatherLocalCacheService cache = new WeatherLocalCacheService();
            cache.put("wx:37.57:126.98", weather("서울"));

            cache.get("wx:37.57:126.98");  // hit
            cache.get("wx:00.00:000.00");  // miss

            assertThat(cache.getStats())
                    .containsEntry("hitCount", 1L)
                    .containsEntry("missCount", 1L);
        }

        @Test
        @DisplayName("clear 후 캐시가 비워진다")
        void clearEmptiesCache() {
            WeatherLocalCacheService cache = new WeatherLocalCacheService();
            cache.put("wx:37.57:126.98", weather("서울"));

            cache.clear();

            assertThat(cache.get("wx:37.57:126.98")).isEmpty();
            assertThat((long) cache.getStats().get("size")).isZero();
        }
    }
}
