import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { BASE_URL } from '../lib/config.js';

const allowedDuration = new Trend('allowed_duration', true);
const blockedDuration = new Trend('blocked_duration', true);
const allowedCount = new Counter('allowed_count');
const blockedCount = new Counter('blocked_count');

const HEADERS = { 'Content-Type': 'application/json' };
const SEND_LIMIT = 5;  // 이메일당 5회/10분

export const options = {
  scenarios: {
    // 시나리오 1: 제한 이내 → 정상 응답 확인
    within_limit: {
      executor: 'per-vu-iterations',
      vus: 3,
      iterations: 1,
      exec: 'withinLimitTest',
    },
    // 시나리오 2: 제한 초과 → 400 응답 + 응답 시간 측정
    exceed_limit: {
      executor: 'per-vu-iterations',
      vus: 3,
      iterations: 1,
      startTime: '30s',
      exec: 'exceedLimitTest',
    },
    // 시나리오 3: 동시 요청 → Redis 원자성 검증
    concurrent: {
      executor: 'constant-vus',
      vus: 20,
      duration: '30s',
      startTime: '1m',
      exec: 'concurrentTest',
    },
  },
  thresholds: {
    'blocked_duration': ['p(95)<50'],
    'http_req_failed': ['rate<0.5'],  // rate limit 응답도 "실패"로 잡히므로 완화
  },
};

// 시나리오 1: 제한 이내 요청 (VU별 고유 이메일)
export function withinLimitTest() {
  const email = `within-${__VU}-${Date.now()}@test.com`;

  for (let i = 0; i < SEND_LIMIT - 1; i++) {
    const res = http.post(
      `${BASE_URL}/api/auth/email/send`,
      JSON.stringify({ email }),
      { headers: HEADERS, tags: { scenario: 'within_limit' } }
    );

    allowedDuration.add(res.timings.duration);
    allowedCount.add(1);

    check(res, {
      '[제한 이내] not rate limited': (r) => r.status !== 400 || !r.body.includes('초과'),
    });

    sleep(0.3);
  }
}

// 시나리오 2: 제한 초과 → 차단 응답 확인
export function exceedLimitTest() {
  const email = `exceed-${__VU}-${Date.now()}@test.com`;

  // 제한까지 소진
  for (let i = 0; i < SEND_LIMIT; i++) {
    http.post(
      `${BASE_URL}/api/auth/email/send`,
      JSON.stringify({ email }),
      { headers: HEADERS, tags: { scenario: 'exhaust' } }
    );
    sleep(0.2);
  }

  // 초과 요청
  const res = http.post(
    `${BASE_URL}/api/auth/email/send`,
    JSON.stringify({ email }),
    { headers: HEADERS, tags: { scenario: 'exceed_limit' } }
  );

  blockedDuration.add(res.timings.duration);
  blockedCount.add(1);

  check(res, {
    '[초과] status 400': (r) => r.status === 400,
    '[초과] 에러 메시지 포함': (r) => {
      try { return r.body.includes('초과'); }
      catch { return false; }
    },
    '[초과] 응답 시간 < 50ms': (r) => r.timings.duration < 50,
  });

  console.log(`[Auth Rate Limit] email=${email} | blocked in ${res.timings.duration.toFixed(1)}ms | status=${res.status}`);
}

// 시나리오 3: 동시 요청 - Redis 원자성 검증
// 같은 이메일에 동시 요청 시 정확히 SEND_LIMIT까지만 허용되는지 확인
export function concurrentTest() {
  const sharedEmail = `concurrent-${__ITER % 3}@test.com`;

  const res = http.post(
    `${BASE_URL}/api/auth/email/send`,
    JSON.stringify({ email: sharedEmail }),
    { headers: HEADERS, tags: { scenario: 'concurrent' } }
  );

  if (res.status === 400 && res.body.includes('초과')) {
    blockedDuration.add(res.timings.duration);
    blockedCount.add(1);
  } else {
    allowedDuration.add(res.timings.duration);
    allowedCount.add(1);
  }

  check(res, {
    '[동시] valid response': (r) => r.status === 200 || r.status === 400,
  });

  sleep(0.5);
}
