import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import ClickableNickname from '../components/ClickableNickname';
import UserIconDisplay from '../components/UserIconDisplay';
import CommentList from '../components/CommentList';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  writerId: number;
  blinded: boolean;
  likeCount: number;
  likedByMe: boolean;
  createdAt: string;
  viewCount: number;
  commentCount: number;
  writerIcon?: string;
  writerCertificates?: string[];
  images?: Array<{
    id: number;
    originalName: string;
    url: string;
  }>;
}

interface Comment {
  id: number;
  content: string;
  writer: string;
  writerId: number;
  blinded?: boolean;
  deleted?: boolean;
  createdAt: string;
  parentId?: number;
  parentWriter?: string;
  writerIcon?: string;
  writerCertificates?: string[];
  likeCount?: number;
  likedByMe?: boolean;
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
  
  // iframe 렌더링을 위한 전역 CSS 스타일 추가
  useEffect(() => {
    const style = document.createElement('style');
    style.textContent = `
      .youtube-content iframe {
        width: 100% !important;
        min-height: 400px !important;
        border: none !important;
        border-radius: 12px !important;
        display: block !important;
        visibility: visible !important;
        background: #000 !important;
      }
      .youtube-content .video-container {
        position: relative !important;
        width: 100% !important;
        padding-bottom: 56.25% !important;
        height: 0 !important;
        margin: 20px 0 !important;
        border-radius: 12px !important;
        overflow: hidden !important;
      }
      .youtube-content .video-container iframe {
        position: absolute !important;
        top: 0 !important;
        left: 0 !important;
        width: 100% !important;
        height: 100% !important;
        min-height: unset !important;
      }
      /* 글씨 크기 및 스타일 적용 */
      .post-content h1 { font-size: 2rem !important; font-weight: bold !important; margin: 1.5rem 0 1rem 0 !important; color: #e2e8f0 !important; }
      .post-content h2 { font-size: 1.75rem !important; font-weight: bold !important; margin: 1.25rem 0 0.75rem 0 !important; color: #e2e8f0 !important; }
      .post-content h3 { font-size: 1.5rem !important; font-weight: bold !important; margin: 1rem 0 0.5rem 0 !important; color: #e2e8f0 !important; }
      .post-content h4 { font-size: 1.25rem !important; font-weight: bold !important; margin: 0.75rem 0 0.5rem 0 !important; color: #e2e8f0 !important; }
      .post-content h5 { font-size: 1.125rem !important; font-weight: bold !important; margin: 0.75rem 0 0.5rem 0 !important; color: #e2e8f0 !important; }
      .post-content h6 { font-size: 1rem !important; font-weight: bold !important; margin: 0.5rem 0 0.25rem 0 !important; color: #e2e8f0 !important; }
      .post-content p { font-size: 1rem !important; line-height: 1.7 !important; margin: 0.75rem 0 !important; color: #cbd5e1 !important; }
      .post-content strong { font-weight: bold !important; color: #f1f5f9 !important; }
      .post-content em { font-style: italic !important; color: #a78bfa !important; }
      .post-content u { text-decoration: underline !important; font-weight: normal !important; color: #cbd5e1 !important; }
      .post-content s, .post-content del { text-decoration: line-through !important; color: #94a3b8 !important; }
      .post-content ul, .post-content ol { padding-left: 1.5rem !important; margin: 1rem 0 !important; }
      .post-content li { margin: 0.5rem 0 !important; color: #cbd5e1 !important; line-height: 1.6 !important; }
      .post-content blockquote { 
        border-left: 4px solid #8b5cf6 !important; 
        padding: 1rem 1.5rem !important; 
        margin: 1.5rem 0 !important; 
        font-style: italic !important;
        background: rgba(139, 92, 246, 0.1) !important;
        border-radius: 0 8px 8px 0 !important;
        color: #c4b5fd !important;
      }
      .post-content code {
        background: rgba(139, 92, 246, 0.2) !important;
        color: #e879f9 !important;
        padding: 0.2rem 0.4rem !important;
        border-radius: 4px !important;
        font-family: 'Courier New', 'Consolas', monospace !important;
        font-size: 0.9em !important;
      }
      .post-content pre {
        background: rgba(0, 0, 0, 0.4) !important;
        padding: 1.5rem !important;
        border-radius: 8px !important;
        overflow-x: auto !important;
        border: 1px solid rgba(139, 92, 246, 0.3) !important;
        margin: 1.5rem 0 !important;
      }
      .post-content pre code {
        background: transparent !important;
        padding: 0 !important;
        color: #f8fafc !important;
      }
      .post-content a {
        color: #a78bfa !important;
        text-decoration: underline !important;
        transition: color 0.2s ease !important;
      }
      .post-content a:hover {
        color: #c4b5fd !important;
      }
      .post-content hr {
        border: none !important;
        height: 2px !important;
        background: linear-gradient(to right, transparent, #8b5cf6, transparent) !important;
        margin: 2rem 0 !important;
      }
    `;
    document.head.appendChild(style);
    
    return () => {
      if (document.head.contains(style)) {
        document.head.removeChild(style);
      }
    };
  }, []);

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

