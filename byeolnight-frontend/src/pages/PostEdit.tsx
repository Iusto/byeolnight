import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from '../lib/axios';

interface FileDto {
  originalName: string;
  s3Key: string;
  url: string;
}

export default function PostEdit() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('NEWS');
  const [images, setImages] = useState<FileDto[]>([]);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchPost = async () => {
      try {
        const res = await axios.get(`/public/posts/${id}`);
        const post = res.data.data;
        setTitle(post.title);
        setContent(post.content);
        setCategory(post.category);
        setImages(post.images || []);
      } catch {
        setError('게시글을 불러오지 못했습니다.');
      }
    };

    fetchPost();
  }, [id]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await axios.put(`/member/posts/${id}`, {
        title,
        content,
        category,
        images,
      });
      navigate(`/posts/${id}`);
    } catch {
      setError('수정 실패');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-2xl mx-auto bg-[#1f2336]/80 backdrop-blur-md p-8 rounded-xl shadow-lg">
        <h2 className="text-3xl font-bold mb-6 drop-shadow-glow">✏ 게시글 수정</h2>

        <form onSubmit={handleSubmit} className="space-y-5">
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="제목"
            required
            className="w-full px-4 py-2 rounded bg-[#2a2e45] focus:outline-none"
          />
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={6}
            placeholder="내용"
            required
            className="w-full px-4 py-2 rounded bg-[#2a2e45] focus:outline-none"
          />
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="w-full px-4 py-2 rounded bg-[#2a2e45] focus:outline-none"
          >
            <option value="NEWS">뉴스</option>
            <option value="DISCUSSION">토론</option>
            <option value="IMAGE">사진</option>
            <option value="EVENT">행사</option>
            <option value="REVIEW">후기</option>
          </select>

          {/* 이미지 이름 출력 */}
          {images.length > 0 && (
            <ul className="text-sm text-green-400">
              {images.map((img, i) => (
                <li key={i}>✔ {img.originalName}</li>
              ))}
            </ul>
          )}

          {error && <p className="text-red-400 text-sm">{error}</p>}

          <button
            type="submit"
            className="w-full bg-blue-500 hover:bg-blue-600 py-2 rounded transition"
          >
            수정 완료
          </button>
        </form>
      </div>
    </div>
  );
}
