import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import PostForm from '../components/PostForm';

// 이미지 URL 정규식
const IMAGE_URL_REGEX = /^https?:\/\/.+\.(jpg|jpeg|png|gif|webp)(\?.*)?$/i;

// 이미지 URL 검증 함수
const isValidImageUrl = (url: string): boolean => {
  return IMAGE_URL_REGEX.test(url);
};

interface FileDto {
  originalName: string;
  s3Key: string;
  url: string;
}

export default function PostEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, refreshUserInfo } = useAuth();
  const { t } = useTranslation();
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);
  const [postData, setPostData] = useState<{
    title: string;
    content: string;
    category: string;
    images: FileDto[];
  } | null>(null);

  // 게시글 데이터 로드
  useEffect(() => {
    const fetchPost = async () => {
      if (!id || isNaN(Number(id))) {
        setError('잘못된 게시글 ID입니다.');
        setLoading(false);
        return;
      }

      try {
        const res = await axios.get(`/public/posts/${id}`);
        const post = res.data?.data || res.data;
        
        if (!post || !post.title) {
          setError('게시글 데이터를 찾을 수 없습니다.');
          setLoading(false);
          return;
        }
        
        // 작성자 또는 관리자만 수정 가능
        if (post.writer !== user?.nickname && user?.role !== 'ADMIN') {
          setError('수정 권한이 없습니다.');
          setLoading(false);
          return;
        }
        
        // 기존 이미지들을 FileDto 형식으로 변환하여 설정
        const existingImages = (post.images || []).map((img: any) => ({
          originalName: img.originalName || '기존 이미지',
          s3Key: img.s3Key || '',
          url: img.url
        }));
        
        // 콘텐츠에서 마크다운 이미지 URL 추출하여 썸네일에 추가
        const markdownImages = (post.content.match(/!\[[^\]]*\]\([^)]+\)/g) || [])
          .map((match: string) => {
            const urlMatch = match.match(/\(([^)]+)\)/);
            return urlMatch ? urlMatch[1] : null;
          })
          .filter(Boolean)
          .filter((url: string) => isValidImageUrl(url))
          .map((url: string, index: number) => ({
            originalName: `콘텐츠 이미지 ${index + 1}`,
            s3Key: '',
            url: url
          }));
        
        // 기존 이미지와 콘텐츠 이미지 합치기 (중복 제거)
        const allImages = [...existingImages];
        markdownImages.forEach((mdImg: FileDto) => {
          if (!allImages.some(img => img.url === mdImg.url)) {
            allImages.push(mdImg);
          }
        });
        
        setPostData({
          title: post.title,
          content: post.content,
          category: post.category,
          images: allImages
        });
        
        console.log('로드된 이미지:', allImages);
        
      } catch (err) {
        console.error('게시글 로드 실패:', err);
        setError('게시글을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchPost();
  }, [id, user]);

  const handleSubmit = async (data: {
    title: string;
    content: string;
    category: string;
    images: FileDto[];
  }) => {
    setError('');
    setIsSubmitting(true);

    try {
      if (!user) {
        setError('로그인이 필요합니다.');
        return;
      }
      
      // 콘텐츠에서 실제 사용된 이미지 URL 추출
      const usedImageUrls = new Set<string>();
      
      // 마크다운 형식 이미지 추출
      const mdImgRegex = /!\[[^\]]*\]\(([^)]+)\)/gi;
      let mdMatch;
      while ((mdMatch = mdImgRegex.exec(data.content)) !== null) {
        usedImageUrls.add(mdMatch[1]);
      }
      
      // 실제 사용된 이미지만 필터링
      const usedImages = data.images.filter(img => usedImageUrls.has(img.url));
      
      const response = await axios.put(`/member/posts/${id}`, {
        title: data.title,
        content: data.content,
        category: data.category,
        images: usedImages
      });
      
      console.log('게시글 수정 완료:', response.data);
      
      // 사용자 정보 새로고침 (포인트 업데이트)
      await refreshUserInfo();
      
      // 게시글 상세 페이지로 이동
      navigate(`/posts/${id}`);
    } catch (err: any) {
      const msg = err?.response?.data?.message || '게시글 수정 실패';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-16 w-16 border-4 border-purple-500 border-t-transparent mb-4"></div>
          <p className="text-xl font-medium text-purple-300">게시글 로딩 중...</p>
        </div>
      </div>
    );
  }

  if (error && !postData) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white flex items-center justify-center">
        <div className="text-center">
          <div className="text-red-400 text-lg mb-4">{error}</div>
          <button 
            onClick={() => navigate('/posts')}
            className="px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded transition"
          >
            게시글 목록으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen min-h-screen-safe bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 sm:from-slate-900 sm:via-purple-900 sm:to-slate-900 mobile-bright text-white mobile-optimized mobile-scroll">

      {/* 헤더 섹션 - 모바일 최적화 */}
      <div className="relative overflow-hidden bg-gradient-to-r from-purple-900/50 to-pink-900/50 border-b border-purple-500/20">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/10 to-pink-600/10"></div>
        <div className="relative max-w-4xl mx-auto px-3 sm:px-6 py-4 sm:py-16 mobile-header">
          <div className="text-center">
            <div className="w-12 h-12 sm:w-16 sm:h-16 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-2xl sm:text-3xl mx-auto mb-3 sm:mb-6 shadow-lg">
              ✏️
            </div>
            <h1 className="text-xl sm:text-4xl md:text-5xl font-bold bg-gradient-to-r from-white via-purple-200 to-white bg-clip-text text-transparent mb-3 sm:mb-4 mobile-text mobile-title px-2">
              {t('home.post_edit')}
            </h1>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-3 sm:px-6 py-3 sm:py-8 mobile-optimized">
        <div className="bg-gradient-to-br from-slate-800/50 to-purple-900/30 backdrop-blur-md rounded-2xl p-3 sm:p-8 border border-purple-500/20 shadow-2xl mobile-section mobile-card-compact">
          {postData && (
            <PostForm
              initialTitle={postData.title}
              initialContent={postData.content}
              initialCategory={postData.category}
              initialImages={postData.images}
              isEdit={true}
              onSubmit={handleSubmit}
              submitButtonText="✏️ 수정 완료"
              isSubmitting={isSubmitting}
              error={error}
            />
          )}
        </div>
      </div>
    </div>
  );
}