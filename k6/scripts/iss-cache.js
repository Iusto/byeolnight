import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, ISS_HIT_CITIES } from '../lib/config.js';

// 응답 시간 메트릭
const hitDuration = new Trend('hit_duration', true);

export const options = {
  scenarios: {
    // ISS 부하 테스트
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
  },
  thresholds: {
    'hit_duration': ['p(95)<50'],
    'http_req_failed': ['rate<0.01'],
  },
};

// ISS 부하 테스트 - 주요 도시 좌표로 요청
export function cacheHitTest() {
  const city = ISS_HIT_CITIES[Math.floor(Math.random() * ISS_HIT_CITIES.length)];

  const res = http.get(
    `${BASE_URL}/api/weather/iss?latitude=${city.lat}&longitude=${city.lon}`,
    { tags: { scenario: 'cache_hit' } }
  );

  hitDuration.add(res.timings.duration);

  check(res, {
    '[ISS] status 200': (r) => r.status === 200,
    '[ISS] has messageKey': (r) => {
      try { return JSON.parse(r.body).messageKey !== undefined; }
      catch { return false; }
    },
  });
}
