import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { getReceivedMessages, getSentMessages, deleteMessage } from '../lib/api/message';
import type { Message } from '../types/message';

export default function MessagesPage() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<'received' | 'sent'>('received');
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    if (user) {
      fetchMessages();
    }
  }, [user, activeTab, currentPage]);

  const fetchMessages = async () => {
    try {
      setLoading(true);
      const response = activeTab === 'received' 
        ? await getReceivedMessages({ page: currentPage, size: 20 })
        : await getSentMessages({ page: currentPage, size: 20 });
      
      setMessages(response.messages);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('쪽지 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (messageId: number) => {
    if (!window.confirm('정말로 삭제하시겠습니까?')) return;

    try {
      await deleteMessage(messageId);
      setMessages(prev => prev.filter(m => m.id !== messageId));
      alert('쪽지가 삭제되었습니다.');
    } catch (error) {
      console.error('쪽지 삭제 실패:', error);
      alert('쪽지 삭제에 실패했습니다.');
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR') + ' ' + date.toLocaleTimeString('ko-KR', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0f1419] via-[#1a1f2e] to-[#2d1b69] flex items-center justify-center">
        <div className="text-center">
          <p className="text-gray-400 text-lg mb-4">로그인이 필요합니다.</p>
          <Link to="/login" className="px-6 py-3 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors">
            로그인하기
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0f1419] via-[#1a1f2e] to-[#2d1b69] py-8">
      <div className="max-w-6xl mx-auto px-6">
        {/* 헤더 */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-white mb-2">📩 쪽지함</h1>
            <p className="text-gray-300">다른 사용자와 개인적인 메시지를 주고받으세요</p>
          </div>
          
          <Link
            to="/messages/new"
            className="px-6 py-3 bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white font-medium rounded-lg shadow-lg hover:shadow-xl transition-all duration-200 transform hover:scale-105"
          >
            ✍️ 쪽지 보내기
          </Link>
        </div>

        {/* 탭 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-sm rounded-xl border border-purple-500/20 overflow-hidden">
          <div className="flex border-b border-purple-500/20">
            <button
              onClick={() => {
                setActiveTab('received');
                setCurrentPage(0);
              }}
              className={`flex-1 px-6 py-4 font-medium transition-all ${
                activeTab === 'received'
                  ? 'bg-purple-600/20 text-purple-300 border-b-2 border-purple-400'
                  : 'text-gray-400 hover:text-gray-300 hover:bg-gray-600/10'
              }`}
            >
              📥 받은 쪽지함
            </button>
            <button
              onClick={() => {
                setActiveTab('sent');
                setCurrentPage(0);
              }}
              className={`flex-1 px-6 py-4 font-medium transition-all ${
                activeTab === 'sent'
                  ? 'bg-purple-600/20 text-purple-300 border-b-2 border-purple-400'
                  : 'text-gray-400 hover:text-gray-300 hover:bg-gray-600/10'
              }`}
            >
              📤 보낸 쪽지함
            </button>
          </div>

          {/* 쪽지 목록 */}
          <div className="p-6">
            {loading ? (
              <div className="text-center py-12">
                <div className="text-white text-xl">로딩 중...</div>
              </div>
            ) : messages.length === 0 ? (
              <div className="text-center py-12">
                <div className="text-6xl mb-4">📭</div>
                <p className="text-gray-400 text-lg">
                  {activeTab === 'received' ? '받은 쪽지가 없습니다.' : '보낸 쪽지가 없습니다.'}
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {messages.map((message) => (
                  <div
                    key={message.id}
                    className={`bg-[#2a2e45]/60 rounded-lg p-4 border transition-all hover:border-purple-400/40 ${
                      activeTab === 'received' && !message.isRead
                        ? 'border-blue-500/30 bg-blue-900/10'
                        : 'border-purple-500/20'
                    }`}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          {activeTab === 'received' && !message.isRead && (
                            <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                          )}
                          <Link
                            to={`/messages/${message.id}`}
                            className="text-lg font-semibold text-white hover:text-purple-300 transition-colors"
                          >
                            {message.title}
                          </Link>
                        </div>
                        
                        <div className="flex items-center gap-4 text-sm text-gray-400 mb-2">
                          <span>
                            {activeTab === 'received' ? '발신자' : '수신자'}: {' '}
                            <span className="text-purple-300">
                              {activeTab === 'received' ? message.senderNickname : message.receiverNickname}
                            </span>
                          </span>
                          <span>{formatDate(message.createdAt)}</span>
                          {message.isRead && message.readAt && (
                            <span className="text-green-400">읽음</span>
                          )}
                        </div>
                        
                        <p className="text-gray-300 line-clamp-2">{message.content}</p>
                      </div>
                      
                      <div className="flex items-center gap-2 ml-4">
                        <Link
                          to={`/messages/${message.id}`}
                          className="px-3 py-1 bg-purple-600/20 hover:bg-purple-600/40 text-purple-300 text-sm rounded transition-all"
                        >
                          보기
                        </Link>
                        <button
                          onClick={() => handleDelete(message.id)}
                          className="w-8 h-8 flex items-center justify-center bg-red-600/20 hover:bg-red-600/40 text-red-300 rounded transition-all group"
                          title="쪽지 삭제"
                        >
                          <span className="text-lg group-hover:scale-110 transition-transform">×</span>
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* 페이지네이션 */}
            {totalPages > 1 && (
              <div className="flex justify-center gap-2 mt-8">
                <button
                  onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                  disabled={currentPage === 0}
                  className="px-4 py-2 bg-purple-600/20 hover:bg-purple-600/40 disabled:bg-gray-600/20 disabled:text-gray-500 text-purple-300 rounded transition-all"
                >
                  이전
                </button>
                <span className="px-4 py-2 text-gray-300">
                  {currentPage + 1} / {totalPages}
                </span>
                <button
                  onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
                  disabled={currentPage >= totalPages - 1}
                  className="px-4 py-2 bg-purple-600/20 hover:bg-purple-600/40 disabled:bg-gray-600/20 disabled:text-gray-500 text-purple-300 rounded transition-all"
                >
                  다음
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}