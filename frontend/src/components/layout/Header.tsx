import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

const Header = () => {
  const { isLoggedIn, logout } = useAuth(); // ✅ 상태 훅은 컴포넌트 내부에서 호출
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header className="bg-white shadow-md px-4 py-3 flex justify-between items-center">
      <Link to="/" className="text-xl font-bold text-blue-600">별 헤는 밤</Link>
      <nav className="space-x-4">
        {isLoggedIn ? (
          <>
            <Link to="/board" className="text-gray-700 hover:text-blue-600">게시판</Link>
            <button
              onClick={handleLogout}
              className="text-red-500 hover:underline"
            >
              로그아웃
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="text-gray-700 hover:text-blue-600">로그인</Link>
            <Link to="/signup" className="text-gray-700 hover:text-blue-600">회원가입</Link>
          </>
        )}
      </nav>
    </header>
  );
};

export default Header;
