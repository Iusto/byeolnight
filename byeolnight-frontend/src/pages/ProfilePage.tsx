import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import WithdrawModal from '../components/WithdrawModal';

type TabType = 'info' | 'edit' | 'password';

export default function ProfilePage() {
  const { user, refreshToken, logout } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<TabType>('info');
  const [loading, setLoading] = useState(false);
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);

  // 프로필 수정 폼
  const [profileForm, setProfileForm] = useState({
    nickname: '',
    currentPassword: '',
  });
  const [profileError, setProfileError] = useState('');
  const [profileSuccess, setProfileSuccess] = useState(false);
  const [nicknameChecked, setNicknameChecked] = useState(false);
  const [nicknameCheckLoading, setNicknameCheckLoading] = useState(false);

  // 비밀번호 변경 폼
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [passwordError, setPasswordError] = useState('');

  // 닉네임 변경 가능 여부 계산
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
      setProfileForm({
        nickname: user.nickname || '',
        currentPassword: '',
      });
    }
  }, [user]);

  // 프로필 폼 변경 핸들러
  const handleProfileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setProfileForm({ ...profileForm, [name]: value });
    
    if (name === 'nickname') {
      setNicknameChecked(false);
    }
    setProfileError('');
  };

  // 비밀번호 폼 변경 핸들러
  const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPasswordForm({ ...passwordForm, [e.target.name]: e.target.value });
    setPasswordError('');
  };

  // 닉네임 중복 검사
  const checkNickname = async () => {
    if (!profileForm.nickname) {
      setProfileError('닉네임을 입력해주세요.');
      return;
    }
    
    if (profileForm.nickname.length < 2 || profileForm.nickname.length > 8) {
      setProfileError('닉네임은 2-8자로 입력해주세요.');
      return;
    }
    
    if (profileForm.nickname === user?.nickname) {
      setNicknameChecked(true);
      setProfileError('');
      return;
    }

    setNicknameCheckLoading(true);
    try {
      const res = await axios.get('/auth/check-nickname', {
        params: { value: profileForm.nickname },
      });
      
      const isAvailable = res.data.data;
      
      if (isAvailable === true) {
        setNicknameChecked(true);
        setProfileError('');
        alert('사용 가능한 닉네임입니다.');
      } else {
        setProfileError('이미 사용 중인 닉네임입니다.');
        setNicknameChecked(false);
      }
    } catch (err: any) {
      setProfileError('닉네임 중복 확인 실패');
      setNicknameChecked(false);
    } finally {
      setNicknameCheckLoading(false);
    }
  };

  // 프로필 수정 제출
  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileError('');
    setProfileSuccess(false);
    setLoading(true);

    if (profileForm.nickname.length < 2 || profileForm.nickname.length > 8) {
      setProfileError('닉네임은 2-8자로 입력해주세요.');
      setLoading(false);
      return;
    }
    
    if (profileForm.nickname !== user?.nickname && !nicknameInfo.canChange) {
      setProfileError(`닉네임은 6개월마다 변경 가능합니다. 다음 변경 가능 시기: ${nicknameInfo.nextChangeDate?.toLocaleDateString('ko-KR')}`);
      setLoading(false);
      return;
    }
    
    if (profileForm.nickname !== user?.nickname && !nicknameChecked) {
      setProfileError('닉네임 중복 확인을 해주세요.');
      setLoading(false);
      return;
    }
    
    if (!user?.socialProvider && !profileForm.currentPassword) {
      setProfileError('현재 비밀번호를 입력해주세요.');
      setLoading(false);
      return;
    }

    try {
      const requestData: any = { nickname: profileForm.nickname };
      
      if (!user?.socialProvider) {
        requestData.currentPassword = profileForm.currentPassword;
      }
      
      await axios.put('/member/users/profile', requestData);
      setProfileSuccess(true);
      await refreshToken();
      
      setTimeout(() => {
        navigate('/');
      }, 1500);
      
    } catch (err: any) {
      const msg = err?.response?.data?.message || '프로필 수정에 실패했습니다.';
      setProfileError(msg);
    } finally {
      setLoading(false);
    }
  };

  // 비밀번호 변경 제출
  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordError('');

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError('새 비밀번호가 일치하지 않습니다.');
      return;
    }

    if (passwordForm.newPassword.length < 8) {
      setPasswordError('새 비밀번호는 8자 이상이어야 합니다.');
      return;
    }

    setLoading(true);

    try {
      await axios.put('member/users/password', {
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      alert('비밀번호가 성공적으로 변경되었습니다.');
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setActiveTab('info');
    } catch (err: any) {
      setPasswordError(err?.response?.data?.message || '비밀번호 변경에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 회원탈퇴 처리
  const handleWithdraw = async (password: string, reason: string) => {
    try {
      await axios.delete('/auth/withdraw', {
        data: { password, reason }
      });
      alert('회원 탈퇴가 완료되었습니다. 그동안 이용해 주셔서 감사합니다.');
      logout();
      navigate('/');
    } catch (err: any) {
      const errorMsg = err?.response?.data?.message || '회원 탈퇴에 실패했습니다.';
      alert(errorMsg);
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
    <div className="min-h-screen min-h-screen-safe bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex justify-center pt-4 sm:pt-20 text-white mobile-optimized">
      <div className="w-full max-w-2xl bg-[#1f2336] rounded-xl shadow-lg overflow-hidden mx-4 sm:mx-0">
        {/* 탭 헤더 */}
        <div className="flex border-b border-gray-600">
          <button
            onClick={() => setActiveTab('info')}
            className={`flex-1 mobile-tab mobile-button touch-target text-center transition-colors text-xs sm:text-sm ${
              activeTab === 'info' 
                ? 'bg-purple-600 text-white' 
                : 'bg-[#2a2e45] text-gray-300 active:text-white'
            }`}
          >
            <span className="block sm:inline">🙋</span>
            <span className="hidden xs:inline ml-1">내 정보</span>
          </button>
          <button
            onClick={() => setActiveTab('edit')}
            className={`flex-1 mobile-tab mobile-button touch-target text-center transition-colors text-xs sm:text-sm ${
              activeTab === 'edit' 
                ? 'bg-purple-600 text-white' 
                : 'bg-[#2a2e45] text-gray-300 active:text-white'
            }`}
          >
            <span className="block sm:inline">✏️</span>
            <span className="hidden xs:inline ml-1">수정</span>
          </button>
          {!user?.socialProvider && (
            <button
              onClick={() => setActiveTab('password')}
              className={`flex-1 mobile-tab mobile-button touch-target text-center transition-colors text-xs sm:text-sm ${
                activeTab === 'password' 
                  ? 'bg-purple-600 text-white' 
                  : 'bg-[#2a2e45] text-gray-300 active:text-white'
              }`}
            >
              <span className="block sm:inline">🔐</span>
              <span className="hidden xs:inline ml-1">비밀번호</span>
            </button>
          )}
        </div>

        {/* 탭 내용 */}
        <div className="p-4 sm:p-8">
          {/* 내 정보 탭 */}
          {activeTab === 'info' && (
            <div className="space-y-4 sm:space-y-6">
              <h2 className="text-xl sm:text-2xl font-bold mb-4 sm:mb-6">내 정보</h2>
              <div className="space-y-3 sm:space-y-4 text-sm sm:text-base">
                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center py-3 border-b border-gray-600 gap-1 sm:gap-0">
                  <span className="text-gray-300 text-xs sm:text-sm">이메일</span>
                  <span className="font-medium break-all">{user.email}</span>
                </div>
                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center py-3 border-b border-gray-600 gap-1 sm:gap-0">
                  <span className="text-gray-300 text-xs sm:text-sm">닉네임</span>
                  <span className="font-medium">{user.nickname}</span>
                </div>
                <div className="py-3 border-b border-gray-600">
                  <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-2 sm:gap-0">
                    <span className="text-gray-300 text-xs sm:text-sm">권한</span>
                    <div className="text-left sm:text-right">
                      <div className="font-medium text-sm sm:text-base">{user.role === 'ADMIN' ? '관리자' : '일반 사용자'}</div>
                      {user.socialProvider && (
                        <span className="text-xs px-2 py-1 bg-blue-500/20 text-blue-300 rounded border border-blue-500/30 mt-1 inline-block">
                          {user.socialProvider === 'google' ? '구글' : 
                           user.socialProvider === 'naver' ? '네이버' : 
                           user.socialProvider === 'kakao' ? '카카오' : user.socialProvider}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
              
              <div className="pt-4 sm:pt-6">
                <button
                  onClick={() => setShowWithdrawModal(true)}
                  className="w-full mobile-button touch-target bg-red-600 active:bg-red-700 text-white px-6 py-3 rounded transition-colors font-medium"
                >
                  ⚠️ 회원 탈퇴
                </button>
              </div>
            </div>
          )}

          {/* 프로필 수정 탭 */}
          {activeTab === 'edit' && (
            <div>
              <h2 className="text-xl sm:text-2xl font-bold mb-4 sm:mb-6">프로필 수정</h2>
              
              <form onSubmit={handleProfileSubmit} className="space-y-4 sm:space-y-5">
                <div>
                  <label className="block text-sm font-medium mb-2">닉네임</label>
                  <div className="space-y-3">
                    <input
                      type="text"
                      name="nickname"
                      value={profileForm.nickname}
                      onChange={handleProfileChange}
                      placeholder="닉네임 (2-8자)"
                      className={`w-full mobile-input px-4 py-3 rounded-md focus:outline-none focus:ring-2 text-base ${
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
                      disabled={nicknameCheckLoading || !profileForm.nickname || (!nicknameInfo.canChange && user?.nicknameChanged)}
                      className="w-full mobile-button touch-target bg-gray-600 active:bg-gray-700 disabled:bg-gray-800 py-3 rounded transition-colors text-sm font-medium"
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
                  </div>
                </div>
                
                {!user?.socialProvider && (
                  <div>
                    <label className="block text-sm font-medium mb-2">현재 비밀번호 확인</label>
                    <input
                      type="password"
                      name="currentPassword"
                      value={profileForm.currentPassword}
                      onChange={handleProfileChange}
                      placeholder="현재 비밀번호를 입력해주세요"
                      className="w-full mobile-input px-4 py-3 rounded-md bg-[#2a2e45] focus:outline-none focus:ring-2 focus:ring-purple-500 text-base"
                      required
                    />
                  </div>
                )}

                {profileError && (
                  <div className="bg-red-500/20 border border-red-500 rounded p-3">
                    <p className="text-red-400 text-sm break-words">{profileError}</p>
                  </div>
                )}
                
                {profileSuccess && (
                  <div className="bg-green-500/20 border border-green-500 rounded p-3">
                    <p className="text-green-400 text-sm">프로필이 성공적으로 수정되었습니다!</p>
                  </div>
                )}
                
                <button
                  type="submit"
                  disabled={loading}
                  className={`w-full mobile-button touch-target py-3 rounded-md transition-colors font-medium ${
                    loading 
                      ? 'bg-gray-600 cursor-not-allowed' 
                      : 'bg-blue-500 active:bg-blue-600'
                  }`}
                >
                  {loading ? '수정 중...' : '저장'}
                </button>
              </form>
            </div>
          )}

          {/* 비밀번호 변경 탭 */}
          {activeTab === 'password' && !user?.socialProvider && (
            <div>
              <h2 className="text-xl sm:text-2xl font-bold mb-4 sm:mb-6">비밀번호 변경</h2>
              
              <form onSubmit={handlePasswordSubmit} className="space-y-4">
                <input
                  type="password"
                  name="currentPassword"
                  placeholder="현재 비밀번호"
                  value={passwordForm.currentPassword}
                  onChange={handlePasswordChange}
                  className="w-full mobile-input px-4 py-3 rounded bg-[#2a2e45] text-white focus:outline-none focus:ring-2 focus:ring-purple-500 text-base"
                  required
                />
                <input
                  type="password"
                  name="newPassword"
                  placeholder="새 비밀번호 (8자 이상)"
                  value={passwordForm.newPassword}
                  onChange={handlePasswordChange}
                  className="w-full mobile-input px-4 py-3 rounded bg-[#2a2e45] text-white focus:outline-none focus:ring-2 focus:ring-purple-500 text-base"
                  required
                />
                <input
                  type="password"
                  name="confirmPassword"
                  placeholder="새 비밀번호 확인"
                  value={passwordForm.confirmPassword}
                  onChange={handlePasswordChange}
                  className="w-full mobile-input px-4 py-3 rounded bg-[#2a2e45] text-white focus:outline-none focus:ring-2 focus:ring-purple-500 text-base"
                  required
                />
                
                {passwordError && <p className="text-red-400 text-sm break-words">{passwordError}</p>}
                
                <button
                  type="submit"
                  disabled={loading}
                  className={`w-full mobile-button touch-target py-3 rounded transition-colors font-medium ${
                    loading
                      ? 'bg-gray-600 cursor-not-allowed'
                      : 'bg-purple-600 active:bg-purple-700'
                  } text-white`}
                >
                  {loading ? '변경 중...' : '비밀번호 변경'}
                </button>
              </form>
            </div>
          )}
        </div>
        
        {/* 회원탈퇴 모달 */}
        <WithdrawModal 
          isOpen={showWithdrawModal}
          onClose={() => setShowWithdrawModal(false)}
          onConfirm={handleWithdraw}
          socialProvider={user?.socialProvider}
        />
      </div>
    </div>
  );
}