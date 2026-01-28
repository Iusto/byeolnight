import { useEffect, useState, useCallback, useMemo } from 'react';
import axios from '../../lib/axios';
import { useAuth } from '../../contexts/AuthContext';
import { useToast } from '../../contexts/ToastContext';
import { useConfirm } from '../../contexts/ConfirmContext';
import {
  AdminReasonModal,
  PointAwardModal,
  AdminPageHeader,
  AdminSearchFilter,
  AdminStatsCard,
  AdminStatusBadge,
  AdminTable,
  Column,
} from '../../components/admin';
import { grantNicknameChangeTicket } from '../../lib/api/admin';
import { getErrorMessage } from '../../types/api';

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

export default function AdminUsersPage() {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [userTypeFilter, setUserTypeFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [showReasonModal, setShowReasonModal] = useState(false);
  const [showPointModal, setShowPointModal] = useState(false);
  const [modalAction, setModalAction] = useState<{ type: string; userId: number; status?: string } | null>(null);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const { user: currentUser } = useAuth();
  const toast = useToast();
  const confirm = useConfirm();

  const fetchUsers = useCallback(async () => {
    try {
      const res = await axios.get('/admin/users');
      const userData = res.data?.data || res.data || [];
      setUsers(Array.isArray(userData) ? userData : []);
    } catch (err) {
      console.error('사용자 목록 조회 실패', err);
      setUsers([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const filteredUsers = useMemo(() => {
    if (!users || users.length === 0) return [];

    return users.filter(user => {
      const statusMatch = statusFilter === 'ALL' ||
        (statusFilter === 'ACTIVE' && user.status !== 'WITHDRAWN') ||
        (statusFilter === 'WITHDRAWN' && user.status === 'WITHDRAWN');

      const typeMatch = userTypeFilter === 'ALL' ||
        (userTypeFilter === 'REGULAR' && !user.socialProvider) ||
        (userTypeFilter === 'SOCIAL' && !!user.socialProvider);

      const searchMatch = searchTerm === '' ||
        user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
        user.nickname.toLowerCase().includes(searchTerm.toLowerCase());

      return statusMatch && typeMatch && searchMatch;
    });
  }, [users, statusFilter, userTypeFilter, searchTerm]);

  // 핸들러 함수들
  const handleLock = async (id: number) => {
    if (currentUser && currentUser.id === id) {
      toast.warning('자기 자신의 계정은 잠금할 수 없습니다.');
      return;
    }
    const confirmed = await confirm({
      title: '계정 잠금',
      message: '정말 이 사용자의 계정을 잠금할까요?',
      confirmText: '잠금',
      type: 'warning',
    });
    if (!confirmed) return;
    try {
      await axios.patch(`/admin/users/${id}/lock`);
      toast.success('계정이 잠금되었습니다.');
      fetchUsers();
    } catch (err) {
      console.error('잠금 실패:', err);
      toast.error('잠금에 실패했습니다.');
    }
  };

  const handleUnlock = async (id: number) => {
    const confirmed = await confirm({
      title: '잠금 해제',
      message: '정말 이 사용자의 계정을 잠금 해제할까요?',
      confirmText: '해제',
      type: 'info',
    });
    if (!confirmed) return;
    try {
      await axios.patch(`/admin/users/${id}/unlock`);
      toast.success('계정 잠금이 해제되었습니다.');
      fetchUsers();
    } catch (err) {
      console.error('잠금 해제 실패:', err);
      toast.error('잠금 해제에 실패했습니다.');
    }
  };

  const handleStatusChange = async (id: number, status: string) => {
    if (currentUser && currentUser.id === id) {
      toast.warning('자기 자신의 상태는 변경할 수 없습니다.');
      return;
    }
    const confirmed = await confirm({
      title: '상태 변경',
      message: `정말 이 사용자의 상태를 ${status}로 변경하시겠습니까?`,
      confirmText: '변경',
      type: 'warning',
    });
    if (!confirmed) return;
    setModalAction({ type: 'status', userId: id, status });
    setShowReasonModal(true);
  };

  const handleDelete = async (id: number) => {
    if (currentUser && currentUser.id === id) {
      toast.warning('자기 자신의 계정은 탈퇴시킬 수 없습니다.');
      return;
    }
    const confirmed = await confirm({
      title: '강제 탈퇴',
      message: '정말 이 사용자를 강제 탈퇴시킬까요? 이 작업은 되돌릴 수 없습니다.',
      confirmText: '탈퇴',
      type: 'danger',
    });
    if (!confirmed) return;
    setModalAction({ type: 'delete', userId: id });
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
        toast.success(`사용자 상태가 ${modalAction.status}로 변경되었습니다.`);
      } else if (modalAction.type === 'delete') {
        await axios.delete(`/admin/users/${modalAction.userId}?reason=${encodeURIComponent(reason)}`);
        toast.success('사용자가 강제 탈퇴 처리되었습니다.');
      }
      fetchUsers();
    } catch (err) {
      console.error('작업 실패:', err);
      toast.error('작업에 실패했습니다.');
    } finally {
      setModalAction(null);
    }
  };

  const handleAwardPoints = useCallback(async (points: number, reason: string) => {
    if (!selectedUserId) return;
    try {
      const response = await axios.post(`/admin/users/${selectedUserId}/points`, { points, reason });
      const message = response.data?.data || response.data?.message || `${points}포인트가 성공적으로 수여되었습니다.`;
      toast.success(message);
      fetchUsers();
    } catch (error: unknown) {
      console.error('포인트 수여 실패:', error);
      toast.error(getErrorMessage(error));
    } finally {
      setShowPointModal(false);
      setSelectedUserId(null);
    }
  }, [selectedUserId, fetchUsers, toast]);

  const handleGrantNicknameChangeTicket = async (userId: number) => {
    const confirmed = await confirm({
      title: '닉네임 변경권 수여',
      message: '이 사용자에게 닉네임 변경권을 수여하시겠습니까?',
      confirmText: '수여',
      type: 'info',
    });
    if (!confirmed) return;
    try {
      await grantNicknameChangeTicket(userId);
      toast.success('닉네임 변경권이 수여되었습니다.');
      fetchUsers();
    } catch (err) {
      console.error('닉네임 변경권 수여 실패:', err);
      toast.error('닉네임 변경권 수여에 실패했습니다.');
    }
  };

  // 테이블 컬럼 정의
  const columns: Column<UserSummary>[] = [
    {
      key: 'id',
      header: 'ID',
      className: 'text-center',
      render: (user) => <span className="text-gray-400">{user.id}</span>,
    },
    {
      key: 'email',
      header: '이메일',
      render: (user) => <span className="text-white">{user.email}</span>,
    },
    {
      key: 'nickname',
      header: '닉네임',
      render: (user) => <span className="text-white">{user.nickname}</span>,
    },
    {
      key: 'socialProvider',
      header: '유형',
      className: 'text-center',
      render: (user) => (
        <AdminStatusBadge
          status={user.socialProvider || 'REGULAR'}
          variant="type"
        />
      ),
    },
    {
      key: 'nicknameTicket',
      header: '닉네임 변경권',
      className: 'text-center',
      render: (user) => (
        <button
          onClick={() => handleGrantNicknameChangeTicket(user.id)}
          className="bg-purple-600 hover:bg-purple-700 text-white px-3 py-1 rounded text-xs font-medium transition disabled:opacity-50"
          disabled={user.status === 'WITHDRAWN'}
        >
          티켓 수여
        </button>
      ),
    },
    {
      key: 'role',
      header: '권한',
      className: 'text-center',
      render: (user) => <AdminStatusBadge status={user.role} variant="role" />,
    },
    {
      key: 'status',
      header: '상태',
      className: 'text-center',
      render: (user) => <AdminStatusBadge status={user.status} />,
    },
    {
      key: 'points',
      header: '포인트',
      className: 'text-center',
      render: (user) => (
        <div className="flex items-center justify-center gap-2">
          <span className="text-yellow-400">*</span>
          <span className="font-bold text-white">{user.points?.toLocaleString() || 0}</span>
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
      ),
    },
    {
      key: 'actions',
      header: '조치',
      className: 'text-center',
      render: (user) => (
        <div className="space-x-1">
          {user.status === 'WITHDRAWN' ? (
            <span className="text-gray-400 text-xs">탈퇴된 계정</span>
          ) : (
            <>
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
              <button
                onClick={() => handleDelete(user.id)}
                className="bg-gray-600 hover:bg-gray-700 text-white px-2 py-1 rounded text-xs font-medium"
              >
                탈퇴
              </button>
            </>
          )}
        </div>
      ),
    },
  ];

  // 필터 그룹 정의
  const filterGroups = [
    {
      options: [
        { value: 'ALL', label: '전체', color: 'bg-blue-600' },
        { value: 'ACTIVE', label: '활성 계정', color: 'bg-green-600' },
        { value: 'WITHDRAWN', label: '탈퇴 계정', color: 'bg-red-600' },
      ],
      value: statusFilter,
      onChange: setStatusFilter,
    },
    {
      options: [
        { value: 'ALL', label: '전체 유형', color: 'bg-purple-600' },
        { value: 'REGULAR', label: '일반 사용자', color: 'bg-orange-600' },
        { value: 'SOCIAL', label: '소셜 사용자', color: 'bg-cyan-600' },
      ],
      value: userTypeFilter,
      onChange: setUserTypeFilter,
    },
  ];

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="사용자 관리"
        description="계정 상태 및 권한을 관리합니다."
      />

      <AdminSearchFilter
        searchValue={searchTerm}
        onSearchChange={setSearchTerm}
        searchPlaceholder="이메일, 닉네임으로 검색..."
        filterGroups={filterGroups}
      />

      {/* 사용자 통계 */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <AdminStatsCard
          title="전체 사용자"
          value={users.length}
          color="purple"
        />
        <AdminStatsCard
          title="활성 사용자"
          value={users.filter(u => u.status === 'ACTIVE').length}
          color="green"
        />
        <AdminStatsCard
          title="탈퇴 사용자"
          value={users.filter(u => u.status === 'WITHDRAWN').length}
          color="red"
        />
        <AdminStatsCard
          title="검색 결과"
          value={filteredUsers.length}
          color="cyan"
        />
      </div>

      <AdminTable
        columns={columns}
        data={filteredUsers}
        keyExtractor={(user) => user.id}
        emptyMessage="사용자 데이터가 없습니다."
        loading={loading}
      />

      {/* 모달들 */}
      {showReasonModal && (
        <AdminReasonModal
          onClose={() => {
            setShowReasonModal(false);
            setModalAction(null);
          }}
          onConfirm={handleReasonConfirm}
          title={modalAction?.type === 'delete' ? '강제 탈퇴 사유' : '상태 변경 사유'}
        />
      )}

      {showPointModal && selectedUserId && (
        <PointAwardModal
          onClose={() => {
            setShowPointModal(false);
            setSelectedUserId(null);
          }}
          onConfirm={handleAwardPoints}
        />
      )}
    </div>
  );
}