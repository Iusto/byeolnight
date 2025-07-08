import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';

interface ReportDetail {
  reportId: number;
  reporterNickname: string;
  reason: string;
  description?: string;
  reviewed: boolean;
  accepted: boolean;
  reportedAt: string;
}

interface ReportedPost {
  id: number;
  title: string;
  writer: string;
  category: string;
  reportCount: number;
  blinded: boolean;
  createdAt: string;
  reportDetails?: ReportDetail[];
}

export default function AdminReportedPostsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [posts, setPosts] = useState<ReportedPost[]>([]);
  const [filteredPosts, setFilteredPosts] = useState<ReportedPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchType, setSearchType] = useState('title');
  const [expandedPost, setExpandedPost] = useState<number | null>(null);

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') {
      navigate('/');
      return;
    }
    fetchReportedPosts();
  }, [user, navigate]);

  const fetchReportedPosts = async () => {
    try {
      const res = await axios.get('/admin/posts/reported');
      const postsData = res.data?.data || [];
      setPosts(postsData);
      setFilteredPosts(postsData);
    } catch (err) {
      console.error('신고된 게시글 조회 실패:', err);
    } finally {
      setLoading(false);
    }
  };
  
  const handleSearch = () => {
    if (!searchKeyword.trim()) {
      setFilteredPosts(posts);
      return;
    }
    
    const filtered = posts.filter(post => {
      switch (searchType) {
        case 'title':
          return post.title.toLowerCase().includes(searchKeyword.toLowerCase());
        case 'writer':
          return post.writer.toLowerCase().includes(searchKeyword.toLowerCase());
        case 'category':
          return post.category.toLowerCase().includes(searchKeyword.toLowerCase());
        default:
          return true;
      }
    });
    
    setFilteredPosts(filtered);
  };
  
  const handleSearchReset = () => {
    setSearchKeyword('');
    setFilteredPosts(posts);
  };

  const handleBlind = async (postId: number) => {
    if (!confirm('이 게시글을 블라인드 처리하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/posts/${postId}/blind`);
      alert('블라인드 처리되었습니다.');
      fetchReportedPosts();
    } catch {
      alert('블라인드 처리에 실패했습니다.');
    }
  };

  const handleUnblind = async (postId: number) => {
    if (!confirm('이 게시글의 블라인드를 해제하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/posts/${postId}/unblind`);
      alert('블라인드가 해제되었습니다.');
      fetchReportedPosts();
    } catch {
      alert('블라인드 해제에 실패했습니다.');
    }
  };

  const handleApproveReport = async (reportId: number) => {
    if (!confirm('이 신고를 승인하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/reports/${reportId}/approve`);
      alert('신고가 승인되었습니다.');
      
      // 로컬 상태 즉시 업데이트
      const updatePosts = (postList: ReportedPost[]) => 
        postList.map(post => ({
          ...post,
          reportDetails: post.reportDetails?.map(report => 
            report.reportId === reportId 
              ? { ...report, reviewed: true, accepted: true }
              : report
          )
        }));
      
      setPosts(updatePosts);
      setFilteredPosts(updatePosts);
    } catch {
      alert('신고 승인에 실패했습니다.');
    }
  };

  const handleRejectReport = async (reportId: number) => {
    const reason = prompt('거부 사유를 입력하세요:');
    if (!reason) return;
    try {
      await axios.patch(`/admin/reports/${reportId}/reject`, { reason });
      alert('신고가 거부되었습니다.');
      
      // 로컬 상태 즉시 업데이트
      const updatePosts = (postList: ReportedPost[]) => 
        postList.map(post => ({
          ...post,
          reportDetails: post.reportDetails?.map(report => 
            report.reportId === reportId 
              ? { ...report, reviewed: true, accepted: false }
              : report
          )
        }));
      
      setPosts(updatePosts);
      setFilteredPosts(updatePosts);
    } catch {
      alert('신고 거부에 실패했습니다.');
    }
  };

  if (loading) return <div className="text-white p-8">로딩 중...</div>;

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center gap-4 mb-8">
          <button
            onClick={() => navigate('/admin/users')}
            className="text-gray-400 hover:text-white transition"
          >
            ← 관리자 페이지
          </button>
          <h1 className="text-3xl font-bold">🚨 신고된 게시글 관리</h1>
        </div>
        
        {/* 검색 기능 */}
        <div className="mb-6 p-4 bg-[#1f2336]/80 rounded-lg border border-gray-600">
          <div className="flex flex-col sm:flex-row gap-3">
            <select
              value={searchType}
              onChange={(e) => setSearchType(e.target.value)}
              className="bg-[#2a2e45] text-white rounded px-3 py-2 text-sm"
            >
              <option value="title">제목</option>
              <option value="writer">작성자</option>
              <option value="category">카테고리</option>
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
          {searchKeyword && (
            <div className="mt-2 text-sm text-gray-300">
              검색 결과: "{searchKeyword}" ({filteredPosts.length}건)
            </div>
          )}
        </div>

        {filteredPosts.length === 0 ? (
          <div className="text-center text-gray-400 py-12">
            {searchKeyword ? '검색 결과가 없습니다.' : '신고된 게시글이 없습니다.'}
          </div>
        ) : (
          <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl shadow-xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-[#2a2e45]">
                  <tr>
                    <th className="px-6 py-4 text-left">제목</th>
                    <th className="px-6 py-4 text-left">작성자</th>
                    <th className="px-6 py-4 text-left">카테고리</th>
                    <th className="px-6 py-4 text-center">신고수</th>
                    <th className="px-6 py-4 text-center">상태</th>
                    <th className="px-6 py-4 text-center">작성일</th>
                    <th className="px-6 py-4 text-center">관리</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredPosts.map((post) => (
                    <React.Fragment key={post.id}>
                      <tr className="border-b border-gray-600 hover:bg-[#2a2e45]/50">
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2">
                            <button
                              onClick={() => setExpandedPost(expandedPost === post.id ? null : post.id)}
                              className="text-gray-400 hover:text-white"
                            >
                              {expandedPost === post.id ? '▼' : '▶'}
                            </button>
                            <button
                              onClick={() => navigate(`/posts/${post.id}`)}
                              className="text-blue-400 hover:text-blue-300 hover:underline text-left max-w-xs truncate block"
                            >
                              {post.title}
                            </button>
                          </div>
                        </td>
                        <td className="px-6 py-4">{post.writer}</td>
                        <td className="px-6 py-4">{post.category}</td>
                        <td className="px-6 py-4 text-center">
                          <div className="flex flex-col items-center gap-1">
                            <span className={`px-2 py-1 rounded text-sm ${
                              post.reportCount >= 3 ? 'bg-red-600' : 'bg-yellow-600'
                            }`}>
                              {post.reportCount}건
                            </span>
                            {post.reportDetails && (
                              <div className="text-xs space-y-1">
                                <div className="text-gray-400">
                                  미처리: {post.reportDetails.filter(r => !r.reviewed).length}건
                                </div>
                                <div className="text-green-400">
                                  승인: {post.reportDetails.filter(r => r.reviewed && r.accepted).length}건
                                </div>
                                <div className="text-red-400">
                                  거부: {post.reportDetails.filter(r => r.reviewed && !r.accepted).length}건
                                </div>
                              </div>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 text-center">
                          {post.blinded ? (
                            <span className="px-2 py-1 bg-red-600 rounded text-sm">블라인드</span>
                          ) : (
                            <span className="px-2 py-1 bg-green-600 rounded text-sm">공개</span>
                          )}
                        </td>
                        <td className="px-6 py-4 text-center text-sm text-gray-400">
                          {new Date(post.createdAt).toLocaleDateString()}
                        </td>
                        <td className="px-6 py-4 text-center">
                          <div className="flex gap-2 justify-center">
                            {post.blinded ? (
                              <button
                                onClick={() => handleUnblind(post.id)}
                                className="px-3 py-1 bg-green-600 hover:bg-green-700 rounded text-sm"
                              >
                                해제
                              </button>
                            ) : (
                              <button
                                onClick={() => handleBlind(post.id)}
                                className="px-3 py-1 bg-orange-600 hover:bg-orange-700 rounded text-sm"
                              >
                                블라인드
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                      {expandedPost === post.id && post.reportDetails && (
                        <tr className="bg-[#2a2e45]/30">
                          <td colSpan={7} className="px-6 py-4">
                            <div className="space-y-3">
                              <div className="flex justify-between items-center mb-3">
                                <h4 className="font-semibold text-yellow-400">📋 신고 상세 내역</h4>
                                <div className="text-sm text-gray-400">
                                  총 {post.reportDetails.length}건 | 
                                  미처리 {post.reportDetails.filter(r => !r.reviewed).length}건 | 
                                  승인 {post.reportDetails.filter(r => r.reviewed && r.accepted).length}건 | 
                                  거부 {post.reportDetails.filter(r => r.reviewed && !r.accepted).length}건
                                </div>
                              </div>
                              {post.reportDetails.map((report) => (
                                <div key={report.reportId} className={`bg-[#1f2336] p-3 rounded border-l-4 ${
                                  !report.reviewed ? 'border-yellow-500' : 
                                  report.accepted ? 'border-green-500' : 'border-red-500'
                                }`}>
                                  <div className="flex justify-between items-start mb-2">
                                    <div>
                                      <span className="font-medium">{report.reporterNickname}</span>
                                      <span className="text-gray-400 text-sm ml-2">
                                        {new Date(report.reportedAt).toLocaleString()}
                                      </span>
                                    </div>
                                    <div className="flex items-center gap-2">
                                      {report.reviewed ? (
                                        <span className={`px-2 py-1 rounded text-xs ${
                                          report.accepted ? 'bg-green-600' : 'bg-red-600'
                                        }`}>
                                          {report.accepted ? '승인' : '거부'}
                                        </span>
                                      ) : (
                                        <>
                                          <span className="px-2 py-1 bg-yellow-600 rounded text-xs mr-2">
                                            미처리
                                          </span>
                                          <button
                                            onClick={() => handleApproveReport(report.reportId)}
                                            className="px-2 py-1 bg-green-600 hover:bg-green-700 rounded text-xs"
                                          >
                                            승인
                                          </button>
                                          <button
                                            onClick={() => handleRejectReport(report.reportId)}
                                            className="px-2 py-1 bg-red-600 hover:bg-red-700 rounded text-xs"
                                          >
                                            거부
                                          </button>
                                        </>
                                      )}
                                    </div>
                                  </div>
                                  <div className="text-sm">
                                    <div className="mb-1">
                                      <span className="text-gray-400">사유:</span> {report.reason}
                                    </div>
                                    {report.description && (
                                      <div>
                                        <span className="text-gray-400">상세:</span> {report.description}
                                      </div>
                                    )}
                                  </div>
                                </div>
                              ))}
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}