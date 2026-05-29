import { useEffect, useState, useCallback, useMemo } from 'react';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import { IpBlockModal, AdminReasonModal, PointAwardModal, ReportDetailModal } from '../components/admin';
import { grantNicknameChangeTicket } from '../lib/api/admin';
import { CATEGORY_LABELS } from './PostList';
import { getErrorMessage } from '../types/api';

interface UserSummary {
  id: number;
  email: string;
  nickname: string;
  role: string;
  status: 'ACTIVE' | 'BANNED' | 'SUSPENDED' | 'WITHDRAWN';
  accountLocked: boolean;
  points: number;
  socialProvider?: string;
}

interface BlindedPost {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: { nickname: string } | string;
  createdAt: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
}

interface Comment {
  id: number;
  content: string;
  writer: string;
  postId?: number;
  postTitle?: string;
  createdAt: string;
}

interface S3Status {
  bucketName: string;
  configuredRegion: string;
  actualRegion?: string;
  regionMatch: boolean;
  connectionStatus: string;
  bucketExists: boolean;
  error?: string;
  warning?: string;
  suggestion?: string;
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

interface ReportedComment {
  id: number;
  content: string;
  writer: string;
  postId: number;
  postTitle: string;
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
  const [blindedComments, setBlindedComments] = useState<Comment[]>([]);
  const [deletedPosts, setDeletedPosts] = useState<BlindedPost[]>([]);
  const [deletedComments, setDeletedComments] = useState<Comment[]>([]);
  const [activeTab, setActiveTab] = useState<'users' | 'ips' | 'posts' | 'reportedPosts' | 'reportedComments' | 'blindComments' | 'deletedPosts' | 'deletedComments' | 'files' | 'scheduler'>('users');
  const [reportedComments, setReportedComments] = useState<ReportedComment[]>([]);
  const [showIpModal, setShowIpModal] = useState(false);
  const [showReasonModal, setShowReasonModal] = useState(false);
  const [showPointModal, setShowPointModal] = useState(false);
  const [showReportModal, setShowReportModal] = useState(false);
  const [selectedReportPost, setSelectedReportPost] = useState<ReportedPost | null>(null);
  const [selectedReportComment, setSelectedReportComment] = useState<ReportedComment | null>(null);
  const [modalAction, setModalAction] = useState<{ type: string; userId: number; status?: string } | null>(null);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'WITHDRAWN'>('ALL');
  const [userTypeFilter, setUserTypeFilter] = useState<'ALL' | 'REGULAR' | 'SOCIAL'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [postSearchTerm, setPostSearchTerm] = useState('');
  const [ipSearchTerm, setIpSearchTerm] = useState('');
  const [orphanImageCount, setOrphanImageCount] = useState<number>(0);
  const [isCleaningFiles, setIsCleaningFiles] = useState(false);
  const [s3Status, setS3Status] = useState<S3Status | null>(null);
  const [showS3Status, setShowS3Status] = useState(false);
  const [schedulerStatus, setSchedulerStatus] = useState<{
    messagesToDelete: number;
    postsToDelete: number;
    usersToCleanup: number;
  }>({ messagesToDelete: 0, postsToDelete: 0, usersToCleanup: 0 });
  const [isRunningScheduler, setIsRunningScheduler] = useState<{
    message: boolean;
    post: boolean;
    user: boolean;
  }>({ message: false, post: false, user: false });
  const { user: currentUser } = useAuth(); // 현재 로그인한 사용자

