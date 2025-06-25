import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  blinded: boolean;
  likeCount: number;
  likedByMe: boolean;
}

interface Comment {
  id: number;
  content: string;
  writer: string;
  createdAt: string;
}

export default function PostDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [post, setPost] = useState<Post | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [newComment, setNewComment] = useState('');
  const [error, setError] = useState('');

  const fetchPost = async () => {
    try {
      const res = await axios.get(`/public/posts/${id}`);
      setPost(res.data.data);
    } catch (err) {
      setError('게시글을 불러올 수 없습니다.');
    }
  };

  const fetchComments = async () => {
    try {
      const res = await axios.get(`/comments/post/${id}`);
      setComments(res.data.data);
    } catch (err) {
      console.error('댓글 조회 실패', err);
    }
  };

  const handleCommentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    try {
      await axios.post('/comments', {
        postId: id,
        content: newComment,
      });
      setNewComment('');
      fetchComments();
    } catch (err) {
      setError('댓글 등록 실패');
    }
  };

  const handleLike = async () => {
    try {
      await axios.post(`/member/posts/${id}/like`);
      fetchPost();
    } catch {
      alert('추천에 실패했습니다.');
    }
  };

  const handleReport = async () => {
    const reason = prompt('신고 사유를 입력하세요');
    if (!reason) return;

    try {
      await axios.post(`/posts/${id}/report`, { reason });
      alert('신고가 접수되었습니다.');
    } catch {
      alert('신고에 실패했습니다.');
    }
  };

  useEffect(() => {
    fetchPost();
    fetchComments();
    setLoading(false);
  }, [id]);

    if (loading) return <div className="text-white p-8">로딩 중...</div>;
    if (!post) return <div className="text-red-400 p-8">{error}</div>;

    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
        <div className="max-w-4xl mx-auto bg-[#1f2336]/80 backdrop-blur-md p-8 rounded-xl shadow-xl">
          <h1 className="text-3xl font-bold mb-2 drop-shadow-glow">{post.title}</h1>
          <div className="text-sm text-gray-400 mb-4">
            ✍ {post.writer} · 🗂 {post.category} · ❤️ {post.likeCount}
            {post.blinded && <span className="text-red-400 ml-2">(블라인드)</span>}
          </div>
          <p className="text-starlight whitespace-pre-wrap mb-6">{post.content}</p>

          <div className="flex gap-4 mb-8">
            <button
              onClick={handleLike}
              className="px-4 py-1 rounded bg-purple-600 hover:bg-purple-700 transition"
            >
              ❤️ 추천
            </button>
            <button
              onClick={handleReport}
              className="px-4 py-1 rounded bg-red-600 hover:bg-red-700 transition"
            >
              🚨 신고
            </button>
          </div>

          <hr className="border-gray-600 my-6" />
          <h2 className="text-2xl font-semibold mb-4">💬 댓글</h2>

          <form onSubmit={handleCommentSubmit} className="mb-6">
            <textarea
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              rows={3}
              placeholder="댓글을 입력하세요..."
              className="w-full p-3 rounded bg-[#2a2e45] text-white focus:outline-none mb-2"
            />
            <button
              type="submit"
              className="bg-blue-500 hover:bg-blue-600 px-4 py-2 rounded text-sm"
              disabled={!user}
            >
              {user ? '댓글 등록' : '로그인 필요'}
            </button>
          </form>

          {comments.length === 0 ? (
            <p className="text-gray-400">댓글이 없습니다.</p>
          ) : (
            <ul className="space-y-4">
              {comments.map((c) => (
                <li key={c.id} className="p-3 bg-[#2a2e45] rounded-md shadow-sm">
                  <div className="text-sm text-starlight">{c.content}</div>
                  <div className="text-xs text-gray-400 mt-1">✍ {c.writer} · {new Date(c.createdAt).toLocaleString()}</div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    );
  }

