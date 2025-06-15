// src/pages/PostWrite.tsx

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { createPost } from '../api/posts';

const PostWrite: React.FC = () => {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('NEWS');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
    }
  }, [navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!title.trim() || !content.trim()) {
      setError('제목과 내용을 모두 입력해주세요.');
      return;
    }

    const token = localStorage.getItem('accessToken');
    if (!token) {
      setError('로그인 정보가 유효하지 않습니다.');
      return;
    }

    try {
      setIsSubmitting(true);
      const postId = await createPost(title, content, category, token);
      navigate(`/board/${postId}`);
    } catch (err: any) {
      if (err.response && err.response.data?.message) {
        setError(`에러: ${err.response.data.message}`);
      } else {
        setError('알 수 없는 오류가 발생했습니다.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 p-4">
      <h2 className="text-xl font-bold">게시글 작성</h2>

      {error && <p className="text-red-500">{error}</p>}

      <input
        className="w-full p-2 border rounded"
        type="text"
        placeholder="제목"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        required
      />

      <select
        className="w-full p-2 border rounded"
        value={category}
        onChange={(e) => setCategory(e.target.value)}
      >
        <option value="NEWS">뉴스</option>
        <option value="DISCUSSION">토론</option>
        <option value="IMAGE">이미지</option>
      </select>

      <textarea
        className="w-full p-2 border rounded h-40"
        placeholder="내용"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        required
      />

      <button
        type="submit"
        className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
        disabled={isSubmitting}
      >
        {isSubmitting ? '등록 중...' : '등록'}
      </button>
    </form>
  );
};

export default PostWrite;
