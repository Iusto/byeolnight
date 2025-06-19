// 예: src/pages/Home.tsx
import { useAuth } from '../hooks/useAuth'

export default function Home() {
  const { user, loading } = useAuth()

  if (loading) return <p>로딩 중...</p>
  if (!user) return <p>로그인이 필요합니다.</p>

  return (
    <div className="text-white text-center mt-10">
      <h2 className="text-xl font-bold">{user.nickname}님, 환영합니다!</h2>
      <p className="text-sm">이메일: {user.email}</p>
      <p className="text-sm">권한: {user.role}</p>
    </div>
  )
}
