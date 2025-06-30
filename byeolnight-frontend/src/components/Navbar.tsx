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
    <header className="bg-[#1f2336]/90 backdrop-blur-md shadow-md sticky top-0 z-50">
      <nav className="max-w-6xl mx-auto px-4 py-3 flex justify-between items-center">
        <Link to="/" className="text-xl font-bold text-starlight drop-shadow-glow hover:text-white">
          ✨ 별 헤는 밤
        </Link>

        <div className="flex items-center gap-4 text-sm">
          <Link to="/posts" className="hover:text-purple-300 transition">게시판</Link>
          {user && (
            <>
              <Link to="/certificates" className="hover:text-purple-300 transition flex items-center gap-1">
                🏆 인증서
              </Link>
              <Link to="/shop" className="hover:text-purple-300 transition flex items-center gap-1">
                🌟 상점
              </Link>
            </>
          )}
          {user ? (
            <>
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-1 bg-yellow-900/30 px-2 py-1 rounded-lg border border-yellow-600/30">
                  <span className="text-yellow-400">⭐</span>
                  <span className="text-yellow-300 font-medium text-sm">{user.points || 0}</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-green-400 text-xs">•</span>
                  <div className="flex items-center gap-2">
                    {equippedIcon && (
                      <StellaIcon
                        iconUrl={equippedIcon.iconUrl}
                        animationClass={equippedIcon.animationClass}
                        grade={equippedIcon.grade}
                        size="sm"
                      />
                    )}
                    <span className="text-purple-300 font-medium">{user.nickname}</span>
                  </div>
                  {localStorage.getItem('rememberMe') === 'true' && (
                    <span className="text-xs text-gray-400 bg-gray-700 px-1 rounded">자동로그인</span>
                  )}
                </div>
              </div>
              <Link to="/me" className="hover:text-purple-300 transition">내 정보</Link>
              {user.role === 'ADMIN' && (
                <Link to="/admin/users" className="hover:text-yellow-300 font-semibold">관리자</Link>
              )}
              <button onClick={logout} className="text-red-400 hover:text-red-300 transition">
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="hover:text-purple-300 transition">로그인</Link>
              <Link to="/signup" className="hover:text-purple-300 transition">회원가입</Link>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}
