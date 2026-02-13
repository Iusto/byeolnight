import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASE_URL, ISS_UNIQUE_GRIDS, ISS_HIT_CITIES } from '../lib/config.js';

// 시나리오별 독립 메트릭
const firstAccessDuration = new Trend('first_access_duration', true);
const cachedAccessDuration = new Trend('cached_access_duration', true);
const lazyLoadSpeedup = new Rate('lazy_load_speedup');
const hitDuration = new Trend('hit_duration', true);

export const options = {
  scenarios: {
    // 시나리오 1: Lazy loading 검증 (고유 그리드 좌표로 미스 → 히트 순차 측정)
    lazy_loading: {
      executor: 'per-vu-iterations',
      vus: 5,
      iterations: 2,
      exec: 'lazyLoadingTest',
    },
    // 시나리오 2: 캐시 히트 부하 테스트 (lazy loading으로 워밍업된 후)
    cache_hit: {
      executor: 'ramping-vus',
      stages: [
        { duration: '15s', target: 10 },
        { duration: '30s', target: 50 },
        { duration: '1m',  target: 50 },
        { duration: '30s', target: 100 },
        { duration: '15s', target: 0 },
      ],
      startTime: '1m',  // lazy loading 완료 후 실행
      exec: 'cacheHitTest',
    },
  },
  thresholds: {
    'hit_duration': ['p(95)<50'],
    'cached_access_duration': ['p(95)<50'],
    'http_req_failed': ['rate<0.01'],
  },
};

// 시나리오 1: Lazy loading - 첫 요청(미스) → 재요청(히트) 비교
export function lazyLoadingTest() {
  // VU * iteration 조합으로 고유 그리드 보장
  const idx = ((__VU - 1) * 2 + __ITER) % ISS_UNIQUE_GRIDS.length;
  const coord = ISS_UNIQUE_GRIDS[idx];

  // 1st 요청: 캐시 미스 (TLE 궤도 계산 발생)
  const res1 = http.get(
    `${BASE_URL}/api/weather/iss?latitude=${coord.lat}&longitude=${coord.lon}`,
    { tags: { scenario: 'lazy_first' }, timeout: '30s' }
  );

  firstAccessDuration.add(res1.timings.duration);

  check(res1, {
    '[Lazy 1st] status 200': (r) => r.status === 200,
  });

  sleep(0.5);

  // 2nd 요청: 캐시 히트 (동일 그리드)
  const res2 = http.get(
    `${BASE_URL}/api/weather/iss?latitude=${coord.lat}&longitude=${coord.lon}`,
    { tags: { scenario: 'lazy_second' } }
  );

  cachedAccessDuration.add(res2.timings.duration);

  const isFaster = res2.timings.duration < res1.timings.duration * 0.5;
  lazyLoadSpeedup.add(isFaster);

  check(res2, {
    '[Lazy 2nd] status 200': (r) => r.status === 200,
    '[Lazy 2nd] 50% 이상 빨라짐': () => isFaster,
  });

  console.log(
    `[ISS Lazy] grid=${coord.name} | 1st=${res1.timings.duration.toFixed(1)}ms → 2nd=${res2.timings.duration.toFixed(1)}ms | speedup=${(res1.timings.duration / res2.timings.duration).toFixed(1)}x`
  );
}

// 시나리오 2: 캐시 히트 부하 테스트
export function cacheHitTest() {
  // lazy loading 테스트에서 워밍업된 도시 좌표 사용
  const city = ISS_HIT_CITIES[Math.floor(Math.random() * ISS_HIT_CITIES.length)];

  // 먼저 한번 요청해서 캐시 적재 보장 (이미 적재되어 있으면 무시됨)
  const res = http.get(
    `${BASE_URL}/api/weather/iss?latitude=${city.lat}&longitude=${city.lon}`,
    { tags: { scenario: 'cache_hit' } }
  );

  hitDuration.add(res.timings.duration);

  check(res, {
    '[히트] status 200': (r) => r.status === 200,
    '[히트] has messageKey': (r) => {
      try { return JSON.parse(r.body).messageKey !== undefined; }
      catch { return false; }
    },
  });
}
