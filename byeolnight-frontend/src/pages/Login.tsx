import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useTranslation } from 'react-i18next'
import LanguageSwitcher from '../components/LanguageSwitcher'

export default function Login() {
  const navigate = useNavigate()
  const { login, user } = useAuth()
  const { t, i18n, ready } = useTranslation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [loginSuccess, setLoginSuccess] = useState(false)
  const [capsLockOn, setCapsLockOn] = useState(false)
  

  
  // 이미 로그인된 사용자 리다이렉트
  useEffect(() => {
    if (user) {
      navigate('/', { replace: true });
    }
  }, [user, navigate]);

  // 에러 메시지를 사용자 친화적으로 변환하는 함수
  const getErrorMessage = (serverMessage: string): string => {
    console.log('Server error message:', serverMessage); // 디버깅용
    
    if (!serverMessage) {
      return t('auth.login_default_error')
    }
    
    // 특별한 서버 메시지 (이모지 포함) - 우선 처리
    if (serverMessage.includes('🔒') || 
        serverMessage.includes('🚫') ||
        serverMessage.includes('⚠️')) {
      return serverMessage
    }
    
    // 계정 상태 관련 에러
    if (serverMessage.includes('BANNED')) {
      return t('auth.account_banned')
    }
    if (serverMessage.includes('SUSPENDED')) {
      return t('auth.account_suspended')
    }
    if (serverMessage.includes('WITHDRAWN')) {
      return t('auth.account_withdrawn')
    }
    
    // 인증 실패 관련 에러
    if (serverMessage.includes('이메일 또는 비밀번호가 올바르지 않습니다') ||
        serverMessage.includes('Invalid credentials') || 
        serverMessage.includes('Bad credentials') ||
        serverMessage.includes('Authentication failed') ||
        serverMessage.includes('로그인 유지 옵션이 비활성화됨') ||
        /\(\d+\/\d+\)/.test(serverMessage)) {
      return t('auth.invalid_credentials')
    }
    
    // 네트워크 에러
    if (serverMessage.includes('Network Error') || 
        serverMessage.includes('ERR_NETWORK') ||
        serverMessage.includes('Failed to fetch')) {
      return t('auth.network_error')
    }
    
    // 기본적으로 서버 메시지 그대로 반환
    return serverMessage
  }

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true) // 시작 시 true
    setError('')

    try {
      await login(email, password, rememberMe)
      setLoginSuccess(true)
      
    } catch (err: any) {
      // 서버에서 보낸 구체적인 에러 메시지 추출
      const serverMessage = err.response?.data?.message || err.message || t('auth.login_default_error')
      const errorMessage = getErrorMessage(serverMessage)
      setError(errorMessage)
    } finally {
      setLoading(false) // 끝날 때 false
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={() => navigate('/')}
            className="text-gray-400 hover:text-white hover:bg-purple-600/50 px-2 py-1 rounded transition-colors"
            title={t('auth.home_tooltip')}
          >
            {t('auth.home_button')}
          </button>
          <h2 className="text-2xl font-bold">{t('auth.login_title')}</h2>
          <div className="w-16 flex justify-end">
            <select 
              value={i18n?.language || 'ko'} 
              onChange={(e) => i18n?.changeLanguage?.(e.target.value)}
              className="px-2 py-1 rounded bg-[#2a2e44] border border-gray-600 text-white text-xs cursor-pointer focus:outline-none focus:ring-2 focus:ring-purple-400/50"
              style={{ minWidth: '50px' }}
            >
              <option value="ko">🇰🇷</option>
              <option value="ja">🇯🇵</option>
              <option value="en">🇺🇸</option>
            </select>
          </div>
        </div>
        <form onSubmit={handleLogin} className="space-y-5">
          <input
            type="email"
            placeholder={t('auth.email_placeholder')}
            className="w-full bg-[#2a2e44] border border-gray-600 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-purple-500"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            disabled={loading}
          />

          <div className="relative">
            <input
              type="password"
              placeholder={t('auth.password_placeholder')}
              className="w-full bg-[#2a2e44] border border-gray-600 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-purple-500"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onKeyDown={(e) => {
                // Caps Lock 감지
                if (e.getModifierState && e.getModifierState('CapsLock')) {
                  setCapsLockOn(true)
                } else {
                  setCapsLockOn(false)
                }
              }}
              onKeyUp={(e) => {
                // Caps Lock 상태 재확인
                if (e.getModifierState && e.getModifierState('CapsLock')) {
                  setCapsLockOn(true)
                } else {
                  setCapsLockOn(false)
                }
              }}
              required
              disabled={loading}
            />
            {capsLockOn && (
              <div className="absolute -bottom-6 left-0 text-xs text-yellow-400 flex items-center gap-1">
                <span>⚠️</span>
                <span>{t('auth.caps_lock_warning')}</span>
              </div>
            )}
          </div>

          <div className="flex items-center gap-2 mt-2">
            <input
              type="checkbox"
              id="rememberMe"
              checked={rememberMe}
              onChange={(e) => setRememberMe(e.target.checked)}
              className="w-4 h-4 text-purple-600 bg-[#2a2e44] border-gray-600 rounded focus:ring-purple-500"
            />
            <label htmlFor="rememberMe" className="text-sm text-gray-300">
              {t('auth.remember_me')}
            </label>
          </div>

          {error && (
            <div className={`text-sm text-center p-3 rounded ${
              error.includes('⚠️ 경고') 
                ? 'text-yellow-300 bg-yellow-900/20 border border-yellow-600/30' 
                : error.includes('🔒') || error.includes('🚫')
                ? 'text-red-300 bg-red-900/20 border border-red-600/30'
                : 'text-red-400'
            }`}>
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading || !email || !password}
            className={`w-full py-2 rounded transition-colors ${
              loading || !email || !password
                ? 'bg-gray-600 cursor-not-allowed text-gray-400'
                : 'bg-purple-700 hover:bg-purple-800 text-white'
            }`}
          >
            {loading ? t('auth.login_loading') : t('auth.login_button')}
          </button>
        </form>
        
        <div className="mt-4 text-center space-y-2">
          <button
            onClick={() => navigate('/reset-password')}
            className="text-purple-400 hover:text-purple-300 text-sm underline"
          >
            {t('auth.forgot_password')}
          </button>
          <div className="text-gray-400 text-sm">
            {t('auth.no_account')}{' '}
            <button
              onClick={() => navigate('/signup')}
              className="text-purple-400 hover:text-purple-300 underline"
            >
              {t('auth.signup_link')}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
