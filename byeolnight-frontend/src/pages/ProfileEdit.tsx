import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';

export default function ProfileEdit() {
  const { user, refreshToken } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const [form, setForm] = useState({
    nickname: '',
    phone: '',
    currentPassword: '',
  });

  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  // 닉네임 변경 가능 여부 및 다음 변경 가능 시기 계산
  const getNicknameChangeInfo = () => {
    if (!user || !user.nicknameChanged || !user.nicknameUpdatedAt) {
      return { canChange: true, nextChangeDate: null };
    }
    
    const lastChanged = new Date(user.nicknameUpdatedAt);
    const nextChangeDate = new Date(lastChanged);
    nextChangeDate.setMonth(nextChangeDate.getMonth() + 6);
    const canChange = new Date() >= nextChangeDate;
    
    return { canChange, nextChangeDate };
  };
  
  const nicknameInfo = getNicknameChangeInfo();

  // 사용자 정보로 폼 초기화
  useEffect(() => {
    if (user) {
      setForm({
        nickname: user.nickname || '',
        phone: user.phone || '',
        currentPassword: '',
      });
    }
  }, [user]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    
    // 닉네임 입력 시 검증
    if (name === 'nickname') {
      const nicknameRegex = /^[가-힣a-zA-Z]{0,8}$/;
      if (!nicknameRegex.test(value)) {
        return; // 유효하지 않은 문자는 입력 차단
      }
    }
    
    setForm({ ...form, [name]: value });
    setError(''); // 입력 시 에러 메시지 제거
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    setLoading(true);

    // 닉네임 검증
    const nicknameRegex = /^[가-힣a-zA-Z]{2,8}$/;
    if (!nicknameRegex.test(form.nickname)) {
      setError('닉네임은 2-8자의 한글 또는 영어만 가능합니다. (특수문자 불가)');
      setLoading(false);
      return;
    }
    
    // 닉네임 변경 가능 여부 확인
    if (form.nickname !== user?.nickname && !nicknameInfo.canChange) {
      setError(`닉네임은 6개월마다 변경 가능합니다. 다음 변경 가능 시기: ${nicknameInfo.nextChangeDate?.toLocaleDateString('ko-KR')}`);
      setLoading(false);
      return;
    }
    
    // 비밀번호 확인
    if (!form.currentPassword) {
      setError('현재 비밀번호를 입력해주세요.');
      setLoading(false);
      return;
    }

    try {
      await axios.put('/users/profile', {
        nickname: form.nickname,
        phone: form.phone,
        currentPassword: form.currentPassword,
      });
      
      setSuccess(true);
      
      // 사용자 정보 갱신
      await refreshToken();
      
      // 2초 후 내 정보 페이지로 이동
      setTimeout(() => {
        navigate('/me');
      }, 2000);
      
    } catch (err: any) {
      const msg = err?.response?.data?.message || '프로필 수정에 실패했습니다.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center text-white">
        <div className="text-center">
          <p className="text-lg mb-4">로그인이 필요합니다.</p>
          <button 
            onClick={() => navigate('/login')}
            className="bg-purple-600 hover:bg-purple-700 px-4 py-2 rounded"
          >
            로그인 하러 가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex justify-center pt-20 text-white">
      <div className="w-full max-w-lg bg-[#1f2336] p-8 rounded-xl shadow-lg">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold">✏️ 프로필 수정</h2>
          <button 
            onClick={() => navigate('/me')}
            className="text-gray-400 hover:text-white"
          >
            ← 뒤로가기
          </button>
        </div>
        
        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-medium mb-2">닉네임</label>
            <input
              type="text"
              name="nickname"
              value={form.nickname}
              onChange={handleChange}
              placeholder="닉네임 (2-8자, 한글/영어만)"
              className={`w-full px-4 py-2 rounded-md focus:outline-none focus:ring-2 ${
                !nicknameInfo.canChange && user?.nicknameChanged
                  ? 'bg-gray-600 text-gray-400 cursor-not-allowed focus:ring-gray-500'
                  : 'bg-[#2a2e45] focus:ring-purple-500'
              }`}
              disabled={!nicknameInfo.canChange && user?.nicknameChanged}
              required
            />
            <div className="text-xs mt-1 space-y-1">
              <p className="text-gray-400">* 한글 또는 영어만 가능, 특수문자 불가 (2-8자)</p>
              <p className="text-yellow-400">* 닉네임은 6개월마다 변경 가능합니다</p>
              {!nicknameInfo.canChange && nicknameInfo.nextChangeDate && (
                <p className="text-red-400">
                  * 다음 변경 가능 시기: {nicknameInfo.nextChangeDate.toLocaleDateString('ko-KR')}
                </p>
              )}
              {nicknameInfo.canChange && user?.nicknameChanged && (
                <p className="text-green-400">* 닉네임 변경 가능</p>
              )}
            </div>
          </div>
          
          <div>
            <label className="block text-sm font-medium mb-2">전화번호</label>
            <input
              type="tel"
              name="phone"
              value={form.phone}
              onChange={handleChange}
              placeholder="전화번호"
              className="w-full px-4 py-2 rounded-md bg-[#2a2e45] focus:outline-none focus:ring-2 focus:ring-purple-500"
              required
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium mb-2">현재 비밀번호 확인</label>
            <input
              type="password"
              name="currentPassword"
              value={form.currentPassword}
              onChange={handleChange}
              placeholder="현재 비밀번호를 입력해주세요"
              className="w-full px-4 py-2 rounded-md bg-[#2a2e45] focus:outline-none focus:ring-2 focus:ring-purple-500"
              required
            />
          </div>
          
          {error && (
            <div className="bg-red-500/20 border border-red-500 rounded p-3">
              <p className="text-red-400 text-sm">{error}</p>
            </div>
          )}
          
          {success && (
            <div className="bg-green-500/20 border border-green-500 rounded p-3">
              <p className="text-green-400 text-sm">프로필이 성공적으로 수정되었습니다. 잠시 후 이동합니다...</p>
            </div>
          )}
          
          <button
            type="submit"
            disabled={loading}
            className={`w-full py-2 rounded-md transition-colors ${
              loading 
                ? 'bg-gray-600 cursor-not-allowed' 
                : 'bg-blue-500 hover:bg-blue-600'
            }`}
          >
            {loading ? '수정 중...' : '저장'}
          </button>
        </form>
      </div>
    </div>
  );
}
