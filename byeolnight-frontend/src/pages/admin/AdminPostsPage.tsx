import { useEffect, useState, useCallback } from 'react';
import axios from '../../lib/axios';
import {
  AdminPageHeader,
  AdminSearchFilter,
  AdminStatsCard,
  ReportDetailModal,
} from '../../components/admin';
import { CATEGORY_LABELS } from '../../constants/postConstants';

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

type TabType = 'reported' | 'blinded' | 'deleted';

export default function AdminPostsPage() {
  const [activeTab, setActiveTab] = useState<TabType>('reported');
  const [blindedPosts, setBlindedPosts] = useState<BlindedPost[]>([]);
  const [reportedPosts, setReportedPosts] = useState<ReportedPost[]>([]);
  const [deletedPosts, setDeletedPosts] = useState<BlindedPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [showReportModal, setShowReportModal] = useState(false);
  const [selectedReportPost, setSelectedReportPost] = useState<ReportedPost | null>(null);

  const fetchData = useCallback(async () => {
    try {
      const [blindedRes, reportedRes, deletedRes] = await Promise.all([
        axios.get('/admin/posts/blinded'),
        axios.get('/admin/posts/reported'),
        axios.get('/admin/posts/deleted'),
      ]);
      setBlindedPosts(blindedRes.data?.data || blindedRes.data || []);
      setReportedPosts(reportedRes.data?.data || reportedRes.data || []);
      setDeletedPosts(deletedRes.data?.data || deletedRes.data || []);
    } catch (err) {
      console.error('게시글 데이터 조회 실패:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleUnblindPost = async (postId: number) => {
    if (!confirm('정말 이 게시글의 블라인드를 해제하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/posts/${postId}/unblind`);
      alert('블라인드가 해제되었습니다.');
      fetchData();
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
      fetchData();
    } catch (err) {
      console.error('블라인드 처리 실패:', err);
      alert('블라인드 처리에 실패했습니다.');
    }
  };

  const handleRestorePost = async (postId: number) => {
    if (!confirm('정말 이 게시글을 복구하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/posts/${postId}/restore`);
      alert('게시글이 복구되었습니다.');
      fetchData();
    } catch (err) {
      console.error('게시글 복구 실패:', err);
      alert('게시글 복구에 실패했습니다.');
    }
  };

  const handleApproveReport = async (reportId: number) => {
    try {
      await axios.patch(`/admin/reports/${reportId}/approve`);
      alert('신고가 승인되었습니다. 신고자들에게 포인트가 지급되었습니다.');
      fetchData();
    } catch (error) {
      console.error('신고 승인 실패:', error);
      alert('신고 승인에 실패했습니다.');
    }
  };

  const handleRejectReport = async (reportId: number, reason: string) => {
    try {
      await axios.patch(`/admin/reports/${reportId}/reject`, { reason });
      alert('신고가 거부되었습니다.');
      fetchData();
    } catch (error) {
      console.error('신고 거부 실패:', error);
      alert('신고 거부에 실패했습니다.');
    }
  };

  const getWriterName = (writer: { nickname: string } | string) => {
    return typeof writer === 'string' ? writer : writer.nickname;
  };

  const filterPosts = <T extends { title: string; writer: string | { nickname: string } }>(posts: T[]) => {
    if (!searchTerm) return posts;
    return posts.filter(post => {
      const writerName = typeof post.writer === 'string' ? post.writer : post.writer.nickname;
      return post.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
        writerName.toLowerCase().includes(searchTerm.toLowerCase());
    });
  };

  const tabs = [
    { key: 'reported' as TabType, label: '신고된 게시글', icon: '🚨', count: reportedPosts.length, color: 'orange' },
    { key: 'blinded' as TabType, label: '블라인드 게시글', icon: '🙈', count: blindedPosts.length, color: 'yellow' },
    { key: 'deleted' as TabType, label: '삭제된 게시글', icon: '🗑️', count: deletedPosts.length, color: 'red' },
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
      <AdminPageHeader
        title="게시글 관리"
        description="신고/블라인드/삭제된 게시글을 관리합니다."
      />

      {/* 통계 */}
      <div className="grid grid-cols-3 gap-4">
        <AdminStatsCard title="신고된 게시글" value={reportedPosts.length} icon="🚨" color="orange" alert={reportedPosts.length > 0} />
        <AdminStatsCard title="블라인드 게시글" value={blindedPosts.length} icon="🙈" color="yellow" />
        <AdminStatsCard title="삭제된 게시글" value={deletedPosts.length} icon="🗑️" color="red" />
      </div>

      {/* 탭 */}
      <div className="flex gap-2 border-b border-gray-700 pb-2">
        {tabs.map(tab => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2 rounded-t-lg flex items-center gap-2 transition ${
              activeTab === tab.key
                ? 'bg-purple-600 text-white'
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            <span>{tab.icon}</span>
            <span>{tab.label}</span>
            {tab.count > 0 && (
              <span className={`text-xs px-2 py-0.5 rounded-full ${
                activeTab === tab.key ? 'bg-white/20' : 'bg-gray-600'
              }`}>
                {tab.count}
              </span>
            )}
          </button>
        ))}
      </div>

      <AdminSearchFilter
        searchValue={searchTerm}
        onSearchChange={setSearchTerm}
        searchPlaceholder="제목, 작성자로 검색..."
      />

      {/* 신고된 게시글 */}
      {activeTab === 'reported' && (
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
          {filterPosts(reportedPosts).length === 0 ? (
            <p className="text-center text-gray-400 py-8">신고된 게시글이 없습니다.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-[#2a2e45] text-gray-300">
                  <tr>
                    <th className="p-3 text-left">제목</th>
                    <th className="p-3">작성자</th>
                    <th className="p-3">카테고리</th>
                    <th className="p-3">신고수</th>
                    <th className="p-3">상태</th>
                    <th className="p-3">작성일</th>
                    <th className="p-3">관리</th>
                  </tr>
                </thead>
                <tbody>
                  {filterPosts(reportedPosts).map(post => (
                    <tr key={post.id} className="border-t border-gray-700 hover:bg-[#252842]/50">
                      <td className="p-3">
                        <button
                          onClick={() => window.open(`/posts/${post.id}`, '_blank')}
                          className="text-blue-400 hover:text-blue-300 hover:underline text-left truncate max-w-xs block"
                        >
                          {post.title}
                        </button>
                      </td>
                      <td className="p-3 text-center text-gray-300">{post.writer}</td>
                      <td className="p-3 text-center">
                        <span className="text-xs px-2 py-1 bg-gray-600 rounded">{CATEGORY_LABELS[post.category] || post.category}</span>
                      </td>
                      <td className="p-3 text-center">
                        <button
                          onClick={() => { setSelectedReportPost(post); setShowReportModal(true); }}
                          className={`px-2 py-1 rounded text-xs font-medium ${
                            post.reportCount >= 5 ? 'bg-red-600 text-white' : 'bg-yellow-600 text-white'
                          }`}
                        >
                          🔍 {post.reportCount}
                        </button>
                      </td>
                      <td className="p-3 text-center">
                        <span className={`text-xs px-2 py-1 rounded ${post.blinded ? 'bg-red-600' : 'bg-green-600'}`}>
                          {post.blinded ? '블라인드' : '공개'}
                        </span>
                      </td>
                      <td className="p-3 text-center text-gray-400 text-xs">{new Date(post.createdAt).toLocaleDateString()}</td>
                      <td className="p-3 text-center">
                        {post.blinded ? (
                          <button onClick={() => handleUnblindPost(post.id)} className="px-2 py-1 bg-green-600 hover:bg-green-700 rounded text-xs">해제</button>
                        ) : (
                          <button onClick={() => handleBlindPost(post.id)} className="px-2 py-1 bg-orange-600 hover:bg-orange-700 rounded text-xs">블라인드</button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* 블라인드 게시글 */}
      {activeTab === 'blinded' && (
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
          {filterPosts(blindedPosts).length === 0 ? (
            <p className="text-center text-gray-400 py-8">블라인드 처리된 게시글이 없습니다.</p>
          ) : (
            <div className="grid gap-4">
              {filterPosts(blindedPosts).map(post => (
                <div key={post.id} className="bg-[#2a2e45] p-4 rounded-lg">
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <h4 className="text-white font-semibold mb-1">{post.title}</h4>
                      <p className="text-gray-400 text-sm mb-2">
                        작성자: {getWriterName(post.writer)} | 카테고리: {post.category} | 조회: {post.viewCount}
                      </p>
                      <p className="text-gray-300 text-sm line-clamp-2">
                        {post.content.length > 100 ? post.content.substring(0, 100) + '...' : post.content}
                      </p>
                    </div>
                    <button
                      onClick={() => handleUnblindPost(post.id)}
                      className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded text-sm ml-4"
                    >
                      블라인드 해제
                    </button>
                  </div>
                  <p className="text-gray-500 text-xs mt-2">블라인드 시간: {new Date(post.createdAt).toLocaleString()}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* 삭제된 게시글 */}
      {activeTab === 'deleted' && (
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
          {filterPosts(deletedPosts).length === 0 ? (
            <p className="text-center text-gray-400 py-8">삭제된 게시글이 없습니다.</p>
          ) : (
            <div className="grid gap-4">
              {filterPosts(deletedPosts).map(post => (
                <div key={post.id} className="bg-[#2a2e45] p-4 rounded-lg">
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <h4 className="text-white font-semibold mb-1">{post.title}</h4>
                      <p className="text-gray-400 text-sm mb-2">
                        작성자: {getWriterName(post.writer)} | 카테고리: {post.category}
                      </p>
                      <p className="text-gray-300 text-sm line-clamp-2">
                        {post.content.length > 100 ? post.content.substring(0, 100) + '...' : post.content}
                      </p>
                    </div>
                    <button
                      onClick={() => handleRestorePost(post.id)}
                      className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-sm ml-4"
                    >
                      복구
                    </button>
                  </div>
                  <p className="text-gray-500 text-xs mt-2">삭제 시간: {new Date(post.createdAt).toLocaleString()}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* 신고 상세 모달 */}
      {showReportModal && selectedReportPost && (
        <ReportDetailModal
          type="post"
          reports={selectedReportPost.reportDetails}
          onClose={() => { setShowReportModal(false); setSelectedReportPost(null); }}
          onApprove={handleApproveReport}
          onReject={handleRejectReport}
        />
      )}
    </div>
  );
}
