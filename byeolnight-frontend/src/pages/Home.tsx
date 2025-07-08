import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { Link } from 'react-router-dom';
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
  blinded: boolean;
  thumbnailUrl?: string;
  dDay?: string;
}

export default function Home() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [starPhotos, setStarPhotos] = useState<Post[]>([]);

  const [newsPosts, setNewsPosts] = useState<Post[]>([]);
  const [reviewPosts, setReviewPosts] = useState<Post[]>([]);
  const [noticePosts, setNoticePosts] = useState<Post[]>([]);
  const [discussionPosts, setDiscussionPosts] = useState<Post[]>([]);
  const [freePosts, setFreePosts] = useState<Post[]>([]);
  const [cinemaPosts, setCinemaPosts] = useState<Post[]>([]);
  const { user } = useAuth();

  useEffect(() => {
    // 인기 게시글 로딩
    axios.get('/public/posts/hot', { params: { size: 6 } })
      .then(res => {
        console.log('인기 게시글 응답:', res.data);
        const data = res.data?.success ? res.data.data : [];
        setPosts(Array.isArray(data) ? data : []);
      })
      .catch(err => {
        console.error('인기 게시글 불러오기 실패', err);
        setPosts([]);
      });

    // 별 사진 게시판
    axios.get('/public/posts', { params: { category: 'IMAGE', sort: 'popular', size: 4 } })
      .then(res => {
        console.log('별 사진 응답:', res.data);
        const content = res.data?.success ? res.data.data?.content || [] : [];
        setStarPhotos(Array.isArray(content) ? content : []);
      })
      .catch(err => {
        console.error('별 사진 게시판 불러오기 실패', err);
        setStarPhotos([]);
      });



    // 우주 뉴스
    axios.get('/public/posts', { params: { category: 'NEWS', sort: 'recent', size: 5 } })
      .then(res => {
        const content = res.data?.success ? res.data.data?.content || [] : [];
        setNewsPosts(Array.isArray(content) ? content : []);
      })
      .catch(err => {
        console.error('우주 뉴스 게시판 불러오기 실패', err);
        setNewsPosts([]);
      });

    // 리뷰 게시판
    axios.get('/public/posts', { params: { category: 'REVIEW', sort: 'recent', size: 5 } })
      .then(res => {
        const content = res.data?.success ? res.data.data?.content || [] : [];
        setReviewPosts(Array.isArray(content) ? content : []);
      })
      .catch(err => {
        console.error('리뷰 게시판 불러오기 실패', err);
        setReviewPosts([]);
      });

    // 공지사항
    axios.get('/public/posts', { params: { category: 'NOTICE', sort: 'recent', size: 5 } })
      .then(res => {
        const content = res.data?.success ? res.data.data?.content || [] : [];
        setNoticePosts(Array.isArray(content) ? content : []);
      })
      .catch(err => {
        console.error('공지사항 불러오기 실패', err);
        setNoticePosts([]);
      });

    // 토론 게시판
    axios.get('/public/posts', { params: { category: 'DISCUSSION', sort: 'recent', size: 5 } })
      .then(res => {
        const content = res.data?.success ? res.data.data?.content || [] : [];
        setDiscussionPosts(Array.isArray(content) ? content : []);
      })
      .catch(err => {
        console.error('토론 게시판 불러오기 실패', err);
        setDiscussionPosts([]);
      });

    // 자유 게시판
    axios.get('/public/posts', { params: { category: 'FREE', sort: 'recent', size: 5 } })
      .then(res => {
        const content = res.data?.success ? res.data.data?.content || [] : [];
        setFreePosts(Array.isArray(content) ? content : []);
      })
      .catch(err => {
        console.error('자유 게시판 불러오기 실패', err);
        setFreePosts([]);
      });

    // 별빛 시네마
    axios.get('/public/posts', { params: { category: 'STARLIGHT_CINEMA', sort: 'recent', size: 5 } })
      .then(res => {
        const content = res.data?.success ? res.data.data?.content || [] : [];
        setCinemaPosts(Array.isArray(content) ? content : []);
      })
      .catch(err => {
        console.error('별빛 시네마 게시판 불러오기 실패', err);
        setCinemaPosts([]);
      });
  }, []);

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return `${date.getFullYear()}.${(date.getMonth() + 1).toString().padStart(2, '0')}.${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
  };

  const Section = ({
    title,
    icon,
    link,
    bgColor,
    borderColor,
    children,
  }: {
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
          전체보기
          <span className="group-hover:translate-x-1 transition-transform">→</span>
        </Link>
      </div>
      <div>
        {children}
      </div>
    </div>
  );

  const PostListItem = (post: Post, showLike = true) => {
    // 블라인드된 게시글은 표시하지 않음
    if (post.blinded) {
      return null;
    }
    
    return (
      <li key={post.id} className="text-[15px] flex items-center justify-between border-b border-gray-700 py-2 hover:text-purple-300 transition">
        <Link to={`/posts/${post.id}`} className="flex flex-col w-full">
          <span className="font-semibold truncate">
            {post.dDay && <span className="text-orange-300 text-sm mr-2">[{post.dDay}]</span>}
            {post.title}
          </span>
          <div className="flex items-center text-[13px] text-gray-400 gap-2 mt-1">
            🖊 {post.writer}
            <span>📅 {formatDate(post.updatedAt)}</span>
            <span>👁 {post.viewCount}</span>
            <span>💬 {post.commentCount || 0}</span>
            {showLike && <span>❤️ {post.likeCount}</span>}
          </div>
        </Link>
      </li>
    );
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white">
      {/* 히어로 섹션 */}
      <div className="relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/20 to-pink-600/20"></div>
        <div className="relative max-w-7xl mx-auto px-4 py-20">
          <div className="text-center">
            <h1 className="text-5xl md:text-7xl font-bold bg-gradient-to-r from-purple-400 via-pink-400 to-indigo-400 bg-clip-text text-transparent mb-6">
              🌌 별 헤는 밤
            </h1>
            <p className="text-xl md:text-2xl text-gray-300 mb-8 max-w-3xl mx-auto">
              우주의 신비를 탐험하고, 지식을 나누며, 꿈을 향해 나아가는 커뮤니티
            </p>
            {user ? (
              <div className="inline-flex items-center gap-3 bg-white/10 backdrop-blur-md px-6 py-3 rounded-full border border-white/20">
                <span className="text-purple-300">👋</span>
                <span className="text-white font-medium">{user.nickname}님, 환영합니다!</span>
              </div>
            ) : (
              <div className="inline-flex items-center gap-3 bg-white/10 backdrop-blur-md px-6 py-3 rounded-full border border-white/20">
                <span className="text-purple-400">✨</span>
                <span className="text-gray-300">로그인하여 더 많은 기능을 이용해보세요</span>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-12">
        {/* 게시판 네비게이션 */}
        <div className="mb-12">
          <div className="text-center mb-8">
            <h2 className="text-3xl font-bold mb-3 bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
              🚀 게시판 둘러보기
            </h2>
            <p className="text-gray-400 text-sm">다양한 주제의 게시판에서 우주의 신비를 탐험해보세요</p>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-7 gap-4">
            <Link to="/posts?category=NEWS&sort=recent" className="group">
              <div className="relative p-4 bg-gradient-to-br from-blue-600/20 to-cyan-600/20 hover:from-blue-600/40 hover:to-cyan-600/40 rounded-xl border border-blue-500/30 hover:border-blue-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-blue-500/25">
                <div className="absolute top-2 right-2">
                  <span className="bg-blue-500 text-white text-xs px-2 py-1 rounded-full font-bold">🤖 AI</span>
                </div>
                <div className="text-3xl mb-2 group-hover:animate-bounce">🚀</div>
                <div className="text-sm font-medium text-blue-100">우주 뉴스</div>
                <div className="text-xs text-blue-300 mt-1">자동 수집</div>
              </div>
            </Link>
            <Link to="/posts?category=DISCUSSION&sort=recent" className="group">
              <div className="relative p-4 bg-gradient-to-br from-green-600/20 to-emerald-600/20 hover:from-green-600/40 hover:to-emerald-600/40 rounded-xl border border-green-500/30 hover:border-green-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-green-500/25">
                <div className="absolute top-2 right-2">
                  <span className="bg-green-500 text-white text-xs px-2 py-1 rounded-full font-bold">🤖 AI</span>
                </div>
                <div className="text-3xl mb-2 group-hover:animate-pulse">💬</div>
                <div className="text-sm font-medium text-green-100">토론</div>
                <div className="text-xs text-green-300 mt-1">주제 생성</div>
              </div>
            </Link>
            <Link to="/posts?category=IMAGE&sort=recent" className="group">
              <div className="p-4 bg-gradient-to-br from-purple-600/20 to-indigo-600/20 hover:from-purple-600/40 hover:to-indigo-600/40 rounded-xl border border-purple-500/30 hover:border-purple-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-purple-500/25">
                <div className="text-3xl mb-2 group-hover:animate-spin">🌌</div>
                <div className="text-sm font-medium text-purple-100">별 사진</div>
                <div className="text-xs text-purple-300 mt-1">갤러리</div>
              </div>
            </Link>

            <Link to="/posts?category=REVIEW&sort=recent" className="group">
              <div className="p-4 bg-gradient-to-br from-yellow-600/20 to-orange-600/20 hover:from-yellow-600/40 hover:to-orange-600/40 rounded-xl border border-yellow-500/30 hover:border-yellow-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-yellow-500/25">
                <div className="text-3xl mb-2 group-hover:animate-pulse">⭐</div>
                <div className="text-sm font-medium text-yellow-100">후기</div>
                <div className="text-xs text-yellow-300 mt-1">리뷰 공유</div>
              </div>
            </Link>
            <Link to="/posts?category=FREE&sort=recent" className="group">
              <div className="p-4 bg-gradient-to-br from-pink-600/20 to-rose-600/20 hover:from-pink-600/40 hover:to-rose-600/40 rounded-xl border border-pink-500/30 hover:border-pink-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-pink-500/25">
                <div className="text-3xl mb-2 group-hover:animate-bounce">🎈</div>
                <div className="text-sm font-medium text-pink-100">자유</div>
                <div className="text-xs text-pink-300 mt-1">자유 소통</div>
              </div>
            </Link>
            <Link to="/posts?category=NOTICE&sort=recent" className="group">
              <div className="p-4 bg-gradient-to-br from-red-600/20 to-orange-600/20 hover:from-red-600/40 hover:to-orange-600/40 rounded-xl border border-red-500/30 hover:border-red-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-red-500/25">
                <div className="text-3xl mb-2 group-hover:animate-pulse">📢</div>
                <div className="text-sm font-medium text-red-100">공지</div>
                <div className="text-xs text-red-300 mt-1">중요 안내</div>
              </div>
            </Link>
            <Link to="/posts?category=STARLIGHT_CINEMA&sort=recent" className="group">
              <div className="relative p-4 bg-gradient-to-br from-purple-600/20 to-pink-600/20 hover:from-purple-600/40 hover:to-pink-600/40 rounded-xl border border-purple-500/30 hover:border-purple-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-purple-500/25 overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-r from-purple-500/10 to-pink-500/10 opacity-0 group-hover:opacity-100 transition-opacity"></div>
                <div className="absolute top-2 right-2">
                  <span className="bg-gradient-to-r from-purple-500 to-pink-500 text-white text-xs px-2 py-1 rounded-full font-bold">🤖 AI</span>
                </div>
                <div className="text-3xl mb-2 relative z-10 group-hover:animate-pulse">🎬</div>
                <div className="text-sm font-medium relative z-10 text-purple-100">별빛 시네마</div>
                <div className="text-xs text-purple-300 mt-1 relative z-10">영상 큐레이션</div>
              </div>
            </Link>
            <Link to="/suggestions" className="group">
              <div className="p-4 bg-gradient-to-br from-orange-600/20 to-amber-600/20 hover:from-orange-600/40 hover:to-amber-600/40 rounded-xl border border-orange-500/30 hover:border-orange-400/50 transition-all duration-300 text-center transform hover:scale-105 shadow-lg hover:shadow-orange-500/25">
                <div className="text-3xl mb-2 group-hover:animate-bounce">💡</div>
                <div className="text-sm font-medium text-orange-100">건의게시판</div>
                <div className="text-xs text-orange-300 mt-1">아이디어 공유</div>
              </div>
            </Link>
          </div>
        </div>



        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-8">
            {/* 📢 공지사항 */}
            <Section 
              title="공지사항" 
              icon="📢" 
              link="/posts?category=NOTICE&sort=recent"
              bgColor="bg-gradient-to-br from-emerald-900/30 to-green-900/30"
              borderColor="border-emerald-500/20"
            >
              <div className="space-y-3">
                {noticePosts.filter(post => !post.blinded).map((post) => (
                  <div key={post.id} className="group bg-emerald-900/20 hover:bg-emerald-900/40 rounded-xl p-4 transition-all duration-300 border border-emerald-700/20 hover:border-emerald-500/50">
                    <Link to={`/posts/${post.id}`} className="block">
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-3">
                          <span className="bg-gradient-to-r from-emerald-500 to-green-500 text-white px-3 py-1 rounded-full text-xs font-bold">
                            공지
                          </span>
                          <span className="font-semibold text-emerald-100 group-hover:text-white transition-colors">
                            {post.title}
                          </span>
                        </div>
                        <span className="text-emerald-300 text-sm">👁 {post.viewCount}</span>
                      </div>
                      <div className="text-emerald-200/70 text-sm">
                        🖊 {post.writer} • 📅 {formatDate(post.updatedAt)}
                      </div>
                    </Link>
                  </div>
                ))}
              </div>
            </Section>

            {/* 🔥 인기 게시글 */}
            <div className="bg-gradient-to-br from-slate-800/50 to-purple-900/30 backdrop-blur-md rounded-2xl p-6 border border-purple-500/20">
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 bg-gradient-to-r from-orange-500 to-red-500 rounded-full flex items-center justify-center">
                  🔥
                </div>
                <h2 className="text-2xl font-bold bg-gradient-to-r from-orange-400 to-red-400 bg-clip-text text-transparent">
                  인기 게시글
                </h2>
              </div>
              <div className="grid md:grid-cols-2 gap-4">
                {posts.filter(post => !post.blinded).map((post) => (
                  <div key={post.id} className="group bg-white/5 hover:bg-white/10 rounded-xl p-4 transition-all duration-300 border border-white/10 hover:border-purple-400/50">
                    <Link to={`/posts/${post.id}`}>
                      <h3 className="font-semibold mb-2 group-hover:text-purple-300 transition-colors">
                        {post.dDay && <span className="bg-orange-500 text-white px-2 py-1 rounded text-xs mr-2">{post.dDay}</span>}
                        {post.title}
                      </h3>
                      <p className="text-sm text-gray-400 line-clamp-2 mb-3">{post.content}</p>
                      <div className="flex items-center justify-between text-xs text-gray-500">
                        <span className="flex items-center gap-1">
                          🖊 {post.writer}
                        </span>
                        <div className="flex items-center gap-3">
                          <span>👁 {post.viewCount}</span>
                          <span>💬 {post.commentCount || 0}</span>
                          <span>❤️ {post.likeCount}</span>
                        </div>
                      </div>
                    </Link>
                  </div>
                ))}
              </div>
            </div>

            {/* 🌌 별 사진 갤러리 */}
            <Section 
              title="밤하늘 별 사진 갤러리" 
              icon="🌌" 
              link="/posts?category=IMAGE&sort=recent"
              bgColor="bg-gradient-to-br from-indigo-900/30 to-purple-900/30"
              borderColor="border-indigo-500/30"
            >
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                {starPhotos.filter(photo => !photo.blinded).map((photo) => (
                  <Link to={`/posts/${photo.id}`} key={photo.id}>
                    <div className="rounded-lg overflow-hidden shadow-lg hover:shadow-indigo-500/50 transition-all duration-300 group bg-indigo-900/20">
                      <img
                        src={photo.thumbnailUrl || '/placeholder-image.jpg'}
                        alt={photo.title}
                        className="w-full h-32 object-cover group-hover:scale-110 transition-transform duration-300"
                      />
                      <div className="px-3 py-2 text-sm truncate text-indigo-100">{photo.title}</div>
                    </div>
                  </Link>
                ))}
              </div>
            </Section>



            {/* 🚀 우주 뉴스 */}
            <Section 
              title="우주 뉴스" 
              icon="🚀" 
              link="/posts?category=NEWS&sort=recent"
              bgColor="bg-gradient-to-br from-blue-900/30 to-cyan-900/30"
              borderColor="border-blue-500/30"
            >
              <div className="mb-3 p-2 bg-blue-800/30 rounded-lg border border-blue-600/30">
                <p className="text-blue-200 text-xs flex items-center gap-2">
                  <span className="text-green-400">🤖</span>
                  <span>뉴스봇이 매일 오전 8시에 최신 우주 뉴스를 자동 수집합니다</span>
                </p>
              </div>
              <div className="space-y-3">
                {newsPosts.filter(post => !post.blinded).map((post) => (
                  <div key={post.id} className="bg-blue-900/20 rounded-lg p-4 hover:bg-blue-900/30 transition-colors">
                    <Link to={`/posts/${post.id}`} className="block">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-blue-100">{post.title}</span>
                        <div className="flex items-center gap-2 text-blue-300 text-sm">
                          <span>❤️ {post.likeCount}</span>
                          <span>💬 {post.commentCount || 0}</span>
                          <span>👁 {post.viewCount}</span>
                        </div>
                      </div>
                      <div className="text-blue-200/70 text-sm mt-1">🖊 {post.writer} • 📅 {formatDate(post.updatedAt)}</div>
                    </Link>
                  </div>
                ))}
              </div>
            </Section>

            {/* ⭐ 리뷰 게시판 */}
            <Section 
              title="리뷰 게시판" 
              icon="⭐" 
              link="/posts?category=REVIEW&sort=recent"
              bgColor="bg-gradient-to-br from-purple-900/30 to-pink-900/30"
              borderColor="border-purple-500/30"
            >
              <div className="space-y-3">
                {reviewPosts.filter(post => !post.blinded).map((post) => (
                  <div key={post.id} className="bg-purple-900/20 rounded-lg p-4 hover:bg-purple-900/30 transition-colors">
                    <Link to={`/posts/${post.id}`} className="block">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-purple-100">{post.title}</span>
                        <div className="flex items-center gap-2 text-purple-300 text-sm">
                          <span>❤️ {post.likeCount}</span>
                          <span>💬 {post.commentCount || 0}</span>
                          <span>👁 {post.viewCount}</span>
                        </div>
                      </div>
                      <div className="text-purple-200/70 text-sm mt-1">🖊 {post.writer} • 📅 {formatDate(post.updatedAt)}</div>
                    </Link>
                  </div>
                ))}
              </div>
            </Section>

            {/* 💬 토론 게시판 */}
            <Section 
              title="토론 게시판" 
              icon="💬" 
              link="/posts?category=DISCUSSION&sort=recent"
              bgColor="bg-gradient-to-br from-green-900/30 to-teal-900/30"
              borderColor="border-green-500/30"
            >
            <div className="mb-3 p-2 bg-green-800/30 rounded-lg border border-green-600/30">
                <p className="text-green-200 text-xs flex items-center gap-2">
                  <span className="text-green-400">🤖</span>
                  <span>AI가 매일 오전 8시에 흥미로운 토론 주제를 자동 선정합니다.</span>
                </p>
              </div>
              <div className="space-y-3">
                {discussionPosts.filter(post => !post.blinded).map((post) => (
                  <div key={post.id} className="bg-green-900/20 rounded-lg p-4 hover:bg-green-900/30 transition-colors">
                    <Link to={`/posts/${post.id}`} className="block">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-green-100">{post.title}</span>
                        <div className="flex items-center gap-2 text-green-300 text-sm">
                          <span>❤️ {post.likeCount}</span>
                          <span>💬 {post.commentCount || 0}</span>
                          <span>👁 {post.viewCount}</span>
                        </div>
                      </div>
                      <div className="text-green-200/70 text-sm mt-1">🖊️ {post.writer} • 📅 {formatDate(post.updatedAt)}</div>
                    </Link>
                  </div>
                ))}
              </div>
            </Section>

            {/* 🎈 자유 게시판 */}
            <Section 
              title="자유 게시판" 
              icon="🎈" 
              link="/posts?category=FREE&sort=recent"
              bgColor="bg-gradient-to-br from-pink-900/30 to-rose-900/30"
              borderColor="border-pink-500/30"
            >
              <div className="space-y-3">
                {freePosts.filter(post => !post.blinded).map((post) => (
                  <div key={post.id} className="bg-pink-900/20 rounded-lg p-4 hover:bg-pink-900/30 transition-colors">
                    <Link to={`/posts/${post.id}`} className="block">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-pink-100">{post.title}</span>
                        <div className="flex items-center gap-2 text-pink-300 text-sm">
                          <span>❤️ {post.likeCount}</span>
                          <span>💬 {post.commentCount || 0}</span>
                          <span>👁 {post.viewCount}</span>
                        </div>
                      </div>
                      <div className="text-pink-200/70 text-sm mt-1">🖊️ {post.writer} • 📅 {formatDate(post.updatedAt)}</div>
                    </Link>
                  </div>
                ))}
              </div>
            </Section>

            {/* 🎬 별빛 시네마 */}
            <Section 
              title="별빛 시네마" 
              icon="🎬" 
              link="/posts?category=STARLIGHT_CINEMA&sort=recent"
              bgColor="bg-gradient-to-br from-purple-900/30 to-pink-900/30"
              borderColor="border-purple-500/30"
            >
              <div className="mb-3 p-3 bg-gradient-to-r from-purple-800/30 to-pink-800/30 rounded-lg border border-purple-600/30">
                <p className="text-purple-200 text-xs flex items-center gap-2">
                  <span className="text-purple-400">🤖</span>
                  <span>AI 봇이 매일 오후 8시에 우주 관련 YouTube 영상을 자동 큐레이션합니다</span>
                </p>
              </div>
              <div className="space-y-3">
                {cinemaPosts.filter(post => !post.blinded).map((post) => (
                  <div key={post.id} className="bg-gradient-to-r from-purple-900/20 to-pink-900/20 rounded-lg p-4 hover:from-purple-900/30 hover:to-pink-900/30 transition-all duration-300 border border-purple-700/20">
                    <Link to={`/posts/${post.id}`} className="block">
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
                      <div className="text-purple-200/70 text-sm mt-1">🤖 {post.writer} • 📅 {formatDate(post.updatedAt)}</div>
                    </Link>
                  </div>
                ))}
              </div>
            </Section>
          </div>

          {/* 💬 채팅 사이드바 */}
          <div className="lg:col-span-1">
            <div className="sticky top-4">
              <div className="min-w-0">
                <ChatSidebar />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}