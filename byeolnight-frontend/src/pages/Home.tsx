import { useEffect, useState, useMemo } from 'react';
import axios from '../lib/axios';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';
import { ChatSidebar } from '../components/chat';
import { TodaySpaceCard } from '../components/ui';
import HeroSection from '../components/home/HeroSection';
import { BoardNavigation, Section, PostCard, LoadingSpinner, StarfieldBackground, BoardSection } from '../components/common';
import { extractFirstImage } from '../utils/formatters';
import type { Post } from '../types/post';

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



  // 관리자는 블라인드된 게시글도 볼 수 있음
  const isAdmin = user?.role === 'ADMIN';
  const filteredPosts = useMemo(() => posts.filter(post => isAdmin || !post.blinded), [posts, isAdmin]);
  const filteredStarPhotos = useMemo(() => starPhotos.filter(photo => isAdmin || !photo.blinded), [starPhotos, isAdmin]);
  const latestNews = useMemo(
    () => boardPosts.NEWS?.find(post => isAdmin || !post.blinded),
    [boardPosts.NEWS, isAdmin],
  );
  const latestCinema = useMemo(
    () => boardPosts.STARLIGHT_CINEMA?.find(post => isAdmin || !post.blinded),
    [boardPosts.STARLIGHT_CINEMA, isAdmin],
  );

  if (authLoading || !dataLoaded) {
    return <LoadingSpinner />;
  }

  return (
    <div className="min-h-screen bg-space-gradient text-white relative overflow-x-hidden">
      {/* 우주 배경 효과 */}
      <div className="fixed inset-0 pointer-events-none">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-purple-900/20 via-slate-900/40 to-slate-900 sm:from-purple-900/20 sm:via-slate-900/40 sm:to-slate-900"></div>
        <StarfieldBackground density={100} />
      </div>

      <div className="relative z-10">
        <HeroSection />
        
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-4 sm:py-12">
          <BoardNavigation />

          <div className="grid grid-cols-1 lg:grid-cols-5 gap-4 sm:gap-8">
            <div className="lg:col-span-3 space-y-4 sm:space-y-8">
              {/* 공지사항 */}
              <BoardSection
                title={t('home.notice_board')}
                icon="📢"
                link="/posts?category=NOTICE&sort=recent"
                posts={boardPosts.NOTICE || []}
                isAdmin={isAdmin}
                bgColor="bg-gradient-to-br from-emerald-900/30 to-green-900/30"
                borderColor="border-emerald-500/20"
                rowClass="bg-emerald-900/20 hover:bg-emerald-900/40 border border-emerald-700/20 hover:border-emerald-500/50"
                statColor="text-emerald-300"
                metaColor="text-emerald-200/70"
                badge={{ text: t('home.notice'), className: 'bg-gradient-to-r from-emerald-500 to-green-500 text-white px-3 py-1 rounded-full' }}
                showLikes={false}
              />



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
                      const imageUrl = extractFirstImage(photo.content);
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

              {/* 카테고리별 게시판 (색상/뱃지/안내배너만 다른 동일 구조) */}
              {([
                {
                  category: 'NEWS', title: t('home.space_news'), icon: '🚀',
                  bgColor: 'bg-gradient-to-br from-blue-900/30 to-cyan-900/30', borderColor: 'border-blue-500/30',
                  rowClass: 'bg-blue-900/20 hover:bg-blue-900/30', statColor: 'text-blue-300', metaColor: 'text-blue-200/70',
                  autoDescription: t('home.news_auto_desc'), autoDescClass: 'bg-blue-800/30 border-blue-600/30 text-blue-200',
                },
                {
                  category: 'REVIEW', title: t('home.review_board'), icon: '⭐',
                  bgColor: 'bg-gradient-to-br from-purple-900/30 to-pink-900/30', borderColor: 'border-purple-500/30',
                  rowClass: 'bg-purple-900/20 hover:bg-purple-900/30', statColor: 'text-purple-300', metaColor: 'text-purple-200/70',
                },
                {
                  category: 'DISCUSSION', title: t('home.discussion_board'), icon: '💬',
                  bgColor: 'bg-gradient-to-br from-green-900/30 to-teal-900/30', borderColor: 'border-green-500/30',
                  rowClass: 'bg-green-900/20 hover:bg-green-900/30', statColor: 'text-green-300', metaColor: 'text-green-200/70',
                  autoDescription: t('home.discussion_auto_desc'), autoDescClass: 'bg-green-800/30 border-green-600/30 text-green-200',
                },
                {
                  category: 'FREE', title: t('home.free_board'), icon: '🎈',
                  bgColor: 'bg-gradient-to-br from-pink-900/30 to-rose-900/30', borderColor: 'border-pink-500/30',
                  rowClass: 'bg-pink-900/20 hover:bg-pink-900/30', statColor: 'text-pink-300', metaColor: 'text-pink-200/70',
                },
                {
                  category: 'STARLIGHT_CINEMA', title: t('home.star_cinema'), icon: '🎬',
                  bgColor: 'bg-gradient-to-br from-purple-900/30 to-pink-900/30', borderColor: 'border-purple-500/30',
                  rowClass: 'bg-gradient-to-r from-purple-900/20 to-pink-900/20 hover:from-purple-900/30 hover:to-pink-900/30 border border-purple-700/20',
                  statColor: 'text-purple-300', metaColor: 'text-purple-200/70',
                  autoDescription: t('home.cinema_auto_desc'), autoDescClass: 'bg-gradient-to-r from-purple-800/30 to-pink-800/30 border-purple-600/30 text-purple-200',
                  badge: { text: '🎬 AI', className: 'bg-gradient-to-r from-purple-500 to-pink-500 text-white px-2 py-1 rounded' }, aiBot: true,
                },
              ] as Array<{
                category: string; title: string; icon: string;
                bgColor: string; borderColor: string; rowClass: string;
                statColor: string; metaColor: string;
                autoDescription?: string; autoDescClass?: string;
                badge?: { text: string; className: string }; aiBot?: boolean;
              }>).map((cfg) => (
                <BoardSection
                  key={cfg.category}
                  title={cfg.title}
                  icon={cfg.icon}
                  link={`/posts?category=${cfg.category}&sort=recent`}
                  posts={boardPosts[cfg.category] || []}
                  isAdmin={isAdmin}
                  bgColor={cfg.bgColor}
                  borderColor={cfg.borderColor}
                  rowClass={cfg.rowClass}
                  statColor={cfg.statColor}
                  metaColor={cfg.metaColor}
                  autoDescription={cfg.autoDescription}
                  autoDescClass={cfg.autoDescClass}
                  badge={cfg.badge}
                  aiBot={cfg.aiBot}
                />
              ))}
            </div>

            {/* 채팅 사이드바 */}
            <div className="lg:col-span-2 order-first lg:order-last">
              <div className="sticky top-4 space-y-6">
                {/* 오늘의 관측 환경과 우주 콘텐츠를 한 카드에서 제공 */}
                <TodaySpaceCard latestNews={latestNews} latestCinema={latestCinema} />
                
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
