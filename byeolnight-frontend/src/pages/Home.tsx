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
}

export default function Home() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [starPhotos, setStarPhotos] = useState<Post[]>([]);
  const [eventPosts, setEventPosts] = useState<Post[]>([]);
  const [newsPosts, setNewsPosts] = useState<Post[]>([]);
  const [noticePosts, setNoticePosts] = useState<Post[]>([]);
  const { user } = useAuth();

  useEffect(() => {
    axios.get('/public/posts/hot', { params: { size: 6 } })
      .then(res => setPosts(res.data.data))
      .catch(err => console.error('인기 게시글 불러오기 실패', err));

    axios.get('/public/posts', { params: { category: 'IMAGE', sort: 'popular', size: 4 } })
      .then(res => setStarPhotos(res.data.data.content))
      .catch(err => console.error('별 사진 게시판 불러오기 실패', err));

    axios.get('/public/posts', { params: { category: 'EVENT', sort: 'recent', size: 5 } })
      .then(res => setEventPosts(res.data.data.content))
      .catch(err => console.error('천문대 게시판 불러오기 실패', err));

    axios.get('/public/posts', { params: { category: 'REVIEW', sort: 'recent', size: 5 } })
      .then(res => setNewsPosts(res.data.data.content))
      .catch(err => console.error('우주 뉴스 게시판 불러오기 실패', err));

    axios.get('/public/posts', { params: { category: 'NOTICE', sort: 'recent', size: 5 } })
      .then(res => setNoticePosts(res.data.data.content))
      .catch(err => console.error('공지사항 불러오기 실패', err));
  }, []);

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return `${date.getFullYear()}.${(date.getMonth() + 1).toString().padStart(2, '0')}.${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
  };

  const Section = ({
    title,
    icon,
    link,
    children,
  }: {
    title: string;
    icon: string;
    link: string;
    children: React.ReactNode;
  }) => (
    <div className="mt-6">
      <div className="flex justify-between items-center mb-3">
        <h3 className="text-xl font-semibold text-purple-200">{icon} {title}</h3>
        <Link to={link} className="text-sm text-purple-300 hover:text-purple-100 underline transition">+ 더보기</Link>
      </div>
      {children}
    </div>
  );

  const PostListItem = (post: Post, showLike = true) => (
    <li key={post.id} className="text-[15px] flex items-center justify-between border-b border-gray-700 py-2 hover:text-purple-300 transition">
      <Link to={`/posts/${post.id}`} className="flex flex-col w-full">
        <span className="font-semibold truncate">{post.title}</span>
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
            {/* 🔥 인기 게시글 */}
            <div className="mt-6">
              <h2 className="text-2xl font-bold text-white drop-shadow-glow mb-4">🔥 인기 게시글</h2>
              <ul className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {posts.map((post) => (
                  <li key={post.id} className="bg-[#1f2336]/80 backdrop-blur-md p-4 rounded-xl shadow hover:shadow-purple-600 transition">
                    <Link to={`/posts/${post.id}`}>
                      <h3 className="text-[16px] font-semibold mb-1">{post.title}</h3>
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

            {/* 🌌 별 사진 */}
            <Section title="밤하늘 별 사진" icon="🌌" link="/posts?category=IMAGE&sort=recent">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                {starPhotos.map((photo) => (
                  <Link to={`/posts/${photo.id}`} key={photo.id}>
                    <div className="rounded overflow-hidden shadow hover:shadow-purple-600 transition group">
                      <img
                        src={photo.thumbnailUrl}
                        alt={photo.title}
                        className="w-full h-32 object-cover group-hover:scale-105 transition-transform"
                      />
                      <div className="px-2 py-1 text-sm truncate bg-[#1f2336]">{photo.title}</div>
                    </div>
                  </Link>
                ))}
              </div>
            </Section>

            {/* 🪐 천문대 일정 */}
            <Section title="천문대 견학 일정 안내" icon="🪐" link="/posts?category=EVENT&sort=recent">
              <ul className="text-sm divide-y divide-gray-700">
                {eventPosts.map((post) => PostListItem(post))}
              </ul>
            </Section>

            {/* 🚀 우주 뉴스 */}
            <Section title="우주 뉴스" icon="🚀" link="/posts?category=REVIEW&sort=recent">
              <ul className="text-sm divide-y divide-gray-700">
                {newsPosts.map((post) => PostListItem(post))}
              </ul>
            </Section>

            {/* 📢 공지사항 */}
            <Section title="공지사항" icon="📢" link="/posts?category=NOTICE&sort=recent">
              <ul className="text-sm divide-y divide-gray-700">
                {noticePosts.map((post) => PostListItem(post, false))}
              </ul>
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
