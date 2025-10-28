import { useEffect, useState, useMemo } from 'react';
import axios from '../lib/axios';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';
import { ChatSidebar } from '../components/chat';
import { WeatherWidget } from '../components/ui';
import HeroSection from '../components/home/HeroSection';
import { BoardNavigation, Section, PostCard, LoadingSpinner } from '../components/common';
import { formatDate, extractFirstImage } from '../utils/formatters';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  writerId: number;
  likeCount: number;
  viewCount: number;
  commentCount: number;
  updatedAt: string;
  createdAt?: string;
  blinded: boolean;
  thumbnailUrl?: string;
  dDay?: string;
}

export default function Home() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [starPhotos, setStarPhotos] = useState<Post[]>([]);
  const [boardPosts, setBoardPosts] = useState<Record<string, Post[]>>({});
  const [dataLoaded, setDataLoaded] = useState(false);
  const { user, loading: authLoading } = useAuth();
  const { t } = useTranslation();



  // API 호출
  useEffect(() => {
    if (dataLoaded) return;
    
    let isMounted = true;
    
    const fetchData = async () => {
      try {
        const categories = ['NEWS', 'REVIEW', 'NOTICE', 'DISCUSSION', 'FREE', 'STARLIGHT_CINEMA'];
        
        const [hotRes, imageRes, ...boardResponses] = await Promise.all([
          axios.get('/public/posts/hot', { params: { size: 6 } }),
          axios.get('/public/posts', { params: { category: 'IMAGE', sort: 'recent', size: 8 } }),
          ...categories.map(category => 
            axios.get('/public/posts', { params: { category, sort: 'recent', size: 5 } })
              .catch(() => ({ data: { success: false, data: { content: [] } } }))
          )
        ]);

        if (!isMounted) return;

        const boardData: Record<string, Post[]> = {};
        boardResponses.forEach((res, index) => {
          boardData[categories[index]] = res.data?.success ? res.data.data?.content || [] : [];
        });
        
        setPosts(hotRes.data?.success ? hotRes.data.data || [] : []);
        setStarPhotos(imageRes.data?.success ? imageRes.data.data?.content || [] : []);
        setBoardPosts(boardData);
        
      } catch (error) {
        console.error('데이터 로딩 실패:', error);
        if (isMounted) {
          setPosts([]);
          setStarPhotos([]);
          setBoardPosts({});
        }
      } finally {
        if (isMounted) {
          setDataLoaded(true);
        }
      }
    };

    fetchData();
    
    return () => {
      isMounted = false;
    };
  }, [dataLoaded]);



  const filteredPosts = useMemo(() => posts.filter(post => !post.blinded), [posts]);
  const filteredStarPhotos = useMemo(() => starPhotos.filter(photo => !photo.blinded), [starPhotos]);

  if (authLoading || !dataLoaded) {
    return <LoadingSpinner />;
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white relative overflow-x-hidden">
      {/* 우주 배경 효과 */}
      <div className="fixed inset-0 pointer-events-none">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-purple-900/20 via-slate-900/40 to-slate-900 sm:from-purple-900/20 sm:via-slate-900/40 sm:to-slate-900"></div>
        {/* 별 효과 */}
        <div className="absolute inset-0">
          {[...Array(50)].map((_, i) => (
            <div
              key={i}
              className="absolute w-1 h-1 bg-white rounded-full animate-pulse"
              style={{
                left: `${Math.random() * 100}%`,
                top: `${Math.random() * 100}%`,
                animationDelay: `${Math.random() * 3}s`,
                animationDuration: `${2 + Math.random() * 2}s`
              }}
            />
          ))}
        </div>
        {/* 유성 효과 */}
        <div className="absolute top-1/4 left-1/4 w-2 h-2 bg-gradient-to-r from-blue-400 to-transparent rounded-full animate-ping opacity-30"></div>
        <div className="absolute top-3/4 right-1/3 w-1 h-1 bg-gradient-to-r from-purple-400 to-transparent rounded-full animate-ping opacity-40" style={{animationDelay: '1s'}}></div>
      </div>
      
      <div className="relative z-10">
        <HeroSection />
        
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-4 sm:py-12">
          <BoardNavigation />

          <div className="grid grid-cols-1 lg:grid-cols-5 gap-4 sm:gap-8">
            <div className="lg:col-span-3 space-y-4 sm:space-y-8">
              {/* 공지사항 */}
              <Section 
                title={t('home.notice_board')} 
                icon="📢" 
                link="/posts?category=NOTICE&sort=recent"
                bgColor="bg-gradient-to-br from-emerald-900/30 to-green-900/30"
                borderColor="border-emerald-500/20"
              >
                <div className="space-y-3">
                  {boardPosts.NOTICE?.filter(post => !post.blinded).map((post) => (
                    <div key={post.id} className="group bg-emerald-900/20 hover:bg-emerald-900/40 rounded-xl p-4 transition-all duration-300 border border-emerald-700/20 hover:border-emerald-500/50">
                      <Link to={`/posts/${post.id}`}>
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-3 flex-1 min-w-0">
                            <span className="bg-gradient-to-r from-emerald-500 to-green-500 text-white px-3 py-1 rounded-full text-xs font-bold flex-shrink-0">
                              {t('home.notice')}
                            </span>
                            <span className="font-bold text-white group-hover:text-emerald-100 transition-colors text-sm sm:text-base line-clamp-1 min-w-0" style={{textShadow: '0 2px 4px rgba(0,0,0,0.8)', filter: 'brightness(1.1)'}}>
                              {post.title}
                            </span>
                          </div>
                          <div className="flex items-center gap-2 text-emerald-300 text-sm">
                            <span>💬 {post.commentCount || 0}</span>
                            <span>👁 {post.viewCount}</span>
                          </div>
                        </div>
                        <div className="text-emerald-200/70 text-sm">
                          🖊 {post.writer} • 📅 {formatDate(post.createdAt || post.updatedAt)}
                        </div>
                      </Link>
                    </div>
                  ))}
                </div>
              </Section>



              {/* 인기 게시글 - 모바일 2열 그리드 */}
              <div className="bg-gradient-to-br from-slate-800/60 via-orange-900/40 to-red-900/50 backdrop-blur-md rounded-2xl p-4 sm:p-6 border border-orange-500/40 relative overflow-hidden group hover:shadow-2xl hover:shadow-orange-500/20 transition-all duration-500">
                {/* 배경 효과 */}
                <div className="absolute inset-0 bg-gradient-to-br from-orange-600/10 to-red-600/10 opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>
                <div className="absolute top-2 right-2 sm:top-4 sm:right-4 w-10 h-10 sm:w-20 sm:h-20 bg-gradient-to-br from-orange-500/20 to-red-500/20 rounded-full blur-xl animate-pulse"></div>
                
                <div className="flex items-center gap-3 sm:gap-4 mb-4 sm:mb-6 relative z-10">
                  <div className="relative">
                    <div className="w-8 h-8 sm:w-12 sm:h-12 bg-gradient-to-r from-orange-500 to-red-500 rounded-full flex items-center justify-center text-base sm:text-xl shadow-lg hover:shadow-orange-500/50 transition-all duration-300 animate-pulse">
                      🔥
                    </div>
                    <div className="absolute inset-0 bg-gradient-to-r from-orange-400 to-red-400 rounded-full blur opacity-40 animate-ping"></div>
                  </div>
                  <h2 className="text-lg sm:text-2xl font-bold bg-gradient-to-r from-orange-400 via-red-400 to-pink-400 bg-clip-text text-transparent">
                    {t('home.popular_posts')}
                  </h2>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-4 relative z-10">
                  {filteredPosts.map((post, index) => (
                    <div key={post.id} style={{animationDelay: `${index * 0.1}s`}} className="animate-fade-in-up">
                      <PostCard post={post} />
                    </div>
                  ))}
                </div>
              </div>



              {/* 별 사진 갤러리 */}
              <Section 
                title={t('home.night_sky_gallery')} 
                icon="🌌" 
                link="/posts?category=IMAGE&sort=recent"
                bgColor="bg-gradient-to-br from-indigo-900/30 to-purple-900/30"
                borderColor="border-indigo-500/30"
              >
                {filteredStarPhotos.length === 0 ? (
                  <div className="text-center py-6 sm:py-8">
                    <div className="text-4xl sm:text-6xl mb-2 sm:mb-4 opacity-50">🌌</div>
                    <p className="text-indigo-300 text-sm">{t('home.no_star_photos')}</p>
                  </div>
                ) : (
                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3 sm:gap-4">
                    {filteredStarPhotos.slice(0, 8).map((photo) => {
                      const imageUrl = photo.thumbnailUrl || extractFirstImage(photo.content);
                      return (
                        <Link to={`/posts/${photo.id}`} key={photo.id} className="block">
                          <div className="rounded-lg overflow-hidden shadow-lg hover:shadow-indigo-500/50 transition-all duration-300 group bg-indigo-900/20 relative aspect-square">
                            {imageUrl ? (
                              <img
                                src={imageUrl}
                                alt="별 사진"
                                className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300"
                                loading="lazy"
                                onError={(e) => {
                                  e.currentTarget.style.display = 'none';
                                  const parent = e.currentTarget.parentElement;
                                  if (parent) {
                                    parent.innerHTML = '<div class="w-full h-full bg-gradient-to-br from-indigo-800/50 to-purple-800/50 flex items-center justify-center"><span class="text-2xl sm:text-4xl opacity-50">🌌</span></div>';
                                  }
                                }}
                              />
                            ) : (
                              <div className="w-full h-full bg-gradient-to-br from-indigo-800/50 to-purple-800/50 flex items-center justify-center">
                                <span className="text-2xl sm:text-4xl opacity-50">🌌</span>
                              </div>
                            )}
                            <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-end">
                              <div className="p-2 sm:p-3 w-full">
                                <p className="text-white text-xs font-medium line-clamp-1">{photo.title}</p>
                                <p className="text-gray-300 text-xs">👁 {photo.viewCount} • ❤️ {photo.likeCount}</p>
                              </div>
                            </div>
                          </div>
                        </Link>
                      );
                    })}
                  </div>
                )}
              </Section>

              {/* 우주 뉴스 */}
              <Section 
                title={t('home.space_news')} 
                icon="🚀" 
                link="/posts?category=NEWS&sort=recent"
                bgColor="bg-gradient-to-br from-blue-900/30 to-cyan-900/30"
                borderColor="border-blue-500/30"
              >
                <div className="mb-3 p-2 bg-blue-800/30 rounded-lg border border-blue-600/30">
                  <p className="text-blue-200 text-xs flex items-center gap-2">
                    <span className="text-green-400">🤖</span>
                    <span>{t('home.news_auto_desc')}</span>
                  </p>
                </div>
                <div className="space-y-3">
                  {boardPosts.NEWS?.filter(post => !post.blinded).map((post) => (
                    <div key={post.id} className="bg-blue-900/20 rounded-lg p-4 hover:bg-blue-900/30 transition-colors">
                      <Link to={`/posts/${post.id}`}>
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-white text-sm sm:text-base line-clamp-1 flex-1 mr-3" style={{textShadow: '0 2px 4px rgba(0,0,0,0.8)', filter: 'brightness(1.1)'}}>{post.title}</span>
                          <div className="flex items-center gap-2 text-blue-300 text-sm">
                            <span>❤️ {post.likeCount}</span>
                            <span>💬 {post.commentCount || 0}</span>
                            <span>👁 {post.viewCount}</span>
                          </div>
                        </div>
                        <div className="text-blue-200/70 text-sm mt-1">
                          🖊 {post.writer} • 📅 {formatDate(post.createdAt || post.updatedAt)}
                        </div>
                      </Link>
                    </div>
                  ))}
                </div>
              </Section>

              {/* 리뷰 게시판 */}
              <Section 
                title={t('home.review_board')} 
                icon="⭐" 
                link="/posts?category=REVIEW&sort=recent"
                bgColor="bg-gradient-to-br from-purple-900/30 to-pink-900/30"
                borderColor="border-purple-500/30"
              >
                <div className="space-y-3">
                  {boardPosts.REVIEW?.filter(post => !post.blinded).map((post) => (
                    <div key={post.id} className="bg-purple-900/20 rounded-lg p-4 hover:bg-purple-900/30 transition-colors">
                      <Link to={`/posts/${post.id}`}>
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-white text-sm sm:text-base line-clamp-1 flex-1 mr-3" style={{textShadow: '0 2px 4px rgba(0,0,0,0.8)', filter: 'brightness(1.1)'}}>{post.title}</span>
                          <div className="flex items-center gap-2 text-purple-300 text-sm">
                            <span>❤️ {post.likeCount}</span>
                            <span>💬 {post.commentCount || 0}</span>
                            <span>👁 {post.viewCount}</span>
                          </div>
                        </div>
                        <div className="text-purple-200/70 text-sm mt-1">
                          🖊 {post.writer} • 📅 {formatDate(post.createdAt || post.updatedAt)}
                        </div>
                      </Link>
                    </div>
                  ))}
                </div>
              </Section>

              {/* 토론 게시판 */}
              <Section 
                title={t('home.discussion_board')} 
                icon="💬" 
                link="/posts?category=DISCUSSION&sort=recent"
                bgColor="bg-gradient-to-br from-green-900/30 to-teal-900/30"
                borderColor="border-green-500/30"
              >
                <div className="mb-3 p-2 bg-green-800/30 rounded-lg border border-green-600/30">
                  <p className="text-green-200 text-xs flex items-center gap-2">
                    <span className="text-green-400">🤖</span>
                    <span>{t('home.discussion_auto_desc')}</span>
                  </p>
                </div>
                <div className="space-y-3">
                  {boardPosts.DISCUSSION?.filter(post => !post.blinded).map((post) => (
                    <div key={post.id} className="bg-green-900/20 rounded-lg p-4 hover:bg-green-900/30 transition-colors">
                      <Link to={`/posts/${post.id}`}>
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-white text-sm sm:text-base line-clamp-1 flex-1 mr-3" style={{textShadow: '0 2px 4px rgba(0,0,0,0.8)', filter: 'brightness(1.1)'}}>{post.title}</span>
                          <div className="flex items-center gap-2 text-green-300 text-sm">
                            <span>❤️ {post.likeCount}</span>
                            <span>💬 {post.commentCount || 0}</span>
                            <span>👁 {post.viewCount}</span>
                          </div>
                        </div>
                        <div className="text-green-200/70 text-sm mt-1">
                          🖊 {post.writer} • 📅 {formatDate(post.createdAt || post.updatedAt)}
                        </div>
                      </Link>
                    </div>
                  ))}
                </div>
              </Section>

              {/* 자유 게시판 */}
              <Section 
                title={t('home.free_board')} 
                icon="🎈" 
                link="/posts?category=FREE&sort=recent"
                bgColor="bg-gradient-to-br from-pink-900/30 to-rose-900/30"
                borderColor="border-pink-500/30"
              >
                <div className="space-y-3">
                  {boardPosts.FREE?.filter(post => !post.blinded).map((post) => (
                    <div key={post.id} className="bg-pink-900/20 rounded-lg p-4 hover:bg-pink-900/30 transition-colors">
                      <Link to={`/posts/${post.id}`}>
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-white text-sm sm:text-base line-clamp-1 flex-1 mr-3" style={{textShadow: '0 2px 4px rgba(0,0,0,0.8)', filter: 'brightness(1.1)'}}>{post.title}</span>
                          <div className="flex items-center gap-2 text-pink-300 text-sm">
                            <span>❤️ {post.likeCount}</span>
                            <span>💬 {post.commentCount || 0}</span>
                            <span>👁 {post.viewCount}</span>
                          </div>
                        </div>
                        <div className="text-pink-200/70 text-sm mt-1">
                          🖊 {post.writer} • 📅 {formatDate(post.createdAt || post.updatedAt)}
                        </div>
                      </Link>
                    </div>
                  ))}
                </div>
              </Section>

              {/* 별빛 시네마 */}
              <Section 
                title={t('home.star_cinema')} 
                icon="🎬" 
                link="/posts?category=STARLIGHT_CINEMA&sort=recent"
                bgColor="bg-gradient-to-br from-purple-900/30 to-pink-900/30"
                borderColor="border-purple-500/30"
              >
                <div className="mb-3 p-3 bg-gradient-to-r from-purple-800/30 to-pink-800/30 rounded-lg border border-purple-600/30">
                  <p className="text-purple-200 text-xs flex items-center gap-2">
                    <span className="text-purple-400">🤖</span>
                    <span>{t('home.cinema_auto_desc')}</span>
                  </p>
                </div>
                <div className="space-y-3">
                  {boardPosts.STARLIGHT_CINEMA?.filter(post => !post.blinded).map((post) => (
                    <div key={post.id} className="bg-gradient-to-r from-purple-900/20 to-pink-900/20 rounded-lg p-4 hover:from-purple-900/30 hover:to-pink-900/30 transition-all duration-300 border border-purple-700/20">
                      <Link to={`/posts/${post.id}`}>
                        <div className="flex items-center justify-between">
                          <span className="font-semibold text-purple-100 flex items-center gap-2 flex-1 mr-3">
                            <span className="bg-gradient-to-r from-purple-500 to-pink-500 text-white px-2 py-1 rounded text-xs font-bold flex-shrink-0">🎬 AI</span>
                            <span className="text-sm sm:text-base line-clamp-1 font-bold text-white" style={{textShadow: '0 2px 4px rgba(0,0,0,0.8)', filter: 'brightness(1.1)'}}>{post.title}</span>
                          </span>
                          <div className="flex items-center gap-2 text-purple-300 text-sm">
                            <span>❤️ {post.likeCount}</span>
                            <span>💬 {post.commentCount || 0}</span>
                            <span>👁 {post.viewCount}</span>
                          </div>
                        </div>
                        <div className="text-purple-200/70 text-sm mt-1">
                          🤖 {post.writer} • 📅 {formatDate(post.createdAt || post.updatedAt)}
                        </div>
                      </Link>
                    </div>
                  ))}
                </div>
              </Section>
            </div>

            {/* 채팅 사이드바 */}
            <div className="lg:col-span-2 order-first lg:order-last">
              <div className="sticky top-4 space-y-6">
                {/* 날씨 위젯 */}
                <WeatherWidget />
                
                {/* 채팅 사이드바 */}
                <ChatSidebar />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}