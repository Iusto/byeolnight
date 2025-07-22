import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';

interface ReportDetail {
  reportId: number;
  reporterNickname: string;
  reason: string;
  reportedAt: string;
  reviewed?: boolean;
  accepted?: boolean;
  description?: string;
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

interface ReportedComment {
  commentId: number;
  content: string;
  writer: string;
  postTitle: string;
  postId: number;
  createdAt: string;
  blinded: boolean;
  reportCount: number;
  reportReasons: string[];
  reportDetails: ReportDetail[];
  allProcessed: boolean; // 모든 신고가 처리되었는지 여부
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
  const [activeTab, setActiveTab] = useState('posts');
  const [reportedComments, setReportedComments] = useState<ReportedComment[]>([]);

  useEffect(() => {
    if (user === null) {
      // 아직 로딩 중이면 대기
      return;
    }
    if (!user || user.role !== 'ADMIN') {
      navigate('/');
      return;
    }
    if (activeTab === 'posts') {
      fetchReportedPosts();
      fetchReportStats();
    } else {
      fetchReportedComments();
    }
  }, [user, navigate]);

  useEffect(() => {
    if (user && user.role === 'ADMIN') {
      if (activeTab === 'comments') {
        fetchReportedComments();
      }
    }
  }, [activeTab]);

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
      
