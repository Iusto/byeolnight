import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';

interface FileDto {
  originalName: string;
  s3Key: string;
  url: string;
}

export default function PostCreate() {
  const navigate = useNavigate();
  const { user, refreshUserInfo } = useAuth();
  const [searchParams] = useSearchParams();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('DISCUSSION');
  const [error, setError] = useState('');

  // URL 파라미터에서 고정 카테고리 설정
  const fixedCategory = searchParams.get('fixedCategory');
  const isFixedCategory = fixedCategory && ['DISCUSSION', 'IMAGE', 'REVIEW', 'FREE', 'NOTICE', 'NEWS', 'EVENT'].includes(fixedCategory);
  
  useEffect(() => {
    if (isFixedCategory) {
      setCategory(fixedCategory);
    }
  }, [fixedCategory, isFixedCategory]);



  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!user) {
      setError('로그인이 필요합니다.');
      return;
    }

    try {
      const response = await axios.post('/member/posts', {
        title,
        content,
        category,
        images: [], // 에디터에서 이미지는 HTML로 처리
      });
      
      console.log('게시글 작성 완료:', response.data);
      
      // 공지글인 경우 알림 생성 확인
      if (category === 'NOTICE') {
        console.log('공지글 작성 완료 - 알림 생성 대기 중...');
        // 3초 후 알림 확인
        setTimeout(async () => {
          try {
            const notificationResponse = await axios.get('/member/notifications/unread/count');
            console.log('공지글 작성 후 알림 개수:', notificationResponse.data);
          } catch (err) {
            console.error('알림 확인 실패:', err);
          }
        }, 3000);
      }
      
      // 사용자 정보 새로고침 (포인트 업데이트)
      await refreshUserInfo();
      
      // 해당 카테고리 게시판으로 이동
      navigate(`/posts?category=${category}&sort=recent`);
    } catch (err: any) {
      const msg = err?.response?.data?.message || '게시글 작성 실패';
      setError(msg);
    }
  };

  // 로그인 검증
  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center text-white">
        <div className="text-center">
          <p className="text-lg mb-4">게시글 작성은 로그인이 필요합니다.</p>
          <button 
            onClick={() => navigate('/login')}
            className="bg-purple-600 hover:bg-purple-700 px-4 py-2 rounded"
          >
            로그인 하러 가기
          </button>
        </div>
      </div>
    );
  }

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
          <div className="bg-white rounded-md">
            <ReactQuill
              theme="snow"
              value={content}
              onChange={setContent}
              placeholder="내용을 입력하세요..."
              modules={{
                toolbar: [
                  [{ 'header': [1, 2, 3, false] }],
                  ['bold', 'italic', 'underline', 'strike'],
                  [{ 'color': [] }, { 'background': [] }],
                  [{ 'list': 'ordered'}, { 'list': 'bullet' }],
                  [{ 'align': [] }],
                  ['link', 'image'],
                  ['clean']
                ]
              }}
              formats={[
                'header', 'bold', 'italic', 'underline', 'strike',
                'color', 'background', 'list', 'bullet', 'align',
                'link', 'image'
              ]}
              style={{
                height: '300px',
                marginBottom: '50px'
              }}
            />
          </div>
          {isFixedCategory ? (
            <div className="w-full px-4 py-2 rounded-md bg-[#2a2e45] text-gray-300">
              카테고리: {{
                DISCUSSION: '토론',
                IMAGE: '사진', 
                REVIEW: '후기',
                FREE: '자유',
                NOTICE: '공지',
                NEWS: '뉴스',
                EVENT: '우주전시회'
              }[category]} (고정)
            </div>
          ) : (
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="w-full px-4 py-2 rounded-md bg-[#2a2e45] focus:outline-none"
            >
              <option value="DISCUSSION">토론</option>
              <option value="IMAGE">사진</option>
              <option value="REVIEW">후기</option>
              <option value="FREE">자유</option>
              {user?.role === 'ADMIN' && (
                <>
                  <option value="NEWS">뉴스</option>
                  <option value="EVENT">우주전시회</option>
                </>
              )}
            </select>
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
