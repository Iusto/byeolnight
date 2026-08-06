import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { BASE_URL } from '../lib/config.js';

const allowedDuration = new Trend('allowed_duration', true);
const blockedDuration = new Trend('blocked_duration', true);
const allowedCount = new Counter('allowed_count');
const blockedCount = new Counter('blocked_count');

const UPLOAD_LIMIT_1H = 10;
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN;

function requestParams(scenario, clientIp) {
  if (!ACCESS_TOKEN) {
    throw new Error('인증이 필요한 테스트입니다. ACCESS_TOKEN 환경 변수를 설정하세요.');
  }
  return {
    headers: {
      Cookie: `accessToken=${ACCESS_TOKEN}`,
      'X-Client-IP': clientIp,
    },
    tags: { scenario },
  };
}

export const options = {
  scenarios: {
    rate_limit_test: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: 'rateLimitTest',
    },
    validation_test: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 3,
      startTime: '1m',
      exec: 'validationTest',
    },
  },
  thresholds: {
    blocked_duration: ['p(95)<50'],
  },
};

// 외부 Vision/S3 비용이 발생하지 않도록 허용되지 않는 확장자의 작은 파일을 사용한다.
export function rateLimitTest() {
  let acceptedBefore429 = 0;

  for (let i = 0; i <= UPLOAD_LIMIT_1H + 2; i++) {
    const body = {
      file: http.file('not-an-image', `rate-limit-${Date.now()}-${i}.txt`, 'text/plain'),
    };
    const res = http.post(
      `${BASE_URL}/api/files/upload`,
      body,
      requestParams('rate_limit', '198.51.100.10')
    );

    if (res.status === 429) {
      blockedDuration.add(res.timings.duration);
      blockedCount.add(1);
      check(res, {
        '[초과] status 429': response => response.status === 429,
        '[초과] 응답 시간 < 50ms': response => response.timings.duration < 50,
      });
      break;
    }

    allowedDuration.add(res.timings.duration);
    allowedCount.add(1);
    acceptedBefore429++;
    check(res, {
      '[허용 후 검증 거부] status 400': response => response.status === 400,
    });
    sleep(0.2);
  }

  console.log(`[File Rate Limit] accepted=${acceptedBefore429}, limit=${UPLOAD_LIMIT_1H}`);
}

export function validationTest() {
  const invalidFiles = [
    { filename: 'test.exe', contentType: 'application/octet-stream' },
    { filename: 'script.sh', contentType: 'text/plain' },
    { filename: 'payload.svg', contentType: 'image/svg+xml' },
  ];
  const file = invalidFiles[__ITER % invalidFiles.length];
  const body = {
    file: http.file('invalid', file.filename, file.contentType),
  };
  const res = http.post(
    `${BASE_URL}/api/files/upload`,
    body,
    requestParams('validation', '198.51.100.20')
  );

  check(res, {
    '[검증] status 400': response => response.status === 400,
  });
}
