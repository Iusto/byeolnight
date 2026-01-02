import React, { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';
import { Post } from '../types/post';
import { CATEGORY_LABELS, CATEGORY_ICONS, USER_WRITABLE_CATEGORIES } from '../constants/postConstants';
import { getCategoryLabel, getCategoryDescription } from '../utils/postHelpers';
import CategoryGrid from '../components/post/CategoryGrid';
import CategoryBanner from '../components/post/CategoryBanner';
import PostSearchBar from '../components/post/PostSearchBar';
import PostPagination from '../components/post/PostPagination';
import PostImageCard from '../components/post/PostImageCard';
import PostListItem from '../components/post/PostListItem';
import AdminSection from '../components/post/AdminSection';

const DiscussionTopicBanner = React.lazy(() => import('../components/post/DiscussionTopicBanner'));

export { CATEGORY_LABELS };

export default function PostList() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { t } = useTranslation();
  
  const [isHeaderVisible, setIsHeaderVisible] = useState(true);
  const [lastScrollY, setLastScrollY] = useState(0);

  const category = searchParams.get('category') || 'NEWS';
  const sort = searchParams.get('sort') || 'recent';
  const page = Number(searchParams.get('page')) || 0;
  const searchType = searchParams.get('searchType') || 'title';
  const searchKeyword = searchParams.get('search') || '';
  
  const [selectedPosts, setSelectedPosts] = useState<number[]>([]);
  const [showMoveModal, setShowMoveModal] = useState(false);
  
  const isAdmin = user?.role === 'ADMIN';
  const canWrite = user && (USER_WRITABLE_CATEGORIES.includes(category) || user.role === 'ADMIN');
  
  const { hotPosts, normalPosts } = React.useMemo(() => {
    const filteredPosts = posts.filter(post => !post.blinded);

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
  }, [posts, sort, isAdmin, user]);

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
        setPosts([]);
      } finally {
        setLoading(false);
      }
    };

    fetchPosts();
  }, [category, sort, page, searchKeyword, searchType]);

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

  const handleSortChange = (s: string) => {
    const params = createUrlParams({ sort: s, page: '0' });
    navigate(`?${params.toString()}`, { replace: true });
  };

  const handlePageChange = (nextPage: number) => {
    const params = createUrlParams({ page: String(nextPage) });
    navigate(`?${params.toString()}`, { replace: true });
  };
  
  const handleSearch = (searchType: string, keyword: string) => {
    const params = createUrlParams({ page: '0', searchType, search: keyword });
    navigate(`?${params.toString()}`, { replace: true });
  };
  
  const handleSearchReset = () => {
    const params = createUrlParams({ page: '0', search: '' });
    navigate(`?${params.toString()}`, { replace: true });
  };

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

  const renderPostList = () => {
    if (loading) {
      return <p className="text-center text-gray-400">🌠 {t('home.loading')}</p>;
    }
    
    if (posts.length === 0) {
      return <p className="text-center text-gray-400">{t('home.no_posts')}</p>;
    }
    
    return (
      <>
        {sort === 'recent' && hotPosts.length > 0 && (
          <>
            <h3 className="text-lg sm:text-2xl font-semibold mb-3 sm:mb-4 text-orange-400 flex items-center gap-2">
              <span className="text-xl sm:text-2xl">🔥</span> {t('home.hot_posts')}
            </h3>
            <div className="space-y-3 mb-6 sm:mb-8">
              {hotPosts.map((post) => (
                <PostListItem 
                  key={post.id}
                  post={post} 
                  isHot={true}
                  isAdmin={isAdmin}
                  selectedPosts={selectedPosts}
                  onSelect={handlePostSelect}
                  showContent={category !== 'IMAGE'}
                />
              ))}
            </div>
          </>
        )}

        <h3 className="text-lg sm:text-2xl font-semibold mb-3 sm:mb-4 text-white flex items-center gap-2">
          <span className="text-xl sm:text-2xl">📄</span>
          {sort === 'popular' ? t('home.posts_popular') : t('home.normal_posts')}
        </h3>
        
        {category === 'IMAGE' ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4 sm:gap-6">
            {normalPosts.map((post) => (
              <PostImageCard 
                key={post.id}
                post={post}
                isAdmin={isAdmin}
                selectedPosts={selectedPosts}
                onSelect={handlePostSelect}
              />
            ))}
          </div>
        ) : (
          <div className="space-y-4">
            {normalPosts.map((post) => (
              <PostListItem 
                key={post.id}
                post={post}
                isAdmin={isAdmin}
                selectedPosts={selectedPosts}
                onSelect={handlePostSelect}
              />
            ))}
          </div>
        )}

        <PostPagination 
          currentPage={page}
          hasMore={posts.length >= 30}
          onPageChange={handlePageChange}
        />
      </>
    );
  };

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

        <CategoryGrid />
        <CategoryBanner category={category} />
        
        {category === 'DISCUSSION' && (
          <React.Suspense fallback={null}>
            <DiscussionTopicBanner />
          </React.Suspense>
        )}
        
        {isAdmin && <AdminSection category={category} />}
        
        <PostSearchBar 
          searchKeyword={searchKeyword}
          searchType={searchType}
          onSearch={handleSearch}
          onReset={handleSearchReset}
        />

        <div className="bg-slate-800/30 rounded-xl p-3 sm:p-4 mb-4 sm:mb-6 border border-slate-600/30">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
            <div className="flex flex-wrap items-center gap-2 sm:gap-3">
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

        <div id="posts-section" className="space-y-4">
          {renderPostList()}
        </div>
        
        {renderCategoryMoveModal()}
      </div>
    </div>
  );
}
