import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useTranslation } from 'react-i18next'
import LanguageSwitcher from '../components/LanguageSwitcher'

console.log('🚨🚨🚨 Login.tsx 파일 로드됨!')

export default function Login() {
  try {
    console.log('🚨🚨🚨 Login 컴포넌트 시작!')
    
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
    
    console.log('🚨 상태 초기화 완료')
  

  
  // 이미 로그인된 사용자 리다이렉트
  useEffect(() => {
    if (user) {
      navigate('/', { replace: true });
    }
  }, [user, navigate]);



  const handleLogin = (e: React.FormEvent) => {
    try {
      console.log('🚨🚨🚨 handleLogin 함수 호출됨!')
      e.preventDefault()
      console.log('🚨 preventDefault 완료')
      
      console.log('🚨 로그인 데이터:', { email, password: password ? '있음' : '없음', rememberMe })
      
      setLoading(true)
      setError('')
      
      console.log('🚨 performLogin 호출 전')
      performLogin()
      console.log('🚨 performLogin 호출 후')
    } catch (err) {
      console.error('🚨 handleLogin 에러:', err)
    }
  }
  
  const performLogin = async () => {
    try {
      console.log('🔑 performLogin 시작')
      console.log('🔑 login 함수 호출 전')
      
      const result = await login(email, password, rememberMe)
      
      console.log('✅ login 함수 완료:', result)
      console.log('🍪 쿠키 확인:', document.cookie)
      
      setLoginSuccess(true)
      console.log('🏠 navigate 호출 전')
      navigate('/', { replace: true })
      console.log('🏠 navigate 호출 후')
    } catch (err: any) {
      console.error('❌ performLogin 에러:', err)
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
            onClick={() => console.log('🚨 로그인 버튼 클릭됨!')}
            className={`w-full py-2 rounded transition-colors ${
              loading || !email || !password
                ? 'bg-gray-600 cursor-not-allowed text-gray-400'
                : 'bg-purple-700 hover:bg-purple-800 text-white'
            }`}
          >
            {loading ? t('auth.login_loading') : t('auth.login_button')}
          </button>
        </form>

        {/* OAuth 로그인 구분선 */}
        <div className="flex items-center my-6">
          <div className="flex-1 border-t border-gray-600"></div>
          <span className="px-3 text-gray-400 text-sm">{t('auth.or_login_with')}</span>
          <div className="flex-1 border-t border-gray-600"></div>
        </div>

        {/* OAuth 로그인 버튼들 */}
        <div className="space-y-3">
          <button
            onClick={() => {
              const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
              const oauthUrl = baseUrl.replace('/api', '') + '/oauth2/authorization/google';
              window.location.href = oauthUrl;
            }}
            className="w-full flex items-center justify-center gap-3 py-2 px-4 bg-white text-gray-700 rounded hover:bg-gray-50 transition-colors border"
            disabled={loading}
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            Google로 로그인
          </button>

          <button
            onClick={() => {
              const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
              const oauthUrl = baseUrl.replace('/api', '') + '/oauth2/authorization/kakao';
              window.location.href = oauthUrl;
            }}
            className="w-full flex items-center justify-center gap-3 py-2 px-4 bg-[#FEE500] text-[#191919] rounded hover:bg-[#FDD835] transition-colors"
            disabled={loading}
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path fill="currentColor" d="M12 3c5.799 0 10.5 3.664 10.5 8.185 0 4.52-4.701 8.184-10.5 8.184a13.5 13.5 0 0 1-1.727-.11l-4.408 2.883c-.501.265-.678.236-.472-.413l.892-3.678c-2.88-1.46-4.785-3.99-4.785-6.866C1.5 6.665 6.201 3 12 3z"/>
            </svg>
            카카오로 로그인
          </button>

          <button
            onClick={() => {
              const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
              const oauthUrl = baseUrl.replace('/api', '') + '/oauth2/authorization/naver';
              window.location.href = oauthUrl;
            }}
            className="w-full flex items-center justify-center gap-3 py-2 px-4 bg-[#03C75A] text-white rounded hover:bg-[#02B351] transition-colors"
            disabled={loading}
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path fill="currentColor" d="M16.273 12.845 7.376 0H0v24h7.726V11.156L16.624 24H24V0h-7.727v12.845z"/>
            </svg>
            네이버로 로그인
          </button>
        </div>
        
        <div className="mt-6 text-center space-y-2">
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
  } catch (componentError) {
    console.error('🚨 Login 컴포넌트 에러:', componentError)
    return <div>로그인 컴포넌트 로드 에러</div>
  }
}
