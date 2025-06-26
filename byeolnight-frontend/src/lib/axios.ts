import axios from 'axios';

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // ← 환경변수에서 API 주소 지정
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // ← 필요 시 쿠키 포함
});

instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');

    // 전체 URL을 정확하게 파싱하기 위해 baseURL과 결합
    const url = new URL(config.url!, config.baseURL);

    // 공개 API는 Authorization 헤더 제거 (예: /api/public/**)
    const isPublicEndpoint = url.pathname.startsWith('/public/');
    const isAuthEndpoint =
      url.pathname.includes('/auth/login') || url.pathname.includes('/auth/signup');

    // 인증이 필요한 경우만 Authorization 헤더 설정
    if (token && !isAuthEndpoint && !isPublicEndpoint) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

export default instance;
