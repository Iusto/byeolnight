import axios from 'axios';

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // ← 환경변수에서 가져오기
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // ← 필요 시 쿠키 인증 사용
});

instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    const isAuthEndpoint = config.url?.includes('/auth/login') || config.url?.includes('/auth/signup');

    if (token && !isAuthEndpoint) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

export default instance;
