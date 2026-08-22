import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import axios from '../lib/axios'
import { getErrorMessage } from '../types/api'

export default function OAuthRecover() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { refreshUserInfo } = useAuth()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const ticket = searchParams.get('ticket')

  useEffect(() => {
    if (!ticket) {
      navigate('/login', { replace: true })
    }
  }, [ticket, navigate])

  const handleRecover = async () => {
    if (!ticket) return

    setLoading(true)
    setError('')

    try {
      await axios.post('/auth/account/recover', { ticket })
      await refreshUserInfo()
      navigate('/', { replace: true })
    } catch (err: unknown) {
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  if (!ticket) return null

  return (
    <div className="min-h-screen bg-space-gradient flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
        <div className="text-center mb-8">
          <div className="text-6xl mb-4">🌌</div>
          <h2 className="text-2xl font-bold mb-2">계정 복구</h2>
          <p className="text-gray-300 text-sm">
            인증된 외부 계정과 연결된 탈퇴 계정이 확인되었습니다.
          </p>
        </div>

        <div className="mb-6 p-4 bg-yellow-900/20 border border-yellow-500/30 rounded-lg">
          <p className="text-yellow-300 text-sm text-center">
            복구하면 이전 활동 기록과 보유 항목을 다시 사용할 수 있습니다.
          </p>
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
            {loading ? '복구 중...' : '계정 복구하기'}
          </button>

          <button
            onClick={() => navigate('/login', { replace: true })}
            disabled={loading}
            className="w-full bg-gray-600 hover:bg-gray-700 text-white px-6 py-2 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            취소
          </button>
        </div>
      </div>
    </div>
  )
}
