import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import axios from '../lib/axios';
import AgreementModal from '../components/AgreementModal';
import TermsOfService from '../components/TermsOfService';
import PrivacyPolicy from '../components/PrivacyPolicy';

export default function Signup() {
  const navigate = useNavigate();
  const { user } = useAuth();
  
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
  const [loading, setLoading] = useState({
    emailSend: false,
    emailVerify: false,
    phoneSend: false,
    phoneVerify: false,
    nicknameCheck: false,
    signup: false,
  });
  
  // 이미 로그인된 사용자 리다이렉트
  useEffect(() => {
    if (user) {
      alert('이미 로그인되었습니다.');
      navigate('/', { replace: true });
    }
  }, []); // 초기 마운트 시에만 실행
  const [emailTimer, setEmailTimer] = useState(0);
  const [phoneTimer, setPhoneTimer] = useState(0);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    
    // 휴대폰번호는 자동 하이픈 추가
    if (name === 'phone') {
      const formatted = formatPhoneNumber(value);
      setForm((prev) => ({
        ...prev,
        [name]: formatted,
      }));
    } else if (name === 'nickname') {
      // 닉네임은 8자 제한만 적용
      if (value.length <= 8) {
        setForm((prev) => ({
          ...prev,
          [name]: value,
        }));
      }
    } else {
      setForm((prev) => ({
        ...prev,
        [name]: value,
      }));
    }
    
    // 입력 변경 시 관련 검증 상태 초기화
    if (name === 'nickname') setNicknameChecked(false);
    if (name === 'email') setEmailVerified(false);
    if (name === 'phone') setPhoneVerified(false);
    
    setError('');
  };

  // 이메일 형식 검증
  const validateEmail = (email: string) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  // 비밀번호 형식 검증
  const validatePassword = (password: string) => {
    const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/;
    return passwordRegex.test(password);
  };

  // 휴대폰 번호 형식 검증 (010-1234-5678 형식)
  const validatePhone = (phone: string) => {
    const phoneRegex = /^01[0-9]-\d{3,4}-\d{4}$/;
    return phoneRegex.test(phone);
  };

  // 전화번호 자동 하이픈 추가
  const formatPhoneNumber = (value: string) => {
    const numbers = value.replace(/[^0-9]/g, '');
    if (numbers.length <= 3) return numbers;
    if (numbers.length <= 7) return `${numbers.slice(0, 3)}-${numbers.slice(3)}`;
    return `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7, 11)}`;
  };

  // 닉네임 형식 검증 (2-8자)
  const validateNickname = (nickname: string) => {
    return nickname.length >= 2 && nickname.length <= 8;
  };

  const sendEmailCode = async () => {
    if (!form.email) {
      setError('이메일을 입력해주세요.');
      return;
    }
    
    if (!validateEmail(form.email)) {
      setError('올바른 이메일 형식을 입력해주세요.');
      return;
    }

    setLoading(prev => ({ ...prev, emailSend: true }));
    try {
      await axios.post('/auth/email/send', { email: form.email });
      alert('이메일 인증 코드가 전송되었습니다.');
      setError('');
      setEmailTimer(300); // 5분 타이머 시작
    } catch (err: any) {
      setError(err?.response?.data?.message || '이메일 전송 실패');
    } finally {
      setLoading(prev => ({ ...prev, emailSend: false }));
    }
  };

  const verifyEmailCode = async () => {
    if (!form.emailCode) {
      setError('인증 코드를 입력해주세요.');
      return;
    }

    setLoading(prev => ({ ...prev, emailVerify: true }));
    try {
      const res = await axios.post('/auth/email/verify', {
        email: form.email,
        code: form.emailCode,
      });
      if (res.data.data === true) {
        setEmailVerified(true);
        setEmailTimer(0); // 인증 완료 시 타이머 중지
        alert('이메일 인증 성공');
        setError('');
      } else {
        setError('이메일 인증 코드가 유효하지 않습니다.');
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || '이메일 인증 실패');
    } finally {
      setLoading(prev => ({ ...prev, emailVerify: false }));
    }
  };

  const sendPhoneCode = async () => {
    if (!form.phone) {
      setError('휴대폰 번호를 입력해주세요.');
      return;
    }
    
    if (!validatePhone(form.phone)) {
      setError('올바른 휴대폰 번호 형식을 입력해주세요. (예: 010-1234-5678)');
      return;
    }

    setLoading(prev => ({ ...prev, phoneSend: true }));
    try {
      await axios.post('/auth/phone/send', { phone: form.phone });
      alert('휴대폰 인증 코드가 전송되었습니다.');
      setError('');
      setPhoneTimer(300); // 5분 타이머 시작
    } catch (err: any) {
      setError(err?.response?.data?.message || '휴대폰 인증 코드 전송 실패');
    } finally {
      setLoading(prev => ({ ...prev, phoneSend: false }));
    }
  };

  const verifyPhoneCode = async () => {
    if (!form.phoneCode) {
      setError('인증 코드를 입력해주세요.');
      return;
    }

    setLoading(prev => ({ ...prev, phoneVerify: true }));
    try {
      const res = await axios.post('/auth/phone/verify', {
        phone: form.phone,
        code: form.phoneCode,
      });
      if (res.data.data === true) {
        setPhoneVerified(true);
        setPhoneTimer(0); // 인증 완료 시 타이머 중지
        alert('전화번호 인증 성공');
        setError('');
      } else {
        setError('전화번호 인증 코드가 유효하지 않습니다.');
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || '전화번호 인증 실패');
    } finally {
      setLoading(prev => ({ ...prev, phoneVerify: false }));
    }
  };

  const checkNickname = async () => {
    if (!form.nickname) {
      setError('닉네임을 입력해주세요.');
      return;
    }
    
    if (!validateNickname(form.nickname)) {
      setError('닉네임은 2-8자로 입력해주세요.');
      return;
    }

    setLoading(prev => ({ ...prev, nicknameCheck: true }));
    try {
      const res = await axios.get('/auth/check-nickname', {
        params: { value: form.nickname },
      });
      if (res.data.data === true) {
        setNicknameChecked(true);
        alert('사용 가능한 닉네임입니다.');
        setError('');
      } else {
        setError('이미 사용 중인 닉네임입니다.');
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || '닉네임 중복 확인 실패');
    } finally {
      setLoading(prev => ({ ...prev, nicknameCheck: false }));
    }
  };

  // 타이머 효과
  useEffect(() => {
    let interval: NodeJS.Timeout;
    
    if (emailTimer > 0) {
      interval = setInterval(() => {
        setEmailTimer(prev => prev - 1);
      }, 1000);
    }
    
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [emailTimer]);
  
  useEffect(() => {
    let interval: NodeJS.Timeout;
    
    if (phoneTimer > 0) {
      interval = setInterval(() => {
        setPhoneTimer(prev => prev - 1);
      }, 1000);
    }
    
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [phoneTimer]);
  
  // 시간 포맷 함수
  const formatTime = (seconds: number) => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    // 필수 입력 항목 검증
    const missingFields = [];
    
    if (!form.email) {
      missingFields.push('이메일을 입력해주세요');
    } else if (!validateEmail(form.email)) {
      missingFields.push('올바른 이메일 형식을 입력해주세요');
    } else if (!emailVerified) {
      missingFields.push('이메일 인증을 완료해주세요');
    }
    
    if (!form.nickname) {
      missingFields.push('닉네임을 입력해주세요');
    } else if (!validateNickname(form.nickname)) {
      missingFields.push('닉네임은 2-8자로 입력해주세요');
    } else if (!nicknameChecked) {
      missingFields.push('닉네임 중복 확인을 해주세요');
    }
    
    if (!form.phone) {
      missingFields.push('휴대폰 번호를 입력해주세요');
    } else if (!validatePhone(form.phone)) {
      missingFields.push('올바른 휴대폰 번호 형식을 입력해주세요 (예: 010-1234-5678)');
    } else if (!phoneVerified) {
      missingFields.push('휴대폰 인증을 완료해주세요');
    }
    
    if (!form.password) {
      missingFields.push('비밀번호를 입력해주세요');
    } else if (!validatePassword(form.password)) {
      missingFields.push('비밀번호는 8자 이상이며, 영문/숫자/특수문자를 포함해야 합니다');
    }
    
    if (!form.confirmPassword) {
      missingFields.push('비밀번호 확인을 입력해주세요');
    } else if (form.password !== form.confirmPassword) {
      missingFields.push('비밀번호가 일치하지 않습니다');
    }
    
    if (!termsAgreed) {
      missingFields.push('이용약관에 동의해주세요');
    }
    
    if (!privacyAgreed) {
      missingFields.push('개인정보 처리방침에 동의해주세요');
    }

    // 누락된 항목이 있으면 에러 메시지 표시
    if (missingFields.length > 0) {
      setError(`다음 항목을 확인해주세요:\n• ${missingFields.join('\n• ')}`);
      return;
    }

    setLoading(prev => ({ ...prev, signup: true }));
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
    } finally {
      setLoading(prev => ({ ...prev, signup: false }));
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={() => navigate('/')}
            className="text-gray-400 hover:text-white hover:bg-purple-600/50 px-2 py-1 rounded transition-colors"
            title="홈으로 돌아가기"
          >
            ← 홈
          </button>
          <h2 className="text-2xl font-bold">🌟 회원가입</h2>
          <div className="w-12"></div>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4 text-sm">
          {/* 이메일 인증 */}
          <div className="space-y-1">
            <input 
              type="email" 
              name="email" 
              placeholder="이메일" 
              value={form.email} 
              onChange={handleChange} 
              className="w-full px-4 py-2 rounded bg-[#2a2e45] focus:outline-none focus:ring-2 focus:ring-purple-500" 
              required 
            />
            <div className="flex gap-2">
              <button 
                type="button" 
                onClick={sendEmailCode} 
                disabled={loading.emailSend || !form.email || emailVerified}
                className={`w-20 py-1 rounded transition-colors text-xs ${
                  loading.emailSend || !form.email || emailVerified
                    ? 'bg-gray-800 cursor-not-allowed text-gray-500'
                    : 'bg-gray-600 hover:bg-gray-700 text-white'
                }`}
              >
                {emailVerified ? '완료' : loading.emailSend ? '전송중' : '전송'}
              </button>
              <div className="flex-1 relative">
                <input 
                  type="text" 
                  name="emailCode" 
                  placeholder="인증코드" 
                  value={form.emailCode} 
                  onChange={handleChange} 
                  className="w-full px-3 pr-16 py-2 rounded bg-[#2a2e45] focus:outline-none focus:ring-2 focus:ring-purple-500" 
                />
                {emailTimer > 0 && (
                  <span className="absolute right-2 top-1/2 transform -translate-y-1/2 text-xs text-orange-400 font-mono">
                    {formatTime(emailTimer)}
                  </span>
                )}
              </div>
              <button 
                type="button" 
                onClick={verifyEmailCode} 
                disabled={loading.emailVerify || !form.emailCode}
                className={`w-16 py-1 rounded transition-colors text-xs ${
                  loading.emailVerify || !form.emailCode
                    ? 'bg-blue-800 cursor-not-allowed text-gray-500'
                    : 'bg-blue-600 hover:bg-blue-700 text-white'
                }`}
              >
                {loading.emailVerify ? '확인중' : '확인'}
              </button>
            </div>
            {emailVerified && <p className="text-green-400 text-xs">✓ 이메일 인증 완료</p>}
          </div>

          {/* 닉네임 중복 확인 */}
          <div className="space-y-1">
            <input 
              type="text" 
              name="nickname" 
              placeholder="닉네임 (2-8자)" 
              value={form.nickname} 
              onChange={handleChange} 
              className="w-full px-4 py-2 rounded bg-[#2a2e45] focus:outline-none focus:ring-2 focus:ring-purple-500" 
              required 
            />
            <p className="text-xs text-gray-400">* 모든 문자 가능 (2-8자)</p>
            <button 
              type="button" 
              onClick={checkNickname} 
              disabled={loading.nicknameCheck || !form.nickname}
              className={`w-full py-1 rounded transition-colors ${
                loading.nicknameCheck || !form.nickname
                  ? 'bg-gray-800 cursor-not-allowed text-gray-500'
                  : 'bg-gray-600 hover:bg-gray-700 text-white'
              }`}
            >
              {loading.nicknameCheck ? '확인중...' : '닉네임 중복 확인'}
            </button>
            {nicknameChecked && <p className="text-green-400 text-xs">✓ 사용 가능한 닉네임</p>}
          </div>

          {/* 휴대폰 인증 */}
          <div className="space-y-1">
            <input 
              type="tel" 
              name="phone" 
              placeholder="휴대폰 번호 (예: 010-1234-5678)" 
              value={form.phone} 
              onChange={handleChange} 
              className="w-full px-4 py-2 rounded bg-[#2a2e45] focus:outline-none focus:ring-2 focus:ring-purple-500" 
              required 
            />
            <p className="text-xs text-gray-400">* 휴대폰번호는 자동으로 하이픈이 추가됩니다</p>
            <p className="text-xs text-green-400">🔒 입력하신 휴대폰번호는 암호화되어 안전하게 저장됩니다</p>
            <div className="flex gap-2">
              <button 
                type="button" 
                onClick={sendPhoneCode} 
                disabled={loading.phoneSend || !form.phone || phoneVerified}
                className={`w-20 py-1 rounded transition-colors text-xs ${
                  loading.phoneSend || !form.phone || phoneVerified
                    ? 'bg-gray-800 cursor-not-allowed text-gray-500'
                    : 'bg-gray-600 hover:bg-gray-700 text-white'
                }`}
              >
                {phoneVerified ? '완료' : loading.phoneSend ? '전송중' : '전송'}
              </button>
              <div className="flex-1 relative">
                <input 
                  type="text" 
                  name="phoneCode" 
                  placeholder="인증코드" 
                  value={form.phoneCode} 
                  onChange={handleChange} 
                  className="w-full px-3 pr-16 py-2 rounded bg-[#2a2e45] focus:outline-none focus:ring-2 focus:ring-purple-500" 
                />
                {phoneTimer > 0 && (
                  <span className="absolute right-2 top-1/2 transform -translate-y-1/2 text-xs text-orange-400 font-mono">
                    {formatTime(phoneTimer)}
                  </span>
                )}
              </div>
              <button 
                type="button" 
                onClick={verifyPhoneCode} 
                disabled={loading.phoneVerify || !form.phoneCode}
                className={`w-16 py-1 rounded transition-colors text-xs ${
                  loading.phoneVerify || !form.phoneCode
                    ? 'bg-blue-800 cursor-not-allowed text-gray-500'
                    : 'bg-blue-600 hover:bg-blue-700 text-white'
                }`}
              >
                {loading.phoneVerify ? '확인중' : '확인'}
              </button>
            </div>
            {phoneVerified && <p className="text-green-400 text-xs">✓ 휴대폰 인증 완료</p>}
          </div>

          {/* 비밀번호 */}
          <input 
            type="password" 
            name="password" 
            placeholder="비밀번호 (8자 이상, 영문/숫자/특수문자 포함)" 
            value={form.password} 
            onChange={handleChange} 
            className="w-full px-4 py-2 rounded bg-[#2a2e45] focus:outline-none focus:ring-2 focus:ring-purple-500" 
            required 
          />
          <input 
            type="password" 
            name="confirmPassword" 
            placeholder="비밀번호 확인" 
            value={form.confirmPassword} 
            onChange={handleChange} 
            className="w-full px-4 py-2 rounded bg-[#2a2e45] focus:outline-none focus:ring-2 focus:ring-purple-500" 
            required 
          />

          {/* 약관 동의 */}
          <div className="space-y-2 text-xs">
            <div className="bg-blue-900/20 border border-blue-600/30 p-3 rounded">
              <p className="text-blue-300 text-center mb-2 font-medium">
                ⚠️ 중요: 약관보기 및 방침보기를 클릭하여 내용을 확인한 후<br/>
                '동의합니다' 버튼을 클릭해야 체크박스가 활성화됩니다.
              </p>
            </div>
            <div className="flex items-center gap-2">
              <input 
                type="checkbox" 
                checked={termsAgreed} 
                readOnly
                className="w-4 h-4 pointer-events-none"
              />
              <span className="text-gray-300">이용약관에 동의합니다</span>
              <button 
                type="button" 
                onClick={() => setShowModal('terms')} 
                className="text-purple-400 underline"
              >
                [약관 보기]
              </button>
            </div>
            <div className="flex items-center gap-2">
              <input 
                type="checkbox" 
                checked={privacyAgreed} 
                readOnly
                className="w-4 h-4 pointer-events-none"
              />
              <span className="text-gray-300">개인정보 처리방침에 동의합니다</span>
              <button 
                type="button" 
                onClick={() => setShowModal('privacy')} 
                className="text-purple-400 underline"
              >
                [방침 보기]
              </button>
            </div>
          </div>

          {error && (
            <div className="bg-red-900/20 border border-red-600/30 p-3 rounded text-xs">
              <p className="text-red-400 whitespace-pre-line">{error}</p>
            </div>
          )}
          
          <button 
            type="submit" 
            disabled={loading.signup}
            className={`w-full py-2 rounded transition-colors ${
              loading.signup
                ? 'bg-gray-600 cursor-not-allowed text-gray-400'
                : 'bg-purple-600 hover:bg-purple-700 text-white'
            }`}
          >
            {loading.signup ? '가입 중...' : '회원가입'}
          </button>
        </form>
      </div>

      {showModal === 'terms' && (
        <AgreementModal 
          title="이용약관" 
          onClose={() => setShowModal(null)} 
          onAgree={() => { setTermsAgreed(true); setShowModal(null); }}
        >
          <TermsOfService />
        </AgreementModal>
      )}
      {showModal === 'privacy' && (
        <AgreementModal 
          title="개인정보 처리방침" 
          onClose={() => setShowModal(null)} 
          onAgree={() => { setPrivacyAgreed(true); setShowModal(null); }}
        >
          <PrivacyPolicy />
        </AgreementModal>
      )}
    </div>
  );
}