import { useEffect, useState, useCallback } from 'react';
import axios from '../../lib/axios';
import { AdminPageHeader, AdminStatsCard } from '../../components/admin';

interface SchedulerStatus {
  messagesToDelete: number;
  postsToDelete: number;
  usersToCleanup: number;
}

interface SchedulerItem {
  key: 'message' | 'post' | 'user';
  title: string;
  description: string;
  icon: string;
  timeIcon: string;
  countLabel: string;
  count: number;
  color: string;
  buttonColor: string;
}

export default function AdminSchedulerPage() {
  const [schedulerStatus, setSchedulerStatus] = useState<SchedulerStatus>({
    messagesToDelete: 0,
    postsToDelete: 0,
    usersToCleanup: 0,
  });
  const [isRunning, setIsRunning] = useState({
    message: false,
    post: false,
    user: false,
  });
  const [loading, setLoading] = useState(true);

  const fetchSchedulerStatus = useCallback(async () => {
    try {
      const res = await axios.get('/admin/scheduler/status');
      const status = res.data?.data || { messagesToDelete: 0, postsToDelete: 0, usersToCleanup: 0 };
      setSchedulerStatus(status);
    } catch (err) {
      console.error('스케줄러 상태 조회 실패:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSchedulerStatus();
  }, [fetchSchedulerStatus]);

  const handleManualScheduler = async (type: 'message' | 'post' | 'user') => {
    const confirmMessages = {
      message: `정말 ${schedulerStatus.messagesToDelete || 0}개의 오래된 쪽지를 영구 삭제하시겠습니까?`,
      post: `정말 ${schedulerStatus.postsToDelete || 0}개의 만료된 게시글을 정리하시겠습니까?`,
      user: `정말 ${schedulerStatus.usersToCleanup || 0}명의 탈퇴 회원 데이터를 완전 삭제하시겠습니까?`
    };

    if (!confirm(confirmMessages[type])) return;

    setIsRunning(prev => ({ ...prev, [type]: true }));

    const endpoints = {
      message: '/admin/scheduler/message-cleanup/manual',
      post: '/admin/scheduler/post-cleanup/manual',
      user: '/admin/scheduler/user-cleanup/manual'
    };

    try {
      const res = await axios.post(endpoints[type]);
      const message = res.data?.message || '스케줄러 실행이 완료되었습니다.';
      alert(message);
      fetchSchedulerStatus();
    } catch (err) {
      console.error('스케줄러 실행 실패:', err);
      alert('스케줄러 실행에 실패했습니다.');
    } finally {
      setIsRunning(prev => ({ ...prev, [type]: false }));
    }
  };

  const schedulerItems: SchedulerItem[] = [
    {
      key: 'message',
      title: '쪽지 정리 스케줄러',
      description: '매일 새벽 2시 - 양쪽 모두 삭제 후 3년 경과한 쪽지를 영구 삭제합니다.',
      icon: '💌',
      timeIcon: '🕐',
      countLabel: '정리 대상 쪽지',
      count: schedulerStatus.messagesToDelete,
      color: 'blue',
      buttonColor: 'bg-blue-600 hover:bg-blue-700',
    },
    {
      key: 'post',
      title: '게시글 정리 스케줄러',
      description: '매일 아침 8시 - 삭제 후 30일 경과한 게시글과 댓글을 영구 삭제합니다.',
      icon: '📝',
      timeIcon: '🕗',
      countLabel: '정리 대상 게시글',
      count: schedulerStatus.postsToDelete,
      color: 'yellow',
      buttonColor: 'bg-yellow-600 hover:bg-yellow-700',
    },
    {
      key: 'user',
      title: '탈퇴 회원 정리 스케줄러',
      description: '매일 오전 10시 - 탈퇴 후 2년 경과한 회원 데이터를 완전 삭제합니다.',
      icon: '👤',
      timeIcon: '🕙',
      countLabel: '정리 대상 회원',
      count: schedulerStatus.usersToCleanup,
      color: 'red',
      buttonColor: 'bg-red-600 hover:bg-red-700',
    },
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-purple-500"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <AdminPageHeader
          title="스케줄러 관리"
          description="자동 정리 작업을 관리합니다."
        />
        <button
          onClick={fetchSchedulerStatus}
          className="bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded-lg text-sm transition"
        >
          🔄 새로고침
        </button>
      </div>

      {/* 통계 */}
      <div className="grid grid-cols-3 gap-4">
        <AdminStatsCard
          title="쪽지 정리 대상"
          value={schedulerStatus.messagesToDelete}
          icon="💌"
          color="blue"
        />
        <AdminStatsCard
          title="게시글 정리 대상"
          value={schedulerStatus.postsToDelete}
          icon="📝"
          color="yellow"
        />
        <AdminStatsCard
          title="회원 정리 대상"
          value={schedulerStatus.usersToCleanup}
          icon="👤"
          color="red"
        />
      </div>

      {/* 스케줄러 카드들 */}
      <div className="grid gap-6">
        {schedulerItems.map((item) => (
          <div key={item.key} className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-lg font-semibold text-white mb-2 flex items-center gap-2">
                  <span>{item.icon}</span>
                  {item.title}
                </h3>
                <p className="text-gray-400 text-sm">{item.description}</p>
              </div>
              <div className="text-2xl">{item.timeIcon}</div>
            </div>

            <div className="flex items-center justify-between bg-[#2a2e45] p-4 rounded-lg">
              <div className="flex items-center gap-4">
                <div className="text-3xl">📊</div>
                <div>
                  <div className="text-2xl font-bold text-white">
                    {item.count.toLocaleString()}{item.key === 'user' ? '명' : '개'}
                  </div>
                  <div className="text-sm text-gray-400">{item.countLabel}</div>
                </div>
              </div>

              <button
                onClick={() => handleManualScheduler(item.key)}
                disabled={item.count === 0 || isRunning[item.key]}
                className={`px-6 py-3 rounded-lg font-medium transition ${
                  item.count === 0 || isRunning[item.key]
                    ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                    : `${item.buttonColor} text-white hover:scale-105 shadow-lg`
                }`}
              >
                {isRunning[item.key] ? (
                  <div className="flex items-center gap-2">
                    <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
                    실행 중...
                  </div>
                ) : (
                  '🧹 수동 실행'
                )}
              </button>
            </div>

            {item.count === 0 && (
              <div className="mt-4 p-3 bg-green-600/20 border border-green-600/50 rounded-lg">
                <div className="flex items-center gap-2 text-green-400">
                  <span>✅</span>
                  <span className="text-sm">정리할 항목이 없습니다.</span>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>

      {/* 스케줄러 정보 */}
      <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
        <h3 className="text-lg font-semibold text-white mb-4">📋 스케줄러 정보</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
          <div className="bg-[#2a2e45] p-4 rounded-lg">
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
          <div className="bg-[#2a2e45] p-4 rounded-lg">
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
          <div className="bg-[#2a2e45] p-4 rounded-lg">
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
  );
}