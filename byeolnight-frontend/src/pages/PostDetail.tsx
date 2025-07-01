import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import { parseMarkdown } from '../utils/markdown';
import UserProfileModal from '../components/UserProfileModal';
import AdminActionModal from '../components/AdminActionModal';
import PostAdminModal from '../components/PostAdminModal';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  blinded: boolean;
  likeCount: number;
  likedByMe: boolean;
  createdAt: string;
  viewCount: number;
  commentCount: number;
}

interface Comment {
  id: number;
  content: string;
  writer: string;
  blinded?: boolean;
  createdAt: string;
  parentId?: number;
  parentWriter?: string;
}

const categoryLabels: Record<string, string> = {
  NEWS: '뉴스',
  DISCUSSION: '토론',
  IMAGE: '사진',
  EVENT: '행사',
  REVIEW: '후기',
  FREE: '자유',
  NOTICE: '공지',
};

export default function PostDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  // ID 유효성 검사
  if (!id || isNaN(Number(id))) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
        <div className="max-w-4xl mx-auto bg-[#1f2336]/80 backdrop-blur-md p-8 rounded-xl shadow-xl">
          <div className="text-red-400 text-center">
            <h1 className="text-2xl font-bold mb-4">잘못된 접근입니다</h1>
            <p className="mb-4">유효하지 않은 게시글 ID입니다.</p>
            <button 
              onClick={() => navigate('/posts')}
              className="px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded transition"
            >
              게시글 목록으로 돌아가기
            </button>
          </div>
        </div>
      </div>
    );
  }

  const [post, setPost] = useState<Post | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [newComment, setNewComment] = useState('');
  const [error, setError] = useState('');
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [showAdminModal, setShowAdminModal] = useState(false);
  const [showPostAdminModal, setShowPostAdminModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState('');
  const [selectedUserId, setSelectedUserId] = useState<number>();
  const [replyTo, setReplyTo] = useState<{id: number, writer: string} | null>(null); // 답글 기능 활성화

  const fetchPost = async () => {
    try {
      const res = await axios.get(`/public/posts/${id}`);
      // 응답 구조 안전하게 처리
      const postData = res.data?.data || res.data;
      setPost(postData);
    } catch (err) {
      console.error('게시글 조회 실패:', err);
      setError('게시글을 불러올 수 없습니다.');
    }
  };

  // 댓글을 계층 구조로 정렬하는 함수
  const organizeComments = (comments: Comment[]) => {
    const parentComments = comments.filter(c => !c.parentId);
    const childComments = comments.filter(c => c.parentId);
    
    const organized: Comment[] = [];
    
    parentComments.forEach(parent => {
      organized.push(parent);
      // 해당 부모 댓글의 답글들을 찾아서 바로 뒤에 추가
      const replies = childComments.filter(child => child.parentId === parent.id);
      organized.push(...replies);
    });
    
    return organized;
  };

  const fetchComments = async () => {
    try {
      console.log('댓글 조회 요청:', `/public/comments/post/${id}`);
      const res = await axios.get(`/public/comments/post/${id}`);
      
      console.log('전체 응답:', res);
      console.log('응답 데이터:', res.data);
      console.log('응답 데이터 타입:', typeof res.data);
      
      let commentsData = [];
      
      // CommonResponse 구조 처리: { success: true, data: [...] }
      if (res.data && typeof res.data === 'object') {
        if (res.data.success === true && res.data.data) {
          commentsData = res.data.data;
          console.log('CommonResponse success 구조로 파싱:', commentsData);
        } else if (res.data.success === false) {
          console.error('API 오류:', res.data.message);
          commentsData = [];
        } else if (Array.isArray(res.data)) {
          // 직접 배열인 경우
          commentsData = res.data;
          console.log('직접 배열로 파싱:', commentsData);
        } else {
          console.warn('예상치 못한 응답 구조:', res.data);
          commentsData = [];
        }
      }
      
      console.log('최종 댓글 데이터:', commentsData);
      console.log('댓글 데이터 길이:', Array.isArray(commentsData) ? commentsData.length : 'Not Array');
      
      // 댓글을 계층 구조로 정렬
      const organizedComments = organizeComments(Array.isArray(commentsData) ? commentsData : []);
      console.log('정렬된 댓글:', organizedComments);
      
      setComments(organizedComments);
    } catch (err) {
      console.error('댓글 조회 실패:', err);
      console.error('에러 상세:', err.response);
      setComments([]);
    }
  };

  const handleCommentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    try {
      console.log('댓글 등록 요청:', {
        postId: Number(id),
        content: newComment
      });
      
      // 인증 토큰 확인
      const token = localStorage.getItem('accessToken');
      console.log('저장된 토큰:', token ? '있음' : '없음');
      console.log('로그인 상태:', user ? user.nickname : '비로그인');
      
      const requestData = {
        postId: Number(id),
        content: newComment,
        parentId: replyTo?.id || null // 답글인 경우 부모 댓글 ID 포함
      };
      
      const response = await axios.post('/member/comments', requestData);
      
      console.log('댓글 등록 성공:', response.data);
      console.log('댓글 등록 응답 전체:', response);
      
      setNewComment('');
      setReplyTo(null); // 답글 상태 초기화
      setError('');
      
      // 답글 등록 성공 메시지
      if (replyTo) {
        console.log(`${replyTo.writer}님에게 답글이 등록되었습니다.`);
      }
      
      // 트랜잭션 커밋을 위해 더 긴 딩레이 후 댓글 새로고침
      setTimeout(() => {
        console.log('댓글 등록 후 새로고침 시작');
        fetchComments();
      }, 1000);
      
    } catch (err: any) {
      console.error('댓글 등록 실패:', err);
      console.error('에러 응답:', err.response);
      const errorMsg = err?.response?.data?.message || '댓글 등록에 실패했습니다.';
      setError(errorMsg);
      alert(errorMsg);
    }
  };

  const handleLike = async () => {
    if (!user) {
      alert('로그인이 필요합니다.');
      return;
    }

    if (post?.likedByMe) {
      // 이미 추천한 경우 아무 동작 안 함 (버튼이 비활성화되어 있음)
      return;
    }

    try {
      await axios.post(`/member/posts/${id}/like`);
      // 추천 성공 시 로컬 상태만 업데이트 (조회수 증가 방지)
      setPost(prev => prev ? {
        ...prev,
        likeCount: prev.likeCount + 1,
        likedByMe: true
      } : null);
    } catch (err: any) {
      const errorMsg = err?.response?.data?.message || '추천에 실패했습니다.';
      alert(errorMsg);
    }
  };

  const handleReport = async () => {
    if (!user) {
      alert('로그인이 필요합니다.');
      return;
    }
    navigate(`/posts/${id}/report`);
  };

  const handleBlind = async () => {
    if (!confirm('이 게시글을 블라인드 처리하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/posts/${id}/blind`);
      alert('게시글이 블라인드 처리되었습니다.');
      navigate('/posts');
    } catch {
      alert('블라인드 처리에 실패했습니다.');
    }
  };

  const handleUserClick = async (writerName: string, isPost: boolean = false) => {
    setSelectedUser(writerName);
    
    if (!user) {
      // 비로그인 사용자도 프로필 보기 가능
      setShowProfileModal(true);
      return;
    }
    
    if (user.role === 'ADMIN') {
      // 관리자는 사용자 ID를 가져와서 관리자 메뉴 모달 표시
      try {
        const res = await axios.get(`/public/users/profile/${writerName}`);
        setSelectedUserId(res.data?.data?.id || res.data?.id);
        
        if (isPost) {
          setShowPostAdminModal(true); // 게시글 작성자인 경우 게시글 관리 모달
        } else {
          setShowAdminModal(true); // 댓글 작성자인 경우 일반 관리자 모달
        }
      } catch (err) {
        console.error('사용자 정보 조회 실패:', err);
        // 사용자 ID를 찾을 수 없어도 프로필 모달은 표시
        setShowProfileModal(true);
      }
    } else {
      // 일반 사용자는 프로필 모달 표시
      setShowProfileModal(true);
    }
  };

  const handlePostBlind = async () => {
    if (!confirm('이 게시글을 블라인드 처리하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/posts/${id}/blind`);
      alert('게시글이 블라인드 처리되었습니다.');
      fetchPost(); // 게시글 새로고침
    } catch {
      alert('블라인드 처리에 실패했습니다.');
    }
  };

  const handlePostDelete = async () => {
    if (!confirm('이 게시글을 완전히 삭제하시겠습니까? (되돌릴 수 없습니다)')) return;
    try {
      await axios.delete(`/admin/posts/${id}`);
      alert('게시글이 삭제되었습니다.');
      navigate('/posts');
    } catch {
      alert('삭제에 실패했습니다.');
    }
  };

  const handleCommentBlind = async (commentId: number) => {
    if (!confirm('이 댓글을 블라인드 처리하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/comments/${commentId}/blind`);
      alert('댓글이 블라인드 처리되었습니다.');
      fetchComments(); // 댓글 새로고침
    } catch {
      alert('블라인드 처리에 실패했습니다.');
    }
  };

  const handleCommentUnblind = async (commentId: number) => {
    if (!confirm('이 댓글의 블라인드를 해제하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/comments/${commentId}/unblind`);
      alert('댓글 블라인드가 해제되었습니다.');
      fetchComments(); // 댓글 새로고침
    } catch {
      alert('블라인드 해제에 실패했습니다.');
    }
  };

  const handleEdit = () => navigate(`/posts/${id}/edit`);

  const handleDelete = async () => {
    if (!window.confirm('정말 삭제하시겠습니까?')) return;
    try {
      await axios.delete(`/member/posts/${id}`);
      alert('삭제되었습니다.');
      navigate('/posts');
    } catch {
      alert('삭제에 실패했습니다.');
    }
  };

  useEffect(() => {
    fetchPost();
    fetchComments();
    setLoading(false);
  }, [id]);

  if (loading) return <div className="text-white p-8">로딩 중...</div>;
  if (!post) return <div className="text-red-400 p-8">{error}</div>;

  const isOwnerOrAdmin = user && (user.nickname === post.writer || user.role === 'ADMIN');
  const formattedDate = new Date(post.createdAt).toLocaleString();
  const categoryName = categoryLabels[post.category] || post.category;

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-4xl mx-auto bg-[#1f2336]/80 backdrop-blur-md p-8 rounded-xl shadow-xl">
        <h1 className="text-3xl font-bold mb-2 drop-shadow-glow">{post.title}</h1>
        <div className="text-sm text-gray-400 mb-4">
          ✍ <button 
            onClick={() => handleUserClick(post.writer, true)}
            className="bg-purple-600/20 hover:bg-purple-600/40 text-purple-200 hover:text-white px-2 py-1 rounded-md transition-all duration-200 font-medium border border-purple-500/30 hover:border-purple-400"
          >
            {post.writer}
          </button> · 🗂 {categoryName} · ❤️ {post.likeCount} · 👁 {post.viewCount} · 📅 {formattedDate}
          {post.blinded && <span className="text-red-400 ml-2">(블라인드)</span>}
        </div>
        <div 
          className="text-starlight mb-6"
          dangerouslySetInnerHTML={{ __html: parseMarkdown(post.content) }}
        />

        <div className="flex flex-wrap gap-4 mb-8">
          {/* 자기가 작성한 글이 아닌 경우에만 추천/신고 버튼 표시 */}
          {user?.nickname !== post.writer && (
            <>
              <button
                onClick={handleLike}
                disabled={!user || post.likedByMe}
                className={`px-4 py-1 rounded transition ${
                  !user
                    ? 'bg-gray-500 cursor-not-allowed text-gray-300'
                    : post.likedByMe
                    ? 'bg-gray-600 cursor-not-allowed'
                    : 'bg-purple-600 hover:bg-purple-700'
                }`}
              >
                {!user ? '❤️ 로그인 필요' : post.likedByMe ? '✅ 이미 추천함' : '❤️ 추천'}
              </button>

              <button
                onClick={handleReport}
                disabled={!user}
                className={`px-4 py-1 rounded transition ${
                  !user
                    ? 'bg-gray-500 cursor-not-allowed text-gray-300'
                    : 'bg-red-600 hover:bg-red-700'
                }`}
              >
                🚨 신고
              </button>
            </>
          )}

          {/* 작성자 또는 관리자 기능 */}
          {user && user.nickname === post.writer && (
            <>
              <button
                onClick={handleEdit}
                className="px-4 py-1 rounded bg-blue-600 hover:bg-blue-700 transition"
              >
                ✏️ 수정
              </button>
              <button
                onClick={handleDelete}
                className="px-4 py-1 rounded bg-gray-600 hover:bg-gray-700 transition"
              >
                🗑 삭제
              </button>
            </>
          )}
          
          {/* 관리자 전용 기능 */}
          {user && user.role === 'ADMIN' && (
            <button
              onClick={handleBlind}
              className="px-4 py-1 rounded bg-orange-600 hover:bg-orange-700 transition"
            >
              👁️‍🗨️ 블라인드
            </button>
          )}
        </div>

        <hr className="border-gray-600 my-6" />
        <h2 className="text-2xl font-semibold mb-4">💬 댓글 ({post.commentCount || comments.length})</h2>

        {/* 일반 댓글 입력창 (답글 모드가 아닐 때만 표시) */}
        {!replyTo && (
          <form onSubmit={handleCommentSubmit} className="mb-6">
            <textarea
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              rows={3}
              placeholder={user ? "댓글을 입력하세요..." : "댓글을 작성하려면 로그인이 필요합니다."}
              className="w-full p-3 rounded bg-[#2a2e45] text-white focus:outline-none mb-2"
              disabled={!user}
            />
            {error && (
              <div className="text-red-400 text-sm mb-2">
                {error}
              </div>
            )}
            <button
              type="submit"
              className={`px-4 py-2 rounded text-sm transition ${
                !user
                  ? 'bg-gray-500 cursor-not-allowed text-gray-300'
                  : 'bg-blue-500 hover:bg-blue-600'
              }`}
              disabled={!user}
            >
              {user ? '댓글 등록' : '로그인 필요'}
            </button>
          </form>
        )}

        {comments.length === 0 ? (
          <p className="text-gray-400">댓글이 없습니다.</p>
        ) : (
          <ul className="space-y-4">
            {comments.map((c) => (
              <li key={c.id} className={`p-3 rounded-md shadow-sm ${
                c.parentId ? 'bg-[#252842] ml-8 border-l-2 border-purple-500' : 'bg-[#2a2e45]'
              }`}>
                {c.parentId && (
                  <div className="text-xs text-purple-300 mb-1">
                    💬 {c.parentWriter}님에게 답글
                  </div>
                )}
                <div className={`text-sm ${c.blinded ? 'text-gray-500 italic' : 'text-starlight'}`}>
                  {c.content}
                  {c.blinded && <span className="text-red-400 ml-2">(블라인드)</span>}
                </div>
                <div className="text-xs text-gray-400 mt-1 flex justify-between items-center">
                  <div className="flex items-center gap-2">
                    <span>
                      ✍ <button 
                        onClick={() => handleUserClick(c.writer)}
                        className="bg-purple-600/20 hover:bg-purple-600/40 text-purple-200 hover:text-white px-1 py-0.5 rounded text-xs transition-all duration-200 font-medium border border-purple-500/30 hover:border-purple-400"
                      >
                        {c.writer}
                      </button> · {new Date(c.createdAt).toLocaleString()}
                    </span>
                    {user && (
                      <button
                        onClick={() => {
                          if (replyTo?.id === c.id) {
                            setReplyTo(null); // 이미 답글 모드인 경우 취소
                          } else {
                            setReplyTo({id: c.id, writer: c.writer});
                          }
                        }}
                        className={`text-xs px-2 py-1 rounded border transition ${
                          replyTo?.id === c.id 
                            ? 'text-red-400 hover:text-red-300 bg-red-600/20 hover:bg-red-600/40 border-red-500/30'
                            : 'text-blue-400 hover:text-blue-300 bg-blue-600/20 hover:bg-blue-600/40 border-blue-500/30'
                        }`}
                      >
                        {replyTo?.id === c.id ? '취소' : '답글'}
                      </button>
                    )}
                  </div>
                  <div className="flex gap-2">
                    {user && user.role === 'ADMIN' && (
                      c.blinded ? (
                        <button
                          onClick={() => handleCommentUnblind(c.id)}
                          className="text-green-400 hover:text-green-300 text-xs px-2 py-1 bg-green-600/20 hover:bg-green-600/40 rounded border border-green-500/30"
                        >
                          해제
                        </button>
                      ) : (
                        <button
                          onClick={() => handleCommentBlind(c.id)}
                          className="text-orange-400 hover:text-orange-300 text-xs px-2 py-1 bg-orange-600/20 hover:bg-orange-600/40 rounded border border-orange-500/30"
                        >
                          블라인드
                        </button>
                      )
                    )}
                  </div>
                </div>
                
                {/* 해당 댓글에 대한 답글 입력창 */}
                {replyTo?.id === c.id && (
                  <div className="mt-3 p-3 bg-[#1a1d2e] rounded border border-purple-500/30">
                    <div className="mb-2 text-xs text-purple-300">
                      💬 {c.writer}님에게 답글 작성
                    </div>
                    <form onSubmit={handleCommentSubmit}>
                      <textarea
                        value={newComment}
                        onChange={(e) => setNewComment(e.target.value)}
                        rows={2}
                        placeholder={`${c.writer}님에게 답글을 입력하세요...`}
                        className="w-full p-2 rounded bg-[#2a2e45] text-white focus:outline-none mb-2 text-sm"
                        autoFocus
                      />
                      {error && (
                        <div className="text-red-400 text-xs mb-2">
                          {error}
                        </div>
                      )}
                      <div className="flex gap-2">
                        <button
                          type="submit"
                          className="px-3 py-1 rounded text-xs bg-blue-500 hover:bg-blue-600 transition"
                        >
                          답글 등록
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setReplyTo(null);
                            setNewComment('');
                          }}
                          className="px-3 py-1 rounded text-xs bg-gray-500 hover:bg-gray-600 transition"
                        >
                          취소
                        </button>
                      </div>
                    </form>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
      
      {/* 사용자 프로필 모달 */}
      <UserProfileModal 
        username={selectedUser}
        isOpen={showProfileModal}
        onClose={() => setShowProfileModal(false)}
      />
      
      {/* 관리자 액션 모달 */}
      <AdminActionModal 
        username={selectedUser}
        userId={selectedUserId}
        isOpen={showAdminModal}
        onClose={() => setShowAdminModal(false)}
      />
      
      {/* 게시글 관리자 모달 */}
      <PostAdminModal 
        username={selectedUser}
        userId={selectedUserId}
        postId={Number(id)}
        isOpen={showPostAdminModal}
        onClose={() => setShowPostAdminModal(false)}
        onPostBlind={handlePostBlind}
        onPostDelete={handlePostDelete}
      />
    </div>
  );
}
