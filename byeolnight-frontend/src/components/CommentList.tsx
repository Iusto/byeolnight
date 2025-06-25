import { useState } from 'react';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';

interface Comment {
  id: number;
  content: string;
  writer: string;
  createdAt: string;
}

interface Props {
  comments: Comment[];
  postId: number;
  onRefresh: () => void;
}

export default function CommentList({ comments, postId, onRefresh }: Props) {
  const { user } = useAuth();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editContent, setEditContent] = useState('');

  const handleEdit = (comment: Comment) => {
    setEditingId(comment.id);
    setEditContent(comment.content);
  };

  const handleUpdate = async (id: number) => {
    try {
      await axios.put(`/comments/${id}`, { content: editContent });
      setEditingId(null);
      setEditContent('');
      onRefresh();
    } catch {
      alert('댓글 수정 실패');
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('댓글을 삭제할까요?')) return;
    try {
      await axios.delete(`/comments/${id}`);
      onRefresh();
    } catch {
      alert('댓글 삭제 실패');
    }
  };

  return (
    <ul className="space-y-4">
      {comments.map((c) => (
        <li key={c.id} className="p-4 bg-[#2a2e45] rounded-xl shadow-sm text-white">
          {editingId === c.id ? (
            <div className="space-y-2">
              <textarea
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                className="w-full p-2 rounded bg-[#1f2336] text-white text-sm"
              />
              <div className="flex gap-2">
                <button
                  onClick={() => handleUpdate(c.id)}
                  className="px-3 py-1 text-sm bg-blue-600 hover:bg-blue-700 rounded"
                >
                  저장
                </button>
                <button
                  onClick={() => {
                    setEditingId(null);
                    setEditContent('');
                  }}
                  className="px-3 py-1 text-sm bg-gray-500 hover:bg-gray-600 rounded"
                >
                  취소
                </button>
              </div>
            </div>
          ) : (
            <>
              <p className="text-sm">{c.content}</p>
              <div className="text-xs text-gray-400 mt-1 flex justify-between items-center">
                <span>✍ {c.writer} · {new Date(c.createdAt).toLocaleString()}</span>
                {user?.nickname === c.writer && (
                  <div className="space-x-2 text-right">
                    <button
                      onClick={() => handleEdit(c)}
                      className="text-blue-400 hover:underline text-xs"
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleDelete(c.id)}
                      className="text-red-400 hover:underline text-xs"
                    >
                      삭제
                    </button>
                  </div>
                )}
              </div>
            </>
          )}
        </li>
      ))}
    </ul>
  );
}
