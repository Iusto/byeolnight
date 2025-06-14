import { useEffect, useState } from "react"
import { Link, useNavigate } from "react-router-dom"
import axiosInstance from "@/lib/axiosInstance"

const Header = () => {
  const [user, setUser] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    const token = localStorage.getItem("accessToken")
    console.log("🔍 accessToken:", token)

    if (!token) return

    axiosInstance.get("/users/me")
      .then(res => {
        console.log("✅ 사용자 정보 응답:", res.data)
        setUser(res.data.data)
      })
      .catch(err => {
        console.error("⛔ 사용자 정보 조회 실패:", err)
        setUser(null)
      })
  }, [])

  const handleLogout = () => {
    localStorage.removeItem("accessToken")
    localStorage.removeItem("refreshToken")
    setUser(null)
    navigate("/login")
  }

  return (
    <header className="flex justify-between items-center p-4 bg-gray-100">
      <Link to="/" className="text-lg font-bold">별 헤는 밤</Link>
      <Link to="/board" className="px-3 py-2 text-blue-600 hover:underline">
        게시판
      </Link>
      <nav className="flex gap-4">
        {user ? (
          <>
            <span>{user.nickname}님</span>
            <Link to="/mypage">내정보</Link>
            <button onClick={handleLogout} className="text-red-500">로그아웃</button>
          </>
        ) : (
          <>
            <Link to="/login">로그인</Link>
            <Link to="/signup">회원가입</Link>
          </>
        )}
      </nav>
    </header>
  )
}

export default Header
