import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { BASE_URL, CACHED_CITIES, UNCACHED_COORDS, DEFAULT_STAGES } from '../lib/config.js';
import { cacheHitDuration, cacheMissDuration, cacheHitRate, checkCacheHit, randomItem } from '../lib/helpers.js';

export const options = {
  stages: DEFAULT_STAGES,
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
    cache_hit_duration: ['p(95)<100'],
    cache_hit_rate: ['rate>0.9'],
  },
};

// 시나리오 A: 주요 도시 좌표 반복 요청 → 캐시 히트 응답 시간 측정
function testCacheHit() {
  const city = randomItem(CACHED_CITIES);
  const res = http.get(
    `${BASE_URL}/api/weather/observation?latitude=${city.lat}&longitude=${city.lon}`,
    { tags: { scenario: 'cache_hit', city: city.name } }
  );

  checkCacheHit(res, 100);

  check(res, {
    '[캐시 히트] status 200': (r) => r.status === 200,
    '[캐시 히트] has observationQuality': (r) => {
      try { return JSON.parse(r.body).observationQuality !== undefined; }
      catch { return false; }
    },
  });
}

// 시나리오 B: 캐시에 없는 좌표 요청 → 캐시 미스 응답 시간 측정
function testCacheMiss() {
  const coord = randomItem(UNCACHED_COORDS);
  // 좌표에 약간의 랜덤 변동을 줘서 캐시 미스를 유도
  const jitter = () => (Math.random() - 0.5) * 0.1;
  const lat = coord.lat + jitter();
  const lon = coord.lon + jitter();

  const res = http.get(
    `${BASE_URL}/api/weather/observation?latitude=${lat.toFixed(4)}&longitude=${lon.toFixed(4)}`,
    { tags: { scenario: 'cache_miss' }, timeout: '15s' }
  );

  cacheMissDuration.add(res.timings.duration);

  check(res, {
    '[캐시 미스] status 200': (r) => r.status === 200,
  });
}

// 시나리오 C: 히트/미스 혼합 비율 (95:5) 시뮬레이션
export default function () {
  group('날씨 캐시 테스트', () => {
    if (Math.random() < 0.95) {
      testCacheHit();
    } else {
      testCacheMiss();
    }
  });

  sleep(Math.random() * 2 + 0.5);
}
