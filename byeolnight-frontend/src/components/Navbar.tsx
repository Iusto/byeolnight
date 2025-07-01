import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import StellaIcon from './StellaIcon';
import { useEffect, useState } from 'react';
import axios from '../lib/axios';

interface EquippedIcon {
  id: number;
  name: string;
  iconUrl: string;
  animationClass?: string;
  grade: string;
}

export default function Navbar() {
  const { user, logout } = useAuth();
  const [equippedIcon, setEquippedIcon] = useState<EquippedIcon | null>(null);

  useEffect(() => {
    if (user?.equippedIconId) {
      fetchEquippedIcon();
    }
  }, [user]);

  const fetchEquippedIcon = async () => {
    try {
      const res = await axios.get('/shop/my-icons');
      const icons = res.data.data || [];
      const equipped = icons.find((icon: any) => icon.equipped);
      setEquippedIcon(equipped || null);
    } catch (err) {
      console.error('장착 아이콘 조회 실패', err);
    }
  };

  return (
    <header className="bg-gradient-to-r from-[#1f2336]/95 via-[#252842]/95 to-[#1f2336]/95 backdrop-blur-md shadow-xl border-b border-purple-500/20 sticky top-0 z-50">
      <nav className="max-w-7xl mx-auto px-6 py-4">
        <div className="flex justify-between items-center">
          {/* 로고 */}
          <Link 
            to="/" 
            className="flex items-center gap-3 text-2xl font-bold text-white hover:text-purple-300 transition-all duration-300 group"
          >
            <div className="relative">
              <span className="text-3xl group-hover:animate-pulse">🌌</span>
              <div className="absolute -top-1 -right-1 w-2 h-2 bg-purple-400 rounded-full animate-ping"></div>
            </div>
            <span className="bg-gradient-to-r from-white via-purple-200 to-white bg-clip-text text-transparent">
              별 헤는 밤
            </span>
          </Link>

          {/* 네비게이션 */}
          <div className="flex items-center gap-6">
            {/* 메인 메뉴 */}
            <div className="hidden md:flex items-center gap-4">
              <Link 
                to="/posts" 
                className="flex items-center gap-2 px-4 py-2 rounded-lg bg-purple-600/20 hover:bg-purple-600/40 text-purple-200 hover:text-white transition-all duration-200 border border-purple-500/30 hover:border-purple-400"
              >
                <span>📚</span>
                <span className="font-medium">게시판</span>
              </Link>
              
              {user && (
                <>
                  <Link 
                    to="/certificates" 
                    className="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-yellow-600/20 text-yellow-300 hover:text-yellow-200 transition-all duration-200"
                  >
                    <span>🏆</span>
                    <span>인증서</span>
                  </Link>
                  <Link 
                    to="/shop" 
                    className="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-blue-600/20 text-blue-300 hover:text-blue-200 transition-all duration-200"
                  >
                    <span>🌟</span>
                    <span>상점</span>
                  </Link>
                </>
              )}
            </div>

            {/* 사용자 영역 */}
            {user ? (
              <div className="flex items-center gap-4">
                {/* 포인트 */}
                <Link to="/points" className="flex items-center gap-2 bg-gradient-to-r from-yellow-900/40 to-orange-900/40 hover:from-yellow-800/50 hover:to-orange-800/50 px-3 py-2 rounded-lg border border-yellow-500/30 hover:border-yellow-400/50 shadow-lg transition-all duration-200 transform hover:scale-105">
                  <span className="text-yellow-400 text-lg animate-pulse">⭐</span>
                  <span className="text-yellow-200 font-bold">{user.points?.toLocaleString() || 0}</span>
                </Link>

                {/* 사용자 정보 */}
                <div className="flex items-center gap-3 bg-[#2a2e45]/60 px-4 py-2 rounded-lg border border-purple-500/30">
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
                    {equippedIcon && (
                      <StellaIcon
                        iconUrl={equippedIcon.iconUrl}
                        animationClass={equippedIcon.animationClass}
                        grade={equippedIcon.grade}
                        size="sm"
                      />
                    )}
                    <span className="text-purple-200 font-semibold">{user.nickname}</span>
                    {localStorage.getItem('rememberMe') === 'true' && (
                      <span className="text-xs text-green-300 bg-green-900/30 px-2 py-1 rounded border border-green-600/30">
                        자동
                      </span>
                    )}
                  </div>
                </div>

                {/* 메뉴 버튼들 */}
                <div className="flex items-center gap-2">
                  <Link 
                    to="/me" 
                    className="p-2 rounded-lg hover:bg-purple-600/20 text-purple-300 hover:text-purple-200 transition-all duration-200"
                    title="내 정보"
                  >
                    <span className="text-lg">👤</span>
                  </Link>
                  
                  {user.role === 'ADMIN' && (
                    <Link 
                      to="/admin/users" 
                      className="p-2 rounded-lg hover:bg-red-600/20 text-red-300 hover:text-red-200 transition-all duration-200"
                      title="관리자"
                    >
                      <span className="text-lg">⚙️</span>
                    </Link>
                  )}
                  
                  <button 
                    onClick={logout}
                    className="p-2 rounded-lg hover:bg-red-600/20 text-red-400 hover:text-red-300 transition-all duration-200"
                    title="로그아웃"
                  >
                    <span className="text-lg">🚪</span>
                  </button>
                </div>
              </div>
            ) : (
              <div className="flex items-center gap-3">
                <Link 
                  to="/login" 
                  className="px-4 py-2 rounded-lg bg-purple-600/20 hover:bg-purple-600/40 text-purple-200 hover:text-white transition-all duration-200 border border-purple-500/30 hover:border-purple-400 font-medium"
                >
                  로그인
                </Link>
                <Link 
                  to="/signup" 
                  className="px-4 py-2 rounded-lg bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white font-medium shadow-lg hover:shadow-xl transition-all duration-200 transform hover:scale-105"
                >
                  회원가입
                </Link>
              </div>
            )}
          </div>
        </div>

        {/* 모바일 메뉴 */}
        {user && (
          <div className="md:hidden mt-4 pt-4 border-t border-purple-500/20">
            <div className="flex justify-center gap-4">
              <Link 
                to="/certificates" 
                className="flex flex-col items-center gap-1 p-3 rounded-lg hover:bg-yellow-600/20 text-yellow-300 transition-all duration-200"
              >
                <span className="text-xl">🏆</span>
                <span className="text-xs">인증서</span>
              </Link>
              <Link 
                to="/shop" 
                className="flex flex-col items-center gap-1 p-3 rounded-lg hover:bg-blue-600/20 text-blue-300 transition-all duration-200"
              >
                <span className="text-xl">🌟</span>
                <span className="text-xs">상점</span>
              </Link>
            </div>
          </div>
        )}
      </nav>
    </header>
  );
}
