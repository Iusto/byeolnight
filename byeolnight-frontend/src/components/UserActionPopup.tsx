import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { sendMessage } from '../lib/api/message';
import UserProfileModal from './UserProfileModal';

interface UserActionPopupProps {
  targetUserId: number;
  targetNickname: string;
  onClose: () => void;
  position: { x: number; y: number };
}

export default function UserActionPopup({ targetUserId, targetNickname, onClose, position }: UserActionPopupProps) {
  const { user } = useAuth();
  const [showMessageForm, setShowMessageForm] = useState(false);
  const [messageContent, setMessageContent] = useState('');
  const [sending, setSending] = useState(false);
  const [showProfileModal, setShowProfileModal] = useState(false);

  const isAdmin = user?.role === 'ADMIN';
  const isSelf = user?.id === targetUserId;

  const handleViewProfile = () => {
    setShowProfileModal(true);
  };

  const handleSendMessage = async () => {
    if (!messageContent.trim()) return;
    
    console.log('쪽지 전송 요청:', {
      targetUserId: targetUserId,
      targetNickname: targetNickname,
      receiverId: targetUserId,
      title: '쪽지',
      content: messageContent.trim(),
      user: user
    });
    
    if (!targetUserId) {
      alert('수신자 ID가 없습니다.');
      setSending(false);
      return;
    }
    
    setSending(true);
    try {
      await sendMessage({
        receiverId: targetUserId,
        title: '쪽지',
        content: messageContent.trim()
      });
      alert('쪽지를 전송했습니다.');
      setMessageContent('');
      setShowMessageForm(false);
      onClose();
    } catch (err: any) {
      console.error('쪽지 전송 오류:', err.response);
      alert(err.response?.data?.message || '쪽지 전송에 실패했습니다.');
    } finally {
      setSending(false);
    }
  };

  const handleAdminAction = async (action: string) => {
    const confirmMessage = {
      ban: '이 사용자를 밴 처리하시겠습니까?',
      suspend: '이 사용자를 정지시키겠습니까?',
      activate: '이 사용자를 활성화하시겠습니까?',
      lock: '이 사용자 계정을 잠그시겠습니까?',
      unlock: '이 사용자 계정을 잠금해제하시겠습니까?'
    }[action];

    if (!confirm(confirmMessage)) return;

    try {
      const reason = action === 'ban' || action === 'suspend' ? 
        prompt('사유를 입력하세요:') : undefined;
      
      if ((action === 'ban' || action === 'suspend') && !reason?.trim()) {
        alert('사유를 입력해주세요.');
        return;
      }

      const endpoint = {
        ban: `/admin/users/${targetUserId}/status`,
        suspend: `/admin/users/${targetUserId}/status`,
        activate: `/admin/users/${targetUserId}/status`,
        lock: `/admin/users/${targetUserId}/lock`,
        unlock: `/admin/users/${targetUserId}/lock`
      }[action];

      const data = {
        ban: { status: 'BANNED', reason },
        suspend: { status: 'SUSPENDED', reason },
        activate: { status: 'ACTIVE' },
        lock: { locked: true },
        unlock: { locked: false }
      }[action];

      await axios.patch(endpoint, data);
      alert('처리되었습니다.');
      onClose();
    } catch (err: any) {
      alert(err.response?.data?.message || '처리에 실패했습니다.');
    }
  };

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/20" onClick={onClose} />
      <div 
        className="fixed z-50 bg-[#1f2336]/95 backdrop-blur-md border border-purple-500/30 rounded-xl shadow-2xl p-5 min-w-64"
        style={{ 
          left: `${position.x}px`, 
          top: `${position.y}px`
        }}
      >
        <div className="text-white font-bold mb-4 border-b border-purple-500/30 pb-3 text-center">
          🌟 {targetNickname}
        </div>

        {showMessageForm ? (
          <div className="space-y-3">
            <textarea
              value={messageContent}
              onChange={(e) => setMessageContent(e.target.value)}
              placeholder="쪽지 내용을 입력하세요..."
              className="w-full h-32 bg-[#2a2e45]/80 border border-purple-500/30 rounded-lg px-4 py-3 text-white text-sm resize-none focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-400/20 transition-all"
              maxLength={500}
            />
            <div className="text-xs text-purple-300/70 mt-1 text-right">
              {messageContent.length}/500
            </div>
            <div className="flex gap-3 mt-4">
              <button
                onClick={handleSendMessage}
                disabled={sending || !messageContent.trim()}
                className="flex-1 bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 disabled:from-gray-600 disabled:to-gray-600 text-white py-2 px-4 rounded-lg text-sm font-medium transition-all duration-200 shadow-lg"
              >
                {sending ? '전송중...' : '💌 전송'}
              </button>
              <button
                onClick={() => setShowMessageForm(false)}
                className="flex-1 bg-gray-600/50 hover:bg-gray-600/70 text-white py-2 px-4 rounded-lg text-sm font-medium transition-all duration-200"
              >
                취소
              </button>
            </div>
          </div>
        ) : (
          <div className="space-y-2">
            <button
              onClick={handleViewProfile}
              className="w-full text-left px-4 py-3 bg-[#2a2e45]/40 hover:bg-purple-600/20 rounded-lg text-white text-sm font-medium transition-all duration-200 flex items-center gap-2 border border-purple-500/20 hover:border-purple-500/30"
            >
              👤 프로필 보기
            </button>
            
            {!isSelf && (
              <button
                onClick={() => setShowMessageForm(true)}
                className="w-full text-left px-4 py-3 bg-[#2a2e45]/40 hover:bg-purple-600/20 rounded-lg text-white text-sm font-medium transition-all duration-200 flex items-center gap-2 border border-purple-500/20 hover:border-purple-500/30"
              >
                💌 쪽지 보내기
              </button>
            )}

            {isAdmin && !isSelf && (
              <>
                <div className="border-t border-red-500/20 pt-2 mt-2">
                  <div className="text-red-300 text-xs mb-2">관리자 기능</div>
                  <button
                    onClick={() => handleAdminAction('ban')}
                    className="w-full text-left px-3 py-2 hover:bg-red-600/20 rounded text-red-300 text-sm"
                  >
                    🚫 밴 처리
                  </button>
                  <button
                    onClick={() => handleAdminAction('suspend')}
                    className="w-full text-left px-3 py-2 hover:bg-orange-600/20 rounded text-orange-300 text-sm"
                  >
                    ⏸️ 정지
                  </button>
                  <button
                    onClick={() => handleAdminAction('activate')}
                    className="w-full text-left px-3 py-2 hover:bg-green-600/20 rounded text-green-300 text-sm"
                  >
                    ✅ 활성화
                  </button>
                  <button
                    onClick={() => handleAdminAction('lock')}
                    className="w-full text-left px-3 py-2 hover:bg-yellow-600/20 rounded text-yellow-300 text-sm"
                  >
                    🔒 계정 잠금
                  </button>
                  <button
                    onClick={() => handleAdminAction('unlock')}
                    className="w-full text-left px-3 py-2 hover:bg-cyan-600/20 rounded text-cyan-300 text-sm"
                  >
                    🔓 잠금 해제
                  </button>
                </div>
              </>
            )}
          </div>
        )}
      </div>
      
      {showProfileModal && (
        <UserProfileModal 
          userId={targetUserId}
          isOpen={showProfileModal}
          onClose={() => setShowProfileModal(false)}
        />
      )}
    </>
  );
}