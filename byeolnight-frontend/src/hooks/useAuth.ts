// src/hooks/useAuth.ts
import { useEffect, useState } from 'react'
import api from '../lib/axios'

export interface User {
  id: number
  email: string
  nickname: string
  role: 'USER' | 'ADMIN'
}

export function useAuth() {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchMe = async () => {
      const token = localStorage.getItem('accessToken')
      if (!token) {
        setLoading(false)
        return
      }

      try {
        const res = await api.get('/users/me')
        setUser(res.data.data)
      } catch (err) {
        console.error('❌ 사용자 정보 조회 실패', err)
        setUser(null)
      } finally {
        setLoading(false)
      }
    }

    fetchMe()
  }, [])

  return { user, loading }
}
