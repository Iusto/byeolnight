import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

// 즉시 실행되는 로그
console.log('🚨🚨🚨 NEW Login.tsx 파일 로드됨!')

export default function Login() {
  console.log('🚨🚨🚨 NEW Login 컴포넌트 실행됨!')
  
  const navigate = useNavigate()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    console.log('🚨🚨🚨 handleSubmit 호출됨!')
    e.preventDefault()
    
    if (!email || !password) {
      console.log('🚨 이메일 또는 비밀번호 없음')
      return
    }
    
    setLoading(true)
    setError('')
    
    try {
      console.log('🚨 login 함수 호출 시작')
      await login(email, password, false)
      console.log('🚨 login 함수 완료')
      console.log('🍪 쿠키:', document.cookie)
      navigate('/', { replace: true })
    } catch (err: any) {
      console.error('🚨 로그인 에러:', err)
      setError(err.message || '로그인 실패')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
        <h2 className="text-2xl font-bold mb-6 text-center">로그인</h2>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <input
            type="email"
            placeholder="이메일"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full bg-[#2a2e44] border border-gray-600 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-purple-500"
            required
          />
          
          <input
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full bg-[#2a2e44] border border-gray-600 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-purple-500"
            required
          />
          
          {error && (
            <div className="text-red-400 text-sm text-center">{error}</div>
          )}
          
          <button
            type="submit"
            disabled={loading}
            onClick={() => console.log('🚨 버튼 클릭됨!')}
            className="w-full py-2 rounded bg-purple-700 hover:bg-purple-800 text-white disabled:bg-gray-600"
          >
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>
      </div>
    </div>
  )
}