import axios from 'axios';

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'https://byeolnight.com/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // 쿠키 포함 (Refresh Token용)
});

console.log('Axios baseURL:', import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api');

// 클라이언트 IP 추출 함수
const getClientIp = async (): Promise<string> => {
  try {
    // 외부 IP 조회 서비스 사용
    const response = await fetch('https://api.ipify.org?format=json');
    const data = await response.json();
    return data.ip || 'unknown';
  } catch (error) {
    console.warn('IP 조회 실패:', error);
    return 'unknown';
  }
};

// 요청 인터셉터
instance.interceptors.request.use(
  async (config) => {
    const token = localStorage.getItem('accessToken');

    // 공개 API 및 인증 API 확인
    const isPublicEndpoint = config.url?.startsWith('/public/');
    const isAuthEndpoint = config.url?.includes('/auth/login') || 
                          config.url?.includes('/auth/signup') ||
                          config.url?.includes('/auth/token/refresh');

    // 인증이 필요한 경우만 Authorization 헤더 설정
    if (token && !isAuthEndpoint && !isPublicEndpoint) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // 클라이언트 IP를 헤더에 추가 (캐시된 IP 사용)
    const cachedIp = sessionStorage.getItem('clientIp');
    if (cachedIp) {
      config.headers['X-Client-IP'] = cachedIp;
    } else {
      // IP가 캐시되지 않은 경우 비동기로 조회
      try {
        const clientIp = await getClientIp();
        sessionStorage.setItem('clientIp', clientIp);
        config.headers['X-Client-IP'] = clientIp;
      } catch (error) {
        config.headers['X-Client-IP'] = 'unknown';
      }
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// 토큰 갱신 중복 방지를 위한 변수
let isRefreshing = false;
let failedQueue: Array<{ resolve: Function; reject: Function }> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token);
    }
  });
  
  failedQueue = [];
};

// 응답 인터셉터 (토큰 갱신 로직 포함)
instance.interceptors.response.use(
  (response) => {
    // CommonResponse 구조 그대로 반환
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // 로그인 API는 토큰 갱신 로직에서 제외
    const isLoginRequest = originalRequest.url?.includes('/auth/login');
    
    // 401 에러이고 토큰 갱신을 시도하지 않은 경우 (로그인 요청 제외)
    if (error.response?.status === 401 && !originalRequest._retry && !isLoginRequest) {
      if (isRefreshing) {
        // 이미 토큰 갱신 중이면 대기열에 추가
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return instance(originalRequest);
        }).catch(err => {
          return Promise.reject(err);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // 로그인 유지 옵션이 있는 경우만 토큰 갱신 시도
        const rememberMe = localStorage.getItem('rememberMe');
        if (rememberMe !== 'true') {
          throw new Error('로그인 유지 옵션이 비활성화됨');
        }

        // Refresh Token으로 새 Access Token 요청
        const refreshResponse = await axios.post(
          `${import.meta.env.VITE_API_BASE_URL || 'https://byeolnight.com/api'}/auth/token/refresh`,
          {},
          { withCredentials: true }
        );

        const newToken = refreshResponse.data.data?.accessToken;
        if (newToken) {
          localStorage.setItem('accessToken', newToken);
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          processQueue(null, newToken);
          console.log('토큰 자동 갱신 성공');
          return instance(originalRequest);
        } else {
          throw new Error('새 토큰을 받지 못했습니다.');
        }
      } catch (refreshError) {
        // Refresh Token도 만료된 경우 로그아웃 처리
        console.warn('토큰 갱신 실패:', refreshError);
        processQueue(refreshError, null);
        localStorage.removeItem('accessToken');
        localStorage.removeItem('rememberMe');
        
        // 현재 페이지가 로그인 페이지가 아니면 리다이렉트
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

// IP 초기화 함수 (앱 시작 시 호출)
export const initializeClientIp = async () => {
  if (!sessionStorage.getItem('clientIp')) {
    try {
      const clientIp = await getClientIp();
      sessionStorage.setItem('clientIp', clientIp);
      console.log('클라이언트 IP 초기화:', clientIp);
    } catch (error) {
      console.warn('IP 초기화 실패:', error);
    }
  }
};

export default instance;
