import { Outlet, NavLink, useLocation } from 'react-router-dom';
import { useState } from 'react';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

const navItems: NavItem[] = [
  { path: '/admin', label: '대시보드', icon: '📊' },
  { path: '/admin/users', label: '사용자 관리', icon: '👥' },
  { path: '/admin/posts', label: '게시글 관리', icon: '📝' },
  { path: '/admin/comments', label: '댓글 관리', icon: '💬' },
  { path: '/admin/ips', label: 'IP 차단', icon: '🚫' },
  { path: '/admin/files', label: '파일 정리', icon: '📁' },
  { path: '/admin/scheduler', label: '스케줄러', icon: '⏰' },
];

export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f]">
      <div className="flex">
        {/* 사이드바 */}
        <aside
          className={`${
            collapsed ? 'w-16' : 'w-56'
          } min-h-screen bg-[#1f2336]/80 backdrop-blur-md border-r border-purple-500/20 transition-all duration-300 flex flex-col`}
        >
          {/* 사이드바 헤더 */}
          <div className="p-4 border-b border-purple-500/20 flex items-center justify-between">
            {!collapsed && (
              <h2 className="text-lg font-bold text-purple-300">관리자</h2>
            )}
            <button
              onClick={() => setCollapsed(!collapsed)}
              className="p-1.5 rounded-lg hover:bg-purple-500/20 text-purple-300 transition-colors text-lg"
            >
              {collapsed ? '▶' : '◀'}
            </button>
          </div>

          {/* 네비게이션 메뉴 */}
          <nav className="flex-1 p-2 space-y-1">
            {navItems.map((item) => {
              const isActive = item.path === '/admin'
                ? location.pathname === '/admin'
                : location.pathname.startsWith(item.path) && item.path !== '/admin';

              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={`flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200 ${
                    isActive
                      ? 'bg-purple-600/40 text-purple-200 border border-purple-400/50'
                      : 'text-gray-400 hover:bg-purple-500/20 hover:text-purple-300'
                  }`}
                  title={collapsed ? item.label : undefined}
                >
                  <span className="flex-shrink-0 text-xl">{item.icon}</span>
                  {!collapsed && <span className="text-sm font-medium">{item.label}</span>}
                </NavLink>
              );
            })}
          </nav>

          {/* 레거시 페이지 링크 */}
          <div className="p-2 border-t border-purple-500/20">
            <NavLink
              to="/admin/legacy"
              className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-gray-500 hover:bg-gray-500/10 hover:text-gray-400 transition-all duration-200"
              title={collapsed ? '기존 페이지' : undefined}
            >
              <span className="flex-shrink-0 text-xl">🔙</span>
              {!collapsed && <span className="text-sm">기존 페이지</span>}
            </NavLink>
          </div>

          {/* 사이드바 푸터 */}
          {!collapsed && (
            <div className="p-4 border-t border-purple-500/20">
              <p className="text-xs text-gray-500 text-center">Admin Panel v1.0</p>
            </div>
          )}
        </aside>

        {/* 메인 콘텐츠 영역 */}
        <main className="flex-1 p-6 overflow-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}