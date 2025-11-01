import axios from 'axios';

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
const processQueue = (error: any) => {
  failedQueue.forEach(({ resolve, reject }) => {
    error ? reject(error) : resolve(null);
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
    // 네트워크 연결 실패 (서버 다운)
    if (!error.response && error.code === 'ERR_NETWORK') {
      window.location.href = '/maintenance.html';
      return Promise.reject(error);
    }
    
    // 서버 점검 중 (403, 502, 503, 504)
    // 403: ALB가 백엔드 다운 시 반환
    if (error.response?.status === 403 || (error.response?.status >= 502 && error.response?.status <= 504)) {
      window.location.href = '/maintenance.html';
      return Promise.reject(error);
    }

    const originalRequest = error.config;

    // 공개 API들은 토큰 재발급 시도하지 않음
    const publicApis = ['/auth/withdraw', '/auth/password/', '/auth/oauth/recover'];
    // 공개 건의사항 API는 비로그인 허용
    const isPublicSuggestionApi = originalRequest.url?.startsWith('/public/suggestions');
    
    if (publicApis.some(api => originalRequest.url?.includes(api)) || isPublicSuggestionApi) {
      return Promise.reject(error);
    }

    // 401 에러가 아니거나 이미 재시도한 경우
    if (error.response?.status !== 401 || originalRequest._retry) {
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
      
      // 회원 전용 API에서만 알림 후 로그인 페이지로 이동
      const currentPath = window.location.pathname;
      const isAuthRequiredApi = originalRequest.url?.startsWith('/member/');
      
      if (currentPath.includes('/suggestions') && isAuthRequiredApi) {
        // 중복 알림 방지를 위해 세션스토리지 체크
        const alertShown = sessionStorage.getItem('auth-alert-shown');
        if (!alertShown) {
          sessionStorage.setItem('auth-alert-shown', 'true');
          alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
        }
        window.location.href = '/login';
        return Promise.reject(error);
      }
      
      // 공개 페이지에서는 리다이렉트하지 않음
      const publicPaths = ['/', '/posts', '/login', '/signup', '/reset-password', '/oauth-recover', '/suggestions'];
      const isPublicPath = publicPaths.some(path => 
        currentPath === path || currentPath.startsWith('/posts/')
      );
      
      // 비공개 페이지에서만 로그인 페이지로 리다이렉트
      if (!isPublicPath && currentPath !== '/login') {
        window.location.href = '/login';
      }
      
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }
  }
);

export default instance;