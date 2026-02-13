export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// ============================================================
// 날씨 캐시 테스트용 좌표
// ============================================================

// 캐시 히트 대상 (70개 프리캐시 도시 중 대표 7개)
export const CACHED_CITIES = [
  { name: '서울',   lat: 37.5665, lon: 126.9780 },
  { name: '부산',   lat: 35.1796, lon: 129.0756 },
  { name: '대구',   lat: 35.8714, lon: 128.6014 },
  { name: '인천',   lat: 37.4563, lon: 126.7052 },
  { name: '대전',   lat: 36.3504, lon: 127.3845 },
  { name: '광주',   lat: 35.1595, lon: 126.8526 },
  { name: '제주',   lat: 33.4996, lon: 126.5312 },
];

// 캐시 미스 확정 좌표 (70개 도시의 0.01 그리드와 겹치지 않는 좌표)
// 서해/동해/남해 해상 및 산간 지역
export const UNCACHED_COORDS = [
  { name: '서해 해상',     lat: 35.50, lon: 125.00 },
  { name: '동해 해상',     lat: 36.55, lon: 130.50 },
  { name: '남해 해상',     lat: 33.00, lon: 127.50 },
  { name: '강원 산간',     lat: 38.50, lon: 128.00 },
  { name: '내륙 산간',     lat: 34.30, lon: 125.50 },
];

// ============================================================
// ISS 캐시 테스트용 좌표
// ============================================================

// ISS는 프리캐시 없음 (순수 lazy cache, 1도 그리드)
// 캐시 미스 보장 좌표 (Math.round 기준 고유 그리드)
export const ISS_UNIQUE_GRIDS = [
  { name: 'grid_30_124', lat: 30.3, lon: 124.3 },  // iss:30:124
  { name: 'grid_31_125', lat: 31.3, lon: 125.3 },  // iss:31:125
  { name: 'grid_32_130', lat: 32.3, lon: 130.3 },  // iss:32:130
  { name: 'grid_29_123', lat: 29.3, lon: 123.3 },  // iss:29:123
  { name: 'grid_28_131', lat: 28.3, lon: 131.3 },  // iss:28:131
  { name: 'grid_27_122', lat: 27.3, lon: 122.3 },  // iss:27:122
  { name: 'grid_26_132', lat: 26.3, lon: 132.3 },  // iss:26:132
  { name: 'grid_25_121', lat: 25.3, lon: 121.3 },  // iss:25:121
  { name: 'grid_24_133', lat: 24.3, lon: 133.3 },  // iss:24:133
  { name: 'grid_23_120', lat: 23.3, lon: 120.3 },  // iss:23:120
];

// ISS 캐시 히트 테스트용 (주요 도시 - 한번 요청되면 2시간 캐시)
export const ISS_HIT_CITIES = CACHED_CITIES;

// ============================================================
// 부하 프로파일
// ============================================================

export const DEFAULT_STAGES = [
  { duration: '30s', target: 10 },
  { duration: '1m',  target: 50 },
  { duration: '2m',  target: 50 },
  { duration: '1m',  target: 100 },
  { duration: '30s', target: 0 },
];

export const RATE_LIMIT_STAGES = [
  { duration: '10s', target: 5 },
  { duration: '30s', target: 10 },
  { duration: '1m',  target: 20 },
  { duration: '10s', target: 0 },
];
