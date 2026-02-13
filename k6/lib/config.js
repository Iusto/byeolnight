export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 캐시 히트 대상 (주요 도시 - WeatherLocalCacheService에 프리캐시됨)
export const CACHED_CITIES = [
  { name: '서울',   lat: 37.5665, lon: 126.9780 },
  { name: '부산',   lat: 35.1796, lon: 129.0756 },
  { name: '대구',   lat: 35.8714, lon: 128.6014 },
  { name: '인천',   lat: 37.4563, lon: 126.7052 },
  { name: '대전',   lat: 36.3504, lon: 127.3845 },
  { name: '광주',   lat: 35.1595, lon: 126.8526 },
  { name: '제주',   lat: 33.4996, lon: 126.5312 },
];

// 캐시 미스 대상 (비주요 좌표)
export const UNCACHED_COORDS = [
  { lat: 36.1234, lon: 127.5678 },
  { lat: 34.5678, lon: 128.1234 },
];

// 기본 부하 프로파일
export const DEFAULT_STAGES = [
  { duration: '30s', target: 10 },   // Warm-up
  { duration: '1m',  target: 50 },   // Ramp-up
  { duration: '2m',  target: 50 },   // Steady state
  { duration: '1m',  target: 100 },  // Peak load
  { duration: '30s', target: 0 },    // Cool-down
];

// Rate Limit 테스트용 경량 프로파일
export const RATE_LIMIT_STAGES = [
  { duration: '10s', target: 5 },
  { duration: '30s', target: 10 },
  { duration: '1m',  target: 20 },
  { duration: '10s', target: 0 },
];
