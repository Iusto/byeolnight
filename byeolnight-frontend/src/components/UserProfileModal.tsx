import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';

interface UserProfile {
  id: number;
  email: string;
  nickname: string;
  phone: string;
  role: string;
  status: string;
  level: number;
  points: number;
  postCount: number;
  commentCount: number;
}

interface UserProfileModalProps {
  username: string;
  isOpen: boolean;
  onClose: () => void;
}

export default function UserProfileModal({ username, isOpen, onClose }: UserProfileModalProps) {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(false);
  const { user: currentUser } = useAuth();

  useEffect(() => {
    if (isOpen && username) {
      fetchUserProfile();
    }
  }, [isOpen, username]);

  const fetchUserProfile = async () => {
    setLoading(true);
    try {
      const res = await axios.get(`/public/users/profile/${username}`);
      setProfile(res.data.data);
    } catch (err) {
      console.error('사용자 프로필 조회 실패', err);
    } finally {
      setLoading(false);
    }
  };

  const handleAdminAction = async (action: string) => {
    if (!profile) return;
    
    switch (action) {
      case 'manage':
        window.location.href = '/admin/users';
        break;
      case 'lock':
        if (confirm(`사용자 "${username}"의 계정을 잠금하시겠습니까?`)) {
          try {
            await axios.patch(`/admin/users/${profile.id}/lock`);
            alert('계정이 잠금되었습니다.');
          } catch {
            alert('계정 잠금에 실패했습니다.');
          }
        }
        break;
      case 'suspend':
        const suspendReason = prompt(`사용자 "${username}"을 정지하는 사유를 입력하세요:`);
        if (suspendReason) {
          try {
            await axios.patch(`/admin/users/${profile.id}/status`, { 
              status: 'SUSPENDED', 
              reason: suspendReason 
            });
            alert('계정이 정지되었습니다.');
          } catch {
            alert('계정 정지에 실패했습니다.');
          }
        }
        break;
      case 'ban':
        const banReason = prompt(`사용자 "${username}"을 밴하는 사유를 입력하세요:`);
        if (banReason) {
          try {
            await axios.patch(`/admin/users/${profile.id}/status`, { 
              status: 'BANNED', 
              reason: banReason 
            });
            alert('계정이 밴되었습니다.');
          } catch {
            alert('계정 밴에 실패했습니다.');
          }
        }
        break;
      case 'withdraw':
        const withdrawReason = prompt(`사용자 "${username}"을 강제 탈퇴시키는 사유를 입력하세요:`);
        if (withdrawReason && confirm('정말 강제 탈퇴시킬까요? 이 작업은 되돌릴 수 없습니다.')) {
          try {
            await axios.delete(`/admin/users/${profile.id}?reason=${encodeURIComponent(withdrawReason)}`);
            alert('사용자가 강제 탈퇴 처리되었습니다.');
          } catch {
            alert('강제 탈퇴 처리에 실패했습니다.');
          }
        }
        break;
    }
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={onClose}>
      <div 
        className="bg-[#1f2336] text-white p-6 rounded-xl max-w-md w-full mx-4 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xl font-bold">👤 사용자 프로필</h3>
          <button 
            onClick={onClose}
            className="text-gray-400 hover:text-white text-2xl"
          >
            ×
          </button>
        </div>

        {loading ? (
          <div className="text-center py-8">
            <div className="animate-spin w-8 h-8 border-2 border-purple-500 border-t-transparent rounded-full mx-auto mb-2"></div>
            <p className="text-gray-400">로딩 중...</p>
          </div>
        ) : profile ? (
          <div className="space-y-4">
            <div className="text-center">
              <div className="text-4xl mb-2">🌟</div>
              <h4 className="text-lg font-semibold text-purple-300">{profile.nickname}</h4>
              <p className="text-sm text-gray-400">레벨 {profile.level} • 포인트 {profile.points}</p>
            </div>

            <div className="grid grid-cols-2 gap-4 text-sm">
              <div className="bg-[#2a2e45] p-3 rounded-lg text-center">
                <div className="text-blue-400 font-semibold">{profile.postCount || 0}</div>
                <div className="text-gray-400">작성 게시글</div>
              </div>
              <div className="bg-[#2a2e45] p-3 rounded-lg text-center">
                <div className="text-green-400 font-semibold">{profile.commentCount || 0}</div>
                <div className="text-gray-400">작성 댓글</div>
              </div>
            </div>

            <div className="text-xs text-gray-500 space-y-1">
              <p>ID: <span className="text-blue-400">{profile.id}</span></p>
              <p>상태: <span className="text-green-400">{profile.status}</span></p>
              <p>권한: <span className="text-yellow-400">{profile.role}</span></p>
            </div>

            {/* 관리자 전용 기능 */}
            {currentUser?.role === 'ADMIN' && (
              <div className="border-t border-gray-600 pt-4">
                <p className="text-sm text-orange-400 mb-3">🔧 관리자 기능</p>
                <div className="grid grid-cols-2 gap-2 mb-3">
                  <button
                    onClick={() => handleAdminAction('lock')}
                    className="bg-yellow-600 hover:bg-yellow-700 text-white px-3 py-2 rounded text-sm transition font-medium"
                  >
                    계정 잠금
                  </button>
                  <button
                    onClick={() => handleAdminAction('suspend')}
                    className="bg-red-600 hover:bg-red-700 text-white px-3 py-2 rounded text-sm transition font-medium"
                  >
                    계정 정지
                  </button>
                  <button
                    onClick={() => handleAdminAction('ban')}
                    className="bg-red-800 hover:bg-red-900 text-white px-3 py-2 rounded text-sm transition font-medium"
                  >
                    계정 밴
                  </button>
                  <button
                    onClick={() => handleAdminAction('withdraw')}
                    className="bg-gray-700 hover:bg-gray-800 text-white px-3 py-2 rounded text-sm transition font-medium"
                  >
                    강제 탈퇴
                  </button>
                </div>
                <button
                  onClick={() => handleAdminAction('manage')}
                  className="w-full bg-blue-600 hover:bg-blue-700 text-white px-3 py-2 rounded text-sm transition font-medium"
                >
                  사용자 관리 페이지
                </button>
              </div>
            )}
          </div>
        ) : (
          <div className="text-center py-8">
            <p className="text-gray-400">사용자 정보를 불러올 수 없습니다.</p>
          </div>
        )}
      </div>
    </div>
  );
}