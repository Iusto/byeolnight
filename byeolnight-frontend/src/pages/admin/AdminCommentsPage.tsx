import { useEffect, useState, useCallback } from 'react';
import axios from '../../lib/axios';
import {
  AdminPageHeader,
  AdminSearchFilter,
  AdminStatsCard,
  ReportDetailModal,
} from '../../components/admin';

interface Comment {
  id: number;
  content: string;
  writer: string;
  postId?: number;
  postTitle?: string;
  createdAt: string;
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

type TabType = 'reported' | 'blinded' | 'deleted';

export default function AdminCommentsPage() {
  const [activeTab, setActiveTab] = useState<TabType>('reported');
  const [blindedComments, setBlindedComments] = useState<Comment[]>([]);
  const [reportedComments, setReportedComments] = useState<ReportedComment[]>([]);
  const [deletedComments, setDeletedComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [showReportModal, setShowReportModal] = useState(false);
  const [selectedReportComment, setSelectedReportComment] = useState<ReportedComment | null>(null);

  const fetchData = useCallback(async () => {
    try {
      const [blindedRes, reportedRes, deletedRes] = await Promise.all([
        axios.get('/admin/comments/blinded'),
        axios.get('/admin/comments/reported'),
        axios.get('/admin/comments/deleted'),
      ]);
      setBlindedComments(blindedRes.data?.data || blindedRes.data || []);
      setReportedComments(reportedRes.data?.data || reportedRes.data || []);
      setDeletedComments(deletedRes.data?.data || deletedRes.data || []);
    } catch (err) {
      console.error('댓글 데이터 조회 실패:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleUnblindComment = async (commentId: number) => {
    if (!confirm('정말 이 댓글의 블라인드를 해제하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/comments/${commentId}/unblind`);
      alert('댓글 블라인드가 해제되었습니다.');
      fetchData();
    } catch (err) {
      console.error('댓글 블라인드 해제 실패:', err);
      alert('댓글 블라인드 해제에 실패했습니다.');
    }
  };

  const handleBlindComment = async (commentId: number) => {
    if (!confirm('이 댓글을 블라인드 처리하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/comments/${commentId}/blind`);
      alert('댓글이 블라인드 처리되었습니다.');
      fetchData();
    } catch (err) {
      console.error('댓글 블라인드 처리 실패:', err);
      alert('댓글 블라인드 처리에 실패했습니다.');
    }
  };

  const handleRestoreComment = async (commentId: number) => {
    if (!confirm('정말 이 댓글을 복구하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/comments/${commentId}/restore`);
      alert('댓글이 복구되었습니다.');
      fetchData();
    } catch (err) {
      console.error('댓글 복구 실패:', err);
      alert('댓글 복구에 실패했습니다.');
    }
  };

  const handleApproveReport = async (reportId: number) => {
    try {
      await axios.post(`/admin/comment-reports/${reportId}/approve`);
      alert('신고가 승인되었습니다. 신고자들에게 포인트가 지급되었습니다.');
      fetchData();
    } catch (error) {
      console.error('신고 승인 실패:', error);
      alert('신고 승인에 실패했습니다.');
    }
  };

  const handleRejectReport = async (reportId: number, reason: string) => {
    try {
      await axios.post(`/admin/comment-reports/${reportId}/reject`, { reason });
      alert('신고가 거부되었습니다.');
      fetchData();
    } catch (error) {
      console.error('신고 거부 실패:', error);
      alert('신고 거부에 실패했습니다.');
    }
  };

  const filterComments = <T extends { content: string; writer: string }>(comments: T[]) => {
    if (!searchTerm) return comments;
    return comments.filter(comment =>
      comment.content?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      comment.writer?.toLowerCase().includes(searchTerm.toLowerCase())
    );
  };

  const tabs = [
    { key: 'reported' as TabType, label: '신고된 댓글', icon: '🚨', count: reportedComments.length, color: 'orange' },
    { key: 'blinded' as TabType, label: '블라인드 댓글', icon: '💬', count: blindedComments.length, color: 'yellow' },
    { key: 'deleted' as TabType, label: '삭제된 댓글', icon: '💭', count: deletedComments.length, color: 'red' },
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
        title="댓글 관리"
        description="신고/블라인드/삭제된 댓글을 관리합니다."
      />

      {/* 통계 */}
      <div className="grid grid-cols-3 gap-4">
        <AdminStatsCard title="신고된 댓글" value={reportedComments.length} icon="🚨" color="orange" alert={reportedComments.length > 0} />
        <AdminStatsCard title="블라인드 댓글" value={blindedComments.length} icon="💬" color="yellow" />
        <AdminStatsCard title="삭제된 댓글" value={deletedComments.length} icon="💭" color="red" />
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
        searchPlaceholder="댓글 내용, 작성자로 검색..."
      />

      {/* 신고된 댓글 */}
      {activeTab === 'reported' && (
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
          {filterComments(reportedComments).length === 0 ? (
            <p className="text-center text-gray-400 py-8">신고된 댓글이 없습니다.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-[#2a2e45] text-gray-300">
                  <tr>
                    <th className="p-3 text-left">댓글 내용</th>
                    <th className="p-3">작성자</th>
                    <th className="p-3">게시글</th>
                    <th className="p-3">신고수</th>
                    <th className="p-3">상태</th>
                    <th className="p-3">작성일</th>
                    <th className="p-3">관리</th>
                  </tr>
                </thead>
                <tbody>
                  {filterComments(reportedComments).map(comment => (
                    <tr key={comment.id} className="border-t border-gray-700 hover:bg-[#252842]/50">
                      <td className="p-3">
                        <span className="text-gray-300 line-clamp-2 max-w-xs block">
                          {comment.content}
                        </span>
                      </td>
                      <td className="p-3 text-center text-gray-300">{comment.writer}</td>
                      <td className="p-3 text-center">
                        <button
                          onClick={() => window.open(`/posts/${comment.postId}`, '_blank')}
                          className="text-blue-400 hover:text-blue-300 hover:underline text-xs truncate max-w-[100px] block"
                        >
                          {comment.postTitle}
                        </button>
                      </td>
                      <td className="p-3 text-center">
                        <button
                          onClick={() => { setSelectedReportComment(comment); setShowReportModal(true); }}
                          className={`px-2 py-1 rounded text-xs font-medium ${
                            comment.reportCount >= 5 ? 'bg-red-600 text-white' : 'bg-yellow-600 text-white'
                          }`}
                        >
                          🔍 {comment.reportCount}
                        </button>
                      </td>
                      <td className="p-3 text-center">
                        <span className={`text-xs px-2 py-1 rounded ${comment.blinded ? 'bg-red-600' : 'bg-green-600'}`}>
                          {comment.blinded ? '블라인드' : '공개'}
                        </span>
                      </td>
                      <td className="p-3 text-center text-gray-400 text-xs">{new Date(comment.createdAt).toLocaleDateString()}</td>
                      <td className="p-3 text-center">
                        {comment.blinded ? (
                          <button onClick={() => handleUnblindComment(comment.id)} className="px-2 py-1 bg-green-600 hover:bg-green-700 rounded text-xs">해제</button>
                        ) : (
                          <button onClick={() => handleBlindComment(comment.id)} className="px-2 py-1 bg-orange-600 hover:bg-orange-700 rounded text-xs">블라인드</button>
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

      {/* 블라인드 댓글 */}
      {activeTab === 'blinded' && (
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
          {filterComments(blindedComments).length === 0 ? (
            <p className="text-center text-gray-400 py-8">블라인드 처리된 댓글이 없습니다.</p>
          ) : (
            <div className="grid gap-4">
              {filterComments(blindedComments).map(comment => (
                <div key={comment.id} className="bg-[#2a2e45] p-4 rounded-lg">
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <p className="text-gray-300 mb-2">{comment.content}</p>
                      <p className="text-gray-400 text-sm">
                        작성자: {comment.writer}
                        {comment.postTitle && ` | 게시글: ${comment.postTitle}`}
                      </p>
                    </div>
                    <button
                      onClick={() => handleUnblindComment(comment.id)}
                      className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded text-sm ml-4"
                    >
                      블라인드 해제
                    </button>
                  </div>
                  <p className="text-gray-500 text-xs mt-2">블라인드 시간: {new Date(comment.createdAt).toLocaleString()}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* 삭제된 댓글 */}
      {activeTab === 'deleted' && (
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
          {filterComments(deletedComments).length === 0 ? (
            <p className="text-center text-gray-400 py-8">삭제된 댓글이 없습니다.</p>
          ) : (
            <div className="grid gap-4">
              {filterComments(deletedComments).map(comment => (
                <div key={comment.id} className="bg-[#2a2e45] p-4 rounded-lg">
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <p className="text-gray-300 mb-2">{comment.content}</p>
                      <p className="text-gray-400 text-sm">
                        작성자: {comment.writer}
                        {comment.postTitle && ` | 게시글: ${comment.postTitle}`}
                      </p>
                    </div>
                    <button
                      onClick={() => handleRestoreComment(comment.id)}
                      className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-sm ml-4"
                    >
                      복구
                    </button>
                  </div>
                  <p className="text-gray-500 text-xs mt-2">삭제 시간: {new Date(comment.createdAt).toLocaleString()}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* 신고 상세 모달 */}
      {showReportModal && selectedReportComment && (
        <ReportDetailModal
          type="comment"
          reports={selectedReportComment.reportDetails}
          onClose={() => { setShowReportModal(false); setSelectedReportComment(null); }}
          onApprove={handleApproveReport}
          onReject={handleRejectReport}
        />
      )}
    </div>
  );
}