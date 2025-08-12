import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useTranslation } from 'react-i18next';
import UserIconDisplay from './UserIconDisplay';
import NotificationDropdown from './notification/NotificationDropdown';
import LanguageSwitcher from './LanguageSwitcher';
import { useState } from 'react';

const NavLink = ({ to, icon, children, className = '', onClick }: {
  to: string;
  icon: string;
  children: React.ReactNode;
  className?: string;
  onClick?: () => void;
}) => (
  <Link
    to={to}
    onClick={onClick}
    className={`flex items-center gap-2 px-3 py-2 rounded-lg transition-all duration-200 ${className}`}
  >
    <span className="text-sm">{icon}</span>
    <span className="text-sm font-medium">{children}</span>
  </Link>
);

const PointsButton = ({ points }: { points: number }) => (
  <Link
    to="/points"
    className="hidden sm:flex items-center gap-2 bg-gradient-to-r from-yellow-900/40 to-amber-900/40 hover:from-yellow-800/50 hover:to-amber-800/50 px-3 py-2 rounded-lg border border-yellow-500/40 hover:border-yellow-400/60 transition-all duration-200 shadow-lg hover:shadow-yellow-500/20"
    title="스텔라 포인트 & 출석체크"
  >
    <div className="flex items-center gap-1">
      <span className="text-yellow-400 text-sm animate-pulse">✨</span>
      <span className="text-yellow-200 text-xs font-bold">{points.toLocaleString()}</span>
    </div>
    <div className="flex items-center gap-1 px-2 py-1 bg-purple-600/30 rounded-md border border-purple-400/30">
      <span className="text-purple-300 text-xs">📅</span>
      <span className="text-purple-200 text-xs font-medium">출석체크</span>
    </div>
  </Link>
);

const UserProfile = ({ user }: { user: any }) => (
  <Link
    to="/profile"
    className="flex items-center gap-2 bg-slate-800/60 hover:bg-slate-700/60 px-3 py-2 rounded-lg border border-purple-500/30 transition-all duration-200"
    title="프로필"
  >
    <div className="w-6 h-6 flex items-center justify-center">
      {user.equippedIconName ? (
        <UserIconDisplay iconName={user.equippedIconName} size="small" className="w-5 h-5" />
      ) : (
        <span className="text-gray-400 text-sm">👤</span>
      )}
    </div>
    <span className="hidden sm:block text-white text-sm font-medium max-w-20 truncate">
      {user.nickname}
    </span>
  </Link>
);

const ActionButton = ({ onClick, icon, title, className = '' }: {
  onClick?: () => void;
  icon: string;
  title: string;
  className?: string;
}) => (
  <button
    onClick={onClick}
    className={`p-2 rounded-lg transition-all duration-200 ${className}`}
    title={title}
  >
    <span className="text-sm">{icon}</span>
  </button>
);

export default function Navbar() {
  const { user, logout } = useAuth();
  const { t } = useTranslation();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const navItems = [
    { to: '/posts', icon: '📚', label: t('nav.posts'), className: 'hover:bg-purple-600/20 text-purple-200 hover:text-white' },
    { to: '/suggestions', icon: '💡', label: t('nav.suggestions'), className: 'hover:bg-orange-600/20 text-orange-300 hover:text-orange-200' },
    { to: '/shop', icon: '✨', label: t('nav.shop'), className: 'hover:bg-gradient-to-r hover:from-purple-500/30 hover:to-pink-500/30 text-white' },
    ...(user ? [{ to: '/certificates', icon: '🏆', label: t('nav.certificates'), className: 'hover:bg-yellow-600/20 text-yellow-300 hover:text-yellow-200' }] : [])
  ];

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
            {navItems.map((item) => (
              <NavLink key={item.to} to={item.to} icon={item.icon} className={item.className}>
                {item.label}
              </NavLink>
            ))}
          </div>

          {/* 사용자 영역 */}
          <div className="flex items-center gap-2">
            {user ? (
              <>
                <PointsButton points={user.points || 0} />
                <UserProfile user={user} />
                <LanguageSwitcher />
                <NotificationDropdown />
                {user.role === 'ADMIN' && (
                  <ActionButton
                    icon="⚙️"
                    title="관리자"
                    className="hover:bg-red-600/20 text-red-300 hover:text-red-200"
                    onClick={() => window.location.href = '/admin/users'}
                  />
                )}
                <ActionButton
                  onClick={logout}
                  icon="🚪"
                  title="로그아웃"
                  className="hover:bg-red-600/20 text-red-400 hover:text-red-300"
                />
                <ActionButton
                  onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                  icon={isMobileMenuOpen ? '✕' : '☰'}
                  title="메뉴"
                  className="lg:hidden bg-purple-600/20 hover:bg-purple-600/40 text-purple-200"
                />
              </>
            ) : (
              <>
                <LanguageSwitcher />
                <Link to="/login" className="px-3 py-2 rounded-lg bg-purple-600/20 hover:bg-purple-600/40 text-purple-200 hover:text-white transition-all duration-200 border border-purple-500/30 text-sm">
                  {t('nav.login')}
                </Link>
                <Link to="/signup" className="px-3 py-2 rounded-lg bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 text-white font-medium transition-all duration-200 text-sm">
                  {t('nav.signup')}
                </Link>
                <ActionButton
                  onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                  icon={isMobileMenuOpen ? '✕' : '☰'}
                  title="메뉴"
                  className="lg:hidden bg-purple-600/20 hover:bg-purple-600/40 text-purple-200"
                />
              </>
            )}
          </div>
        </div>

        {/* 모바일 메뉴 */}
        {isMobileMenuOpen && (
          <div className="lg:hidden border-t border-purple-500/20 py-4">
            <div className="grid grid-cols-2 gap-3">
              {navItems.map((item) => (
                <Link
                  key={item.to}
                  to={item.to}
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="flex flex-col items-center gap-2 p-4 rounded-lg bg-slate-800/40 hover:bg-slate-700/60 text-white transition-all duration-200 border border-slate-600/30"
                >
                  <span className="text-xl">{item.icon}</span>
                  <span className="text-xs font-medium">{item.label}</span>
                </Link>
              ))}
            </div>
            {user && (
              <Link
                to="/points"
                onClick={() => setIsMobileMenuOpen(false)}
                className="flex items-center justify-between mt-3 p-4 rounded-lg bg-gradient-to-r from-yellow-900/40 to-amber-900/40 hover:from-yellow-800/50 hover:to-amber-800/50 border border-yellow-500/40 text-yellow-200 transition-all duration-200 shadow-lg"
              >
                <div className="flex items-center gap-2">
                  <span className="text-lg animate-pulse">✨</span>
                  <span className="text-sm font-bold">포인트: {user.points?.toLocaleString() || 0}</span>
                </div>
                <div className="flex items-center gap-1 px-2 py-1 bg-purple-600/30 rounded border border-purple-400/30">
                  <span className="text-xs">📅</span>
                  <span className="text-xs font-medium text-purple-200">출석체크</span>
                </div>
              </Link>
            )}
          </div>
        )}
      </nav>
    </header>
  );
}