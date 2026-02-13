import http from 'k6/http';
import { check, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { BASE_URL, CACHED_CITIES, UNCACHED_COORDS } from '../lib/config.js';

// 시나리오별 독립 메트릭
const hitDuration = new Trend('hit_duration', true);
const missDuration = new Trend('miss_duration', true);
const hitRate = new Rate('hit_rate');

export const options = {
  scenarios: {
    // 시나리오 1: 캐시 히트 순수 성능 (sleep 없이 최대 처리량 측정)
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
    // 시나리오 2: 캐시 미스 (외부 API 호출 유발, 소수 VU로 제어)
    cache_miss: {
      executor: 'per-vu-iterations',
      vus: 3,
      iterations: 2,
      startTime: '2m30s',  // 히트 테스트 완료 후 실행
      exec: 'cacheMissTest',
    },
  },
  thresholds: {
    'hit_duration': ['p(95)<50', 'p(99)<100'],
    'miss_duration': ['p(95)<10000'],
    'hit_rate{scenario:cache_hit}': ['rate>0.99'],
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
  hitRate.add(res.status === 200);

  check(res, {
    '[히트] status 200': (r) => r.status === 200,
    '[히트] has observationQuality': (r) => {
      try { return JSON.parse(r.body).observationQuality !== undefined; }
      catch { return false; }
    },
  });
}

// 시나리오 2: 캐시 미스 - 70개 도시 그리드 밖 좌표로 요청 (외부 API 호출)
export function cacheMissTest() {
  // VU별로 다른 좌표 사용 + iteration마다 0.01 이동해 매번 새 그리드
  const base = UNCACHED_COORDS[(__VU - 1) % UNCACHED_COORDS.length];
  const lat = base.lat + (__ITER * 0.02);
  const lon = base.lon + (__ITER * 0.02);

  const res = http.get(
    `${BASE_URL}/api/weather/observation?latitude=${lat.toFixed(4)}&longitude=${lon.toFixed(4)}`,
    { tags: { scenario: 'cache_miss' }, timeout: '30s' }
  );

  missDuration.add(res.timings.duration);
  hitRate.add(false);  // 미스로 기록

  check(res, {
    '[미스] status 200': (r) => r.status === 200,
    '[미스] response time > 100ms': (r) => r.timings.duration > 100,
  });
}
