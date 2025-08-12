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
  const { user } = useAuth();
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
    const fetchData = async () => {
      try {
        // 인기 게시글
        const hotRes = await axios.get('/public/posts/hot', { params: { size: 6 } });
        setPosts(hotRes.data?.success ? hotRes.data.data || [] : []);

        // 별 사진
        const imageRes = await axios.get('/public/posts', { 
          params: { category: 'IMAGE', sort: 'recent', size: 8 } 
        });
        setStarPhotos(imageRes.data?.success ? imageRes.data.data?.content || [] : []);

        // 각 게시판 데이터
        const boardData: Record<string, Post[]> = {};
        const categories = ['NEWS', 'REVIEW', 'NOTICE', 'DISCUSSION', 'FREE', 'STARLIGHT_CINEMA'];
        
        await Promise.all(categories.map(async (category) => {
          try {
            const res = await axios.get('/public/posts', { 
              params: { category, sort: 'recent', size: 5 } 
            });
            boardData[category] = res.data?.success ? res.data.data?.content || [] : [];
          } catch {
            boardData[category] = [];
          }
        }));
        
        setBoardPosts(boardData);
      } catch (error) {
        console.error('데이터 로딩 실패:', error);
      }
    };

    fetchData();
  }, []);

  // 컴포넌트들
  const HeroSection = () => (
    <div className="relative overflow-hidden bg-gradient-to-r from-purple-600/20 to-pink-600/20">
      <div className="max-w-7xl mx-auto px-4 py-20 text-center">
        <h1 className="text-5xl md:text-7xl font-bold mb-6">
          <span className="text-5xl mr-2">🌌</span>
          <span className="bg-gradient-to-r from-purple-400 via-pink-400 to-indigo-400 bg-clip-text text-transparent">
            {t('home.bigtitle')}
          </span>
        </h1>
        <p className="text-xl md:text-2xl text-gray-300 mb-8 max-w-3xl mx-auto">
          {t('home.subtitle')}
        </p>
        <div className="inline-flex items-center gap-3 bg-white/10 backdrop-blur-md px-6 py-3 rounded-full border border-white/20">
          <span className="text-purple-300">{user ? '👋' : '✨'}</span>
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
        <h2 className="text-3xl font-bold mb-3 flex items-center justify-center gap-2">
          <span className="text-3xl">🚀</span>
          <span className="bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
            {t('home.explore_boards')}
          </span>
        </h2>
        <p className="text-gray-400 text-sm">{t('home.explore_subtitle')}</p>
      </div>
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-8 gap-4">
        {BOARD_CONFIGS.map((board) => (
          <Link 
            key={board.key} 
            to={board.path || `/posts?category=${board.key}&sort=recent`} 
            className="group"
          >
            <div className={`relative p-4 bg-gradient-to-br from-${board.color}-600/20 to-${board.color}-600/20 hover:from-${board.color}-600/40 hover:to-${board.color}-600/40 rounded-xl border border-${board.color}-500/30 hover:border-${board.color}-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-${board.color}-500/25`}>
              {board.hasAI && (
                <div className="absolute top-2 right-2">
                  <span className={`bg-${board.color}-500 text-white text-xs px-2 py-1 rounded-full font-bold`}>
                    🤖 AI
                  </span>
                </div>
              )}
              <div className="text-3xl mb-2 group-hover:animate-bounce">
                {board.icon}
              </div>
              <div className={`text-sm font-medium text-${board.color}-100`}>
                {board.key === 'NEWS' ? t('home.space_news') : 
                 board.key === 'IMAGE' ? t('home.star_photo') :
                 board.key === 'STARLIGHT_CINEMA' ? t('home.star_cinema') :
                 t(`home.${board.key.toLowerCase()}`)}
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );

  const PostCard = ({ post, showStats = true }: { post: Post; showStats?: boolean }) => (
    <div className="group bg-white/5 hover:bg-white/10 rounded-xl p-4 transition-all duration-300 border border-white/10 hover:border-purple-400/50">
      <Link to={`/posts/${post.id}`}>
        <h3 className="font-semibold mb-2 group-hover:text-purple-300 transition-colors">
          {post.dDay && (
            <span className="bg-orange-500 text-white px-2 py-1 rounded text-xs mr-2">
              {post.dDay}
            </span>
          )}
          {post.title}
        </h3>
        {showStats && (
          <div className="flex items-center justify-between text-xs text-gray-500">
            <span>🖊 {post.writer}</span>
            <div className="flex items-center gap-3">
              <span>👁 {post.viewCount}</span>
              <span>💬 {post.commentCount || 0}</span>
              <span>❤️ {post.likeCount}</span>
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
    <div className={`${bgColor} ${borderColor} backdrop-blur-md rounded-2xl p-6 border shadow-lg hover:shadow-xl transition-all duration-300`}>
      <div className="flex justify-between items-center mb-6">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-2xl">
            {icon}
          </div>
          <h3 className="text-2xl font-bold bg-gradient-to-r from-white to-gray-300 bg-clip-text text-transparent">
            {title}
          </h3>
        </div>
        <Link 
          to={link} 
          className="group flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 text-white rounded-full text-sm font-medium transition-all duration-200 shadow-lg hover:shadow-xl"
        >
          {t('home.view_all')}
          <span className="group-hover:translate-x-1 transition-transform">→</span>
        </Link>
      </div>
      {children}
    </div>
  );

  const filteredPosts = useMemo(() => posts.filter(post => !post.blinded), [posts]);
  const filteredStarPhotos = useMemo(() => starPhotos.filter(photo => !photo.blinded), [starPhotos]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white">
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
            <div className="bg-gradient-to-br from-slate-800/50 to-purple-900/30 backdrop-blur-md rounded-2xl p-6 border border-purple-500/20">
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 bg-gradient-to-r from-orange-500 to-red-500 rounded-full flex items-center justify-center">
                  🔥
                </div>
                <h2 className="text-2xl font-bold bg-gradient-to-r from-orange-400 to-red-400 bg-clip-text text-transparent">
                  {t('home.popular_posts')}
                </h2>
              </div>
              <div className="grid md:grid-cols-2 gap-4">
                {filteredPosts.map((post) => (
                  <PostCard key={post.id} post={post} />
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
  );
}