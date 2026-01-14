import { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import axios from '../lib/axios'
import { getErrorMessage } from '../types/api'

export default function Login() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [language, setLanguage] = useState('ko')
  const [rememberMe, setRememberMe] = useState(false)
  const [loginMessage, setLoginMessage] = useState('')
  const [showRecoverModal, setShowRecoverModal] = useState(false)
  const [recoverEmail, setRecoverEmail] = useState('')
  const [recoverLoading, setRecoverLoading] = useState(false)

  useEffect(() => {
    // URL에서 온 경우 적절한 메시지 표시
    const from = location.state?.from?.pathname
    if (from) {
      if (from.includes('/suggestions/new') || from.includes('/suggestions/') && from.includes('/edit')) {
        setLoginMessage('건의사항 작성 및 수정은 로그인이 필요합니다.')
      } else {
        setLoginMessage('로그인이 필요한 페이지입니다.')
      }
    }
  }, [location])


  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!email || !password) {
      return
    }

    setLoading(true)
    setError('')

    try {
      await login(email, password, rememberMe)
      navigate('/', { replace: true })
    } catch (err: unknown) {
      // 서버에서 온 실제 에러 메시지 추출
      const errorMessage = getErrorMessage(err)

      // 복구 가능한 계정인지 확인
      if (errorMessage.startsWith('RECOVERABLE_ACCOUNT:')) {
        const emailPart = errorMessage.split(':')[1]
        setRecoverEmail(emailPart || email)
        setShowRecoverModal(true)
        setError('')
      } else {
        setError(errorMessage)
      }
    } finally {
      setLoading(false)
    }
  }

  const handleRecover = async () => {
    setRecoverLoading(true)
    try {
      const response = await axios.post('/auth/oauth/recover', {
        email: recoverEmail,
        provider: null,
        recover: true
      })

      alert(response.data.message || '계정이 복구되었습니다. 다시 로그인해주세요.')
      setShowRecoverModal(false)
      setRecoverEmail('')
      // 복구 후 폼 초기화
      setEmail('')
      setPassword('')
    } catch (err: unknown) {
      alert(getErrorMessage(err))
    } finally {
      setRecoverLoading(false)
    }
  }

  const handleCancelRecover = () => {
    setShowRecoverModal(false)
    setRecoverEmail('')
  }

  // 인앱브라우저 감지 함수
  const isInAppBrowser = () => {
    const userAgent = navigator.userAgent.toLowerCase();
    return /kakaotalk|naver|line|instagram|facebook|twitter|wechat/.test(userAgent) ||
           /; wv\)|version\/[\d.]+.*mobile.*safari/.test(userAgent);
  };

  const handleSocialLogin = (provider: string) => {
    // 구글은 인앱브라우저에서 차단
    if (provider === 'google' && isInAppBrowser()) {
      alert('구글 로그인은 인앱브라우저에서 지원되지 않습니다. \n일반 브라우저에서 이용해주세요.');
      return;
    }

    // 인앱브라우저에서는 새 창 열기 시도
    if (isInAppBrowser()) {
      const authUrl = `${window.location.origin}/oauth2/authorization/${provider}`;
      const newWindow = window.open(authUrl, '_blank', 'width=500,height=600');
      
      if (!newWindow) {
        // 팝업 차단 시 직접 이동
        window.location.href = authUrl;
      } else {
        // 새 창이 열렸을 때 주기적으로 확인
        const checkClosed = setInterval(() => {
          if (newWindow.closed) {
            clearInterval(checkClosed);
            // 창이 닫히면 페이지 새로고침
            window.location.reload();
          }
        }, 1000);
      }
    } else {
      // 일반 브라우저에서는 기존 방식
      window.location.href = `/oauth2/authorization/${provider}`;
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
      rememberMe: '로그인 유지'
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
      rememberMe: 'Remember Me'
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
      rememberMe: 'ログイン状態を保持'
    }
  }

  const t = texts[language as keyof typeof texts]

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
        {/* 상단 버튼들 */}
        <div className="flex justify-between items-center mb-4">
          <button
            onClick={() => navigate('/')}
            className="px-3 py-1 bg-transparent border border-blue-500 text-blue-400 rounded hover:bg-blue-500 hover:text-white transition-colors text-sm"
          >
            {t.goHome}
          </button>
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            className="bg-[#2a2e44] border border-gray-600 rounded px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
          >
            <option value="ko">한국어</option>
            <option value="en">English</option>
            <option value="ja">日본語</option>
          </select>
        </div>

        <h2 className="text-2xl font-bold mb-6 text-center">{t.title}</h2>
        
        {loginMessage && (
          <div className="mb-4 p-3 bg-blue-500/20 border border-blue-500/30 rounded-lg text-blue-300 text-sm text-center">
            {loginMessage}
          </div>
        )}
        
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
          
          <div className="flex items-center">
            <input
              type="checkbox"
              id="rememberMe"
              checked={rememberMe}
              onChange={(e) => setRememberMe(e.target.checked)}
              className="mr-2 rounded border-gray-600 bg-[#2a2e44] text-purple-500 focus:ring-purple-500"
            />
            <label htmlFor="rememberMe" className="text-sm text-gray-300">
              {t.rememberMe}
            </label>
          </div>
          
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
              className="w-full py-2 px-4 bg-white text-gray-800 rounded hover:bg-gray-100 flex items-center justify-center space-x-2 relative"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
              <span>{t.googleLogin}</span>
              {isInAppBrowser() && (
                <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs px-1 rounded-full">⚠</span>
              )}
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
            onClick={() => navigate('/password-reset')}
            className="w-full py-2 px-4 bg-transparent border border-gray-500 text-gray-400 rounded hover:bg-gray-500 hover:text-white transition-colors"
          >
            {t.resetPassword}
          </button>
          

        </div>
      </div>

      {/* 계정 복구 모달 */}
      {showRecoverModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="w-full max-w-md bg-[#1f2336] border border-gray-600 rounded-xl shadow-2xl p-6">
            <div className="text-center mb-6">
              <div className="text-6xl mb-4">🌌</div>
              <h3 className="text-2xl font-bold mb-2">계정 복구</h3>
              <p className="text-gray-300 text-sm">
                탈퇴한 계정을 복구하시겠습니까?
              </p>
            </div>

            <div className="bg-[#2a2d47] p-4 rounded-lg mb-6">
              <p className="text-sm text-gray-300 mb-2">이메일:</p>
              <p className="text-white font-medium">{recoverEmail}</p>
            </div>

            <div className="mb-6 p-4 bg-yellow-900/20 border border-yellow-500/30 rounded-lg">
              <p className="text-yellow-300 text-sm text-center">
                • 30일 이내에만 복구 가능합니다<br />
                • 복구 시 이전 활동 내역과 포인트가 유지됩니다
              </p>
            </div>

            <div className="flex gap-3">
              <button
                onClick={handleCancelRecover}
                disabled={recoverLoading}
                className="flex-1 px-4 py-3 bg-gray-600 hover:bg-gray-700 text-white rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
              >
                취소
              </button>
              <button
                onClick={handleRecover}
                disabled={recoverLoading}
                className="flex-1 px-4 py-3 bg-green-600 hover:bg-green-700 text-white rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {recoverLoading ? '처리 중...' : '복구하기'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}