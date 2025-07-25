import axios from 'axios';

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'https://byeolnight.com/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // 쿠키 포함 (Refresh Token용)
  timeout: 30000, // 기본 타임아웃 30초로 설정 (모바일 환경 대응)
});

// FormData를 사용할 때 Content-Type 헤더를 자동으로 설정하도록 함
instance.defaults.transformRequest = [
  function(data, headers) {
    // FormData인 경우 Content-Type 헤더 삭제 (브라우저가 자동으로 설정)
    if (data instanceof FormData) {
      console.log('파일 업로드 요청 감지 - FormData 사용');
      delete headers['Content-Type'];
      return data;
    }
    // 이미 문자열이면 그대로 반환
    if (typeof data === 'string') return data;
    // 객체면 JSON.stringify로 변환
    if (typeof data === 'object' && data !== null) {
      return JSON.stringify(data);
    }
    return data;
  }
];

console.log('Axios baseURL:', import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api');

// 클라이언트 IP 추출 함수
const getClientIp = async (): Promise<string> => {
  try {
    // 안전한 방법으로 IP 가져오기
    return 'client-ip';
  } catch (error) {
    console.warn('IP 조회 실패:', error);
    return 'unknown';
  }
};

// 요청 인터셉터 (인앱 브라우저 호환)
instance.interceptors.request.use(
  async (config) => {
    // 인앱 브라우저에서는 쿠키에서 토큰을 직접 읽어서 헤더에 추가
    const userAgent = navigator.userAgent;
    const isInApp = /kakaotalk|naver|inapp|fbav|instagram|line/i.test(userAgent);
    
    if (isInApp) {
      const getCookie = (name: string) => {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) {
          return parts.pop()?.split(';').shift() || null;
        }
        return null;
      };
      
      const accessToken = getCookie('accessToken');
      if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`;
        console.log(`인앱 브라우저 - 쿠키에서 토큰 추가: ${config.url}`);
      }
    }
    
    console.log(`API 요청: ${config.url} (쿠키 기반 인증)`);
    return config;
  },
  (error) => Promise.reject(error)
);

// 토큰 갱신 중복 방지를 위한 변수
let isRefreshing = false;
let failedQueue: Array<{ resolve: Function; reject: Function }> = [];

const processQueue = (error: any) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(null);
    }
  });
  
  failedQueue = [];
};

// 응답 인터셉터 (인앱 브라우저 호환)
instance.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // 401 에러이고 토큰 갱신을 시도하지 않은 경우
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => {
          return instance(originalRequest);
        }).catch(err => {
          return Promise.reject(err);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // 로그인 유지 옵션 확인 (인앱 브라우저 호환)
        const getSafeRememberMe = (): boolean => {
          try {
            const localStorage_value = localStorage.getItem('rememberMe');
            const sessionStorage_value = sessionStorage.getItem('rememberMe');
            return localStorage_value === 'true' || sessionStorage_value === 'true';
          } catch (storageError) {
            console.warn('Storage 접근 실패 (인앱브라우저):', storageError);
            // 인앱브라우저에서는 기본적으로 로그인 유지 활성화
            return true;
          }
        };
        
        const rememberMe = getSafeRememberMe();
        
        if (!rememberMe) {
          throw new Error('로그인 유지 옵션이 비활성화됨');
        }

        console.log('토큰 갱신 시도 (쿠키 기반)');
        
        // Refresh Token으로 새 Access Token 요청 (쿠키 기반)
        const refreshResponse = await axios.post(
          `${import.meta.env.VITE_API_BASE_URL || 'https://byeolnight.com/api'}/auth/token/refresh`,
          {},
          { withCredentials: true }
        );

        if (refreshResponse.data.success) {
          console.log('토큰 갱신 성공 (쿠키로 자동 저장됨)');
          processQueue(null);
          return instance(originalRequest);
        } else {
          throw new Error('새 토큰을 받지 못했습니다.');
        }
      } catch (refreshError) {
        console.warn('토큰 갱신 실패:', refreshError);
        processQueue(refreshError);
        
        // Storage 안전하게 정리
        try {
          localStorage.removeItem('rememberMe');
          sessionStorage.removeItem('rememberMe');
        } catch (storageError) {
          console.warn('Storage 정리 실패 (인앱브라우저):', storageError);
        }
        
        // 로그인 유지 옵션이 있었는데 토큰 갱신에 실패한 경우만 리다이렉트
        if (rememberMe && window.location.pathname !== '/login') {
          console.log('토큰 갱신 실패, 로그인 페이지로 이동');
          window.location.href = '/login';
        }
        
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // 401 에러이지만 로그인 유지 옵션이 없는 경우 그냥 에러 반환
    console.log('401 에러이지만 로그인 유지 옵션 없음 - 에러 반환');
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