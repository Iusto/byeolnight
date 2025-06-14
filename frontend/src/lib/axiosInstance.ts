import axios from 'axios'

// ✅ Axios 인스턴스 생성
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // .env에서 API 주소 로딩
  headers: {
    'Content-Type': 'application/json',
  },
  // withCredentials: true, // ❌ JWT만 사용하는 경우엔 필요 없음 (쿠키 기반일 때만 사용)
})

// ✅ 요청 인터셉터 - accessToken 삽입 (단, public 경로 제외)
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')

    // ✅ public API 경로에는 토큰 삽입 금지
    if (config.url?.startsWith('/public')) {
      delete config.headers['Authorization']
    } else if (token) {
      config.headers['Authorization'] = `Bearer ${token}` // 안전하게 삽입
    }

    return config
  },
  (error) => {
    console.error('요청 인터셉터 에러:', error)
    return Promise.reject(error)
  }
)

// ✅ 응답 인터셉터 - 에러 처리 통일
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      // 공통 에러 처리 로직
      switch (status) {
        case 401:
          console.warn('⛔ 인증 실패: 로그인 필요')
          localStorage.removeItem('accessToken')
          window.location.href = '/login'
          break
        case 403:
          console.warn('⛔ 권한 없음')
          alert('권한이 없습니다.')
          break
        case 500:
          console.error('💥 서버 오류 발생:', data.message || '알 수 없는 오류')
          break
        default:
          console.error('❌ 에러 응답:', data)
          break
      }
    } else {
      // 네트워크 또는 설정 오류
      console.error('🚫 요청 실패:', error.message)
    }
    return Promise.reject(error)
  }
)

console.log('✅ BASE_URL:', import.meta.env.VITE_API_BASE_URL)

export default axiosInstance
