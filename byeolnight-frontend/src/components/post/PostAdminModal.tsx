import React from 'react';
import { createPortal } from 'react-dom';

interface PostAdminModalProps {
  isOpen: boolean;
  onClose: () => void;
  username: string;
  userId?: number;
  postId: number;
  onPostBlind: () => void;
  onPostDelete: () => void;
  onUserBan?: (username: string, duration: number) => void;
}

const PostAdminModal: React.FC<PostAdminModalProps> = ({
  isOpen,
  onClose,
  username,
  userId,
  postId,
  onPostBlind,
  onPostDelete,
  onUserBan
}) => {
  if (!isOpen) return null;

  const handleBanUser = (duration: number) => {
    if (onUserBan && confirm(`${username}님을 ${duration}분간 채팅 금지하시겠습니까?`)) {
      onUserBan(username, duration);
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
          <h3 className="text-lg font-semibold text-white">⚠️ 게시글 관리</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-white text-xl"
          >
            ✕
          </button>
        </div>

        <div className="mb-4 p-3 bg-gray-700 rounded-lg">
          <div className="text-sm text-gray-300">작성자</div>
          <div className="text-white font-semibold">{username}</div>
        </div>

        <div className="space-y-3">
          <button
            onClick={() => {
              onPostBlind();
              onClose();
            }}
            className="w-full flex items-center gap-3 px-4 py-3 text-left bg-yellow-600 hover:bg-yellow-700 text-white rounded-lg transition-colors"
          >
            <span className="text-xl">🙈</span>
            <div>
              <div className="font-semibold">게시글 블라인드</div>
              <div className="text-sm opacity-80">이 게시글을 숨김 처리합니다</div>
            </div>
          </button>

          <button
            onClick={() => {
              onPostDelete();
              onClose();
            }}
            className="w-full flex items-center gap-3 px-4 py-3 text-left bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors"
          >
            <span className="text-xl">🗑️</span>
            <div>
              <div className="font-semibold">게시글 삭제</div>
              <div className="text-sm opacity-80">이 게시글을 완전히 삭제합니다</div>
            </div>
          </button>

          {onUserBan && (
            <div className="border-t border-gray-600 pt-3">
              <div className="text-sm text-gray-300 mb-2">사용자 제재</div>
              
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
          )}
        </div>
      </div>
    </div>
  );

  return createPortal(modalContent, document.body);
};

export default PostAdminModal;