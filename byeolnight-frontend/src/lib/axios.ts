import axios from 'axios';
import { checkServerHealth, redirectToMaintenance } from '../utils/healthCheck';

// API 기본 URL 설정
// Docker 빌드 시 설정된 환경변수 사용
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 
  (window.location.hostname === 'localhost' ? 'http://localhost:8080' : '/api');
const REQUEST_TIMEOUT = 30000;

// Axios 인스턴스 생성
const instance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
  timeout: REQUEST_TIMEOUT,
});

// 토큰 갱신 상태 관리
let isRefreshing = false;
let failedQueue: Array<{ resolve: Function; reject: Function }> = [];



// 대기열 처리
const processQueue = (error: unknown) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(null);
    }
  });
  failedQueue = [];
};

// 토큰 갱신 시도
const refreshToken = async () => {
  const response = await axios.post(`${API_BASE_URL}/auth/token/refresh`, {}, {
    withCredentials: true
  });
  
  if (!response.data.success) {
    throw new Error('토큰 갱신 실패');
  }
  
  return response;
};

// 요청 인터셉터
instance.interceptors.request.use(
  (config) => {
    // FormData 처리
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type'];
    }
    
    // HttpOnly 쿠키는 브라우저가 자동으로 전송 (withCredentials: true)
    return config;
  },
  (error) => Promise.reject(error)
);

// 응답 인터셉터
instance.interceptors.response.use(
  (response) => response,
  async (error) => {
    // 네트워크 연결 실패 또는 서버 점검 중 (502, 503, 504)
    const isNetworkError = (!error.response && error.code === 'ERR_NETWORK') || 
                           (error.response?.status >= 502 && error.response?.status <= 504);
    
    if (isNetworkError) {
      // 사이트 이용 중 서버 다운 감지
      const isHealthy = await checkServerHealth();
      
      if (!isHealthy) {
        redirectToMaintenance();
        return Promise.reject(new Error('서버 점검 중입니다'));
      }
    }

    const originalRequest = error.config;

    // 401 에러가 아니거나 이미 재시도한 경우
    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }

    // 공개 API들은 토큰 재발급 시도하지 않음 (백엔드에서 이미 permitAll 처리됨)
    const publicApis = ['/auth/me', '/auth/login', '/auth/signup', '/auth/password/', '/auth/oauth/', '/public/'];
    if (publicApis.some(api => originalRequest.url?.includes(api))) {
      return Promise.reject(error);
    }

    // 토큰 갱신 중인 경우 대기열에 추가
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then(() => instance(originalRequest))
        .catch(err => Promise.reject(err));
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      await refreshToken();
      // HttpOnly 쿠키로 새 토큰이 자동 저장됨
      processQueue(null);
      return instance(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError);
      // 토큰 갱신 실패 시 에러만 반환 (각 페이지/컴포넌트에서 처리)
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }
  }
);

export default instance;