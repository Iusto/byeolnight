import { useState, useEffect } from 'react';
import axios from '../../lib/axios';
import { useAuth } from '../../contexts/AuthContext';
import { UserIconDisplay } from '../user';
import { EmojiPicker } from '../chat';
import { ClickableNickname } from '../user';
import { getErrorMessage } from '../../types/api';

interface Comment {
  id: number;
  content: string;
  writer: string;
  writerId?: number;
  createdAt: string;
  likeCount: number;
  reportCount: number;
  isPopular: boolean;
  blinded: boolean;
  deleted: boolean;
  writerIcon?: string;
  writerCertificates?: string[];
  parentId?: number;
  parentWriter?: string;
  children?: Comment[];
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
  const [replyingTo, setReplyingTo] = useState<number | null>(null);
  const [replyContent, setReplyContent] = useState('');
  const [adminComments, setAdminComments] = useState<Comment[]>([]);
  
  const COMMENT_MAX_LENGTH = 500;

  // 관리자용 댓글 데이터 가져오기
  const fetchAdminComments = async () => {
    if (user?.role === 'ADMIN') {
      try {
        const response = await axios.get(`/admin/posts/${postId}/comments`);
        setAdminComments(response.data || []);
      } catch (error) {
        console.error('관리자 댓글 조회 실패:', error);
      }
    }
  };

  // 컴포넌트 마운트 시 관리자 댓글 데이터 가져오기
  useEffect(() => {
    fetchAdminComments();
  }, [user, postId]);

  // 관리자인 경우 원본 댓글 데이터 사용
  const displayComments = user?.role === 'ADMIN' && adminComments.length > 0 ? adminComments : comments;

  // onRefresh 시에도 관리자 댓글 데이터 새로고침
  const handleRefresh = () => {
    onRefresh();
    fetchAdminComments();
  };

  const handleEdit = (comment: Comment) => {
    setEditingId(comment.id);
    setEditContent(comment.content);
  };

  const handleUpdate = async (id: number) => {
    try {
      await axios.put(`/member/comments/${id}`, { content: editContent });
      setEditingId(null);
      setEditContent('');
      handleRefresh();
    } catch {
      alert('댓글 수정 실패');
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('댓글을 삭제할까요?')) return;
    try {
      await axios.delete(`/member/comments/${id}`);
      handleRefresh();
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
      handleRefresh();
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
      handleRefresh();
    } catch (error: unknown) {
      alert(getErrorMessage(error));
    }
  };

  const handleReply = (commentId: number) => {
    setReplyingTo(commentId);
    setReplyContent('');
  };

  const handleSubmitReply = async (parentId: number) => {
    if (!replyContent.trim()) {
      alert('답글 내용을 입력해주세요.');
      return;
    }

    try {
      await axios.post('/member/comments', {
        postId,
        content: replyContent,
        parentId
      });
      setReplyingTo(null);
      setReplyContent('');
      handleRefresh();
    } catch (error: unknown) {
      alert(getErrorMessage(error));
    }
  };

  // 관리자용 댓글 블라인드 처리/해제 함수
  const handleBlindToggle = async (id: number, currentBlindStatus: boolean) => {
    try {
      if (currentBlindStatus) {
        // 블라인드 해제
        await axios.patch(`/admin/comments/${id}/unblind`);
        alert('블라인드가 해제되었습니다.');
      } else {
        // 블라인드 처리
        await axios.patch(`/admin/comments/${id}/blind`);
        alert('블라인드 처리되었습니다.');
      }
      handleRefresh();
    } catch (error: unknown) {
      alert(getErrorMessage(error));
    }
  };



