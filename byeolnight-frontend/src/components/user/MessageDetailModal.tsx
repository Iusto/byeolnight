import type { Message } from '../../types/message';

interface MessageDetailModalProps {
  message: Message;
  formatDate: (date?: string) => string;
  onClose: () => void;
}

/** 선택한 쪽지의 상세 내용을 표시하는 모달입니다. */
export default function MessageDetailModal({ message, formatDate, onClose }: MessageDetailModalProps) {
  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-3 sm:p-4">
      <div className="bg-slate-800/95 backdrop-blur-md rounded-xl border border-purple-500/20 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-3 sm:p-4 border-b border-purple-500/20">
          <h2 className="text-base sm:text-lg font-bold text-white">📩 쪽지 상세</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-purple-600/20 rounded-lg transition-colors text-gray-400 hover:text-white min-w-[40px] min-h-[40px] flex items-center justify-center"
          >
            ✕
          </button>
        </div>
        <div className="p-3 sm:p-4 space-y-3 sm:space-y-4">
          <div>
            <h3 className="text-sm sm:text-base font-bold text-white mb-2">{message.title}</h3>
            <div className="text-xs sm:text-sm text-gray-400 space-y-1">
              <p>보낸이: {message.senderNickname}</p>
              <p>받는이: {message.receiverNickname}</p>
              <p>보낸 시간: {formatDate(message.createdAt)}</p>
              {message.isRead && message.readAt && <p>읽은 시간: {formatDate(message.readAt)}</p>}
            </div>
          </div>
          <hr className="border-gray-600" />
          <div className="text-white whitespace-pre-wrap text-sm sm:text-base">{message.content}</div>
        </div>
      </div>
    </div>
  );
}
