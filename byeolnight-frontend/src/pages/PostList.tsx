// PostList.tsx
import React, { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import DiscussionTopicBanner from '../components/DiscussionTopicBanner';

// 상수 및 타입 정의

// 게시글 타입 정의
interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  writerIcon?: string;
  blinded: boolean;
  blindType?: string; // 'ADMIN_BLIND' 또는 'REPORT_BLIND'
  likeCount: number;
  likedByMe: boolean;
  hot: boolean;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
  dDay?: string;
}

const CATEGORY_LABELS: Record<string, string> = {
  NEWS: '뉴스',
  DISCUSSION: '토론',
  IMAGE: '사진',
  REVIEW: '후기',
  FREE: '자유',
  NOTICE: '공지',
  STARLIGHT_CINEMA: '별빛 시네마',
};

// AdminUserPage.tsx에서도 사용할 수 있도록 export
export { CATEGORY_LABELS };

// 카테고리 관련 상수
const RESTRICTED_CATEGORIES = ['NEWS', 'NOTICE', 'STARLIGHT_CINEMA'];
const USER_WRITABLE_CATEGORIES = ['DISCUSSION', 'IMAGE', 'REVIEW', 'FREE'];

// 카테고리별 아이콘 매핑
const CATEGORY_ICONS: Record<string, string> = {
  NEWS: '🚀',
  DISCUSSION: '💬',
  IMAGE: '🌌',
  REVIEW: '⭐',
  FREE: '🎈',
  NOTICE: '📢',
  STARLIGHT_CINEMA: '🎬'
};

// 카테고리별 설명 매핑
const CATEGORY_DESCRIPTIONS: Record<string, string> = {
  NEWS: '최신 우주 뉴스를 AI가 자동으로 수집합니다',
  DISCUSSION: 'AI가 생성한 토론 주제로 깊이 있는 대화를 나눠보세요',
  IMAGE: '아름다운 우주 사진을 공유하고 감상해보세요',
  REVIEW: '우주 관련 경험과 후기를 나눠주세요',
  FREE: '우주에 대한 자유로운 이야기를 나눠보세요',
  NOTICE: '중요한 공지사항을 확인하세요',
  STARLIGHT_CINEMA: 'AI가 큐레이션한 우주 영상을 감상하세요'
};

// 작성자 아이콘 통일
const renderStellaIcon = (iconName: string | null | undefined) => {
  return '👤'; // 모든 사용자에게 동일한 아이콘 사용
};

