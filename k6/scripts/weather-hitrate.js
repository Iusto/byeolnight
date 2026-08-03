import http from 'k6/http';
import { check } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { BASE_URL, CACHED_CITIES } from '../lib/config.js';

/**
 * 날씨 캐시 적중률 측정
 *
 * weather-cache.js는 프리캐시된 70개 도시 좌표를 그대로 조회하므로
 * 정의상 miss가 발생할 수 없다(적중률 100%는 시나리오가 만든 상한값).
 *
 * 이 스크립트는 "실제 사용자 좌표 분포"를 모사해 의미 있는 적중률을 측정한다.
 *   - 도시 선택: 인구 규모를 반영한 가중 추출 (상위 도시에 요청이 집중)
 *   - 좌표 생성: 도시 중심점이 아니라 반경 내 임의 지점 (실제 GPS 위치)
 *
 * 프리캐시는 도시당 '한 점'이므로, 도심에서 2km만 벗어나도 다른 0.01 그리드가 된다.
 * 따라서 초반에는 miss가 발생하고 온디맨드 캐싱으로 점차 채워진다 —
 * 이 수렴 과정 자체가 측정 대상이다.
 *
 * 실행:
 *   k6 run -e BASE_URL=http://localhost:8080 k6/scripts/weather-hitrate.js
 *   k6 run -e BASE_URL=... -e RADIUS_KM=10 k6/scripts/weather-hitrate.js
 */

const hitDuration = new Trend('hitrate_duration', true);
const requestCount = new Counter('hitrate_requests');

// 사용자 위치 산포 반경 (km). 클수록 캐시 키가 흩어져 적중률이 낮아진다.
const RADIUS_KM = Number(__ENV.RADIUS_KM || 8);
const DEG_PER_KM = 1 / 111; // 위도 1도 ≈ 111km

export const options = {
  scenarios: {
    realistic_distribution: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 20 },
        { duration: '2m', target: 50 },
        { duration: '30s', target: 0 },
      ],
      exec: 'realisticTest',
    },
  },
  thresholds: {
    'http_req_failed': ['rate<0.01'],
  },
};

/**
 * 인구 가중치 — 실제 사용자 요청은 대도시에 집중된다(Zipf 유사).
 * 앞쪽(서울·수도권)에 높은 가중치를 주어 현실적인 편중을 만든다.
 */
const WEIGHTS = CACHED_CITIES.map((_, i) => 1 / Math.pow(i + 1, 0.7));
const TOTAL_WEIGHT = WEIGHTS.reduce((a, b) => a + b, 0);

function pickCity() {
  let r = Math.random() * TOTAL_WEIGHT;
  for (let i = 0; i < WEIGHTS.length; i++) {
    r -= WEIGHTS[i];
    if (r <= 0) return CACHED_CITIES[i];
  }
  return CACHED_CITIES[0];
}

/** 도시 중심에서 반경 내 임의 지점 (원 내부 균등 분포) */
function jitter(city) {
  const angle = Math.random() * 2 * Math.PI;
  const dist = Math.sqrt(Math.random()) * RADIUS_KM * DEG_PER_KM;
  const latRad = (city.lat * Math.PI) / 180;
  return {
    lat: city.lat + dist * Math.cos(angle),
    lon: city.lon + (dist * Math.sin(angle)) / Math.cos(latRad),
  };
}

/** Actuator 카운터 조회 (없으면 null) */
function readCounter(name) {
  const res = http.get(`${BASE_URL}/actuator/metrics/${name}`);
  if (res.status !== 200) return null;
  try {
    const m = JSON.parse(res.body).measurements.find((x) => x.statistic === 'COUNT');
    return m ? m.value : null;
  } catch {
    return null;
  }
}

export function setup() {
  const hit = readCounter('cache.weather.hit');
  const miss = readCounter('cache.weather.miss');

  if (hit === null || miss === null) {
    console.warn('⚠ Actuator 카운터를 읽을 수 없습니다. 적중률 집계를 건너뜁니다.');
    console.warn('  management.endpoints.web.exposure.include 에 metrics 가 있는지 확인하세요.');
  } else {
    console.log(`[시작] hit=${hit} miss=${miss} · 산포 반경 ${RADIUS_KM}km`);
  }
  return { hit, miss };
}

export function realisticTest() {
  const coord = jitter(pickCity());

  const res = http.get(
    `${BASE_URL}/api/weather/observation` +
      `?latitude=${coord.lat.toFixed(4)}&longitude=${coord.lon.toFixed(4)}`,
    { tags: { scenario: 'realistic' }, timeout: '30s' }
  );

  hitDuration.add(res.timings.duration);
  requestCount.add(1);

  check(res, {
    'status 200': (r) => r.status === 200,
    'has observationQuality': (r) => {
      try {
        return JSON.parse(r.body).observationQuality !== undefined;
      } catch {
        return false;
      }
    },
  });
}

export function teardown(start) {
  if (start.hit === null || start.miss === null) return;

  const hit = readCounter('cache.weather.hit');
  const miss = readCounter('cache.weather.miss');
  if (hit === null || miss === null) return;

  const dHit = hit - start.hit;
  const dMiss = miss - start.miss;
  const total = dHit + dMiss;

  console.log('');
  console.log('════════ 캐시 적중률 (이번 실행분) ════════');
  console.log(`  산포 반경 : ${RADIUS_KM}km`);
  console.log(`  hit       : ${dHit}`);
  console.log(`  miss      : ${dMiss}`);
  console.log(`  총 요청   : ${total}`);
  console.log(
    `  적중률    : ${total > 0 ? ((dHit / total) * 100).toFixed(2) : '0.00'}%`
  );
  console.log('═══════════════════════════════════════════');
  console.log('※ 온디맨드 캐싱으로 진행될수록 miss가 줄어드는 수렴 구조.');
  console.log('  반복 실행 시 캐시가 이미 차 있으므로, 재측정은 서버 재기동 후 수행할 것.');
}
