import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';

interface Comment {
  id: number;
  content: string;
}

interface Post {
  id: number;
  title: string;
  content: string;
}

const PostDetail = () => {
  const { id } = useParams();
  const [post, setPost] = useState<Post | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [newComment, setNewComment] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchPost = async () => {
      try {
        const res = await fetch(`/api/posts/${id}`);
        if (!res.ok) throw new Error('게시글을 불러올 수 없습니다.');
        const data = await res.json();
        setPost(data);
      } catch (err: any) {
        setError(err.message);
      }
    };

    const fetchComments = async () => {
      try {
        const res = await fetch(`/api/posts/${id}/comments`);
        if (!res.ok) throw new Error('댓글을 불러올 수 없습니다.');
        const data = await res.json();
        setComments(data);
      } catch (err: any) {
        setError(err.message);
      }
    };

    fetchPost();
    fetchComments();
  }, [id]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    try {
      const res = await fetch(`/api/posts/${id}/comments`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: newComment })
      });
      if (!res.ok) throw new Error('댓글 등록 실패');

      setNewComment('');
      const updatedComments = await fetch(`/api/posts/${id}/comments`).then(res => res.json());
      setComments(updatedComments);
    } catch (err: any) {
      setError(err.message);
    }
  };

  if (error) return <p style={{ color: 'red' }}>{error}</p>;
  if (!post) return <p>로딩 중...</p>;

  return (
    <div>
      <h1>{post.title}</h1>
      <p>{post.content}</p>

      <h2>댓글</h2>
      <ul>
        {comments.map(c => (
          <li key={c.id}>{c.content}</li>
        ))}
      </ul>

      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="댓글을 입력하세요"
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
        />
        <button type="submit">댓글 등록</button>
      </form>
    </div>
  );
};

export default PostDetail;