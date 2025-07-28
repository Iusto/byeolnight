import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useTranslation } from 'react-i18next';
import UserIconDisplay from './UserIconDisplay';
import NotificationDropdown from './notification/NotificationDropdown';
import LanguageSwitcher from './LanguageSwitcher';
import { useState } from 'react';

export default function Navbar() {
  const { user, logout } = useAuth();
  const { t } = useTranslation();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  return (
    <header className="bg-gradient-to-r from-slate-900/95 via-purple-900/95 to-slate-900/95 backdrop-blur-md shadow-2xl border-b border-purple-500/30 sticky top-0 z-50">
      <nav className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* 로고 */}
          <Link 
            to="/" 
            className="flex items-center gap-3 text-xl sm:text-2xl font-bold text-white hover:text-purple-300 transition-all duration-300 group"
          >
            <div className="relative">
              <span className="text-2xl sm:text-3xl group-hover:animate-pulse">🌌</span>
              <div className="absolute -top-1 -right-1 w-2 h-2 bg-purple-400 rounded-full animate-ping"></div>
            </div>
            <span className="bg-gradient-to-r from-white via-purple-200 to-white bg-clip-text text-transparent hidden sm:block">
              {t('nav.logo_full') || '별 헤는 밤'}
            </span>
            <span className="bg-gradient-to-r from-white via-purple-200 to-white bg-clip-text text-transparent sm:hidden">
              {t('nav.logo_short') || '별헤는밤'}
            </span>
          </Link>

          {/* 데스크톱 네비게이션 */}
          <div className="hidden lg:flex items-center gap-3">
            <Link 
              to="/posts" 
              className="group flex items-center gap-2 px-3 py-2 rounded-full bg-gradient-to-r from-purple-600/20 to-blue-600/20 hover:from-purple-600/40 hover:to-blue-600/40 text-purple-200 hover:text-white transition-all duration-300 border border-purple-500/30 hover:border-purple-400/50"
            >
              <span className="group-hover:animate-bounce text-sm">📚</span>
              <span className="font-medium text-sm">{t('nav.posts')}</span>
            </Link>
            
            <Link 
              to="/suggestions" 
              className="group flex items-center gap-2 px-3 py-2 rounded-full hover:bg-orange-600/20 text-orange-300 hover:text-orange-200 transition-all duration-300"
            >
              <span className="group-hover:animate-pulse text-sm">💡</span>
              <span className="text-sm">{t('nav.suggestions')}</span>
            </Link>
            
            <Link 
              to="/shop" 
              className="relative flex items-center gap-2 px-4 py-2 rounded-full bg-gradient-to-r from-purple-600/30 via-pink-600/30 to-purple-600/30 hover:from-purple-500/50 hover:via-pink-500/50 hover:to-purple-500/50 text-white font-medium transition-all duration-300 transform hover:scale-105 shadow-lg hover:shadow-purple-500/25 border border-purple-400/30 hover:border-purple-300/50 overflow-hidden group"
            >
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-1000 ease-in-out"></div>
              
              <div className="relative flex items-center justify-center">
                <span className="text-lg animate-pulse group-hover:animate-spin transition-all duration-300">✨</span>
                <div className="absolute -top-1 -right-1 w-1 h-1 bg-yellow-300 rounded-full animate-ping"></div>
              </div>
              
              <span className="relative z-10 bg-gradient-to-r from-yellow-200 via-white to-pink-200 bg-clip-text text-transparent font-bold">
                {t('nav.shop')}
              </span>
            </Link>
            
            {user && (
              <Link 
                to="/certificates" 
                className="group flex items-center gap-2 px-3 py-2 rounded-full hover:bg-yellow-600/20 text-yellow-300 hover:text-yellow-200 transition-all duration-300"
              >
                <span className="group-hover:animate-bounce text-sm">🏆</span>
                <span className="text-sm">{t('nav.certificates')}</span>
              </Link>
            )}
          </div>

          {/* 사용자 영역 */}
          <div className="flex items-center gap-3">
            {user ? (
              <>
                {/* 포인트 (데스크톱만) */}
                <Link 
                  to="/points" 
                  className="hidden sm:flex items-center gap-2 bg-gradient-to-r from-yellow-900/40 to-orange-900/40 hover:from-yellow-800/50 hover:to-orange-800/50 px-3 py-2 rounded-full border border-yellow-500/30 hover:border-yellow-400/50 shadow-lg transition-all duration-300 transform hover:scale-105 group cursor-pointer"
                  title="출석체크 & 포인트 내역 보기"
                >
                  <span className="text-yellow-400 text-sm animate-pulse group-hover:animate-bounce">✨</span>
                  <div className="flex flex-col items-center">
                    <span className="text-yellow-200 font-bold text-xs leading-tight">{user.points?.toLocaleString() || 0}</span>
                    <span className="text-yellow-300/70 text-xs leading-tight group-hover:text-yellow-200 transition-colors">{t('nav.points')}</span>
                  </div>
                  <span className="text-yellow-300/50 text-xs group-hover:text-yellow-200 transition-colors">📊</span>
                </Link>

                {/* 사용자 정보 */}
                <div className="flex items-center gap-2 bg-gradient-to-r from-slate-800/80 to-purple-900/80 px-2 py-1.5 rounded-full border border-purple-500/40 shadow-lg backdrop-blur-sm">
                  {/* 사용자 아이콘 */}
                  {user.equippedIconName ? (
                    <div className="w-7 h-7 flex items-center justify-center bg-gradient-to-br from-purple-600/30 to-blue-600/30 rounded-full border border-purple-400/30">
                      <UserIconDisplay
                        iconName={user.equippedIconName}
                        size="small"
                        className="text-base"
                      />
                    </div>
                  ) : (
                    <div className="w-7 h-7 flex items-center justify-center bg-gradient-to-br from-gray-600/30 to-gray-700/30 rounded-full">
                      <span className="text-xs text-gray-400">👤</span>
                    </div>
                  )}
                  
                  {/* 닉네임 (데스크톱만) */}
                  <span className="hidden sm:block text-white font-medium text-xs">{user.nickname}</span>
                </div>

                {/* 액션 버튼들 */}
                <div className="flex items-center gap-2">
                  <LanguageSwitcher />
                  <NotificationDropdown />
                  
                  <Link 
                    to="/profile" 
                    className="p-1.5 rounded-full bg-white hover:bg-gray-100 text-gray-800 hover:text-gray-900 transition-all duration-200"
                    title="내 정보"
                  >
                    <span className="text-xs">👤</span>
                  </Link>
                  
                  {user.role === 'ADMIN' && (
                    <Link 
                      to="/admin/users" 
                      className="p-1.5 rounded-full hover:bg-red-600/20 text-red-300 hover:text-red-200 transition-all duration-200"
                      title="관리자"
                    >
                      <span className="text-xs">⚙️</span>
                    </Link>
                  )}
                  
                  <button 
                    onClick={logout}
                    className="p-1.5 rounded-full hover:bg-red-600/20 text-red-400 hover:text-red-300 transition-all duration-200"
                    title="로그아웃"
                  >
                    <span className="text-xs">🚪</span>
                  </button>
                </div>

                {/* 모바일 메뉴 버튼 */}
                <button
                  onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                  className="lg:hidden p-2 rounded-full bg-purple-600/20 hover:bg-purple-600/40 text-purple-200 transition-all duration-200"
                >
                  <span className="text-lg">{isMobileMenuOpen ? '✕' : '☰'}</span>
                </button>
              </>
            ) : (
              <div className="flex items-center gap-3">
                <LanguageSwitcher />
                <Link 
                  to="/login" 
                  className="px-4 py-2 rounded-full bg-purple-600/20 hover:bg-purple-600/40 text-purple-200 hover:text-white transition-all duration-300 border border-purple-500/30 hover:border-purple-400 font-medium text-sm"
                >
                  {t('nav.login')}
                </Link>
                <Link 
                  to="/signup" 
                  className="px-4 py-2 rounded-full bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 text-white font-medium shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-105 text-sm"
                >
                  {t('nav.signup')}
                </Link>
                
                {/* 모바일 메뉴 버튼 (비로그인) */}
                <button
                  onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                  className="lg:hidden p-2 rounded-full bg-purple-600/20 hover:bg-purple-600/40 text-purple-200 transition-all duration-200"
                >
                  <span className="text-lg">{isMobileMenuOpen ? '✕' : '☰'}</span>
                </button>
              </div>
            )}
          </div>
        </div>

        {/* 모바일 메뉴 */}
        {isMobileMenuOpen && (
          <div className="lg:hidden border-t border-purple-500/20 py-4">
            <div className="grid grid-cols-2 gap-3">
              <Link 
                to="/posts"
                onClick={() => setIsMobileMenuOpen(false)}
                className="flex flex-col items-center gap-2 p-4 rounded-xl bg-purple-600/20 hover:bg-purple-600/30 text-purple-200 transition-all duration-200"
              >
                <span className="text-2xl">📚</span>
                <span className="text-sm font-medium">{t('nav.posts')}</span>
              </Link>
              <Link 
                to="/shop"
                onClick={() => setIsMobileMenuOpen(false)}
                className="flex flex-col items-center gap-2 p-4 rounded-xl bg-gradient-to-br from-purple-600/20 to-pink-600/20 hover:from-purple-600/30 hover:to-pink-600/30 text-white transition-all duration-200"
              >
                <span className="text-2xl animate-pulse">✨</span>
                <span className="text-sm font-medium">{t('nav.shop')}</span>
              </Link>
              <Link 
                to="/suggestions"
                onClick={() => setIsMobileMenuOpen(false)}
                className="flex flex-col items-center gap-2 p-4 rounded-xl bg-orange-600/20 hover:bg-orange-600/30 text-orange-200 transition-all duration-200"
              >
                <span className="text-2xl">💡</span>
                <span className="text-sm font-medium">{t('nav.suggestions')}</span>
              </Link>
              {user && (
                <Link 
                  to="/certificates"
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="flex flex-col items-center gap-2 p-4 rounded-xl bg-yellow-600/20 hover:bg-yellow-600/30 text-yellow-200 transition-all duration-200"
                >
                  <span className="text-2xl">🏆</span>
                  <span className="text-sm font-medium">{t('nav.certificates')}</span>
                </Link>
              )}
            </div>
            
            {/* 모바일 포인트 */}
            {user && (
              <Link 
                to="/points"
                onClick={() => setIsMobileMenuOpen(false)}
                className="flex items-center justify-center gap-3 mt-3 p-4 rounded-xl bg-gradient-to-r from-yellow-900/40 to-orange-900/40 hover:from-yellow-800/50 hover:to-orange-800/50 border border-yellow-500/30 hover:border-yellow-400/50 transition-all duration-300 group"
              >
                <span className="text-yellow-400 text-xl animate-pulse group-hover:animate-bounce">✨</span>
                <div className="flex flex-col items-center">
                  <span className="text-yellow-200 font-bold">포인트: {user.points?.toLocaleString() || 0}</span>
                  <span className="text-yellow-300/70 text-sm group-hover:text-yellow-200 transition-colors">{t('nav.points')} & 내역보기</span>
                </div>
                <span className="text-yellow-300/50 text-lg group-hover:text-yellow-200 transition-colors">📊</span>
              </Link>
            )}
          </div>
        )}
      </nav>
    </header>
  );
}