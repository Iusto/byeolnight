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
      
      // 모바일 환경에서 타임아웃 증가
      const isMobile = /Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
      if (isMobile) {
        console.log('모바일 환경 감지 - 타임아웃 증가');
        headers['X-Mobile-Upload'] = 'true';
      }
      
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

// 클라이언트 IP 추출 함수 (모바일 호환성 개선)
const getClientIp = async (): Promise<string> => {
  try {
    // 타임아웃 설정으로 모바일 네트워크 대응
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3000); // 3초 타임아웃
    
    const response = await fetch('https://api.ipify.org?format=json', {
      signal: controller.signal,
      headers: {
        'Accept': 'application/json'
      }
    });
    
    clearTimeout(timeoutId);
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    
    const data = await response.json();
    return data.ip || 'unknown';
  } catch (error) {
    console.warn('IP 조회 실패 (모바일 네트워크 이슈 가능):', error);
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
    
    // 로그인 요청인 경우 데이터 형식 확인 및 로깅
    if (config.url?.includes('/auth/login') && config.data) {
      console.log('로그인 요청 인터셉터:', {
        contentType: config.headers['Content-Type'],
        dataType: typeof config.data,
        isArray: Array.isArray(config.data),
        userAgent: navigator.userAgent,
        isInApp: /KAKAOTALK|Instagram|NAVER|Snapchat|Line/i.test(navigator.userAgent)
      });
      
      // 인앱브라우저 호환성 개선
      if (typeof config.data === 'string') {
        try {
          // 문자열로 전송된 경우 파싱 시도
          const parsedData = JSON.parse(config.data);
          if (parsedData && typeof parsedData === 'object') {
            config.data = parsedData;
          }
        } catch (e) {
          console.warn('로그인 데이터 파싱 실패:', e);
        }
      }
      
      // 배열 형태로 전송되는 문제 방지
      if (Array.isArray(config.data)) {
        console.warn('로그인 데이터가 배열 형태로 전송되었습니다. 객체로 변환합니다.');
        const email = config.data[0]?.email || '';
        const password = config.data[0]?.password || '';
        config.data = { email, password };
      }
    }

    // 인증이 필요한 경우만 Authorization 헤더 설정
    if (token && !isAuthEndpoint && !isPublicEndpoint) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // 클라이언트 IP를 헤더에 추가 (모바일 호환성 개선)
    try {
      const cachedIp = sessionStorage.getItem('clientIp');
      if (cachedIp) {
        config.headers['X-Client-IP'] = cachedIp;
      } else {
        // 모바일 호환성을 위해 IP 조회 비활성화 (임시)
        const isMobile = /Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
        config.headers['X-Client-IP'] = isMobile ? 'mobile-device' : 'desktop-unknown';
        
        // PC에서만 IP 조회 시도
        if (!isMobile) {
          getClientIp().then(ip => {
            if (ip !== 'unknown') {
              sessionStorage.setItem('clientIp', ip);
            }
          }).catch(() => {
            // PC에서도 IP 조회 실패 시 무시
          });
        }
      }
    } catch (error) {
      // sessionStorage 접근 실패 시에도 요청 진행
      config.headers['X-Client-IP'] = 'storage-error';
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

        // 인앱브라우저 호환성 개선
        const isInApp = /KAKAOTALK|Instagram|NAVER|Snapchat|Line/i.test(navigator.userAgent);
        console.log('토큰 갱신 시도 - 인앱브라우저 감지:', isInApp);
        
        // Refresh Token으로 새 Access Token 요청
        const refreshResponse = await axios.post(
          `${import.meta.env.VITE_API_BASE_URL || 'https://byeolnight.com/api'}/auth/token/refresh`,
          {},
          { 
            withCredentials: true,
            // 인앱브라우저에서 타임아웃 증가
            timeout: isInApp ? 10000 : 5000 
          }
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
