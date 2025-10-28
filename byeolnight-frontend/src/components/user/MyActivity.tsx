import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import axios from '../../lib/axios';
import { useAuth } from '../../contexts/AuthContext';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writerNickname: string;
  likeCount: number;
  commentCount: number;
  viewCount: number;
  isBlinded: boolean;
  createdAt: string;
}

interface Comment {
  id: number;
  postId: number;
  postTitle: string;
  content: string;
  writerNickname: string;
  parentId?: number;
  isBlinded: boolean;
  createdAt: string;
}

interface Message {
  id: number;
  senderId: number;
  senderNickname: string;
  receiverId: number;
  receiverNickname: string;
  title: string;
  content: string;
  isRead: boolean;
  readAt?: string;
  createdAt: string;
}

interface MyActivityData {
  myPosts: Post[];
  myComments: Comment[];
  receivedMessages: {
    messages: Message[];
    totalCount: number;
  };
  sentMessages: {
    messages: Message[];
    totalCount: number;
  };
  totalPostCount: number;
  totalCommentCount: number;
  totalReceivedMessageCount: number;
  totalSentMessageCount: number;
}

const categoryLabels: Record<string, string> = {
  NEWS: '뉴스',
  DISCUSSION: '토론',
  IMAGE: '사진',
  EVENT: '행사',
  REVIEW: '후기',
  FREE: '자유',
  NOTICE: '공지',
};

