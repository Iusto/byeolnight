import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, CACHED_CITIES, UNCACHED_COORDS } from '../lib/config.js';

// 응답 시간 메트릭
const hitDuration = new Trend('hit_duration', true);
const missDuration = new Trend('miss_duration', true);

export const options = {
  scenarios: {
    // 시나리오 1: 캐시 히트 부하 (프리캐시된 70개 도시)
    cache_hit: {
      executor: 'ramping-vus',
      stages: [
        { duration: '15s', target: 10 },
        { duration: '30s', target: 50 },
        { duration: '1m',  target: 50 },
        { duration: '30s', target: 100 },
        { duration: '15s', target: 0 },
      ],
      exec: 'cacheHitTest',
    },
    // 시나리오 2: 캐시 미스 부하 (외부 API 호출 유발)
    cache_miss: {
      executor: 'per-vu-iterations',
      vus: 3,
      iterations: 2,
      startTime: '2m30s',
      exec: 'cacheMissTest',
    },
  },
  thresholds: {
    'hit_duration': ['p(95)<50', 'p(99)<100'],
    'miss_duration': ['p(95)<10000'],
    'http_req_failed{scenario:cache_hit}': ['rate<0.01'],
  },
};

// 시나리오 1: 캐시 히트 - 프리캐시된 70개 도시 좌표로 요청
export function cacheHitTest() {
  const city = CACHED_CITIES[Math.floor(Math.random() * CACHED_CITIES.length)];

  const res = http.get(
    `${BASE_URL}/api/weather/observation?latitude=${city.lat}&longitude=${city.lon}`,
    { tags: { scenario: 'cache_hit', city: city.name } }
  );

  hitDuration.add(res.timings.duration);

  check(res, {
    '[히트] status 200': (r) => r.status === 200,
    '[히트] has observationQuality': (r) => {
      try { return JSON.parse(r.body).observationQuality !== undefined; }
      catch { return false; }
    },
  });
}

// 시나리오 2: 캐시 미스 - 70개 도시 그리드 밖 좌표로 요청
export function cacheMissTest() {
  const base = UNCACHED_COORDS[(__VU - 1) % UNCACHED_COORDS.length];
  const lat = base.lat + (__ITER * 0.02);
  const lon = base.lon + (__ITER * 0.02);

  const res = http.get(
    `${BASE_URL}/api/weather/observation?latitude=${lat.toFixed(4)}&longitude=${lon.toFixed(4)}`,
    { tags: { scenario: 'cache_miss' }, timeout: '30s' }
  );

  missDuration.add(res.timings.duration);

  check(res, {
    '[미스] status 200': (r) => r.status === 200,
  });
}
