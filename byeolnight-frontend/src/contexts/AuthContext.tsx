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
      console.log('내 정보 조회 시도 - 쿠키 기반 인증');
      
      const res = await axios.get('/member/users/me');
      console.log('내 정보 응답 성공:', res.data);
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
      console.log('사용자 정보 설정 성공:', userData);
      return true;
    } catch (err: any) {
      console.error('내 정보 조회 실패:', {
        message: err?.message,
        status: err?.response?.status,
        statusText: err?.response?.statusText,
        data: err?.response?.data
      });
      setUser(null);
      return false;
    }
  };

  // 토큰 갱신 함수
  const refreshToken = async (): Promise<boolean> => {
    try {
      const res = await axios.post('/auth/token/refresh');
      console.log('토큰 갱신 응답:', res.data);
      
      // 쿠키 기반이므로 별도 저장 불필요
      if (res.data?.success) {
        console.log('토큰 갱신 성공 - 쿠키로 자동 저장됨');
        
        // 토큰 갱신 후 사용자 정보 가져오기
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
      console.log('로그인 요청 시작 (인앱브라우저 호환):', { email, rememberMe });
      
      const loginData = {
        email: email,
        password: password
      };
      
      console.log('로그인 요청 데이터:', JSON.stringify(loginData));
      
      const res = await axios.post('/auth/login', loginData);
      
      console.log('로그인 응답:', res.data);

      if (res.data?.success) {
        console.log('로그인 성공 - 쿠키로 토큰 자동 저장됨');
        
        // 로그인 유지 옵션을 안전하게 저장 (인앱브라우저 호환)
        try {
          if (rememberMe) {
            localStorage.setItem('rememberMe', 'true');
          } else {
            localStorage.removeItem('rememberMe');
          }
        } catch (storageError) {
          console.warn('localStorage 접근 실패 (인앱브라우저):', storageError);
          // localStorage 실패해도 로그인은 계속 진행
        }

        // 토큰은 쿠키로 저장되므로 바로 사용자 정보 가져오기
        console.log('로그인 성공 - 사용자 정보 가져오기 시도');
        await fetchMyInfo();
        console.log('로그인 완료');
      } else {
        throw new Error('로그인에 실패했습니다.');
      }
    } catch (err: any) {
      console.error('로그인 에러 상세:', {
        message: err?.message,
        status: err?.response?.status,
        statusText: err?.response?.statusText,
        data: err?.response?.data
      });
      
      // 서버에서 온 구체적인 에러 메시지 전달
      const errorMessage = err?.response?.data?.message || '로그인에 실패했습니다.';
      throw new Error(errorMessage);
    }
  };

  const logout = async () => {
    try {
      // 백엔드 로그아웃 API 호출
      await axios.post('/auth/logout');
    } catch (error) {
      console.error('로그아웃 API 호출 실패:', error);
    } finally {
      // 로컬 상태 정리 (인앱브라우저 호환)
      try {
        localStorage.removeItem('rememberMe');
      } catch (storageError) {
        console.warn('localStorage 접근 실패 (인앱브라우저):', storageError);
      }
      setUser(null);
      alert("로그아웃 되었습니다.");
      navigate('/');
    }
  };

  // 초기 로딩 시 로그인 상태 확인
  useEffect(() => {
    const initializeAuth = async () => {
      let rememberMe = 'false';
      
      // localStorage 안전하게 접근 (인앱브라우저 호환)
      try {
        rememberMe = localStorage.getItem('rememberMe') || 'false';
      } catch (storageError) {
        console.warn('localStorage 접근 실패 (인앱브라우저):', storageError);
      }
      
      console.log('인증 상태 확인 (쿠키 기반):', { rememberMe });
      
      // 쿠키에 토큰이 있는지 확인하기 위해 사용자 정보 조회 시도
      console.log('쿠키 토큰으로 사용자 정보 조회 시도');
      const success = await fetchMyInfo();
      
      if (!success && rememberMe === 'true') {
        console.log('사용자 정보 조회 실패, 토큰 갱신 시도');
        try {
          await refreshToken();
        } catch (refreshError) {
          console.log('초기 토큰 갱신 실패 - 비로그인 상태로 유지');
        }
      }
      
      setLoading(false);
    };

    initializeAuth();
  }, []);

  // 주기적으로 토큰 갱신 (로그인 유지 옵션이 있는 경우)
  useEffect(() => {
    let rememberMe = 'false';
    try {
      rememberMe = localStorage.getItem('rememberMe') || 'false';
    } catch (storageError) {
      console.warn('localStorage 접근 실패 (인앱브라우저):', storageError);
    }
    
    if (user && rememberMe === 'true') {
      // 25분마다 토큰 갱신 시도 (Access Token이 30분이므로)
      const interval = setInterval(async () => {
        try {
          await refreshToken();
        } catch (refreshError) {
          console.log('주기적 토큰 갱신 실패:', refreshError);
        }
      }, 25 * 60 * 1000);

      return () => clearInterval(interval);
    }
  }, [user]);

  const refreshUserInfo = async () => {
    console.log('사용자 정보 새로고침 시작');
    const success = await fetchMyInfo();
    console.log('사용자 정보 새로고침 결과:', success);
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