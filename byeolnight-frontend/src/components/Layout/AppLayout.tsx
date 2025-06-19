import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext' //

export default function AppLayout() {
  const navigate = useNavigate()
  const { user, loading, logout } = useAuth()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-[#0b0b2a] text-white flex flex-col">
      <header className="bg-black py-4 px-6 flex justify-between items-center">
        <Link to="/" className="text-xl font-bold flex items-center gap-2">
          <img src="/vite.svg" alt="logo" className="w-6 h-6" />
          별 헤는 밤
        </Link>
        <div className="space-x-4">
          {!loading && !user && (
            <>
              <Link to="/login" className="bg-white text-black px-3 py-1 rounded hover:bg-gray-100">
                로그인
              </Link>
              <Link to="/signup" className="bg-white text-black px-3 py-1 rounded hover:bg-gray-100">
                회원가입
              </Link>
            </>
          )}
          {!loading && user && (
            <>
              <span className="text-sm text-gray-300">{user.nickname}님</span>
              <Link to="/me" className="bg-white text-black px-3 py-1 rounded hover:bg-gray-100">
                마이페이지
              </Link>
              <button onClick={handleLogout} className="bg-red-500 text-white px-3 py-1 rounded hover:bg-red-600">
                로그아웃
              </button>
            </>
          )}
        </div>
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="text-center text-sm py-4 text-gray-500">
        © 2025 별 헤는 밤 커뮤니티
      </footer>
    </div>
  )
}
