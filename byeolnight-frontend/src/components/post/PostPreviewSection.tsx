import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import axios from '../lib/axios'

interface Post {
  id: number
  title: string
  author: string
  likeCount: number
}

interface Props {
  title: string
  apiUrl: string
}

export default function PostPreviewSection({ title, apiUrl }: Props) {
  const [posts, setPosts] = useState<Post[]>([])

  useEffect(() => {
    axios.get(apiUrl)
      .then(res => {
        const data = res.data?.data?.content || []
        setPosts(data)
      })
      .catch(err => console.error(`❌ ${title} 가져오기 실패`, err))
  }, [apiUrl])

  return (
    <section className="bg-[#1e293b] rounded-xl p-5 shadow-md hover:shadow-lg transition">
      <h2 className="text-xl font-bold text-white mb-4 border-b border-gray-600 pb-2">{title}</h2>
      <ul className="space-y-2">
        {posts.length === 0 && <li className="text-gray-400">게시글이 없습니다.</li>}
        {posts.map(post => (
          <li key={post.id} className="flex justify-between items-center text-sm text-white hover:text-blue-300 transition">
            <Link to={`/posts/${post.id}`} className="truncate max-w-[80%]">{post.title}</Link>
            <span className="text-xs text-pink-400">❤️ {post.likeCount}</span>
          </li>
        ))}
      </ul>
    </section>
  )
}
