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
  likeCount: number;
  viewCount: number;
  updatedAt: string;
  blinded: boolean;
  thumbnailUrl?: string;
  dDay?: string;
}

export default function Home() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [starPhotos, setStarPhotos] = useState<Post[]>([]);
  const [eventPosts, setEventPosts] = useState<Post[]>([]);
  const [newsPosts, setNewsPosts] = useState<Post[]>([]);
  const [reviewPosts, setReviewPosts] = useState<Post[]>([]);
  const [noticePosts, setNoticePosts] = useState<Post[]>([]);
  const { user } = useAuth();

  useEffect(() => {
    // 인기 게시글 로딩
    axios.get('/public/posts/hot', { params: { size: 6 } })
      .then(res => {
        const data = res.data || [];
        setPosts(Array.isArray(data) ? data : []);
      })
      .catch(err => {
        console.error('인기 게시글 불러오기 실패', err);
        setPosts([]);
      });

    // 별 사진 게시판
    axios.get('/public/posts', { params: { category: 'IMAGE', sort: 'popular', size: 4 } })
      .then(res => {
        const content = res.data?.content || [];
        setStarPhotos(Array.isArray(content) ? content : []);
      })
      .catch(err => {
        console.error('별 사진 게시판 불러오기 실패', err);
        setStarPhotos([]);
      });

    // 우주 전시회
    axios.get('/public/posts', { params: { category: 'EVENT', sort: 'recent', size: 5 } })
      .then(res => {
        const content = res.data?.content || [];
        setEventPosts(Array.isArray(content) ? content : []);
      })
      .catch(err => {
        console.error('우주 전시회 게시판 불러오기 실패', err);
        setEventPosts([]);
      });

    // 우주 뉴스
    axios.get('/public/posts', { params: { category: 'NEWS', sort: 'recent', size: 5 } })
      .then(res => {
        const content = res.data?.content || [];
        setNewsPosts(Array.isArray(content) ? content : []);
      })
      .catch(err => {
        console.error('우주 뉴스 게시판 불러오기 실패', err);
        setNewsPosts([]);
      });

    // 리뷰 게시판
    axios.get('/public/posts', { params: { category: 'REVIEW', sort: 'recent', size: 5 } })
      .then(res => {
        const content = res.data?.content || [];
        setReviewPosts(Array.isArray(content) ? content : []);
      })
      .catch(err => {
        console.error('리뷰 게시판 불러오기 실패', err);
        setReviewPosts([]);
      });

    // 공지사항
    axios.get('/public/posts', { params: { category: 'NOTICE', sort: 'recent', size: 5 } })
      .then(res => {
        const content = res.data?.content || [];
        setNoticePosts(Array.isArray(content) ? content : []);
      })
      .catch(err => {
        console.error('공지사항 불러오기 실패', err);
        setNoticePosts([]);
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
    <div className={`mt-8 p-6 rounded-xl border-2 ${bgColor} ${borderColor} shadow-lg hover:shadow-xl transition-all duration-300`}>
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-2xl font-bold text-white flex items-center gap-3">
          <span className="text-3xl">{icon}</span>
          <span className="bg-gradient-to-r from-white to-gray-300 bg-clip-text text-transparent">{title}</span>
        </h3>
        <Link 
          to={link} 
          className="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg text-sm font-medium transition-all duration-200 backdrop-blur-sm border border-white/20"
        >
          전체보기 →
        </Link>
      </div>
      <div className="border-t border-white/10 pt-4">
        {children}
      </div>
    </div>
  );

  const PostListItem = (post: Post, showLike = true) => (
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
          {showLike && <span>❤️ {post.likeCount}</span>}
        </div>
      </Link>
    </li>
  );

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-10 px-4">
      <div className="max-w-7xl mx-auto">
        {/* 👤 사용자 정보 영역 */}
        <div className="text-right mb-4">
          {user ? (
            <p className="text-purple-300 text-sm">{user.nickname}님, 환영합니다!</p>
          ) : (
            <p className="text-purple-400 text-sm">로그인을 해주세요</p>
          )}
        </div>

        <div className="flex flex-col lg:flex-row gap-6">
          <div className="flex-1">
            {/* 📢 공지사항 */}
            <Section 
              title="공지사항" 
              icon="📢" 
              link="/posts?category=NOTICE&sort=recent"
              bgColor="bg-gradient-to-br from-green-900/30 to-emerald-900/30"
              borderColor="border-green-500/30"
            >
              <div className="space-y-3">
                {noticePosts.map((post) => (
                  <div key={post.id} className="bg-green-900/20 rounded-lg p-4 hover:bg-green-900/30 transition-colors">
                    <Link to={`/posts/${post.id}`} className="block">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-green-100 flex items-center gap-2">
                          <span className="bg-green-500 text-white px-2 py-1 rounded text-xs font-bold">공지</span>
                          {post.title}
                        </span>
                        <span className="text-green-300 text-sm">👁 {post.viewCount}</span>
                      </div>
                      <div className="text-green-200/70 text-sm mt-1">🖊 {post.writer} • 📅 {formatDate(post.updatedAt)}</div>
                    </Link>
                  </div>
                ))}
              </div>
            </Section>

            {/* 🔥 인기 게시글 */}
            <div className="mt-8">
              <h2 className="text-2xl font-bold text-white drop-shadow-glow mb-4">🔥 인기 게시글</h2>
              <ul className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {posts.map((post) => (
                  <li key={post.id} className="bg-[#1f2336]/80 backdrop-blur-md p-4 rounded-xl shadow hover:shadow-purple-600 transition">
                    <Link to={`/posts/${post.id}`}>
                      <h3 className="text-[16px] font-semibold mb-1">
                        {post.dDay && <span className="text-orange-300 text-sm mr-2">[{post.dDay}]</span>}
                        {post.title}
                      </h3>
                      <div className="text-sm text-gray-300 line-clamp-2">{post.content}</div>
                      <div className="flex gap-2 text-[13px] text-gray-400 mt-2">
                        🖊 {post.writer}
                        <span>📅 {formatDate(post.updatedAt)}</span>
                        <span>👁 {post.viewCount}</span>
                        <span>❤️ {post.likeCount}</span>
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
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
                {starPhotos.map((photo) => (
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

            {/* 🏛️ 우주 전시회 */}
            <Section 
              title="우주 전시회" 
              icon="🏛️" 
              link="/posts?category=EVENT&sort=recent"
              bgColor="bg-gradient-to-br from-orange-900/30 to-red-900/30"
              borderColor="border-orange-500/30"
            >
              <div className="mb-3 p-2 bg-orange-800/30 rounded-lg border border-orange-600/30">
                <p className="text-orange-200 text-xs flex items-center gap-2">
                  <span className="text-green-400">🤖</span>
                  <span>우주전시회봇이 매일 오전 7시에 전국 우주 관련 전시회 정보를 자동 수집합니다</span>
                </p>
              </div>
              <div className="space-y-3">
                {eventPosts.map((post) => (
                  <div key={post.id} className="bg-orange-900/20 rounded-lg p-4 hover:bg-orange-900/30 transition-colors">
                    <Link to={`/posts/${post.id}`} className="block">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-orange-100 flex items-center gap-2">
                          {post.dDay && <span className="bg-orange-500 text-white px-2 py-1 rounded text-xs font-bold">{post.dDay}</span>}
                          {post.title}
                        </span>
                        <span className="text-orange-300 text-sm">👁 {post.viewCount}</span>
                      </div>
                      <div className="text-orange-200/70 text-sm mt-1">🖊 {post.writer} • 📅 {formatDate(post.updatedAt)}</div>
                    </Link>
                  </div>
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
                  <span>뉴스봇이 매일 오전 6시, 오후 12시에 최신 우주 뉴스를 자동 수집합니다</span>
                </p>
              </div>
              <div className="space-y-3">
                {newsPosts.map((post) => (
                  <div key={post.id} className="bg-blue-900/20 rounded-lg p-4 hover:bg-blue-900/30 transition-colors">
                    <Link to={`/posts/${post.id}`} className="block">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-blue-100">{post.title}</span>
                        <div className="flex items-center gap-2 text-blue-300 text-sm">
                          <span>❤️ {post.likeCount}</span>
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
                {reviewPosts.map((post) => (
                  <div key={post.id} className="bg-purple-900/20 rounded-lg p-4 hover:bg-purple-900/30 transition-colors">
                    <Link to={`/posts/${post.id}`} className="block">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-purple-100">{post.title}</span>
                        <div className="flex items-center gap-2 text-purple-300 text-sm">
                          <span>❤️ {post.likeCount}</span>
                          <span>👁 {post.viewCount}</span>
                        </div>
                      </div>
                      <div className="text-purple-200/70 text-sm mt-1">🖊 {post.writer} • 📅 {formatDate(post.updatedAt)}</div>
                    </Link>
                  </div>
                ))}
              </div>
            </Section>
          </div>

          {/* 💬 채팅 사이드바 */}
          <div className="w-full lg:w-96">
            <ChatSidebar />
          </div>
        </div>
      </div>
    </div>
  );
}