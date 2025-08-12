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
          <Link to="/" className="flex items-center gap-2 text-xl font-bold text-white hover:text-purple-300 transition-colors">
            <span className="text-2xl">🌌</span>
            <span className="hidden sm:block">{t('nav.logo_full') || '별 헤는 밤'}</span>
            <span className="sm:hidden">{t('nav.logo_short') || '별헤는밤'}</span>
          </Link>

          {/* 데스크톱 네비게이션 */}
          <div className="hidden lg:flex items-center gap-1">
            <Link to="/posts" className="flex items-center gap-1 px-3 py-2 rounded-lg hover:bg-purple-600/20 text-purple-200 hover:text-white transition-colors">
              <span>📚</span>
              <span className="text-sm">{t('nav.posts')}</span>
            </Link>
            <Link to="/suggestions" className="flex items-center gap-1 px-3 py-2 rounded-lg hover:bg-orange-600/20 text-orange-300 hover:text-orange-200 transition-colors">
              <span>💡</span>
              <span className="text-sm">{t('nav.suggestions')}</span>
            </Link>
            <Link to="/shop" className="flex items-center gap-1 px-3 py-2 rounded-lg hover:bg-gradient-to-r hover:from-purple-500/30 hover:to-pink-500/30 text-white transition-colors">
              <span>✨</span>
              <span className="text-sm font-medium">{t('nav.shop')}</span>
            </Link>
            {user && (
              <Link to="/certificates" className="flex items-center gap-1 px-3 py-2 rounded-lg hover:bg-yellow-600/20 text-yellow-300 hover:text-yellow-200 transition-colors">
                <span>🏆</span>
                <span className="text-sm">{t('nav.certificates')}</span>
              </Link>
            )}
          </div>

          {/* 사용자 영역 */}
          <div className="flex items-center gap-2">
            {user ? (
              <>
                {/* 포인트 */}
                <Link to="/points" className="hidden sm:flex items-center gap-1 bg-yellow-900/30 hover:bg-yellow-800/40 px-2 py-1 rounded-lg border border-yellow-500/30 transition-colors" title="포인트">
                  <span className="text-yellow-400 text-sm">✨</span>
                  <span className="text-yellow-200 text-xs font-bold">{user.points?.toLocaleString() || 0}</span>
                </Link>

                {/* 사용자 정보 */}
                <Link to="/profile" className="flex items-center gap-1.5 bg-slate-800/60 hover:bg-slate-700/60 px-2 py-1 rounded-lg border border-purple-500/30 transition-colors group" title="프로필 & 출석체크">
                  {user.equippedIconName ? (
                    <UserIconDisplay iconName={user.equippedIconName} size="small" />
                  ) : (
                    <span className="text-gray-400 text-sm">👤</span>
                  )}
                  <div className="hidden sm:flex flex-col">
                    <span className="text-white text-xs font-medium max-w-20 truncate">{user.nickname}</span>
                    <span className="text-purple-300 text-xs opacity-70 group-hover:opacity-100 transition-opacity">출석체크</span>
                  </div>
                </Link>

                {/* 액션 버튼들 */}
                <LanguageSwitcher />
                <NotificationDropdown />
                {user.role === 'ADMIN' && (
                  <Link to="/admin/users" className="p-1 rounded hover:bg-red-600/20 text-red-300 hover:text-red-200 transition-colors" title="관리자">
                    <span className="text-sm">⚙️</span>
                  </Link>
                )}
                <button onClick={logout} className="p-1 rounded hover:bg-red-600/20 text-red-400 hover:text-red-300 transition-colors" title="로그아웃">
                  <span className="text-sm">🚪</span>
                </button>

                {/* 모바일 메뉴 버튼 */}
                <button onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)} className="lg:hidden p-2 rounded bg-purple-600/20 hover:bg-purple-600/40 text-purple-200 transition-colors">
                  <span>{isMobileMenuOpen ? '✕' : '☰'}</span>
                </button>
              </>
            ) : (
              <>
                <LanguageSwitcher />
                <Link to="/login" className="px-3 py-1.5 rounded-lg bg-purple-600/20 hover:bg-purple-600/40 text-purple-200 hover:text-white transition-colors border border-purple-500/30 text-sm">
                  {t('nav.login')}
                </Link>
                <Link to="/signup" className="px-3 py-1.5 rounded-lg bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 text-white font-medium transition-colors text-sm">
                  {t('nav.signup')}
                </Link>
                <button onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)} className="lg:hidden p-2 rounded bg-purple-600/20 hover:bg-purple-600/40 text-purple-200 transition-colors">
                  <span>{isMobileMenuOpen ? '✕' : '☰'}</span>
                </button>
              </>
            )}
          </div>
        </div>

        {/* 모바일 메뉴 */}
        {isMobileMenuOpen && (
          <div className="lg:hidden border-t border-purple-500/20 py-3">
            <div className="grid grid-cols-2 gap-2">
              <Link to="/posts" onClick={() => setIsMobileMenuOpen(false)} className="flex flex-col items-center gap-1 p-3 rounded-lg bg-purple-600/20 hover:bg-purple-600/30 text-purple-200 transition-colors">
                <span className="text-xl">📚</span>
                <span className="text-xs">{t('nav.posts')}</span>
              </Link>
              <Link to="/shop" onClick={() => setIsMobileMenuOpen(false)} className="flex flex-col items-center gap-1 p-3 rounded-lg bg-gradient-to-br from-purple-600/20 to-pink-600/20 hover:from-purple-600/30 hover:to-pink-600/30 text-white transition-colors">
                <span className="text-xl">✨</span>
                <span className="text-xs">{t('nav.shop')}</span>
              </Link>
              <Link to="/suggestions" onClick={() => setIsMobileMenuOpen(false)} className="flex flex-col items-center gap-1 p-3 rounded-lg bg-orange-600/20 hover:bg-orange-600/30 text-orange-200 transition-colors">
                <span className="text-xl">💡</span>
                <span className="text-xs">{t('nav.suggestions')}</span>
              </Link>
              {user && (
                <Link to="/certificates" onClick={() => setIsMobileMenuOpen(false)} className="flex flex-col items-center gap-1 p-3 rounded-lg bg-yellow-600/20 hover:bg-yellow-600/30 text-yellow-200 transition-colors">
                  <span className="text-xl">🏆</span>
                  <span className="text-xs">{t('nav.certificates')}</span>
                </Link>
              )}
            </div>
            {user && (
              <Link to="/points" onClick={() => setIsMobileMenuOpen(false)} className="flex items-center justify-center gap-2 mt-2 p-3 rounded-lg bg-yellow-900/30 hover:bg-yellow-800/40 border border-yellow-500/30 text-yellow-200 transition-colors">
                <span className="text-lg">✨</span>
                <span className="text-sm font-bold">포인트: {user.points?.toLocaleString() || 0}</span>
              </Link>
            )}
          </div>
        )}
      </nav>
    </header>
  );
}