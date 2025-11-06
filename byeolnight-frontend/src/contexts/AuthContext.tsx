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
  login: (email: string, password: string, rememberMe?: boolean) => Promise<void>;
  logout: () => void;
  refreshUserInfo: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();



  const fetchMyInfo = async (): Promise<boolean> => {
    try {
      const res = await axios.get('/member/users/me');
      const userData = res.data?.success ? res.data.data : null;
      
      if (!userData) {
        setUser(null);
        return false;
      }
      
      // 대표 인증서 정보 가져오기 (실패해도 계속 진행)
      const certRes = await axios.get('/member/certificates/representative').catch(() => null);
      if (certRes?.data?.data) {
        userData.representativeCertificate = {
          icon: certRes.data.data.icon,
          name: certRes.data.data.name
        };
      }
      
      setUser(userData);
      return true;
    } catch (error: any) {
      // 401 에러는 정상 (비로그인 상태)
      if (error.response?.status === 401) {
        setUser(null);
        return false;
      }
      setUser(null);
      return false;
    }
  };

  const login = async (email: string, password: string, rememberMe: boolean = false) => {
    try {
      const res = await axios.post('/auth/login', { email, password, rememberMe });
      
      if (!res.data?.success) {
        throw new Error(res.data?.message || 'Login failed');
      }
      
      await fetchMyInfo();
    } catch (error: any) {
      // 서버에서 온 실제 에러 메시지를 그대로 전달
      const errorMessage = error.response?.data?.message || error.message || 'Login failed';
      throw new Error(errorMessage);
    }
  };

  const logout = async () => {
    try {
      await axios.post('/auth/logout');
    } catch {
      // 로그아웃 API 실패해도 계속 진행
    } finally {
      setUser(null);
      window.location.href = '/';
    }
  };

  // 초기 로딩 시 로그인 상태 확인
  useEffect(() => {
    const initAuth = async () => {
      // HttpOnly 쿠키는 document.cookie로 확인 불가
      // 무조건 API 호출해서 확인 (401이면 비로그인)
      await fetchMyInfo();
      setLoading(false);
    };
    
    initAuth();
  }, []);

  // 토큰 갱신은 axios 인터셉터에서 자동 처리 (401 시)

  const refreshUserInfo = async () => {
    try {
      await fetchMyInfo();
    } catch (error) {
      console.error('사용자 정보 갱신 실패:', error);
      throw error;
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, refreshUserInfo }}>
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