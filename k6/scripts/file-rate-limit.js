import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { BASE_URL } from '../lib/config.js';

const allowedDuration = new Trend('allowed_duration', true);
const blockedDuration = new Trend('blocked_duration', true);
const allowedCount = new Counter('allowed_count');
const blockedCount = new Counter('blocked_count');

const PRESIGNED_LIMIT_1H = 20;  // IP당 20회/시간

export const options = {
  scenarios: {
    // 시나리오 1: Rate Limit 도달 과정 + 초과 시 429 확인
    rate_limit_test: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: 'rateLimitTest',
    },
    // 시나리오 2: 비지원 파일 형식 → 400 확인
    validation_test: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 3,
      startTime: '1m',
      exec: 'validationTest',
    },
  },
  thresholds: {
    'blocked_duration': ['p(95)<50'],
  },
};

// 시나리오 1: Presigned URL Rate Limit 도달 테스트
export function rateLimitTest() {
  let allowedBefore429 = 0;

  for (let i = 0; i <= PRESIGNED_LIMIT_1H + 2; i++) {
    const filename = `test-${Date.now()}-${i}.jpg`;

    const res = http.post(
      `${BASE_URL}/api/files/presigned-url?filename=${filename}&contentType=image/jpeg`,
      null,
      { tags: { scenario: 'rate_limit' } }
    );

    if (res.status === 429) {
      blockedDuration.add(res.timings.duration);
      blockedCount.add(1);

      check(res, {
        '[초과] status 429': (r) => r.status === 429,
        '[초과] 에러 메시지': (r) => {
          try { return r.body.includes('한도') || r.body.includes('초과'); }
          catch { return false; }
        },
        '[초과] 응답 시간 < 50ms': (r) => r.timings.duration < 50,
      });

      console.log(`[File Rate Limit] 429 at request #${i + 1} | allowed=${allowedBefore429} | blocked in ${res.timings.duration.toFixed(1)}ms`);
      break;
    }

    allowedDuration.add(res.timings.duration);
    allowedCount.add(1);
    allowedBefore429++;

    check(res, {
      '[허용] status 200': (r) => r.status === 200,
      '[허용] has uploadUrl': (r) => {
        try { return JSON.parse(r.body).data.uploadUrl !== undefined; }
        catch { return false; }
      },
    });

    sleep(0.2);
  }

  console.log(`[File Rate Limit] Total allowed before block: ${allowedBefore429} (limit: ${PRESIGNED_LIMIT_1H})`);
}

// 시나리오 2: 파일 형식 검증
export function validationTest() {
  const invalidFiles = [
    { filename: 'test.exe', contentType: 'application/octet-stream' },
    { filename: 'script.sh', contentType: 'text/plain' },
    { filename: 'hack.php', contentType: 'text/php' },
  ];

  const file = invalidFiles[__ITER % invalidFiles.length];

  const res = http.post(
    `${BASE_URL}/api/files/presigned-url?filename=${file.filename}&contentType=${file.contentType}`,
    null,
    { tags: { scenario: 'validation' } }
  );

  check(res, {
    '[검증] status 400': (r) => r.status === 400,
    '[검증] 에러 메시지': (r) => {
      try { return r.body.includes('지원되지 않는'); }
      catch { return false; }
    },
  });
}
