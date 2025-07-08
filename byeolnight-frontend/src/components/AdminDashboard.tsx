import React, { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// 관리자 기능 타입 정의
interface AdminChatStats {
  totalMessages: number;
  blindedMessages: number;
  bannedUsers: number;
  activeUsers: number;
}

interface ChatBanInfo {
  userId: string;
  username: string;
  bannedUntil: string;
  reason?: string;
  bannedBy: string;
}

interface BlindedMessage {
  messageId: string;
  originalMessage: string;
  sender: string;
  blindedBy: string;
  blindedAt: string;
  reason?: string;
}

const AdminDashboard: React.FC = () => {
  const [stats, setStats] = useState<AdminChatStats | null>(null);
  const [bannedUsers, setBannedUsers] = useState<ChatBanInfo[]>([]);
  const [blindedMessages, setBlindedMessages] = useState<BlindedMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [stompClient, setStompClient] = useState<Client | null>(null);
  const [showAllBanned, setShowAllBanned] = useState(false);
  const [showAllBlinded, setShowAllBlinded] = useState(false);
  const [allBannedUsers, setAllBannedUsers] = useState<ChatBanInfo[]>([]);
  const [allBlindedMessages, setAllBlindedMessages] = useState<BlindedMessage[]>([]);
  const [loadingMore, setLoadingMore] = useState(false);
  const [bannedSearchTerm, setBannedSearchTerm] = useState('');
  const [blindedSearchTerm, setBlindedSearchTerm] = useState('');
  const [totalBannedCount, setTotalBannedCount] = useState(0);
  const [totalBlindedCount, setTotalBlindedCount] = useState(0);
  const [generatingDiscussion, setGeneratingDiscussion] = useState(false);
  const [generatingCinema, setGeneratingCinema] = useState(false);

  useEffect(() => {
    loadAdminData();
    connectWebSocket();
    
    return () => {
      if (stompClient) {
        stompClient.deactivate();
      }
    };
  }, []);

  const connectWebSocket = () => {
    const socket = new SockJS(import.meta.env.VITE_WS_URL || '/ws');
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${localStorage.getItem('accessToken') || ''}`,
      },
      onConnect: () => {
        // 관리자 알림 구독
        client.subscribe('/topic/admin/chat-update', (message) => {
          const data = JSON.parse(message.body);
          handleAdminUpdate(data);
        });
      },
      onStompError: (error) => {
        console.error('WebSocket 연결 오류:', error);
      }
    });
    
    client.activate();
    setStompClient(client);
  };

  const handleAdminUpdate = (data: any) => {
    switch (data.type) {
      case 'MESSAGE_BLINDED':
        // 블라인드된 메시지를 목록에 추가
        loadAdminData();
        break;
      case 'MESSAGE_UNBLINDED':
        // 블라인드 해제된 메시지를 목록에서 제거
        const messageId = data.messageId;
        setBlindedMessages(prev => prev.filter(msg => msg.messageId !== messageId));
        setAllBlindedMessages(prev => prev.filter(msg => msg.messageId !== messageId));
        break;
      case 'USER_BANNED':
        loadAdminData();
        break;
      case 'USER_UNBANNED':
        // 해제된 사용자를 목록에서 제거
        const username = data.username;
        setBannedUsers(prev => prev.filter(user => user.username !== username));
        setAllBannedUsers(prev => prev.filter(user => user.username !== username));
        break;
    }
  };

  const loadAdminData = async () => {
    try {
      const [statsRes, bannedRes, blindedRes, totalBannedRes, totalBlindedRes] = await Promise.all([
        axios.get('/admin/chat/stats'),
        axios.get('/admin/chat/banned-users?limit=5&offset=0'),
        axios.get('/admin/chat/blinded-messages?limit=5&offset=0'),
        axios.get('/admin/chat/banned-users?limit=1000&offset=0'),
        axios.get('/admin/chat/blinded-messages?limit=1000&offset=0')
      ]);

      setStats(statsRes.data);
      setBannedUsers(bannedRes.data);
      setBlindedMessages(blindedRes.data);
      setTotalBannedCount(totalBannedRes.data.length);
      setTotalBlindedCount(totalBlindedRes.data.length);
      
      console.log('관리자 대시보드 데이터 로드 완료');
    } catch (error) {
      console.error('관리자 데이터 로드 실패:', error);
      setStats({ totalMessages: 0, blindedMessages: 0, bannedUsers: 0, activeUsers: 0 });
      setBannedUsers([]);
      setBlindedMessages([]);
      setTotalBannedCount(0);
      setTotalBlindedCount(0);
    } finally {
      setLoading(false);
    }
  };

  const loadAllBannedUsers = async () => {
    if (allBannedUsers.length > 0) return;
    
    setLoadingMore(true);
    try {
      const response = await axios.get('/admin/chat/banned-users?limit=1000&offset=0');
      setAllBannedUsers(response.data);
    } catch (error) {
      console.error('전체 제재 사용자 로드 실패:', error);
    } finally {
      setLoadingMore(false);
    }
  };

  const loadAllBlindedMessages = async () => {
    if (allBlindedMessages.length > 0) return;
    
    setLoadingMore(true);
    try {
      const response = await axios.get('/admin/chat/blinded-messages?limit=1000&offset=0');
      setAllBlindedMessages(response.data);
    } catch (error) {
      console.error('전체 블라인드 메시지 로드 실패:', error);
    } finally {
      setLoadingMore(false);
    }
  };

  const handleShowAllBanned = async () => {
    if (!showAllBanned) {
      await loadAllBannedUsers();
    }
    setShowAllBanned(!showAllBanned);
  };

  const handleShowAllBlinded = async () => {
    if (!showAllBlinded) {
      await loadAllBlindedMessages();
    }
    setShowAllBlinded(!showAllBlinded);
  };

  const handleUnbanUser = async (userId: string) => {
    if (!confirm('이 사용자의 채팅 금지를 해제하시겠습니까?')) return;

    try {
      await axios.delete(`/admin/chat/ban/${userId}`);
      setBannedUsers(prev => prev.filter(user => user.userId !== userId));
      setAllBannedUsers(prev => prev.filter(user => user.userId !== userId));
      setTotalBannedCount(prev => Math.max(0, prev - 1));
      // 통계 업데이트
      if (stats) {
        setStats(prev => prev ? { ...prev, bannedUsers: Math.max(0, prev.bannedUsers - 1) } : prev);
      }
      console.log(`사용자 ${userId} 채팅 금지 해제됨`);
    } catch (error) {
      console.error('채팅 금지 해제 실패:', error);
      alert('채팅 금지 해제에 실패했습니다.');
    }
  };

  const handleUnblindMessage = async (messageId: string) => {
    if (!confirm('이 메시지의 블라인드를 해제하시겠습니까?')) return;

    try {
      await axios.delete(`/admin/chat/blind/${messageId}`);
      setBlindedMessages(prev => prev.filter(msg => msg.messageId !== messageId));
      setAllBlindedMessages(prev => prev.filter(msg => msg.messageId !== messageId));
      setTotalBlindedCount(prev => Math.max(0, prev - 1));
      // 통계 업데이트
      if (stats) {
        setStats(prev => prev ? { ...prev, blindedMessages: Math.max(0, prev.blindedMessages - 1) } : prev);
      }
      console.log(`메시지 ${messageId} 블라인드 해제됨`);
    } catch (error) {
      console.error('메시지 블라인드 해제 실패:', error);
      alert('메시지 블라인드 해제에 실패했습니다.');
    }
  };

  const handleGenerateDiscussion = async () => {
    if (!confirm('토론 주제를 생성하시겠습니까?')) return;
    
    setGeneratingDiscussion(true);
    try {
      await axios.post('/admin/discussions/generate-topic');
      alert('토론 주제가 성공적으로 생성되었습니다!');
    } catch (error) {
      console.error('토론 주제 생성 실패:', error);
      alert('토론 주제 생성에 실패했습니다.');
    } finally {
      setGeneratingDiscussion(false);
    }
  };

  const handleGenerateCinema = async () => {
    if (!confirm('별빛 시네마 포스트를 생성하시겠습니까?')) return;
    
    setGeneratingCinema(true);
    try {
      await axios.post('/admin/cinema/generate-post');
      alert('별빛 시네마 포스트가 성공적으로 생성되었습니다!');
    } catch (error) {
      console.error('별빛 시네마 생성 실패:', error);
      alert('별빛 시네마 생성에 실패했습니다.');
    } finally {
      setGeneratingCinema(false);
    }
  };

  if (loading) {
    return (
      <div className="bg-[#1f2336]/70 backdrop-blur-md p-6 rounded-xl">
        <div className="text-center text-gray-400">
          <div className="animate-spin w-8 h-8 border-2 border-purple-500 border-t-transparent rounded-full mx-auto mb-2"></div>
          관리자 데이터 로딩 중...
        </div>
      </div>
    );
  }

  return (
    <div className="bg-[#1f2336]/70 backdrop-blur-md p-6 rounded-xl space-y-6">
      <h2 className="text-xl font-bold text-purple-300 mb-4">🛡️ 채팅 관리 대시보드</h2>
      
      {/* AI 콘텐츠 생성 버튼 */}
      <div className="bg-black/30 p-4 rounded-lg">
        <h3 className="text-lg font-semibold text-purple-300 mb-3">🤖 AI 콘텐츠 생성</h3>
        <div className="flex gap-3">
          <button
            onClick={handleGenerateDiscussion}
            disabled={generatingDiscussion}
            className="bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2"
          >
            {generatingDiscussion ? (
              <>
                <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
                생성 중...
              </>
            ) : (
              <>
                💬 토론 주제 생성
              </>
            )}
          </button>
          <button
            onClick={handleGenerateCinema}
            disabled={generatingCinema}
            className="bg-purple-600 hover:bg-purple-700 disabled:bg-gray-600 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2"
          >
            {generatingCinema ? (
              <>
                <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
                생성 중...
              </>
            ) : (
              <>
                🎬 별빛 시네마 생성
              </>
            )}
          </button>
        </div>
      </div>

      {/* 통계 */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-black/30 p-4 rounded-lg text-center">
            <div className="text-2xl font-bold text-blue-400">{stats.totalMessages}</div>
            <div className="text-sm text-gray-400">총 메시지</div>
          </div>
          <div className="bg-black/30 p-4 rounded-lg text-center">
            <div className="text-2xl font-bold text-yellow-400">{stats.blindedMessages}</div>
            <div className="text-sm text-gray-400">블라인드 메시지</div>
          </div>
          <div className="bg-black/30 p-4 rounded-lg text-center">
            <div className="text-2xl font-bold text-red-400">{stats.bannedUsers}</div>
            <div className="text-sm text-gray-400">제재 사용자</div>
          </div>
          <div className="bg-black/30 p-4 rounded-lg text-center">
            <div className="text-2xl font-bold text-green-400">{stats.activeUsers}</div>
            <div className="text-sm text-gray-400">활성 사용자</div>
          </div>
        </div>
      )}

      {/* 제재된 사용자 목록 */}
      <div>
        <div className="flex justify-between items-center mb-3">
          <h3 className="text-lg font-semibold text-purple-300">🚫 제재된 사용자 ({totalBannedCount})</h3>
          {totalBannedCount > 5 && (
            <button
              onClick={handleShowAllBanned}
              disabled={loadingMore}
              className="text-sm text-purple-400 hover:text-purple-300 transition-colors disabled:opacity-50"
            >
              {loadingMore ? '로딩...' : showAllBanned ? '접기' : '더보기'}
            </button>
          )}
        </div>
        {/* 검색 입력 */}
        <div className="mb-3">
          <input
            type="text"
            placeholder="닉네임으로 검색..."
            value={bannedSearchTerm}
            onChange={(e) => setBannedSearchTerm(e.target.value)}
            className="w-full px-3 py-2 bg-black/30 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-purple-500"
          />
        </div>
        {bannedUsers.length === 0 ? (
          <div className="text-gray-400 text-center py-4">제재된 사용자가 없습니다.</div>
        ) : (
          <div className="space-y-2 max-h-60 overflow-y-auto">
            {(showAllBanned ? allBannedUsers : bannedUsers)
              .filter(user => user.username.toLowerCase().includes(bannedSearchTerm.toLowerCase()))
              .map((user) => (
              <div key={user.userId} className="bg-black/30 p-3 rounded-lg flex justify-between items-center">
                <div>
                  <div className="font-semibold text-white">{user.username}</div>
                  <div className="text-sm text-gray-400">
                    제재 해제: {new Date(user.bannedUntil).toLocaleString('ko-KR')}
                  </div>
                  {user.reason && (
                    <div className="text-sm text-yellow-300">사유: {user.reason}</div>
                  )}
                </div>
                <button
                  onClick={() => handleUnbanUser(user.userId)}
                  className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded text-sm"
                >
                  해제
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 블라인드된 메시지 목록 */}
      <div>
        <div className="flex justify-between items-center mb-3">
          <h3 className="text-lg font-semibold text-purple-300">🙈 블라인드된 메시지 ({totalBlindedCount})</h3>
          {totalBlindedCount > 5 && (
            <button
              onClick={handleShowAllBlinded}
              disabled={loadingMore}
              className="text-sm text-purple-400 hover:text-purple-300 transition-colors disabled:opacity-50"
            >
              {loadingMore ? '로딩...' : showAllBlinded ? '접기' : '더보기'}
            </button>
          )}
        </div>
        {/* 검색 입력 */}
        <div className="mb-3">
          <input
            type="text"
            placeholder="작성자 또는 메시지 내용으로 검색..."
            value={blindedSearchTerm}
            onChange={(e) => setBlindedSearchTerm(e.target.value)}
            className="w-full px-3 py-2 bg-black/30 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-purple-500"
          />
        </div>
        {blindedMessages.length === 0 ? (
          <div className="text-gray-400 text-center py-4">블라인드된 메시지가 없습니다.</div>
        ) : (
          <div className="space-y-2 max-h-60 overflow-y-auto">
            {(showAllBlinded ? allBlindedMessages : blindedMessages)
              .filter(msg => 
                msg.sender.toLowerCase().includes(blindedSearchTerm.toLowerCase()) ||
                msg.originalMessage.toLowerCase().includes(blindedSearchTerm.toLowerCase())
              )
              .map((msg) => (
              <div key={msg.messageId} className="bg-black/30 p-3 rounded-lg">
                <div className="flex justify-between items-start mb-2">
                  <div className="text-sm text-gray-300 flex-1">
                    <div className="mb-1">
                      <span className="text-purple-300">작성자:</span> <span className="text-white">{msg.sender}</span>
                    </div>
                    <div>
                      <span className="text-purple-300">원본:</span> <span className="text-white">{msg.originalMessage}</span>
                    </div>
                  </div>
                  <button
                    onClick={() => handleUnblindMessage(msg.messageId)}
                    className="bg-blue-600 hover:bg-blue-700 text-white px-2 py-1 rounded text-xs ml-2 flex-shrink-0"
                  >
                    해제
                  </button>
                </div>
                <div className="text-xs text-gray-400">
                  블라인드: {new Date(msg.blindedAt).toLocaleString('ko-KR')} | 
                  관리자: {msg.blindedBy}
                </div>
                {msg.reason && (
                  <div className="text-xs text-yellow-300">사유: {msg.reason}</div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard;