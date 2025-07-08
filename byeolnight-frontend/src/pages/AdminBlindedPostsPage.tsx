import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { Link } from 'react-router-dom';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  likeCount: number;
  blinded: boolean;
  blindType?: string; // ADMIN_BLIND 또는 REPORT_BLIND
}

export default function AdminBlindedPostsPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchBlinded = async () => {
    try {
      console.log('블라인드 게시글 조회 시작');
      const res = await axios.get('/admin/posts/blinded');
      console.log('블라인드 게시글 API 응답:', res.data);
      
      // axios 인터셉터에서 이미 data를 추출했으므로 res.data가 실제 데이터
      const postsData = Array.isArray(res.data) ? res.data : (res.data?.data || res.data || []);
      console.log('블라인드 게시글 데이터:', postsData);
      setPosts(postsData);
    } catch (err) {
      console.error('블라인드 게시글 조회 실패', err);
      console.error('에러 응답:', err.response?.data);
    } finally {
      setLoading(false);
    }
  };

  const handleUnblind = async (id: number) => {
    if (!confirm('블라인드를 해제하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/posts/${id}/unblind`);
      fetchBlinded();
    } catch {
      alert('해제 실패');
    }
  };

  useEffect(() => {
    fetchBlinded();
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-5xl mx-auto">
        <h2 className="text-3xl font-bold mb-8 drop-shadow-glow text-center">🚫 블라인드 게시글 관리</h2>
        
        {/* 테스트 버튼들 */}
        <div className="mb-6 flex gap-4 justify-center">
          <button
            onClick={async () => {
              try {
                const res = await axios.post('/admin/test/create-blinded-post');
                alert(res.data);
                fetchBlinded();
              } catch (err) {
                console.error('테스트 게시글 생성 실패:', err);
                alert('테스트 게시글 생성 실패');
              }
            }}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded"
          >
            테스트 블라인드 게시글 생성
          </button>
          <button
            onClick={async () => {
              try {
                const res = await axios.get('/admin/test/check-blinded-posts');
                alert(res.data);
              } catch (err) {
                console.error('블라인드 게시글 수 확인 실패:', err);
              }
            }}
            className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded"
          >
            블라인드 게시글 수 확인
          </button>
          <button
            onClick={async () => {
              try {
                const res = await axios.post('/admin/test/sync-report-counts');
                alert(res.data);
                fetchBlinded();
              } catch (err) {
                console.error('신고수 동기화 실패:', err);
                alert('신고수 동기화 실패');
              }
            }}
            className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded"
          >
            신고수 동기화
          </button>
        </div>

        {loading ? (
          <p className="text-center text-gray-400">로딩 중...</p>
        ) : posts.length === 0 ? (
          <p className="text-center text-gray-400">블라인드된 게시글이 없습니다.</p>
        ) : (
          <ul className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            {posts.map((post) => (
              <li
                key={post.id}
                className="bg-[#1f2336]/80 backdrop-blur-md p-5 rounded-xl shadow hover:shadow-red-600 transition-shadow"
              >
                <Link to={`/posts/${post.id}`}>
                  <h3 className="text-lg font-semibold mb-2 text-white">
                    {post.title} <span className="text-red-400 text-sm">(블라인드)</span>
                  </h3>
                  <p className="text-sm text-gray-300 mb-2 line-clamp-3">{post.content}</p>
                  <div className="text-sm text-gray-400 mb-3">
                    ✍ {post.writer} · 🗂 {post.category} · ❤️ {post.likeCount}
                    {post.blindType && (
                      <span className="ml-2 px-2 py-1 rounded text-xs font-medium
                        {post.blindType === 'ADMIN_BLIND' 
                          ? 'bg-red-600/20 text-red-400 border border-red-600/30' 
                          : 'bg-orange-600/20 text-orange-400 border border-orange-600/30'}
                      ">
                        {post.blindType === 'ADMIN_BLIND' ? '👮 관리자 블라인드' : '🚨 신고 블라인드'}
                      </span>
                    )}
                  </div>
                </Link>
                <button
                  onClick={() => handleUnblind(post.id)}
                  className="w-full mt-2 bg-green-500 hover:bg-green-600 text-black py-1 rounded text-sm"
                >
                  ✅ 블라인드 해제
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
