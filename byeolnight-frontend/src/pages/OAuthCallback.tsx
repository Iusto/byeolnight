import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import axios from '../lib/axios'

export default function OAuthCallback() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { setUser } = useAuth()
  const [status, setStatus] = useState<'loading' | 'success' | 'error' | 'nickname_required'>('loading')
  const [error, setError] = useState('')
  const [nickname, setNickname] = useState('')

  useEffect(() => {
    const handleOAuthCallback = async () => {
      try {
        const token = searchParams.get('token')
        const error = searchParams.get('error')
        const needsNickname = searchParams.get('needsNickname')

        if (error) {
          setError(decodeURIComponent(error))
          setStatus('error')
          return
        }

        if (needsNickname === 'true' && token) {
          // 닉네임 설정 페이지로 리다이렉트
          const userId = searchParams.get('userId')
          navigate(`/oauth/setup-nickname?userId=${userId}&token=${token}`)
          return
        }

        if (token) {
          // 토큰이 있으면 사용자 정보 조회
          const response = await axios.get('/member/users/me')
          if (response.data.success) {
            setUser(response.data.data)
            setStatus('success')
            setTimeout(() => navigate('/'), 1500)
          } else {
            throw new Error('사용자 정보 조회 실패')
          }
        } else {
          throw new Error('인증 토큰이 없습니다')
        }
      } catch (err: any) {
        console.error('OAuth 콜백 처리 오류:', err)
        setError(err.message || 'OAuth 로그인 처리 중 오류가 발생했습니다')
        setStatus('error')
      }
    }

    handleOAuthCallback()
  }, [searchParams, navigate, setUser])

  const handleNicknameSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!nickname.trim()) return

    try {
      const token = searchParams.get('token')
      const response = await axios.post('/auth/oauth/setup-nickname', {
        nickname: nickname.trim()
      }, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (response.data.success) {
        setUser(response.data.data)
        setStatus('success')
        setTimeout(() => navigate('/'), 1500)
      } else {
        setError(response.data.message || '닉네임 설정에 실패했습니다')
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '닉네임 설정 중 오류가 발생했습니다')
    }
  }

  if (status === 'loading') {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center">
        <div className="text-center text-white">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-500 mx-auto mb-4"></div>
          <p>로그인 처리 중...</p>
        </div>
      </div>
    )
  }

  if (status === 'nickname_required') {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center px-4">
        <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
          <h2 className="text-2xl font-bold text-center mb-6">닉네임 설정</h2>
          <p className="text-gray-300 text-center mb-6">
            별 헤는 밤에 오신 것을 환영합니다!<br/>
            사용하실 닉네임을 설정해주세요.
          </p>
          
          <form onSubmit={handleNicknameSubmit} className="space-y-4">
            <input
              type="text"
              placeholder="닉네임을 입력하세요"
              className="w-full bg-[#2a2e44] border border-gray-600 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-purple-500"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              maxLength={20}
              required
            />
            
            {error && (
              <div className="text-sm text-red-400 text-center">
                {error}
              </div>
            )}
            
            <button
              type="submit"
              disabled={!nickname.trim()}
              className={`w-full py-2 rounded transition-colors ${
                !nickname.trim()
                  ? 'bg-gray-600 cursor-not-allowed text-gray-400'
                  : 'bg-purple-700 hover:bg-purple-800 text-white'
              }`}
            >
              닉네임 설정 완료
            </button>
          </form>
        </div>
      </div>
    )
  }

  if (status === 'success') {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center">
        <div className="text-center text-white">
          <div className="text-green-500 text-6xl mb-4">✓</div>
          <h2 className="text-2xl font-bold mb-2">로그인 성공!</h2>
          <p className="text-gray-300">메인 페이지로 이동합니다...</p>
        </div>
      </div>
    )
  }

  if (status === 'error') {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center px-4">
        <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg text-center">
          <div className="text-red-500 text-6xl mb-4">✗</div>
          <h2 className="text-2xl font-bold mb-4">로그인 실패</h2>
          <p className="text-gray-300 mb-6">{error}</p>
          <button
            onClick={() => navigate('/login')}
            className="bg-purple-700 hover:bg-purple-800 text-white px-6 py-2 rounded transition-colors"
          >
            로그인 페이지로 돌아가기
          </button>
        </div>
      </div>
    )
  }

  return null
}