import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import { MarkdownRenderer, NewsArticleContent } from '../components/post';
import { ClickableNickname, UserIconDisplay } from '../components/user';
import { CommentList, CommentForm } from '../components/post';
import { getErrorMessage, isAxiosError } from '../types/api';
import type { PostDetail as Post } from '../types/post';
import { StarfieldBackground } from '../components/common';

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
  STARLIGHT_CINEMA: '별빛시네마',
};

export default function PostDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { t } = useTranslation();
  
  const [post, setPost] = useState<Post | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // iframe 렌더링을 위한 전역 CSS 스타일 추가
  useEffect(() => {
    const style = document.createElement('style');
    style.textContent = `

      /* 글씨 크기 및 스타일 적용 */
      .post-content { overflow-wrap: break-word !important; word-wrap: break-word !important; }
      .post-content h1 { font-size: 2rem !important; font-weight: bold !important; margin: 1.5rem 0 1rem 0 !important; color: #e2e8f0 !important; }
      .post-content h2 { font-size: 1.75rem !important; font-weight: bold !important; margin: 1.25rem 0 0.75rem 0 !important; color: #e2e8f0 !important; }
      .post-content h3 { font-size: 1.5rem !important; font-weight: bold !important; margin: 1rem 0 0.5rem 0 !important; color: #e2e8f0 !important; }
      .post-content h4 { font-size: 1.25rem !important; font-weight: bold !important; margin: 0.75rem 0 0.5rem 0 !important; color: #e2e8f0 !important; }
      .post-content h5 { font-size: 1.125rem !important; font-weight: bold !important; margin: 0.75rem 0 0.5rem 0 !important; color: #e2e8f0 !important; }
      .post-content h6 { font-size: 1rem !important; font-weight: bold !important; margin: 0.5rem 0 0.25rem 0 !important; color: #e2e8f0 !important; }
      .post-content img { max-width: 100% !important; height: auto !important; margin: 16px 0 !important; border-radius: 8px !important; display: block !important; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important; cursor: pointer !important; }
      .post-content img:hover { opacity: 0.9 !important; transition: opacity 0.2s ease !important; }
      .post-content p { font-size: 1rem !important; line-height: 1.7 !important; margin: 0.4rem 0 !important; color: #cbd5e1 !important; white-space: pre-wrap !important; word-wrap: break-word !important; }
      .post-content strong { font-weight: bold !important; color: #f1f5f9 !important; }
      .post-content em { font-style: italic !important; color: #a78bfa !important; }
      .post-content u { text-decoration: underline !important; font-weight: normal !important; color: #cbd5e1 !important; }
      .post-content s, .post-content del { text-decoration: line-through !important; color: #94a3b8 !important; }
      .post-content ul, .post-content ol { padding-left: 1.5rem !important; }
      .post-content li { color: #cbd5e1 !important; line-height: 1.6 !important; white-space: pre-wrap !important; }
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

  const [iframeSupported, setIframeSupported] = useState<boolean | null>(null);

  const fetchPost = async () => {
    try {
      const res = await axios.get(`/public/posts/${id}`);
      // 응답 구조 안전하게 처리
      const postData = res.data?.data || res.data;
      console.log('게시글 데이터:', postData);
      console.log('게시글 이미지 데이터:', postData?.images);
      
      // 삭제된 게시글은 관리자도 접근 불가
      if (postData && postData.deleted) {
        setError('삭제된 게시글입니다.');
        setLoading(false);
        return;
      }
      
      // 블라인드 처리된 게시글은 관리자만 접근 가능
      if (postData && postData.blinded && (!user || user.role !== 'ADMIN')) {
        setError('블라인드 처리된 게시글입니다.');
        setLoading(false);
        return;
      }
      
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
    } catch (err: unknown) {
      console.error('게시글 조회 실패:', err);
      if (isAxiosError(err) && err.response?.status === 404) {
        setError(t('home.post_not_found'));
      } else {
        setError(t('home.cannot_load_post'));
      }
    } finally {
      setLoading(false);
    }
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
      
      console.log('처리된 댓글:', enhancedComments);
      
      setComments(enhancedComments);
    } catch (err: unknown) {
      console.error('댓글 조회 실패:', err);
      console.error('에러 상세:', isAxiosError(err) ? err.response : err);
      setComments([]);
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
    } catch (err: unknown) {
      alert(getErrorMessage(err));
    }
  };

  const handleReport = async () => {
    if (!user) {
      alert('로그인이 필요합니다.');
      return;
    }
    navigate(`/posts/${id}/report`);
  };

  const handlePostBlind = async () => {
    if (!confirm('이 게시글을 블라인드 처리하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/posts/${id}/blind`);
      alert('게시글이 블라인드 처리되었습니다.');
      navigate(`/posts?category=${post?.category || 'FREE'}`);
    } catch (error: unknown) {
      console.error('게시글 블라인드 실패:', error);
      alert(getErrorMessage(error));
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
    // ID 유효성 검사
    if (!id || isNaN(Number(id))) {
      setLoading(false);
      return;
    }
    
    const loadData = async () => {
      await fetchPost();
      await fetchComments();
    };
    
    loadData();
    
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
  


  // ID 유효성 검사 - 로딩 완료 후에만 실행
  if (!loading && (!id || isNaN(Number(id)))) {
    return (
      <div className="min-h-screen bg-space-gradient text-white py-12 px-6">
        <div className="max-w-4xl mx-auto bg-[#1f2336]/80 backdrop-blur-md p-8 rounded-xl shadow-xl">
          <div className="text-red-400 text-center">
            <h1 className="text-2xl font-bold mb-4">{t('home.invalid_access')}</h1>
            <p className="mb-4">{t('home.invalid_post_id')}</p>
            <button 
              onClick={() => navigate('/posts')}
              className="px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded transition"
            >
              {t('home.back_to_posts')}
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-space-gradient text-white flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-16 w-16 border-4 border-purple-500 border-t-transparent mb-4"></div>
          <p className="text-xl font-medium text-purple-300">{t('home.loading_post')}</p>
          <p className="text-sm text-gray-400 mt-2">{t('home.please_wait')}</p>
        </div>
      </div>
    );
  }
  
  if (!post || error) {
    return (
      <div className="min-h-screen bg-space-gradient text-white py-12 px-6">
        <div className="max-w-4xl mx-auto bg-[#1f2336]/80 backdrop-blur-md p-8 rounded-xl shadow-xl">
          <div className="text-red-400 text-center">
            <h1 className="text-2xl font-bold mb-4">{t('home.inaccessible_post')}</h1>
            <button 
              onClick={() => navigate('/posts')}
              className="px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded transition"
            >
              {t('home.back_to_posts')}
            </button>
          </div>
        </div>
      </div>
    );
  }

  const isOwnerOrAdmin = user && (user.nickname === post.writer || user.role === 'ADMIN');
  // createdAt 사용 (업데이트 시간이 아닌 작성 시간 표시)
  const formattedDate = new Date(post.createdAt).toLocaleString();
  const categoryName = categoryLabels[post.category] || post.category;

  return (
    <div className="min-h-screen bg-space-gradient text-white relative">
      <div className="fixed inset-0 -z-10 pointer-events-none">
        <StarfieldBackground density={50} />
      </div>
      {/* 헤더 섹션 - 모바일 최적화 */}
      <div className="relative overflow-hidden bg-gradient-to-r from-purple-900/50 to-pink-900/50 border-b border-purple-500/20">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/10 to-pink-600/10"></div>
        <div className="relative max-w-4xl mx-auto px-4 sm:px-6 py-6 sm:py-12">
          <div className="flex items-center gap-2 sm:gap-4 mb-4 sm:mb-6">
            <button
              onClick={() => navigate(`/posts?category=${post.category}`)}
              className="flex items-center gap-1 sm:gap-2 px-3 sm:px-4 py-2 bg-white/10 hover:bg-white/20 active:bg-white/30 rounded-full text-xs sm:text-sm font-medium transition-all duration-200 backdrop-blur-sm border border-white/20 min-h-[44px] touch-manipulation"
            >
              ← <span className="hidden sm:inline">{categoryName} {t('home.board')}</span>
              <span className="sm:hidden">{categoryName}</span>
            </button>
            <div className="w-10 h-10 sm:w-12 sm:h-12 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-xl sm:text-2xl shadow-lg border-2 border-white/20">
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
          <h1 className="text-2xl sm:text-4xl md:text-5xl font-bold mb-4 sm:mb-6 leading-tight">
            <span className="text-white" style={{color: 'white !important'}}>
              {post.title}
            </span>
          </h1>
          {/* 작성자 정보 - 모바일 최적화 */}
          <div className="flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-4 mb-4">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 sm:w-12 sm:h-12 rounded-full border-2 border-purple-400/50 bg-gradient-to-r from-purple-500/20 to-pink-500/20 backdrop-blur-sm flex items-center justify-center overflow-hidden">
                <UserIconDisplay iconName={post.writerIcon} size="small" className="text-xl sm:text-2xl" />
              </div>
              <div className="flex-1">
                <div className="flex items-center gap-2 sm:gap-3 flex-wrap">
                  <span className="text-base sm:text-lg font-semibold text-white">{post.writer}</span>
                  <ClickableNickname 
                    userId={post.writerId} 
                    nickname={post.writer}
                    className="text-xs text-gray-400 hover:text-purple-300 active:text-purple-200 transition-colors border border-gray-600 hover:border-purple-400 px-2 py-1 rounded min-h-[32px] touch-manipulation"
                  >
                    {t('home.user_info')}
                  </ClickableNickname>
                </div>
                <div className="flex items-center gap-2 sm:gap-3 text-xs sm:text-sm text-gray-300 mt-1 flex-wrap">
                  <span className="flex items-center gap-1">
                    <span className="bg-slate-800/50 rounded px-1 border border-slate-600/30" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>❤️</span> {post.likeCount}
                  </span>
                  <span className="flex items-center gap-1">
                    <span className="bg-slate-800/50 rounded px-1 border border-slate-600/30" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>👁</span> {post.viewCount}
                  </span>
                  <span className="flex items-center gap-1">
                    <span className="bg-slate-800/50 rounded px-1 border border-slate-600/30" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>📅</span> 
                    <span className="hidden sm:inline">{formattedDate}</span>
                    <span className="sm:hidden">{new Date(post.createdAt).toLocaleDateString()}</span>
                  </span>
                </div>
              </div>
            </div>
            {post.writerCertificates && post.writerCertificates.length > 0 && (
              <div className="flex gap-1 sm:gap-2 flex-wrap">
                {post.writerCertificates.slice(0, 2).map((cert, idx) => {
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
                      {icon} <span className="hidden sm:inline">{cert}</span>
                    </span>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 sm:px-6 py-4 sm:py-8">
        <div className="bg-gradient-to-br from-slate-800/50 to-purple-900/30 backdrop-blur-md rounded-xl sm:rounded-2xl p-4 sm:p-8 border border-purple-500/20 shadow-2xl">
          {/* 게시글 내용 */}
          <div className="mb-8">
            {post.category === 'NEWS' ? (
              <NewsArticleContent content={post.content} />
            ) : (
              <MarkdownRenderer content={post.content} />
            )}
            
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
          

        
        
        {/* 크롤링 이미지 표시 (외부 URL) */}
        {(() => {
          const imageUrlMatch = post.content?.match(/🖼️ 관련 이미지: (https?:\/\/[^\s]+)/g);
          if (imageUrlMatch) {
            const imageUrls = imageUrlMatch.map(match => match.replace('🖼️ 관련 이미지: ', ''));
            return (
              <div className="mb-6">
                <h3 className="text-lg font-semibold mb-3 text-purple-300">🖼️ {t('home.related_images')}</h3>
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
                  {!user ? `❤️ ${t('home.login_required_like')}` : post.likedByMe ? `✅ ${t('home.already_liked')}` : `❤️ ${t('home.like_with_count')} (${post.likeCount})`}
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
                  🚨 {t('home.report')}
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
                  ✏️ {t('home.edit')}
                </button>
                <button
                  onClick={handleDelete}
                  className="flex items-center gap-2 px-6 py-3 rounded-xl bg-gray-600/80 hover:bg-gray-600 text-white font-medium transition-all duration-200 shadow-lg transform hover:scale-105"
                >
                  🗑 {t('home.delete')}
                </button>
              </>
            )}

            {/* 관리자 전용 기능 */}
            {user && user.role === 'ADMIN' && (
              <button
                onClick={handlePostBlind}
                className="flex items-center gap-2 px-6 py-3 rounded-xl bg-orange-600/80 hover:bg-orange-600 text-white font-medium transition-all duration-200 shadow-lg hover:shadow-orange-500/25 transform hover:scale-105"
              >
                👁️‍🗨️ {t('home.blind')}
              </button>
            )}
          </div>
          {/* 댓글 섹션 */}
          <div className="border-t border-purple-500/20 pt-8">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-2xl font-bold bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
                💬 {t('home.comments')} ({post.commentCount || comments.length})
              </h2>
            </div>

        <CommentForm 
          postId={Number(id)} 
          onCommentAdded={fetchComments}
        />

        <CommentList 
          comments={comments.map(c => ({
            id: c.id,
            content: c.content,
            writer: c.writer,
            writerId: c.writerId,
            createdAt: c.createdAt,
            likeCount: c.likeCount || 0,
            reportCount: 0,
            isPopular: (c.likeCount || 0) >= 3,
            blinded: c.blinded || false,
            deleted: c.deleted || false,
            writerIcon: c.writerIcon,
            writerCertificates: c.writerCertificates,
            parentId: c.parentId,
            parentWriter: c.parentWriter
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
