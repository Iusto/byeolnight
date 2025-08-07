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



  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      await login(email, password, rememberMe)
      setLoginSuccess(true)
    } catch (err: any) {
      let errorMessage = t('auth.login_default_error')
      
      const serverMessage = err.response?.data?.message || err.message
      
      if (serverMessage) {
        // 존재하지 않는 아이디
        if (serverMessage.includes('존재하지 않는 아이디')) {
          errorMessage = t('auth.user_not_found')
        }
        // 비밀번호 오류
        else if (serverMessage.includes('비밀번호가 올바르지 않습니다')) {
          errorMessage = serverMessage // 시도 횟수 포함해서 그대로 사용
        }
        // 이모지 포함 메시지는 그대로 사용
        else if (serverMessage.includes('🔒') || serverMessage.includes('🚫') || serverMessage.includes('⚠️')) {
          errorMessage = serverMessage
        }
        // 기타 서버 메시지
        else {
          errorMessage = serverMessage
        }
      }
      
      setError(errorMessage)
    } finally {
      setLoading(false)
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
            <div className="text-sm text-center p-3 rounded text-red-400">
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
