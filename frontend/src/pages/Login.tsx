import { useState } from 'react'
import axiosInstance from '@/lib/axiosInstance'
import { useNavigate } from 'react-router-dom'

const Login = () => {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const navigate = useNavigate()

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const res = await axiosInstance.post('/auth/login', { email, password })

      const { accessToken, refreshToken } = res.data.data
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)

      console.log('✅ 로그인 성공: 토큰 저장 완료')

      // ✅ 사용자 정보 요청
      const userRes = await axiosInstance.get('/users/me') // or '/user/me'
      console.log('👤 사용자 정보:', userRes.data)

      window.location.href = '/board'
    } catch (err: any) {
      alert(err.response?.data?.message || '로그인 실패')
    }
  }

  return (
    <div className="flex justify-center items-center min-h-screen bg-gray-50">
      <div className="w-full max-w-md bg-white p-6 rounded shadow">
        <h2 className="text-2xl font-bold mb-6 text-center">로그인</h2>
        <form onSubmit={handleLogin}>
          <input
            type="email"
            placeholder="이메일"
            className="w-full mb-4 p-2 border rounded"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <input
            type="password"
            placeholder="비밀번호"
            className="w-full mb-4 p-2 border rounded"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <button
            type="submit"
            className="w-full bg-blue-500 text-white py-2 rounded hover:bg-blue-600"
          >
            로그인
          </button>
        </form>
      </div>
    </div>
  )
}

export default Login