export default function MyActivity() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<'posts' | 'comments' | 'messages'>('posts');
  const [messageTab, setMessageTab] = useState<'received' | 'sent'>('received');
  const [activity, setActivity] = useState<MyActivityData | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedMessage, setSelectedMessage] = useState<Message | null>(null);

  useEffect(() => {
    if (user) {
      fetchMyActivity();
    }
  }, [user]);

  const fetchMyActivity = async () => {
    try {
      setLoading(true);
      const response = await axios.get('/member/users/my-activity?page=0&size=10');
      if (response.data.success) {
        setActivity(response.data.data);
      }
    } catch (error) {
      console.error('내 활동 내역 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleMessageClick = async (message: Message) => {
    setSelectedMessage(message);
    
    // 받은 쪽지이고 읽지 않은 경우 읽음 처리
    if (messageTab === 'received' && !message.isRead) {
      try {
        await axios.patch(`/member/messages/${message.id}/read`);
        // 로컬 상태 업데이트
        setActivity(prev => {
          if (!prev) return prev;
          return {
            ...prev,
            receivedMessages: {
              ...prev.receivedMessages,
              messages: prev.receivedMessages.messages.map(m => 
                m.id === message.id ? { ...m, isRead: true, readAt: new Date().toISOString() } : m
              )
            }
          };
        });
      } catch (error) {
        console.error('쪽지 읽음 처리 실패:', error);
      }
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const truncateText = (text: string, maxLength: number) => {
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  };

  if (!user) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-400">로그인이 필요합니다.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="text-center py-8">
        <p className="text-white">로딩 중...</p>
      </div>
    );
  }

  if (!activity) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-400">활동 내역을 불러올 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 통계 요약 */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-[#2a2e45]/60 rounded-lg p-4 text-center">
          <div className="text-2xl font-bold text-purple-300">{activity.totalPostCount}</div>
          <div className="text-sm text-gray-400">작성한 글</div>
        </div>
        <div className="bg-[#2a2e45]/60 rounded-lg p-4 text-center">
          <div className="text-2xl font-bold text-blue-300">{activity.totalCommentCount}</div>
          <div className="text-sm text-gray-400">작성한 댓글</div>
        </div>
        <div className="bg-[#2a2e45]/60 rounded-lg p-4 text-center">
          <div className="text-2xl font-bold text-green-300">{activity.totalReceivedMessageCount}</div>
          <div className="text-sm text-gray-400">받은 쪽지</div>
        </div>
        <div className="bg-[#2a2e45]/60 rounded-lg p-4 text-center">
          <div className="text-2xl font-bold text-orange-300">{activity.totalSentMessageCount}</div>
          <div className="text-sm text-gray-400">보낸 쪽지</div>
        </div>
      </div>

      {/* 탭 메뉴 */}
      <div className="flex space-x-1 bg-[#2a2e45]/60 rounded-lg p-1">
        <button
          onClick={() => setActiveTab('posts')}
          className={`flex-1 py-2 px-4 rounded-md transition-all ${
            activeTab === 'posts'
              ? 'bg-purple-600 text-white'
              : 'text-gray-400 hover:text-white hover:bg-purple-600/20'
          }`}
        >
          📝 내 게시글
        </button>
        <button
          onClick={() => setActiveTab('comments')}
          className={`flex-1 py-2 px-4 rounded-md transition-all ${
            activeTab === 'comments'
              ? 'bg-purple-600 text-white'
              : 'text-gray-400 hover:text-white hover:bg-purple-600/20'
          }`}
        >
          💬 내 댓글
        </button>
        <button
          onClick={() => setActiveTab('messages')}
          className={`flex-1 py-2 px-4 rounded-md transition-all ${
            activeTab === 'messages'
              ? 'bg-purple-600 text-white'
              : 'text-gray-400 hover:text-white hover:bg-purple-600/20'
          }`}
        >
          📩 쪽지함
        </button>
      </div>

      {/* 내용 영역 */}
      <div className="bg-[#2a2e45]/60 rounded-lg p-6">
        {activeTab === 'posts' && (
          <div className="space-y-4">
            <h3 className="text-lg font-bold text-white mb-4">📝 내가 작성한 게시글</h3>
            {activity.myPosts.length === 0 ? (
              <p className="text-gray-400 text-center py-8">작성한 게시글이 없습니다.</p>
            ) : (
              <div className="space-y-3">
                {activity.myPosts.map((post) => (
                  <div key={post.id} className="bg-[#1f2336]/60 rounded-lg p-4 hover:bg-[#1f2336]/80 transition-colors">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <Link
                          to={`/posts/${post.id}`}
                          className="text-white hover:text-purple-300 font-medium block mb-2"
                        >
                          {post.title}
                          {post.isBlinded && <span className="text-red-400 ml-2">(블라인드)</span>}
                        </Link>
                        <p className="text-gray-400 text-sm mb-2">
                          {truncateText(post.content.replace(/<[^>]*>/g, ''), 100)}
                        </p>
                        <div className="flex items-center gap-4 text-xs text-gray-500">
                          <span>🗂 {categoryLabels[post.category] || post.category}</span>
                          <span>❤️ {post.likeCount}</span>
                          <span>💬 {post.commentCount}</span>
                          <span>👁 {post.viewCount}</span>
                          <span>📅 {formatDate(post.createdAt)}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'comments' && (
          <div className="space-y-4">
            <h3 className="text-lg font-bold text-white mb-4">💬 내가 작성한 댓글</h3>
            {activity.myComments.length === 0 ? (
              <p className="text-gray-400 text-center py-8">작성한 댓글이 없습니다.</p>
            ) : (
              <div className="space-y-3">
                {activity.myComments.map((comment) => (
                  <div key={comment.id} className="bg-[#1f2336]/60 rounded-lg p-4 hover:bg-[#1f2336]/80 transition-colors">
                    <div className="mb-2">
                      <Link
                        to={`/posts/${comment.postId}`}
                        className="text-purple-300 hover:text-purple-200 text-sm font-medium"
                      >
                        📄 {comment.postTitle}
                      </Link>
                      {comment.parentId && (
                        <span className="text-xs text-gray-500 ml-2">(답글)</span>
                      )}
                    </div>
                    <p className="text-white mb-2">
                      {comment.content}
                      {comment.isBlinded && <span className="text-red-400 ml-2">(블라인드)</span>}
                    </p>
                    <div className="text-xs text-gray-500">
                      📅 {formatDate(comment.createdAt)}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'messages' && (
          <div className="space-y-4">
            {/* 쪽지함 탭 */}
            <div className="flex space-x-1 bg-[#1f2336]/60 rounded-lg p-1">
              <button
                onClick={() => setMessageTab('received')}
                className={`flex-1 py-2 px-4 rounded-md transition-all text-sm ${
                  messageTab === 'received'
                    ? 'bg-blue-600 text-white'
                    : 'text-gray-400 hover:text-white hover:bg-blue-600/20'
                }`}
              >
                📥 받은 쪽지 ({activity.totalReceivedMessageCount})
              </button>
              <button
                onClick={() => setMessageTab('sent')}
                className={`flex-1 py-2 px-4 rounded-md transition-all text-sm ${
                  messageTab === 'sent'
                    ? 'bg-blue-600 text-white'
                    : 'text-gray-400 hover:text-white hover:bg-blue-600/20'
                }`}
              >
                📤 보낸 쪽지 ({activity.totalSentMessageCount})
              </button>
            </div>

            {/* 쪽지 목록 */}
            <div className="space-y-3">
              {messageTab === 'received' ? (
                activity.receivedMessages.messages.length === 0 ? (
                  <p className="text-gray-400 text-center py-8">받은 쪽지가 없습니다.</p>
                ) : (
                  activity.receivedMessages.messages.map((message) => (
                    <div
                      key={message.id}
                      onClick={() => handleMessageClick(message)}
                      className={`bg-[#1f2336]/60 rounded-lg p-4 hover:bg-[#1f2336]/80 transition-colors cursor-pointer ${
                        !message.isRead ? 'border-l-4 border-blue-500' : ''
                      }`}
                    >
                      <div className="flex items-start justify-between mb-2">
                        <h4 className={`font-medium ${
                          !message.isRead ? 'text-white font-bold' : 'text-gray-300'
                        }`}>
                          {message.title}
                          {!message.isRead && <span className="text-blue-400 ml-2">●</span>}
                        </h4>
                        <span className="text-xs text-gray-500">
                          {formatDate(message.createdAt)}
                        </span>
                      </div>
                      <p className="text-sm text-gray-400 mb-2">
                        보낸이: {message.senderNickname}
                      </p>
                      <p className="text-sm text-gray-300">
                        {truncateText(message.content, 100)}
                      </p>
                    </div>
                  ))
                )
              ) : (
                activity.sentMessages.messages.length === 0 ? (
                  <p className="text-gray-400 text-center py-8">보낸 쪽지가 없습니다.</p>
                ) : (
                  activity.sentMessages.messages.map((message) => (
                    <div
                      key={message.id}
                      onClick={() => handleMessageClick(message)}
                      className="bg-[#1f2336]/60 rounded-lg p-4 hover:bg-[#1f2336]/80 transition-colors cursor-pointer"
                    >
                      <div className="flex items-start justify-between mb-2">
                        <h4 className="text-gray-300 font-medium">
                          {message.title}
                        </h4>
                        <span className="text-xs text-gray-500">
                          {formatDate(message.createdAt)}
                        </span>
                      </div>
                      <p className="text-sm text-gray-400 mb-2">
                        받는이: {message.receiverNickname}
                      </p>
                      <p className="text-sm text-gray-300">
                        {truncateText(message.content, 100)}
                      </p>
                      <div className="text-xs text-gray-500 mt-2">
                        {message.isRead ? (
                          <span className="text-green-400">✓ 읽음 ({formatDate(message.readAt!)})</span>
                        ) : (
                          <span className="text-gray-400">○ 읽지 않음</span>
                        )}
                      </div>
                    </div>
                  ))
                )
              )}
            </div>
          </div>
        )}
      </div>

      {/* 쪽지 상세 모달 */}
      {selectedMessage && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-[#1f2336]/95 backdrop-blur-md rounded-xl border border-purple-500/20 w-full max-w-2xl max-h-[80vh] overflow-y-auto">
            <div className="flex items-center justify-between p-6 border-b border-purple-500/20">
              <h2 className="text-xl font-bold text-white">📩 쪽지 상세</h2>
              <button
                onClick={() => setSelectedMessage(null)}
                className="p-2 hover:bg-purple-600/20 rounded-lg transition-colors text-gray-400 hover:text-white"
              >
                ✕
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <h3 className="text-lg font-bold text-white mb-2">{selectedMessage.title}</h3>
                <div className="text-sm text-gray-400 space-y-1">
                  <p>보낸이: {selectedMessage.senderNickname}</p>
                  <p>받는이: {selectedMessage.receiverNickname}</p>
                  <p>보낸 시간: {formatDate(selectedMessage.createdAt)}</p>
                  {selectedMessage.isRead && selectedMessage.readAt && (
                    <p>읽은 시간: {formatDate(selectedMessage.readAt)}</p>
                  )}
                </div>
              </div>
              <hr className="border-gray-600" />
              <div className="text-white whitespace-pre-wrap">
                {selectedMessage.content}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}