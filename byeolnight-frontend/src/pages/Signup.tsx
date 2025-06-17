import { useState } from 'react'
import axios from 'axios'
import { useNavigate } from 'react-router-dom'

export default function SignupPage() {
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
    passwordCheck: '',
  })
  const [error, setError] = useState('')
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
      await axios.post('/api/auth/email/verify', {
        email: form.email,
        code: form.emailCode,
      })
      alert('이메일 인증 성공')
      setForm((prev) => ({ ...prev, emailVerified: true }))
    } catch {
      alert('인증 코드가 올바르지 않습니다.')
    } finally {
      setStatus({ ...status, verifyingEmail: false })
    }
  }

  const checkNickname = async () => {
    setStatus({ ...status, checkingNickname: true })
    try {
      const res = await axios.get(`/api/users/check-nickname?value=${form.nickname}`)
      if (res.data.available) {
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
      await axios.post('/api/auth/phone/send', { phone: form.phone })
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
      await axios.post('/api/auth/phone/verify', {
        phone: form.phone,
        code: form.phoneCode,
      })
      alert('휴대폰 인증 성공')
      setForm((prev) => ({ ...prev, phoneVerified: true }))
    } catch {
      alert('인증 코드가 올바르지 않습니다.')
    } finally {
      setStatus({ ...status, verifyingPhone: false })
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!form.emailVerified) return alert('이메일 인증을 완료해주세요.')
    if (!form.nicknameChecked) return alert('닉네임 중복 확인을 해주세요.')
    if (!form.phoneVerified) return alert('휴대폰 인증을 완료해주세요.')
    if (!/^.*[!@#$%^&*()_+{}\[\]:;<>,.?~\\/-].*$/.test(form.password) || form.password.length < 8)
      return alert('비밀번호는 특수문자 포함 8자 이상이어야 합니다.')
    if (form.password !== form.passwordCheck)
      return alert('비밀번호가 일치하지 않습니다.')

    try {
      await axios.post('/api/auth/signup', {
        email: form.email,
        password: form.password,
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
    <div className="max-w-md mx-auto mt-10 bg-white p-6 rounded shadow">
      <h2 className="text-xl font-bold mb-4">🌠 별 헤는 밤 회원가입</h2>
      <form onSubmit={handleSubmit} className="space-y-4">

        {/* 이메일 인증 */}
        <div className="space-y-2">
          <input
            name="email"
            type="email"
            placeholder="이메일"
            value={form.email}
            onChange={handleChange}
            className="w-full border p-2 rounded"
            required
          />
          <div className="flex gap-2">
            <button type="button" onClick={sendEmailCode} className="bg-blue-600 text-white px-2 py-1 rounded">
              인증 코드 전송
            </button>
            <input
              name="emailCode"
              type="text"
              placeholder="인증 코드"
              value={form.emailCode}
              onChange={handleChange}
              className="flex-1 border p-2 rounded"
            />
            <button type="button" onClick={verifyEmailCode} className="bg-green-600 text-white px-2 py-1 rounded">
              인증 확인
            </button>
          </div>
        </div>

        {/* 닉네임 + 중복확인 */}
        <div className="flex gap-2">
          <input
            name="nickname"
            type="text"
            placeholder="닉네임"
            value={form.nickname}
            onChange={handleChange}
            className="flex-1 border p-2 rounded"
            required
          />
          <button type="button" onClick={checkNickname} className="bg-gray-700 text-white px-2 py-1 rounded">
            중복 확인
          </button>
        </div>

        {/* 전화번호 */}
        <input
          name="phone"
          type="tel"
          placeholder="전화번호 (숫자만)"
          value={form.phone}
          onChange={handleChange}
          className="w-full border p-2 rounded"
        />
        <div className="flex gap-2">
          <button type="button" onClick={sendPhoneCode} className="bg-blue-600 text-white px-2 py-1 rounded">
            인증 코드 전송
          </button>
          <input
            name="phoneCode"
            type="text"
            placeholder="인증 코드"
            value={form.phoneCode}
            onChange={handleChange}
            className="flex-1 border p-2 rounded"
          />
          <button type="button" onClick={verifyPhoneCode} className="bg-green-600 text-white px-2 py-1 rounded">
            인증 확인
          </button>
        </div>

        {/* 비밀번호 */}
        <input
          name="password"
          type="password"
          placeholder="비밀번호 (8자 이상, 특수문자 포함)"
          value={form.password}
          onChange={handleChange}
          className="w-full border p-2 rounded"
          required
        />
        <input
          name="passwordCheck"
          type="password"
          placeholder="비밀번호 확인"
          value={form.passwordCheck}
          onChange={handleChange}
          className="w-full border p-2 rounded"
          required
        />

        <button type="submit" className="w-full bg-black text-white py-2 rounded hover:bg-gray-800">
          가입하기
        </button>
      </form>
    </div>
  )
}
