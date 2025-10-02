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
      
      // 대표 인증서 정보 가져오기
      try {
        const certRes = await axios.get('/member/certificates/representative');
        if (certRes.data?.data) {
          userData.representativeCertificate = {
            icon: certRes.data.data.icon,
            name: certRes.data.data.name
          };
        }
      } catch {
        // 인증서 조회 실패 무시
      }
      
      setUser(userData);
      return true;
    } catch (error) {
      console.warn('사용자 정보 조회 실패:', error);
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
      if (user) await axios.post('/auth/logout');
    } catch {
      // 로그아웃 API 실패 무시
    }
    
    setUser(null);
    alert("로그아웃 되었습니다.");
    navigate('/');
  };

  // 초기 로딩 시 로그인 상태 확인
  useEffect(() => {
    const initAuth = async () => {
      await fetchMyInfo();
      setLoading(false);
    };
    
    initAuth();
  }, []);

  // 로그인된 사용자만 토큰 갱신
  useEffect(() => {
    if (!user) return;
    
    let failureCount = 0;
    const maxFailures = 3;
    
    const interval = setInterval(async () => {
      try {
        const res = await axios.post('/auth/token/refresh');
        if (!res.data?.success) {
          failureCount++;
          if (failureCount >= maxFailures) {
            console.warn('토큰 갱신 연속 실패로 로그아웃 처리');
            setUser(null);
          }
        } else {
          failureCount = 0; // 성공 시 실패 카운트 리셋
        }
      } catch (error) {
        failureCount++;
        console.warn(`토큰 갱신 실패 (${failureCount}/${maxFailures}):`, error);
        if (failureCount >= maxFailures) {
          console.warn('토큰 갱신 연속 실패로 로그아웃 처리');
          setUser(null);
        }
      }
    }, 25 * 60 * 1000);

    return () => clearInterval(interval);
  }, [user]);

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