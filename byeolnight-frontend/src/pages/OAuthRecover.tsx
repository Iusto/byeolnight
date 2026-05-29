import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import axios from '../lib/axios'
import { getErrorMessage } from '../types/api'

export default function OAuthRecover() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [isReady, setIsReady] = useState(false)

  const email = searchParams.get('email')
  const provider = searchParams.get('provider')

  useEffect(() => {
    // URL 파라미터가 없으면 로그인 페이지로 리다이렉트
    if (!email || !provider) {
      navigate('/login')
      return
    }
    setIsReady(true)
  }, [email, provider, navigate])

  const getProviderName = (provider: string) => {
    switch (provider) {
      case 'google': return 'Google'
      case 'kakao': return 'Kakao'
      case 'naver': return 'Naver'
      default: return provider
    }
  }

  const handleRecover = async () => {
    setLoading(true)
    setError('')

    try {
      const response = await axios.post('/auth/oauth/recover', {
        email,
        provider,
        recover: true
      })

      alert(response.data.message || '계정이 복구되었습니다.')
      // 복구 후 해당 소셜 로그인으로 리다이렉트
      window.location.href = `/oauth2/authorization/${provider}`
    } catch (err: unknown) {
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }



  if (!email || !provider) {
    return null
  }

  return (
    <div className="min-h-screen bg-space-gradient flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
        <div className="text-center mb-8">
          <div className="text-6xl mb-4">🌌</div>
          <h2 className="text-2xl font-bold mb-2">계정 복구</h2>
          <p className="text-gray-300 text-sm">
            {getProviderName(provider)} 계정으로 이전에 가입한 기록이 있습니다.
          </p>
        </div>

        <div className="bg-[#2a2d47] p-4 rounded-lg mb-6">
          <p className="text-sm text-gray-300 mb-2">이메일:</p>
          <p className="text-white font-medium">{email}</p>
        </div>

        {error && (
          <div className="bg-red-500/20 border border-red-500/30 rounded-lg p-3 mb-6">
            <p className="text-red-200 text-sm text-center">{error}</p>
          </div>
        )}

        <div className="space-y-4">
          <button
            onClick={handleRecover}
            disabled={loading}
            className="w-full bg-green-600 hover:bg-green-700 text-white px-6 py-3 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? '처리 중...' : '계정 복구하기'}
          </button>

          <button
            onClick={() => navigate('/login')}
            disabled={loading}
            className="w-full bg-gray-600 hover:bg-gray-700 text-white px-6 py-2 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            로그인 페이지로 돌아가기
          </button>
        </div>

        <div className="mt-6 text-xs text-gray-400 text-center">
          <p>• 30일 이내에만 복구 가능합니다</p>
          <p>• 복구 시 이전 활동 내역과 포인트가 유지됩니다</p>
        </div>
      </div>
    </div>
  )
}