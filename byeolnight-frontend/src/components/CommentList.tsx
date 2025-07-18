import { useState } from 'react';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import UserIconDisplay from './UserIconDisplay';

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
  writerIcon?: string;
  writerCertificates?: string[];
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
  const [newComment, setNewComment] = useState('');
  
  const COMMENT_MAX_LENGTH = 500;

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

  // 댓글 렌더링 함수
  const renderComment = (c: Comment) => (
    <>
      {editingId === c.id ? (
        <div className="space-y-2">
          <div className="relative">
            <textarea
              value={editContent}
              onChange={(e) => {
                if (e.target.value.length <= COMMENT_MAX_LENGTH) {
                  setEditContent(e.target.value);
                }
              }}
              className="w-full p-2 rounded bg-[#1f2336] text-white text-sm"
              maxLength={COMMENT_MAX_LENGTH}
            />
            <div className="text-xs text-gray-400 mt-1 text-right">
              {editContent.length}/{COMMENT_MAX_LENGTH}
            </div>
          </div>
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
          <div className="break-words overflow-wrap-anywhere">
            <p className="text-sm whitespace-pre-wrap">{c.blinded ? '[블라인드 처리된 댓글입니다]' : c.content}</p>
          </div>
          
          <div className="mt-3 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
            <div className="flex items-center gap-4 text-xs text-gray-400">
              <div className="flex items-center gap-2">
                {c.writerIcon && (
                  <div className="w-6 h-6 rounded-full border border-purple-400/50 p-0.5 bg-gradient-to-r from-purple-500/20 to-pink-500/20">
                    <UserIconDisplay iconName={c.writerIcon} size="small" />
                  </div>
                )}
                <span>✍ {c.writer}</span>
              </div>
              <span>{new Date(c.createdAt).toLocaleString()}</span>
              
              {/* 작성자 인증서 표시 */}
              {c.writerCertificates && c.writerCertificates.length > 0 && (
                <div className="flex gap-1 ml-2">
                  {c.writerCertificates.slice(0, 2).map((cert, idx) => {
                    const certIcons = {
                      '별빛 탐험가': '🌠',
                      '우주인 등록증': '🌍',
                      '은하 통신병': '📡',
                      '별 관측 매니아': '🔭',
                      '별빛 채팅사': '🗨️',
                      '별 헤는 밤 시민증': '🏅',
                      '별빛 수호자': '🛡️',
                      '우주 실험자': '⚙️',
                      '건의왕': '💡',
                      '은하 관리자 훈장': '🏆'
                    };
                    const icon = certIcons[cert] || '🏆';
                    
                    return (
                      <span key={idx} className="inline-flex items-center gap-1 px-1.5 py-0.5 bg-gradient-to-r from-yellow-500/20 to-orange-500/20 text-yellow-300 text-xs font-medium rounded-full border border-yellow-500/30" title={cert}>
                        {icon} {cert}
                      </span>
                    );
                  })}
                </div>
              )}
            </div>
            
            <div className="flex items-center gap-2 flex-wrap">
              {/* 좋아요 버튼 - 로그인한 사용자만 */}
              {user && !c.blinded && (
                <button
                  onClick={() => handleLike(c.id)}
                  className={`flex items-center gap-1 px-3 py-1.5 rounded-md text-xs font-medium transition-all duration-200 ${
                    likedComments.has(c.id)
                      ? 'bg-red-500/20 text-red-400 hover:bg-red-500/30 border border-red-500/30'
                      : 'bg-gray-600/30 text-gray-300 hover:bg-gray-600/50 border border-gray-600/30'
                  }`}
                >
                  {likedComments.has(c.id) ? '❤️' : '🤍'} {c.likeCount}
                </button>
              )}
              
              {/* 신고 버튼 - 다른 사용자 댓글만 */}
              {user && user.nickname !== c.writer && !c.blinded && (
                <button
                  onClick={() => setReportingId(c.id)}
                  className="flex items-center gap-1 px-3 py-1.5 bg-orange-600/30 text-orange-300 hover:bg-orange-600/50 rounded-md text-xs font-medium transition-all duration-200 border border-orange-600/30"
                >
                  🚨 신고
                </button>
              )}
              
              {/* 좋아요 수만 표시 - 비로그인 사용자 */}
              {!user && c.likeCount > 0 && (
                <span className="flex items-center gap-1 px-2 py-1 text-xs text-gray-400">
                  🤍 {c.likeCount}
                </span>
              )}
              
              {/* 수정/삭제 버튼 - 작성자만 */}
              {user?.nickname === c.writer && (
                <div className="flex gap-1">
                  <button
                    onClick={() => handleEdit(c)}
                    className="px-3 py-1.5 bg-blue-600/30 text-blue-300 hover:bg-blue-600/50 rounded-md text-xs font-medium transition-all duration-200 border border-blue-600/30"
                  >
                    수정
                  </button>
                  <button
                    onClick={() => handleDelete(c.id)}
                    className="px-3 py-1.5 bg-red-600/30 text-red-300 hover:bg-red-600/50 rounded-md text-xs font-medium transition-all duration-200 border border-red-600/30"
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
    </>
  );

  // TOP3 댓글과 일반 댓글 분리
  const topComments = comments.filter(c => c.isPopular).slice(0, 3);
  const regularComments = comments.filter(c => !c.isPopular);

  return (
    <div className="space-y-6">
      {/* TOP3 인기 댓글 */}
      {topComments.length > 0 && (
        <div className="mb-8">
          <h3 className="text-lg font-bold text-yellow-400 mb-4 flex items-center gap-2">
            🏆 TOP3 인기 댓글
          </h3>
          <ul className="space-y-4">
            {topComments.map((c, index) => (
              <li key={c.id} className="p-4 bg-gradient-to-r from-yellow-900/20 to-orange-900/20 rounded-xl shadow-sm text-white border border-yellow-500/30 relative">
                <div className="absolute top-2 right-2 bg-yellow-500 text-black text-xs font-bold px-2 py-1 rounded-full">
                  #{index + 1}
                </div>
                {renderComment(c)}
              </li>
            ))}
          </ul>
        </div>
      )}
      
      {/* 일반 댓글 */}
      <ul className="space-y-4">
        {regularComments.map((c) => (
          <li key={c.id} className="p-4 bg-[#2a2e45] rounded-xl shadow-sm text-white">
            {renderComment(c)}
          </li>
        ))}
      </ul>
    </div>
  );
}
