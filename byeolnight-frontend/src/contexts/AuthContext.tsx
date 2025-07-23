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
      // 헤더에 토큰이 있는지 확인하기 위해 토큰 로그 추가
      const accessToken = localStorage.getItem('accessToken');
      console.log('내 정보 조회 시도 - 토큰 여부:', accessToken ? '있음' : '없음');
      
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
      
      // 응답 본문에서 토큰 가져와서 저장
      if (res.data?.success) {
        const newAccessToken = res.data?.data?.accessToken;
        if (newAccessToken) {
          localStorage.setItem('accessToken', newAccessToken);
          console.log('토큰 갱신 성공 - 저장된 토큰:', newAccessToken.substring(0, 10) + '...');
          
          // 토큰 저장 후 사용자 정보 가져오기
          await fetchMyInfo();
          return true;
        } else {
          console.warn('갱신된 토큰이 응답 본문에 없습니다.');
          return false;
        }
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
      
      // 객체 형태로 명확하게 지정하여 배열 형태로 전송되는 문제 방지
      const loginData = {
        email: email,
        password: password
      };
      
      console.log('로그인 요청 데이터:', JSON.stringify(loginData));
      
      const res = await axios.post('/auth/login', loginData);
      
      console.log('로그인 응답:', res.data);

      if (res.data?.success) {
        // 응답 본문에서 토큰 가져와서 저장
        const accessToken = res.data?.data?.accessToken;
        if (accessToken) {
          localStorage.setItem('accessToken', accessToken);
          console.log('로그인 성공 - 저장된 토큰:', accessToken.substring(0, 10) + '...');
        } else {
          console.warn('토큰이 응답 본문에 없습니다.');
        }
        
        // 로그인 유지 옵션 저장
        if (rememberMe) {
          localStorage.setItem('rememberMe', 'true');
        } else {
          localStorage.removeItem('rememberMe');
        }

        // 토큰 저장 후 사용자 정보 가져오기
        console.log('로그인 성공 - 사용자 정보 가져오기 시도');
        await fetchMyInfo();
        console.log('로그인 완료 - 사용자 정보:', user);
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
      // 로컬 상태 정리
      localStorage.removeItem('rememberMe');
      localStorage.removeItem('accessToken');
      setUser(null);
      alert("로그아웃 되었습니다.");
      navigate('/');
    }
  };

  // 초기 로딩 시 로그인 상태 확인
  useEffect(() => {
    const initializeAuth = async () => {
      const accessToken = localStorage.getItem('accessToken');
      const rememberMe = localStorage.getItem('rememberMe');
      
      console.log('인증 상태 확인:', { 
        accessToken: accessToken ? '존재함' : '없음', 
        tokenLength: accessToken?.length,
        rememberMe 
      });
      
      if (accessToken) {
        console.log('저장된 토큰으로 사용자 정보 조회 시도');
        const success = await fetchMyInfo();
        
        if (!success && rememberMe === 'true') {
          console.log('사용자 정보 조회 실패, 토큰 갱신 시도');
          await refreshToken();
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