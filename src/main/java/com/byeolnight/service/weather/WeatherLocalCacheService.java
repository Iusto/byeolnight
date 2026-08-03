package com.byeolnight.service.weather;

import com.byeolnight.dto.weather.WeatherResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * 로컬 메모리 기반 날씨 캐시 서비스
 *
 * <p>스케줄러가 30분마다 주요 도시를 갱신하고, 그 외 지역은 첫 요청 시 적재된다.
 *
 * <h2>만료 전략</h2>
 * <ul>
 *   <li><b>TTL 35분</b> — 스케줄 주기(30분)보다 길게 잡아, 갱신 직전에 항목이 먼저
 *       사라져 불필요한 외부 호출이 발생하는 것을 막는다. 반대로 스케줄러가 멈추면
 *       35분 뒤 만료되므로 오래된 날씨가 무기한 서빙되지 않는다.</li>
 *   <li><b>최대 10,000건</b> — 온디맨드 경로는 사용자가 보낸 임의 좌표를 키로 쓰므로
 *       상한이 없으면 메모리가 계속 증가한다. 초과 시 W-TinyLFU 정책으로 축출된다.
 *       주요 도시 70개 + 온디맨드 지역을 담기에 충분한 크기다.</li>
 * </ul>
 *
 * <h2>직접 구현 대신 Caffeine을 쓰는 이유</h2>
 * TTL만이라면 {@code ConcurrentHashMap}에 적재 시각을 함께 저장해 직접 처리할 수 있다.
 * 그러나 크기 상한과 축출 정책까지 직접 만들려면 접근 순서 관리가 필요해
 * 동시성 처리가 복잡해진다. 캐시의 본질이 아닌 부분이라 검증된 구현에 위임한다.
 */
@Slf4j
@Service
public class WeatherLocalCacheService {

    /** 스케줄 주기(30분) + 여유 5분 */
    static final Duration TTL = Duration.ofMinutes(35);

    /** 온디맨드 적재로 인한 무제한 증가 방지 */
    static final long MAX_ENTRIES = 10_000;

    private final Cache<String, WeatherResponse> cache;

    public WeatherLocalCacheService() {
        this(Ticker.systemTicker());
    }

    /** 테스트에서 시간을 제어하기 위한 생성자 */
    WeatherLocalCacheService(Ticker ticker) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(MAX_ENTRIES)
                .expireAfterWrite(TTL)
                .ticker(ticker)
                .recordStats()
                .build();
    }

    /**
     * 캐시에서 날씨 데이터 조회
     * TTL이 지난 항목은 조회되지 않는다.
     */
    public Optional<WeatherResponse> get(String cacheKey) {
        WeatherResponse cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("로컬 캐시 HIT: cacheKey={}", cacheKey);
            return Optional.of(cached);
        }
        log.debug("로컬 캐시 MISS: cacheKey={}", cacheKey);
        return Optional.empty();
    }

    /**
     * 캐시에 날씨 데이터 저장
     */
    public void put(String cacheKey, WeatherResponse data) {
        cache.put(cacheKey, data);
        log.info("로컬 캐시 저장: cacheKey={}, location={}", cacheKey, data.getLocation());
    }

    /**
     * 캐시 전체 삭제 (관리자용)
     */
    public void clear() {
        long size = cache.estimatedSize();
        cache.invalidateAll();
        log.info("로컬 캐시 전체 삭제: {}개 항목", size);
    }

    /**
     * 캐시 통계
     * 적중률과 축출 건수를 함께 노출해 캐시 동작을 관측할 수 있게 한다.
     */
    public Map<String, Object> getStats() {
        // Caffeine의 축출은 쓰기 이후 비동기로 수행되므로, 대량 적재 직후에는
        // estimatedSize()가 상한을 잠시 넘을 수 있다. 진단용 조회이므로
        // 유지보수를 먼저 실행해 정확한 수치를 반환한다.
        cache.cleanUp();

        CacheStats stats = cache.stats();
        return Map.of(
                "size", cache.estimatedSize(),
                "maxSize", MAX_ENTRIES,
                "ttlMinutes", TTL.toMinutes(),
                "hitCount", stats.hitCount(),
                "missCount", stats.missCount(),
                "hitRate", stats.hitRate(),
                "evictionCount", stats.evictionCount(),
                "keys", cache.asMap().keySet()
        );
    }
}
