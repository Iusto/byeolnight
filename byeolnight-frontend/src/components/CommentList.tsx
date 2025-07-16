import { useState } from 'react';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';

interface Comment {
  id: number;
  content: string;
  writer: string;
  createdAt: string;
  likeCount: number;
  reportCount: number;
  isPopular: boolean;
  blinded: boolean;
  deleted: boolean;
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
  const [likedComments, setLikedComments] = useState<Set<number>>(new Set());
  const [reportingId, setReportingId] = useState<number | null>(null);
  const [reportReason, setReportReason] = useState('');
  const [reportDescription, setReportDescription] = useState('');

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
      await axios.delete(`/member/comments/${id}`);
      onRefresh();
    } catch {
      alert('댓글 삭제 실패');
    }
  };

  const handleLike = async (id: number) => {
    try {
      const response = await axios.post(`/member/comments/${id}/like`);
      const liked = response.data.data;
      
      if (liked) {
        setLikedComments(prev => new Set([...prev, id]));
      } else {
        setLikedComments(prev => {
          const newSet = new Set(prev);
          newSet.delete(id);
          return newSet;
        });
      }
      onRefresh();
    } catch {
      alert('좋아요 처리 실패');
    }
  };

  const handleReport = async (id: number) => {
    if (!reportReason.trim()) {
      alert('신고 사유를 입력해주세요.');
      return;
    }
    
    try {
      await axios.post(`/member/comments/${id}/report`, null, {
        params: {
          reason: reportReason,
          description: reportDescription
        }
      });
      alert('신고가 접수되었습니다.');
      setReportingId(null);
      setReportReason('');
      setReportDescription('');
      onRefresh();
    } catch (error: any) {
      alert(error.response?.data?.message || '신고 실패');
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
              <p className="text-sm">{c.blinded ? '[블라인드 처리된 댓글입니다]' : c.content}</p>
              
              <div className="mt-3 flex items-center justify-between">
                <div className="flex items-center gap-4 text-xs text-gray-400">
                  <span>✍ {c.writer}</span>
                  <span>{new Date(c.createdAt).toLocaleString()}</span>
                </div>
                
                <div className="flex items-center gap-2">
                  {user && !c.blinded && (
                    <button
                      onClick={() => handleLike(c.id)}
                      className={`flex items-center gap-1 px-2 py-1 rounded text-xs transition-colors ${
                        likedComments.has(c.id)
                          ? 'bg-red-500/20 text-red-400 hover:bg-red-500/30'
                          : 'bg-gray-600/50 text-gray-300 hover:bg-gray-600/70'
                      }`}
                    >
                      {likedComments.has(c.id) ? '❤️' : '🤍'} {c.likeCount}
                    </button>
                  )}
                  
                  {user && user.nickname !== c.writer && !c.blinded && (
                    <button
                      onClick={() => setReportingId(c.id)}
                      className="px-2 py-1 bg-orange-600/50 text-orange-300 hover:bg-orange-600/70 rounded text-xs transition-colors"
                    >
                      🚨 신고
                    </button>
                  )}
                  
                  {user?.nickname === c.writer && (
                    <div className="flex gap-1">
                      <button
                        onClick={() => handleEdit(c)}
                        className="px-2 py-1 bg-blue-600/50 text-blue-300 hover:bg-blue-600/70 rounded text-xs transition-colors"
                      >
                        수정
                      </button>
                      <button
                        onClick={() => handleDelete(c.id)}
                        className="px-2 py-1 bg-red-600/50 text-red-300 hover:bg-red-600/70 rounded text-xs transition-colors"
                      >
                        삭제
                      </button>
                    </div>
                  )}
                </div>
              </div>
              
              {reportingId === c.id && (
                <div className="mt-3 p-3 bg-gray-800/50 rounded-lg border border-orange-500/30">
                  <h4 className="text-sm font-medium text-orange-300 mb-2">댓글 신고</h4>
                  <select
                    value={reportReason}
                    onChange={(e) => setReportReason(e.target.value)}
                    className="w-full p-2 mb-2 bg-gray-700 text-white rounded text-sm"
                  >
                    <option value="">신고 사유 선택</option>
                    <option value="스팸">스팸</option>
                    <option value="욕설">욕설/비방</option>
                    <option value="음란물">음란물</option>
                    <option value="기타">기타</option>
                  </select>
                  <textarea
                    value={reportDescription}
                    onChange={(e) => setReportDescription(e.target.value)}
                    placeholder="상세 내용 (선택사항)"
                    className="w-full p-2 mb-2 bg-gray-700 text-white rounded text-sm h-16 resize-none"
                  />
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleReport(c.id)}
                      className="px-3 py-1 bg-orange-600 hover:bg-orange-700 text-white rounded text-sm"
                    >
                      신고하기
                    </button>
                    <button
                      onClick={() => {
                        setReportingId(null);
                        setReportReason('');
                        setReportDescription('');
                      }}
                      className="px-3 py-1 bg-gray-600 hover:bg-gray-700 text-white rounded text-sm"
                    >
                      취소
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
      </li>
    );
  }
}
}
