// src/pages/PostEdit.tsx

import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchPostById, updatePost } from '../api/posts';
import { Post } from '../types/Post';

const PostEdit: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [post, setPost] = useState<Post | null>(null);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    if (!id) {
      setError('잘못된 게시글 접근입니다.');
      return;
    }

    fetchPostById(Number(id))
      .then(data => setPost(data))
      .catch(() => setError('게시글 불러오기 실패'));
  }, [id, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const token = localStorage.getItem('accessToken');
    if (!token || !post) return;

    try {
      setIsSubmitting(true);
      await updatePost(post.id, post.title, post.content, post.category, token);
      navigate(`/board/${post.id}`);
    } catch {
      setError('게시글 수정에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (error) return <p className="text-red-500">{error}</p>;
  if (!post) return <p>로딩 중...</p>;

  return (
    <form onSubmit={handleSubmit} className="space-y-4 p-4">
      <h2 className="text-xl font-bold">게시글 수정</h2>

      <input
        className="w-full p-2 border rounded"
        type="text"
        value={post.title}
        onChange={(e) => setPost({ ...post, title: e.target.value })}
        required
      />

      <select
        className="w-full p-2 border rounded"
        value={post.category}
        onChange={(e) => setPost({ ...post, category: e.target.value })}
      >
        <option value="NEWS">뉴스</option>
        <option value="DISCUSSION">토론</option>
        <option value="IMAGE">이미지</option>
      </select>

      <textarea
        className="w-full p-2 border rounded h-40"
        value={post.content}
        onChange={(e) => setPost({ ...post, content: e.target.value })}
        required
      />

      <button
        type="submit"
        className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
        disabled={isSubmitting}
      >
        {isSubmitting ? '수정 중...' : '수정 완료'}
      </button>
    </form>
  );
};

export default PostEdit;
