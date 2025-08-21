import { useState } from 'react';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import EmojiPicker from './EmojiPicker';

interface Props {
  postId: number;
  onCommentAdded: () => void;
}

export default function CommentForm({ postId, onCommentAdded }: Props) {
  const { user } = useAuth();
  const [newComment, setNewComment] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  const COMMENT_MAX_LENGTH = 500;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim() || isSubmitting) return;

    setIsSubmitting(true);
    try {
      const requestData = {
        postId: postId,
        content: newComment
      };
      
      await axios.post('/member/comments', requestData);
      
      setNewComment('');
      setError('');
      
      // 댓글 등록 후 목록 새로고침
      setTimeout(() => {
        onCommentAdded();
      }, 1000);
      
    } catch (err: any) {
      const errorMsg = err?.response?.data?.message || '댓글 등록에 실패했습니다.';
      setError(errorMsg);
      alert(errorMsg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="mb-6">
      <div className="relative">
        <textarea
          value={newComment}
          onChange={(e) => {
            if (e.target.value.length <= COMMENT_MAX_LENGTH) {
              setNewComment(e.target.value);
            }
          }}
          rows={3}
          placeholder={user ? "댓글을 입력하세요..." : "댓글을 작성하려면 로그인이 필요합니다."}
          className="w-full p-3 rounded bg-[#2a2e45] text-white focus:outline-none mb-2"
          disabled={!user || isSubmitting}
          maxLength={COMMENT_MAX_LENGTH}
        />
        <div className="text-xs text-gray-400 mb-3 text-right">
          {newComment.length}/{COMMENT_MAX_LENGTH}
        </div>
      </div>
      {error && (
        <div className="text-red-400 text-sm mb-3">
          {error}
        </div>
      )}
      <div className="flex items-center gap-2">
        {user && (
          <EmojiPicker
            onEmojiSelect={(emoji) => {
              const newText = newComment + emoji;
              if (newText.length <= COMMENT_MAX_LENGTH) {
                setNewComment(newText);
              }
            }}
            className="flex-shrink-0"
          />
        )}
        <button
          type="submit"
          className={`flex-1 px-4 py-2 rounded text-sm transition ${
            !user || isSubmitting
              ? 'bg-gray-500 cursor-not-allowed text-gray-300'
              : 'bg-blue-500 hover:bg-blue-600'
          }`}
          disabled={!user || isSubmitting}
        >
          {isSubmitting ? '등록 중...' : user ? '댓글 등록' : '로그인 필요'}
        </button>
      </div>
    </form>
  );
}