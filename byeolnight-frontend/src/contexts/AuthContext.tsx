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

  // 세션 인증 상태 확인 함수
  const waitForSessionAuth = async (): Promise<void> => {
    const maxAttempts = 10;
    const delay = 200;
    
    for (let i = 0; i < maxAttempts; i++) {
      try {
        // 간단한 API 요청으로 세션 상태 확인
        const testResponse = await axios.get('/member/users/me');
        if (testResponse.status === 200) {
          return; // 세션 인증 성공
        }
      } catch (error: any) {
        if (error?.response?.status !== 401) {
          return; // 401이 아닌 다른 에러는 세션 문제가 아님
        }
      }
      
      await new Promise(resolve => setTimeout(resolve, delay));
    }
    
    throw new Error('세션 인증 대기 시간 초과');
  };

  const fetchMyInfo = async () => {
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
      if (err?.response?.status === 401) {
        setUser(null);
        return false;
      }
      
      setUser(null);
      return false;
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

  const login = async (email: string, password: string, rememberMe: boolean = false) => {
    try {
      const loginData = { email, password };
      const res = await axios.post('/auth/login', loginData);

      if (res.data?.success) {
        const safeSetRememberMe = (value: boolean) => {
          try {
            if (value) {
              localStorage.setItem('rememberMe', 'true');
              sessionStorage.setItem('rememberMe', 'true');
            } else {
              localStorage.removeItem('rememberMe');
              sessionStorage.removeItem('rememberMe');
            }
          } catch (storageError) {
            // Storage 접근 실패 시 무시
          }
        };
        
        safeSetRememberMe(rememberMe);
        
        // 세션 인증 상태가 될 때까지 기다린 후 /me API 호출
        await waitForSessionAuth();
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
      try {
        localStorage.removeItem('rememberMe');
        sessionStorage.removeItem('rememberMe');
      } catch (storageError) {
        // Storage 접근 실패 시 무시
      }
      setUser(null);
      alert("로그아웃 되었습니다.");
      navigate('/');
    }
  };

  const getSafeRememberMe = (): boolean => {
    try {
      const localStorage_value = localStorage.getItem('rememberMe');
      const sessionStorage_value = sessionStorage.getItem('rememberMe');
      return localStorage_value === 'true' || sessionStorage_value === 'true';
    } catch (storageError) {
      return true;
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

  // 주기적으로 토큰 갱신
  useEffect(() => {
    const rememberMe = getSafeRememberMe();
    
    if (user && rememberMe) {
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