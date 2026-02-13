import { Trend, Counter, Rate } from 'k6/metrics';

// 캐시 히트/미스 별도 메트릭
export const cacheHitDuration = new Trend('cache_hit_duration', true);
export const cacheMissDuration = new Trend('cache_miss_duration', true);
export const cacheHitRate = new Rate('cache_hit_rate');

// Rate Limit 커스텀 메트릭
export const rateLimitBlockedCount = new Counter('rate_limit_blocked');
export const rateLimitAllowedCount = new Counter('rate_limit_allowed');
export const rateLimitResponseTime = new Trend('rate_limit_response_time', true);

export function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}
