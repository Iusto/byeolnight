import { createContext, useContext, useEffect, useState } from 'react'
import axios from '../lib/axios'
import { useNavigate } from 'react-router-dom'

interface User {
  id: number
  email: string
  nickname: string
  level: number
  exp: number
}

interface AuthContextType {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  const fetchMyInfo = async () => {
    try {
      const token = localStorage.getItem('accessToken')
      const res = await axios.get('/users/me', {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })
      setUser(res.data.data) // ✅ 응답 구조 반영
    } catch (err) {
      console.error('사용자 정보 조회 실패:', err)
      setUser(null)
    } finally {
      setLoading(false)
    }
  }

  const login = async (email: string, password: string) => {
    try {
      const res = await axios.post(
        '/auth/login',
        {
          email: email.trim(),
          password: password.trim(),
        },
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      )

      console.log('로그인 응답:', res.data)

      const accessToken = res.data?.data?.accessToken
      const refreshToken = res.data?.data?.refreshToken

      if (!accessToken) {
        throw new Error('accessToken이 응답에 없습니다')
      }

      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)

      axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`

      await fetchMyInfo()
      navigate('/')
    } catch (err) {
      console.error('로그인 실패:', err)
      throw err
    }
  }

  const logout = async () => {
    try {
      const token = localStorage.getItem('accessToken')
      if (token) {
        await axios.post('/auth/logout', null, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        })
      }
    } catch (err) {
      console.error('로그아웃 요청 실패:', err)
    } finally {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      setUser(null)
      navigate('/login')
    }
  }

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
    }
    fetchMyInfo()
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
