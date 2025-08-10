import React, { useState, useEffect } from 'react';
import { MessageDto } from '../../types/message';
import { messageApi } from '../../lib/api/message';

interface MessageListProps {
  type: 'received' | 'sent';
}

const MessageList: React.FC<MessageListProps> = ({ type }) => {
  const [messages, setMessages] = useState<MessageDto.Summary[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  useEffect(() => {
    loadMessages();
  }, [type]);

  const loadMessages = async () => {
    try {
      setLoading(true);
      const response = type === 'received' 
        ? await messageApi.getReceivedMessages(page)
        : await messageApi.getSentMessages(page);
      
      setMessages(response.content);
      setHasMore(!response.last);
    } catch (error) {
      console.error('쪽지 목록 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (messageId: number) => {
    if (confirm('정말 삭제하시겠습니까?')) {
      try {
        await messageApi.deleteMessage(messageId);
        setMessages(messages.filter(msg => msg.id !== messageId));
      } catch (error) {
        console.error('쪽지 삭제 실패:', error);
      }
    }
  };

  if (loading) return <div className="text-center py-4">로딩 중...</div>;

  return (
    <div className="space-y-2">
      {messages.length === 0 ? (
        <div className="text-center py-8 text-gray-500">
          {type === 'received' ? '받은 쪽지가 없습니다' : '보낸 쪽지가 없습니다'}
        </div>
      ) : (
        messages.map((message) => (
          <div
            key={message.id}
            className={`p-4 border rounded-lg hover:bg-gray-50 ${
              !message.isRead && type === 'received' ? 'bg-blue-50 border-blue-200' : 'bg-white'
            }`}
          >
            <div className="flex justify-between items-start">
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-1">
                  <span className="font-medium">{message.senderNickname}</span>
                  {!message.isRead && type === 'received' && (
                    <span className="bg-red-500 text-white text-xs px-2 py-1 rounded">새 쪽지</span>
                  )}
                </div>
                <h3 className="font-semibold text-gray-900 mb-1">{message.title}</h3>
                <p className="text-sm text-gray-500">
                  {new Date(message.createdAt).toLocaleString()}
                </p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => window.location.href = `/messages/${message.id}`}
                  className="text-blue-600 hover:text-blue-800 text-sm"
                >
                  읽기
                </button>
                <button
                  onClick={() => handleDelete(message.id)}
                  className="text-red-600 hover:text-red-800 text-sm"
                >
                  삭제
                </button>
              </div>
            </div>
          </div>
        ))
      )}
    </div>
  );
};

export default MessageList;