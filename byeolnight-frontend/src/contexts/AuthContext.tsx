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
      const newToken = res.data?.success ? res.data.data?.accessToken : null;
      
      if (newToken) {
        localStorage.setItem('accessToken', newToken);
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
      const res = await axios.post('/auth/login', {
        email,
        password,
      });
      
      console.log('로그인 응답:', res.data);

      const token = res.data?.success ? res.data.data?.accessToken : null;
      if (token) {
        localStorage.setItem('accessToken', token);
        
        // 로그인 유지 옵션 저장
        if (rememberMe) {
          localStorage.setItem('rememberMe', 'true');
        } else {
          localStorage.removeItem('rememberMe');
        }

        await fetchMyInfo();
        navigate('/');
      } else {
        throw new Error('토큰을 받지 못했습니다.');
      }
    } catch (err: any) {
      console.error('로그인 에러:', err);
      // 서버에서 온 구체적인 에러 메시지 전달
      const errorMessage = err?.response?.data?.message || '로그인에 실패했습니다.';
      throw new Error(errorMessage);
    }
  };

  const logout = async () => {
    try {
      // 백엔드 로그아웃 API 호출
      const token = localStorage.getItem('accessToken');
      if (token) {
        await axios.post('/auth/logout', {}, {
          headers: { Authorization: `Bearer ${token}` }
        });
      }
    } catch (error) {
      console.error('로그아웃 API 호출 실패:', error);
    } finally {
      // 로컬 상태 정리
      localStorage.removeItem('accessToken');
      setUser(null);
      navigate('/login');
    }
  };

  // 초기 로딩 시 로그인 상태 확인
  useEffect(() => {
    const initializeAuth = async () => {
      const token = localStorage.getItem('accessToken');
      const rememberMe = localStorage.getItem('rememberMe');
      
      if (token) {
        // 사용자 정보 조회 시도
        const success = await fetchMyInfo();
        
        // 실패 시 토큰 갱신 시도 (로그인 유지 옵션이 있는 경우)
        if (!success && rememberMe === 'true') {
          const refreshSuccess = await refreshToken();
          if (!refreshSuccess) {
            // 토큰 갱신도 실패하면 로그아웃
            localStorage.removeItem('accessToken');
            localStorage.removeItem('rememberMe');
          }
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
