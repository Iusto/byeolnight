import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../lib/axios';

interface FileDto {
  originalName: string;
  s3Key: string;
  url: string;
}

export default function PostCreate() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('NEWS');
  const [images, setImages] = useState<FileDto[]>([]);
  const [error, setError] = useState('');

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      const filename = encodeURIComponent(file.name);
      const res = await axios.get('/files/presign', { params: { filename } });
      const { url, s3Key } = res.data.data;

      await fetch(url, { method: 'PUT', body: file });

      setImages((prev) => [
        ...prev,
        {
          originalName: file.name,
          s3Key,
          url: url.split('?')[0],
        },
      ]);
    } catch (err) {
      setError('파일 업로드 실패');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      await axios.post('/member/posts', {
        title,
        content,
        category,
        images,
      });
      navigate('/');
    } catch (err: any) {
      const msg = err?.response?.data?.message || '게시글 작성 실패';
      setError(msg);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex justify-center pt-20 text-white">
      <div className="w-full max-w-2xl bg-[#1f2336] p-8 rounded-xl shadow-lg">
        <h2 className="text-2xl font-bold mb-6">📝 게시글 작성</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <input
            type="text"
            placeholder="제목"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            className="w-full px-4 py-2 rounded-md bg-[#2a2e45] focus:outline-none"
          />
          <textarea
            placeholder="내용"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={6}
            required
            className="w-full px-4 py-2 rounded-md bg-[#2a2e45] focus:outline-none"
          />
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="w-full px-4 py-2 rounded-md bg-[#2a2e45] focus:outline-none"
          >
            <option value="NEWS">뉴스</option>
            <option value="DISCUSSION">토론</option>
            <option value="IMAGE">사진</option>
            <option value="EVENT">행사</option>
            <option value="REVIEW">후기</option>
          </select>
          <input
            type="file"
            accept="image/*"
            onChange={handleFileChange}
            className="w-full text-sm text-gray-300"
          />
          {images.length > 0 && (
            <ul className="text-sm text-green-300">
              {images.map((img, i) => (
                <li key={i}>✔️ {img.originalName}</li>
              ))}
            </ul>
          )}
          {error && <p className="text-red-400 text-sm">{error}</p>}
          <button
            type="submit"
            className="w-full bg-blue-500 hover:bg-blue-600 transition-colors py-2 rounded-md"
          >
            등록하기
          </button>
        </form>
      </div>
    </div>
  );
}
