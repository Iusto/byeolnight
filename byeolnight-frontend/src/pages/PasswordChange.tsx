import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';

export default function PasswordChange() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [form, setForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (form.newPassword !== form.confirmPassword) {
      setError('새 비밀번호가 일치하지 않습니다.');
      return;
    }

    if (form.newPassword.length < 8) {
      setError('새 비밀번호는 8자 이상이어야 합니다.');
      return;
    }

    setLoading(true);

    try {
      await axios.put('member/users/password', {
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
      });
      alert('비밀번호가 성공적으로 변경되었습니다.');
      navigate('/me');
    } catch (err: any) {
      setError(err?.response?.data?.message || '비밀번호 변경에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center px-4">
        <p className="text-white">로그인이 필요합니다.</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-[#1f2336] text-white p-8 rounded-xl shadow-lg">
        <h2 className="text-2xl font-bold mb-6 text-center">🔐 비밀번호 변경</h2>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <input
            type="password"
            name="currentPassword"
            placeholder="현재 비밀번호"
            value={form.currentPassword}
            onChange={handleChange}
            className="w-full px-4 py-2 rounded bg-[#2a2e45] text-white focus:outline-none focus:ring-2 focus:ring-purple-500"
            required
          />
          <input
            type="password"
            name="newPassword"
            placeholder="새 비밀번호 (8자 이상)"
            value={form.newPassword}
            onChange={handleChange}
            className="w-full px-4 py-2 rounded bg-[#2a2e45] text-white focus:outline-none focus:ring-2 focus:ring-purple-500"
            required
          />
          <input
            type="password"
            name="confirmPassword"
            placeholder="새 비밀번호 확인"
            value={form.confirmPassword}
            onChange={handleChange}
            className="w-full px-4 py-2 rounded bg-[#2a2e45] text-white focus:outline-none focus:ring-2 focus:ring-purple-500"
            required
          />
          
          {error && <p className="text-red-400 text-sm">{error}</p>}
          
          <button
            type="submit"
            disabled={loading}
            className={`w-full py-2 rounded transition-colors font-medium ${
              loading
                ? 'bg-gray-600 cursor-not-allowed'
                : 'bg-purple-600 hover:bg-purple-700'
            } text-white`}
          >
            {loading ? '변경 중...' : '비밀번호 변경'}
          </button>
          
          <button
            type="button"
            onClick={() => navigate('/me')}
            className="w-full bg-gray-600 hover:bg-gray-700 text-white py-2 rounded transition-colors font-medium"
          >
            취소
          </button>
        </form>
      </div>
    </div>
  );
}