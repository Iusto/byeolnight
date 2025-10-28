import React from 'react';
import { createPortal } from 'react-dom';

interface AdminActionModalProps {
  isOpen: boolean;
  onClose: () => void;
  messageId: string;
  sender: string;
  isBlinded?: boolean;
  onMessageBlind: (messageId: string) => void;
  onMessageUnblind: (messageId: string) => void;
  onUserBan: (username: string, duration: number, reason?: string) => void;
}

const AdminActionModal: React.FC<AdminActionModalProps> = ({
  isOpen,
  onClose,
  messageId,
  sender,
  isBlinded = false,
  onMessageBlind,
  onMessageUnblind,
  onUserBan
}) => {
  if (!isOpen) return null;

  const handleBlindMessage = () => {
    if (isBlinded) {
      if (confirm('이 메시지의 블라인드를 해제하시겠습니까?')) {
        onMessageUnblind(messageId);
        onClose();
      }
    } else {
      const reason = prompt('메시지 블라인드 처리 사유를 입력하세요:', '부적절한 내용');
      if (reason !== null && reason.trim()) {
        onMessageBlind(messageId);
        onClose();
      }
    }
  };

  const handleBanUser = (duration: number) => {
    const reason = prompt(`${sender}님을 ${duration}분간 채팅 금지하는 사유를 입력하세요:`, '부적절한 채팅');
    if (reason !== null && reason.trim()) {
      onUserBan(sender, duration, reason.trim());
      alert(`${sender}님이 ${duration}분간 채팅 금지 처리되었습니다.`);
      onClose();
    }
  };

  const modalContent = (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center">
      {/* 배경 오버레이 */}
      <div 
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
      />
      
      {/* 모달 컨텐츠 */}
      <div className="relative bg-gray-800 border border-gray-600 rounded-xl shadow-2xl p-6 min-w-[300px] max-w-[400px] mx-4">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-white">⚠️ 관리자 제재</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-white text-xl"
          >
            ✕
          </button>
        </div>

        <div className="mb-4 p-3 bg-gray-700 rounded-lg">
          <div className="text-sm text-gray-300">대상 사용자</div>
          <div className="text-white font-semibold">{sender}</div>
        </div>

        <div className="space-y-3">
          <button
            onClick={handleBlindMessage}
            className={`w-full flex items-center gap-3 px-4 py-3 text-left rounded-lg transition-colors ${
              isBlinded 
                ? 'bg-green-600 hover:bg-green-700 text-white'
                : 'bg-yellow-600 hover:bg-yellow-700 text-white'
            }`}
          >
            <span className="text-xl">{isBlinded ? '👀' : '🙈'}</span>
            <div>
              <div className="font-semibold">
                {isBlinded ? '블라인드 해제' : '메시지 블라인드'}
              </div>
              <div className="text-sm opacity-80">
                {isBlinded ? '이 메시지를 다시 보이게 합니다' : '이 메시지를 숨김 처리합니다'}
              </div>
            </div>
          </button>

          <div className="border-t border-gray-600 pt-3">
            <div className="text-sm text-gray-300 mb-2">채팅 금지</div>
            
            <button
              onClick={() => handleBanUser(10)}
              className="w-full flex items-center gap-3 px-4 py-2 text-left bg-orange-600 hover:bg-orange-700 text-white rounded-lg transition-colors mb-2"
            >
              <span>🔇</span>
              <span>10분 채팅금지</span>
            </button>
            
            <button
              onClick={() => handleBanUser(60)}
              className="w-full flex items-center gap-3 px-4 py-2 text-left bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors mb-2"
            >
              <span>🚫</span>
              <span>1시간 채팅금지</span>
            </button>
            
            <button
              onClick={() => handleBanUser(1440)}
              className="w-full flex items-center gap-3 px-4 py-2 text-left bg-red-700 hover:bg-red-800 text-white rounded-lg transition-colors"
            >
              <span>⛔</span>
              <span>24시간 채팅금지</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );

  // Portal을 사용해서 body에 직접 렌더링
  return createPortal(modalContent, document.body);
};

export default AdminActionModal;