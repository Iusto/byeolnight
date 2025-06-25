import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';

export default function ProfileEdit() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    nickname: user?.nickname || '',
    phone: user?.phone || '',
    currentPassword: '',
  });

  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess(false);

    try {
      await axios.put('/users/profile', form);
      setSuccess(true);
      setTimeout(() => navigate('/me'), 1500);
    } catch (err: any) {
      const msg = err?.response?.data?.message || '수정 실패';
      setError(msg);
    }
  };

  if (!user) return <div className="text-white p-8">로그인이 필요합니다.</div>;

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex justify-center pt-20 text-white">
      <div className="w-full max-w-lg bg-[#1f2336] p-8 rounded-xl shadow-lg">
        <h2 className="text-2xl font-bold mb-6">✏️ 프로필 수정</h2>
        <form onSubmit={handleSubmit} className="space-y-5">
          <input
            type="text"
            name="nickname"
            value={form.nickname}
            onChange={handleChange}
            placeholder="닉네임"
            className="w-full px-4 py-2 rounded-md bg-[#2a2e45] focus:outline-none"
            required
          />
          <input
            type="tel"
            name="phone"
            value={form.phone}
            onChange={handleChange}
            placeholder="전화번호"
            className="w-full px-4 py-2 rounded-md bg-[#2a2e45] focus:outline-none"
            required
          />
          <input
            type="password"
            name="currentPassword"
            value={form.currentPassword}
            onChange={handleChange}
            placeholder="현재 비밀번호"
            className="w-full px-4 py-2 rounded-md bg-[#2a2e45] focus:outline-none"
            required
          />
          {error && <p className="text-red-400 text-sm">{error}</p>}
          {success && <p className="text-green-400 text-sm">프로필이 수정되었습니다.</p>}
          <button
            type="submit"
            className="w-full bg-blue-500 hover:bg-blue-600 transition-colors py-2 rounded-md"
          >
            저장
          </button>
        </form>
      </div>
    </div>
  );
}
