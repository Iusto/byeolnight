import React, { useEffect, useState, useCallback } from 'react';
import axios from '../../lib/axios';

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

const AdminChatTable: React.FC = () => {
  const [stats, setStats] = useState<AdminChatStats | null>(null);
  const [bannedUsers, setBannedUsers] = useState<ChatBanInfo[]>([]);
  const [blindedMessages, setBlindedMessages] = useState<BlindedMessage[]>([]);
  const [loading, setLoading] = useState(true);

  
  // 필터 및 정렬 상태
  const [activeTab, setActiveTab] = useState<'banned' | 'blinded'>('banned');
  const [sortOrder, setSortOrder] = useState<'newest' | 'oldest'>('newest');
  const [userFilter, setUserFilter] = useState('');
  const [reasonFilter, setReasonFilter] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');

  useEffect(() => {
    loadAdminData();
  }, []);



  const loadAdminData = async () => {
    try {
      const [statsRes, bannedRes, blindedRes] = await Promise.all([
        axios.get('/admin/chat/stats'),
        axios.get('/admin/chat/banned-users?limit=1000&offset=0'),
        axios.get('/admin/chat/blinded-messages?limit=1000&offset=0')
      ]);

      setStats(statsRes.data);
      setBannedUsers(bannedRes.data);
      setBlindedMessages(blindedRes.data);
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
      if (stats) {
        setStats(prev => prev ? { ...prev, bannedUsers: Math.max(0, prev.bannedUsers - 1) } : prev);
      }
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
      if (stats) {
        setStats(prev => prev ? { ...prev, blindedMessages: Math.max(0, prev.blindedMessages - 1) } : prev);
      }
    } catch (error) {
      console.error('메시지 블라인드 해제 실패:', error);
      alert('메시지 블라인드 해제에 실패했습니다.');
    }
  };

  // 필터링 및 정렬 로직
  const getFilteredBannedUsers = useCallback(() => {
    let filtered = bannedUsers.filter(user => {
      const matchesUser = !userFilter || user.username.toLowerCase().includes(userFilter.toLowerCase());
      const matchesReason = !reasonFilter || (user.reason && user.reason.toLowerCase().includes(reasonFilter.toLowerCase()));
      return matchesUser && matchesReason;
    });

    return filtered.sort((a, b) => {
      const dateA = new Date(a.bannedUntil).getTime();
      const dateB = new Date(b.bannedUntil).getTime();
      return sortOrder === 'newest' ? dateB - dateA : dateA - dateB;
    });
  }, [bannedUsers, userFilter, reasonFilter, sortOrder]);

  const getFilteredBlindedMessages = useCallback(() => {
    let filtered = blindedMessages.filter(msg => {
      const matchesUser = !userFilter || msg.sender.toLowerCase().includes(userFilter.toLowerCase());
      const matchesKeyword = !searchKeyword || msg.originalMessage.toLowerCase().includes(searchKeyword.toLowerCase());
      const matchesReason = !reasonFilter || (msg.reason && msg.reason.toLowerCase().includes(reasonFilter.toLowerCase()));
      return matchesUser && matchesKeyword && matchesReason;
    });

    return filtered.sort((a, b) => {
      const dateA = new Date(a.blindedAt).getTime();
      const dateB = new Date(b.blindedAt).getTime();
      return sortOrder === 'newest' ? dateB - dateA : dateA - dateB;
    });
  }, [blindedMessages, userFilter, searchKeyword, reasonFilter, sortOrder]);

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

      {/* 탭 및 필터 */}
      <div className="space-y-4">
        {/* 탭 */}
        <div className="flex gap-2">
          <button
            onClick={() => setActiveTab('banned')}
            className={`px-4 py-2 rounded-lg transition-colors ${
              activeTab === 'banned' 
                ? 'bg-red-600 text-white' 
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            🚫 제재된 사용자 ({bannedUsers.length})
          </button>
          <button
            onClick={() => setActiveTab('blinded')}
            className={`px-4 py-2 rounded-lg transition-colors ${
              activeTab === 'blinded' 
                ? 'bg-yellow-600 text-white' 
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            🙈 블라인드된 메시지 ({blindedMessages.length})
          </button>
        </div>

        {/* 필터 및 정렬 */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          <select
            value={sortOrder}
            onChange={(e) => setSortOrder(e.target.value as 'newest' | 'oldest')}
            className="bg-black/30 border border-gray-600 rounded px-3 py-2 text-white"
          >
            <option value="newest">📅 최신순</option>
            <option value="oldest">📅 오래된순</option>
          </select>
          
          <input
            type="text"
            placeholder="🙍 작성자 필터"
            value={userFilter}
            onChange={(e) => setUserFilter(e.target.value)}
            className="bg-black/30 border border-gray-600 rounded px-3 py-2 text-white placeholder-gray-400"
          />
          
          {activeTab === 'blinded' && (
            <input
              type="text"
              placeholder="🔍 키워드 검색"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              className="bg-black/30 border border-gray-600 rounded px-3 py-2 text-white placeholder-gray-400"
            />
          )}
          
          <input
            type="text"
            placeholder="⚠️ 사유 필터"
            value={reasonFilter}
            onChange={(e) => setReasonFilter(e.target.value)}
            className="bg-black/30 border border-gray-600 rounded px-3 py-2 text-white placeholder-gray-400"
          />
        </div>
      </div>

      {/* 테이블 */}
      <div className="overflow-x-auto">
        {activeTab === 'banned' ? (
          <table className="w-full bg-black/30 rounded-lg overflow-hidden">
            <thead className="bg-red-900/50">
              <tr>
                <th className="px-4 py-3 text-left text-white">작성자</th>
                <th className="px-4 py-3 text-left text-white">제재 해제</th>
                <th className="px-4 py-3 text-left text-white">사유</th>
                <th className="px-4 py-3 text-center text-white">해제</th>
              </tr>
            </thead>
            <tbody>
              {getFilteredBannedUsers().map((user) => (
                <tr key={user.userId} className="border-t border-gray-600/30 hover:bg-gray-700/30">
                  <td className="px-4 py-3 text-white font-medium">{user.username}</td>
                  <td className="px-4 py-3 text-gray-300 text-sm">
                    {new Date(user.bannedUntil).toLocaleString('ko-KR')}
                  </td>
                  <td className="px-4 py-3 text-yellow-300 text-sm">
                    {user.reason || '사유 없음'}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <button
                      onClick={() => handleUnbanUser(user.userId)}
                      className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded text-sm"
                    >
                      해제
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <table className="w-full bg-black/30 rounded-lg overflow-hidden">
            <thead className="bg-yellow-900/50">
              <tr>
                <th className="px-4 py-3 text-left text-white">작성자</th>
                <th className="px-4 py-3 text-left text-white">내용</th>
                <th className="px-4 py-3 text-left text-white">블라인드 일시</th>
                <th className="px-4 py-3 text-left text-white">사유</th>
                <th className="px-4 py-3 text-center text-white">해제</th>
              </tr>
            </thead>
            <tbody>
              {getFilteredBlindedMessages().map((msg) => (
                <tr key={msg.messageId} className="border-t border-gray-600/30 hover:bg-gray-700/30">
                  <td className="px-4 py-3 text-white font-medium">{msg.sender}</td>
                  <td className="px-4 py-3 text-gray-300 text-sm max-w-xs truncate">
                    {msg.originalMessage}
                  </td>
                  <td className="px-4 py-3 text-gray-300 text-sm">
                    {new Date(msg.blindedAt).toLocaleString('ko-KR')}
                  </td>
                  <td className="px-4 py-3 text-yellow-300 text-sm">
                    {msg.reason || '사유 없음'}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <button
                      onClick={() => handleUnblindMessage(msg.messageId)}
                      className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-sm"
                    >
                      해제
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* 결과 없음 */}
      {((activeTab === 'banned' && getFilteredBannedUsers().length === 0) ||
        (activeTab === 'blinded' && getFilteredBlindedMessages().length === 0)) && (
        <div className="text-center text-gray-400 py-8">
          필터 조건에 맞는 데이터가 없습니다.
        </div>
      )}
    </div>
  );
};

export default AdminChatTable;