      // 로컬 상태 즉시 업데이트
      if (activeTab === 'posts') {
        setPosts(posts.map(post => ({
          ...post,
          reports: post.reports.map(report => 
            report.reportId === reportId 
              ? { ...report, reviewed: true, accepted: true }
              : report
          )
        })));
      } else {
        setReportedComments(reportedComments.map(comment => {
          // 현재 댓글의 신고 상세 내역 업데이트
          const updatedReportDetails = comment.reportDetails.map(report => 
            report.reportId === reportId 
              ? { ...report, reviewed: true, accepted: true }
              : report
          );
          
          // 모든 신고가 처리되었는지 확인
          const allProcessed = updatedReportDetails.every(report => report.reviewed);
          
          return {
            ...comment,
            reportDetails: updatedReportDetails,
            allProcessed: allProcessed
          };
        }));
      }
    } catch {
      alert('신고 승인에 실패했습니다.');
    }
  };

  const handleRejectReport = async (reportId: number) => {
    const reason = prompt('거부 사유를 입력하세요:');
    if (!reason) return;
    
    if (!confirm('이 신고를 허위 신고로 거부하시겠습니까?')) return;
    try {
      await axios.post(`/admin/reports/${reportId}/reject`, { reason });
      alert('신고가 거부되었습니다. 신고자에게 페널티가 적용됩니다.');
      
      // 로컬 상태 즉시 업데이트
      if (activeTab === 'posts') {
        setPosts(posts.map(post => ({
          ...post,
          reports: post.reports.map(report => 
            report.reportId === reportId 
              ? { ...report, reviewed: true, accepted: false }
              : report
          ),
          // 신고 거부 시 신고수 감소
          totalReportCount: post.reports.some(r => r.reportId === reportId) ? 
            post.totalReportCount - 1 : post.totalReportCount
        })));
      } else {
        setReportedComments(reportedComments.map(comment => {
          // 현재 댓글의 신고 상세 내역 업데이트
          const updatedReportDetails = comment.reportDetails.map(report => 
            report.reportId === reportId 
              ? { ...report, reviewed: true, accepted: false }
              : report
          );
          
          // 모든 신고가 처리되었는지 확인
          const allProcessed = updatedReportDetails.every(report => report.reviewed);
          
          // 신고 거부 시 신고수 감소
          const newReportCount = comment.reportDetails.some(r => r.reportId === reportId) ? 
            comment.reportCount - 1 : comment.reportCount;
          
          return {
            ...comment,
            reportDetails: updatedReportDetails,
            reportCount: newReportCount,
            allProcessed: allProcessed
          };
        }));
      }
    } catch {
      alert('신고 거부에 실패했습니다.');
    }
  };

  const toggleExpanded = (postId: number) => {
    setExpandedPost(expandedPost === postId ? null : postId);
  };

  const fetchReportedComments = async () => {
    try {
      const res = await axios.get('/admin/reports/comments');
      setReportedComments(res.data?.data?.content || []);
    } catch (err) {
      console.error('신고된 댓글 조회 실패:', err);
    }
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

        {/* 탭 메뉴 */}
        <div className="mb-6">
          <div className="flex border-b border-gray-600">
            <button
              onClick={() => setActiveTab('posts')}
              className={`px-6 py-3 font-medium transition ${
                activeTab === 'posts'
                  ? 'text-purple-400 border-b-2 border-purple-400'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              📝 신고된 게시글
            </button>
            <button
              onClick={() => setActiveTab('comments')}
              className={`px-6 py-3 font-medium transition ${
                activeTab === 'comments'
                  ? 'text-purple-400 border-b-2 border-purple-400'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              💬 신고된 댓글
            </button>
          </div>
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

        {/* 콘텐츠 영역 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl shadow-xl overflow-hidden">
          {activeTab === 'posts' ? (
            // 신고된 게시글 목록
            posts.length === 0 ? (
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
                          <span className="px-2 py-1 bg-gray-600 rounded text-xs whitespace-nowrap">{post.category}</span>
                        </div>
                        <div className="text-gray-300 text-sm mb-2">
                          작성자: {post.writer} | 작성일: {new Date(post.createdAt).toLocaleDateString()}
                        </div>
                        <div className="text-gray-400 text-sm line-clamp-2">{post.content}</div>
                      </div>
                      <div className="flex flex-col items-end gap-2">
                        <button 
                          onClick={() => toggleExpanded(post.postId)}
                          className={`px-3 py-1 rounded text-sm font-bold cursor-pointer hover:brightness-110 flex items-center gap-1 ${
                            post.totalReportCount >= 5 ? 'bg-red-600' : 
                            post.totalReportCount >= 3 ? 'bg-orange-600' : 'bg-yellow-600'
                          }`}
                          title="신고 내역 보기"
                        >
                          <span>신고 {post.totalReportCount}건</span>
                          <span className="bg-white bg-opacity-20 rounded-full p-1 text-xs">🔍</span>
                        </button>
                        <button
                          onClick={() => toggleExpanded(post.postId)}
                          className="px-3 py-1 bg-purple-600 hover:bg-purple-700 rounded text-sm transition"
                        >
                          {expandedPost === post.postId ? '접기 ▲' : '상세보기 ▼'}
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
                                {report.reviewed ? (
                                  <span className={`px-3 py-1 rounded text-sm ${
                                    report.accepted ? 'bg-green-800' : 'bg-red-800'
                                  }`}>
                                    {report.accepted ? '승인됨' : '거부됨'}
                                  </span>
                                ) : (
                                  <>
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
                                  </>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )
          ) : (
            // 신고된 댓글 목록
            reportedComments.length === 0 ? (
              <div className="text-center text-gray-400 py-12">
                신고된 댓글이 없습니다.
              </div>
            ) : (
              <div className="divide-y divide-gray-600">
                {reportedComments.map((comment: any) => (
                  <div key={comment.commentId} className="p-6">
                    <div className="flex justify-between items-start mb-4">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          <button
                            onClick={() => navigate(`/posts/${comment.postId}`)}
                            className="text-blue-400 hover:text-blue-300 hover:underline text-lg font-semibold"
                          >
                            {comment.postTitle}
                          </button>
                          {comment.blinded && (
                            <span className="px-2 py-1 bg-red-600 rounded text-xs">블라인드</span>
                          )}
                        </div>
                        <div className="text-gray-300 text-sm mb-2">
                          작성자: {comment.writer} | 작성일: {new Date(comment.createdAt).toLocaleDateString()}
                        </div>
                        <div className="text-gray-400 text-sm p-3 bg-[#2a2e45]/60 rounded">
                          {comment.content}
                        </div>
                      </div>
                      <div className="flex flex-col items-end gap-2">
                        <button 
                          onClick={() => toggleExpanded(comment.commentId)}
                          className={`px-3 py-1 rounded text-sm font-bold cursor-pointer hover:brightness-110 flex items-center gap-1 ${
                            comment.reportCount >= 5 ? 'bg-red-600' : 
                            comment.reportCount >= 3 ? 'bg-orange-600' : 'bg-yellow-600'
                          }`}
                          title="신고 내역 보기"
                        >
                          <span>신고 {comment.reportCount}건</span>
                          <span className="bg-white bg-opacity-20 rounded-full p-1 text-xs">🔍</span>
                        </button>
                        <button
                          onClick={() => toggleExpanded(comment.commentId)}
                          className="px-3 py-1 bg-purple-600 hover:bg-purple-700 rounded text-sm transition"
                        >
                          {expandedPost === comment.commentId ? '접기 ▲' : '상세보기 ▼'}
                        </button>
                      </div>
                    </div>

                    {/* 신고 상세 내역 */}
                    {expandedPost === comment.commentId && (
                      <div className="mt-4 p-4 bg-[#2a2e45]/60 rounded-lg">
                        <div className="flex justify-between items-center mb-3">
                          <h4 className="font-semibold">신고 내역</h4>
                          {comment.allProcessed && (
                            <span className="px-3 py-1 bg-gray-600 rounded text-xs">
                              모든 신고 처리 완료
                            </span>
                          )}
                        </div>
                        <div className="space-y-3">
                          {comment.reportDetails.map((report: any) => (
                            <div key={report.reportId} className="flex justify-between items-center p-3 bg-[#1f2336]/60 rounded">
                              <div>
                                <div className="font-medium">
                                  <span className="text-purple-400">신고자:</span> {report.reporterNickname}
                                </div>
                                <div className="text-sm text-gray-400">
                                  <span className="text-yellow-400">사유:</span> {report.reason}
                                </div>
                                {report.description && (
                                  <div className="text-xs text-gray-500 mt-1">
                                    <span className="text-blue-400">상세:</span> {report.description}
                                  </div>
                                )}
                                <div className="text-xs text-gray-500">
                                  {new Date(report.reportedAt).toLocaleString()}
                                </div>
                              </div>
                              <div className="flex gap-2">
                                {report.reviewed ? (
                                  <span className={`px-3 py-1 rounded text-sm ${
                                    report.accepted ? 'bg-green-800' : 'bg-red-800'
                                  }`}>
                                    {report.accepted ? '승인됨' : '거부됨'}
                                  </span>
                                ) : (
                                  <>
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
                                  </>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )
          )}
        </div>
      </div>
    </div>
  );
}