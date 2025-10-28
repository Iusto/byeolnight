import { useState, useEffect } from 'react';
import axios from '../lib/axios';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  writerId: number;
  likeCount: number;
  viewCount: number;
  commentCount: number;
  updatedAt: string;
  createdAt?: string;
  blinded: boolean;
  thumbnailUrl?: string;
  dDay?: string;
}

export const usePosts = (category: string, size: number = 5) => {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        const res = await axios.get('/public/posts', {
          params: { category, sort: 'recent', size }
        });
        setPosts(res.data?.success ? res.data.data?.content || [] : []);
        setError(null);
      } catch (err) {
        console.error('게시글 조회 실패:', err);
        setPosts([]);
        setError('게시글을 불러올 수 없습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchPosts();
  }, [category, size]);

  return { posts, loading, error };
};
