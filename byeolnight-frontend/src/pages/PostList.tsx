// PostList.tsx
import React, { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';

// 조건부 import
const DiscussionTopicBanner = React.lazy(() => import('../components/post/DiscussionTopicBanner'));

// 게시글 타입 정의
interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  writerIcon?: string;
  blinded: boolean;
  blindType?: string;
  likeCount: number;
  likedByMe: boolean;
  hot: boolean;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
  dDay?: string;
  thumbnailUrl?: string;
}

// 카테고리 라벨 함수 (다국어 지원)
const getCategoryLabel = (category: string, t: any): string => {
  const labels: Record<string, string> = {
    NEWS: t('home.space_news'),
    DISCUSSION: t('home.discussion'),
    IMAGE: t('home.star_photo'),
    REVIEW: t('home.review'),
    FREE: t('home.free'),
    NOTICE: t('home.notice'),
    STARLIGHT_CINEMA: t('home.star_cinema'),
  };
  return labels[category] || category;
};

// 카테고리 설명 함수 (다국어 지원)
const getCategoryDescription = (category: string, t: any): string => {
  const descriptions: Record<string, string> = {
    NEWS: t('home.news_auto_desc'),
    DISCUSSION: t('home.discussion_auto_desc'),
    IMAGE: t('home.share_beautiful_space_photos'),
    REVIEW: t('home.share_space_experiences'),
    FREE: t('home.share_free_space_stories'),
    NOTICE: t('home.check_important_notices'),
    STARLIGHT_CINEMA: t('home.cinema_auto_desc')
  };
  return descriptions[category] || '';
};

// 호환성을 위한 카테고리 라벨 (AdminUserPage.tsx에서 사용)
export const CATEGORY_LABELS: Record<string, string> = {
  NEWS: '뉴스',
  DISCUSSION: '토론',
  IMAGE: '사진',
  REVIEW: '후기',
  FREE: '자유',
  NOTICE: '공지',
  STARLIGHT_CINEMA: '별빛 시네마',
};

// 카테고리 관련 상수
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

