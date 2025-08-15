import { useEffect, useState, useMemo } from 'react';
import axios from '../lib/axios';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import ChatSidebar from '../components/ChatSidebar';
import { useAuth } from '../contexts/AuthContext';

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

// 게시판 설정
const BOARD_CONFIGS = [
  { key: 'NEWS', icon: '🚀', color: 'blue', hasAI: true },
  { key: 'DISCUSSION', icon: '💬', color: 'green', hasAI: true },
  { key: 'IMAGE', icon: '🌌', color: 'purple' },
  { key: 'REVIEW', icon: '⭐', color: 'yellow' },
  { key: 'FREE', icon: '🎈', color: 'pink' },
  { key: 'NOTICE', icon: '📢', color: 'red' },
  { key: 'STARLIGHT_CINEMA', icon: '🎬', color: 'purple', hasAI: true },
  { key: 'SUGGESTIONS', icon: '💡', color: 'orange', path: '/suggestions' }
];

export default function Home() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [starPhotos, setStarPhotos] = useState<Post[]>([]);
  const [boardPosts, setBoardPosts] = useState<Record<string, Post[]>>({});
  const [dataLoaded, setDataLoaded] = useState(false);
  const { user, loading: authLoading } = useAuth();
  const { t } = useTranslation();

  // 날짜 포맷팅
  const formatDate = (dateStr: string) => {
    if (!dateStr) return '';
    try {
      return new Date(dateStr).toLocaleString();
    } catch {
      return dateStr;
    }
  };

  // 이미지 추출
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

  // 컴포넌트들
  const HeroSection = () => (
    <div className="relative overflow-hidden bg-gradient-to-r from-purple-600/20 via-indigo-600/20 to-pink-600/20">
      {/* 행성 배경 효과 */}
      <div className="absolute top-10 right-10 w-32 h-32 bg-gradient-to-br from-purple-500/30 to-pink-500/30 rounded-full blur-xl animate-pulse"></div>
      <div className="absolute bottom-10 left-10 w-24 h-24 bg-gradient-to-br from-blue-500/20 to-cyan-500/20 rounded-full blur-lg animate-pulse" style={{animationDelay: '1s'}}></div>
      
      <div className="max-w-7xl mx-auto px-4 py-12 sm:py-20 text-center relative">
        <div className="mb-6 sm:mb-8">
          <div className="inline-block animate-float">
            <span className="text-4xl sm:text-6xl md:text-8xl drop-shadow-2xl">🌌</span>
          </div>
        </div>
        <h1 className="text-3xl sm:text-5xl md:text-7xl font-bold mb-4 sm:mb-6 animate-fade-in px-2">
          <span className="bg-gradient-to-r from-purple-400 via-pink-400 to-indigo-400 bg-clip-text text-transparent drop-shadow-lg mobile-text">
            {t('home.bigtitle')}
          </span>
        </h1>
        <p className="text-lg sm:text-xl md:text-2xl text-gray-300 mobile-text-secondary mb-6 sm:mb-8 max-w-3xl mx-auto animate-fade-in-delay px-4">
          {t('home.subtitle')}
        </p>
        <div className="inline-flex items-center gap-3 bg-gradient-to-r from-white/10 to-white/5 backdrop-blur-md px-8 py-4 rounded-full border border-white/20 shadow-2xl hover:shadow-purple-500/25 transition-all duration-300 hover:scale-105">
          <span className="text-2xl animate-bounce">{user ? '👋' : '✨'}</span>
          <span className="text-white font-medium">
            {user ? `${user.nickname}${t('home.welcome')}` : t('home.login_prompt')}
          </span>
        </div>
      </div>
    </div>
  );

  const BoardNavigation = () => (
    <div className="mb-12">
      <div className="text-center mb-8">
        <div className="inline-block mb-4">
          <div className="relative">
            <span className="text-4xl animate-spin-slow">🚀</span>
            <div className="absolute -inset-2 bg-gradient-to-r from-purple-600 to-pink-600 rounded-full blur opacity-20 animate-pulse"></div>
          </div>
        </div>
        <h2 className="text-3xl font-bold mb-3">
          <span className="bg-gradient-to-r from-purple-400 via-pink-400 to-indigo-400 bg-clip-text text-transparent">
            {t('home.explore_boards')}
          </span>
        </h2>
        <p className="text-gray-400 text-sm">{t('home.explore_subtitle')}</p>
      </div>
      <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-2 sm:gap-4">
        {BOARD_CONFIGS.map((board, index) => (
          <Link 
            key={board.key} 
            to={board.path || `/posts?category=${board.key}&sort=recent`} 
            className="group touch-target"
          >
            <div 
              className={`relative mobile-card p-2 sm:p-3 bg-gradient-to-br from-${board.color}-500/40 to-${board.color}-600/40 active:from-${board.color}-500/60 active:to-${board.color}-600/60 mouse:hover:from-${board.color}-600/40 mouse:hover:to-${board.color}-600/40 rounded-lg sm:rounded-xl border border-${board.color}-400/50 active:border-${board.color}-300/70 mouse:hover:border-${board.color}-400/50 transition-all duration-300 text-center transform active:scale-95 mouse:hover:scale-105 mouse:hover:-translate-y-1 shadow-lg mouse:hover:shadow-${board.color}-500/25 backdrop-blur-sm min-h-[70px] sm:min-h-[90px] flex flex-col justify-center`}
              style={{animationDelay: `${index * 0.1}s`}}
            >
              {board.hasAI && (
                <div className="absolute -top-2 -right-2">
                  <div className="relative">
                    <span className={`bg-gradient-to-r from-${board.color}-500 to-${board.color}-600 text-white text-xs px-2 py-1 rounded-full font-bold shadow-lg animate-pulse`}>
                      🤖 AI
                    </span>
                    <div className={`absolute inset-0 bg-gradient-to-r from-${board.color}-400 to-${board.color}-500 rounded-full blur opacity-50 animate-ping`}></div>
                  </div>
                </div>
              )}
              <div className="text-xl sm:text-2xl mb-1 sm:mb-2 group-hover:animate-bounce group-hover:scale-110 transition-transform duration-300">
                {board.icon}
              </div>
              <div className={`text-xs sm:text-sm font-medium text-${board.color}-100 group-hover:text-white transition-colors leading-tight`}>
                {board.key === 'NEWS' ? t('home.space_news') : 
                 board.key === 'IMAGE' ? t('home.star_photo') :
                 board.key === 'STARLIGHT_CINEMA' ? t('home.star_cinema') :
                 t(`home.${board.key.toLowerCase()}`)}
              </div>
              {/* 호버 시 글로우 효과 */}
              <div className={`absolute inset-0 bg-gradient-to-br from-${board.color}-400/0 to-${board.color}-600/0 group-hover:from-${board.color}-400/10 group-hover:to-${board.color}-600/10 rounded-xl transition-all duration-300`}></div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );

  const PostCard = ({ post, showStats = true }: { post: Post; showStats?: boolean }) => (
    <div className="group mobile-post-card bg-gradient-to-br from-white/10 to-white/20 hover:from-white/15 hover:to-white/25 rounded-xl p-3 sm:p-4 transition-all duration-300 border border-white/20 hover:border-purple-400/50 backdrop-blur-sm hover:shadow-lg hover:shadow-purple-500/20 hover:-translate-y-1">
      <Link to={`/posts/${post.id}`}>
        <h3 className="font-semibold mb-2 group-hover:text-purple-300 transition-colors line-clamp-2">
          {post.dDay && (
            <span className="bg-gradient-to-r from-orange-500 to-red-500 text-white px-2 py-1 rounded-full text-xs mr-2 shadow-lg animate-pulse">
              {post.dDay}
            </span>
          )}
          {post.title}
        </h3>
        {showStats && (
          <div className="flex items-center justify-between text-xs text-gray-400 group-hover:text-gray-300 transition-colors">
            <span className="flex items-center gap-1">
              <span className="text-purple-400">🖊</span> 
              <span>{post.writer}</span>
            </span>
            <div className="flex items-center gap-3">
              <span className="flex items-center gap-1 hover:text-blue-400 transition-colors">
                <span>👁</span> {post.viewCount}
              </span>
              <span className="flex items-center gap-1 hover:text-green-400 transition-colors">
                <span>💬</span> {post.commentCount || 0}
              </span>
              <span className="flex items-center gap-1 hover:text-red-400 transition-colors">
                <span>❤️</span> {post.likeCount}
              </span>
            </div>
          </div>
        )}
      </Link>
    </div>
  );

  const Section = ({ title, icon, link, bgColor, borderColor, children }: {
    title: string;
    icon: string;
    link: string;
    bgColor: string;
    borderColor: string;
    children: React.ReactNode;
  }) => (
    <div className={`${bgColor.replace('/30', '/50').replace('/20', '/40')} ${borderColor.replace('/20', '/40')} backdrop-blur-md rounded-2xl p-4 sm:p-6 border shadow-lg hover:shadow-xl transition-all duration-300 relative overflow-hidden group mobile-card`}>
      {/* 섹션 배경 효과 */}
      <div className="absolute inset-0 bg-gradient-to-br from-white/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>
      
      <div className="flex justify-between items-center mb-4 sm:mb-6 relative z-10">
        <div className="flex items-center gap-2 sm:gap-4">
          <div className="relative">
            <div className="w-8 h-8 sm:w-12 sm:h-12 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-lg sm:text-2xl shadow-lg hover:shadow-purple-500/50 transition-all duration-300 hover:scale-110">
              {icon}
            </div>
            <div className="absolute inset-0 bg-gradient-to-r from-purple-400 to-pink-400 rounded-full blur opacity-30 animate-pulse"></div>
          </div>
          <h3 className="text-lg sm:text-2xl font-bold bg-gradient-to-r from-white via-purple-200 to-pink-200 bg-clip-text text-transparent mobile-text">
            {title}
          </h3>
        </div>
        <Link 
          to={link} 
          className="group/btn flex items-center gap-1 sm:gap-2 px-3 py-2 sm:px-6 sm:py-3 bg-gradient-to-r from-purple-600 via-purple-700 to-pink-600 hover:from-purple-700 hover:via-purple-800 hover:to-pink-700 text-white rounded-full text-xs sm:text-sm font-medium transition-all duration-300 shadow-lg hover:shadow-purple-500/50 hover:scale-105 backdrop-blur-sm border border-purple-400/30 mobile-button touch-target"
        >
          <span className="hidden sm:inline">{t('home.view_all')}</span>
          <span className="sm:hidden">더보기</span>
          <span className="group-hover/btn:translate-x-1 group-hover/btn:scale-110 transition-all duration-200 text-xs sm:text-sm">🚀</span>
        </Link>
      </div>
      <div className="relative z-10">
        {children}
      </div>
    </div>
  );

  const filteredPosts = useMemo(() => posts.filter(post => !post.blinded), [posts]);
  const filteredStarPhotos = useMemo(() => starPhotos.filter(photo => !photo.blinded), [starPhotos]);

  if (authLoading || !dataLoaded) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white flex items-center justify-center">
        <div className="text-center">
          <div className="text-6xl mb-4 animate-spin-slow">🌌</div>
          <p className="text-xl text-purple-300">우주를 탐험하는 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen min-h-screen-safe bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 sm:from-slate-900 sm:via-purple-900 sm:to-slate-900 mobile-bright text-white relative overflow-hidden mobile-optimized">
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
        
        <div className="max-w-7xl mx-auto px-4 py-12">
          <BoardNavigation />

          <div className="grid lg:grid-cols-5 gap-8">
            <div className="lg:col-span-3 space-y-8">
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
                          <div className="flex items-center gap-3">
                            <span className="bg-gradient-to-r from-emerald-500 to-green-500 text-white px-3 py-1 rounded-full text-xs font-bold">
                              {t('home.notice')}
                            </span>
                            <span className="font-semibold text-emerald-100 group-hover:text-white transition-colors">
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

              {/* 인기 게시글 */}
              <div className="mobile-section bg-gradient-to-br from-slate-800/60 via-orange-900/40 to-red-900/50 backdrop-blur-md rounded-2xl p-4 sm:p-6 border border-orange-500/40 relative overflow-hidden group hover:shadow-2xl hover:shadow-orange-500/20 transition-all duration-500">
                {/* 배경 효과 */}
                <div className="absolute inset-0 bg-gradient-to-br from-orange-600/10 to-red-600/10 opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>
                <div className="absolute top-4 right-4 w-20 h-20 bg-gradient-to-br from-orange-500/20 to-red-500/20 rounded-full blur-xl animate-pulse"></div>
                
                <div className="flex items-center gap-3 mb-6 relative z-10">
                  <div className="relative">
                    <div className="w-12 h-12 bg-gradient-to-r from-orange-500 to-red-500 rounded-full flex items-center justify-center text-xl shadow-lg hover:shadow-orange-500/50 transition-all duration-300 animate-pulse">
                      🔥
                    </div>
                    <div className="absolute inset-0 bg-gradient-to-r from-orange-400 to-red-400 rounded-full blur opacity-40 animate-ping"></div>
                  </div>
                  <h2 className="text-2xl font-bold bg-gradient-to-r from-orange-400 via-red-400 to-pink-400 bg-clip-text text-transparent">
                    {t('home.popular_posts')}
                  </h2>
                </div>
                <div className="grid md:grid-cols-2 gap-4 relative z-10">
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
                  <div className="text-center py-8">
                    <div className="text-6xl mb-4 opacity-50">🌌</div>
                    <p className="text-indigo-300">{t('home.no_star_photos')}</p>
                  </div>
                ) : (
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    {filteredStarPhotos.slice(0, 8).map((photo) => {
                      const imageUrl = photo.thumbnailUrl || extractFirstImage(photo.content);
                      return (
                        <Link to={`/posts/${photo.id}`} key={photo.id}>
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
                                    parent.innerHTML = '<div class="w-full h-full bg-gradient-to-br from-indigo-800/50 to-purple-800/50 flex items-center justify-center"><span class="text-4xl opacity-50">🌌</span></div>';
                                  }
                                }}
                              />
                            ) : (
                              <div className="w-full h-full bg-gradient-to-br from-indigo-800/50 to-purple-800/50 flex items-center justify-center">
                                <span className="text-4xl opacity-50">🌌</span>
                              </div>
                            )}
                            <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-end">
                              <div className="p-3 w-full">
                                <p className="text-white text-sm font-medium truncate">{photo.title}</p>
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
                          <span className="font-semibold text-blue-100">{post.title}</span>
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
                          <span className="font-semibold text-purple-100">{post.title}</span>
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
                          <span className="font-semibold text-green-100">{post.title}</span>
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
                          <span className="font-semibold text-pink-100">{post.title}</span>
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
                          <span className="font-semibold text-purple-100 flex items-center gap-2">
                            <span className="bg-gradient-to-r from-purple-500 to-pink-500 text-white px-2 py-1 rounded text-xs font-bold">🎬 AI</span>
                            {post.title}
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
            <div className="lg:col-span-2">
              <div className="sticky top-4">
                <ChatSidebar />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}