export default function PostList() {
  // 상태 관리
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  // URL 파라미터 추출
  const category = searchParams.get('category') || 'NEWS';
  const sort = searchParams.get('sort') || 'recent';
  const page = Number(searchParams.get('page')) || 0;
  const searchType = searchParams.get('searchType') || 'title';
  const searchKeyword = searchParams.get('search') || '';
  
  // 검색 관련 상태
  const [searchInput, setSearchInput] = useState(searchKeyword);
  const [searchTypeInput, setSearchTypeInput] = useState(searchType);
  
  // 관리자 기능 관련 상태
  const [selectedPosts, setSelectedPosts] = useState<number[]>([]);
  const [showMoveModal, setShowMoveModal] = useState(false);
  
  // 게시글 분류
  const hotPosts = posts.filter((p) => p.hot).slice(0, 4);
  const normalPosts = sort === 'popular' ? posts.slice(0, 30) : posts.filter((p) => !p.hot).slice(0, 25);
  
  // 권한 관련 변수
  const canWrite = user && (USER_WRITABLE_CATEGORIES.includes(category) || user.role === 'ADMIN');
  const isAdmin = user?.role === 'ADMIN';

  // 게시글 데이터 가져오기
  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        const params: any = { category, sort, page, size: 30 };
        if (searchKeyword && searchKeyword.trim()) {
          params.searchType = searchType;
          params.search = searchKeyword.trim();
        }
        
        const res = await axios.get('/public/posts', { params });
        const responseData = res.data?.data || res.data;
        const content = responseData?.content || [];
        setPosts(content);
      } catch (err) {
        console.error('게시글 목록 조회 실패', err);
        setPosts([]); // 오류 시 빈 배열로 설정
      } finally {
        setLoading(false);
      }
    };

    fetchPosts();
  }, [category, sort, page, searchKeyword, searchType]);

  // URL 파라미터 생성 유틸리티 함수
  const createUrlParams = (options: { category?: string, sort?: string, page?: string, searchType?: string, search?: string }) => {
    const params = new URLSearchParams();
    params.set('category', options.category || category);
    params.set('sort', options.sort || sort);
    params.set('page', options.page || String(page));
    
    if (options.search || searchKeyword) {
      params.set('searchType', options.searchType || searchType);
      params.set('search', options.search || searchKeyword);
    }
    
    return params;
  };

  // 카테고리 변경 핸들러
  const handleCategoryChange = (cat: string) => {
    // React 상태 업데이트 배칭 문제 해결
    setTimeout(() => {
      try {
        const params = createUrlParams({ category: cat, page: '0' });
        navigate(`?${params.toString()}`, { replace: true });
      } catch (error) {
        console.error('카테고리 변경 오류:', error);
      }
    }, 0);
  };

  // 정렬 변경 핸들러
  const handleSortChange = (s: string) => {
    const params = createUrlParams({ sort: s, page: '0' });
    navigate(`?${params.toString()}`, { replace: true });
  };

  // 페이지 변경 핸들러
  const handlePageChange = (nextPage: number) => {
    const params = createUrlParams({ page: String(nextPage) });
    navigate(`?${params.toString()}`, { replace: true });
  };
  
  // 검색 핸들러
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchInput.trim()) {
      const params = createUrlParams({ 
        page: '0', 
        searchType: searchTypeInput, 
        search: searchInput.trim() 
      });
      navigate(`?${params.toString()}`, { replace: true });
    }
  };
  
  // 검색 초기화 핸들러
  const handleSearchReset = () => {
    setSearchInput('');
    const params = createUrlParams({ page: '0', search: '' });
    navigate(`?${params.toString()}`, { replace: true });
  };

  // 게시글 선택 관리

  const handlePostSelect = (postId: number, checked: boolean) => {
    if (checked) {
      setSelectedPosts(prev => [...prev, postId]);
    } else {
      setSelectedPosts(prev => prev.filter(id => id !== postId));
    }
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      const allPostIds = [...hotPosts, ...normalPosts].map(p => p.id);
      setSelectedPosts(allPostIds);
    } else {
      setSelectedPosts([]);
    }
  };

  // 관리자 기능: 카테고리 이동
  const handleMoveCategory = async (targetCategory: string) => {
    if (selectedPosts.length === 0) {
      alert('이동할 게시글을 선택하세요.');
      return;
    }
    
    if (!confirm(`선택한 ${selectedPosts.length}개 게시글을 ${CATEGORY_LABELS[targetCategory]} 게시판으로 이동하시겠습니까?`)) {
      return;
    }
    
    try {
      await axios.patch('/admin/posts/move-category', {
        postIds: selectedPosts,
        targetCategory
      });
      alert('카테고리 이동이 완료되었습니다.');
      setSelectedPosts([]);
      setShowMoveModal(false);
      window.location.reload();
    } catch (error) {
      console.error('카테고리 이동 실패:', error);
      alert('카테고리 이동에 실패했습니다.');
    }
  };

  // 게시글 목록 렌더링 함수
  const renderPostList = () => {
    if (loading) {
      return <p className="text-center text-gray-400">🌠 로딩 중...</p>;
    }
    
    if (posts.length === 0) {
      return <p className="text-center text-gray-400">게시글이 없습니다.</p>;
    }
    
    return (
      <>
        {/* 인기 게시글 */}
        {sort === 'recent' && hotPosts.length > 0 && (
          <>
            <h3 className="text-2xl font-semibold mb-4 text-orange-400">🔥 인기 게시글</h3>
            <ul className="grid grid-cols-1 sm:grid-cols-2 gap-6 mb-8">
              {hotPosts.map((post) => renderPostItem(post, true))}
            </ul>
          </>
        )}

        {/* 일반 게시글 */}
        <h3 className="text-2xl font-semibold mb-4 text-white">
          {sort === 'popular' ? '📄 게시글 (추천순)' : '📄 일반 게시글'}
        </h3>
        
        {category === 'IMAGE' ? (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {normalPosts.map((post) => renderImagePostItem(post))}
          </div>
        ) : (
          <ul className="space-y-4">
            {normalPosts.map((post) => renderPostItem(post))}
          </ul>
        )}

        {/* 페이지네이션 */}
        <div className="mt-10 flex justify-center gap-2">
          {page > 0 && (
            <button
              onClick={() => handlePageChange(page - 1)}
              className="px-3 py-1 bg-gray-600 rounded hover:bg-gray-500"
            >
              이전
            </button>
          )}
          <span className="px-3 py-1 bg-gray-800 rounded text-white">Page {page + 1}</span>
          {posts.length >= 30 && (
            <button
              onClick={() => handlePageChange(page + 1)}
              className="px-3 py-1 bg-gray-600 rounded hover:bg-gray-500"
            >
              다음
            </button>
          )}
        </div>
      </>
    );
  };
  
  // 일반 게시글 아이템 렌더링
  const renderPostItem = (post: Post, isHot: boolean = false) => {
    const postClasses = post.blinded 
      ? 'bg-[#1a1a2e]/50 border border-gray-700/30 opacity-70 hover:bg-[#1e1e3a]/60 hover:border-gray-600/40' 
      : isHot
        ? 'from-[#1f2336]/90 to-[#252842]/90 border border-orange-500/30 hover:shadow-orange-500/50'
        : 'bg-[#1f2336]/80 border border-gray-600/50 hover:bg-[#252842]/80 hover:border-purple-500/30 hover:shadow-lg';
    
    return (
      <li
        key={post.id}
        className={`${isHot ? 'bg-gradient-to-br' : ''} rounded-${isHot ? 'xl' : 'lg'} p-${isHot ? '6' : '4'} transition-all duration-${isHot ? '300' : '200'} transform ${isHot ? 'hover:scale-[1.02]' : ''} ${postClasses}`}
      >
        <div className="flex items-start gap-3">
          {isAdmin && (
            <input
              type="checkbox"
              checked={selectedPosts.includes(post.id)}
              onChange={(e) => handlePostSelect(post.id, e.target.checked)}
              className={`w-4 h-4 ${isHot ? 'mb-3' : 'mt-1'}`}
            />
          )}
          <div className="flex-1">
            <Link to={`/posts/${post.id}`} className="block">
              <div className="flex justify-between items-start mb-2">
                <h4 className={`${isHot ? 'text-lg' : 'text-base'} font-semibold text-white flex-1 mr-4 flex items-center gap-2`}>
                  {isHot && <span className="text-orange-400">🔥</span>}
                  {post.dDay && <span className="bg-orange-500 text-white px-2 py-1 rounded text-xs mr-2">[{post.dDay}]</span>}
                  {post.blinded ? (
                    post.blindType === 'ADMIN_BLIND' 
                      ? '관리자가 직접 블라인드 처리한 게시글입니다'
                      : '다수의 신고로 블라인드 처리된 게시글입니다'
                  ) : post.title}
                  {post.blinded && (
                    <span className={`text-xs ml-2 px-2 py-1 rounded ${
                      post.blindType === 'ADMIN_BLIND' 
                        ? 'bg-red-600/20 text-red-400 border border-red-500/30' 
                        : 'bg-yellow-600/20 text-yellow-400 border border-yellow-500/30'
                    }`}>
                      {post.blindType === 'ADMIN_BLIND' ? '관리자 블라인드' : '신고 블라인드'}
                    </span>
                  )}
                </h4>
                {isHot && <span className="bg-orange-500/20 text-orange-300 px-2 py-1 rounded text-xs font-bold">HOT</span>}
                <div className="flex gap-3 text-sm text-gray-400 flex-shrink-0">
                  <span className="flex items-center gap-1">💬 {post.blinded ? '***' : (post.commentCount || 0)}</span>
                  <span className="flex items-center gap-1">❤️ {post.blinded ? '***' : post.likeCount}</span>
                </div>
              </div>
              {isHot && <p className="text-sm text-gray-300 mb-4 line-clamp-2 leading-relaxed">{post.content}</p>}
              <div className="flex justify-between items-center text-sm text-gray-400">
                <span className="flex items-center gap-1">
                  <span className="bg-slate-700/50 rounded px-2 py-1 border border-slate-600/30" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>
                    {post.blinded ? '🔒' : (post.writerIcon ? renderStellaIcon(post.writerIcon) : '✍️')}
                  </span>
                  {post.blinded ? '***' : post.writer}
                </span>
                <span className="flex items-center gap-1">📅 {post.blinded ? '****-**-**' : new Date(post.createdAt).toLocaleDateString()}</span>
              </div>
            </Link>
          </div>
        </div>
      </li>
    );
  };
  
  // 이미지 게시글 아이템 렌더링
  const renderImagePostItem = (post: Post) => {
    // 게시글 내용에서 첫 번째 이미지 URL 추출
    const extractFirstImageUrl = (content: string) => {
      if (!content) return null;
      
      // 다양한 형태의 이미지 태그 처리 (큰따옴표, 작은따옴표 모두 지원)
      const imgRegexes = [
        /<img[^>]+src="([^"]+)"/i,  // 큰따옴표
        /<img[^>]+src='([^']+)'/i,   // 작은따옴표
        /<img[^>]+src=([^\s>]+)/i    // 따옴표 없는 경우
      ];
      
      // 이미지 태그 처리
      for (const regex of imgRegexes) {
        const match = content.match(regex);
        if (match && match[1]) {
          // 따옴표 제거 처리
          let url = match[1];
          if (url.startsWith('"') && url.endsWith('"')) {
            url = url.substring(1, url.length - 1);
          }
          return url;
        }
      }
      
      // 이미지 URL이 직접 포함되어 있는 경우 처리
      const urlRegexes = [
        /(https?:\/\/[^\s]+\.(?:jpg|jpeg|png|gif|webp|svg|bmp))/i,
        /(https?:\/\/[^\s]+\.(?:jpg|jpeg|png|gif|webp|svg|bmp)[^\s]*)/i
      ];
      
      for (const regex of urlRegexes) {
        const match = content.match(regex);
        if (match && match[1]) {
          return match[1];
        }
      }
      
      // S3 URL 형태 처리
      const s3Regex = /https?:\/\/[\w.-]+\.s3\.[\w.-]+\.amazonaws\.com\/[^\s"'<>]+/i;
      const s3Match = content.match(s3Regex);
      if (s3Match) {
        return s3Match[0];
      }
      
      return null;
    };
    
    const imgSrc = post.blinded ? null : extractFirstImageUrl(post.content);
    
    return (
      <div 
        key={post.id} 
        className={`rounded-lg overflow-hidden transition-all duration-300 transform hover:scale-[1.03] ${post.blinded ? 'opacity-70' : ''}`}
      >
        <Link to={`/posts/${post.id}`} className="block h-full">
          <div className="relative aspect-square bg-slate-800/50 overflow-hidden">
            {imgSrc ? (
              <img 
                src={imgSrc} 
                alt={post.title} 
                className="w-full h-full object-cover"
                onError={(e) => {
                  // 이미지 로드 오류 시 기본 이미지로 대체
                  console.log('이미지 로드 오류:', imgSrc);
                  e.currentTarget.onerror = null; // 무한 루프 방지
                  e.currentTarget.src = 'https://via.placeholder.com/300x300?text=우주+이미지';
                }}
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-slate-700/50 to-purple-900/30">
                <span className="text-4xl">🌌</span>
              </div>
            )}
            
            {/* 관리자 체크박스 */}
            {isAdmin && (
              <div className="absolute top-2 left-2 z-10">
                <input
                  type="checkbox"
                  checked={selectedPosts.includes(post.id)}
                  onChange={(e) => handlePostSelect(post.id, e.target.checked)}
                  className="w-4 h-4"
                />
              </div>
            )}
            
            {/* 블라인드 표시 */}
            {post.blinded && (
              <div className="absolute inset-0 bg-black/70 flex items-center justify-center">
                <span className="text-4xl">🔒</span>
              </div>
            )}
          </div>
          
          <div className="p-3 bg-slate-800/80">
            <h4 className="text-sm font-medium text-white line-clamp-1">
              {post.blinded ? (
                post.blindType === 'ADMIN_BLIND' 
                  ? '관리자 블라인드 처리됨'
                  : '신고로 블라인드 처리됨'
              ) : post.title}
            </h4>
            
            <div className="flex justify-between items-center mt-2 text-xs text-gray-400">
              <span className="flex items-center gap-1">
                <span className="bg-slate-700/50 rounded px-1 py-0.5 border border-slate-600/30">
                  {post.blinded ? '🔒' : (post.writerIcon ? renderStellaIcon(post.writerIcon) : '✍️')}
                </span>
                <span className="truncate max-w-[80px]">{post.blinded ? '***' : post.writer}</span>
              </span>
              <div className="flex gap-2">
                <span className="flex items-center">💬 {post.blinded ? '*' : (post.commentCount || 0)}</span>
                <span className="flex items-center">❤️ {post.blinded ? '*' : post.likeCount}</span>
              </div>
            </div>
          </div>
        </Link>
      </div>
    );
  };
  
  // 카테고리 이동 모달 렌더링
  const renderCategoryMoveModal = () => {
    if (!showMoveModal) return null;
    
    return (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div className="bg-[#1f2336] p-6 rounded-xl max-w-md w-full mx-4">
          <h3 className="text-lg font-semibold mb-4">카테고리 이동</h3>
          <p className="text-gray-300 mb-4">선택한 {selectedPosts.length}개 게시글을 어느 게시판으로 이동하시겠습니까?</p>
          <div className="grid grid-cols-2 gap-3 mb-6">
            {Object.entries(CATEGORY_LABELS)
              .filter(([cat]) => cat !== category)
              .map(([cat, label]) => (
                <button
                  key={cat}
                  onClick={() => handleMoveCategory(cat)}
                  className="p-3 bg-[#2a2e45] hover:bg-[#3a3e55] rounded-lg text-center transition-colors"
                >
                  {label}
                </button>
              ))
            }
          </div>
          <div className="flex gap-3">
            <button
              onClick={() => setShowMoveModal(false)}
              className="flex-1 px-4 py-2 bg-gray-600 hover:bg-gray-700 rounded"
            >
              취소
            </button>
          </div>
        </div>
      </div>
    );
  };
  
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white">
      {/* 헤더 섹션 */}
      <div className="relative overflow-hidden bg-gradient-to-r from-purple-900/50 to-pink-900/50 border-b border-purple-500/20">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/10 to-pink-600/10"></div>
        <div className="relative max-w-6xl mx-auto px-6 py-16">
          <div className="text-center">
            <div className="inline-flex items-center gap-3 mb-4">
              <div className="w-16 h-16 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-3xl shadow-lg">
                <span style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>
                  {CATEGORY_ICONS[category]}
                </span>
              </div>
            </div>
            <h1 className="text-5xl font-bold bg-gradient-to-r from-white via-purple-200 to-white bg-clip-text text-transparent mb-4">
              {CATEGORY_LABELS[category]} 게시판
            </h1>
            <p className="text-xl text-gray-300 max-w-2xl mx-auto">
              {CATEGORY_DESCRIPTIONS[category]}
            </p>
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-6 py-8">

        {/* 카테고리 선택 */}
      <div className="text-center mb-10">
          <h2 className="text-3xl font-bold mb-3 flex items-center justify-center gap-2">
            <span className="text-white text-3xl animate-pulse">🚀</span>
            <span className="bg-gradient-to-r from-purple-400 via-pink-400 to-blue-400 bg-clip-text text-transparent">
              게시판 둘러보기
            </span>
          </h2>
          <p className="text-gray-300 text-sm mb-6">다양한 주제의 게시판에서 우주의 신비를 탐험해보세요</p>
          <div className="w-24 h-1 bg-gradient-to-r from-purple-500 via-pink-500 to-blue-500 rounded-full mx-auto"></div>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-7 gap-6">
          <Link to="/posts?category=NEWS&sort=recent" className="group">
            <div className="relative p-6 bg-gradient-to-br from-blue-600/20 to-cyan-600/20 hover:from-blue-600/40 hover:to-cyan-600/40 rounded-2xl border border-blue-500/30 hover:border-blue-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-blue-500/25 overflow-hidden h-full">
              <div className="absolute -top-10 -right-10 w-24 h-24 bg-blue-500/10 rounded-full"></div>
              <div className="absolute -bottom-10 -left-10 w-20 h-20 bg-cyan-500/10 rounded-full"></div>
              <div className="absolute top-2 right-2">
                <span className="bg-gradient-to-r from-blue-500 to-cyan-500 text-white text-xs px-2 py-1 rounded-full font-bold shadow-md">🤖 AI</span>
              </div>
              <div className="w-16 h-16 mx-auto bg-gradient-to-br from-blue-500/20 to-cyan-500/20 rounded-full flex items-center justify-center mb-4 group-hover:animate-pulse">
                <div className="text-3xl" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>🚀</div>
              </div>
              <div className="text-base font-bold text-blue-100 mb-1">우주 뉴스</div>
              <div className="text-xs text-blue-300 bg-blue-500/10 rounded-full py-1 px-3 inline-block">자동 수집</div>
            </div>
          </Link>
          <Link to="/posts?category=DISCUSSION&sort=recent" className="group">
            <div className="relative p-6 bg-gradient-to-br from-green-600/20 to-emerald-600/20 hover:from-green-600/40 hover:to-emerald-600/40 rounded-2xl border border-green-500/30 hover:border-green-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-green-500/25 overflow-hidden h-full">
              <div className="absolute -top-10 -right-10 w-24 h-24 bg-green-500/10 rounded-full"></div>
              <div className="absolute -bottom-10 -left-10 w-20 h-20 bg-emerald-500/10 rounded-full"></div>
              <div className="absolute top-2 right-2">
                <span className="bg-gradient-to-r from-green-500 to-emerald-500 text-white text-xs px-2 py-1 rounded-full font-bold shadow-md">🤖 AI</span>
              </div>
              <div className="w-16 h-16 mx-auto bg-gradient-to-br from-green-500/20 to-emerald-500/20 rounded-full flex items-center justify-center mb-4 group-hover:animate-pulse">
                <div className="text-3xl" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>💬</div>
              </div>
              <div className="text-base font-bold text-green-100 mb-1">토론</div>
              <div className="text-xs text-green-300 bg-green-500/10 rounded-full py-1 px-3 inline-block">주제 생성</div>
            </div>
          </Link>
          <Link to="/posts?category=IMAGE&sort=recent" className="group">
            <div className="relative p-6 bg-gradient-to-br from-purple-600/20 to-indigo-600/20 hover:from-purple-600/40 hover:to-indigo-600/40 rounded-2xl border border-purple-500/30 hover:border-purple-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-purple-500/25 overflow-hidden h-full">
              <div className="absolute -top-10 -right-10 w-24 h-24 bg-purple-500/10 rounded-full"></div>
              <div className="absolute -bottom-10 -left-10 w-20 h-20 bg-indigo-500/10 rounded-full"></div>
              <div className="w-16 h-16 mx-auto bg-gradient-to-br from-purple-500/20 to-indigo-500/20 rounded-full flex items-center justify-center mb-4 group-hover:animate-spin">
                <div className="text-3xl" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>🌌</div>
              </div>
              <div className="text-base font-bold text-purple-100 mb-1">별 사진</div>
              <div className="text-xs text-purple-300 bg-purple-500/10 rounded-full py-1 px-3 inline-block">갤러리</div>
            </div>
          </Link>

          <Link to="/posts?category=REVIEW&sort=recent" className="group">
            <div className="relative p-6 bg-gradient-to-br from-yellow-600/20 to-orange-600/20 hover:from-yellow-600/40 hover:to-orange-600/40 rounded-2xl border border-yellow-500/30 hover:border-yellow-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-yellow-500/25 overflow-hidden h-full">
              <div className="absolute -top-10 -right-10 w-24 h-24 bg-yellow-500/10 rounded-full"></div>
              <div className="absolute -bottom-10 -left-10 w-20 h-20 bg-orange-500/10 rounded-full"></div>
              <div className="w-16 h-16 mx-auto bg-gradient-to-br from-yellow-500/20 to-orange-500/20 rounded-full flex items-center justify-center mb-4 group-hover:animate-pulse">
                <div className="text-3xl" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>⭐</div>
              </div>
              <div className="text-base font-bold text-yellow-100 mb-1">후기</div>
              <div className="text-xs text-yellow-300 bg-yellow-500/10 rounded-full py-1 px-3 inline-block">리뷰 공유</div>
            </div>
          </Link>
          <Link to="/posts?category=FREE&sort=recent" className="group">
            <div className="relative p-6 bg-gradient-to-br from-pink-600/20 to-rose-600/20 hover:from-pink-600/40 hover:to-rose-600/40 rounded-2xl border border-pink-500/30 hover:border-pink-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-pink-500/25 overflow-hidden h-full">
              <div className="absolute -top-10 -right-10 w-24 h-24 bg-pink-500/10 rounded-full"></div>
              <div className="absolute -bottom-10 -left-10 w-20 h-20 bg-rose-500/10 rounded-full"></div>
              <div className="w-16 h-16 mx-auto bg-gradient-to-br from-pink-500/20 to-rose-500/20 rounded-full flex items-center justify-center mb-4 group-hover:animate-bounce">
                <div className="text-3xl" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>🎈</div>
              </div>
              <div className="text-base font-bold text-pink-100 mb-1">자유</div>
              <div className="text-xs text-pink-300 bg-pink-500/10 rounded-full py-1 px-3 inline-block">자유 소통</div>
            </div>
          </Link>
          <Link to="/posts?category=NOTICE&sort=recent" className="group">
            <div className="relative p-6 bg-gradient-to-br from-red-600/20 to-orange-600/20 hover:from-red-600/40 hover:to-orange-600/40 rounded-2xl border border-red-500/30 hover:border-red-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-red-500/25 overflow-hidden h-full">
              <div className="absolute -top-10 -right-10 w-24 h-24 bg-red-500/10 rounded-full"></div>
              <div className="absolute -bottom-10 -left-10 w-20 h-20 bg-orange-500/10 rounded-full"></div>
              <div className="w-16 h-16 mx-auto bg-gradient-to-br from-red-500/20 to-orange-500/20 rounded-full flex items-center justify-center mb-4 group-hover:animate-pulse">
                <div className="text-3xl" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>📢</div>
              </div>
              <div className="text-base font-bold text-red-100 mb-1">공지</div>
              <div className="text-xs text-red-300 bg-red-500/10 rounded-full py-1 px-3 inline-block">중요 안내</div>
            </div>
          </Link>
          <Link to="/posts?category=STARLIGHT_CINEMA&sort=recent" className="group">
            <div className="relative p-6 bg-gradient-to-br from-purple-600/20 to-pink-600/20 hover:from-purple-600/40 hover:to-pink-600/40 rounded-2xl border border-purple-500/30 hover:border-purple-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-purple-500/25 overflow-hidden h-full">
              <div className="absolute -top-10 -right-10 w-24 h-24 bg-purple-500/10 rounded-full"></div>
              <div className="absolute -bottom-10 -left-10 w-20 h-20 bg-pink-500/10 rounded-full"></div>
              <div className="absolute top-2 right-2">
                <span className="bg-gradient-to-r from-purple-500 to-pink-500 text-white text-xs px-2 py-1 rounded-full font-bold shadow-md">🤖 AI</span>
              </div>
              <div className="w-16 h-16 mx-auto bg-gradient-to-br from-purple-500/20 to-pink-500/20 rounded-full flex items-center justify-center mb-4 group-hover:animate-pulse">
                <div className="text-3xl" style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>🎬</div>
              </div>
              <div className="text-base font-bold text-purple-100 mb-1">별빛 시네마</div>
              <div className="text-xs text-purple-300 bg-purple-500/10 rounded-full py-1 px-3 inline-block">영상 큐레이션</div>
            </div>
          </Link>
        </div>

        {/* 게시판별 이용안내 배너 */}
        {category === 'NEWS' && (
          <div className="mb-6 p-4 bg-blue-900/30 rounded-lg border border-blue-600/30">
            <div className="flex items-center gap-3 mb-2">
              <span className="text-2xl">🤖</span>
              <h3 className="text-lg font-semibold text-blue-200">자동 뉴스 업데이트</h3>
            </div>
            <p className="text-blue-200 text-sm leading-relaxed">
              <strong>뉴스봇</strong>이 매일 <strong>오전 8시</strong>에 우주 뉴스를 자동 수집합니다.
              <br />
              <strong>[대상 출처]</strong><br />
              <strong>NewsData.io API:</strong> 전 세계 한국어 우주 관련 뉴스<br />
              <strong>키워드:</strong> 우주, 천문학, NASA, 스페이스X, 화성, 달, 위성, 항공우주
              <br />
              매번 새 게시글로 등록되며, 중복 가능성은 낮습니다.
            </p>
          </div>
        )}
        
        {category === 'IMAGE' && (
          <div className="mb-6 p-4 bg-purple-900/30 rounded-lg border border-purple-600/30">
            <div className="flex items-center gap-3 mb-2">
              <span className="text-2xl">🌌</span>
              <h3 className="text-lg font-semibold text-purple-200">사진 게시판 이용안내</h3>
            </div>
            <p className="text-purple-200 text-sm leading-relaxed">
              우주와 관련된 <strong>아름다운 사진들</strong>을 공유해주세요! 하늘, 별, 행성, 우주 관측 사진 등 모든 우주 관련 이미지를 환영합니다.
              <br />
              <strong>📷 추천 컨텐츠:</strong> 천체 사진, 우주 관측 사진, 우주 관련 예술 작품, 우주선 및 우주 정거장 사진
            </p>
          </div>
        )}
        
        {category === 'REVIEW' && (
          <div className="mb-6 p-4 bg-yellow-900/30 rounded-lg border border-yellow-600/30">
            <div className="flex items-center gap-3 mb-2">
              <span className="text-2xl">⭐</span>
              <h3 className="text-lg font-semibold text-yellow-200">후기 게시판 이용안내</h3>
            </div>
            <p className="text-yellow-200 text-sm leading-relaxed">
              우주 관련 <strong>경험과 후기</strong>를 나눠주세요! 천체 관측, 과학관 방문, 우주 관련 도서나 영화 감상문 등을 공유해주세요.
              <br />
              <strong>🎆 추천 컨텐츠:</strong> 천체관측 후기, 과학관 방문기, 우주 관련 도서/영화 리뷰, 망원경 구매 후기
            </p>
          </div>
        )}
        
        {category === 'FREE' && (
          <div className="mb-6 p-4 bg-green-900/30 rounded-lg border border-green-600/30">
            <div className="flex items-center gap-3 mb-2">
              <span className="text-2xl">🎈</span>
              <h3 className="text-lg font-semibold text-green-200">자유 게시판 이용안내</h3>
            </div>
            <p className="text-green-200 text-sm leading-relaxed">
              우주와 관련된 <strong>자유로운 이야기</strong>를 나눠주세요! 우주에 대한 궁금증, 생각, 일상 이야기 등 어떤 주제든 환영합니다.
              <br />
              <strong>🚀 추천 컨텐츠:</strong> 우주 관련 질문, 일상 이야기, 우주 관련 꿈과 목표, 우주 관련 취미 공유
            </p>
          </div>
        )}
        
        {category === 'NOTICE' && (
          <div className="mb-6 p-4 bg-red-900/30 rounded-lg border border-red-600/30">
            <div className="flex items-center gap-3 mb-2">
              <span className="text-2xl">📢</span>
              <h3 className="text-lg font-semibold text-red-200">공지사항 게시판</h3>
            </div>
            <p className="text-red-200 text-sm leading-relaxed">
              사이트 운영과 관련된 <strong>중요한 공지사항</strong>을 확인하세요. 업데이트, 이벤트, 정책 변경 등의 정보를 안내합니다.
              <br />
              <strong>⚠️ 주의:</strong> 공지사항은 관리자만 작성할 수 있습니다.
            </p>
          </div>
        )}
        
        {category === 'STARLIGHT_CINEMA' && (
          <div className="mb-6 p-4 bg-gradient-to-r from-purple-900/30 to-pink-900/30 rounded-lg border border-purple-600/30">
            <div className="flex items-center gap-3 mb-2">
              <span className="text-2xl">🤖</span>
              <h3 className="text-lg font-semibold text-purple-200">AI 별빛 시네마 큐레이션</h3>
            </div>
            <p className="text-purple-200 text-sm leading-relaxed">
              <strong>시네마봇</strong>이 매일 <strong>오후 8시</strong>에 우주 관련 YouTube 영상을 자동 큐레이션합니다.
              <br />
              <strong>🎬 큐레이션 내용:</strong> 우주 다큐멘터리, 천체 관측 영상, NASA 공식 영상, 우주 과학 교육 콘텐츠
              <br />
              <strong>🤖 AI 요약:</strong> 각 영상마다 GPT가 생성한 한국어 요약 제공
              <br />
              <strong>⚠️ 주의:</strong> 별빛 시네마는 AI 봇 전용 게시판입니다.
            </p>
          </div>
        )}
        
        {/* 토론 카테고리 - 오늘의 토론 주제 배너 */}
        {category === 'DISCUSSION' && <DiscussionTopicBanner />}
        
        {/* 관리자 토론 주제 생성 */}
        {category === 'DISCUSSION' && user?.role === 'ADMIN' && (
          <div className="mb-6 p-4 bg-blue-900/30 rounded-lg border border-blue-600/30">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-lg font-semibold text-blue-200 mb-2">🤖 관리자 토론 주제 관리</h3>
                <p className="text-blue-200 text-sm">
                  매일 오전 8시 자동 생성 | 스케줄 실패 시 수동 생성 가능
                </p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={async () => {
                    try {
                      const response = await axios.get('/admin/discussions/status');
                      const data = response.data?.data || response.data;
                      
                      if (typeof data === 'object' && data !== null) {
                        const { totalDiscussionPosts, lastUpdated, todayTopicExists, todayTopicTitle } = data;
                        
                        // 시스템 상태 판단
                        const now = new Date();
                        const today = now.toDateString();
                        const isHealthy = todayTopicExists || totalDiscussionPosts > 0;
                        const statusIcon = isHealthy ? '✅' : '⚠️';
                        const statusText = isHealthy ? '정상 작동 중' : '주의 필요';
                        
                        let warningMessage = '';
                        if (!todayTopicExists) {
                          warningMessage = '\n⚠️ 오늘의 토론 주제가 생성되지 않았습니다. 수동 생성을 고려해주세요.';
                        }
                        
                        const statusMessage = `📊 토론 게시판 상태\n\n` +
                          `• 총 토론 게시글: ${totalDiscussionPosts || 0}개\n` +
                          `• 오늘의 토론 주제: ${todayTopicExists ? '생성됨' : '없음'}\n` +
                          `${todayTopicTitle ? `• 주제: "${todayTopicTitle}"\n` : ''}` +
                          `• 마지막 업데이트: ${lastUpdated ? new Date(lastUpdated).toLocaleString('ko-KR') : '정보 없음'}\n\n` +
                          `${statusIcon} 토론 시스템 ${statusText}${warningMessage}`;
                        alert(statusMessage);
                      } else {
                        alert('⚠️ 토론 시스템 상태를 확인할 수 없습니다.');
                      }
                    } catch (error) {
                      console.error('상태 확인 실패:', error);
                      alert('상태 확인에 실패했습니다.');
                    }
                  }}
                  className="bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg transition-colors"
                >
                  📊 상태 확인
                </button>
                <button
                  onClick={async () => {
                    if (!confirm('새로운 토론 주제를 생성하시겠습니까? 기존 주제는 비활성화됩니다.')) return;
                    
                    try {
                      const response = await axios.post('/admin/discussions/generate-topic');
                      alert('토론 주제가 성공적으로 생성되었습니다!');
                      window.location.reload();
                    } catch (error) {
                      console.error('토론 주제 생성 실패:', error);
                      alert('토론 주제 생성에 실패했습니다.');
                    }
                  }}
                  className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors"
                >
                  🎯 토론 주제 생성
                </button>
              </div>
            </div>
          </div>
        )}
        
        {/* 관리자 별빛 시네마 생성 */}
        {category === 'STARLIGHT_CINEMA' && user?.role === 'ADMIN' && (
          <div className="mb-6 p-4 bg-purple-900/30 rounded-lg border border-purple-600/30">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-lg font-semibold text-purple-200 mb-2">🤖 관리자 시네마 관리</h3>
                <p className="text-purple-200 text-sm">
                  매일 오후 8시 자동 생성 | 스케줄 실패 시 수동 생성 가능
                </p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={async () => {
                    try {
                      const response = await axios.get('/admin/cinema/status');
                      const data = response.data?.data || response.data;
                      
                      if (typeof data === 'object' && data !== null) {
                        const { totalCinemaPosts, lastUpdated, latestPostExists, latestPostTitle } = data;
                        
                        // 시스템 상태 판단
                        const now = new Date();
                        const lastUpdateTime = lastUpdated ? new Date(lastUpdated) : null;
                        const daysSinceUpdate = lastUpdateTime ? Math.floor((now - lastUpdateTime) / (1000 * 60 * 60 * 24)) : 999;
                        
                        const isHealthy = latestPostExists && totalCinemaPosts > 0 && daysSinceUpdate < 2;
                        const statusIcon = isHealthy ? '✅' : '⚠️';
                        const statusText = isHealthy ? '정상 작동 중' : '주의 필요';
                        
                        let warningMessage = '';
                        if (!latestPostExists) {
                          warningMessage = '\n⚠️ 최신 시네마 포스트가 없습니다. 수동 생성을 고려해주세요.';
                        } else if (daysSinceUpdate >= 2) {
                          warningMessage = `\n⚠️ 마지막 업데이트가 ${daysSinceUpdate}일 전입니다. 스케줄러 확인이 필요합니다.`;
                        }
                        
                        const statusMessage = `🎬 별빛 시네마 상태\n\n` +
                          `• 총 시네마 게시글: ${totalCinemaPosts || 0}개\n` +
                          `• 최신 포스트: ${latestPostExists ? '있음' : '없음'}\n` +
                          `${latestPostTitle ? `• 제목: "${latestPostTitle}"\n` : ''}` +
                          `• 마지막 업데이트: ${lastUpdated ? new Date(lastUpdated).toLocaleString('ko-KR') : '정보 없음'}\n\n` +
                          `${statusIcon} 별빛 시네마 시스템 ${statusText}${warningMessage}`;
                        alert(statusMessage);
                      } else {
                        alert('⚠️ 별빛 시네마 시스템 상태를 확인할 수 없습니다.');
                      }
                    } catch (error) {
                      console.error('상태 확인 실패:', error);
                      alert('상태 확인에 실패했습니다.');
                    }
                  }}
                  className="bg-pink-600 hover:bg-pink-700 text-white px-4 py-2 rounded-lg transition-colors"
                >
                  📊 상태 확인
                </button>
                <button
                  onClick={async () => {
                    if (!confirm('새로운 별빛 시네마 포스트를 생성하시겠습니까?')) return;
                    
                    try {
                      const response = await axios.post('/admin/cinema/generate-post');
                      alert('별빛 시네마 포스트가 성공적으로 생성되었습니다!');
                      window.location.reload();
                    } catch (error) {
                      console.error('별빛 시네마 생성 실패:', error);
                      alert('별빛 시네마 생성에 실패했습니다.');
                    }
                  }}
                  className="bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg transition-colors"
                >
                  🎬 시네마 포스트 생성
                </button>
              </div>
            </div>
          </div>
        )}
        
        {/* 관리자 뉴스 생성 */}
        {category === 'NEWS' && user?.role === 'ADMIN' && (
          <div className="mb-6 p-4 bg-green-900/30 rounded-lg border border-green-600/30">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-lg font-semibold text-green-200 mb-2">🤖 관리자 뉴스 관리</h3>
                <p className="text-green-200 text-sm">
                  매일 오전 8시 자동 수집 | 스케줄 실패 시 수동 수집 가능
                </p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={async () => {
                    try {
                      const response = await axios.get('/admin/crawler/status');
                      alert(response.data.data || '뉴스 크롤러 시스템이 정상 작동 중입니다.');
                    } catch (error) {
                      console.error('상태 확인 실패:', error);
                      alert('상태 확인에 실패했습니다.');
                    }
                  }}
                  className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors"
                >
                  📊 상태 확인
                </button>
                <button
                  onClick={async () => {
                    if (!confirm('NewsData.io API를 통해 최신 우주 뉴스를 수집하시겠습니까?')) return;
                    
                    try {
                      const response = await axios.post('/admin/crawler/start');
                      alert('뉴스 수집이 성공적으로 완료되었습니다!');
                      window.location.reload();
                    } catch (error) {
                      console.error('뉴스 수집 실패:', error);
                      alert('뉴스 수집에 실패했습니다.');
                    }
                  }}
                  className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg transition-colors"
                >
                  🚀 뉴스 수집
                </button>
              </div>
            </div>
          </div>
        )}
        


        {/* 검색 기능 */}
        <div className="mb-8">
          <div className="bg-gradient-to-r from-slate-800/50 to-purple-900/30 backdrop-blur-md rounded-2xl p-6 border border-purple-500/20 shadow-lg">
            <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-4">
              {/* 검색 유형 선택 */}
              <select
                value={searchTypeInput}
                onChange={(e) => setSearchTypeInput(e.target.value)}
                className="bg-slate-700/50 text-white rounded-xl px-4 py-3 text-sm border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all duration-200"
                aria-label="검색 유형 선택"
              >
                <option value="title">제목</option>
                <option value="content">내용</option>
                <option value="titleAndContent">제목+내용</option>
                <option value="writer">글작성자</option>
              </select>
              
              {/* 검색어 입력 */}
              <input
                type="text"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="검색어를 입력하세요..."
                className="flex-1 bg-slate-700/50 text-white rounded-xl px-4 py-3 text-sm border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent placeholder-gray-400 transition-all duration-200"
                aria-label="검색어 입력"
              />
              
              {/* 검색 버튼 */}
              <div className="flex gap-3">
                <button
                  type="submit"
                  className="px-6 py-3 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 text-white rounded-xl text-sm font-medium transition-all duration-200 transform hover:scale-105 shadow-lg hover:shadow-purple-500/25"
                  aria-label="검색 실행"
                >
                  🔍 검색
                </button>
                
                {/* 초기화 버튼 (검색어가 있을 때만 표시) */}
                {searchKeyword && (
                  <button
                    type="button"
                    onClick={handleSearchReset}
                    className="px-6 py-3 bg-slate-600 hover:bg-slate-700 text-white rounded-xl text-sm font-medium transition-all duration-200"
                    aria-label="검색 초기화"
                  >
                    초기화
                  </button>
                )}
              </div>
            </form>
            
            {/* 현재 검색 상태 표시 */}
            {searchKeyword && (
              <div className="mt-4 p-3 bg-purple-900/30 rounded-lg border border-purple-600/30 animate-fadeIn">
                <p className="text-sm text-purple-200 flex items-center gap-2">
                  <span className="text-purple-300">검색 결과:</span> 
                  <span className="font-semibold text-white bg-purple-800/30 px-2 py-1 rounded">"{searchKeyword}"</span> 
                  <span className="text-purple-300 text-xs bg-purple-800/20 px-2 py-1 rounded-full">
                    {searchType === 'titleAndContent' ? '제목+내용' : 
                     searchType === 'title' ? '제목' : 
                     searchType === 'content' ? '내용' : '글작성자'}
                  </span>
                </p>
              </div>
            )}
          </div>
        </div>

        {/* 정렬 및 글쓰기 */}
        <div className="flex justify-between items-center mb-6">
          <div className="flex items-center gap-4">
            {/* 정렬 옵션 */}
            <div className="flex items-center gap-2">
              <label className="text-base text-gray-300">정렬:</label>
              <select
                value={sort}
                onChange={(e) => handleSortChange(e.target.value)}
                className="bg-[#2a2e45] text-sm rounded px-3 py-1 focus:outline-none focus:ring-2 focus:ring-purple-500"
              >
                <option value="recent">최신순</option>
                <option value="popular">추천순</option>
              </select>
            </div>
            
            {/* 관리자 기능: 게시글 선택 및 카테고리 이동 */}
            {isAdmin && (
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="selectAll"
                  checked={selectedPosts.length > 0 && selectedPosts.length === [...hotPosts, ...normalPosts].length}
                  onChange={(e) => handleSelectAll(e.target.checked)}
                  className="w-4 h-4"
                />
                <label htmlFor="selectAll" className="text-sm text-gray-300">전체선택</label>
                {selectedPosts.length > 0 && (
                  <button
                    onClick={() => setShowMoveModal(true)}
                    className="ml-2 px-3 py-1 bg-blue-600 hover:bg-blue-700 text-white text-sm rounded transition-colors"
                  >
                    카테고리 이동 ({selectedPosts.length})
                  </button>
                )}
              </div>
            )}
          </div>
          
          {/* 글쓰기 버튼 */}
          {canWrite && (
            <Link
              to={`/posts/write?fixedCategory=${category}`}
              className="px-6 py-2 bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white font-semibold rounded-lg shadow-lg hover:shadow-xl transition-all duration-200 transform hover:scale-105"
            >
              ✍️ {CATEGORY_LABELS[category]} 글쓰기
            </Link>
          )}
        </div>

        {/* 게시글 목록 */}
        {renderPostList()}
        
        {/* 카테고리 이동 모달 */}
        {renderCategoryMoveModal()}
      </div>
    </div>
  );
}
