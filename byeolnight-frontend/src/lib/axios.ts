import axios from 'axios';

// API 기본 URL 설정
// 개발: .env.local 파일 사용
// 배포: 상대 경로 사용 (같은 도메인)
const API_BASE_URL = import.meta.env.DEV 
  ? (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080')
  : '/api';
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

// 로그인 유지 옵션 확인 (안전한 Storage 접근)
const getRememberMeOption = (): boolean => {
  try {
    return localStorage.getItem('rememberMe') === 'true' || 
           sessionStorage.getItem('rememberMe') === 'true';
  } catch {
    return true; // 인앱브라우저 대응
  }
};

// 대기열 처리
const processQueue = (error: any) => {
  failedQueue.forEach(({ resolve, reject }) => {
    error ? reject(error) : resolve(null);
  });
  failedQueue = [];
};

// Storage 정리
const clearAuthStorage = () => {
  try {
    localStorage.removeItem('rememberMe');
    sessionStorage.removeItem('rememberMe');
  } catch {
    // 인앱브라우저에서 Storage 접근 실패 시 무시
  }
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
    const originalRequest = error.config;

    // 401 에러가 아니거나 이미 재시도한 경우
    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }

    // 로그인 유지 옵션이 비활성화된 경우
    if (!getRememberMeOption()) {
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
      clearAuthStorage();
      
      // 로그인 페이지가 아닌 경우 리다이렉트
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
      
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }
  }
);

export default instance;