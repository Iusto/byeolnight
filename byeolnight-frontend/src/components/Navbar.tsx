import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();

  return (
    <header className="bg-[#1f2336]/90 backdrop-blur-md shadow-md sticky top-0 z-50">
      <nav className="max-w-6xl mx-auto px-4 py-3 flex justify-between items-center">
        <Link to="/" className="text-xl font-bold text-starlight drop-shadow-glow hover:text-white">
          ✨ 별 헤는 밤
        </Link>

        <div className="flex items-center gap-4 text-sm">
          <Link to="/posts" className="hover:text-purple-300 transition">게시판</Link>
          {user ? (
            <>
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
