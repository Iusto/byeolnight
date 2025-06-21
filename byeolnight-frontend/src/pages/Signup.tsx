import { useState } from 'react'
import axios from 'axios'
import { useNavigate } from 'react-router-dom'

export default function Signup() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    email: '',
    emailCode: '',
    emailVerified: false,
    nickname: '',
    nicknameChecked: false,
    phone: '',
    phoneCode: '',
    phoneVerified: false,
    password: '',
    confirmPassword: '',
  })
  const [status, setStatus] = useState({
    sendingEmail: false,
    verifyingEmail: false,
    checkingNickname: false,
    sendingPhone: false,
    verifyingPhone: false,
  })

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const sendEmailCode = async () => {
    setStatus({ ...status, sendingEmail: true })
    try {
      await axios.post('/api/auth/email/send', { email: form.email })
      alert('이메일로 인증 코드를 보냈습니다.')
    } catch {
      alert('이메일 발송 실패')
    } finally {
      setStatus({ ...status, sendingEmail: false })
    }
  }

  const verifyEmailCode = async () => {
    setStatus({ ...status, verifyingEmail: true })
    try {
      const { data: isValid } = await axios.post('/api/auth/email/verify', {
        email: form.email,
        code: form.emailCode,
      })

      if (isValid) {
        alert('이메일 인증 성공')
        setForm((prev) => ({ ...prev, emailVerified: true }))
      } else {
        alert('인증 코드가 올바르지 않습니다.')
      }
    } catch {
      alert('서버 오류로 인증 실패')
    } finally {
      setStatus({ ...status, verifyingEmail: false })
    }
  }

  const checkNickname = async () => {
    setStatus({ ...status, checkingNickname: true })
    try {
      const { data: isDuplicated } = await axios.get(`/api/auth/check-nickname?value=${form.nickname}`)
      if (!isDuplicated) {
        alert('사용 가능한 닉네임입니다.')
        setForm((prev) => ({ ...prev, nicknameChecked: true }))
      } else {
        alert('이미 사용 중인 닉네임입니다.')
      }
    } catch {
      alert('닉네임 중복 확인 실패')
    } finally {
      setStatus({ ...status, checkingNickname: false })
    }
  }

  const sendPhoneCode = async () => {
    setStatus({ ...status, sendingPhone: true })
    try {
      const cleanedPhone = form.phone.replace(/[^0-9]/g, '')
      if (!/^\d{10,11}$/.test(cleanedPhone)) {
        alert('전화번호는 숫자만 10~11자리여야 합니다.')
        setStatus({ ...status, sendingPhone: false })
        return
      }

      await axios.post('/api/auth/phone/send', { phone: cleanedPhone })
      alert('휴대폰으로 인증 코드를 보냈습니다.')
    } catch {
      alert('휴대폰 인증 코드 발송 실패')
    } finally {
      setStatus({ ...status, sendingPhone: false })
    }
  }

  const verifyPhoneCode = async () => {
    setStatus({ ...status, verifyingPhone: true })
    try {
      const cleanedPhone = form.phone.replace(/[^0-9]/g, '')
      const res = await axios.post('/api/auth/phone/verify', {
        phone: cleanedPhone,
        code: form.phoneCode,
      })

      if (res.data.success) {
        alert(res.data.message)
        setForm((prev) => ({ ...prev, phoneVerified: true }))
      } else {
        alert(res.data.message)
      }
    } catch {
      alert('휴대폰 인증 요청 실패')
    } finally {
      setStatus({ ...status, verifyingPhone: false })
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!form.email || !form.emailCode || !form.emailVerified) return alert('이메일 인증을 완료해주세요.')
    if (!form.nickname || !form.nicknameChecked) return alert('닉네임 확인이 필요합니다.')
    if (!form.phone || !form.phoneCode || !form.phoneVerified) return alert('휴대폰 인증을 완료해주세요.')
    if (!form.password || !/^.*[!@#$%^&*()_+{}\[\]:;<>,.?~\\/-].*$/.test(form.password) || form.password.length < 8)
      return alert('비밀번호는 특수문자 포함 8자 이상이어야 합니다.')
    if (form.password !== form.confirmPassword) return alert('비밀번호가 일치하지 않습니다.')

    try {
      await axios.post('/api/auth/signup', {
        email: form.email,
        password: form.password,
        confirmPassword: form.confirmPassword,
        nickname: form.nickname,
        phone: form.phone,
      })
      alert('회원가입 완료! 로그인 페이지로 이동합니다.')
      navigate('/login')
    } catch {
      alert('회원가입 실패')
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center px-4">
      <div className="w-full max-w-lg bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
        <h2 className="text-2xl font-bold mb-6 text-center">🌠 별 헤는 밤 회원가입</h2>
        <form onSubmit={handleSubmit} className="space-y-5">

          {/* 이메일 인증 */}
          <div>
            <input name="email" type="email" placeholder="이메일" value={form.email} onChange={handleChange}
              className="w-full bg-[#2a2e44] border border-gray-600 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <div className="flex gap-2 mt-2">
              <button type="button" onClick={sendEmailCode} className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded">
                인증 코드 전송
              </button>
              <input name="emailCode" placeholder="인증 코드" value={form.emailCode} onChange={handleChange}
                className="flex-1 bg-[#2a2e44] border border-gray-600 rounded px-2 py-1"
              />
              <button type="button" onClick={verifyEmailCode} className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded">
                인증 확인
              </button>
            </div>
          </div>

          {/* 닉네임 */}
          <div className="flex gap-2">
            <input name="nickname" placeholder="닉네임" value={form.nickname} onChange={handleChange}
              className="flex-1 bg-[#2a2e44] border border-gray-600 rounded px-3 py-2"
            />
            <button type="button" onClick={checkNickname} className="bg-gray-700 hover:bg-gray-600 text-white px-3 py-1 rounded">
              중복 확인
            </button>
          </div>

          {/* 전화번호 */}
          <div>
            <input name="phone" placeholder="전화번호 (숫자만)" value={form.phone} onChange={handleChange}
              className="w-full bg-[#2a2e44] border border-gray-600 rounded px-3 py-2"
            />
            <div className="flex gap-2 mt-2">
              <button type="button" onClick={sendPhoneCode} className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded">
                인증 코드 전송
              </button>
              <input name="phoneCode" placeholder="인증 코드" value={form.phoneCode} onChange={handleChange}
                className="flex-1 bg-[#2a2e44] border border-gray-600 rounded px-2 py-1"
              />
              <button type="button" onClick={verifyPhoneCode} className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded">
                인증 확인
              </button>
            </div>
          </div>

          {/* 비밀번호 */}
          <div>
            <input name="password" type="password" placeholder="비밀번호 (8자 이상, 특수문자 포함)" value={form.password} onChange={handleChange}
              className="w-full bg-[#2a2e44] border border-gray-600 rounded px-3 py-2"
            />
            <input name="confirmPassword" type="password" placeholder="비밀번호 확인" value={form.confirmPassword} onChange={handleChange}
              className="w-full bg-[#2a2e44] border border-gray-600 rounded px-3 py-2 mt-2"
            />
          </div>

          <button type="submit" className="w-full bg-purple-700 hover:bg-purple-800 text-white py-2 rounded">
            🌌 가입하기
          </button>
        </form>
      </div>
    </div>
  )
}
