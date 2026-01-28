import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from '../../lib/axios';

interface DashboardStats {
  totalUsers: number;
  activeUsers: number;
  todayNewUsers: number;
  reportedPosts: number;
  reportedComments: number;
  blockedIps: number;
  blindedPosts: number;
  blindedComments: number;
  schedulerStatus: {
    messagesToDelete: number;
    postsToDelete: number;
    usersToCleanup: number;
  };
}

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<DashboardStats>({
    totalUsers: 0,
    activeUsers: 0,
    todayNewUsers: 0,
    reportedPosts: 0,
    reportedComments: 0,
    blockedIps: 0,
    blindedPosts: 0,
    blindedComments: 0,
    schedulerStatus: {
      messagesToDelete: 0,
      postsToDelete: 0,
      usersToCleanup: 0,
    },
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const [
          usersRes,
          reportedPostsRes,
          reportedCommentsRes,
          blockedIpsRes,
          blindedPostsRes,
          blindedCommentsRes,
          schedulerRes,
        ] = await Promise.all([
          axios.get('/admin/users'),
          axios.get('/admin/posts/reported'),
          axios.get('/admin/comments/reported'),
          axios.get('/admin/blocked-ips'),
          axios.get('/admin/posts/blinded'),
          axios.get('/admin/comments/blinded'),
          axios.get('/admin/scheduler/status').catch(() => ({ data: { data: { messagesToDelete: 0, postsToDelete: 0, usersToCleanup: 0 } } })),
        ]);

        const users = usersRes.data?.data || usersRes.data || [];
        const reportedPosts = reportedPostsRes.data?.data || reportedPostsRes.data || [];
        const reportedComments = reportedCommentsRes.data?.data || reportedCommentsRes.data || [];
        const blockedIps = blockedIpsRes.data?.data || blockedIpsRes.data || [];
        const blindedPosts = blindedPostsRes.data?.data || blindedPostsRes.data || [];
        const blindedComments = blindedCommentsRes.data?.data || blindedCommentsRes.data || [];
        const schedulerStatus = schedulerRes.data?.data || { messagesToDelete: 0, postsToDelete: 0, usersToCleanup: 0 };

        // 오늘 가입한 사용자 수 계산
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const todayNewUsers = Array.isArray(users)
          ? users.filter((u: { createdAt: string }) => {
              const createdAt = new Date(u.createdAt);
              return createdAt >= today;
            }).length
          : 0;

        setStats({
          totalUsers: Array.isArray(users) ? users.length : 0,
          activeUsers: Array.isArray(users) ? users.filter((u: { status: string }) => u.status === 'ACTIVE').length : 0,
          todayNewUsers,
          reportedPosts: Array.isArray(reportedPosts) ? reportedPosts.length : 0,
          reportedComments: Array.isArray(reportedComments) ? reportedComments.length : 0,
          blockedIps: Array.isArray(blockedIps) ? blockedIps.length : 0,
          blindedPosts: Array.isArray(blindedPosts) ? blindedPosts.length : 0,
          blindedComments: Array.isArray(blindedComments) ? blindedComments.length : 0,
          schedulerStatus,
        });
      } catch (err) {
        console.error('대시보드 통계 조회 실패:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  const totalSchedulerItems = stats.schedulerStatus.messagesToDelete + stats.schedulerStatus.postsToDelete + stats.schedulerStatus.usersToCleanup;

  const statCards = [
    {
      title: '전체 사용자',
      value: stats.totalUsers,
      subValue: `활성: ${stats.activeUsers}`,
      icon: '👥',
      color: 'from-blue-500 to-blue-600',
      link: '/admin/users',
    },
    {
      title: '오늘 신규 가입',
      value: stats.todayNewUsers,
      subValue: '오늘 가입한 회원',
      icon: '🆕',
      color: 'from-green-500 to-green-600',
      link: '/admin/users',
    },
    {
      title: '신고된 게시글',
      value: stats.reportedPosts,
      subValue: `블라인드: ${stats.blindedPosts}`,
      icon: '📝',
      color: 'from-orange-500 to-orange-600',
      link: '/admin/posts',
      alert: stats.reportedPosts > 0,
    },
    {
      title: '신고된 댓글',
      value: stats.reportedComments,
      subValue: `블라인드: ${stats.blindedComments}`,
      icon: '💬',
      color: 'from-yellow-500 to-yellow-600',
      link: '/admin/comments',
      alert: stats.reportedComments > 0,
    },
    {
      title: '차단된 IP',
      value: stats.blockedIps,
      subValue: '현재 차단 중',
      icon: '🚫',
      color: 'from-red-500 to-red-600',
      link: '/admin/ips',
    },
    {
      title: '정리 대기',
      value: totalSchedulerItems,
      subValue: `쪽지 ${stats.schedulerStatus.messagesToDelete} / 게시글 ${stats.schedulerStatus.postsToDelete} / 회원 ${stats.schedulerStatus.usersToCleanup}`,
      icon: '🧹',
      color: 'from-purple-500 to-purple-600',
      link: '/admin/scheduler',
      alert: totalSchedulerItems > 100,
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
      {/* 헤더 */}
      <div>
        <h1 className="text-2xl font-bold text-white">관리자 대시보드</h1>
        <p className="text-gray-400 mt-1">사이트 현황을 한눈에 확인하세요.</p>
      </div>

      {/* 알림 배너 */}
      {(stats.reportedPosts > 0 || stats.reportedComments > 0) && (
        <div className="bg-orange-500/20 border border-orange-500/50 rounded-lg p-4 flex items-center gap-3">
          <span className="text-2xl">⚠️</span>
          <div>
            <p className="text-orange-200 font-medium">처리가 필요한 신고가 있습니다</p>
            <p className="text-orange-300/70 text-sm">
              게시글 {stats.reportedPosts}건, 댓글 {stats.reportedComments}건의 신고가 대기 중입니다.
            </p>
          </div>
        </div>
      )}

      {/* 통계 카드 그리드 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {statCards.map((card) => (
          <Link
            key={card.title}
            to={card.link}
            className="relative bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-5 border border-purple-500/20 hover:border-purple-500/40 transition-all duration-200 group"
          >
            {card.alert && (
              <span className="absolute top-3 right-3 w-3 h-3 bg-red-500 rounded-full animate-pulse" />
            )}
            <div className={`inline-flex p-3 rounded-lg bg-gradient-to-br ${card.color} mb-3`}>
              <span className="text-2xl">{card.icon}</span>
            </div>
            <p className="text-gray-400 text-sm">{card.title}</p>
            <p className="text-3xl font-bold text-white mt-1">{card.value}</p>
            <p className="text-gray-500 text-xs mt-1">{card.subValue}</p>
          </Link>
        ))}
      </div>

      {/* 빠른 링크 */}
      <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-5 border border-purple-500/20">
        <h2 className="text-lg font-semibold text-white mb-4">빠른 관리</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <Link
            to="/admin/users"
            className="px-4 py-3 bg-purple-500/20 hover:bg-purple-500/30 rounded-lg text-purple-300 text-sm text-center transition-colors"
          >
            사용자 관리
          </Link>
          <Link
            to="/admin/posts"
            className="px-4 py-3 bg-purple-500/20 hover:bg-purple-500/30 rounded-lg text-purple-300 text-sm text-center transition-colors"
          >
            게시글 관리
          </Link>
          <Link
            to="/admin/files"
            className="px-4 py-3 bg-purple-500/20 hover:bg-purple-500/30 rounded-lg text-purple-300 text-sm text-center transition-colors"
          >
            파일 정리
          </Link>
          <Link
            to="/admin/scheduler"
            className="px-4 py-3 bg-purple-500/20 hover:bg-purple-500/30 rounded-lg text-purple-300 text-sm text-center transition-colors"
          >
            스케줄러
          </Link>
        </div>
      </div>
    </div>
  );
}