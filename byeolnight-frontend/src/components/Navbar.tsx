import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import UserIconDisplay from './UserIconDisplay';
import NotificationDropdown from './notification/NotificationDropdown';

export default function Navbar() {
  const { user, logout } = useAuth();

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
              
              <Link 
                to="/suggestions" 
                className="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-orange-600/20 text-orange-300 hover:text-orange-200 transition-all duration-200"
              >
                <span>💡</span>
                <span>건의게시판</span>
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
                    className="relative flex items-center gap-2 px-4 py-2 rounded-lg bg-gradient-to-r from-purple-600/30 via-blue-600/30 to-purple-600/30 hover:from-purple-500/40 hover:via-blue-500/40 hover:to-purple-500/40 text-white font-medium transition-all duration-300 transform hover:scale-105 shadow-lg hover:shadow-purple-500/25 border border-purple-400/30 hover:border-purple-300/50 overflow-hidden group"
                  >
                    {/* 반짝이는 배경 효과 */}
                    <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-1000 ease-in-out"></div>
                    
                    {/* 스텔라 아이콘 */}
                    <div className="relative flex items-center justify-center">
                      <span className="text-lg animate-pulse group-hover:animate-spin transition-all duration-300">✨</span>
                      {/* 주변 반짝이 효과 */}
                      <div className="absolute -top-1 -right-1 w-1 h-1 bg-yellow-300 rounded-full animate-ping"></div>
                      <div className="absolute -bottom-1 -left-1 w-1 h-1 bg-blue-300 rounded-full animate-ping" style={{animationDelay: '0.5s'}}></div>
                      <div className="absolute top-0 left-0 w-1 h-1 bg-purple-300 rounded-full animate-ping" style={{animationDelay: '1s'}}></div>
                    </div>
                    
                    <span className="relative z-10 bg-gradient-to-r from-yellow-200 via-white to-blue-200 bg-clip-text text-transparent font-bold group-hover:from-yellow-100 group-hover:via-white group-hover:to-blue-100 transition-all duration-300">
                      스텔라 상점
                    </span>
                    
                    {/* 우주 먼지 효과 */}
                    <div className="absolute top-1 right-2 w-0.5 h-0.5 bg-white rounded-full animate-pulse opacity-60"></div>
                    <div className="absolute bottom-2 left-3 w-0.5 h-0.5 bg-purple-200 rounded-full animate-pulse opacity-40" style={{animationDelay: '0.7s'}}></div>
                  </Link>
                </>
              )}
            </div>

            {/* 사용자 영역 */}
            {user ? (
              <div className="flex items-center gap-4">
                {/* 포인트 */}
                <Link to="/points" className="flex items-center gap-2 bg-gradient-to-r from-yellow-900/40 to-orange-900/40 hover:from-yellow-800/50 hover:to-orange-800/50 px-3 py-2 rounded-lg border border-yellow-500/30 hover:border-yellow-400/50 shadow-lg transition-all duration-200 transform hover:scale-105">
                  <span className="text-yellow-400 text-lg animate-pulse">✨</span>
                  <span className="text-yellow-200 font-bold">{user.points?.toLocaleString() || 0}</span>
                </Link>

                {/* 사용자 정보 */}
                <div className="flex items-center gap-3 bg-[#2a2e45]/60 px-4 py-2 rounded-lg border border-purple-500/30">
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
                    {user.equippedIconName && (
                      <UserIconDisplay
                        iconName={user.equippedIconName}
                        size="small"
                        className="text-purple-300"
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
                  <NotificationDropdown />
                  
                  <Link 
                    to="/profile" 
                    className="p-2 rounded-lg bg-white/90 hover:bg-white text-gray-700 hover:text-gray-900 transition-all duration-200 shadow-md hover:shadow-lg transform hover:scale-105"
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
                className="relative flex flex-col items-center gap-1 p-3 rounded-lg bg-gradient-to-b from-purple-600/20 to-blue-600/20 hover:from-purple-500/30 hover:to-blue-500/30 text-white transition-all duration-300 transform hover:scale-105 shadow-lg border border-purple-400/20 group overflow-hidden"
              >
                {/* 모바일 반짝이는 배경 효과 */}
                <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-1000 ease-in-out"></div>
                
                <div className="relative">
                  <span className="text-xl animate-pulse group-hover:animate-spin transition-all duration-300">✨</span>
                  <div className="absolute -top-0.5 -right-0.5 w-0.5 h-0.5 bg-yellow-300 rounded-full animate-ping"></div>
                </div>
                <span className="text-xs font-medium bg-gradient-to-r from-yellow-200 to-blue-200 bg-clip-text text-transparent">스텔라</span>
              </Link>
              <Link 
                to="/suggestions" 
                className="flex flex-col items-center gap-1 p-3 rounded-lg hover:bg-orange-600/20 text-orange-300 transition-all duration-200"
              >
                <span className="text-xl">💡</span>
                <span className="text-xs">건의게시판</span>
              </Link>
            </div>
          </div>
        )}
      </nav>
    </header>
  );
}
