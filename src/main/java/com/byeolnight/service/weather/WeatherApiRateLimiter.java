package com.byeolnight.service.weather;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.stereotype.Component;

/**
 * OpenWeather 계정의 분당 호출 한도를 지키기 위한 단일 인스턴스 공통 제한기.
 *
 * <p>무료 플랜의 분당 60회보다 여유를 둔 분당 48회(초당 0.8회)로 제한한다.
 * 스케줄 수집, 온디맨드 조회, 재시도가 모두 같은 인스턴스를 공유한다.</p>
 */
@Component
public class WeatherApiRateLimiter {

    static final double PERMITS_PER_SECOND = 0.8;

    private final RateLimiter rateLimiter;

    public WeatherApiRateLimiter() {
        this(RateLimiter.create(PERMITS_PER_SECOND));
    }

    WeatherApiRateLimiter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * 다음 호출 허용 시점까지 대기하고, 실제 대기 시간을 초 단위로 반환한다.
     */
    public double acquire() {
        return rateLimiter.acquire();
    }
}
