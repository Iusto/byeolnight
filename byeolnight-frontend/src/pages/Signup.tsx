import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import AgreementModal from '../components/AgreementModal';
import TermsOfService from '../components/TermsOfService';
import PrivacyPolicy from '../components/PrivacyPolicy';

export default function Signup() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    email: '',
    emailCode: '',
    nickname: '',
    phone: '',
    phoneCode: '',
    password: '',
    confirmPassword: '',
  });

  const [error, setError] = useState('');
  const [emailVerified, setEmailVerified] = useState(false);
  const [phoneVerified, setPhoneVerified] = useState(false);
  const [nicknameChecked, setNicknameChecked] = useState(false);
  const [termsAgreed, setTermsAgreed] = useState(false);
  const [privacyAgreed, setPrivacyAgreed] = useState(false);
  const [showModal, setShowModal] = useState<'terms' | 'privacy' | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!termsAgreed || !privacyAgreed) {
      setError('이용약관 및 개인정보 처리방침에 모두 동의해야 합니다.');
      return;
    }
    if (form.password !== form.confirmPassword) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }
    if (!emailVerified) {
      setError('이메일 인증을 완료해주세요.');
      return;
    }
    if (!phoneVerified) {
      setError('전화번호 인증을 완료해주세요.');
      return;
    }
    if (!nicknameChecked) {
      setError('닉네임 중복 확인을 해주세요.');
      return;
    }

    try {
      await axios.post('/auth/signup', {
        ...form,
        agree: true,
      });
      alert('회원가입이 완료되었습니다.');
      navigate('/login');
    } catch (err: any) {
      const message = err?.response?.data?.message || '회원가입 실패';
      setError(message);
    }
  };

  const sendEmailCode = async () => {
    await axios.post('/auth/email/send', { email: form.email });
    alert('이메일 인증 코드가 전송되었습니다.');
  };

  const verifyEmailCode = async () => {
    const res = await axios.post('/auth/email/verify', {
      email: form.email,
      code: form.emailCode,
    });
    if (res.data === true) {
      setEmailVerified(true);
      alert('이메일 인증 성공');
    } else {
      setError('이메일 인증 코드가 유효하지 않습니다.');
    }
  };

  const sendPhoneCode = async () => {
    await axios.post('/auth/phone/send', { phone: form.phone });
    alert('휴대폰 인증 코드가 전송되었습니다.');
  };

  const verifyPhoneCode = async () => {
    const res = await axios.post('/auth/phone/verify', {
      phone: form.phone,
      code: form.phoneCode,
    });
    if (res.data.success) {
      setPhoneVerified(true);
      alert('전화번호 인증 성공');
    } else {
      setError(res.data.message);
    }
  };

  const checkNickname = async () => {
    const res = await axios.get('/auth/check-nickname', {
      params: { value: form.nickname },
    });
    if (!res.data) {
      setNicknameChecked(true);
      alert('사용 가능한 닉네임입니다.');
    } else {
      setError('이미 사용 중인 닉네임입니다.');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
        <h2 className="text-2xl font-bold mb-6 text-center">🌟 회원가입</h2>
        <form onSubmit={handleSubmit} className="space-y-4 text-sm">
          <div className="space-y-1">
            <input type="email" name="email" placeholder="이메일" value={form.email} onChange={handleChange} className="w-full px-4 py-2 rounded bg-[#2a2e45]" required />
            <div className="flex gap-2">
              <button type="button" onClick={sendEmailCode} className="flex-1 bg-gray-600 py-1 rounded">코드 전송</button>
              <input type="text" name="emailCode" placeholder="인증코드" value={form.emailCode} onChange={handleChange} className="flex-1 px-2 rounded bg-[#2a2e45]" />
              <button type="button" onClick={verifyEmailCode} className="flex-1 bg-blue-600 py-1 rounded">인증 확인</button>
            </div>
          </div>

          <div className="space-y-1">
            <input type="text" name="nickname" placeholder="닉네임" value={form.nickname} onChange={handleChange} className="w-full px-4 py-2 rounded bg-[#2a2e45]" required />
            <button type="button" onClick={checkNickname} className="w-full bg-gray-600 py-1 rounded">닉네임 중복 확인</button>
          </div>

          <div className="space-y-1">
            <input type="tel" name="phone" placeholder="휴대폰 번호" value={form.phone} onChange={handleChange} className="w-full px-4 py-2 rounded bg-[#2a2e45]" required />
            <div className="flex gap-2">
              <button type="button" onClick={sendPhoneCode} className="flex-1 bg-gray-600 py-1 rounded">코드 전송</button>
              <input type="text" name="phoneCode" placeholder="인증코드" value={form.phoneCode} onChange={handleChange} className="flex-1 px-2 rounded bg-[#2a2e45]" />
              <button type="button" onClick={verifyPhoneCode} className="flex-1 bg-blue-600 py-1 rounded">인증 확인</button>
            </div>
          </div>

          <input type="password" name="password" placeholder="비밀번호" value={form.password} onChange={handleChange} className="w-full px-4 py-2 rounded bg-[#2a2e45]" required />
          <input type="password" name="confirmPassword" placeholder="비밀번호 확인" value={form.confirmPassword} onChange={handleChange} className="w-full px-4 py-2 rounded bg-[#2a2e45]" required />

          <div className="space-y-2 text-xs">
            <div className="flex items-center gap-2">
              <input type="checkbox" checked={termsAgreed} readOnly />
              <span className="text-gray-300">이용약관에 동의합니다</span>
              <button type="button" onClick={() => setShowModal('terms')} className="text-purple-400 underline">[약관 보기]</button>
            </div>
            <div className="flex items-center gap-2">
              <input type="checkbox" checked={privacyAgreed} readOnly />
              <span className="text-gray-300">개인정보 처리방침에 동의합니다</span>
              <button type="button" onClick={() => setShowModal('privacy')} className="text-purple-400 underline">[방침 보기]</button>
            </div>
          </div>

          {error && <p className="text-red-400 text-xs text-center">{error}</p>}
          <button type="submit" className="w-full bg-purple-600 hover:bg-purple-700 transition-colors py-2 rounded">회원가입</button>
        </form>
      </div>

      {showModal === 'terms' && (
        <AgreementModal title="이용약관" onClose={() => setShowModal(null)} onAgree={() => { setTermsAgreed(true); setShowModal(null); }}>
          <TermsOfService />
        </AgreementModal>
      )}
      {showModal === 'privacy' && (
        <AgreementModal title="개인정보 처리방침" onClose={() => setShowModal(null)} onAgree={() => { setPrivacyAgreed(true); setShowModal(null); }}>
          <PrivacyPolicy />
        </AgreementModal>
      )}
    </div>
  );
}
