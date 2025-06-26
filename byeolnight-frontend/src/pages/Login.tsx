import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function Login() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false) // ✅ 로딩 상태 추가

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true) // 시작 시 true
    setError('')

    try {
      await login(email, password)
      // 로그인 성공 시 navigate가 내부에 포함되어 있으면 생략 가능
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.')
    } finally {
      setLoading(false) // 끝날 때 false
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
        <h2 className="text-2xl font-bold mb-6 text-center">🔐 로그인</h2>
        <form onSubmit={handleLogin} className="space-y-5">
          <input
            type="email"
            placeholder="이메일"
            className="w-full bg-[#2a2e44] border border-gray-600 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-purple-500"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            disabled={loading}
          />

          <input
            type="password"
            placeholder="비밀번호"
            className="w-full bg-[#2a2e44] border border-gray-600 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-purple-500"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            disabled={loading}
          />

          {error && <p className="text-red-400 text-sm text-center">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className={`w-full py-2 rounded transition-colors ${
              loading
                ? 'bg-gray-600 cursor-not-allowed'
                : 'bg-purple-700 hover:bg-purple-800'
            }`}
          >
            {loading ? '🌠 로그인 중...' : '🌌 로그인'}
          </button>
        </form>
      </div>
    </div>
  )
}
