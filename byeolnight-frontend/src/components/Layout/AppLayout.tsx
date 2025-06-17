import { ReactNode } from 'react'

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-black text-white px-6 py-4 shadow-md flex justify-between items-center">
        <h1 className="text-xl font-bold">🌌 별 헤는 밤</h1>
        <div>
          {/* 로그인 상태에 따라 바뀔 버튼 위치 */}
          <button className="text-sm hover:underline">로그인</button>
        </div>
      </header>

      <main className="flex-1 px-6 py-8">{children}</main>

      <footer className="bg-black text-center text-sm py-4 text-gray-400">
        © 2025 별 헤는 밤 커뮤니티
      </footer>
    </div>
  )
}