export default function PostList() {
  // 상태 관리
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { t } = useTranslation();
  
  // 스크롤 헤더 숨김 상태
  const [isHeaderVisible, setIsHeaderVisible] = useState(true);
  const [lastScrollY, setLastScrollY] = useState(0);

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
  
  // 게시글 분류 (한 번의 순회로 최적화)
  const { hotPosts, normalPosts } = React.useMemo(() => {
    // 블라인드 필터링: 관리자가 아니면 블라인드된 게시글 제외
    const filteredPosts = isAdmin ? posts : posts.filter(post => !post.blinded);

    if (sort === 'popular') {
      return { hotPosts: [], normalPosts: filteredPosts.slice(0, 30) };
    }
    const hot: Post[] = [];
    const normal: Post[] = [];
    filteredPosts.forEach(post => {
      if (post.hot && hot.length < 4) {
        hot.push(post);
      } else if (!post.hot && normal.length < 25) {
        normal.push(post);
      }
    });
    return { hotPosts: hot, normalPosts: normal };
  }, [posts, sort, isAdmin]);
  
  // 권한 관련 변수
  const canWrite = user && (USER_WRITABLE_CATEGORIES.includes(category) || user.role === 'ADMIN');
  const isAdmin = user?.role === 'ADMIN';

  // 스크롤 헤더 숨김 효과
  useEffect(() => {
    const handleScroll = () => {
      const currentScrollY = window.scrollY;
      
      if (currentScrollY < 100) {
        setIsHeaderVisible(true);
      } else if (currentScrollY > lastScrollY && currentScrollY > 200) {
        setIsHeaderVisible(false);
      } else if (currentScrollY < lastScrollY) {
        setIsHeaderVisible(true);
      }
      
      setLastScrollY(currentScrollY);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, [lastScrollY]);

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

  // 해시 기반 자동 스크롤
  useEffect(() => {
    if (window.location.hash === '#posts-section') {
      setTimeout(() => {
        const element = document.getElementById('posts-section');
        if (element) {
          element.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      }, 100);
    }
  }, [posts]);

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
    const params = createUrlParams({ category: cat, page: '0' });
    navigate(`?${params.toString()}`, { replace: true });
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

  // 이미지 추출 함수
  const extractFirstImage = (content: string): string | null => {
    if (!content) return null;
    const imgMatches = [
      content.match(/<img[^>]+src=["']([^"']+)["'][^>]*>/i),
      content.match(/!\[.*?\]\(([^)]+)\)/),
      content.match(/https?:\/\/[^\s]+\.(jpg|jpeg|png|gif|webp)/i)
    ];
    
    for (const match of imgMatches) {
      if (match?.[1] && !match[1].includes('placeholder')) {
        return match[1].trim();
      }
    }
    return null;
  };

  // 이미지 로드 실패 상태 관리
  const [failedImages, setFailedImages] = useState<Set<number>>(new Set());

  // 공통 이미지 오류 처리 함수
  const handleImageError = (e: React.SyntheticEvent<HTMLImageElement>) => {
    e.currentTarget.style.display = 'none';
    const parent = e.currentTarget.parentElement;
    if (parent) {
      parent.innerHTML = '<div class="w-full h-full bg-gradient-to-br from-slate-700/50 to-purple-900/30 flex items-center justify-center"><span class="text-3xl sm:text-4xl">🌌</span></div>';
    }
  };

  // 통합된 게시글 렌더링 함수
  const renderPost = (post: Post, variant: 'hot-card' | 'normal-card' | 'image-card' | 'list-item', isHot = false) => {
    const isBlinded = post.blinded && !isAdmin;
    const displayTitle = isBlinded ? 
      (post.blindType === 'ADMIN_BLIND' ? '관리자 블라인드 처리됨' : '신고로 블라인드 처리됨') : 
      post.title;
    const displayContent = isBlinded ? '내용을 볼 수 없습니다.' : post.content;
    const displayWriter = isBlinded ? '***' : post.writer;
    const displayStats = isBlinded ? '*' : '';

    const commonProps = {
      key: post.id,
      className: `transition-all duration-300 transform hover:scale-[1.02] active:scale-[0.98] touch-manipulation ${
        variant === 'image-card' ? 'rounded-xl overflow-hidden' : 'rounded-xl p-4'
      } ${
        isBlinded ? 'opacity-80 border-red-500/50' : 
        isHot ? 'bg-gradient-to-br from-orange-600/20 to-red-600/20 border border-orange-500/30 hover:shadow-orange-500/25' :
        'bg-slate-800/50 border border-slate-600/30 hover:border-purple-500/40 hover:shadow-lg'
      }`
    };

    if (variant === 'image-card') {
      const imageUrl = isBlinded ? null : (post.thumbnailUrl || extractFirstImage(post.content));
      const hasImageFailed = failedImages.has(post.id);
      
      return (
        <div {...commonProps}>
          <Link to={`/posts/${post.id}`} className="block">
            <div className="relative aspect-square bg-slate-700/50">
              {isAdmin && (
                <div className="absolute top-2 left-2 z-20">
                  <input
                    type="checkbox"
                    checked={selectedPosts.includes(post.id)}
                    onChange={(e) => {
                      e.preventDefault();
                      e.stopPropagation();
                      handlePostSelect(post.id, e.target.checked);
                    }}
                    className="w-4 h-4 bg-white/90 rounded border-2 border-gray-400"
                    onClick={(e) => e.stopPropagation()}
                  />
                </div>
              )}
              
              {post.blinded && (
                <div className="absolute top-2 right-2 z-20">
                  <span className={`text-xs px-2 py-1 rounded-full font-bold ${
                    post.blindType === 'ADMIN_BLIND' 
                      ? 'bg-red-600/90 text-red-100 border border-red-400/50' 
                      : 'bg-yellow-600/90 text-yellow-100 border border-yellow-400/50'
                  }`}>
                    {post.blindType === 'ADMIN_BLIND' ? '관리자' : '신고'}
                  </span>
                </div>
              )}
              
              {imageUrl && !hasImageFailed ? (
                <img
                  src={imageUrl}
                  alt="별 사진"
                  className="w-full h-full object-cover"
                  loading="lazy"
                  onError={handleImageError}
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-slate-700/50 to-purple-900/30">
                  <span className="text-3xl">🌌</span>
                </div>
              )}
              
              {post.blinded && (
                <div className="absolute inset-0 bg-black/80 flex flex-col items-center justify-center z-10">
                  <span className="text-4xl mb-2">🔒</span>
                  <span className={`text-xs px-2 py-1 rounded font-bold ${
                    post.blindType === 'ADMIN_BLIND' 
                      ? 'bg-red-600/90 text-red-100' 
                      : 'bg-yellow-600/90 text-yellow-100'
                  }`}>
                    {post.blindType === 'ADMIN_BLIND' ? '관리자 블라인드' : '신고 블라인드'}
                  </span>
                </div>
              )}
            </div>
            
            <div className={`p-3 ${
              post.blinded 
                ? 'bg-slate-800/60 border-t border-red-500/30' 
                : 'bg-slate-800/80'
            }`}>
              <h4 className={`text-sm font-medium line-clamp-2 mb-2 ${
                post.blinded ? 'text-gray-300' : 'text-white'
              }`}>
                {displayTitle}
              </h4>
              
              <div className="flex justify-between items-center text-xs text-gray-400">
                <div className="flex items-center gap-1">
                  <span className={`rounded px-1 py-0.5 ${
                    post.blinded 
                      ? 'bg-red-700/50 border border-red-600/30' 
                      : 'bg-slate-700/50'
                  }`}>
                    {isBlinded ? '🔒' : '👤'}
                  </span>
                  <span className="truncate max-w-[60px]">{displayWriter}</span>
                </div>
                <div className="flex gap-2">
                  <span className={isBlinded ? 'text-gray-500' : ''}>
                    💬 {isBlinded ? displayStats : (post.commentCount || 0)}
                  </span>
                  <span className={isBlinded ? 'text-gray-500' : ''}>
                    ❤️ {isBlinded ? displayStats : post.likeCount}
                  </span>
                </div>
              </div>
            </div>
          </Link>
        </div>
      );
    }

    if (variant === 'list-item') {
      return (
        <div {...commonProps}>
          <div className="flex items-start gap-3">
            {isAdmin && (
              <input
                type="checkbox"
                checked={selectedPosts.includes(post.id)}
                onChange={(e) => handlePostSelect(post.id, e.target.checked)}
                className="w-4 h-4 mt-1 flex-shrink-0"
              />
            )}
            <div className="flex-1 min-w-0">
              <Link to={`/posts/${post.id}`} className="block">
                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start mb-2 gap-2">
                  <h4 className="text-base sm:text-lg font-semibold text-white flex-1 flex items-center gap-2 line-clamp-2">
                    {isHot && <span className="text-orange-400 flex-shrink-0">🔥</span>}
                    {post.dDay && <span className="bg-orange-500 text-white px-2 py-1 rounded text-xs flex-shrink-0">[{post.dDay}]</span>}
                    <span className="break-words">{displayTitle}</span>
                    {post.blinded && (
                      <span className={`text-xs px-2 py-1 rounded flex-shrink-0 ${
                        post.blindType === 'ADMIN_BLIND' 
                          ? 'bg-red-600/20 text-red-400 border border-red-500/30' 
                          : 'bg-yellow-600/20 text-yellow-400 border border-yellow-500/30'
                      }`}>
                        {post.blindType === 'ADMIN_BLIND' ? '관리자 블라인드' : '신고 블라인드'}
                      </span>
                    )}
                  </h4>
                  <div className="flex items-center gap-3 text-sm text-gray-400 flex-shrink-0">
                    {isHot && <span className="bg-orange-500/20 text-orange-300 px-2 py-1 rounded text-xs font-bold">HOT</span>}
                    <span className="flex items-center gap-1">💬 {isBlinded ? displayStats : (post.commentCount || 0)}</span>
                    <span className="flex items-center gap-1">❤️ {isBlinded ? displayStats : post.likeCount}</span>
                  </div>
                </div>
                {isHot && category !== 'IMAGE' && (
                  <p className="text-sm text-gray-300 mb-3 line-clamp-2 leading-relaxed">
                    {displayContent}
                  </p>
                )}
                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2 text-sm text-gray-400">
                  <span className="flex items-center gap-2">
                    <span className="bg-slate-700/50 rounded px-2 py-1 border border-slate-600/30">
                      {isBlinded ? '🔒' : '👤'}
                    </span>
                    <span className="truncate">{displayWriter}</span>
                  </span>
                  <span className="flex items-center gap-1 text-xs sm:text-sm">
                    📅 {isBlinded ? '****-**-**' : new Date(post.createdAt).toLocaleDateString()}
                  </span>
                </div>
              </Link>
            </div>
          </div>
        </div>
      );
    }

    // card variants (hot-card, normal-card)
    return (
      <div {...commonProps}>
        <Link to={`/posts/${post.id}`} className="block">
          <div className="flex items-start gap-3 mb-3">
            <div className={`flex-shrink-0 w-${isHot ? '10' : '8'} h-${isHot ? '10' : '8'} bg-gradient-to-br ${isHot ? 'from-orange-500/30 to-red-500/30' : 'from-purple-500/30 to-pink-500/30'} rounded-full flex items-center justify-center`}>
              <span className={`text-${isHot ? 'lg' : 'sm'}`}>{isHot ? '🔥' : (CATEGORY_ICONS[post.category] || '📝')}</span>
            </div>
            <div className="flex-1 min-w-0">
              <h4 className="text-sm sm:text-base font-semibold text-white line-clamp-2 leading-tight mb-2">
                {displayTitle}
              </h4>
              <p className={`text-xs sm:text-sm text-gray-300 ${isHot ? 'line-clamp-2 leading-relaxed' : 'line-clamp-1'}`}>
                {isHot ? displayContent : displayContent.substring(0, 60) + '...'}
              </p>
            </div>
          </div>
          <div className="flex justify-between items-center text-xs text-gray-400">
            <div className="flex items-center gap-2">
              <span className="bg-slate-700/50 rounded px-1.5 py-0.5">
                {isBlinded ? '🔒' : '👤'}
              </span>
              <span className="truncate max-w-[80px]">{displayWriter}</span>
            </div>
            <div className="flex items-center gap-3 flex-shrink-0">
              <span className="flex items-center gap-1">💬 {isBlinded ? displayStats : (post.commentCount || 0)}</span>
              <span className="flex items-center gap-1">❤️ {isBlinded ? displayStats : post.likeCount}</span>
            </div>
          </div>
        </Link>
      </div>
    );
  };

  // 게시글 목록 렌더링 함수
  const renderPostList = () => {
    if (loading) {
      return <p className="text-center text-gray-400">🌠 {t('home.loading')}</p>;
    }
    
    if (posts.length === 0) {
      return <p className="text-center text-gray-400">{t('home.no_posts')}</p>;
    }
    
    return (
      <>
        {/* 인기 게시글 */}
        {sort === 'recent' && hotPosts.length > 0 && (
          <>
            <h3 className="text-lg sm:text-2xl font-semibold mb-3 sm:mb-4 text-orange-400 flex items-center gap-2">
              <span className="text-xl sm:text-2xl">🔥</span> {t('home.hot_posts')}
            </h3>
            <div className="space-y-3 mb-6 sm:mb-8">
              {hotPosts.map((post) => renderPost(post, 'list-item', true))}
            </div>
          </>
        )}

        {/* 일반 게시글 */}
        <h3 className="text-lg sm:text-2xl font-semibold mb-3 sm:mb-4 text-white flex items-center gap-2">
          <span className="text-xl sm:text-2xl">📄</span>
          {sort === 'popular' ? t('home.posts_popular') : t('home.normal_posts')}
        </h3>
        
        {category === 'IMAGE' ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4 sm:gap-6">
            {normalPosts.map((post) => renderPost(post, 'image-card'))}
          </div>
        ) : (
          <div className="space-y-4">
            {normalPosts.map((post) => renderPost(post, 'list-item'))}
          </div>
        )}

        {/* 페이지네이션 - 터치 최적화 */}
        <div className="mt-8 flex justify-center items-center gap-4">
          {page > 0 && (
            <button
              onClick={() => handlePageChange(page - 1)}
              className="flex items-center justify-center w-12 h-12 bg-purple-600/80 hover:bg-purple-600 active:bg-purple-700 rounded-full text-white font-medium transition-all duration-200 shadow-lg hover:shadow-purple-500/25 transform hover:scale-105 active:scale-95"
            >
              ←
            </button>
          )}
          <span className="px-6 py-3 bg-slate-800/80 rounded-lg text-white font-medium border border-purple-500/30">
            {page + 1}
          </span>
          {posts.length >= 30 && (
            <button
              onClick={() => handlePageChange(page + 1)}
              className="flex items-center justify-center w-12 h-12 bg-purple-600/80 hover:bg-purple-600 active:bg-purple-700 rounded-full text-white font-medium transition-all duration-200 shadow-lg hover:shadow-purple-500/25 transform hover:scale-105 active:scale-95"
            >
              →
            </button>
          )}
        </div>
      </>
    );
  };

  
  // 카테고리 이동 모달 렌더링
  const renderCategoryMoveModal = () => {
    if (!showMoveModal) return null;
    
    return (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div className="bg-[#1f2336] p-6 rounded-xl max-w-md w-full mx-4">
          <h3 className="text-lg font-semibold mb-4">{t('home.move_category')}</h3>
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
              {t('home.cancel')}
            </button>
          </div>
        </div>
      </div>
    );
  };
  
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white overflow-x-hidden">
      {/* 헤더 섹션 - 스크롤 시 자동 숨김 */}
      <div className={`sticky top-0 z-40 transition-transform duration-300 ${isHeaderVisible ? 'translate-y-0' : '-translate-y-full'} relative overflow-hidden bg-gradient-to-r from-purple-900/90 to-pink-900/90 backdrop-blur-md border-b border-purple-500/20`}>
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/10 to-pink-600/10"></div>
        <div className="relative max-w-6xl mx-auto px-4 sm:px-6 py-4 sm:py-8">
          <div className="text-center">
            <div className="inline-flex items-center gap-2 sm:gap-3 mb-2 sm:mb-4">
              <div className="w-8 h-8 sm:w-12 sm:h-12 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-lg sm:text-2xl shadow-lg">
                <span style={{ fontFamily: 'Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif' }}>
                  {CATEGORY_ICONS[category]}
                </span>
              </div>
            </div>
            <h1 className="text-lg sm:text-3xl font-bold text-white mb-2 sm:mb-3">
              {getCategoryLabel(category, t)} {t('home.board')}
            </h1>
            <p className="text-sm sm:text-base text-gray-200 max-w-2xl mx-auto px-2 sm:px-4 hidden sm:block">
              {getCategoryDescription(category, t)}
            </p>
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-4 sm:py-8">

        {/* 카테고리 선택 - 모바일 최적화 */}
      <div className="text-center mb-6 sm:mb-10">
          <h2 className="text-xl sm:text-3xl font-bold mb-2 sm:mb-3 flex items-center justify-center gap-2">
            <span className="text-white text-xl sm:text-3xl animate-pulse">🚀</span>
            <span className="bg-gradient-to-r from-purple-400 via-pink-400 to-blue-400 bg-clip-text text-transparent">
              {t('home.explore_boards')}
            </span>
          </h2>
          <p className="text-gray-300 text-xs sm:text-sm mb-4 sm:mb-6 px-4">{t('home.explore_boards_desc')}</p>
          <div className="w-16 sm:w-24 h-1 bg-gradient-to-r from-purple-500 via-pink-500 to-blue-500 rounded-full mx-auto"></div>
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 xl:grid-cols-7 gap-3 sm:gap-4">
          <Link to="/posts?category=NEWS&sort=recent#posts-section" className="group">
            <div className="relative p-3 sm:p-4 bg-gradient-to-br from-blue-600/70 to-cyan-600/70 hover:from-blue-600/90 hover:to-cyan-600/90 active:from-blue-700/90 active:to-cyan-700/90 rounded-xl border border-blue-500/50 hover:border-blue-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-blue-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
              <div className="absolute top-1 right-1">
                <span className="bg-gradient-to-r from-blue-500 to-cyan-500 text-white text-xs px-1.5 py-0.5 rounded-full font-bold shadow-md">🤖</span>
              </div>
              <div className="w-8 h-8 mx-auto bg-gradient-to-br from-blue-500/30 to-cyan-500/30 rounded-full flex items-center justify-center mb-2">
                <div className="text-xl">🚀</div>
              </div>
              <div className="text-sm font-bold text-blue-100 mb-1 leading-tight">{t('home.space_news')}</div>
              <div className="text-xs text-blue-300 bg-blue-500/20 rounded-full py-1 px-2 inline-block">{t('home.news_auto')}</div>
            </div>
          </Link>
          <Link to="/posts?category=DISCUSSION&sort=recent#posts-section" className="group">
            <div className="relative p-3 sm:p-4 bg-gradient-to-br from-green-600/70 to-emerald-600/70 hover:from-green-600/90 hover:to-emerald-600/90 active:from-green-700/90 active:to-emerald-700/90 rounded-xl border border-green-500/50 hover:border-green-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-green-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
              <div className="absolute top-1 right-1">
                <span className="bg-gradient-to-r from-green-500 to-emerald-500 text-white text-xs px-1.5 py-0.5 rounded-full font-bold shadow-md">🤖</span>
              </div>
              <div className="w-8 h-8 mx-auto bg-gradient-to-br from-green-500/30 to-emerald-500/30 rounded-full flex items-center justify-center mb-2">
                <div className="text-xl">💬</div>
              </div>
              <div className="text-sm font-bold text-green-100 mb-1 leading-tight">{t('home.discussion')}</div>
              <div className="text-xs text-green-300 bg-green-500/20 rounded-full py-1 px-2 inline-block">{t('home.discussion_auto')}</div>
            </div>
          </Link>
          <Link to="/posts?category=IMAGE&sort=recent#posts-section" className="group">
            <div className="relative p-3 sm:p-4 bg-gradient-to-br from-purple-600/70 to-indigo-600/70 hover:from-purple-600/90 hover:to-indigo-600/90 active:from-purple-700/90 active:to-indigo-700/90 rounded-xl border border-purple-500/50 hover:border-purple-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-purple-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
              <div className="w-8 h-8 mx-auto bg-gradient-to-br from-purple-500/30 to-indigo-500/30 rounded-full flex items-center justify-center mb-2">
                <div className="text-xl">🌌</div>
              </div>
              <div className="text-sm font-bold text-purple-100 mb-1 leading-tight">{t('home.star_photo')}</div>
              <div className="text-xs text-purple-300 bg-purple-500/20 rounded-full py-1 px-2 inline-block">{t('home.gallery')}</div>
            </div>
          </Link>

          <Link to="/posts?category=REVIEW&sort=recent#posts-section" className="group">
            <div className="relative p-3 sm:p-4 bg-gradient-to-br from-yellow-600/70 to-orange-600/70 hover:from-yellow-600/90 hover:to-orange-600/90 active:from-yellow-700/90 active:to-orange-700/90 rounded-xl border border-yellow-500/50 hover:border-yellow-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-yellow-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
              <div className="w-8 h-8 mx-auto bg-gradient-to-br from-yellow-500/30 to-orange-500/30 rounded-full flex items-center justify-center mb-2">
                <div className="text-xl">⭐</div>
              </div>
              <div className="text-sm font-bold text-yellow-100 mb-1 leading-tight">{t('home.review')}</div>
              <div className="text-xs text-yellow-300 bg-yellow-500/20 rounded-full py-1 px-2 inline-block">{t('home.review_share')}</div>
            </div>
          </Link>
          <Link to="/posts?category=FREE&sort=recent#posts-section" className="group">
            <div className="relative p-3 sm:p-4 bg-gradient-to-br from-pink-600/70 to-rose-600/70 hover:from-pink-600/90 hover:to-rose-600/90 active:from-pink-700/90 active:to-rose-700/90 rounded-xl border border-pink-500/50 hover:border-pink-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-pink-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
              <div className="w-8 h-8 mx-auto bg-gradient-to-br from-pink-500/30 to-rose-500/30 rounded-full flex items-center justify-center mb-2">
                <div className="text-xl">🎈</div>
              </div>
              <div className="text-sm font-bold text-pink-100 mb-1 leading-tight">{t('home.free')}</div>
              <div className="text-xs text-pink-300 bg-pink-500/20 rounded-full py-1 px-2 inline-block">{t('home.free_chat')}</div>
            </div>
          </Link>
          <Link to="/posts?category=NOTICE&sort=recent#posts-section" className="group">
            <div className="relative p-3 sm:p-4 bg-gradient-to-br from-red-600/70 to-orange-600/70 hover:from-red-600/90 hover:to-orange-600/90 active:from-red-700/90 active:to-orange-700/90 rounded-xl border border-red-500/50 hover:border-red-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-red-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
              <div className="w-8 h-8 mx-auto bg-gradient-to-br from-red-500/30 to-orange-500/30 rounded-full flex items-center justify-center mb-2">
                <div className="text-xl">📢</div>
              </div>
              <div className="text-sm font-bold text-red-100 mb-1 leading-tight">{t('home.notice')}</div>
              <div className="text-xs text-red-300 bg-red-500/20 rounded-full py-1 px-2 inline-block">{t('home.important_notice')}</div>
            </div>
          </Link>
          <Link to="/posts?category=STARLIGHT_CINEMA&sort=recent#posts-section" className="group">
            <div className="relative p-3 sm:p-4 bg-gradient-to-br from-purple-600/70 to-pink-600/70 hover:from-purple-600/90 hover:to-pink-600/90 active:from-purple-700/90 active:to-pink-700/90 rounded-xl border border-purple-500/50 hover:border-purple-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-purple-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
              <div className="absolute top-1 right-1">
                <span className="bg-gradient-to-r from-purple-500 to-pink-500 text-white text-xs px-1.5 py-0.5 rounded-full font-bold shadow-md">🤖</span>
              </div>
              <div className="w-8 h-8 mx-auto bg-gradient-to-br from-purple-500/30 to-pink-500/30 rounded-full flex items-center justify-center mb-2">
                <div className="text-xl">🎬</div>
              </div>
              <div className="text-sm font-bold text-purple-100 mb-1 leading-tight">{t('home.star_cinema')}</div>
              <div className="text-xs text-purple-300 bg-purple-500/20 rounded-full py-1 px-2 inline-block">{t('home.video_curation')}</div>
            </div>
          </Link>
        </div>

        {/* 게시판별 이용안내 배너 */}
        {category === 'NEWS' && (
          <div className="mb-4 sm:mb-6 p-3 sm:p-4 bg-blue-900/30 rounded-lg border border-blue-600/30">
            <div className="flex items-center gap-2 sm:gap-3 mb-1 sm:mb-2">
              <span className="text-lg sm:text-2xl">🤖</span>
              <h3 className="text-sm sm:text-lg font-semibold text-blue-200">{t('home.auto_news_update')}</h3>
            </div>
            <p className="text-blue-200 text-xs sm:text-sm leading-relaxed">
              {t('home.news_bot_desc')}
              <br />
              <strong>[{t('home.target_sources')}]</strong><br />
              {t('home.newsdata_api')}<br />
              {t('home.keywords')}
              <br />
              {t('home.news_registration')}
            </p>
          </div>
        )}
        
        {category === 'IMAGE' && (
          <div className="mb-4 sm:mb-6 p-3 sm:p-4 bg-purple-900/30 rounded-lg border border-purple-600/30">
            <div className="flex items-center gap-2 sm:gap-3 mb-1 sm:mb-2">
              <span className="text-lg sm:text-2xl">🌌</span>
              <h3 className="text-sm sm:text-lg font-semibold text-purple-200">{t('home.star_photo')} {t('home.board_usage_guide')}</h3>
            </div>
            <p className="text-purple-200 text-xs sm:text-sm leading-relaxed">
              {t('home.image_board_desc')}
              <br />
              <strong>📷 {t('home.image_recommended')}</strong>
            </p>
          </div>
        )}
        
        {category === 'REVIEW' && (
          <div className="mb-4 sm:mb-6 p-3 sm:p-4 bg-yellow-900/30 rounded-lg border border-yellow-600/30">
            <div className="flex items-center gap-2 sm:gap-3 mb-1 sm:mb-2">
              <span className="text-lg sm:text-2xl">⭐</span>
              <h3 className="text-sm sm:text-lg font-semibold text-yellow-200">{t('home.review')} {t('home.board_usage_guide')}</h3>
            </div>
            <p className="text-yellow-200 text-xs sm:text-sm leading-relaxed">
              {t('home.review_board_desc')}
              <br />
              <strong>🎆 {t('home.review_recommended')}</strong>
            </p>
          </div>
        )}
        
        {category === 'FREE' && (
          <div className="mb-4 sm:mb-6 p-3 sm:p-4 bg-green-900/30 rounded-lg border border-green-600/30">
            <div className="flex items-center gap-2 sm:gap-3 mb-1 sm:mb-2">
              <span className="text-lg sm:text-2xl">🎈</span>
              <h3 className="text-sm sm:text-lg font-semibold text-green-200">{t('home.free')} {t('home.board_usage_guide')}</h3>
            </div>
            <p className="text-green-200 text-xs sm:text-sm leading-relaxed">
              {t('home.free_board_desc')}
              <br />
              <strong>🚀 {t('home.recommended_content')}</strong>
            </p>
          </div>
        )}
        
        {category === 'NOTICE' && (
          <div className="mb-4 sm:mb-6 p-3 sm:p-4 bg-red-900/30 rounded-lg border border-red-600/30">
            <div className="flex items-center gap-2 sm:gap-3 mb-1 sm:mb-2">
              <span className="text-lg sm:text-2xl">📢</span>
              <h3 className="text-sm sm:text-lg font-semibold text-red-200">{t('home.notice_board')} {t('home.board_usage_guide')}</h3>
            </div>
            <p className="text-red-200 text-xs sm:text-sm leading-relaxed">
              {t('home.notice_board_desc')}
              <br />
              <strong>⚠️ {t('home.notice_warning')}</strong>
            </p>
          </div>
        )}
        
        {category === 'STARLIGHT_CINEMA' && (
          <div className="mb-4 sm:mb-6 p-3 sm:p-4 bg-gradient-to-r from-purple-900/30 to-pink-900/30 rounded-lg border border-purple-600/30">
            <div className="flex items-center gap-2 sm:gap-3 mb-1 sm:mb-2">
              <span className="text-lg sm:text-2xl">🤖</span>
              <h3 className="text-sm sm:text-lg font-semibold text-purple-200">{t('home.ai_cinema_curation')}</h3>
            </div>
            <p className="text-purple-200 text-xs sm:text-sm leading-relaxed">
              {t('home.cinema_bot_desc')}
              <br />
              <strong>🎬 {t('home.curation_content')}</strong>
              <br />
              <strong>🤖 {t('home.ai_summary')}</strong>
              <br />
              <strong>⚠️ {t('home.cinema_warning')}</strong>
            </p>
          </div>
        )}
        
        {/* 토론 카테고리 - 오늘의 토론 주제 배너 */}
        {category === 'DISCUSSION' && (
          <React.Suspense fallback={null}>
            <DiscussionTopicBanner />
          </React.Suspense>
        )}
        
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
        


        {/* 검색 기능 - 컴팩트한 디자인 */}
        <div className="mb-4 sm:mb-6">
          <div className="bg-slate-800/50 rounded-xl p-3 sm:p-4 border border-slate-600/30">
            <form onSubmit={handleSearch}>
              <div className="flex gap-1 w-full overflow-hidden">
                <select
                  value={searchTypeInput}
                  onChange={(e) => setSearchTypeInput(e.target.value)}
                  className="bg-slate-700/50 text-white rounded-lg px-1 py-2 text-xs border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 transition-all duration-200 w-14 sm:w-auto sm:min-w-[80px] flex-shrink-0"
                >
                  <option value="title">제목</option>
                  <option value="content">내용</option>
                  <option value="titleAndContent">전체</option>
                  <option value="writer">작성자</option>
                </select>
                
                <input
                  type="text"
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  placeholder="검색"
                  className="flex-1 min-w-0 bg-slate-700/50 text-white rounded-lg px-2 py-2 text-sm border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 placeholder-gray-400 transition-all duration-200"
                />
                
                <button
                  type="submit"
                  className="px-2 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-sm transition-all duration-200 flex-shrink-0"
                >
                  🔍
                </button>
                
                {(searchKeyword || searchInput) && (
                  <button
                    type="button"
                    onClick={handleSearchReset}
                    className="px-2 py-2 bg-slate-600 hover:bg-slate-700 text-white rounded-lg text-sm transition-all duration-200 flex-shrink-0"
                  >
                    ✕
                  </button>
                )}
              </div>
            </form>
            
            {/* 검색 결과 표시 */}
            {searchKeyword && (
              <div className="mt-2 p-2 bg-purple-900/30 rounded-lg border border-purple-600/30">
                <p className="text-xs sm:text-sm text-purple-200 flex flex-wrap items-center gap-1">
                  <span className="text-purple-300">{t('home.search_result')}:</span> 
                  <span className="font-semibold text-white bg-purple-800/30 px-1.5 py-0.5 rounded text-xs">"{searchKeyword}"</span> 
                  <span className="text-purple-300 text-xs bg-purple-800/20 px-1.5 py-0.5 rounded-full">
                    {searchType === 'titleAndContent' ? '전체' : 
                     searchType === 'title' ? '제목' : 
                     searchType === 'content' ? '내용' : '작성자'}
                  </span>
                </p>
              </div>
            )}
          </div>
        </div>

        {/* 정렬 및 글쓰기 - 최적화된 레이아웃 */}
        <div className="bg-slate-800/30 rounded-xl p-3 sm:p-4 mb-4 sm:mb-6 border border-slate-600/30">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
            {/* 왼쪽: 정렬 및 관리자 기능 */}
            <div className="flex flex-wrap items-center gap-2 sm:gap-3">
              {/* 정렬 옵션 */}
              <div className="flex items-center gap-2">
                <span className="text-xs sm:text-sm text-gray-300">{t('home.sort')}:</span>
                <select
                  value={sort}
                  onChange={(e) => handleSortChange(e.target.value)}
                  className="bg-slate-700/50 text-white rounded-lg px-2 py-1.5 text-xs sm:text-sm border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 transition-all duration-200 min-w-0"
                >
                  <option value="recent">{t('home.recent')}</option>
                  <option value="popular">{t('home.popular')}</option>
                </select>
              </div>
              
              {/* 관리자 기능 */}
              {isAdmin && (
                <>
                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="selectAll"
                      checked={selectedPosts.length > 0 && selectedPosts.length === [...hotPosts, ...normalPosts].length}
                      onChange={(e) => handleSelectAll(e.target.checked)}
                      className="w-4 h-4"
                    />
                    <label htmlFor="selectAll" className="text-xs sm:text-sm text-gray-300">{t('home.select_all')}</label>
                  </div>
                  {selectedPosts.length > 0 && (
                    <button
                      onClick={() => setShowMoveModal(true)}
                      className="px-2 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs sm:text-sm rounded-lg transition-colors"
                    >
                      {t('home.move_category')} ({selectedPosts.length})
                    </button>
                  )}
                </>
              )}
            </div>
            
            {/* 오른쪽: 글쓰기 버튼 */}
            {canWrite && (
              <Link
                to={`/posts/write?fixedCategory=${category}`}
                className="w-full sm:w-auto px-3 py-2 bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white font-medium rounded-lg shadow-lg transition-all duration-200 text-center text-xs sm:text-sm"
              >
                ✍️ {getCategoryLabel(category, t)} {t('home.write_post')}
              </Link>
            )}
          </div>
        </div>



        {/* 게시글 목록 */}
        <div id="posts-section" className="space-y-4">
          {renderPostList()}
        </div>
        
        {/* 카테고리 이동 모달 */}
        {renderCategoryMoveModal()}
      </div>
    </div>
  );
}
