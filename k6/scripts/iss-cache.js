import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { BASE_URL, CACHED_CITIES, DEFAULT_STAGES } from '../lib/config.js';
import { cacheHitDuration, cacheMissDuration, cacheHitRate, randomItem } from '../lib/helpers.js';

export const options = {
  stages: DEFAULT_STAGES,
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
    cache_hit_duration: ['p(95)<100'],
    cache_hit_rate: ['rate>0.8'],
  },
};

// ISS 캐시 키는 1도 그리드 (iss:LAT:LON) → 같은 그리드 내 좌표는 캐시 히트
const ISS_PRECACHED_CITIES = CACHED_CITIES.slice(0, 10);

// 캐시 미스 좌표 (프리캐시되지 않은 1도 그리드)
const ISS_UNCACHED_COORDS = [
  { lat: 33.12, lon: 125.45 },
  { lat: 38.90, lon: 130.12 },
  { lat: 34.22, lon: 131.55 },
];

// 시나리오 A: 프리캐시된 도시 좌표 반복 요청
function testPreCachedCity() {
  const city = randomItem(ISS_PRECACHED_CITIES);
  const res = http.get(
    `${BASE_URL}/api/weather/iss?latitude=${city.lat}&longitude=${city.lon}`,
    { tags: { scenario: 'iss_cache_hit', city: city.name } }
  );

  const duration = res.timings.duration;
  const isHit = duration < 200;
  cacheHitRate.add(isHit);

  if (isHit) {
    cacheHitDuration.add(duration);
  } else {
    cacheMissDuration.add(duration);
  }

  check(res, {
    '[ISS 캐시 히트] status 200': (r) => r.status === 200,
    '[ISS 캐시 히트] has messageKey': (r) => {
      try { return JSON.parse(r.body).messageKey !== undefined; }
      catch { return false; }
    },
  });
}

// 시나리오 B: 캐시 미등록 좌표 요청 후 재요청 (lazy loading 검증)
function testLazyLoading() {
  const coord = ISS_UNCACHED_COORDS[Math.floor(Math.random() * ISS_UNCACHED_COORDS.length)];

  // 첫 번째 요청: 캐시 미스 (lazy loading 트리거)
  const res1 = http.get(
    `${BASE_URL}/api/weather/iss?latitude=${coord.lat}&longitude=${coord.lon}`,
    { tags: { scenario: 'iss_lazy_first' }, timeout: '15s' }
  );

  cacheMissDuration.add(res1.timings.duration);

  check(res1, {
    '[Lazy 1st] status 200': (r) => r.status === 200,
  });

  sleep(1);

  // 두 번째 요청: 캐시 히트 (lazy loaded 데이터)
  const res2 = http.get(
    `${BASE_URL}/api/weather/iss?latitude=${coord.lat}&longitude=${coord.lon}`,
    { tags: { scenario: 'iss_lazy_second' } }
  );

  const isHit = res2.timings.duration < 200;
  cacheHitRate.add(isHit);
  if (isHit) {
    cacheHitDuration.add(res2.timings.duration);
  }

  check(res2, {
    '[Lazy 2nd] status 200': (r) => r.status === 200,
    '[Lazy 2nd] faster than 1st': () => res2.timings.duration < res1.timings.duration,
  });
}

export default function () {
  group('ISS 캐시 테스트', () => {
    if (Math.random() < 0.8) {
      testPreCachedCity();
    } else {
      testLazyLoading();
    }
  });

  sleep(Math.random() * 2 + 0.5);
}
