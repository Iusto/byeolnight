import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import IpBlockModal from '../components/IpBlockModal';
import AdminReasonModal from '../components/AdminReasonModal';
import PointAwardModal from '../components/PointAwardModal';
import ReportDetailModal from '../components/ReportDetailModal';

interface UserSummary {
  id: number;
  email: string;
  nickname: string;
  role: string;
  status: 'ACTIVE' | 'BANNED' | 'SUSPENDED' | 'WITHDRAWN';
  accountLocked: boolean;
  points: number;
}

interface BlindedPost {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  createdAt: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
}

interface ReportedPost {
  id: number;
  title: string;
  writer: string;
  category: string;
  reportCount: number;
  blinded: boolean;
  createdAt: string;
  reportReasons: string[];
  reportDetails: {
    reportId: number;
    reporterNickname: string;
    reason: string;
    description?: string;
    reviewed: boolean;
    accepted?: boolean;
    reportedAt: string;
  }[];
}

export default function AdminUserPage() {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [blockedIps, setBlockedIps] = useState<string[]>([]);
  const [blindedPosts, setBlindedPosts] = useState<BlindedPost[]>([]);
  const [reportedPosts, setReportedPosts] = useState<ReportedPost[]>([]);
  const [blindedComments, setBlindedComments] = useState<any[]>([]);
  const [deletedPosts, setDeletedPosts] = useState<any[]>([]);
  const [deletedComments, setDeletedComments] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState<'users' | 'ips' | 'posts' | 'reports' | 'blindComments' | 'deletedPosts' | 'deletedComments'>('users');
  const [showIpModal, setShowIpModal] = useState(false);
  const [showReasonModal, setShowReasonModal] = useState(false);
  const [showPointModal, setShowPointModal] = useState(false);
  const [showReportModal, setShowReportModal] = useState(false);
  const [selectedReportPost, setSelectedReportPost] = useState<ReportedPost | null>(null);
  const [modalAction, setModalAction] = useState<{ type: string; userId: number; status?: string } | null>(null);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'WITHDRAWN'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [postSearchTerm, setPostSearchTerm] = useState('');
  const [ipSearchTerm, setIpSearchTerm] = useState('');
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

  const handleAwardPoints = async (points: number, reason: string) => {
    if (!selectedUserId) return;
    
    try {
      const response = await axios.post(`/admin/users/${selectedUserId}/points`, {
        points: points,
        reason: reason
      });
      
      console.log('포인트 수여 응답:', response.data); // 디버깅용
      
      // 응답 상태 코드가 200대이면 성공으로 처리
      if (response.status >= 200 && response.status < 300) {
        const message = response.data?.data || response.data?.message || `${points}포인트가 성공적으로 수여되었습니다.`;
        alert(message);
        fetchUsers(); // 사용자 목록 새로고침
      } else {
        const errorMessage = response.data?.message || '포인트 수여에 실패했습니다.';
        alert(errorMessage);
      }
    } catch (error: any) {
      console.error('포인트 수여 실패:', error);
      const errorMessage = error.response?.data?.message || '포인트 수여에 실패했습니다.';
      alert(errorMessage);
    } finally {
      setShowPointModal(false);
      setSelectedUserId(null);
    }
  };

  const handleApproveReport = async (reportId: number) => {
    try {
      await axios.patch(`/admin/reports/${reportId}/approve`);
      alert('신고가 승인되었습니다. 신고자들에게 포인트가 지급되었습니다.');
      fetchReportedPosts();
    } catch (error) {
      console.error('신고 승인 실패:', error);
      alert('신고 승인에 실패했습니다.');
    }
  };

  const handleRejectReport = async (reportId: number, reason: string) => {
    try {
      await axios.patch(`/admin/reports/${reportId}/reject`, { reason });
      alert('신고가 거부되었습니다.');
      fetchReportedPosts();
    } catch (error) {
      console.error('신고 거부 실패:', error);
      alert('신고 거부에 실패했습니다.');
    }
  };

  useEffect(() => {
    fetchUsers();
    fetchBlockedIps();
    fetchBlindedPosts();
    fetchReportedPosts();
    fetchBlindedComments();
    fetchDeletedPosts();
    fetchDeletedComments();
  }, []);

  const fetchBlindedPosts = async () => {
    try {
      const res = await axios.get('/admin/posts/blinded');
      const postsData = Array.isArray(res.data) ? res.data : (res.data?.data || res.data || []);
      setBlindedPosts(postsData);
    } catch (err) {
      console.error('블라인드 게시글 목록 조회 실패', err);
    }
  };

  const fetchReportedPosts = async () => {
    try {
      const res = await axios.get('/admin/posts/reported');
      console.log('신고된 게시글 API 응답:', res.data);
      const postsData = Array.isArray(res.data) ? res.data : (res.data?.data || res.data || []);
      console.log('신고된 게시글 데이터:', postsData);
      setReportedPosts(postsData);
    } catch (err) {
      console.error('신고된 게시글 목록 조회 실패', err);
    }
  };

  const fetchBlindedComments = async () => {
    try {
      const res = await axios.get('/admin/comments/blinded');
      const commentsData = Array.isArray(res.data) ? res.data : (res.data?.data || res.data || []);
      setBlindedComments(commentsData);
    } catch (err) {
      console.error('블라인드 댓글 목록 조회 실패', err);
    }
  };

  const fetchDeletedPosts = async () => {
    try {
      const res = await axios.get('/admin/posts/deleted');
      const postsData = Array.isArray(res.data) ? res.data : (res.data?.data || res.data || []);
      setDeletedPosts(postsData);
    } catch (err) {
      console.error('삭제된 게시글 목록 조회 실패', err);
    }
  };

  const fetchDeletedComments = async () => {
    try {
      const res = await axios.get('/admin/comments/deleted');
      const commentsData = Array.isArray(res.data) ? res.data : (res.data?.data || res.data || []);
      setDeletedComments(commentsData);
    } catch (err) {
      console.error('삭제된 댓글 목록 조회 실패', err);
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
      fetchReportedPosts();
    } catch (err) {
      console.error('게시글 삭제 실패:', err);
      alert('게시글 삭제에 실패했습니다.');
    }
  };

  const handleBlindPost = async (postId: number) => {
    if (!confirm('이 게시글을 블라인드 처리하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/posts/${postId}/blind`);
      alert('블라인드 처리되었습니다.');
      fetchReportedPosts();
      fetchBlindedPosts();
    } catch (err) {
      console.error('블라인드 처리 실패:', err);
      alert('블라인드 처리에 실패했습니다.');
    }
  };

  const handleUnblindReportedPost = async (postId: number) => {
    if (!confirm('이 게시글의 블라인드를 해제하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/posts/${postId}/unblind`);
      alert('블라인드가 해제되었습니다.');
      fetchReportedPosts();
      fetchBlindedPosts();
    } catch (err) {
      console.error('블라인드 해제 실패:', err);
      alert('블라인드 해제에 실패했습니다.');
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

  const handleUnblindComment = async (commentId: number) => {
    if (!confirm('정말 이 댓글의 블라인드를 해제하시겠습니까?')) return;
    
    try {
      await axios.patch(`/admin/comments/${commentId}/unblind`);
      alert('댓글 블라인드가 해제되었습니다.');
      fetchBlindedComments();
    } catch (err) {
      console.error('댓글 블라인드 해제 실패:', err);
      alert('댓글 블라인드 해제에 실패했습니다.');
    }
  };

  const handleRestorePost = async (postId: number) => {
    if (!confirm('정말 이 게시글을 복구하시겠습니까?')) return;
    
    try {
      await axios.patch(`/admin/posts/${postId}/restore`);
      alert('게시글이 복구되었습니다.');
      fetchDeletedPosts();
    } catch (err) {
      console.error('게시글 복구 실패:', err);
      alert('게시글 복구에 실패했습니다.');
    }
  };

  const handleRestoreComment = async (commentId: number) => {
    if (!confirm('정말 이 댓글을 복구하시겠습니까?')) return;
    
    try {
      await axios.patch(`/admin/comments/${commentId}/restore`);
      alert('댓글이 복구되었습니다.');
      fetchDeletedComments();
    } catch (err) {
      console.error('댓글 복구 실패:', err);
      alert('댓글 복구에 실패했습니다.');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white px-6 py-12">
      <div className="max-w-6xl mx-auto">
        <h2 className="text-3xl font-bold mb-8 drop-shadow-glow text-center">🔐 관리자 - 사용자 관리</h2>
        
        {/* 관리자 메뉴 카드 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 mb-8">
          <button
            onClick={() => setActiveTab('users')}
            className={`p-6 rounded-xl border-2 transition-all duration-200 ${
              activeTab === 'users'
                ? 'bg-purple-600/40 border-purple-400 text-white shadow-lg transform scale-105'
                : 'bg-[#1f2336]/80 border-gray-600/50 text-gray-300 hover:bg-[#252842]/80 hover:border-purple-500/50'
            }`}
          >
            <div className="text-3xl mb-2">👥</div>
            <div className="font-semibold">사용자 관리</div>
            <div className="text-sm text-gray-400 mt-1">계정 상태 및 권한 관리</div>
          </button>
          
          <button
            onClick={() => setActiveTab('posts')}
            className={`p-6 rounded-xl border-2 transition-all duration-200 ${
              activeTab === 'posts'
                ? 'bg-yellow-600/40 border-yellow-400 text-white shadow-lg transform scale-105'
                : 'bg-[#1f2336]/80 border-gray-600/50 text-gray-300 hover:bg-[#252842]/80 hover:border-yellow-500/50'
            }`}
          >
            <div className="text-3xl mb-2">🙈</div>
            <div className="font-semibold">블라인드 게시글</div>
            <div className="text-sm text-gray-400 mt-1">블라인드 처리된 게시글 관리</div>
          </button>
          
          <button
            onClick={() => setActiveTab('reports')}
            className={`p-6 rounded-xl border-2 transition-all duration-200 ${
              activeTab === 'reports'
                ? 'bg-orange-600/40 border-orange-400 text-white shadow-lg transform scale-105'
                : 'bg-[#1f2336]/80 border-gray-600/50 text-gray-300 hover:bg-[#252842]/80 hover:border-orange-500/50'
            }`}
          >
            <div className="text-3xl mb-2">🚨</div>
            <div className="font-semibold">신고된 게시글</div>
            <div className="text-sm text-gray-400 mt-1">신고된 게시글 관리</div>
          </button>
          
          <button
            onClick={() => setActiveTab('ips')}
            className={`p-6 rounded-xl border-2 transition-all duration-200 ${
              activeTab === 'ips'
                ? 'bg-red-600/40 border-red-400 text-white shadow-lg transform scale-105'
                : 'bg-[#1f2336]/80 border-gray-600/50 text-gray-300 hover:bg-[#252842]/80 hover:border-red-500/50'
            }`}
          >
            <div className="text-3xl mb-2">🚫</div>
            <div className="font-semibold">IP 차단 관리</div>
            <div className="text-sm text-gray-400 mt-1">IP 주소 차단 및 해제</div>
          </button>
          
          <button
            onClick={() => setActiveTab('blindComments')}
            className={`p-6 rounded-xl border-2 transition-all duration-200 ${
              activeTab === 'blindComments'
                ? 'bg-gray-600/40 border-gray-400 text-white shadow-lg transform scale-105'
                : 'bg-[#1f2336]/80 border-gray-600/50 text-gray-300 hover:bg-[#252842]/80 hover:border-gray-500/50'
            }`}
          >
            <div className="text-3xl mb-2">💬</div>
            <div className="font-semibold">블라인드 댓글</div>
            <div className="text-sm text-gray-400 mt-1">블라인드 처리된 댓글 관리</div>
          </button>
          
          <button
            onClick={() => setActiveTab('deletedPosts')}
            className={`p-6 rounded-xl border-2 transition-all duration-200 ${
              activeTab === 'deletedPosts'
                ? 'bg-red-600/40 border-red-400 text-white shadow-lg transform scale-105'
                : 'bg-[#1f2336]/80 border-gray-600/50 text-gray-300 hover:bg-[#252842]/80 hover:border-red-500/50'
            }`}
          >
            <div className="text-3xl mb-2">🗑️</div>
            <div className="font-semibold">삭제된 게시글</div>
            <div className="text-sm text-gray-400 mt-1">삭제된 게시글 관리</div>
          </button>
          
          <button
            onClick={() => setActiveTab('deletedComments')}
            className={`p-6 rounded-xl border-2 transition-all duration-200 ${
              activeTab === 'deletedComments'
                ? 'bg-red-600/40 border-red-400 text-white shadow-lg transform scale-105'
                : 'bg-[#1f2336]/80 border-gray-600/50 text-gray-300 hover:bg-[#252842]/80 hover:border-red-500/50'
            }`}
          >
            <div className="text-3xl mb-2">💭</div>
            <div className="font-semibold">삭제된 댓글</div>
            <div className="text-sm text-gray-400 mt-1">삭제된 댓글 관리</div>
          </button>
        </div>

        {activeTab === 'users' ? (
          // 사용자 관리 섹션
          <div>
            {/* 검색 및 필터 */}
            <div className="flex flex-col gap-4 mb-6">
              <div className="flex justify-center">
                <input
                  type="text"
                  placeholder="이메일, 닉네임으로 검색..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-80 px-4 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>
              <div className="flex gap-2 justify-center">
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
                  <th className="p-3">포인트</th>
                  <th className="p-3">조치</th>
                </tr>
              </thead>
              <tbody>
                {users && users.length > 0 ? users
                  .filter(user => {
                    // 상태 필터
                    let statusMatch = true;
                    if (statusFilter === 'ACTIVE') statusMatch = user.status !== 'WITHDRAWN';
                    else if (statusFilter === 'WITHDRAWN') statusMatch = user.status === 'WITHDRAWN';
                    
                    // 검색 필터
                    const searchMatch = searchTerm === '' || 
                      user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
                      user.nickname.toLowerCase().includes(searchTerm.toLowerCase());
                    
                    return statusMatch && searchMatch;
                  })
                  .map((user) => (
                  <tr key={user.id} className="border-t border-gray-700">
                    <td className="p-3 text-center">{user.id}</td>
                    <td className="p-3">{user.email}</td>
                    <td className="p-3">{user.nickname}</td>
                    <td className="p-3">{user.phone}</td>
                    <td className="p-3 text-center">{user.role}</td>
                    <td className="p-3 text-center">{user.status}</td>
                    <td className="p-3 text-center">
                      <div className="flex items-center justify-center gap-2">
                        <span className="text-yellow-400">⭐</span>
                        <span className="font-bold">{user.points?.toLocaleString() || 0}</span>
                        <button
                          onClick={() => {
                            setSelectedUserId(user.id);
                            setShowPointModal(true);
                          }}
                          className="bg-yellow-600 hover:bg-yellow-700 text-white px-2 py-1 rounded text-xs font-medium ml-2"
                        >
                          수여
                        </button>
                      </div>
                    </td>
                    <td className="p-3 text-center space-x-1">
                      {user.status === 'WITHDRAWN' ? (
                        <span className="text-gray-400 text-xs">탈퇴된 계정</span>
                      ) : (
                        <>
                          {/* 잠금/해제 버튼 */}
                          {user.accountLocked ? (
                            <button
                              onClick={() => handleUnlock(user.id)}
                              className="bg-green-500 hover:bg-green-600 text-white px-2 py-1 rounded text-xs font-medium"
                            >
                              잠금해제
                            </button>
                          ) : (
                            <button
                              onClick={() => handleLock(user.id)}
                              className="bg-yellow-500 hover:bg-yellow-600 text-black px-2 py-1 rounded text-xs font-medium"
                            >
                              잠금
                            </button>
                          )}
                          
                          {/* 상태 변경 버튼 */}
                          {user.status === 'ACTIVE' ? (
                            <button
                              onClick={() => handleStatusChange(user.id, 'SUSPENDED')}
                              className="bg-red-500 hover:bg-red-600 text-white px-2 py-1 rounded text-xs font-medium"
                            >
                              정지
                            </button>
                          ) : (
                            <button
                              onClick={() => handleStatusChange(user.id, 'ACTIVE')}
                              className="bg-blue-500 hover:bg-blue-600 text-white px-2 py-1 rounded text-xs font-medium"
                            >
                              복구
                            </button>
                          )}
                          
                          {/* 탈퇴 버튼 */}
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
                    <td colSpan={8} className="p-8 text-center text-gray-400">
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
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-semibold text-white">🙈 블라인드 처리된 게시글</h3>
              <input
                type="text"
                placeholder="제목, 작성자로 검색..."
                value={postSearchTerm}
                onChange={(e) => setPostSearchTerm(e.target.value)}
                className="w-64 px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
            </div>
            
            {blindedPosts.filter(post => 
              postSearchTerm === '' ||
              post.title.toLowerCase().includes(postSearchTerm.toLowerCase()) ||
              post.writer.nickname.toLowerCase().includes(postSearchTerm.toLowerCase())
            ).length === 0 ? (
              <p className="text-center text-gray-400 py-8">블라인드 처리된 게시글이 없습니다.</p>
            ) : (
              <div className="grid gap-4">
                {blindedPosts
                  .filter(post => 
                    postSearchTerm === '' ||
                    post.title.toLowerCase().includes(postSearchTerm.toLowerCase()) ||
                    post.writer.nickname.toLowerCase().includes(postSearchTerm.toLowerCase())
                  )
                  .map((post) => (
                  <div key={post.id} className="bg-[#2a2e45] p-4 rounded-lg">
                    <div className="flex justify-between items-start mb-3">
                      <div className="flex-1">
                        <h4 className="text-white font-semibold mb-1">{post.title}</h4>
                        <p className="text-gray-400 text-sm mb-2">
                          작성자: {post.writer} | 카테고리: {post.category} | 조회: {post.viewCount} | 추천: {post.likeCount} | 댓글: {post.commentCount}
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
        ) : activeTab === 'reports' ? (
          // 신고된 게시글 관리 섹션
          <div className="bg-[#1f2336]/80 backdrop-blur rounded-xl p-6">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-semibold text-white">🚨 신고된 게시글</h3>
              <input
                type="text"
                placeholder="제목, 작성자로 검색..."
                value={postSearchTerm}
                onChange={(e) => setPostSearchTerm(e.target.value)}
                className="w-64 px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
            </div>
            
            {reportedPosts.filter(post => 
              postSearchTerm === '' ||
              post.title.toLowerCase().includes(postSearchTerm.toLowerCase()) ||
              post.writer.toLowerCase().includes(postSearchTerm.toLowerCase())
            ).length === 0 ? (
              <p className="text-center text-gray-400 py-8">신고된 게시글이 없습니다.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full table-fixed">
                  <colgroup>
                    <col className="w-2/5" />
                    <col className="w-20" />
                    <col className="w-16" />
                    <col className="w-20" />
                    <col className="w-16" />
                    <col className="w-20" />
                    <col className="w-24" />
                  </colgroup>
                  <thead className="bg-[#2a2e45]">
                    <tr>
                      <th className="px-4 py-4 text-left">제목</th>
                      <th className="px-3 py-4 text-left">작성자</th>
                      <th className="px-3 py-4 text-left">카테고리</th>
                      <th className="px-3 py-4 text-center">신고수</th>
                      <th className="px-3 py-4 text-center">상태</th>
                      <th className="px-3 py-4 text-center">작성일</th>
                      <th className="px-3 py-4 text-center">관리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reportedPosts
                      .filter(post => 
                        postSearchTerm === '' ||
                        post.title.toLowerCase().includes(postSearchTerm.toLowerCase()) ||
                        post.writer.toLowerCase().includes(postSearchTerm.toLowerCase())
                      )
                      .map((post) => (
                      <tr key={post.id} className="border-b border-gray-600 hover:bg-[#2a2e45]/50">
                        <td className="px-4 py-4">
                          <button
                            onClick={() => window.open(`/posts/${post.id}`, '_blank')}
                            className="text-blue-400 hover:text-blue-300 hover:underline text-left w-full truncate block"
                            title={post.title}
                          >
                            {post.title}
                          </button>
                        </td>
                        <td className="px-3 py-4 truncate">{post.writer}</td>
                        <td className="px-3 py-4 text-xs">{post.category}</td>
                        <td className="px-3 py-4 text-center">
                          <button
                            onClick={() => {
                              console.log('선택된 게시글:', post);
                              console.log('신고 상세 데이터:', post.reportDetails);
                              setSelectedReportPost(post);
                              setShowReportModal(true);
                            }}
                            className={`px-2 py-1 rounded text-xs font-medium hover:scale-105 transition-all duration-200 shadow-md whitespace-nowrap ${
                              post.reportCount >= 5 
                                ? 'bg-red-600 hover:bg-red-700 text-white' 
                                : 'bg-yellow-600 hover:bg-yellow-700 text-white'
                            }`}
                            title="클릭하여 신고 상세 내역 보기"
                          >
                            🚨{post.reportCount || 0}
                          </button>
                        </td>
                        <td className="px-3 py-4 text-center">
                          {post.blinded ? (
                            <span className="px-2 py-1 bg-red-600 rounded text-xs whitespace-nowrap">블라인드</span>
                          ) : (
                            <span className="px-2 py-1 bg-green-600 rounded text-xs whitespace-nowrap">공개</span>
                          )}
                        </td>
                        <td className="px-3 py-4 text-center text-xs text-gray-400">
                          {new Date(post.createdAt).toLocaleDateString()}
                        </td>
                        <td className="px-3 py-4 text-center">
                          <div className="flex gap-1 justify-center">
                            {post.blinded ? (
                              <button
                                onClick={() => handleUnblindReportedPost(post.id)}
                                className="px-2 py-1 bg-green-600 hover:bg-green-700 rounded text-xs whitespace-nowrap"
                              >
                                해제
                              </button>
                            ) : (
                              <button
                                onClick={() => handleBlindPost(post.id)}
                                className="px-2 py-1 bg-orange-600 hover:bg-orange-700 rounded text-xs whitespace-nowrap"
                              >
                                블라인드
                              </button>
                            )}
                            <button
                              onClick={() => handleDeletePost(post.id)}
                              className="px-2 py-1 bg-red-600 hover:bg-red-700 rounded text-xs whitespace-nowrap"
                            >
                              삭제
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        ) : activeTab === 'blindComments' ? (
          // 블라인드 댓글 관리 섹션
          <div className="bg-[#1f2336]/80 backdrop-blur rounded-xl p-6">
            <h3 className="text-xl font-semibold text-white mb-6">💬 블라인드 처리된 댓글</h3>
            {blindedComments.length === 0 ? (
              <p className="text-center text-gray-400 py-8">블라인드 처리된 댓글이 없습니다.</p>
            ) : (
              <div className="space-y-4">
                {blindedComments.map((comment) => (
                  <div key={comment.id} className="bg-[#2a2e45] p-4 rounded-lg">
                    <div className="flex justify-between items-start mb-3">
                      <div className="flex-1">
                        <p className="text-gray-300 mb-2">{comment.content}</p>
                        <p className="text-gray-400 text-sm">
                          작성자: {comment.writer} | 게시글: {comment.postTitle}
                        </p>
                      </div>
                      <button
                        onClick={() => handleUnblindComment(comment.id)}
                        className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded text-sm"
                      >
                        블라인드 해제
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : activeTab === 'deletedPosts' ? (
          // 삭제된 게시글 관리 섹션
          <div className="bg-[#1f2336]/80 backdrop-blur rounded-xl p-6">
            <h3 className="text-xl font-semibold text-white mb-6">🗑️ 삭제된 게시글</h3>
            {deletedPosts.length === 0 ? (
              <p className="text-center text-gray-400 py-8">삭제된 게시글이 없습니다.</p>
            ) : (
              <div className="space-y-4">
                {deletedPosts.map((post) => (
                  <div key={post.id} className="bg-[#2a2e45] p-4 rounded-lg">
                    <div className="flex justify-between items-start mb-3">
                      <div className="flex-1">
                        <h4 className="text-white font-semibold mb-1">{post.title}</h4>
                        <p className="text-gray-400 text-sm mb-2">
                          작성자: {post.writer} | 카테고리: {post.category}
                        </p>
                        <p className="text-gray-300 text-sm line-clamp-2">
                          {post.content?.length > 100 ? post.content.substring(0, 100) + '...' : post.content}
                        </p>
                      </div>
                      <button
                        onClick={() => handleRestorePost(post.id)}
                        className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-sm"
                      >
                        복구
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : activeTab === 'deletedComments' ? (
          // 삭제된 댓글 관리 섹션
          <div className="bg-[#1f2336]/80 backdrop-blur rounded-xl p-6">
            <h3 className="text-xl font-semibold text-white mb-6">💭 삭제된 댓글</h3>
            {deletedComments.length === 0 ? (
              <p className="text-center text-gray-400 py-8">삭제된 댓글이 없습니다.</p>
            ) : (
              <div className="space-y-4">
                {deletedComments.map((comment) => (
                  <div key={comment.id} className="bg-[#2a2e45] p-4 rounded-lg">
                    <div className="flex justify-between items-start mb-3">
                      <div className="flex-1">
                        <p className="text-gray-300 mb-2">{comment.content}</p>
                        <p className="text-gray-400 text-sm">
                          작성자: {comment.writer} | 게시글: {comment.postTitle}
                        </p>
                      </div>
                      <button
                        onClick={() => handleRestoreComment(comment.id)}
                        className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-sm"
                      >
                        복구
                      </button>
                    </div>
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
              <div className="flex gap-3 items-center">
                <input
                  type="text"
                  placeholder="IP 주소로 검색... (ex: 192.168)"
                  value={ipSearchTerm}
                  onChange={(e) => setIpSearchTerm(e.target.value)}
                  className="w-64 px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
                <button
                  onClick={() => setShowIpModal(true)}
                  className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded font-medium transition"
                >
                  + IP 차단 추가
                </button>
              </div>
            </div>
            
            {blockedIps.filter(ip => 
              ipSearchTerm === '' || ip.includes(ipSearchTerm)
            ).length === 0 ? (
              <p className="text-center text-gray-400 py-8">차단된 IP가 없습니다.</p>
            ) : (
              <div className="grid gap-3">
                {blockedIps
                  .filter(ip => ipSearchTerm === '' || ip.includes(ipSearchTerm))
                  .map((ip) => (
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
        
        {/* 포인트 수여 모달 */}
        <PointAwardModal 
          isOpen={showPointModal}
          onClose={() => {
            setShowPointModal(false);
            setSelectedUserId(null);
          }}
          onConfirm={handleAwardPoints}
        />
        
        {/* 신고 상세 모달 */}
        <ReportDetailModal 
          isOpen={showReportModal}
          onClose={() => {
            setShowReportModal(false);
            setSelectedReportPost(null);
          }}
          postTitle={selectedReportPost?.title || ''}
          reports={selectedReportPost?.reportDetails || []}
          onApprove={handleApproveReport}
          onReject={handleRejectReport}
          onRefresh={fetchReportedPosts}
        />
      </div>
    </div>
  );
}
