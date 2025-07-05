import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';

interface ReportDetail {
  reportId: number;
  reporterNickname: string;
  reason: string;
  reportedAt: string;
}

interface ReportedPost {
  postId: number;
  title: string;
  content: string;
  writer: string;
  category: string;
  createdAt: string;
  blinded: boolean;
  totalReportCount: number;
  reports: ReportDetail[];
}

interface ReportStats {
  [reason: string]: number;
}

export default function AdminReportsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [posts, setPosts] = useState<ReportedPost[]>([]);
  const [stats, setStats] = useState<ReportStats>({});
  const [loading, setLoading] = useState(true);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchType, setSearchType] = useState('title');
  const [expandedPost, setExpandedPost] = useState<number | null>(null);

  useEffect(() => {
    if (user === null) {
      // 아직 로딩 중이면 대기
      return;
    }
    if (!user || user.role !== 'ADMIN') {
      navigate('/');
      return;
    }
    fetchReportedPosts();
    fetchReportStats();
  }, [user, navigate]);

  const fetchReportedPosts = async () => {
    try {
      const params: any = {};
      if (searchKeyword.trim()) {
        params.search = searchKeyword.trim();
        params.searchType = searchType;
      }
      
      const res = await axios.get('/admin/reports/posts', { params });
      setPosts(res.data?.data?.content || []);
    } catch (err) {
      console.error('신고된 게시글 조회 실패:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchReportStats = async () => {
    try {
      const res = await axios.get('/admin/reports/stats');
      setStats(res.data?.data || {});
    } catch (err) {
      console.error('신고 통계 조회 실패:', err);
    }
  };

  const handleSearch = () => {
    fetchReportedPosts();
  };

  const handleSearchReset = () => {
    setSearchKeyword('');
    fetchReportedPosts();
  };

  const handleApproveReport = async (reportId: number) => {
    if (!confirm('이 신고를 유효한 신고로 승인하시겠습니까?')) return;
    try {
      await axios.post(`/admin/reports/${reportId}/approve`);
      alert('신고가 승인되었습니다. 신고자에게 포인트가 지급됩니다.');
      fetchReportedPosts();
    } catch {
      alert('신고 승인에 실패했습니다.');
    }
  };

  const handleRejectReport = async (reportId: number) => {
    if (!confirm('이 신고를 허위 신고로 거부하시겠습니까?')) return;
    try {
      await axios.post(`/admin/reports/${reportId}/reject`);
      alert('신고가 거부되었습니다. 신고자에게 페널티가 적용됩니다.');
      fetchReportedPosts();
    } catch {
      alert('신고 거부에 실패했습니다.');
    }
  };

  const toggleExpanded = (postId: number) => {
    setExpandedPost(expandedPost === postId ? null : postId);
  };

  if (loading) return <div className="text-white p-8">로딩 중...</div>;

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-7xl mx-auto">
        <div className="flex items-center gap-4 mb-8">
          <button
            onClick={() => navigate('/admin/users')}
            className="text-gray-400 hover:text-white transition"
          >
            ← 관리자 페이지
          </button>
          <h1 className="text-3xl font-bold">🚨 신고 관리</h1>
        </div>

        {/* 신고 통계 */}
        <div className="mb-8 p-6 bg-[#1f2336]/80 backdrop-blur-md rounded-xl shadow-xl">
          <h2 className="text-xl font-semibold mb-4">📊 신고 사유별 통계</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {Object.entries(stats).map(([reason, count]) => (
              <div key={reason} className="bg-[#2a2e45]/60 p-4 rounded-lg text-center">
                <div className="text-2xl font-bold text-red-400">{count}</div>
                <div className="text-sm text-gray-300">{reason}</div>
              </div>
            ))}
          </div>
        </div>

        {/* 검색 기능 */}
        <div className="mb-6 p-4 bg-[#1f2336]/80 rounded-lg border border-gray-600">
          <div className="flex flex-col sm:flex-row gap-3">
            <select
              value={searchType}
              onChange={(e) => setSearchType(e.target.value)}
              className="bg-[#2a2e45] text-white rounded px-3 py-2 text-sm"
            >
              <option value="title">게시글 제목</option>
              <option value="writer">작성자</option>
            </select>
            <input
              type="text"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
              placeholder="검색어를 입력하세요..."
              className="flex-1 bg-[#2a2e45] text-white rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <div className="flex gap-2">
              <button
                onClick={handleSearch}
                className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded text-sm transition"
              >
                🔍 검색
              </button>
              <button
                onClick={handleSearchReset}
                className="px-4 py-2 bg-gray-600 hover:bg-gray-700 text-white rounded text-sm transition"
              >
                초기화
              </button>
            </div>
          </div>
        </div>

        {/* 신고된 게시글 목록 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl shadow-xl overflow-hidden">
          {posts.length === 0 ? (
            <div className="text-center text-gray-400 py-12">
              {searchKeyword ? '검색 결과가 없습니다.' : '신고된 게시글이 없습니다.'}
            </div>
          ) : (
            <div className="divide-y divide-gray-600">
              {posts.map((post) => (
                <div key={post.postId} className="p-6">
                  <div className="flex justify-between items-start mb-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <button
                          onClick={() => navigate(`/posts/${post.postId}`)}
                          className="text-blue-400 hover:text-blue-300 hover:underline text-lg font-semibold"
                        >
                          {post.title}
                        </button>
                        {post.blinded && (
                          <span className="px-2 py-1 bg-red-600 rounded text-xs">블라인드</span>
                        )}
                        <span className="px-2 py-1 bg-gray-600 rounded text-xs">{post.category}</span>
                      </div>
                      <div className="text-gray-300 text-sm mb-2">
                        작성자: {post.writer} | 작성일: {new Date(post.createdAt).toLocaleDateString()}
                      </div>
                      <div className="text-gray-400 text-sm line-clamp-2">{post.content}</div>
                    </div>
                    <div className="flex flex-col items-end gap-2">
                      <span className={`px-3 py-1 rounded text-sm font-bold ${
                        post.totalReportCount >= 5 ? 'bg-red-600' : 
                        post.totalReportCount >= 3 ? 'bg-orange-600' : 'bg-yellow-600'
                      }`}>
                        신고 {post.totalReportCount}건
                      </span>
                      <button
                        onClick={() => toggleExpanded(post.postId)}
                        className="px-3 py-1 bg-purple-600 hover:bg-purple-700 rounded text-sm transition"
                      >
                        {expandedPost === post.postId ? '접기' : '상세보기'}
                      </button>
                    </div>
                  </div>

                  {/* 신고 상세 내역 */}
                  {expandedPost === post.postId && (
                    <div className="mt-4 p-4 bg-[#2a2e45]/60 rounded-lg">
                      <h4 className="font-semibold mb-3">신고 내역</h4>
                      <div className="space-y-3">
                        {post.reports.map((report) => (
                          <div key={report.reportId} className="flex justify-between items-center p-3 bg-[#1f2336]/60 rounded">
                            <div>
                              <div className="font-medium">{report.reporterNickname}</div>
                              <div className="text-sm text-gray-400">{report.reason}</div>
                              <div className="text-xs text-gray-500">
                                {new Date(report.reportedAt).toLocaleString()}
                              </div>
                            </div>
                            <div className="flex gap-2">
                              <button
                                onClick={() => handleApproveReport(report.reportId)}
                                className="px-3 py-1 bg-green-600 hover:bg-green-700 rounded text-sm transition"
                              >
                                승인
                              </button>
                              <button
                                onClick={() => handleRejectReport(report.reportId)}
                                className="px-3 py-1 bg-red-600 hover:bg-red-700 rounded text-sm transition"
                              >
                                거부
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}