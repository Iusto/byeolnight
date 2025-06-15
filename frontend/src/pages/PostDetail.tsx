import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchPostById, deletePost } from '../api/posts';
import { Post } from '../types/Post';

const PostDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [post, setPost] = useState<Post | null>(null);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    if (!id) {
      setError('잘못된 접근입니다.');
      return;
    }

    fetchPostById(Number(id))
      .then(setPost)
      .catch(() => setError('게시글을 불러오지 못했습니다.'));
  }, [id]);

  const handleDelete = async () => {
    const confirm = window.confirm('정말 삭제하시겠습니까?');
    if (!confirm || !post) return;

    const token = localStorage.getItem('accessToken');
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    try {
      await deletePost(post.id, token);
      alert('삭제 완료');
      navigate('/board');
    } catch {
      alert('삭제에 실패했습니다.');
    }
  };

  const handleEdit = () => {
    if (post) navigate(`/board/edit/${post.id}`);
  };

  if (error) return <p className="text-red-500">{error}</p>;
  if (!post) return <p>로딩 중...</p>;

  return (
    <div className="space-y-4 p-4">
      <h1 className="text-2xl font-bold">{post.title}</h1>
      <p className="text-gray-600 text-sm">작성자: {post.writer}</p>
      <p className="mt-2 text-gray-800 whitespace-pre-wrap">{post.content}</p>
      <p className="text-sm text-right text-gray-500">
        추천수: {post.likeCount} / 내가 추천함: {post.likedByMe ? 'O' : 'X'}
      </p>

      {post.mine && (
        <div className="flex gap-2 justify-end mt-4">
          <button
            onClick={handleEdit}
            className="bg-yellow-500 text-white px-4 py-2 rounded"
          >
            수정
          </button>
          <button
            onClick={handleDelete}
            className="bg-red-500 text-white px-4 py-2 rounded"
          >
            삭제
          </button>
        </div>
      )}
    </div>
  );
};

export default PostDetail;
