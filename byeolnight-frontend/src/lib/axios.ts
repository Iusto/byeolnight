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
    // 쿠키 기반 인증이므로 별도 헤더 추가 불필요
    // withCredentials: true로 쿠키가 자동 전송됨
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

// 응답 인터셉터 임시 비활성화 (디버깅용)
instance.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    console.log('axios 인터셉터에서 에러 발생:', error.response?.data?.message);
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