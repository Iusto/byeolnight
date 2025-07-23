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

// 요청 인터셉터
instance.interceptors.request.use(
  async (config) => {
    // 공개 API 및 인증 API 확인
    const isPublicEndpoint = config.url?.startsWith('/public/');
    const isAuthEndpoint = config.url?.includes('/auth/login') || 
                          config.url?.includes('/auth/signup') ||
                          config.url?.includes('/auth/token/refresh');
    
    // 토큰을 헤더에 추가 (쿠키와 이중으로 전송)
    const accessToken = localStorage.getItem('accessToken');
    if (accessToken && !isPublicEndpoint && !isAuthEndpoint) {
      config.headers['Authorization'] = `Bearer ${accessToken}`;
      console.log(`요청 헤더에 토큰 추가: ${config.url} - ${accessToken.substring(0, 10)}...`);
    } else if (!isPublicEndpoint && !isAuthEndpoint) {
      console.warn(`토큰 없음 - 헤더 추가 안함: ${config.url}`);
    }

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

// 응답 인터셉터 (토큰 갱신 로직 포함)
instance.interceptors.response.use(
  (response) => {
    // CommonResponse 구조 그대로 반환
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // 401 에러이고 토큰 갱신을 시도하지 않은 경우
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 이미 토큰 갱신 중이면 대기열에 추가
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
        // 로그인 유지 옵션이 있는 경우만 토큰 갱신 시도
        const rememberMe = localStorage.getItem('rememberMe');
        if (rememberMe !== 'true') {
          throw new Error('로그인 유지 옵션이 비활성화됨');
        }

        console.log('토큰 갱신 시도');
        
        // Refresh Token으로 새 Access Token 요청
        const refreshResponse = await axios.post(
          `${import.meta.env.VITE_API_BASE_URL || 'https://byeolnight.com/api'}/auth/token/refresh`,
          {},
          { withCredentials: true }
        );

        if (refreshResponse.data.success) {
          // 응답 본문에서 토큰 가져와서 저장
          const newAccessToken = refreshResponse.data?.data?.accessToken;
          if (newAccessToken) {
            localStorage.setItem('accessToken', newAccessToken);
            console.log('토큰 갱신 성공:', newAccessToken.substring(0, 10) + '...');
            
            // 원래 요청에 새 토큰 적용
            originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
          } else {
            console.warn('갱신된 토큰이 응답 본문에 없습니다.');
          }
          
          processQueue(null);
          return instance(originalRequest);
        } else {
          throw new Error('새 토큰을 받지 못했습니다.');
        }
      } catch (refreshError) {
        // Refresh Token도 만료된 경우 로그아웃 처리
        console.warn('토큰 갱신 실패:', refreshError);
        processQueue(refreshError);
        localStorage.removeItem('rememberMe');
        localStorage.removeItem('accessToken');
        
        // 현재 페이지가 로그인 페이지가 아니면 리다이렉트
        if (window.location.pathname !== '/login') {
          console.log('인증 실패, 로그인 페이지로 이동');
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

export default instance;