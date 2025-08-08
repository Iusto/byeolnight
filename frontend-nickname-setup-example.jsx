// 소셜 로그인 후 닉네임 설정 페이지 예시 (React)
import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

const SetupNickname = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [nickname, setNickname] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  const userId = searchParams.get('userId');

  useEffect(() => {
    if (!userId) {
      navigate('/auth/login');
    }
  }, [userId, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (nickname.length < 2 || nickname.length > 8) {
      setError('닉네임은 2-8자로 입력해주세요.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const response = await axios.post('/api/auth/oauth/setup-nickname', {
        userId,
        nickname,
        deviceInfo: navigator.userAgent,
        ipAddress: 'client' // 서버에서 실제 IP 추출
      });

      // 토큰 저장
      localStorage.setItem('accessToken', response.data.accessToken);
      
      // 메인 페이지로 이동
      navigate('/');
      
    } catch (error) {
      setError(error.response?.data?.error || '닉네임 설정에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            닉네임 설정
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            별 헤는 밤에서 사용할 닉네임을 설정해주세요
          </p>
        </div>
        
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="nickname" className="sr-only">
              닉네임
            </label>
            <input
              id="nickname"
              name="nickname"
              type="text"
              required
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              className="appearance-none rounded-md relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
              placeholder="닉네임 (2-8자)"
              maxLength={8}
            />
          </div>

          {error && (
            <div className="text-red-600 text-sm text-center">
              {error}
            </div>
          )}

          <div>
            <button
              type="submit"
              disabled={loading || !nickname.trim()}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
            >
              {loading ? '설정 중...' : '닉네임 설정 완료'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default SetupNickname;