  // 댓글 렌더링 함수
  const renderComment = (c: Comment, isReply: boolean = false) => (
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
              className="w-full p-2 pr-10 rounded bg-[#1f2336] text-white text-sm"
              maxLength={COMMENT_MAX_LENGTH}
            />
            <EmojiPicker
              onEmojiSelect={(emoji) => {
                const newText = editContent + emoji;
                if (newText.length <= COMMENT_MAX_LENGTH) {
                  setEditContent(newText);
                }
              }}
              className="absolute top-1 right-1"
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
            <p className="text-sm whitespace-pre-wrap">
              {/* 관리자는 블라인드/삭제된 댓글도 원본 내용 표시 */}
              {user?.role === 'ADMIN' ? (
                <>
                  {c.content}
                  {(c.blinded || c.deleted) && (
                    <span className={`ml-2 text-xs px-2 py-1 rounded ${
                      c.blinded ? 'bg-red-600/20 text-red-400' : 'bg-gray-600/20 text-gray-400'
                    }`}>
                      ({c.blinded ? '블라인드됨' : '삭제됨'})
                    </span>
                  )}
                </>
              ) : (c.blinded || c.deleted) ? (
                <span className="text-gray-500 italic">
                  {c.blinded ? '[블라인드 처리된 댓글입니다]' : '[삭제된 댓글입니다]'}
                </span>
              ) : (
                c.content
              )}
            </p>
          </div>
          
          {/* 사용자 정보 */}
          <div className="mt-2 flex items-center gap-2 text-xs text-gray-400">
            {c.writerIcon && ((!c.deleted && !c.blinded) || user?.role === 'ADMIN') && (
              <div className="w-10 h-10 rounded-full border border-purple-400/50 p-0.5 bg-gradient-to-r from-purple-500/20 to-pink-500/20">
                <UserIconDisplay iconName={c.writerIcon} size="large" />
              </div>
            )}
            <div className="flex items-center gap-2">
              {((!c.deleted && !c.blinded) || user?.role === 'ADMIN') && (
                <>
                  <span>✍ {c.writer}</span>
                  {c.writerId && (
                    <ClickableNickname userId={c.writerId} nickname={c.writer} className="text-xs text-gray-500 hover:text-purple-400 transition-colors border border-gray-600 hover:border-purple-400 px-1.5 py-0.5 rounded">
                      사용자정보보기
                    </ClickableNickname>
                  )}
                  {/* 관리자에게 댓글 상태 표시 */}
                  {user?.role === 'ADMIN' && (c.blinded || c.deleted) && (
                    <span className={`text-xs px-2 py-1 rounded ${
                      c.blinded ? 'bg-red-600/20 text-red-400 border border-red-600/50' : 'bg-gray-600/20 text-gray-400 border border-gray-600/50'
                    }`}>
                      {c.blinded ? '블라인드' : '삭제됨'}
                    </span>
                  )}
                </>
              )}
            </div>
            <span>•</span>
            <span>{new Date(c.createdAt).toLocaleString()}</span>
            
            {/* 인증서 배지 복원 */}
            {c.writerCertificates && c.writerCertificates.length > 0 && ((!c.deleted && !c.blinded) || user?.role === 'ADMIN') && (
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
                    <span key={idx} className="inline-flex items-center gap-1 px-1.5 py-0.5 bg-gradient-to-r from-yellow-500/20 to-orange-500/20 text-yellow-300 text-xs font-medium rounded-full border border-yellow-500/30 animate-pulse" title={cert}>
                      {icon} {cert}
                    </span>
                  );
                })}
              </div>
            )}
          </div>
          
          {/* 버튼들 */}
          <div className="mt-2 flex items-center gap-1 text-xs">
            {!c.deleted && !c.blinded && (
              <>
                {/* 좋아요 */}
                {user && (
                  <button
                    onClick={() => handleLike(c.id)}
                    className={`px-2 py-1 rounded text-xs transition-colors ${
                      likedComments.has(c.id) ? 'text-red-400 bg-red-500/10' : 'text-gray-400 hover:text-red-400 hover:bg-red-500/10'
                    }`}
                  >
                    {likedComments.has(c.id) ? '❤️' : '🤍'} {c.likeCount}
                  </button>
                )}
                
                {/* 답글 */}
                {user && (
                  <button
                    onClick={() => handleReply(c.id)}
                    className="px-2 py-1 text-gray-400 hover:text-green-400 hover:bg-green-500/10 rounded text-xs transition-colors"
                  >
                    💬 답글
                  </button>
                )}
                
                {/* 수정/삭제 */}
                {user?.nickname === c.writer && (
                  <>
                    <button
                      onClick={() => handleEdit(c)}
                      className="px-2 py-1 text-gray-400 hover:text-blue-400 hover:bg-blue-500/10 rounded text-xs transition-colors"
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleDelete(c.id)}
                      className="px-2 py-1 text-gray-400 hover:text-red-400 hover:bg-red-500/10 rounded text-xs transition-colors"
                    >
                      삭제
                    </button>
                  </>
                )}
                
                {/* 신고 */}
                {user && user.nickname !== c.writer && (
                  <button
                    onClick={() => setReportingId(c.id)}
                    className="px-2 py-1 text-gray-400 hover:text-orange-400 hover:bg-orange-500/10 rounded text-xs transition-colors"
                  >
                    🚨
                  </button>
                )}
                
                {/* 비로그인 좋아요 수 */}
                {!user && c.likeCount > 0 && (
                  <span className="px-2 py-1 text-gray-400 text-xs">
                    🤍 {c.likeCount}
                  </span>
                )}
              </>
            )}
            
            {/* 관리자 버튼 */}
            {user?.role === 'ADMIN' && (
              <>
                <span className="text-gray-600">|</span>
                <button
                  onClick={() => handleBlindToggle(c.id, c.blinded)}
                  className={`px-2 py-1 rounded text-xs transition-colors ${
                    c.blinded ? 'text-green-400 hover:text-green-300 hover:bg-green-500/10' : 'text-purple-400 hover:text-purple-300 hover:bg-purple-500/10'
                  }`}
                >
                  {c.blinded ? '해제' : '블라인드'}
                </button>
                {/* 삭제된 댓글 표시 */}
                {c.deleted && (
                  <span className="px-2 py-1 text-gray-500 text-xs bg-gray-700/30 rounded">
                    삭제됨
                  </span>
                )}
              </>
            )}
            
            {/* 블라인드/삭제된 댓글의 좋아요 수 표시 */}
            {(c.deleted || c.blinded) && c.likeCount > 0 && (
              <span className="px-2 py-1 text-gray-500 text-xs">
                🤍 {c.likeCount}
              </span>
            )}
          </div>
          
          {/* 신고 폼은 블라인드/삭제되지 않은 댓글에만 표시 */}
          {reportingId === c.id && !c.deleted && !c.blinded && (
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
          
          {/* 답글 작성 폼은 블라인드/삭제되지 않은 댓글에만 표시 */}
          {replyingTo === c.id && !c.deleted && !c.blinded && (
            <div className="mt-3 p-3 bg-gray-800/50 rounded-lg border border-green-500/30">
              <h4 className="text-sm font-medium text-green-300 mb-2">답글 작성</h4>
              <div className="relative">
                <textarea
                  value={replyContent}
                  onChange={(e) => {
                    if (e.target.value.length <= COMMENT_MAX_LENGTH) {
                      setReplyContent(e.target.value);
                    }
                  }}
                  placeholder="답글을 입력하세요..."
                  className="w-full p-2 pr-10 bg-gray-700 text-white rounded text-sm h-20 resize-none"
                  maxLength={COMMENT_MAX_LENGTH}
                />
                <EmojiPicker
                  onEmojiSelect={(emoji) => {
                    const newText = replyContent + emoji;
                    if (newText.length <= COMMENT_MAX_LENGTH) {
                      setReplyContent(newText);
                    }
                  }}
                  className="absolute top-1 right-1"
                />
                <div className="text-xs text-gray-400 mt-1 text-right">
                  {replyContent.length}/{COMMENT_MAX_LENGTH}
                </div>
              </div>
              <div className="flex gap-2 mt-2">
                <button
                  onClick={() => handleSubmitReply(c.id)}
                  className="px-3 py-1 bg-green-600 hover:bg-green-700 text-white rounded text-sm"
                >
                  답글 작성
                </button>
                <button
                  onClick={() => {
                    setReplyingTo(null);
                    setReplyContent('');
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

  // 댓글을 평면적 구조로 정리
  const organizeComments = (comments: Comment[]) => {
    const rootComments = comments.filter(c => !c.parentId);
    const allReplies = comments.filter(c => c.parentId);
    
    // 각 루트 댓글에 모든 관련 답글들을 평면적으로 연결
    const organizedComments = rootComments.map(root => {
      // 이 루트 댓글과 관련된 모든 답글들 찾기
      const getRootId = (comment: Comment): number => {
        if (!comment.parentId) return comment.id;
        const parent = comments.find(c => c.id === comment.parentId);
        return parent ? getRootId(parent) : comment.id;
      };
      
      const relatedReplies = allReplies.filter(reply => getRootId(reply) === root.id);
      
      return {
        ...root,
        children: relatedReplies
      };
    });
    
    return organizedComments;
  };
  
  const organizedComments = organizeComments(displayComments);
  
  // TOP3 댓글과 일반 댓글 분리 (루트 댓글만)
  const topComments = organizedComments.filter(c => c.isPopular).slice(0, 3);
  const regularComments = organizedComments.filter(c => !c.isPopular);

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
                
                {/* TOP3 댓글의 답글들 표시 */}
                {c.children && c.children.length > 0 && (
                  <div className="mt-4 ml-8 space-y-3 border-l-2 border-gray-600 pl-4">
                    {c.children.map((reply) => (
                      <div key={reply.id} className="p-3 bg-gray-800/30 rounded-lg">
                        <div className="text-xs text-green-400 mb-2 flex items-center gap-1">
                          ㄴ <span className="font-medium">{reply.parentWriter || c.writer}</span>님에게 답글
                        </div>
                        {renderComment(reply, true)}
                      </div>
                    ))}
                  </div>
                )}
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
            
            {/* 답글들 표시 */}
            {c.children && c.children.length > 0 && (
              <div className="mt-4 ml-8 space-y-3 border-l-2 border-gray-600 pl-4">
                {c.children.map((reply) => (
                  <div key={reply.id} className="p-3 bg-gray-800/30 rounded-lg">
                    <div className="text-xs text-green-400 mb-2 flex items-center gap-1">
                      ㄴ <span className="font-medium">{reply.parentWriter || c.writer}</span>님에게 답글
                    </div>
                    {renderComment(reply, true)}
                  </div>
                ))}
              </div>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
