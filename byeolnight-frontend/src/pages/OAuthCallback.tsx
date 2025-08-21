import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import axios from '../lib/axios'

export default function OAuthCallback() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { refreshUserInfo } = useAuth()
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading')
  const [error, setError] = useState('')

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



        if (token) {
          // OAuth 로그인 성공 - HttpOnly 쿠키로 토큰이 자동 저장됨
          // AuthContext에서 자동으로 사용자 정보를 로드하므로 즉시 리다이렉트
          setStatus('success');
          setTimeout(() => navigate('/'), 800);
        } else {
          // 토큰이 없어도 HttpOnly 쿠키로 인증될 수 있음
          setStatus('success');
          setTimeout(() => navigate('/'), 800);
        }
      } catch (err: any) {
        console.error('OAuth 콜백 처리 오류:', err)
        setError(err.message || 'OAuth 로그인 처리 중 오류가 발생했습니다')
        setStatus('error')
      }
    }

    handleOAuthCallback()
  }, [searchParams, navigate, refreshUserInfo])



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