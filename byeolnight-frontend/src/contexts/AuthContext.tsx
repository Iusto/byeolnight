import { createContext, useContext, useEffect, useState } from 'react';
import axios from '../lib/axios';
import { checkServerHealth, redirectToMaintenance } from '../utils/healthCheck';
import { getErrorMessage } from '../types/api';
import type { User } from '../types/user';

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

  const fetchMyInfo = async (): Promise<boolean> => {
    try {
      const res = await axios.get('/auth/me');
      const userData = res.data?.success ? res.data.data : null;

      if (!userData) {
        setUser(null);
        return false;
      }

      setUser(userData);
      return true;
    } catch {
      // 에러 발생 시 비로그인 상태로 처리
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
    } catch (error: unknown) {
      // 서버에서 온 실제 에러 메시지를 그대로 전달
      throw new Error(getErrorMessage(error));
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

  useEffect(() => {
    const initAuth = async () => {
      // 앱 시작 시 서버 상태 확인
      const isHealthy = await checkServerHealth();
      
      if (!isHealthy) {
        redirectToMaintenance();
        return;
      }
      
      // 사용자 정보 로드
      try {
        await fetchMyInfo();
      } catch {
        setUser(null);
      } finally {
        setLoading(false);
      }
    };
    
    initAuth();
  }, []);

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