  const [replyTo, setReplyTo] = useState<{id: number, writer: string} | null>(null);
  const [editingComment, setEditingComment] = useState<{id: number, content: string} | null>(null);
  const [editContent, setEditContent] = useState('');
  const [iframeSupported, setIframeSupported] = useState<boolean | null>(null);
  const [likedComments, setLikedComments] = useState<Set<number>>(new Set());

  const fetchPost = async () => {
    try {
      const res = await axios.get(`/public/posts/${id}`);
      // 응답 구조 안전하게 처리
      const postData = res.data?.data || res.data;
      console.log('게시글 데이터:', postData);
      
      // 작성자 정보 보완 (선택적)
      if (postData && postData.writerId) {
        try {
          const writerRes = await axios.get(`/public/users/${postData.writerId}/profile`);
          if (writerRes.data?.success) {
            const writerData = writerRes.data.data;
            postData.writerIcon = writerData.equippedIcon;
            postData.writerCertificates = writerData.representativeCertificates || [];
          }
        } catch (writerErr) {
          // 작성자 정보 조회 실패 시 기본값 유지
          console.warn('작성자 정보 조회 실패, 기본 아이콘 사용');
        }
      }
      
      setPost(postData);
    } catch (err) {
      console.error('게시글 조회 실패:', err);
      setError('게시글을 불러올 수 없습니다.');
    }
  };