  // 공통 API 호출 함수
  const fetchData = useCallback(async function<T>(endpoint: string, setter: (data: T[]) => void, errorMsg: string) {
    try {
      const res = await axios.get(endpoint);
      const data = Array.isArray(res.data) ? res.data : (res.data?.data || []);
      setter(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error(errorMsg, err);
      setter([]);
    }
  }, []);

  const fetchUsers = useCallback(async () => {
    try {
      const res = await axios.get('/admin/users');
      const userData = res.data?.data || res.data || [];
      const userList = Array.isArray(userData) ? userData : [];
      setUsers(userList);
    } catch (err) {
      console.error('사용자 목록 조회 실패', err);
      setUsers([]);
    } finally {
      setLoading(false);
    }
  }, []);

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

  const handleAwardPoints = useCallback(async (points: number, reason: string) => {
    if (!selectedUserId) return;
    
    try {
      const response = await axios.post(`/admin/users/${selectedUserId}/points`, { points, reason });
      const message = response.data?.data || response.data?.message || `${points}포인트가 성공적으로 수여되었습니다.`;
      alert(message);
      fetchUsers();
    } catch (error: unknown) {
      console.error('포인트 수여 실패:', error);
      alert(getErrorMessage(error));
    } finally {
      setShowPointModal(false);
      setSelectedUserId(null);
    }
  }, [selectedUserId, fetchUsers]);

  const handleApproveReport = async (reportId: number) => {
    try {
      // 선택된 신고 타입에 따라 다른 엔드포인트 호출
      if (selectedReportPost) {
        await axios.post(`/admin/reports/${reportId}/approve`);
      } else if (selectedReportComment) {
        await axios.post(`/admin/comment-reports/${reportId}/approve`);
      }
      alert('신고가 승인되었습니다. 신고자들에게 포인트가 지급되었습니다.');
      fetchReportedPosts();
      fetchReportedComments();
    } catch (error) {
      console.error('신고 승인 실패:', error);
      alert('신고 승인에 실패했습니다.');
    }
  };

  const handleRejectReport = async (reportId: number, reason: string) => {
    try {
      // 선택된 신고 타입에 따라 다른 엔드포인트 호출
      if (selectedReportPost) {
        await axios.post(`/admin/reports/${reportId}/reject`, { reason });
      } else if (selectedReportComment) {
        await axios.post(`/admin/comment-reports/${reportId}/reject`, { reason });
      }
      alert('신고가 거부되었습니다.');
      fetchReportedPosts();
      fetchReportedComments();
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
    fetchReportedComments();
    fetchBlindedComments();
    fetchDeletedPosts();
    fetchDeletedComments();
    fetchOrphanImageCount();
    fetchSchedulerStatus();
    fetchS3Status();
  }, []);

  const fetchBlindedPosts = useCallback(() => fetchData('/admin/posts/blinded', setBlindedPosts, '블라인드 게시글 목록 조회 실패'), [fetchData]);
  const fetchReportedPosts = useCallback(() => fetchData('/admin/posts/reported', setReportedPosts, '신고된 게시글 목록 조회 실패'), [fetchData]);
  const fetchReportedComments = useCallback(() => fetchData('/admin/comments/reported', setReportedComments, '신고된 댓글 목록 조회 실패'), [fetchData]);
  const fetchBlindedComments = useCallback(() => fetchData('/admin/comments/blinded', setBlindedComments, '블라인드 댓글 목록 조회 실패'), [fetchData]);
  const fetchDeletedPosts = useCallback(() => fetchData('/admin/posts/deleted', setDeletedPosts, '삭제된 게시글 목록 조회 실패'), [fetchData]);
  const fetchDeletedComments = useCallback(() => fetchData('/admin/comments/deleted', setDeletedComments, '삭제된 댓글 목록 조회 실패'), [fetchData]);

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
  
  const handleBlindComment = async (commentId: number) => {
    if (!confirm('이 댓글을 블라인드 처리하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/comments/${commentId}/blind`);
      alert('댓글이 블라인드 처리되었습니다.');
      fetchReportedComments();
      fetchBlindedComments();
    } catch (err) {
      console.error('댓글 블라인드 처리 실패:', err);
      alert('댓글 블라인드 처리에 실패했습니다.');
    }
  };
  
  const handleUnblindReportedComment = async (commentId: number) => {
    if (!confirm('이 댓글의 블라인드를 해제하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/comments/${commentId}/unblind`);
      alert('댓글 블라인드가 해제되었습니다.');
      fetchReportedComments();
      fetchBlindedComments();
    } catch (err) {
      console.error('댓글 블라인드 해제 실패:', err);
      alert('댓글 블라인드 해제에 실패했습니다.');
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

  const fetchBlockedIps = useCallback(() => fetchData('/admin/blocked-ips', setBlockedIps, '차단된 IP 목록 조회 실패'), [fetchData]);

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

  const fetchOrphanImageCount = async () => {
    try {
      const res = await axios.get('/admin/files/orphan-count');
      const count = res.data?.data || 0;
      setOrphanImageCount(count);
    } catch (err) {
      console.error('고아 이미지 개수 조회 실패:', err);
      setOrphanImageCount(0);
    }
  };

  const fetchS3Status = async () => {
    try {
      const res = await axios.get('/admin/files/s3-status');
      const status = res.data?.data || {};
      setS3Status(status);
    } catch (err) {
      console.error('S3 상태 조회 실패:', err);
    }
  };

  const handleCleanupOrphanImages = async () => {
    if (!confirm(`정말 ${orphanImageCount}개의 오래된 파일을 정리하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.`)) return;
    
    setIsCleaningFiles(true);
    try {
      const res = await axios.post('/admin/files/cleanup-orphans');
      const deletedCount = res.data?.data || 0;
      const message = res.data?.message || `${deletedCount}개의 파일이 정리되었습니다.`;
      alert(message);
      fetchOrphanImageCount(); // 개수 새로고침
    } catch (err) {
      console.error('파일 정리 실패:', err);
      alert('파일 정리에 실패했습니다.');
    } finally {
      setIsCleaningFiles(false);
    }
  };

  const fetchSchedulerStatus = async () => {
    try {
      const res = await axios.get('/admin/scheduler/status');
      const status = res.data?.data || { messagesToDelete: 0, postsToDelete: 0, usersToCleanup: 0 };
      setSchedulerStatus(status);
    } catch (err) {
      console.error('스케줄러 상태 조회 실패:', err);
    }
  };

  const handleManualScheduler = async (type: 'message' | 'post' | 'user') => {
    const confirmMessages = {
      message: `정말 ${schedulerStatus.messagesToDelete || 0}개의 오래된 쪽지를 영구 삭제하시겠습니까?`,
      post: `정말 ${schedulerStatus.postsToDelete || 0}개의 만료된 게시글을 정리하시겠습니까?`,
      user: `정말 ${schedulerStatus.usersToCleanup || 0}명의 탈퇴 회원 데이터를 완전 삭제하시겠습니까?`
    };
    
    if (!confirm(confirmMessages[type] + '\n\n이 작업은 되돌릴 수 없습니다.')) return;
    
    setIsRunningScheduler(prev => ({ ...prev, [type]: true }));
    
    try {
      const endpoints = {
        message: '/admin/scheduler/message-cleanup/manual',
        post: '/admin/scheduler/post-cleanup/manual',
        user: '/admin/scheduler/user-cleanup/manual'
      };
      
      const res = await axios.post(endpoints[type]);
      const message = res.data?.message || '작업이 완료되었습니다.';
      alert(message);
      fetchSchedulerStatus(); // 상태 새로고침
    } catch (err) {
      console.error(`${type} 스케줄러 실행 실패:`, err);
      alert('작업 실행에 실패했습니다.');
    } finally {
      setIsRunningScheduler(prev => ({ ...prev, [type]: false }));
    }
  };

  const handleGrantNicknameChangeTicket = useCallback(async (userId: number) => {
    if (!confirm('이 사용자에게 닉네임 변경권을 수여하시겠습니까?\n\n닉네임 변경 제한이 해제되어 즉시 닉네임을 변경할 수 있습니다.')) return;
    
    try {
      await grantNicknameChangeTicket(userId);
      alert('닉네임 변경권이 성공적으로 수여되었습니다.');
    } catch (err) {
      console.error('닉네임 변경권 수여 실패:', err);
      alert('닉네임 변경권 수여에 실패했습니다.');
    }
  }, []);

  // 스케줄러 총 카운트
  const totalSchedulerCount = useMemo(() => {
    return schedulerStatus.messagesToDelete + schedulerStatus.postsToDelete + schedulerStatus.usersToCleanup;
  }, [schedulerStatus]);

  // 필터링된 사용자 목록
  const filteredUsers = useMemo(() => {
    if (!users || users.length === 0) return [];
    
    return users.filter(user => {
      // 상태 필터
      const statusMatch = statusFilter === 'ALL' || 
        (statusFilter === 'ACTIVE' && user.status !== 'WITHDRAWN') ||
        (statusFilter === 'WITHDRAWN' && user.status === 'WITHDRAWN');
      
      // 사용자 유형 필터
      const typeMatch = userTypeFilter === 'ALL' ||
        (userTypeFilter === 'REGULAR' && !user.socialProvider) ||
        (userTypeFilter === 'SOCIAL' && !!user.socialProvider);
      
      // 검색 필터
      const searchMatch = searchTerm === '' || 
        user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
        user.nickname.toLowerCase().includes(searchTerm.toLowerCase());
      
      return statusMatch && typeMatch && searchMatch;
    });
  }, [users, statusFilter, userTypeFilter, searchTerm]);

  return (
    <div className="min-h-screen bg-space-gradient text-white px-6 py-12">
      <div className="max-w-6xl mx-auto">
        <h2 className="text-3xl font-bold mb-8 drop-shadow-glow text-center">🔐 관리자 - 사용자 관리</h2>
        
        {/* 관리자 메뉴 카드 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-4 mb-8 max-h-none overflow-visible">
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
            onClick={() => setActiveTab('reportedPosts')}
            className={`p-6 rounded-xl border-2 transition-all duration-200 ${
              activeTab === 'reportedPosts'
                ? 'bg-orange-600/40 border-orange-400 text-white shadow-lg transform scale-105'
                : 'bg-[#1f2336]/80 border-gray-600/50 text-gray-300 hover:bg-[#252842]/80 hover:border-orange-500/50'
            }`}
          >
            <div className="text-3xl mb-2">🚨</div>
            <div className="font-semibold">신고된 게시글</div>
            <div className="text-sm text-gray-400 mt-1">신고된 게시글 관리</div>
          </button>
          
          <button
            onClick={() => setActiveTab('reportedComments')}
            className={`p-6 rounded-xl border-2 transition-all duration-200 ${
              activeTab === 'reportedComments'
                ? 'bg-pink-600/40 border-pink-400 text-white shadow-lg transform scale-105'
                : 'bg-[#1f2336]/80 border-gray-600/50 text-gray-300 hover:bg-[#252842]/80 hover:border-pink-500/50'
            }`}
          >
            <div className="text-3xl mb-2">💬</div>
            <div className="font-semibold">신고된 댓글</div>
            <div className="text-sm text-gray-400 mt-1">신고된 댓글 관리</div>
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
          
          <button
            onClick={() => setActiveTab('files')}
            className={`p-6 rounded-xl border-2 transition-all duration-200 ${
              activeTab === 'files'
                ? 'bg-blue-600/40 border-blue-400 text-white shadow-lg transform scale-105'
                : 'bg-[#1f2336]/80 border-gray-600/50 text-gray-300 hover:bg-[#252842]/80 hover:border-blue-500/50'
            }`}
          >
            <div className="text-3xl mb-2">📁</div>
            <div className="font-semibold">파일 정리</div>
            <div className="text-sm text-gray-400 mt-1">고아 이미지 파일 관리</div>
            {orphanImageCount > 0 && (
              <div className="text-xs bg-red-500 text-white px-2 py-1 rounded-full mt-2 inline-block">
                {orphanImageCount}개
              </div>
            )}
          </button>
          
          <button
            onClick={() => setActiveTab('scheduler')}
            className={`p-6 rounded-xl border-2 transition-all duration-200 ${
              activeTab === 'scheduler'
                ? 'bg-green-600/40 border-green-400 text-white shadow-lg transform scale-105'
                : 'bg-[#1f2336]/80 border-gray-600/50 text-gray-300 hover:bg-[#252842]/80 hover:border-green-500/50'
            }`}
          >
            <div className="text-3xl mb-2">⏰</div>
            <div className="font-semibold">스케줄러 관리</div>
            <div className="text-sm text-gray-400 mt-1">자동 정리 작업 관리</div>
            {totalSchedulerCount > 0 && (
              <div className="text-xs bg-orange-500 text-white px-2 py-1 rounded-full mt-2 inline-block">
                {totalSchedulerCount}건
              </div>
            )}
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
              <div className="flex gap-2 justify-center flex-wrap">
                {/* 계정 상태 필터 */}
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
                
                {/* 구분선 */}
                <div className="w-px bg-gray-600 mx-2"></div>
                
                {/* 사용자 유형 필터 */}
                <button
                  onClick={() => setUserTypeFilter('ALL')}
                  className={`px-4 py-2 rounded text-sm transition ${
                    userTypeFilter === 'ALL'
                      ? 'bg-purple-600 text-white'
                      : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                  }`}
                >
                  전체 유형
                </button>
                <button
                  onClick={() => setUserTypeFilter('REGULAR')}
                  className={`px-4 py-2 rounded text-sm transition ${
                    userTypeFilter === 'REGULAR'
                      ? 'bg-orange-600 text-white'
                      : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                  }`}
                >
                  일반 사용자
                </button>
                <button
                  onClick={() => setUserTypeFilter('SOCIAL')}
                  className={`px-4 py-2 rounded text-sm transition ${
                    userTypeFilter === 'SOCIAL'
                      ? 'bg-cyan-600 text-white'
                      : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                  }`}
                >
                  소셜 사용자
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
                  <th className="p-3">유형</th>
                  <th className="p-3">닉네임 변경권</th>
                  <th className="p-3">권한</th>
                  <th className="p-3">상태</th>
                  <th className="p-3">포인트</th>
                  <th className="p-3">조치</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.length > 0 ? filteredUsers
                  .map((user) => (
                  <tr key={user.id} className="border-t border-gray-700">
                    <td className="p-3 text-center">{user.id}</td>
                    <td className="p-3">{user.email}</td>
                    <td className="p-3">{user.nickname}</td>
                    <td className="p-3 text-center">
                      {user.socialProvider ? (
                        <div className="flex items-center justify-center gap-1">
                          <span className="text-xs px-2 py-1 bg-cyan-600 text-white rounded-full font-medium">
                            {user.socialProvider === 'GOOGLE' && '🔗 구글'}
                            {user.socialProvider === 'KAKAO' && '🔗 카카오'}
                            {user.socialProvider === 'NAVER' && '🔗 네이버'}
                            {!['GOOGLE', 'KAKAO', 'NAVER'].includes(user.socialProvider) && `🔗 ${user.socialProvider}`}
                          </span>
                        </div>
                      ) : (
                        <span className="text-xs px-2 py-1 bg-orange-600 text-white rounded-full font-medium">
                          📧 일반
                        </span>
                      )}
                    </td>
                    <td className="p-3 text-center">
                      <button
                        onClick={() => handleGrantNicknameChangeTicket(user.id)}
                        className="bg-purple-600 hover:bg-purple-700 text-white px-3 py-1 rounded text-xs font-medium transition"
                        disabled={user.status === 'WITHDRAWN'}
                        title="닉네임 변경 제한을 해제합니다"
                      >
                        티켓 수여
                      </button>
                    </td>
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
                    <td colSpan={9} className="p-8 text-center text-gray-400">
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
              (typeof post.writer === 'string' ? post.writer : post.writer.nickname).toLowerCase().includes(postSearchTerm.toLowerCase())
            ).length === 0 ? (
              <p className="text-center text-gray-400 py-8">블라인드 처리된 게시글이 없습니다.</p>
            ) : (
              <div className="grid gap-4">
                {blindedPosts
                  .filter(post => 
                    postSearchTerm === '' ||
                    post.title.toLowerCase().includes(postSearchTerm.toLowerCase()) ||
                    (typeof post.writer === 'string' ? post.writer : post.writer.nickname).toLowerCase().includes(postSearchTerm.toLowerCase())
                  )
                  .map((post) => (
                  <div key={post.id} className="bg-[#2a2e45] p-4 rounded-lg">
                    <div className="flex justify-between items-start mb-3">
                      <div className="flex-1">
                        <h4 className="text-white font-semibold mb-1">{post.title}</h4>
                        <p className="text-gray-400 text-sm mb-2">
                          작성자: {typeof post.writer === 'string' ? post.writer : post.writer.nickname} | 카테고리: {post.category} | 조회: {post.viewCount} | 추천: {post.likeCount} | 댓글: {post.commentCount}
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
        ) : activeTab === 'reportedPosts' ? (
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
                      <th className="px-3 py-4 text-left">제목</th>
                      <th className="px-3 py-4 text-left">작성자</th>
                      <th className="px-4 py-4 text-left">카테고리</th>
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
                        <td className="px-3 py-4">
                          <span className={`category-label category-label-${post.category.toLowerCase()}`}>{CATEGORY_LABELS[post.category] || post.category}</span>
                        </td>
                        <td className="px-3 py-4 text-center">
                          <button
                            onClick={() => {
                              console.log('선택된 게시글:', post);
                              console.log('신고 상세 데이터:', post.reportDetails);
                              setSelectedReportPost(post);
                              setShowReportModal(true);
                            }}
                            className={`px-2 py-1 rounded text-xs font-medium hover:scale-105 transition-all duration-200 shadow-md whitespace-nowrap flex items-center gap-1 ${
                              post.reportCount >= 5 
                                ? 'bg-red-600 hover:bg-red-700 text-white' 
                                : 'bg-yellow-600 hover:bg-yellow-700 text-white'
                            }`}
                            title="클릭하여 신고 상세 내역 보기"
                          >
                            <span>🔍</span> 🚨{post.reportCount || 0}
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
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        ) : activeTab === 'reportedComments' ? (
          // 신고된 댓글 관리 섹션
          <div className="bg-[#1f2336]/80 backdrop-blur rounded-xl p-6">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-semibold text-white">💬 신고된 댓글</h3>
              <input
                type="text"
                placeholder="댓글 내용, 작성자로 검색..."
                value={postSearchTerm}
                onChange={(e) => setPostSearchTerm(e.target.value)}
                className="w-64 px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
            </div>
            
            {reportedComments.filter(comment => 
              postSearchTerm === '' ||
              comment.content?.toLowerCase().includes(postSearchTerm.toLowerCase()) ||
              comment.writer?.toLowerCase().includes(postSearchTerm.toLowerCase())
            ).length === 0 ? (
              <p className="text-center text-gray-400 py-8">신고된 댓글이 없습니다.</p>
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
                      <th className="px-3 py-4 text-left">댓글 내용</th>
                      <th className="px-3 py-4 text-left">작성자</th>
                      <th className="px-3 py-4 text-left">게시글</th>
                      <th className="px-3 py-4 text-center">신고수</th>
                      <th className="px-3 py-4 text-center">상태</th>
                      <th className="px-3 py-4 text-center">작성일</th>
                      <th className="px-3 py-4 text-center">관리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reportedComments
                      .filter(comment => 
                        postSearchTerm === '' ||
                        comment.content?.toLowerCase().includes(postSearchTerm.toLowerCase()) ||
                        comment.writer?.toLowerCase().includes(postSearchTerm.toLowerCase())
                      )
                      .map((comment) => (
                      <tr key={comment.id} className="border-b border-gray-600 hover:bg-[#2a2e45]/50">
                        <td className="px-4 py-4">
                          <div className="text-gray-300 truncate">{comment.content}</div>
                        </td>
                        <td className="px-3 py-4 truncate">{comment.writer}</td>
                        <td className="px-3 py-4 truncate">
                          <button
                            onClick={() => window.open(`/posts/${comment.postId}`, '_blank')}
                            className="text-blue-400 hover:text-blue-300 hover:underline truncate block w-full text-left"
                            title="게시글로 이동"
                          >
                            {comment.postTitle}
                          </button>
                        </td>
                        <td className="px-3 py-4 text-center">
                          <button
                            onClick={() => {
                              console.log('선택된 댓글:', comment);
                              console.log('신고 상세 데이터:', comment.reportDetails);
                              setSelectedReportComment(comment);
                              setShowReportModal(true);
                            }}
                            className={`px-2 py-1 rounded text-xs font-medium hover:scale-105 transition-all duration-200 shadow-md whitespace-nowrap flex items-center gap-1 mx-auto ${
                              comment.reportCount >= 5 
                                ? 'bg-red-600 hover:bg-red-700 text-white' 
                                : 'bg-yellow-600 hover:bg-yellow-700 text-white'
                            }`}
                            title="클릭하여 신고 상세 내역 보기"
                          >
                            <span>🔍</span> 🚨{comment.reportCount || 0}
                          </button>
                        </td>
                        <td className="px-3 py-4 text-center">
                          {comment.blinded ? (
                            <span className="px-2 py-1 bg-red-600 rounded text-xs whitespace-nowrap">블라인드</span>
                          ) : (
                            <span className="px-2 py-1 bg-green-600 rounded text-xs whitespace-nowrap">공개</span>
                          )}
                        </td>
                        <td className="px-3 py-4 text-center text-xs text-gray-400">
                          {new Date(comment.createdAt).toLocaleDateString()}
                        </td>
                        <td className="px-3 py-4 text-center">
                          <div className="flex gap-1 justify-center">
                            {comment.blinded ? (
                              <button
                                onClick={() => handleUnblindReportedComment(comment.id)}
                                className="px-2 py-1 bg-green-600 hover:bg-green-700 rounded text-xs whitespace-nowrap"
                              >
                                해제
                              </button>
                            ) : (
                              <button
                                onClick={() => handleBlindComment(comment.id)}
                                className="px-2 py-1 bg-orange-600 hover:bg-orange-700 rounded text-xs whitespace-nowrap"
                              >
                                블라인드
                              </button>
                            )}
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
                          작성자: {comment.writer} | 게시글: 
                          {comment.postId && comment.postTitle ? (
                            <button
                              onClick={() => {
                                console.log('게시글 이동 시도:', comment.postId, comment.postTitle);
                                window.open(`/posts/${comment.postId}`, '_blank');
                              }}
                              className="text-blue-400 hover:text-blue-300 hover:underline ml-1"
                              title="게시글로 이동"
                            >
                              {comment.postTitle}
                            </button>
                          ) : (
                            <span className="text-gray-500 ml-1">삭제된 게시글</span>
                          )}
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
                        <p className="text-gray-300 mb-2">
                          {comment.content}
                        </p>
                        <p className="text-gray-400 text-sm">
                          작성자: {comment.writer} | 게시글: 
                          {comment.postId && comment.postTitle ? (
                            <button
                              onClick={() => {
                                console.log('게시글 이동 시도:', comment.postId, comment.postTitle);
                                window.open(`/posts/${comment.postId}`, '_blank');
                              }}
                              className="text-blue-400 hover:text-blue-300 hover:underline ml-1"
                              title="게시글로 이동"
                            >
                              {comment.postTitle}
                            </button>
                          ) : (
                            <span className="text-gray-500 ml-1">삭제된 게시글</span>
                          )}
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
        ) : activeTab === 'scheduler' ? (
          // 스케줄러 관리 섹션
          <div className="bg-[#1f2336]/80 backdrop-blur rounded-xl p-6">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-semibold text-white">⏰ 스케줄러 관리</h3>
              <button
                onClick={fetchSchedulerStatus}
                className="bg-gray-600 hover:bg-gray-700 text-white px-3 py-2 rounded text-sm transition"
              >
                🔄 새로고침
              </button>
            </div>
            
            <div className="grid gap-6">
              {/* 쪽지 정리 스케줄러 */}
              <div className="bg-[#2a2e45] p-6 rounded-lg">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h4 className="text-lg font-semibold text-white mb-2">💌 쪽지 정리 스케줄러</h4>
                    <p className="text-gray-400 text-sm">
                      매일 새벽 2시 - 양쪽 모두 삭제 후 3년 경과한 쪽지를 영구 삭제합니다.
                    </p>
                  </div>
                  <div className="text-2xl">🕐</div>
                </div>
                
                <div className="flex items-center justify-between bg-[#1f2336] p-4 rounded-lg">
                  <div className="flex items-center gap-4">
                    <div className="text-3xl">📊</div>
                    <div>
                      <div className="text-2xl font-bold text-white">
                        {(schedulerStatus.messagesToDelete || 0).toLocaleString()}개
                      </div>
                      <div className="text-sm text-gray-400">정리 대상 쪽지</div>
                    </div>
                  </div>
                  
                  <button
                    onClick={() => handleManualScheduler('message')}
                    disabled={(schedulerStatus.messagesToDelete || 0) === 0 || isRunningScheduler.message}
                    className={`px-6 py-3 rounded-lg font-medium transition ${
                      schedulerStatus.messagesToDelete === 0 || isRunningScheduler.message
                        ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                        : 'bg-blue-600 hover:bg-blue-700 text-white hover:scale-105 shadow-lg'
                    }`}
                  >
                    {isRunningScheduler.message ? (
                      <div className="flex items-center gap-2">
                        <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
                        실행 중...
                      </div>
                    ) : (
                      '🧹 수동 실행'
                    )}
                  </button>
                </div>
                
                {(schedulerStatus.messagesToDelete || 0) === 0 && (
                  <div className="mt-4 p-3 bg-green-600/20 border border-green-600/50 rounded-lg">
                    <div className="flex items-center gap-2 text-green-400">
                      <span>✅</span>
                      <span className="text-sm">정리할 쪽지가 없습니다.</span>
                    </div>
                  </div>
                )}
              </div>
              
              {/* 게시글 정리 스케줄러 */}
              <div className="bg-[#2a2e45] p-6 rounded-lg">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h4 className="text-lg font-semibold text-white mb-2">📝 게시글 정리 스케줄러</h4>
                    <p className="text-gray-400 text-sm">
                      매일 아침 8시 - 삭제 후 30일 경과한 게시글과 댓글을 영구 삭제합니다.
                    </p>
                  </div>
                  <div className="text-2xl">🕒</div>
                </div>
                
                <div className="flex items-center justify-between bg-[#1f2336] p-4 rounded-lg">
                  <div className="flex items-center gap-4">
                    <div className="text-3xl">📊</div>
                    <div>
                      <div className="text-2xl font-bold text-white">
                        {(schedulerStatus.postsToDelete || 0).toLocaleString()}개
                      </div>
                      <div className="text-sm text-gray-400">정리 대상 게시글</div>
                    </div>
                  </div>
                  
                  <button
                    onClick={() => handleManualScheduler('post')}
                    disabled={(schedulerStatus.postsToDelete || 0) === 0 || isRunningScheduler.post}
                    className={`px-6 py-3 rounded-lg font-medium transition ${
                      schedulerStatus.postsToDelete === 0 || isRunningScheduler.post
                        ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                        : 'bg-yellow-600 hover:bg-yellow-700 text-white hover:scale-105 shadow-lg'
                    }`}
                  >
                    {isRunningScheduler.post ? (
                      <div className="flex items-center gap-2">
                        <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
                        실행 중...
                      </div>
                    ) : (
                      '🧹 수동 실행'
                    )}
                  </button>
                </div>
                
                {(schedulerStatus.postsToDelete || 0) === 0 && (
                  <div className="mt-4 p-3 bg-green-600/20 border border-green-600/50 rounded-lg">
                    <div className="flex items-center gap-2 text-green-400">
                      <span>✅</span>
                      <span className="text-sm">정리할 게시글이 없습니다.</span>
                    </div>
                  </div>
                )}
              </div>
              
              {/* 탈퇴 회원 정리 스케줄러 */}
              <div className="bg-[#2a2e45] p-6 rounded-lg">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h4 className="text-lg font-semibold text-white mb-2">👤 탈퇴 회원 정리 스케줄러</h4>
                    <p className="text-gray-400 text-sm">
                      매일 오전 10시 - 탈퇴 후 2년 경과한 회원 데이터를 완전 삭제합니다.
                    </p>
                  </div>
                  <div className="text-2xl">🕙</div>
                </div>
                
                <div className="flex items-center justify-between bg-[#1f2336] p-4 rounded-lg">
                  <div className="flex items-center gap-4">
                    <div className="text-3xl">📊</div>
                    <div>
                      <div className="text-2xl font-bold text-white">
                        {(schedulerStatus.usersToCleanup || 0).toLocaleString()}명
                      </div>
                      <div className="text-sm text-gray-400">정리 대상 회원</div>
                    </div>
                  </div>
                  
                  <button
                    onClick={() => handleManualScheduler('user')}
                    disabled={(schedulerStatus.usersToCleanup || 0) === 0 || isRunningScheduler.user}
                    className={`px-6 py-3 rounded-lg font-medium transition ${
                      schedulerStatus.usersToCleanup === 0 || isRunningScheduler.user
                        ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                        : 'bg-red-600 hover:bg-red-700 text-white hover:scale-105 shadow-lg'
                    }`}
                  >
                    {isRunningScheduler.user ? (
                      <div className="flex items-center gap-2">
                        <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
                        실행 중...
                      </div>
                    ) : (
                      '🧹 수동 실행'
                    )}
                  </button>
                </div>
                
                {(schedulerStatus.usersToCleanup || 0) === 0 && (
                  <div className="mt-4 p-3 bg-green-600/20 border border-green-600/50 rounded-lg">
                    <div className="flex items-center gap-2 text-green-400">
                      <span>✅</span>
                      <span className="text-sm">정리할 회원이 없습니다.</span>
                    </div>
                  </div>
                )}
              </div>
              

              
              {/* 스케줄러 정보 카드 */}
              <div className="bg-[#2a2e45] p-6 rounded-lg">
                <h4 className="text-lg font-semibold text-white mb-4">📋 스케줄러 정보</h4>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                  <div className="bg-[#1f2336] p-4 rounded-lg">
                    <div className="text-blue-400 font-medium mb-2 flex items-center gap-2">
                      <span className="text-xl">🕐</span>
                      <span>새벽 2시</span>
                    </div>
                    <ul className="text-gray-300 space-y-1">
                      <li>• 쪽지 자동 정리</li>
                      <li>• 3년 경과 쪽지 삭제</li>
                      <li>• 양쪽 모두 삭제한 경우만</li>
                    </ul>
                  </div>
                  <div className="bg-[#1f2336] p-4 rounded-lg">
                    <div className="text-yellow-400 font-medium mb-2 flex items-center gap-2">
                      <span className="text-xl">🕗</span>
                      <span>아침 8시</span>
                    </div>
                    <ul className="text-gray-300 space-y-1">
                      <li>• 게시글/댓글 정리</li>
                      <li>• 30일 경과 삭제 게시글</li>
                      <li>• 관련 파일도 함께 삭제</li>
                    </ul>
                  </div>
                  <div className="bg-[#1f2336] p-4 rounded-lg">
                    <div className="text-red-400 font-medium mb-2 flex items-center gap-2">
                      <span className="text-xl">🕙</span>
                      <span>오전 10시</span>
                    </div>
                    <ul className="text-gray-300 space-y-1">
                      <li>• 탈퇴 회원 정리</li>
                      <li>• 2년 경과 탈퇴 회원</li>
                      <li>• 개인정보 완전 삭제</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>
        ) : activeTab === 'files' ? (
          // 파일 정리 관리 섹션
          <div className="bg-[#1f2336]/80 backdrop-blur rounded-xl p-6">
            <h3 className="text-xl font-semibold text-white mb-6">📁 파일 정리 관리</h3>
            
            <div className="grid gap-6">
              {/* 고아 이미지 정리 카드 */}
              <div className="bg-[#2a2e45] p-6 rounded-lg">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h4 className="text-lg font-semibold text-white mb-2">🗑️ 고아 이미지 정리</h4>
                    <p className="text-gray-400 text-sm">
                      업로드 후 게시글에 사용되지 않은 오래된 이미지 파일들을 정리합니다.
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => setShowS3Status(!showS3Status)}
                      className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-2 rounded text-sm transition"
                    >
                      🔍 S3 상태
                    </button>
                    <button
                      onClick={() => {
                        fetchOrphanImageCount();
                        fetchS3Status();
                      }}
                      className="bg-gray-600 hover:bg-gray-700 text-white px-3 py-2 rounded text-sm transition"
                      disabled={isCleaningFiles}
                    >
                      🔄 새로고침
                    </button>
                  </div>
                </div>
                
                <div className="flex items-center justify-between bg-[#1f2336] p-4 rounded-lg">
                  <div className="flex items-center gap-4">
                    <div className="text-3xl">📊</div>
                    <div>
                      <div className="text-2xl font-bold text-white">
                        {orphanImageCount.toLocaleString()}개
                      </div>
                      <div className="text-sm text-gray-400">정리 대상 파일</div>
                    </div>
                  </div>
                  
                  <button
                    onClick={handleCleanupOrphanImages}
                    disabled={orphanImageCount === 0 || isCleaningFiles}
                    className={`px-6 py-3 rounded-lg font-medium transition ${
                      orphanImageCount === 0 || isCleaningFiles
                        ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                        : 'bg-red-600 hover:bg-red-700 text-white hover:scale-105 shadow-lg'
                    }`}
                  >
                    {isCleaningFiles ? (
                      <div className="flex items-center gap-2">
                        <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
                        정리 중...
                      </div>
                    ) : (
                      '🧹 파일 정리 실행'
                    )}
                  </button>
                </div>
                
                {orphanImageCount === 0 && (
                  <div className="mt-4 p-3 bg-green-600/20 border border-green-600/50 rounded-lg">
                    <div className="flex items-center gap-2 text-green-400">
                      <span>✅</span>
                      <span className="text-sm">정리할 파일이 없습니다. 시스템이 깨끗합니다!</span>
                    </div>
                  </div>
                )}
                
                {orphanImageCount > 0 && (
                  <div className="mt-4 p-3 bg-yellow-600/20 border border-yellow-600/50 rounded-lg">
                    <div className="flex items-center gap-2 text-yellow-400">
                      <span>⚠️</span>
                      <span className="text-sm">
                        7일 이상 된 미사용 파일들입니다. 정리를 권장합니다.
                      </span>
                    </div>
                  </div>
                )}
                
                {/* S3 상태 정보 */}
                {showS3Status && s3Status && (
                  <div className="mt-4 p-4 bg-[#1f2336] rounded-lg">
                    <h5 className="text-white font-medium mb-3">📊 S3 연결 상태</h5>
                    <div className="grid grid-cols-2 gap-3 text-sm">
                      <div>
                        <span className="text-gray-400">버킷:</span>
                        <span className="text-white ml-2">{s3Status.bucketName}</span>
                      </div>
                      <div>
                        <span className="text-gray-400">설정 리전:</span>
                        <span className="text-white ml-2">{s3Status.configuredRegion}</span>
                      </div>
                      {s3Status.actualRegion && (
                        <div>
                          <span className="text-gray-400">실제 리전:</span>
                          <span className={`ml-2 ${
                            s3Status.regionMatch ? 'text-green-400' : 'text-yellow-400'
                          }`}>
                            {s3Status.actualRegion}
                            {!s3Status.regionMatch && ' ⚠️'}
                          </span>
                        </div>
                      )}
                      <div>
                        <span className="text-gray-400">연결 상태:</span>
                        <span className={`ml-2 ${
                          s3Status.connectionStatus === 'SUCCESS' ? 'text-green-400' : 'text-red-400'
                        }`}>
                          {s3Status.connectionStatus === 'SUCCESS' ? '✅ 정상' : '❌ 오류'}
                        </span>
                      </div>
                      <div>
                        <span className="text-gray-400">버킷 존재:</span>
                        <span className={`ml-2 ${
                          s3Status.bucketExists ? 'text-green-400' : 'text-red-400'
                        }`}>
                          {s3Status.bucketExists ? '✅ 존재' : '❌ 없음'}
                        </span>
                      </div>
                    </div>
                    {s3Status.error && (
                      <div className="mt-3 p-2 bg-red-600/20 border border-red-600/50 rounded text-red-400 text-sm">
                        <strong>오류:</strong> {s3Status.error}
                      </div>
                    )}
                    {s3Status.warning && (
                      <div className="mt-3 p-2 bg-yellow-600/20 border border-yellow-600/50 rounded text-yellow-400 text-sm">
                        <strong>경고:</strong> {s3Status.warning}
                      </div>
                    )}
                    {s3Status.suggestion && (
                      <div className="mt-2 p-2 bg-blue-600/20 border border-blue-600/50 rounded text-blue-400 text-sm">
                        <strong>해결 방법:</strong> {s3Status.suggestion}
                      </div>
                    )}
                  </div>
                )}
              </div>
              
              {/* 파일 관리 정보 카드 */}
              <div className="bg-[#2a2e45] p-6 rounded-lg">
                <h4 className="text-lg font-semibold text-white mb-4">📋 파일 관리 정보</h4>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                  <div className="bg-[#1f2336] p-4 rounded-lg">
                    <div className="text-blue-400 font-medium mb-2">🔄 자동 정리 정책</div>
                    <ul className="text-gray-300 space-y-1">
                      <li>• 업로드 후 7일 경과 파일 대상</li>
                      <li>• 게시글에 사용되지 않은 파일만</li>
                      <li>• AWS S3 Lifecycle 정책 적용</li>
                    </ul>
                  </div>
                  <div className="bg-[#1f2336] p-4 rounded-lg">
                    <div className="text-green-400 font-medium mb-2">💡 관리 팁</div>
                    <ul className="text-gray-300 space-y-1">
                      <li>• 정기적인 파일 정리 권장</li>
                      <li>• 스토리지 비용 절약 효과</li>
                      <li>• 시스템 성능 최적화</li>
                      <li>• S3 상태 버튼으로 연결 문제 진단</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
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
            setSelectedReportComment(null);
          }}
          postTitle={selectedReportPost ? selectedReportPost.title : (selectedReportComment ? `댓글: ${selectedReportComment.content.substring(0, 30)}...` : '')}
          reports={selectedReportPost ? selectedReportPost.reportDetails : (selectedReportComment ? selectedReportComment.reportDetails : [])}
          onApprove={handleApproveReport}
          onReject={handleRejectReport}
          onRefresh={() => {
            if (selectedReportPost) fetchReportedPosts();
            if (selectedReportComment) fetchReportedComments();
          }}
        />
      </div>
    </div>
  );
}
