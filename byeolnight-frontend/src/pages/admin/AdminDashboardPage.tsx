import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from '../../lib/axios';
import CinemaManagementCard from '../../components/admin/CinemaManagementCard';

interface UserSummary {
  status: string;
  createdAt: string;
}

interface DashboardStats {
  totalUsers: number;
  activeUsers: number;
  suspendedUsers: number;
  withdrawnUsers: number;
  todayNewUsers: number;
  pendingPostReports: number;
  pendingCommentReports: number;
  blockedIps: number;
  blindedPosts: number;
  blindedComments: number;
  schedulerStatus: {
    messagesToDelete: number;
    postsToDelete: number;
    usersToCleanup: number;
  };
}

function BarChart({ label, value, total, color }: { label: string; value: number; total: number; color: string }) {
  const pct = total > 0 ? Math.round((value / total) * 100) : 0;
  return (
    <div>
      <div className="flex justify-between text-sm mb-1">
        <span className="text-gray-300">{label}</span>
        <span className="text-white font-semibold">{value.toLocaleString()} ({pct}%)</span>
      </div>
      <div className="h-3 bg-gray-700 rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full transition-all duration-700 ${color}`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}

function DonutRing({ value, total, color, label }: { value: number; total: number; color: string; label: string }) {
  const pct = total > 0 ? Math.round((value / total) * 100) : 0;
  const circumference = 2 * Math.PI * 36;
  const dash = (pct / 100) * circumference;
  return (
    <div className="flex flex-col items-center">
      <div className="relative w-24 h-24">
        <svg viewBox="0 0 80 80" className="w-24 h-24 -rotate-90">
          <circle cx="40" cy="40" r="36" fill="none" stroke="#374151" strokeWidth="8" />
          <circle
            cx="40" cy="40" r="36" fill="none"
            stroke={color} strokeWidth="8"
            strokeDasharray={`${dash} ${circumference}`}
            strokeLinecap="round"
            className="transition-all duration-700"
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-xl font-bold text-white">{pct}%</span>
        </div>
      </div>
      <span className="text-xs text-gray-400 mt-1">{label}</span>
      <span className="text-sm font-semibold text-white">{value.toLocaleString()}</span>
    </div>
  );
}

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<DashboardStats>({
    totalUsers: 0,
    activeUsers: 0,
    suspendedUsers: 0,
    withdrawnUsers: 0,
    todayNewUsers: 0,
    pendingPostReports: 0,
    pendingCommentReports: 0,
    blockedIps: 0,
    blindedPosts: 0,
    blindedComments: 0,
    schedulerStatus: { messagesToDelete: 0, postsToDelete: 0, usersToCleanup: 0 },
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const [
          usersRes,
          reportStatsRes,
          blockedIpsRes,
          blindedPostsRes,
          blindedCommentsRes,
          schedulerRes,
        ] = await Promise.all([
          axios.get('/admin/users'),
          axios.get('/admin/dashboard/report-stats'),
          axios.get('/admin/blocked-ips'),
          axios.get('/admin/posts/blinded'),
          axios.get('/admin/comments/blinded'),
          axios.get('/admin/scheduler/status').catch(() => ({
            data: { data: { messagesToDelete: 0, postsToDelete: 0, usersToCleanup: 0 } },
          })),
        ]);

        const users: UserSummary[] = usersRes.data?.data || usersRes.data || [];
        const reportStats = reportStatsRes.data?.data || reportStatsRes.data || {};
        const blockedIps = blockedIpsRes.data?.data || blockedIpsRes.data || [];
        const blindedPosts = blindedPostsRes.data?.data || blindedPostsRes.data || [];
        const blindedComments = blindedCommentsRes.data?.data || blindedCommentsRes.data || [];
        const schedulerStatus = schedulerRes.data?.data || { messagesToDelete: 0, postsToDelete: 0, usersToCleanup: 0 };

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        setStats({
          totalUsers: Array.isArray(users) ? users.length : 0,
          activeUsers: Array.isArray(users) ? users.filter(u => u.status === 'ACTIVE').length : 0,
          suspendedUsers: Array.isArray(users) ? users.filter(u => u.status === 'SUSPENDED' || u.status === 'BANNED').length : 0,
          withdrawnUsers: Array.isArray(users) ? users.filter(u => u.status === 'WITHDRAWN').length : 0,
          todayNewUsers: Array.isArray(users) ? users.filter(u => new Date(u.createdAt) >= today).length : 0,
          pendingPostReports: Number(reportStats.pendingPostReports) || 0,
          pendingCommentReports: Number(reportStats.pendingCommentReports) || 0,
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

  const totalSchedulerItems =
    stats.schedulerStatus.messagesToDelete +
    stats.schedulerStatus.postsToDelete +
    stats.schedulerStatus.usersToCleanup;

  const totalContent = stats.pendingPostReports + stats.pendingCommentReports + stats.blindedPosts + stats.blindedComments;

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-purple-500" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">관리자 대시보드</h1>
        <p className="text-gray-400 mt-1">사이트 현황을 한눈에 확인하세요.</p>
      </div>

      {/* 알림 배너 */}
      {(stats.pendingPostReports > 0 || stats.pendingCommentReports > 0) && (
        <div className="bg-orange-500/20 border border-orange-500/50 rounded-lg p-4 flex items-center gap-3">
          <span className="text-2xl">⚠️</span>
          <div>
            <p className="text-orange-200 font-medium">처리가 필요한 신고가 있습니다</p>
            <p className="text-orange-300/70 text-sm">
              게시글 {stats.pendingPostReports}건, 댓글 {stats.pendingCommentReports}건의 신고가 대기 중입니다.
            </p>
          </div>
          <div className="ml-auto flex gap-2">
            {stats.pendingPostReports > 0 && (
              <Link to="/admin/posts" className="px-3 py-1.5 bg-orange-600 hover:bg-orange-700 text-white text-sm rounded-lg transition">
                게시글 신고
              </Link>
            )}
            {stats.pendingCommentReports > 0 && (
              <Link to="/admin/comments" className="px-3 py-1.5 bg-orange-600 hover:bg-orange-700 text-white text-sm rounded-lg transition">
                댓글 신고
              </Link>
            )}
          </div>
        </div>
      )}

      {/* 요약 수치 카드 */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: '전체 사용자', value: stats.totalUsers, icon: '👥', color: 'text-blue-400', link: '/admin/users' },
          { label: '오늘 신규', value: stats.todayNewUsers, icon: '🆕', color: 'text-green-400', link: '/admin/users' },
          { label: '차단된 IP', value: stats.blockedIps, icon: '🚫', color: 'text-red-400', link: '/admin/ips' },
          { label: '정리 대기', value: totalSchedulerItems, icon: '🧹', color: 'text-purple-400', link: '/admin/scheduler', alert: totalSchedulerItems > 100 },
        ].map(card => (
          <Link
            key={card.label}
            to={card.link}
            className="relative bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-5 border border-purple-500/20 hover:border-purple-500/40 transition-all group"
          >
            {card.alert && <span className="absolute top-3 right-3 w-2.5 h-2.5 bg-red-500 rounded-full animate-pulse" />}
            <p className="text-gray-400 text-sm">{card.icon} {card.label}</p>
            <p className={`text-3xl font-bold mt-1 ${card.color}`}>{card.value.toLocaleString()}</p>
          </Link>
        ))}
      </div>

      {/* 차트 영역 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        <CinemaManagementCard />

        {/* 사용자 현황 - 바 차트 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
          <h2 className="text-lg font-semibold text-white mb-5 flex items-center gap-2">
            👥 사용자 현황
            <span className="text-sm font-normal text-gray-400">총 {stats.totalUsers.toLocaleString()}명</span>
          </h2>
          <div className="space-y-4">
            <BarChart label="활성 사용자" value={stats.activeUsers} total={stats.totalUsers} color="bg-green-500" />
            <BarChart label="정지/차단 사용자" value={stats.suspendedUsers} total={stats.totalUsers} color="bg-orange-500" />
            <BarChart label="탈퇴 사용자" value={stats.withdrawnUsers} total={stats.totalUsers} color="bg-gray-500" />
          </div>
          <Link to="/admin/users" className="mt-4 block text-center text-sm text-purple-400 hover:text-purple-300 transition">
            사용자 관리 →
          </Link>
        </div>

        {/* 콘텐츠 현황 - 도넛 링 차트 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
          <h2 className="text-lg font-semibold text-white mb-5 flex items-center gap-2">
            📝 콘텐츠 현황
            <span className="text-sm font-normal text-gray-400">관리 항목 {totalContent}건</span>
          </h2>
          {totalContent === 0 ? (
            <div className="flex flex-col items-center justify-center h-32 text-green-400">
              <span className="text-4xl mb-2">✅</span>
              <span className="text-sm">모든 콘텐츠가 정상입니다</span>
            </div>
          ) : (
            <div className="flex justify-around items-end">
              <DonutRing value={stats.pendingPostReports} total={Math.max(totalContent, 1)} color="#f97316" label="대기 신고(게시글)" />
              <DonutRing value={stats.pendingCommentReports} total={Math.max(totalContent, 1)} color="#eab308" label="대기 신고(댓글)" />
              <DonutRing value={stats.blindedPosts} total={Math.max(totalContent, 1)} color="#ef4444" label="블라인드 게시글" />
              <DonutRing value={stats.blindedComments} total={Math.max(totalContent, 1)} color="#6b7280" label="블라인드 댓글" />
            </div>
          )}
          <div className="flex gap-2 mt-4 justify-center">
            <Link to="/admin/posts" className="text-sm text-orange-400 hover:text-orange-300 transition">게시글 →</Link>
            <span className="text-gray-600">|</span>
            <Link to="/admin/comments" className="text-sm text-yellow-400 hover:text-yellow-300 transition">댓글 →</Link>
          </div>
        </div>

        {/* 스케줄러 정리 현황 - 가로 바 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
          <h2 className="text-lg font-semibold text-white mb-5">⏰ 정리 대기 현황</h2>
          <div className="space-y-4">
            {[
              { label: '쪽지', value: stats.schedulerStatus.messagesToDelete, color: 'bg-blue-500', unit: '개' },
              { label: '게시글', value: stats.schedulerStatus.postsToDelete, color: 'bg-yellow-500', unit: '개' },
              { label: '탈퇴 회원', value: stats.schedulerStatus.usersToCleanup, color: 'bg-red-500', unit: '명' },
            ].map(item => {
              const maxVal = Math.max(
                stats.schedulerStatus.messagesToDelete,
                stats.schedulerStatus.postsToDelete,
                stats.schedulerStatus.usersToCleanup,
                1
              );
              const pct = Math.round((item.value / maxVal) * 100);
              return (
                <div key={item.label}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-gray-300">{item.label}</span>
                    <span className="text-white font-semibold">{item.value.toLocaleString()}{item.unit}</span>
                  </div>
                  <div className="h-3 bg-gray-700 rounded-full overflow-hidden">
                    <div className={`h-full rounded-full transition-all duration-700 ${item.color}`} style={{ width: `${pct}%` }} />
                  </div>
                </div>
              );
            })}
          </div>
          <Link to="/admin/scheduler" className="mt-4 block text-center text-sm text-purple-400 hover:text-purple-300 transition">
            스케줄러 관리 →
          </Link>
        </div>

        {/* 시스템 상태 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
          <h2 className="text-lg font-semibold text-white mb-5">🔧 시스템 상태</h2>
          <div className="space-y-3">
            {[
              { label: '대기 중인 게시글 신고', value: stats.pendingPostReports, ok: stats.pendingPostReports === 0, link: '/admin/posts' },
              { label: '대기 중인 댓글 신고', value: stats.pendingCommentReports, ok: stats.pendingCommentReports === 0, link: '/admin/comments' },
              { label: '차단 IP', value: stats.blockedIps, ok: true, link: '/admin/ips' },
              { label: '정리 대기 항목', value: totalSchedulerItems, ok: totalSchedulerItems < 100, link: '/admin/scheduler' },
            ].map(item => (
              <Link
                key={item.label}
                to={item.link}
                className="flex items-center justify-between p-3 bg-[#2a2e45] rounded-lg hover:bg-[#323654] transition"
              >
                <div className="flex items-center gap-2">
                  <span className={`w-2.5 h-2.5 rounded-full ${item.ok ? 'bg-green-500' : 'bg-red-500 animate-pulse'}`} />
                  <span className="text-gray-300 text-sm">{item.label}</span>
                </div>
                <span className={`text-sm font-semibold ${item.ok ? 'text-green-400' : 'text-red-400'}`}>
                  {item.value.toLocaleString()}건
                </span>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
