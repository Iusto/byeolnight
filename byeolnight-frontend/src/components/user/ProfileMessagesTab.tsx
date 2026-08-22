import type { MouseEvent } from 'react';
import type { Message, MessageListResponse } from '../../types/message';

type MessageBox = 'received' | 'sent';

interface ProfileMessagesTabProps {
  activeBox: MessageBox;
  loading: boolean;
  received: MessageListResponse;
  sent: MessageListResponse;
  formatDate: (date?: string) => string;
  onBoxChange: (box: MessageBox) => void;
  onDelete: (messageId: number, event: MouseEvent) => void;
  onMessageClick: (message: Message) => void;
  onRefresh: () => void;
}

const boxButtonClass = (active: boolean) => {
  const base = 'flex-1 py-2 px-2 sm:px-3 rounded-lg transition-all text-xs sm:text-sm min-h-[40px] sm:min-h-[44px] flex items-center justify-center';
  return active
    ? `${base} bg-blue-600 text-white`
    : `${base} text-gray-400 hover:text-white hover:bg-blue-600/20`;
};

const truncate = (text: string, maxLength = 100) =>
  text.length > maxLength ? `${text.substring(0, maxLength)}...` : text;

/** 프로필 화면에서 쪽지함의 목록 표현만 담당합니다. */
export default function ProfileMessagesTab({
  activeBox,
  loading,
  received,
  sent,
  formatDate,
  onBoxChange,
  onDelete,
  onMessageClick,
  onRefresh,
}: ProfileMessagesTabProps) {
  const messages = activeBox === 'received' ? received.messages : sent.messages;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg sm:text-2xl font-bold text-white">📩 쪽지함</h2>
        <button
          onClick={onRefresh}
          className="px-3 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg text-xs sm:text-sm transition-colors"
        >
          🔄 새로고침
        </button>
      </div>

      <div className="bg-slate-700/30 rounded-lg p-1">
        <div className="flex gap-1">
          <button onClick={() => onBoxChange('received')} className={boxButtonClass(activeBox === 'received')}>
            <span className="block sm:hidden">📥 ({received.totalCount})</span>
            <span className="hidden sm:block">📥 받은 쪽지 ({received.totalCount})</span>
          </button>
          <button onClick={() => onBoxChange('sent')} className={boxButtonClass(activeBox === 'sent')}>
            <span className="block sm:hidden">📤 ({sent.totalCount})</span>
            <span className="hidden sm:block">📤 보낸 쪽지 ({sent.totalCount})</span>
          </button>
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8 text-gray-400">로딩 중...</div>
      ) : messages.length === 0 ? (
        <div className="text-center text-gray-400 py-8">
          <div className="text-4xl mb-2">{activeBox === 'received' ? '📭' : '📤'}</div>
          <p>{activeBox === 'received' ? '받은 쪽지가 없습니다.' : '보낸 쪽지가 없습니다.'}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {messages.map(message => (
            <div
              key={message.id}
              onClick={() => onMessageClick(message)}
              className={[
                'bg-slate-700/30 rounded-lg p-3 sm:p-4 hover:bg-slate-700/50 transition-colors cursor-pointer',
                activeBox === 'received' ? 'min-h-[60px] sm:min-h-[80px] flex flex-col justify-between' : '',
                activeBox === 'received' && !message.isRead ? 'border-l-4 border-blue-500' : '',
              ].join(' ')}
            >
              <div className="flex items-start justify-between mb-2">
                <h4 className={`font-medium flex-1 ${activeBox === 'received' && !message.isRead ? 'text-white font-bold' : 'text-gray-300'}`}>
                  {message.title}
                  {activeBox === 'received' && !message.isRead && <span className="text-blue-400 ml-2">●</span>}
                </h4>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-gray-500">{formatDate(message.createdAt)}</span>
                  <button
                    onClick={event => onDelete(message.id, event)}
                    className="p-2 text-gray-400 hover:text-red-400 hover:bg-red-500 hover:bg-opacity-10 rounded transition-colors flex-shrink-0 min-w-[44px] min-h-[44px] flex items-center justify-center"
                    title="쪽지 삭제"
                  >
                    ✕
                  </button>
                </div>
              </div>
              <p className="text-sm text-gray-400 mb-2">
                {activeBox === 'received' ? `보낸이: ${message.senderNickname}` : `받는이: ${message.receiverNickname}`}
              </p>
              <p className="text-sm text-gray-300">{truncate(message.content)}</p>
              {activeBox === 'sent' && (
                <div className="text-xs text-gray-500 mt-2">
                  {message.isRead
                    ? <span className="text-green-400">✓ 읽음 ({formatDate(message.readAt)})</span>
                    : <span className="text-gray-400">○ 읽지 않음</span>}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
