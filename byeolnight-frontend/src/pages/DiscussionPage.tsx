import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import { DiscussionTopicBanner } from '../components/post';
import { useAuth } from '../contexts/AuthContext';

interface DiscussionPost {
  id: number;
  title: string;
  content: string;
  writer: string;
  writerId: number;
  category: string;
  likeCount: number;
  viewCount: number;
  commentCount: number;
  createdAt: string;
  originTopicId?: number;
  writerIcon?: string;
}

export default function DiscussionPage() {
  const [posts, setPosts] = useState<DiscussionPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const navigate = useNavigate();
  const { user } = useAuth();

  useEffect(() => {
    fetchDiscussionPosts();
  }, [currentPage]);

  const fetchDiscussionPosts = async () => {
    try {
      const response = await axios.get('/public/discussions', {
        params: {
          page: currentPage,
          size: 10,
          sort: 'createdAt,desc'
        }
      });
      
      setPosts(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
    } catch (error) {
      console.error('토론 게시글 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const handlePostClick = (postId: number) => {
    navigate(`/posts/${postId}`);
  };

  const handleWriteClick = () => {
    if (!user) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }
    navigate('/posts/new?category=DISCUSSION');
  };

  return (
    <div className="min-h-screen bg-space-gradient text-white py-12 px-6">
      <div className="max-w-6xl mx-auto">
        {/* 페이지 헤더 */}
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold bg-gradient-to-r from-purple-400 to-blue-400 bg-clip-text text-transparent mb-4">
            🌌 토론 게시판
          </h1>
          <p className="text-gray-300 text-lg">
            우주의 신비로운 주제들을 함께 탐구해보세요
          </p>
        </div>

        {/* 오늘의 토론 주제 배너 */}
        <DiscussionTopicBanner />

        {/* 게시글 작성 버튼 */}
        <div className="flex justify-between items-center mb-8">
          <h2 className="text-2xl font-semibold text-purple-300">💭 토론 게시글</h2>
          <button
            onClick={handleWriteClick}
            className="px-6 py-3 bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white rounded-lg transition-all duration-200 hover:scale-105 shadow-lg font-medium"
          >
            ✍️ 토론글 작성
          </button>
        </div>

        {/* 게시글 목록 */}
        {loading ? (
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="bg-[#1f2336]/80 backdrop-blur-md p-6 rounded-xl animate-pulse">
                <div className="h-6 bg-purple-400/20 rounded mb-3"></div>
                <div className="h-4 bg-purple-400/20 rounded mb-2"></div>
                <div className="h-4 bg-purple-400/20 rounded w-2/3"></div>
              </div>
            ))}
          </div>
        ) : posts.length === 0 ? (
          <div className="text-center py-16">
            <div className="text-6xl mb-4">🌌</div>
            <h3 className="text-xl font-semibold text-gray-300 mb-2">
              아직 토론글이 없습니다
            </h3>
            <p className="text-gray-400 mb-6">
              첫 번째 토론글을 작성해보세요!
            </p>
            <button
              onClick={handleWriteClick}
              className="px-6 py-3 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors"
            >
              토론글 작성하기
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            {posts.map((post) => (
              <div
                key={post.id}
                onClick={() => handlePostClick(post.id)}
                className="bg-[#1f2336]/80 backdrop-blur-md p-6 rounded-xl border border-purple-500/20 hover:border-purple-400/40 transition-all duration-200 cursor-pointer hover:shadow-xl hover:shadow-purple-500/10"
              >
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <span className="text-lg">{post.writerIcon || '🌟'}</span>
                    <span className="text-purple-300 font-medium">{post.writer}</span>
                    {post.originTopicId && (
                      <span className="px-2 py-1 bg-blue-600/30 border border-blue-400/30 rounded-full text-xs text-blue-200">
                        의견글
                      </span>
                    )}
                  </div>
                  <span className="text-xs text-gray-400">
                    {new Date(post.createdAt).toLocaleDateString()}
                  </span>
                </div>

                <h3 className="text-xl font-semibold text-white mb-3 hover:text-purple-200 transition-colors">
                  {post.title}
                </h3>

                <p className="text-gray-300 mb-4 line-clamp-2">
                  {post.content.replace(/<[^>]*>/g, '').substring(0, 150)}
                  {post.content.length > 150 && '...'}
                </p>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-4 text-sm text-gray-400">
                    <span className="flex items-center gap-1">
                      <span>👁️</span>
                      {post.viewCount}
                    </span>
                    <span className="flex items-center gap-1">
                      <span>❤️</span>
                      {post.likeCount}
                    </span>
                    <span className="flex items-center gap-1">
                      <span>💬</span>
                      {post.commentCount}
                    </span>
                  </div>
                  <div className="text-xs text-purple-400">
                    토론 참여하기 →
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* 페이지네이션 */}
        {totalPages > 1 && (
          <div className="flex justify-center mt-12">
            <div className="flex gap-2">
              <button
                onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
                disabled={currentPage === 0}
                className="px-4 py-2 bg-purple-600/50 hover:bg-purple-600 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg transition-colors"
              >
                이전
              </button>
              
              {[...Array(Math.min(5, totalPages))].map((_, i) => {
                const pageNum = Math.max(0, Math.min(totalPages - 5, currentPage - 2)) + i;
                return (
                  <button
                    key={pageNum}
                    onClick={() => setCurrentPage(pageNum)}
                    className={`px-4 py-2 rounded-lg transition-colors ${
                      currentPage === pageNum
                        ? 'bg-purple-600 text-white'
                        : 'bg-purple-600/30 hover:bg-purple-600/50 text-purple-200'
                    }`}
                  >
                    {pageNum + 1}
                  </button>
                );
              })}
              
              <button
                onClick={() => setCurrentPage(Math.min(totalPages - 1, currentPage + 1))}
                disabled={currentPage >= totalPages - 1}
                className="px-4 py-2 bg-purple-600/50 hover:bg-purple-600 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg transition-colors"
              >
                다음
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}