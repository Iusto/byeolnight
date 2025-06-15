// src/pages/PostList.tsx

import React, { useEffect, useState } from 'react';
import { fetchPosts } from '../api/posts';
import { Post } from '../types/Post';
import { useNavigate } from 'react-router-dom';

const PostList: React.FC = () => {
  const [posts, setPosts] = useState<Post[]>([]);
  const [error, setError] = useState<string>('');
  const navigate = useNavigate(); // ✅ 여기서 호출해야 정상 작동

  useEffect(() => {
    fetchPosts()
      .then(setPosts)
      .catch(() => setError('게시글을 불러오지 못했습니다.'));
  }, []);

  if (error) return <p className="text-red-500">{error}</p>;

  return (
    <div className="space-y-4">
      {posts.map(post => (
        <div
          key={post.id}
          className="border p-4 rounded shadow hover:bg-gray-50 transition cursor-pointer"
          onClick={() => navigate(`/posts/${post.id}`)} // ✅ 클릭 시 상세 페이지 이동
        >
          <h2 className="text-xl font-semibold">{post.title}</h2>
          <p className="text-gray-600 text-sm">{post.writer}</p>
          <p className="mt-2 text-gray-800">{post.content.slice(0, 100)}...</p>
          <p className="text-sm text-right text-gray-500">
            추천수: {post.likeCount}
          </p>
        </div>
      ))}
    </div>
  );
};

export default PostList;
