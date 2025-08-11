import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function Login() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [language, setLanguage] = useState('ko')
  const [showSocialRecovery, setShowSocialRecovery] = useState(false)
  const [recoveryEmail, setRecoveryEmail] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!email || !password) {
      return
    }
    
    setLoading(true)
    setError('')
    
    try {
      await login(email, password, false)
      navigate('/', { replace: true })
    } catch (err: any) {
      setError(err.message || '로그인 실패')
    } finally {
      setLoading(false)
    }
  }

  const handleSocialLogin = (provider: string) => {
    window.location.href = `/oauth2/authorization/${provider}`
  }

  const handleSocialRecovery = async () => {
    if (!recoveryEmail) {
      alert('이메일을 입력해주세요.')
      return
    }
    
    try {
      const response = await fetch('/api/auth/social/recover', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: recoveryEmail })
      })
      
      const data = await response.json()
      if (data.success) {
        alert('계정이 복구되었습니다. 다시 로그인해주세요.')
        setShowSocialRecovery(false)
        setRecoveryEmail('')
      } else {
        alert(data.message || '복구 실패')
      }
    } catch (err) {
      alert('복구 요청 중 오류가 발생했습니다.')
    }
  }

  const texts = {
    ko: {
      title: '로그인',
      email: '이메일',
      password: '비밀번호',
      loginButton: '로그인',
      loggingIn: '로그인 중...',
      socialLogin: '소셜 로그인',
      googleLogin: 'Google로 로그인',
      kakaoLogin: 'Kakao로 로그인',
      naverLogin: 'Naver로 로그인',
      signup: '회원가입',
      resetPassword: '비밀번호 재설정',
      goHome: '홈페이지로',
      socialRecovery: '소셜 계정 복구',
      socialRecoveryDesc: '소셜 로그인이 안 되시나요? 계정 복구를 요청하세요.',
      recoveryEmail: '복구할 이메일',
      recover: '복구 요청',
      cancel: '취소'
    },
    en: {
      title: 'Login',
      email: 'Email',
      password: 'Password',
      loginButton: 'Login',
      loggingIn: 'Logging in...',
      socialLogin: 'Social Login',
      googleLogin: 'Login with Google',
      kakaoLogin: 'Login with Kakao',
      naverLogin: 'Login with Naver',
      signup: 'Sign Up',
      resetPassword: 'Reset Password',
      goHome: 'Go Home',
      socialRecovery: 'Social Account Recovery',
      socialRecoveryDesc: 'Having trouble with social login? Request account recovery.',
      recoveryEmail: 'Recovery Email',
      recover: 'Request Recovery',
      cancel: 'Cancel'
    },
    ja: {
      title: 'ログイン',
      email: 'メール',
      password: 'パスワード',
      loginButton: 'ログイン',
      loggingIn: 'ログイン中...',
      socialLogin: 'ソーシャルログイン',
      googleLogin: 'Googleでログイン',
      kakaoLogin: 'Kakaoでログイン',
      naverLogin: 'Naverでログイン',
      signup: 'ユーザー登録',
      resetPassword: 'パスワードリセット',
      goHome: 'ホームへ',
      socialRecovery: 'ソーシャルアカウント復旧',
      socialRecoveryDesc: 'ソーシャルログインに問題がありますか？アカウント復旧をリクエストしてください。',
      recoveryEmail: '復旧メール',
      recover: '復旧リクエスト',
      cancel: 'キャンセル'
    }
  }

  const t = texts[language as keyof typeof texts]

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
        {/* 언어 선택 */}
        <div className="flex justify-end mb-4">
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            className="bg-[#2a2e44] border border-gray-600 rounded px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
          >
            <option value="ko">한국어</option>
            <option value="en">English</option>
            <option value="ja">日本語</option>
          </select>
        </div>

        <h2 className="text-2xl font-bold mb-6 text-center">{t.title}</h2>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <input
            type="email"
            placeholder={t.email}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full bg-[#2a2e44] border border-gray-600 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-purple-500"
            required
          />
          
          <input
            type="password"
            placeholder={t.password}
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
            className="w-full py-2 rounded bg-purple-700 hover:bg-purple-800 text-white disabled:bg-gray-600"
          >
            {loading ? t.loggingIn : t.loginButton}
          </button>
        </form>

        {/* 소셜 로그인 */}
        <div className="mt-6">
          <div className="text-center text-gray-400 text-sm mb-4">{t.socialLogin}</div>
          
          <div className="space-y-2">
            <button
              onClick={() => handleSocialLogin('google')}
              className="w-full py-2 px-4 bg-white text-gray-800 rounded hover:bg-gray-100 flex items-center justify-center space-x-2"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
              <span>{t.googleLogin}</span>
            </button>

            <button
              onClick={() => handleSocialLogin('kakao')}
              className="w-full py-2 px-4 bg-[#FEE500] text-black rounded hover:bg-[#FDD835] flex items-center justify-center space-x-2"
            >
              <div className="w-5 h-5 bg-black rounded-full flex items-center justify-center">
                <span className="text-[#FEE500] text-xs font-bold">K</span>
              </div>
              <span>{t.kakaoLogin}</span>
            </button>

            <button
              onClick={() => handleSocialLogin('naver')}
              className="w-full py-2 px-4 bg-[#03C75A] text-white rounded hover:bg-[#02B351] flex items-center justify-center space-x-2"
            >
              <div className="w-5 h-5 bg-white rounded flex items-center justify-center">
                <span className="text-[#03C75A] text-xs font-bold">N</span>
              </div>
              <span>{t.naverLogin}</span>
            </button>
          </div>
        </div>

        {/* 추가 버튼들 */}
        <div className="mt-6 space-y-2">
          <button
            onClick={() => navigate('/signup')}
            className="w-full py-2 px-4 bg-transparent border border-purple-500 text-purple-400 rounded hover:bg-purple-500 hover:text-white transition-colors"
          >
            {t.signup}
          </button>
          
          <button
            onClick={() => navigate('/reset-password')}
            className="w-full py-2 px-4 bg-transparent border border-gray-500 text-gray-400 rounded hover:bg-gray-500 hover:text-white transition-colors"
          >
            {t.resetPassword}
          </button>
          
          <button
            onClick={() => navigate('/')}
            className="w-full py-2 px-4 bg-transparent border border-blue-500 text-blue-400 rounded hover:bg-blue-500 hover:text-white transition-colors"
          >
            {t.goHome}
          </button>
          
          <button
            onClick={() => setShowSocialRecovery(true)}
            className="w-full py-2 px-4 bg-transparent border border-orange-500 text-orange-400 rounded hover:bg-orange-500 hover:text-white transition-colors text-sm"
          >
            {t.socialRecovery}
          </button>
        </div>
      </div>
      
      {/* 소셜 계정 복구 모달 */}
      {showSocialRecovery && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-[#1f2336] p-6 rounded-xl max-w-md w-full mx-4">
            <h3 className="text-lg font-bold text-white mb-4">{t.socialRecovery}</h3>
            <p className="text-gray-300 text-sm mb-4">{t.socialRecoveryDesc}</p>
            
            <input
              type="email"
              placeholder={t.recoveryEmail}
              value={recoveryEmail}
              onChange={(e) => setRecoveryEmail(e.target.value)}
              className="w-full bg-[#2a2e44] border border-gray-600 rounded px-3 py-2 mb-4 text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
            />
            
            <div className="flex space-x-3">
              <button
                onClick={() => {
                  setShowSocialRecovery(false)
                  setRecoveryEmail('')
                }}
                className="flex-1 py-2 bg-gray-600 hover:bg-gray-700 text-white rounded transition-colors"
              >
                {t.cancel}
              </button>
              <button
                onClick={handleSocialRecovery}
                className="flex-1 py-2 bg-orange-600 hover:bg-orange-700 text-white rounded transition-colors"
              >
                {t.recover}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}