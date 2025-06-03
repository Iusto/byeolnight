import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

interface Post {
  id: number;
  title: string;
}

const PostList = () => {
  const [posts, setPosts] = useState<Post[]>([]);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const fetchPosts = async () => {
      try {
        const response = await fetch('/api/posts');
        if (!response.ok) {
          throw new Error('게시글을 불러오는 데 실패했습니다.');
        }
        const data = await response.json();
        setPosts(data);
      } catch (err: any) {
        setError(err.message);
      }
    };

    fetchPosts();
  }, []);

  const handleClick = (id: number) => {
    navigate(`/posts/${id}`);
  };

  return (
    <div>
      <h1>게시글 목록</h1>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <ul>
        {posts.map(post => (
          <li key={post.id} onClick={() => handleClick(post.id)} style={{ cursor: 'pointer' }}>
            {post.title}
          </li>
        ))}
      </ul>
    </div>
  );
};

export default PostList;