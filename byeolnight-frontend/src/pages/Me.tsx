import { useState } from 'react'
import api from '../lib/axios'
import { useAuth } from '../contexts/AuthContext'

export default function MePage() {
  const { user, fetchUser } = useAuth()
  const [nickname, setNickname] = useState(user?.nickname || '')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await api.put('/users/profile', {
        nickname,
        password: password || undefined,
      })
      setMessage('프로필이 수정되었습니다.')
      setPassword('')
      fetchUser()
    } catch {
      setMessage('수정 중 오류가 발생했습니다.')
    }
  }

  return (
    <div className="max-w-md mx-auto mt-10 bg-white text-black p-6 rounded shadow">
      <h2 className="text-xl font-bold mb-4">🙋 내 정보 수정</h2>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block mb-1 text-sm font-medium">이메일</label>
          <input
            type="email"
            value={user?.email || ''}
            disabled
            className="w-full border p-2 rounded bg-gray-100"
          />
        </div>
        <div>
          <label className="block mb-1 text-sm font-medium">닉네임</label>
          <input
            type="text"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            className="w-full border p-2 rounded"
          />
        </div>
        <div>
          <label className="block mb-1 text-sm font-medium">새 비밀번호 (선택)</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="변경 시 입력"
            className="w-full border p-2 rounded"
          />
        </div>
        <button type="submit" className="w-full bg-black text-white py-2 rounded hover:bg-gray-800">
          수정하기
        </button>
        {message && <p className="text-center text-sm mt-2 text-blue-600">{message}</p>}
      </form>
    </div>
  )
}
