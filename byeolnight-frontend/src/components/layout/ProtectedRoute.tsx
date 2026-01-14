import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { useEffect } from 'react';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requireAuth?: boolean;
  requireAdmin?: boolean;
}

export const ProtectedRoute = ({
  children,
  requireAuth = true,
  requireAdmin = false
}: ProtectedRouteProps) => {
  const { user, loading, refreshUserInfo } = useAuth();
  const location = useLocation();

  // 로딩 중일 때는 로딩 표시
  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-purple-500"></div>
      </div>
    );
  }

  // 인증이 필요한데 로그인하지 않은 경우
  // 홈 페이지는 로그인하지 않아도 접근 가능하도록 예외 처리
  if (requireAuth && !user && location.pathname !== '/') {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // 관리자 권한이 필요한데 일반 사용자인 경우
  if (requireAdmin && user?.role !== 'ADMIN') {
    return <Navigate to="/unauthorized" replace />;
  }

  return <>{children}</>;
};