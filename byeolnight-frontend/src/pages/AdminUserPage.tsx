import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import IpBlockModal from '../components/IpBlockModal';
import AdminReasonModal from '../components/AdminReasonModal';

interface UserSummary {
  id: number;
  email: string;
  nickname: string;
  phone: string;
  role: string;
  status: 'ACTIVE' | 'BANNED' | 'SUSPENDED' | 'WITHDRAWN';
}

interface BlindedPost {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: {
    nickname: string;
  };
  createdAt: string;
  viewCount: number;
  likeCount: number;
}

export default function AdminUserPage() {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [blockedIps, setBlockedIps] = useState<string[]>([]);
  const [blindedPosts, setBlindedPosts] = useState<BlindedPost[]>([]);
  const [activeTab, setActiveTab] = useState<'users' | 'ips' | 'posts'>('users');
  const [showIpModal, setShowIpModal] = useState(false);
  const [showReasonModal, setShowReasonModal] = useState(false);
  const [modalAction, setModalAction] = useState<{ type: string; userId: number; status?: string } | null>(null);
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'WITHDRAWN'>('ALL');
  const { user: currentUser } = useAuth(); // 현재 로그인한 사용자

  const fetchUsers = async () => {
    try {
      const res = await axios.get('/admin/users');
      // 응답 데이터를 안전하게 처리
      const userData = res.data?.data || res.data || [];
      const userList = Array.isArray(userData) ? userData : [];
      
      // 관리자 계정 필터링 (일반 사용자만 표시)
      const filteredUsers = userList.filter(user => user.role !== 'ADMIN');
      setUsers(filteredUsers);
    } catch (err) {
      console.error('사용자 목록 조회 실패', err);
      setUsers([]); // 오류 시 빈 배열로 설정
    } finally {
      setLoading(false);
    }
  };

  const handleLock = async (id: number) => {
    // 현재 로그인한 사용자 보호
    if (currentUser && currentUser.id === id) {
      alert('자기 자신의 계정은 잠금할 수 없습니다.');
      return;
    }
    
    if (!confirm('정말 이 계정을 잠금하시겠습니까?')) return;
    
    try {
      await axios.patch(`/admin/users/${id}/lock`);
      alert('계정이 잠금되었습니다.');
      fetchUsers();
    } catch (err) {
      console.error('계정 잠금 실패:', err);
      alert('계정 잠금에 실패했습니다.');
    }
  };

  const handleUnlock = async (id: number) => {
    if (!confirm('정말 이 계정의 잠금을 해제하시겠습니까?')) return;
    
    try {
      await axios.patch(`/admin/users/${id}/unlock`);
      alert('계정 잠금이 해제되었습니다.');
      fetchUsers();
    } catch (err) {
      console.error('계정 잠금 해제 실패:', err);
      alert('계정 잠금 해제에 실패했습니다.');
    }
  };

  const handleStatusChange = (id: number, status: string) => {
    // 현재 로그인한 사용자 보호
    if (currentUser && currentUser.id === id) {
      alert('자기 자신의 계정 상태는 변경할 수 없습니다.');
      return;
    }
    
    setModalAction({ type: 'status', userId: id, status });
    setShowReasonModal(true);
  };

  const handleReasonConfirm = async (reason: string) => {
    if (!modalAction) return;
    
    try {
      if (modalAction.type === 'status') {
        await axios.patch(`/admin/users/${modalAction.userId}/status`, { 
          status: modalAction.status, 
          reason 
        });
        alert(`사용자 상태가 ${modalAction.status}로 변경되었습니다.`);
      } else if (modalAction.type === 'delete') {
        await axios.delete(`/admin/users/${modalAction.userId}?reason=${encodeURIComponent(reason)}`);
        alert('사용자가 강제 탈퇴 처리되었습니다.');
      }
      fetchUsers();
    } catch (err) {
      console.error('작업 실패:', err);
      alert('작업에 실패했습니다.');
    } finally {
      setModalAction(null);
    }
  };

  const handleDelete = (id: number) => {
    // 현재 로그인한 사용자 보호
    if (currentUser && currentUser.id === id) {
      alert('자기 자신의 계정은 탈퇴시킬 수 없습니다.');
      return;
    }
    
    if (!confirm('정말 이 사용자를 강제 탈퇴시킬까요? 이 작업은 되돌릴 수 없습니다.')) return;
    
    setModalAction({ type: 'delete', userId: id });
    setShowReasonModal(true);
  };

  useEffect(() => {
    fetchUsers();
    fetchBlockedIps();
    fetchBlindedPosts();
  }, []);

  const fetchBlindedPosts = async () => {
    try {
      const res = await axios.get('/admin/posts/blinded');
      setBlindedPosts(res.data?.data || []);
    } catch (err) {
      console.error('블라인드 게시글 목록 조회 실패', err);
    }
  };

  const handleUnblindPost = async (postId: number) => {
    if (!confirm('정말 이 게시글의 블라인드를 해제하시겠습니까?')) return;
    
    try {
      await axios.patch(`/admin/posts/${postId}/unblind`);
      alert('블라인드가 해제되었습니다.');
      fetchBlindedPosts();
    } catch (err) {
      console.error('블라인드 해제 실패:', err);
      alert('블라인드 해제에 실패했습니다.');
    }
  };

  const handleDeletePost = async (postId: number) => {
    if (!confirm('정말 이 게시글을 완전히 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) return;
    
    try {
      await axios.delete(`/admin/posts/${postId}`);
      alert('게시글이 삭제되었습니다.');
      fetchBlindedPosts();
    } catch (err) {
      console.error('게시글 삭제 실패:', err);
      alert('게시글 삭제에 실패했습니다.');
    }
  };

  const fetchBlockedIps = async () => {
    try {
      const res = await axios.get('/admin/blocked-ips');
      console.log('IP 목록 응답:', res.data); // 디버깅용
      const ipData = res.data?.data || res.data || [];
      setBlockedIps(Array.isArray(ipData) ? ipData : []);
    } catch (err) {
      console.error('차단된 IP 목록 조회 실패', err);
      setBlockedIps([]);
    }
  };

  const handleBlockIp = async (ip: string, duration: number) => {
    try {
      await axios.post('/admin/blocked-ips', {
        ip: ip,
        durationMinutes: duration
      });
      alert(`IP ${ip}가 ${duration}분간 차단되었습니다.`);
      fetchBlockedIps();
    } catch (err) {
      console.error('IP 차단 실패:', err);
      alert('IP 차단에 실패했습니다.');
    }
  };

  const handleUnblockIp = async (ip: string) => {
    if (confirm(`IP ${ip}의 차단을 해제하시겠습니까?`)) {
      try {
        await axios.delete(`/admin/blocked-ips?ip=${encodeURIComponent(ip)}`);
        alert(`IP ${ip}의 차단이 해제되었습니다.`);
        fetchBlockedIps();
      } catch (err) {
        console.error('IP 차단 해제 실패:', err);
        alert('IP 차단 해제에 실패했습니다.');
      }
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white px-6 py-12">
      <div className="max-w-6xl mx-auto">
        <h2 className="text-3xl font-bold mb-8 drop-shadow-glow text-center">🔐 관리자 - 사용자 관리</h2>
        
        {/* 탭 메뉴 */}
        <div className="flex gap-4 mb-6 justify-center">
          <button
            onClick={() => setActiveTab('users')}
            className={`px-6 py-2 rounded transition ${
              activeTab === 'users'
                ? 'bg-purple-600 text-white' 
                : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
            }`}
          >
            👥 사용자 관리
          </button>
          <button
            onClick={() => setActiveTab('posts')}
            className={`px-6 py-2 rounded transition ${
              activeTab === 'posts'
                ? 'bg-purple-600 text-white' 
                : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
            }`}
          >
            🙈 블라인드 게시글
          </button>
          <button
            onClick={() => setActiveTab('ips')}
            className={`px-6 py-2 rounded transition ${
              activeTab === 'ips'
                ? 'bg-purple-600 text-white' 
                : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
            }`}
          >
            🚫 IP 차단 관리
          </button>
        </div>

        {activeTab === 'users' ? (
          // 사용자 관리 섹션
          <div>
            {/* 상태 필터 */}
            <div className="flex gap-2 mb-4 justify-center">
              <button
                onClick={() => setStatusFilter('ALL')}
                className={`px-4 py-2 rounded text-sm transition ${
                  statusFilter === 'ALL'
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                }`}
              >
                전체
              </button>
              <button
                onClick={() => setStatusFilter('ACTIVE')}
                className={`px-4 py-2 rounded text-sm transition ${
                  statusFilter === 'ACTIVE'
                    ? 'bg-green-600 text-white'
                    : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                }`}
              >
                활성 계정
              </button>
              <button
                onClick={() => setStatusFilter('WITHDRAWN')}
                className={`px-4 py-2 rounded text-sm transition ${
                  statusFilter === 'WITHDRAWN'
                    ? 'bg-red-600 text-white'
                    : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                }`}
              >
                탈퇴 계정
              </button>
            </div>
            
            {loading ? (
              <p className="text-center text-gray-400">불러오는 중...</p>
            ) : (
              <table className="w-full text-sm border border-gray-600 bg-[#1f2336]/80 backdrop-blur rounded-xl overflow-hidden">
              <thead className="bg-[#2a2e45] text-gray-300">
                <tr>
                  <th className="p-3">ID</th>
                  <th className="p-3">이메일</th>
                  <th className="p-3">닉네임</th>
                  <th className="p-3">전화번호</th>
                  <th className="p-3">권한</th>
                  <th className="p-3">상태</th>
                  <th className="p-3">조치</th>
                </tr>
              </thead>
              <tbody>
                {users && users.length > 0 ? users
                  .filter(user => {
                    if (statusFilter === 'ALL') return true;
                    if (statusFilter === 'ACTIVE') return user.status !== 'WITHDRAWN';
                    if (statusFilter === 'WITHDRAWN') return user.status === 'WITHDRAWN';
                    return true;
                  })
                  .map((user) => (
                  <tr key={user.id} className="border-t border-gray-700">
                    <td className="p-3 text-center">{user.id}</td>
                    <td className="p-3">{user.email}</td>
                    <td className="p-3">{user.nickname}</td>
                    <td className="p-3">{user.phone}</td>
                    <td className="p-3 text-center">{user.role}</td>
                    <td className="p-3 text-center">{user.status}</td>
                    <td className="p-3 text-center space-x-1">
                      {user.status === 'WITHDRAWN' ? (
                        <span className="text-gray-400 text-xs">탈퇴된 계정</span>
                      ) : user.status === 'ACTIVE' ? (
                        <>
                          <button
                            onClick={() => handleLock(user.id)}
                            className="bg-yellow-500 hover:bg-yellow-600 text-black px-2 py-1 rounded text-xs font-medium"
                          >
                            잠금
                          </button>
                          <button
                            onClick={() => handleStatusChange(user.id, 'SUSPENDED')}
                            className="bg-red-500 hover:bg-red-600 text-white px-2 py-1 rounded text-xs font-medium"
                          >
                            정지
                          </button>
                          <button
                            onClick={() => handleDelete(user.id)}
                            className="bg-gray-600 hover:bg-gray-700 text-white px-2 py-1 rounded text-xs font-medium"
                          >
                            탈퇴
                          </button>
                        </>
                      ) : (
                        <>
                          <button
                            onClick={() => handleUnlock(user.id)}
                            className="bg-green-500 hover:bg-green-600 text-white px-2 py-1 rounded text-xs font-medium"
                          >
                            해제
                          </button>
                          <button
                            onClick={() => handleStatusChange(user.id, 'ACTIVE')}
                            className="bg-blue-500 hover:bg-blue-600 text-white px-2 py-1 rounded text-xs font-medium"
                          >
                            복구
                          </button>
                          <button
                            onClick={() => handleDelete(user.id)}
                            className="bg-gray-600 hover:bg-gray-700 text-white px-2 py-1 rounded text-xs font-medium"
                          >
                            탈퇴
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan={7} className="p-8 text-center text-gray-400">
                      사용자 데이터가 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
              </table>
            )}
          </div>
        ) : activeTab === 'posts' ? (
          // 블라인드 게시글 관리 섹션
          <div className="bg-[#1f2336]/80 backdrop-blur rounded-xl p-6">
            <h3 className="text-xl font-semibold text-white mb-6">🙈 블라인드 처리된 게시글</h3>
            
            {blindedPosts.length === 0 ? (
              <p className="text-center text-gray-400 py-8">블라인드 처리된 게시글이 없습니다.</p>
            ) : (
              <div className="grid gap-4">
                {blindedPosts.map((post) => (
                  <div key={post.id} className="bg-[#2a2e45] p-4 rounded-lg">
                    <div className="flex justify-between items-start mb-3">
                      <div className="flex-1">
                        <h4 className="text-white font-semibold mb-1">{post.title}</h4>
                        <p className="text-gray-400 text-sm mb-2">
                          작성자: {post.writer.nickname} | 카테고리: {post.category} | 조회: {post.viewCount} | 추천: {post.likeCount}
                        </p>
                        <p className="text-gray-300 text-sm line-clamp-2">
                          {post.content.length > 100 ? post.content.substring(0, 100) + '...' : post.content}
                        </p>
                      </div>
                      <div className="flex gap-2 ml-4">
                        <button
                          onClick={() => handleUnblindPost(post.id)}
                          className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded text-sm font-medium transition"
                        >
                          블라인드 해제
                        </button>
                        <button
                          onClick={() => handleDeletePost(post.id)}
                          className="bg-red-600 hover:bg-red-700 text-white px-3 py-1 rounded text-sm font-medium transition"
                        >
                          완전 삭제
                        </button>
                      </div>
                    </div>
                    <p className="text-gray-500 text-xs">
                      블라인드 시간: {new Date(post.createdAt).toLocaleString()}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : (
          // IP 차단 관리 섹션
          <div className="bg-[#1f2336]/80 backdrop-blur rounded-xl p-6">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-semibold text-white">🚫 차단된 IP 목록</h3>
              <button
                onClick={() => setShowIpModal(true)}
                className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded font-medium transition"
              >
                + IP 차단 추가
              </button>
            </div>
            
            {blockedIps.length === 0 ? (
              <p className="text-center text-gray-400 py-8">차단된 IP가 없습니다.</p>
            ) : (
              <div className="grid gap-3">
                {blockedIps.map((ip) => (
                  <div key={ip} className="flex justify-between items-center bg-[#2a2e45] p-4 rounded-lg">
                    <div>
                      <span className="text-white font-mono">{ip}</span>
                      <span className="text-gray-400 text-sm ml-2">차단됨</span>
                    </div>
                    <button
                      onClick={() => handleUnblockIp(ip)}
                      className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded text-sm font-medium transition"
                    >
                      차단 해제
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
        
        {/* IP 차단 모달 */}
        <IpBlockModal 
          isOpen={showIpModal}
          onClose={() => setShowIpModal(false)}
          onConfirm={handleBlockIp}
        />
        
        {/* 사유 입력 모달 */}
        <AdminReasonModal 
          isOpen={showReasonModal}
          onClose={() => {
            setShowReasonModal(false);
            setModalAction(null);
          }}
          onConfirm={handleReasonConfirm}
          title={
            modalAction?.type === 'status' 
              ? `사용자 상태 변경 (${modalAction.status})`
              : '사용자 강제 탈퇴'
          }
          placeholder={
            modalAction?.type === 'status'
              ? `상태 변경 사유를 입력하세요...`
              : '탈퇴 사유를 입력하세요...'
          }
        />
      </div>
    </div>
  );
}
