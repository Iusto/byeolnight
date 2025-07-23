import { createContext, useContext, useEffect, useState } from 'react';
import axios from '../lib/axios';
import { useNavigate } from 'react-router-dom';

interface User {
  id: number;
  email: string;
  nickname: string;
  phone: string;
  role: string;
  points: number;
  equippedIconId?: number;
  equippedIconName?: string;
  representativeCertificate?: {
    icon: string;
    name: string;
  };
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string, rememberMe?: boolean) => Promise<void>;
  logout: () => void;
  refreshToken: () => Promise<boolean>;
  refreshUserInfo: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const fetchMyInfo = async () => {
    try {
      const res = await axios.get('/member/users/me');
      console.log('내 정보 응답:', res.data);
      const userData = res.data?.success ? res.data.data : null;
      
      if (!userData) {
        throw new Error('사용자 데이터를 받지 못했습니다.');
      }
      
      // 대표 인증서 정보도 가져오기
      try {
        const certRes = await axios.get('/member/certificates/representative');
        if (certRes.data?.data) {
          userData.representativeCertificate = {
            icon: certRes.data.data.icon,
            name: certRes.data.data.name
          };
        }
      } catch (certErr) {
        console.log('대표 인증서 조회 실패 (정상)', certErr);
      }
      
      setUser(userData);
      return true;
    } catch (err) {
      console.error('내 정보 조회 실패:', err);
      setUser(null);
      return false;
    }
  };

  // 토큰 갱신 함수
  const refreshToken = async (): Promise<boolean> => {
    try {
      const res = await axios.post('/auth/token/refresh');
      // 토큰이 쿠키로 전달되지만 응답 본문에서도 가져와서 사용
      if (res.data?.success) {
        // 응답 본문에서 토큰 가져와서 저장
        const newAccessToken = res.data?.data?.accessToken;
        if (newAccessToken) {
          localStorage.setItem('accessToken', newAccessToken);
          console.log('토큰 갱신 성공:', newAccessToken.substring(0, 10) + '...');
        } else {
          console.warn('갱신된 토큰이 응답 본문에 없습니다.');
        }
        
        await fetchMyInfo();
        return true;
      }
      return false;
    } catch (error) {
      console.error('토큰 갱신 실패:', error);
      return false;
    }
  };

  const login = async (email: string, password: string, rememberMe: boolean = false) => {
    try {
      console.log('로그인 요청 시작:', { email, rememberMe });
      console.log('모바일 디버그:', {
        userAgent: navigator.userAgent,
        isMobile: /Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent),
        baseURL: axios.defaults.baseURL
      });
      
      // 객체 형태로 명확하게 지정하여 배열 형태로 전송되는 문제 방지
      const loginData = {
        email: email,
        password: password
      };
      
      console.log('로그인 요청 데이터:', JSON.stringify(loginData));
      
      const res = await axios.post('/auth/login', loginData);
      
      console.log('로그인 응답:', res.data);

      if (res.data?.success) {
        // 쿠키로 전달된 토큰을 사용하지만, 응답 본문에서도 토큰을 가져와서 저장
        const accessToken = res.data?.data?.accessToken;
        if (accessToken) {
          // 응답 본문에서 받은 토큰을 저장
          localStorage.setItem('accessToken', accessToken);
          console.log('토큰 저장 성공:', accessToken.substring(0, 10) + '...');
        } else {
          console.warn('토큰이 응답 본문에 없습니다.');
        }
        
        // 로그인 유지 옵션 저장
        if (rememberMe) {
          localStorage.setItem('rememberMe', 'true');
        } else {
          localStorage.removeItem('rememberMe');
        }

        await fetchMyInfo();
        // 인앱브라우저 호환성을 위해 setTimeout 제거
        // 로그인 성공 후 리다이렉션은 Login 컴포넌트에서 처리
      } else {
        throw new Error('로그인에 실패했습니다.');
      }
    } catch (err: any) {
      console.error('로그인 에러 상세:', {
        message: err?.message,
        status: err?.response?.status,
        statusText: err?.response?.statusText,
        data: err?.response?.data,
        config: {
          url: err?.config?.url,
          baseURL: err?.config?.baseURL,
          headers: err?.config?.headers
        },
        network: err?.code === 'NETWORK_ERROR' || err?.message?.includes('Network Error'),
        timeout: err?.code === 'ECONNABORTED'
      });
      
      // 네트워크 오류 시 사용자 친화적 메시지
      if (err?.code === 'NETWORK_ERROR' || err?.message?.includes('Network Error')) {
        throw new Error('네트워크 연결에 실패했습니다. 인터넷 연결을 확인해주세요.');
      }
      
      // 서버에서 온 구체적인 에러 메시지 전달
      const errorMessage = err?.response?.data?.message || '로그인에 실패했습니다.';
      throw new Error(errorMessage);
    }
  };

  const logout = async () => {
    try {
      // 백엔드 로그아웃 API 호출
      // 쿠키에 토큰이 있으므로 별도의 헤더 설정 필요 없음
      await axios.post('/auth/logout');
    } catch (error) {
      console.error('로그아웃 API 호출 실패:', error);
    } finally {
      // 로컬 상태 정리
      localStorage.removeItem('rememberMe'); // rememberMe 옵션 삭제
      localStorage.removeItem('accessToken'); // 토큰 삭제
      setUser(null);
      alert("로그아웃 되었습니다.");
      navigate('/');
    }
  };

  // 초기 로딩 시 로그인 상태 확인
  useEffect(() => {
    const initializeAuth = async () => {
      const rememberMe = localStorage.getItem('rememberMe');
      
      // 인증이 필요 없는 경로 확인
      const isHomePage = window.location.pathname === '/';
      const isPublicPage = window.location.pathname.includes('/posts') && !window.location.pathname.includes('/posts/new');
      
      // sitemap.xml, robots.txt 등 정적 파일 경로 확인
      const isStaticFile = [
        '/sitemap.xml', 
        '/robots.txt',
        '/favicon.ico'
      ].some(path => window.location.pathname === path) || 
        window.location.pathname.startsWith('/sitemap-');
      
      if (isHomePage || isPublicPage || isStaticFile) {
        console.log('인증이 필요 없는 페이지에서는 사용자 정보 조회를 스킵합니다.');
        setLoading(false);
        return;
      }
      
      // 사용자 정보 조회 시도 (쿠키에 토큰이 있는지 확인)
      const success = await fetchMyInfo();
      
      // 실패 시 토큰 갱신 시도 (로그인 유지 옵션이 있는 경우)
      if (!success && rememberMe === 'true') {
        const refreshSuccess = await refreshToken();
        if (!refreshSuccess) {
          // 토큰 갱신도 실패하면 로그인 유지 옵션 제거
          localStorage.removeItem('rememberMe');
        }
      }
      
      setLoading(false);
    };

    initializeAuth();
  }, []);

  // 주기적으로 토큰 갱신 (로그인 유지 옵션이 있는 경우)
  useEffect(() => {
    const rememberMe = localStorage.getItem('rememberMe');
    if (user && rememberMe === 'true') {
      // 25분마다 토큰 갱신 시도 (Access Token이 30분이므로)
      const interval = setInterval(() => {
        refreshToken();
      }, 25 * 60 * 1000);

      return () => clearInterval(interval);
    }
  }, [user]);

  const refreshUserInfo = async () => {
    console.log('사용자 정보 새로고침 시작');
    const success = await fetchMyInfo();
    console.log('사용자 정보 새로고침 결과:', success);
    if (success) {
      console.log('새로고침된 사용자 포인트:', user?.points);
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, refreshToken, refreshUserInfo }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth는 AuthProvider 안에서 사용해야 합니다.');
  }
  return context;
};