  // 댓글을 계층 구조로 정렬하는 함수 (재귀적으로 모든 답글 처리)
  const organizeComments = (comments: Comment[]) => {
    const organized: Comment[] = [];
    
    // 재귀적으로 답글을 찾는 함수
    const addReplies = (parentId: number, depth = 0) => {
      const replies = comments.filter(c => c.parentId === parentId);
      replies.forEach(reply => {
        organized.push(reply);
        // 이 답글의 답글들도 재귀적으로 추가
        addReplies(reply.id, depth + 1);
      });
    };
    
    // 최상위 댓글들부터 시작
    const parentComments = comments.filter(c => !c.parentId);
    parentComments.forEach(parent => {
      organized.push(parent);
      // 해당 부모 댓글의 모든 답글들을 재귀적으로 추가
      addReplies(parent.id);
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
      
      // 댓글 작성자 정보 보완 (선택적)
      const enhancedComments = (Array.isArray(commentsData) ? commentsData : []).map(comment => {
        // 기본 아이콘과 빈 인증서 리스트로 초기화
        if (!comment.writerIcon) comment.writerIcon = null;
        if (!comment.writerCertificates) comment.writerCertificates = [];
        return comment;
      });
      
      // 댓글을 계층 구조로 정렬
      const organizedComments = organizeComments(enhancedComments);
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
      setTimeout(async () => {
        console.log('댓글 등록 후 새로고침 시작');
        fetchComments();
        
        // 알림 생성 확인
        try {
          const notificationResponse = await axios.get('/member/notifications/unread/count');
          console.log('댓글 작성 후 알림 개수:', notificationResponse.data);
        } catch (err) {
          console.error('알림 확인 실패:', err);
        }
      }, 2000);
      
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



  const handlePostBlind = async () => {
    if (!confirm('이 게시글을 블라인드 처리하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/posts/${id}/blind`);
      alert('게시글이 블라인드 처리되었습니다.');
      navigate(`/posts?category=${post?.category || 'FREE'}`);
    } catch (error: any) {
      console.error('게시글 블라인드 실패:', error);
      const errorMessage = error.response?.data?.message || '블라인드 처리에 실패했습니다.';
      alert(errorMessage);
    }
  };

  const handleCommentEdit = async (commentId: number) => {
    if (!editContent.trim()) return;
    try {
      await axios.put(`/member/comments/${commentId}`, { content: editContent });
      setEditingComment(null);
      setEditContent('');
      fetchComments();
    } catch {
      alert('댓글 수정에 실패했습니다.');
    }
  };

  const handleCommentDelete = async (commentId: number) => {
    if (!confirm('댓글을 삭제하시겠습니까?')) return;
    try {
      await axios.delete(`/member/comments/${commentId}`);
      fetchComments();
    } catch {
      alert('댓글 삭제에 실패했습니다.');
    }
  };

  const handleCommentBlind = async (commentId: number) => {
    if (!confirm('이 댓글을 블라인드 처리하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/comments/${commentId}/blind`);
      alert('댓글이 블라인드 처리되었습니다.');
      fetchComments();
    } catch (error: any) {
      console.error('댓글 블라인드 실패:', error);
      const errorMessage = error.response?.data?.message || '블라인드 처리에 실패했습니다.';
      alert(errorMessage);
    }
  };

  const handleCommentUnblind = async (commentId: number) => {
    if (!confirm('이 댓글의 블라인드를 해제하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/comments/${commentId}/unblind`);
      alert('댓글 블라인드가 해제되었습니다.');
      fetchComments();
    } catch (error: any) {
      console.error('댓글 블라인드 해제 실패:', error);
      const errorMessage = error.response?.data?.message || '블라인드 해제에 실패했습니다.';
      alert(errorMessage);
    }
  };

  const handleCommentLike = async (commentId: number) => {
    try {
      const response = await axios.post(`/member/comments/${commentId}/like`);
      const liked = response.data.data;
      
      if (liked) {
        setLikedComments(prev => new Set([...prev, commentId]));
      } else {
        setLikedComments(prev => {
          const newSet = new Set(prev);
          newSet.delete(commentId);
          return newSet;
        });
      }
      fetchComments();
    } catch {
      alert('좋아요 처리 실패');
    }
  };

  const handleCommentReport = async (commentId: number) => {
    const reason = prompt('신고 사유를 입력해주세요.');
    if (!reason?.trim()) return;
    
    try {
      await axios.post(`/member/comments/${commentId}/report`, null, {
        params: {
          reason: reason,
          description: ''
        }
      });
      alert('신고가 접수되었습니다.');
    } catch (error: any) {
      alert(error.response?.data?.message || '신고 실패');
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
    
    // iframe 지원 여부 체크 (개발용)
    if (process.env.NODE_ENV === 'development') {
      checkIframeSupport();
    }
  }, [id]);
  
  // iframe 로딩 보장
  useEffect(() => {
    if (post) {
      const timer = setTimeout(() => {
        const iframes = document.querySelectorAll('iframe[src*="youtube.com"]');
        iframes.forEach((iframe) => {
          if (!iframe.getAttribute('data-loaded')) {
            iframe.setAttribute('data-loaded', 'true');
            // iframe 재로드
            const src = iframe.getAttribute('src');
            if (src) {
              iframe.setAttribute('src', src);
            }
          }
        });
      }, 1000);
      
      return () => clearTimeout(timer);
    }
  }, [post]);
  
  const checkIframeSupport = () => {
    try {
      // iframe 생성 테스트
      const testIframe = document.createElement('iframe');
      testIframe.style.display = 'none';
      testIframe.src = 'about:blank';
      
      // CSP 정책 체크
      const cspMeta = document.querySelector('meta[http-equiv="Content-Security-Policy"]');
      const cspHeader = cspMeta?.getAttribute('content') || '';
      
      // iframe이 차단되는지 확인
      const isBlocked = cspHeader.includes('frame-src \'none\'') || 
                       cspHeader.includes('child-src \'none\'') ||
                       window.self !== window.top; // 이미 iframe 안에 있는 경우
      
      document.body.appendChild(testIframe);
      
      setTimeout(() => {
        try {
          // iframe 접근 테스트
          const canAccess = testIframe.contentWindow !== null;
          setIframeSupported(!isBlocked && canAccess);
          document.body.removeChild(testIframe);
        } catch (e) {
          setIframeSupported(false);
          document.body.removeChild(testIframe);
        }
      }, 100);
      
    } catch (e) {
      console.log('iframe 지원 체크 실패:', e);
      setIframeSupported(false);
    }
  };
  
  // iframe을 실제 YouTube 플레이어로 렌더링하는 함수
  const processIframeContent = (content: string) => {
    // 이미 완성된 iframe은 그대로 유지
    return content;
  };

  if (loading) return <div className="text-white p-8">로딩 중...</div>;
  if (!post) return <div className="text-red-400 p-8">{error}</div>;

  const isOwnerOrAdmin = user && (user.nickname === post.writer || user.role === 'ADMIN');
  const formattedDate = new Date(post.createdAt).toLocaleString();
  const categoryName = categoryLabels[post.category] || post.category;

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white">
      {/* 헤더 섹션 */}
      <div className="relative overflow-hidden bg-gradient-to-r from-purple-900/50 to-pink-900/50 border-b border-purple-500/20">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/10 to-pink-600/10"></div>
        <div className="relative max-w-4xl mx-auto px-6 py-12">
          <div className="flex items-center gap-4 mb-6">
            <button
              onClick={() => navigate(`/posts?category=${post.category}`)}
              className="flex items-center gap-2 px-4 py-2 bg-white/10 hover:bg-white/20 rounded-full text-sm font-medium transition-all duration-200 backdrop-blur-sm border border-white/20"
            >
              ← {categoryName} 게시판
            </button>
            <div className="w-12 h-12 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-2xl shadow-lg border-2 border-white/20">
              <span style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif', textShadow: '0 0 4px rgba(0,0,0,0.8)' }}>
                {{
                  NEWS: '🚀',
                  DISCUSSION: '💬',
                  IMAGE: '🌌',
                  REVIEW: '⭐',
                  FREE: '🎈',
                  NOTICE: '📢',
                  STARLIGHT_CINEMA: '🎬'
                }[post.category] || '📝'}
              </span>
            </div>
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-6 leading-tight">
            <span className="text-white" style={{color: 'white !important'}}>
              {post.title}
            </span>
          </h1>
          {/* 작성자 정보 */}
          <div className="flex items-center gap-4 mb-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-full border-2 border-purple-400/50 p-1 bg-gradient-to-r from-purple-500/20 to-pink-500/20 backdrop-blur-sm">
                <UserIconDisplay iconName={post.writerIcon} size="large" className="text-2xl" />
              </div>
              <div>
                <ClickableNickname 
                  userId={post.writerId} 
                  nickname={post.writer}
                  className="text-lg font-semibold text-white hover:text-purple-300 transition-colors"
                />
                <div className="flex items-center gap-3 text-sm text-gray-300 mt-1">
                  <span className="flex items-center gap-1">
                    <span className="bg-slate-800/50 rounded px-1 border border-slate-600/30" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>❤️</span> {post.likeCount}
                  </span>
                  <span className="flex items-center gap-1">
                    <span className="bg-slate-800/50 rounded px-1 border border-slate-600/30" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>👁</span> {post.viewCount}
                  </span>
                  <span className="flex items-center gap-1">
                    <span className="bg-slate-800/50 rounded px-1 border border-slate-600/30" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>📅</span> {formattedDate}
                  </span>
                </div>
              </div>
            </div>
            {post.writerCertificates && post.writerCertificates.length > 0 && (
              <div className="flex gap-2 ml-auto">
                {post.writerCertificates.slice(0, 3).map((cert, idx) => {
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
                    <span key={idx} className="inline-flex items-center gap-1 px-2 py-1 bg-gradient-to-r from-yellow-500/20 to-orange-500/20 text-yellow-300 text-xs font-medium rounded-full border border-yellow-500/30 animate-pulse" title={cert}>
                      {icon} {cert}
                    </span>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-6 py-8">
        <div className="bg-gradient-to-br from-slate-800/50 to-purple-900/30 backdrop-blur-md rounded-2xl p-8 border border-purple-500/20 shadow-2xl">
          {/* 게시글 내용 */}
          <div className="mb-8">
            <div className="prose prose-lg max-w-none dark:prose-invert youtube-content post-content">
              <ReactMarkdown
                children={post.content.replace(/🖼️ 관련 이미지: (https?:\/\/[^\s\n]+)/g, '')}
                remarkPlugins={[remarkGfm]}
                rehypePlugins={[rehypeRaw]}
              />
            </div>
            
            {/* iframe 지원 상태 표시 (개발용) */}
            {process.env.NODE_ENV === 'development' && iframeSupported !== null && (
              <div className={`mt-4 p-3 rounded-lg text-sm ${
                iframeSupported 
                  ? 'bg-green-900/30 text-green-300 border border-green-500/30'
                  : 'bg-red-900/30 text-red-300 border border-red-500/30'
              }`}>
                🔧 개발 정보: iframe 지원 {iframeSupported ? '✅ 활성화' : '❌ 차단됨'}
              </div>
            )}
          </div>
        
        {/* S3 이미지 표시 */}
        {post.images && post.images.length > 0 && (
          <div className="mb-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {post.images.map((image) => (
                <div key={image.id} className="relative group">
                  <img
                    src={image.url}
                    alt={image.originalName}
                    className="w-full h-auto rounded-lg shadow-lg hover:shadow-xl transition-shadow cursor-pointer"
                    onClick={() => window.open(image.url, '_blank')}
                    onError={(e) => {
                      console.error('이미지 로드 실패:', image.url);
                      e.currentTarget.style.display = 'none';
                    }}
                  />
                  <div className="absolute bottom-0 left-0 right-0 bg-black/50 text-white text-xs p-2 rounded-b-lg opacity-0 group-hover:opacity-100 transition-opacity">
                    {image.originalName}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
        
        {/* 크롤링 이미지 표시 (외부 URL) */}
        {(() => {
          const imageUrlMatch = post.content.match(/🖼️ 관련 이미지: (https?:\/\/[^\s]+)/g);
          if (imageUrlMatch) {
            const imageUrls = imageUrlMatch.map(match => match.replace('🖼️ 관련 이미지: ', ''));
            return (
              <div className="mb-6">
                <h3 className="text-lg font-semibold mb-3 text-purple-300">🖼️ 관련 이미지</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {imageUrls.map((url, index) => (
                    <div key={index} className="relative group">
                      <img
                        src={url}
                        alt={`관련 이미지 ${index + 1}`}
                        className="w-full h-auto rounded-lg shadow-lg hover:shadow-xl transition-shadow cursor-pointer"
                        onClick={() => window.open(url, '_blank')}
                        onError={(e) => {
                          console.error('외부 이미지 로드 실패:', url);
                          e.currentTarget.style.display = 'none';
                        }}
                      />
                    </div>
                  ))}
                </div>
              </div>
            );
          }
          return null;
        })()}

          {/* 액션 버튼 */}
          <div className="flex flex-wrap gap-3 mb-8 p-6 bg-slate-800/30 rounded-xl border border-slate-700/50">
            {/* 자기가 작성한 글이 아니고 공지글이 아닌 경우에만 추천/신고 버튼 표시 */}
            {user?.nickname !== post.writer && post.category !== 'NOTICE' && (
              <>
                <button
                  onClick={handleLike}
                  disabled={!user || post.likedByMe}
                  className={`flex items-center gap-2 px-6 py-3 rounded-xl font-medium transition-all duration-200 ${
                    !user
                      ? 'bg-gray-600/50 cursor-not-allowed text-gray-400'
                      : post.likedByMe
                      ? 'bg-gray-600/50 cursor-not-allowed text-gray-300'
                      : 'bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 text-white shadow-lg hover:shadow-purple-500/25 transform hover:scale-105'
                  }`}
                >
                  {!user ? '❤️ 로그인 필요' : post.likedByMe ? '✅ 이미 추천함' : `❤️ 추천 (${post.likeCount})`}
                </button>

                <button
                  onClick={handleReport}
                  disabled={!user}
                  className={`flex items-center gap-2 px-6 py-3 rounded-xl font-medium transition-all duration-200 ${
                    !user
                      ? 'bg-gray-600/50 cursor-not-allowed text-gray-400'
                      : 'bg-red-600/80 hover:bg-red-600 text-white shadow-lg hover:shadow-red-500/25 transform hover:scale-105'
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
                  className="flex items-center gap-2 px-6 py-3 rounded-xl bg-blue-600/80 hover:bg-blue-600 text-white font-medium transition-all duration-200 shadow-lg hover:shadow-blue-500/25 transform hover:scale-105"
                >
                  ✏️ 수정
                </button>
                <button
                  onClick={handleDelete}
                  className="flex items-center gap-2 px-6 py-3 rounded-xl bg-gray-600/80 hover:bg-gray-600 text-white font-medium transition-all duration-200 shadow-lg transform hover:scale-105"
                >
                  🗑 삭제
                </button>
              </>
            )}

            {/* 관리자 전용 기능 */}
            {user && user.role === 'ADMIN' && (
              <button
                onClick={handlePostBlind}
                className="flex items-center gap-2 px-6 py-3 rounded-xl bg-orange-600/80 hover:bg-orange-600 text-white font-medium transition-all duration-200 shadow-lg hover:shadow-orange-500/25 transform hover:scale-105"
              >
                👁️‍🗨️ 블라인드
              </button>
            )}
          </div>
          {/* 댓글 섹션 */}
          <div className="border-t border-purple-500/20 pt-8">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-2xl font-bold bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
                💬 댓글 ({post.commentCount || comments.length})
              </h2>
            </div>

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

        <CommentList 
          comments={comments.map(c => ({
            id: c.id,
            content: c.content,
            writer: c.writer,
            createdAt: c.createdAt,
            likeCount: c.likeCount || 0,
            reportCount: 0,
            isPopular: (c.likeCount || 0) >= 3,
            blinded: c.blinded || false,
            deleted: c.deleted || false
          }))}
          postId={Number(id)}
          onRefresh={fetchComments}
        />
          </div>
        </div>
      </div>
    </div>
  );
}
