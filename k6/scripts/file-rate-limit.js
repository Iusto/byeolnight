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

// IP당 20회/시간 제한
const PRESIGNED_URL_LIMIT_1H = 20;

// 시나리오 A: Presigned URL 요청 Rate Limit 도달 시 429 확인
function testPresignedUrlRateLimit() {
  const filename = `test-${__VU}-${Date.now()}.jpg`;

  // 제한까지 요청 소진
  for (let i = 0; i < PRESIGNED_URL_LIMIT_1H; i++) {
    const res = http.post(
      `${BASE_URL}/api/files/presigned-url?filename=${filename}&contentType=image/jpeg`,
      null,
      { tags: { scenario: 'exhaust_presigned' } }
    );

    rateLimitResponseTime.add(res.timings.duration);

    if (res.status === 429) {
      rateLimitBlockedCount.add(1);
      check(res, {
        '[Presigned URL] 429 before limit': () => false,
      });
      return;
    }

    rateLimitAllowedCount.add(1);
    sleep(0.2);
  }

  // 제한 초과 요청
  const blockedRes = http.post(
    `${BASE_URL}/api/files/presigned-url?filename=${filename}&contentType=image/jpeg`,
    null,
    { tags: { scenario: 'presigned_blocked' } }
  );

  rateLimitResponseTime.add(blockedRes.timings.duration);
  rateLimitBlockedCount.add(1);

  check(blockedRes, {
    '[Presigned URL 초과] status 429': (r) => r.status === 429,
    '[Presigned URL 초과] error message': (r) => {
      try { return r.body.includes('한도를 초과'); }
      catch { return false; }
    },
    '[Presigned URL 초과] response time < 50ms': (r) => r.timings.duration < 50,
  });
}

// 시나리오 B: 정상 요청으로 응답 형식 확인
function testPresignedUrlSuccess() {
  const filename = `success-${__VU}-${Date.now()}.png`;

  const res = http.post(
    `${BASE_URL}/api/files/presigned-url?filename=${filename}&contentType=image/png`,
    null,
    { tags: { scenario: 'presigned_success' } }
  );

  checkRateLimit(res, 429);

  check(res, {
    '[정상 요청] status 200': (r) => r.status === 200,
    '[정상 요청] has uploadUrl': (r) => {
      try { return JSON.parse(r.body).data.uploadUrl !== undefined; }
      catch { return false; }
    },
    '[정상 요청] has s3Key': (r) => {
      try { return JSON.parse(r.body).data.s3Key !== undefined; }
      catch { return false; }
    },
  });
}

// 시나리오 C: 지원되지 않는 파일 형식 → 400 확인
function testUnsupportedFormat() {
  const res = http.post(
    `${BASE_URL}/api/files/presigned-url?filename=test.exe&contentType=application/octet-stream`,
    null,
    { tags: { scenario: 'unsupported_format' } }
  );

  check(res, {
    '[비지원 형식] status 400': (r) => r.status === 400,
    '[비지원 형식] error message': (r) => {
      try { return r.body.includes('지원되지 않는'); }
      catch { return false; }
    },
  });
}

export default function () {
  const scenario = Math.random();

  if (scenario < 0.4) {
    group('파일 업로드 Rate Limit - Presigned URL 제한', testPresignedUrlRateLimit);
  } else if (scenario < 0.8) {
    group('파일 업로드 Rate Limit - 정상 요청', testPresignedUrlSuccess);
  } else {
    group('파일 업로드 Rate Limit - 비지원 형식', testUnsupportedFormat);
  }

  sleep(1);
}
