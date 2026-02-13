import { check } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';

// 캐시 히트/미스 커스텀 메트릭
export const cacheHitDuration = new Trend('cache_hit_duration', true);
export const cacheMissDuration = new Trend('cache_miss_duration', true);
export const cacheHitRate = new Rate('cache_hit_rate');

// Rate Limit 커스텀 메트릭
export const rateLimitBlockedCount = new Counter('rate_limit_blocked');
export const rateLimitAllowedCount = new Counter('rate_limit_allowed');
export const rateLimitResponseTime = new Trend('rate_limit_response_time', true);

export function checkCacheHit(res, expectedMaxMs = 100) {
  const isHit = res.timings.duration < expectedMaxMs;
  cacheHitRate.add(isHit);

  if (isHit) {
    cacheHitDuration.add(res.timings.duration);
  } else {
    cacheMissDuration.add(res.timings.duration);
  }

  return check(res, {
    'status is 200': (r) => r.status === 200,
    'response body is not empty': (r) => r.body && r.body.length > 0,
  });
}

export function checkRateLimit(res, limitStatus = 429) {
  rateLimitResponseTime.add(res.timings.duration);

  if (res.status === limitStatus) {
    rateLimitBlockedCount.add(1);
  } else {
    rateLimitAllowedCount.add(1);
  }

  return res.status;
}

export function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}
