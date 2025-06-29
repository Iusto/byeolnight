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
      case 'MESSAGE_UNBLINDED':
      case 'USER_BANNED':
      case 'USER_UNBANNED':
        // 데이터 새로고침
        loadAdminData();
        break;
    }
  };

  const loadAdminData = async () => {
    try {
      const [statsRes, bannedRes, blindedRes] = await Promise.all([
        axios.get('/admin/chat/stats'),
        axios.get('/admin/chat/banned-users'),
        axios.get('/admin/chat/blinded-messages')
      ]);

      setStats(statsRes.data);
      setBannedUsers(bannedRes.data);
      setBlindedMessages(blindedRes.data);
      
      console.log('관리자 대시보드 데이터 로드 완료');
    } catch (error) {
      console.error('관리자 데이터 로드 실패:', error);
      setStats({ totalMessages: 0, blindedMessages: 0, bannedUsers: 0, activeUsers: 0 });
      setBannedUsers([]);
      setBlindedMessages([]);
    } finally {
      setLoading(false);
    }
  };

  const handleUnbanUser = async (userId: string) => {
    if (!confirm('이 사용자의 채팅 금지를 해제하시겠습니까?')) return;

    try {
      await axios.delete(`/admin/chat/ban/${userId}`);
      setBannedUsers(prev => prev.filter(user => user.userId !== userId));
      console.log(`사용자 ${userId} 채팅 금지 해제됨`);
    } catch (error) {
      console.error('채팅 금지 해제 실패:', error);
      alert('채팅 금지 해제에 실패했습니다.');
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
        <h3 className="text-lg font-semibold text-purple-300 mb-3">🚫 제재된 사용자</h3>
        {bannedUsers.length === 0 ? (
          <div className="text-gray-400 text-center py-4">제재된 사용자가 없습니다.</div>
        ) : (
          <div className="space-y-2 max-h-60 overflow-y-auto">
            {bannedUsers.map((user) => (
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
        <h3 className="text-lg font-semibold text-purple-300 mb-3">🙈 블라인드된 메시지</h3>
        {blindedMessages.length === 0 ? (
          <div className="text-gray-400 text-center py-4">블라인드된 메시지가 없습니다.</div>
        ) : (
          <div className="space-y-2 max-h-60 overflow-y-auto">
            {blindedMessages.slice(0, 10).map((msg) => (
              <div key={msg.messageId} className="bg-black/30 p-3 rounded-lg">
                <div className="text-sm text-gray-300 mb-1">
                  원본: <span className="text-white">{msg.originalMessage}</span>
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