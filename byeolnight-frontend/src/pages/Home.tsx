import PostPreviewSection from '../components/PostPreviewSection'
import ChatSidebar from '../components/ChatSidebar'
import { useAuth } from '../hooks/useAuth'

export default function Home() {
  const { user } = useAuth()

  return (
    <div className="bg-gradient-to-b from-[#0f172a] to-[#1e293b] min-h-screen text-white">
      <div className="max-w-6xl mx-auto px-6 py-12">
        <header className="mb-10 text-center">
          <h1 className="text-3xl md:text-4xl font-bold text-white flex items-center justify-center gap-2">
            🌌 <span>별 헤는 밤에 오신 것을 환영합니다</span>
          </h1>
          <p className="mt-2 text-gray-300 text-sm md:text-base">
            커뮤니티에 오신 것을 진심으로 환영합니다. 자유롭게 별 이야기를 나누어보세요!
          </p>
        </header>

        <div className="flex flex-col md:flex-row gap-8">
          <main className="flex-1 grid grid-cols-1 sm:grid-cols-2 gap-6">
            <PostPreviewSection title="🔥 인기 게시글" apiUrl="/public/posts?sort=popular" />
            <PostPreviewSection title="🌌 밤하늘 별 사진 게시판" apiUrl="/public/posts?category=IMAGE" />
            <PostPreviewSection title="🛰️ 우주 뉴스 게시판" apiUrl="/public/posts?category=NEWS" />
            <PostPreviewSection title="🔭 천문대 견학 게시판" apiUrl="/public/posts?category=DISCUSSION" />
          </main>

          <aside className="w-full md:w-80">
            {user ? (
              <ChatSidebar />
            ) : (
              <section className="bg-[#1f2937] rounded-lg p-4 shadow text-white">
                <h2 className="text-lg font-semibold mb-2">💬 공용 채팅방</h2>
                <p className="text-gray-400">로그인 시 채팅 이용이 가능합니다.</p>
              </section>
            )}
          </aside>
        </div>

        <footer className="mt-12 text-center text-sm text-gray-400">
          © 2025 별 헤는 밤 커뮤니티
        </footer>
      </div>
    </div>
  )
}
