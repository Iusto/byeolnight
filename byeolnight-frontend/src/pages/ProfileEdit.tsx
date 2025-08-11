import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import WithdrawModal from '../components/WithdrawModal';

export default function ProfileEdit() {
  const { user, refreshToken } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const [form, setForm] = useState({
    nickname: '',
    currentPassword: '',
  });

  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [nicknameChecked, setNicknameChecked] = useState(false);
  const [nicknameCheckLoading, setNicknameCheckLoading] = useState(false);
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);

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
        currentPassword: '',
      });
    }
  }, [user]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    
    setForm({ ...form, [name]: value });
    
    // 닉네임 변경 시 중복검사 초기화
    if (name === 'nickname') {
      setNicknameChecked(false);
    }
    
    setError(''); // 입력 시 에러 메시지 제거
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    setLoading(true);

    // 닉네임 검증
    if (form.nickname.length < 2 || form.nickname.length > 8) {
      setError('닉네임은 2-8자로 입력해주세요.');
      setLoading(false);
      return;
    }
    
    // 닉네임 변경 가능 여부 확인
    if (form.nickname !== user?.nickname && !nicknameInfo.canChange) {
      setError(`닉네임은 6개월마다 변경 가능합니다. 다음 변경 가능 시기: ${nicknameInfo.nextChangeDate?.toLocaleDateString('ko-KR')}`);
      setLoading(false);
      return;
    }
    
    // 닉네임이 변경되었으면 중복검사 필수
    if (form.nickname !== user?.nickname && !nicknameChecked) {
      setError('닉네임 중복 확인을 해주세요.');
      setLoading(false);
      return;
    }
    
    // 소셜 로그인 사용자가 아닌 경우에만 비밀번호 확인
    if (!user?.socialProvider && !form.currentPassword) {
      setError('현재 비밀번호를 입력해주세요.');
      setLoading(false);
      return;
    }

    try {
      const requestData: any = {
        nickname: form.nickname,
      };
      
      // 소셜 로그인 사용자가 아닌 경우에만 비밀번호 포함
      if (!user?.socialProvider) {
        requestData.currentPassword = form.currentPassword;
      }
      
      await axios.put('/member/users/profile', requestData);
      
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

  // 닉네임 중복 검사
  const checkNickname = async () => {
    if (!form.nickname) {
      setError('닉네임을 입력해주세요.');
      return;
    }
    
    if (!validateNickname(form.nickname)) {
      setError('닉네임은 2-8자로 입력해주세요.');
      return;
    }
    
    // 기존 닉네임과 동일한 경우
    if (form.nickname === user?.nickname) {
      setNicknameChecked(true);
      setError('');
      return;
    }

    setNicknameCheckLoading(true);
    try {
      const res = await axios.get('/auth/check-nickname', {
        params: { value: form.nickname },
      });
      console.log('API 전체 응답:', res);
      console.log('API 데이터:', res.data);
      console.log('API 결과값:', res.data.data);
      
      // CommonResponse 구조: { success: boolean, data: boolean, message: string }
      const isAvailable = res.data.data;
      
      if (isAvailable === true) {
        setNicknameChecked(true);
        setError('');
        alert('사용 가능한 닉네임입니다.');
      } else if (isAvailable === false) {
        setError('이미 사용 중인 닉네임입니다.');
        setNicknameChecked(false);
      } else {
        console.error('예상치 못한 API 응답:', isAvailable);
        setError('닉네임 중복 확인 실패');
        setNicknameChecked(false);
      }
    } catch (err: any) {
      setError('닉네임 중복 확인 실패');
      setNicknameChecked(false);
    } finally {
      setNicknameCheckLoading(false);
    }
  };
  
  // 닉네임 유효성 검사
  const validateNickname = (nickname: string) => {
    return nickname.length >= 2 && nickname.length <= 8;
  };

  // 회원탈퇴 처리
  const handleWithdraw = async (password: string, reason: string) => {
    try {
      // 소셜 사용자는 간단한 확인만
      if (user?.socialProvider) {
        const confirmed = window.confirm(
          `정말로 탈퇴하시겠습니까?\n\n` +
          `• 모든 개인정보가 삭제됩니다\n` +
          `• 작성한 게시글/댓글은 유지됩니다\n` +
          `• 보유 포인트와 아이콘이 소멸됩니다\n` +
          `• 동일 이메일로 재가입 가능합니다`
        );
        
        if (!confirmed) {
          return;
        }
      }
      
      await axios.delete('/api/auth/withdraw', {
        data: {
          password: user?.socialProvider ? '' : password,
          reason: reason || '사용자 요청'
        }
      });
      
      alert('회원탈퇴가 완료되었습니다. 그동안 이용해 주셔서 감사합니다.');
      
      // 로그아웃 처리 후 메인 페이지로 이동
      localStorage.clear();
      sessionStorage.clear();
      navigate('/', { replace: true });
      window.location.reload();
      
    } catch (error: any) {
      console.error('회원탈퇴 실패:', error);
      const errorMessage = error?.response?.data?.message || '회원탈퇴에 실패했습니다.';
      alert(errorMessage);
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
            <div className="space-y-2">
              <input
                type="text"
                name="nickname"
                value={form.nickname}
                onChange={handleChange}
                placeholder="닉네임 (2-8자)"
                className={`w-full px-4 py-2 rounded-md focus:outline-none focus:ring-2 ${
                  !nicknameInfo.canChange && user?.nicknameChanged
                    ? 'bg-gray-600 text-gray-400 cursor-not-allowed focus:ring-gray-500'
                    : 'bg-[#2a2e45] focus:ring-purple-500'
                }`}
                disabled={!nicknameInfo.canChange && user?.nicknameChanged}
                required
              />
              <button
                type="button"
                onClick={checkNickname}
                disabled={nicknameCheckLoading || !form.nickname || (!nicknameInfo.canChange && user?.nicknameChanged)}
                className="w-full bg-gray-600 hover:bg-gray-700 disabled:bg-gray-800 py-2 rounded transition-colors text-sm"
              >
                {nicknameCheckLoading ? '확인중...' : '닉네임 중복 확인'}
              </button>
              {nicknameChecked && (
                <p className="text-green-400 text-sm">✓ 사용 가능한 닉네임</p>
              )}
            </div>
            <div className="text-xs mt-1 space-y-1">
              <p className="text-gray-400">* 모든 문자 가능 (2-8자)</p>
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
          
          {/* 소셜 로그인 사용자가 아닌 경우에만 비밀번호 입력칸 표시 */}
          {!user?.socialProvider && (
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
          )}
          
          {/* 소셜 로그인 사용자를 위한 안내 메시지 */}
          {user?.socialProvider && (
            <div className="bg-blue-500/20 border border-blue-500 rounded p-3">
              <p className="text-blue-400 text-sm">
                🔗 {user.socialProvider === 'google' ? 'Google' : user.socialProvider === 'kakao' ? 'Kakao' : 'Naver'} 계정으로 로그인하셨습니다. 닉네임만 변경 가능합니다.
              </p>
            </div>
          )}
          
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
        
        {/* 회원탈퇴 버튼 */}
        <div className="mt-8 pt-6 border-t border-gray-600">
          {user?.socialProvider ? (
            // 소셜 사용자는 간단한 탈퇴 버튼
            <button
              onClick={() => handleWithdraw('', '소셜 로그인 사용자 탈퇴')}
              className="w-full py-2 bg-red-600 hover:bg-red-700 text-white rounded-md transition-colors text-sm"
            >
              ⚠️ 간편 탈퇴 ({user.socialProvider === 'google' ? 'Google' : user.socialProvider === 'kakao' ? 'Kakao' : 'Naver'})
            </button>
          ) : (
            // 일반 사용자는 모달을 통한 탈퇴
            <button
              onClick={() => setShowWithdrawModal(true)}
              className="w-full py-2 bg-red-600 hover:bg-red-700 text-white rounded-md transition-colors text-sm"
            >
              ⚠️ 회원 탈퇴
            </button>
          )}
        </div>
        
        {/* 회원탈퇴 모달 */}
        <WithdrawModal
          isOpen={showWithdrawModal}
          onClose={() => setShowWithdrawModal(false)}
          onConfirm={handleWithdraw}
          isSocialUser={!!user?.socialProvider}
        />
      </div>
    </div>
  );
}
