import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import axios from '../lib/axios';
import { getErrorMessage } from '../types/api';

export default function PasswordReset() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [form, setForm] = useState({
    email: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [step, setStep] = useState<'request' | 'reset'>('request');
  const [loading, setLoading] = useState(false); // 로딩 상태 추가

  // 토큰이 있으면 토큰 검증 후 재설정 단계로
  useEffect(() => {
    if (token) {
      validateToken();
    }
  }, [token]);

  const validateToken = async () => {
    setLoading(true);
    try {
      await axios.get(`/auth/password/validate-token?token=${token}`);
      setStep('reset');
    } catch (err: unknown) {
      setError(getErrorMessage(err));
      setStep('request');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleRequestReset = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true); // 로딩 시작

    try {
      await axios.post('/auth/password/reset-request', {
        email: form.email,
      });
      setSuccess(true);
    } catch (err: unknown) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false); // 로딩 종료
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (form.newPassword !== form.confirmPassword) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    setLoading(true); // 로딩 시작

    try {
      await axios.post('/auth/password/reset-confirm', {
        token,
        newPassword: form.newPassword,
      });
      alert('비밀번호가 성공적으로 변경되었습니다.');
      navigate('/login');
    } catch (err: unknown) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false); // 로딩 종료
    }
  };

  // 로딩 상태일 때 표시
  if (loading && token) {
    return (
      <div className="min-h-screen bg-space-gradient flex items-center justify-center px-4">
        <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg text-center">
          <h2 className="text-2xl font-bold mb-6">🔑 비밀번호 재설정</h2>
          <div className="flex items-center justify-center gap-2 mb-4">
            <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            <span>토큰 검증 중...</span>
          </div>
          {error && (
            <div className="mt-4 space-y-4">
              <p className="text-red-400">{error}</p>
              <button
                onClick={() => navigate('/login')}
                className="w-full bg-purple-600 hover:bg-purple-700 py-2 rounded"
              >
                로그인 페이지로
              </button>
            </div>
          )}
        </div>
      </div>
    );
  }

  if (step === 'request') {
    return (
      <div className="min-h-screen bg-space-gradient flex items-center justify-center px-4">
        <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
          <h2 className="text-2xl font-bold mb-6 text-center">🔑 비밀번호 재설정</h2>
          
          {success ? (
            <div className="text-center space-y-4">
              <p className="text-green-400">이메일로 비밀번호 재설정 링크를 전송했습니다.</p>
              <p className="text-sm text-gray-300">이메일을 확인하여 비밀번호를 재설정해주세요.</p>
              <button
                onClick={() => navigate('/login')}
                className="w-full bg-purple-600 hover:bg-purple-700 py-2 rounded"
              >
                로그인 페이지로
              </button>
            </div>
          ) : (
            <form onSubmit={handleRequestReset} className="space-y-4">
              <input
                type="email"
                name="email"
                placeholder="이메일 주소"
                value={form.email}
                onChange={handleChange}
                className="w-full px-4 py-2 rounded bg-[#2a2e45] focus:outline-none"
                required
              />
              {error && <p className="text-red-400 text-sm">{error}</p>}
              <button
                type="submit"
                disabled={loading}
                className={`w-full py-2 rounded transition-colors ${
                  loading
                    ? 'bg-gray-600 cursor-not-allowed'
                    : 'bg-purple-600 hover:bg-purple-700'
                }`}
              >
                {loading ? (
                  <div className="flex items-center justify-center gap-2">
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    전송 중...
                  </div>
                ) : (
                  '재설정 링크 전송'
                )}
              </button>
            </form>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-space-gradient flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
        <h2 className="text-2xl font-bold mb-6 text-center">🔑 새 비밀번호 설정</h2>
        <form onSubmit={handleResetPassword} className="space-y-4">
          <input
            type="password"
            name="newPassword"
            placeholder="새 비밀번호"
            value={form.newPassword}
            onChange={handleChange}
            className="w-full px-4 py-2 rounded bg-[#2a2e45] focus:outline-none"
            required
          />
          <input
            type="password"
            name="confirmPassword"
            placeholder="비밀번호 확인"
            value={form.confirmPassword}
            onChange={handleChange}
            className="w-full px-4 py-2 rounded bg-[#2a2e45] focus:outline-none"
            required
          />
          {error && <p className="text-red-400 text-sm">{error}</p>}
          <button
            type="submit"
            className="w-full bg-purple-600 hover:bg-purple-700 py-2 rounded"
          >
            비밀번호 변경
          </button>
        </form>
      </div>
    </div>
  );
}