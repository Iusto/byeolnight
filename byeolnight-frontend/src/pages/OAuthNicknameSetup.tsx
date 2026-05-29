import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useTranslation } from 'react-i18next';
import { getErrorMessage } from '../types/api';

const OAuthNicknameSetup: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  
  const [nickname, setNickname] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [isChecking, setIsChecking] = useState(false);
  const [isAvailable, setIsAvailable] = useState<boolean | null>(null);
  
  const userId = searchParams.get('userId');
  const token = searchParams.get('token');

  useEffect(() => {
    if (!userId || !token) {
      navigate('/auth/login');
    }
  }, [userId, token, navigate]);

  // 닉네임 중복 검사
  const checkNickname = async (nicknameValue: string) => {
    if (nicknameValue.length < 2 || nicknameValue.length > 8) {
      setIsAvailable(null);
      return;
    }

    setIsChecking(true);
    try {
      const response = await axios.get(`/api/auth/check-nickname?value=${encodeURIComponent(nicknameValue)}`);
      setIsAvailable(response.data.data);
      setError('');
    } catch (error) {
      console.error('닉네임 중복 검사 실패:', error);
      setIsAvailable(null);
    } finally {
      setIsChecking(false);
    }
  };

  // 닉네임 입력 시 디바운스 적용
  useEffect(() => {
    const timer = setTimeout(() => {
      if (nickname.trim()) {
        checkNickname(nickname.trim());
      } else {
        setIsAvailable(null);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [nickname]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (nickname.length < 2 || nickname.length > 8) {
      setError('닉네임은 2-8자로 입력해주세요.');
      return;
    }

    if (!isAvailable) {
      setError('사용할 수 없는 닉네임입니다.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const response = await axios.post('/api/auth/oauth/setup-nickname', {
        userId: parseInt(userId!),
        nickname: nickname.trim()
      });

      // 토큰 저장 (쿠키로 자동 설정됨)
      console.log('닉네임 설정 완료:', response.data);
      
      // 메인 페이지로 이동
      navigate('/');
      
    } catch (error: unknown) {
      setError(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const getNicknameStatus = () => {
    if (!nickname.trim()) return null;
    if (nickname.length < 2 || nickname.length > 8) {
      return { color: 'text-red-500', message: '2-8자로 입력해주세요' };
    }
    if (isChecking) {
      return { color: 'text-gray-500', message: '확인 중...' };
    }
    if (isAvailable === true) {
      return { color: 'text-green-500', message: '사용 가능한 닉네임입니다' };
    }
    if (isAvailable === false) {
      return { color: 'text-red-500', message: '이미 사용 중인 닉네임입니다' };
    }
    return null;
  };

  const nicknameStatus = getNicknameStatus();

  return (
    <div className="min-h-screen flex items-center justify-center bg-space-gradient">
      <div className="max-w-md w-full mx-4">
        <div className="bg-white/10 backdrop-blur-md rounded-2xl p-8 shadow-2xl border border-white/20">
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-white mb-2">🌌 별 헤는 밤</h1>
            <h2 className="text-xl font-semibold text-white/90 mb-2">닉네임 설정</h2>
            <p className="text-white/70 text-sm">
              별 헤는 밤에서 사용할 닉네임을 설정해주세요
            </p>
          </div>
          
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label htmlFor="nickname" className="block text-sm font-medium text-white/90 mb-2">
                닉네임
              </label>
              <input
                id="nickname"
                name="nickname"
                type="text"
                required
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-blue-400 focus:border-transparent transition-all"
                placeholder="닉네임 (2-8자)"
                maxLength={8}
                disabled={loading}
              />
              {nicknameStatus && (
                <p className={`text-sm mt-2 ${nicknameStatus.color}`}>
                  {nicknameStatus.message}
                </p>
              )}
            </div>

            {error && (
              <div className="bg-red-500/20 border border-red-500/30 rounded-lg p-3">
                <p className="text-red-200 text-sm text-center">{error}</p>
              </div>
            )}

            <button
              type="submit"
              disabled={loading || !nickname.trim() || isAvailable !== true}
              className="w-full py-3 px-4 bg-gradient-to-r from-blue-500 to-purple-600 text-white font-semibold rounded-lg shadow-lg hover:from-blue-600 hover:to-purple-700 focus:outline-none focus:ring-2 focus:ring-blue-400 focus:ring-offset-2 focus:ring-offset-transparent transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                <div className="flex items-center justify-center">
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2"></div>
                  설정 중...
                </div>
              ) : (
                '닉네임 설정 완료'
              )}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default OAuthNicknameSetup;