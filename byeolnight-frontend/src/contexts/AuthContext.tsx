import { createContext, useContext, useEffect, useState } from 'react';
import axios from '../lib/axios';
import { useNavigate } from 'react-router-dom';

interface User {
  id: number;
  email: string;
  nickname: string;
  role: string;
  points: number;
  equippedIconId?: number;
  equippedIconName?: string;
  socialProvider?: string;
  representativeCertificate?: {
    icon: string;
    name: string;
  };
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  refreshToken: () => Promise<boolean>;
  refreshUserInfo: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [isFetching, setIsFetching] = useState(false);
  const navigate = useNavigate();



  const fetchMyInfo = async () => {
    if (isFetching) {
      return false; // 이미 요청 중이면 중단
    }
    
    setIsFetching(true);
    try {
      const res = await axios.get('/member/users/me');
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
        // 대표 인증서 조회 실패는 무시
      }
      
      setUser(userData);
      return true;
    } catch (err: any) {
      // 401 에러는 비로그인 상태로 처리 (정상)
      setUser(null);
      return false;
    } finally {
      setIsFetching(false);
    }
  };

  const refreshToken = async (): Promise<boolean> => {
    try {
      const res = await axios.post('/auth/token/refresh');
      
      if (res.data?.success) {
        await fetchMyInfo();
        return true;
      }
      return false;
    } catch (error) {
      return false;
    }
  };

  const login = async (email: string, password: string) => {
    try {
      const loginData = { email, password };
      const res = await axios.post('/auth/login', loginData);

      if (res.data?.success) {
        // HttpOnly 쿠키로 토큰이 자동 저장됨
        // 로그인 성공 후 사용자 정보 조회
        await fetchMyInfo();
      } else {
        throw new Error(res.data?.message || 'Login failed');
      }
    } catch (err: any) {
      const errorMessage = err?.response?.data?.message || err?.message || 'Authentication failed';
      throw new Error(errorMessage);
    }
  };

  const logout = async () => {
    try {
      await axios.post('/auth/logout');
    } catch (error) {
      // 로그아웃 API 실패는 무시
    } finally {
      // HttpOnly 쿠키는 서버에서 자동 삭제됨
      setUser(null);
      alert("로그아웃 되었습니다.");
      navigate('/');
    }
  };



  // 초기 로딩 시 로그인 상태 확인 (페이지 새로고침 시에만)
  useEffect(() => {
    const initializeAuth = async () => {
      await fetchMyInfo();
      setLoading(false);
    };

    initializeAuth();
  }, []);

  // 주기적으로 토큰 갱신 (HttpOnly 쿠키 방식에서는 항상 실행)
  useEffect(() => {
    if (user) {
      const interval = setInterval(async () => {
        try {
          await refreshToken();
        } catch (refreshError) {
          // 토큰 갱신 실패 시 무시
        }
      }, 25 * 60 * 1000);

      return () => clearInterval(interval);
    }
  }, [user]);

  const refreshUserInfo = async () => {
    await fetchMyInfo();
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