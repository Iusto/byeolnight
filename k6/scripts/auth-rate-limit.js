import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { BASE_URL, RATE_LIMIT_STAGES } from '../lib/config.js';
import { rateLimitBlockedCount, rateLimitAllowedCount, rateLimitResponseTime, checkRateLimit } from '../lib/helpers.js';

export const options = {
  stages: RATE_LIMIT_STAGES,
  thresholds: {
    http_req_duration: ['p(95)<500'],
    rate_limit_response_time: ['p(95)<50'],
  },
};

const HEADERS = { 'Content-Type': 'application/json' };

// 이메일당 5회/10분 제한
const SEND_LIMIT_PER_EMAIL = 5;

// 시나리오 A: 제한 이내 요청 → 정상 응답 확인
function testWithinLimit() {
  const uniqueEmail = `test-within-${__VU}-${Date.now()}@example.com`;

  for (let i = 0; i < SEND_LIMIT_PER_EMAIL - 1; i++) {
    const res = http.post(
      `${BASE_URL}/api/auth/email/send`,
      JSON.stringify({ email: uniqueEmail }),
      { headers: HEADERS, tags: { scenario: 'within_limit' } }
    );

    checkRateLimit(res, 400);

    check(res, {
      '[제한 이내] status is not rate limited': (r) => r.status !== 400 || !r.body.includes('횟수를 초과'),
    });

    sleep(0.5);
  }
}

// 시나리오 B: 제한 초과 요청 → 400 응답 확인 및 응답 시간 측정
function testExceedLimit() {
  const uniqueEmail = `test-exceed-${__VU}-${Date.now()}@example.com`;

  // 제한까지 요청 소진
  for (let i = 0; i < SEND_LIMIT_PER_EMAIL; i++) {
    http.post(
      `${BASE_URL}/api/auth/email/send`,
      JSON.stringify({ email: uniqueEmail }),
      { headers: HEADERS, tags: { scenario: 'exhaust_limit' } }
    );
    sleep(0.3);
  }

  // 제한 초과 요청
  const res = http.post(
    `${BASE_URL}/api/auth/email/send`,
    JSON.stringify({ email: uniqueEmail }),
    { headers: HEADERS, tags: { scenario: 'exceed_limit' } }
  );

  rateLimitResponseTime.add(res.timings.duration);
  rateLimitBlockedCount.add(1);

  check(res, {
    '[제한 초과] status 400': (r) => r.status === 400,
    '[제한 초과] error message contains 초과': (r) => {
      try { return r.body.includes('초과'); }
      catch { return false; }
    },
    '[제한 초과] response time < 50ms': (r) => r.timings.duration < 50,
  });
}

// 시나리오 C: 다수 VU에서 동시 요청 → Redis 원자성 검증
function testConcurrentAccess() {
  const sharedEmail = `test-concurrent-${__ITER % 5}@example.com`;

  const res = http.post(
    `${BASE_URL}/api/auth/email/send`,
    JSON.stringify({ email: sharedEmail }),
    { headers: HEADERS, tags: { scenario: 'concurrent' } }
  );

  const status = checkRateLimit(res, 400);

  check(res, {
    '[동시 요청] valid response (200 or 400)': (r) => r.status === 200 || r.status === 400,
  });
}

export default function () {
  const scenario = Math.random();

  if (scenario < 0.3) {
    group('인증 Rate Limit - 제한 이내', testWithinLimit);
  } else if (scenario < 0.6) {
    group('인증 Rate Limit - 제한 초과', testExceedLimit);
  } else {
    group('인증 Rate Limit - 동시 요청', testConcurrentAccess);
  }

  sleep(1);
}
