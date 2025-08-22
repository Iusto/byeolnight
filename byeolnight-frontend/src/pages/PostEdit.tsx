import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import TuiEditor from '../components/TuiEditor';
import { sanitizeHtml } from '../utils/htmlSanitizer';
import ImageUploader from '../components/ImageUploader';

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
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [contentLength, setContentLength] = useState(0);
  const [category, setCategory] = useState('DISCUSSION');
  const [error, setError] = useState('');
  const [uploadedImages, setUploadedImages] = useState<FileDto[]>([]);
  const [isImageValidating, setIsImageValidating] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  const [validationAlert, setValidationAlert] = useState<{message: string, type: 'success' | 'error' | 'warning', imageUrl?: string} | null>(null);
  const editorRef = useRef<any>(null);
  

  
  // 에디터에 이미지 삽입 함수 (오버로드 지원)
  const insertImageToEditor = (imageData: FileDto | string, altText: string) => {
    const imageUrl = typeof imageData === 'string' ? imageData : imageData.url;
    
    // URL 검증
    if (!isValidImageUrl(imageUrl)) {
      setValidationAlert({
        message: '유효하지 않은 이미지 URL입니다.',
        type: 'error'
      });
      return;
    }
    
    // TUI Editor에 이미지 삽입
    try {
      if (editorRef.current?.getInstance) {
        const instance = editorRef.current.getInstance();
        if (instance) {
          instance.exec('addImage', { imageUrl, altText });
          setTimeout(() => {
            const newContent = instance.getMarkdown();
            setContent(newContent);
          }, 100);
        }
      } else {
        setContent(prev => prev + `![${altText}](${imageUrl})\n`);
      }
    } catch (error) {
      console.error('이미지 삽입 중 오류:', error);
      setContent(prev => prev + `![${altText}](${imageUrl})\n`);
      setValidationAlert({
        message: '이미지 삽입 중 오류가 발생했습니다.',
        type: 'error'
      });
    }
  };
  


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
        
        setTitle(post.title);
        setContent(post.content);
        setCategory(post.category);
        
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
        
        setUploadedImages(allImages);
        console.log('로드된 이미지:', allImages);
        
        // 콘텐츠 길이 계산
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = post.content;
        const textContent = tempDiv.textContent || tempDiv.innerText || '';
        setContentLength(textContent.length);
        
      } catch (err) {
        console.error('게시글 로드 실패:', err);
        setError('게시글을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchPost();
  }, [id, user]);
  


  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // 중복 제출 방지
    if (isSubmitting) {
      return;
    }
    
    setError('');
    setIsSubmitting(true);

    try {
      if (!user) {
        setError('로그인이 필요합니다.');
        return;
      }
      
      // 길이 검증
      if (title.length > 100) {
        setError('제목은 100자를 초과할 수 없습니다.');
        return;
      }
      
      if (content.length > 50000) {
        setError('내용은 50,000자를 초과할 수 없습니다.');
        return;
      }

      // HTML 보안 검증
      const finalContent = sanitizeHtml(content);
      
      // 콘텐츠에서 실제 사용된 이미지 URL 추출
      const usedImageUrls = new Set<string>();
      
      // 마크다운 형식 이미지 추출
      const mdImgRegex = /!\[[^\]]*\]\(([^)]+)\)/gi;
      let mdMatch;
      while ((mdMatch = mdImgRegex.exec(finalContent)) !== null) {
        usedImageUrls.add(mdMatch[1]);
      }
      
      // 실제 사용된 이미지만 필터링
      const usedImages = uploadedImages.filter(img => usedImageUrls.has(img.url));
      
      const response = await axios.put(`/member/posts/${id}`, {
        title,
        content: finalContent,
        category,
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

  if (error) {
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
          <form onSubmit={handleSubmit} className="space-y-4 sm:space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2 mobile-text">{t('post.title')}</label>
              <div className="relative">
                <input
                  type="text"
                  placeholder={t('home.title_placeholder')}
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  maxLength={100}
                  required
                  className="w-full px-3 py-3 sm:px-4 sm:py-3 rounded-xl bg-slate-700/50 text-white border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent placeholder-gray-400 transition-all duration-200 mobile-input touch-target mobile-text"
                />
                <div className={`text-xs mt-1 mobile-caption ${title.length > 90 ? 'text-red-400' : 'text-gray-400'}`}>
                  {title.length}/100
                </div>
              </div>
            </div>
            <div>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-3 gap-2 sm:gap-0">
                <label className="text-sm font-medium text-gray-300 mobile-text">{t('post.content')}</label>
                <ImageUploader
                  uploadedImages={uploadedImages}
                  setUploadedImages={setUploadedImages}
                  onImageInsert={insertImageToEditor}
                  isImageValidating={isImageValidating}
                  setIsImageValidating={setIsImageValidating}
                  validationAlert={validationAlert}
                  setValidationAlert={setValidationAlert}
                />
              </div>
              <div className="rounded-xl overflow-hidden border border-slate-600/50 quill-wrapper">
                <div className="quill-container" style={{ height: window.innerWidth <= 768 ? '350px' : '500px', display: 'flex', flexDirection: 'column' }}>
                  <TuiEditor
                    ref={editorRef}
                    value={content}
                    onChange={(newContent) => {
                      setContent(newContent);
                      // HTML 태그를 제외한 순수 텍스트 길이 계산
                      const tempDiv = document.createElement('div');
                      tempDiv.innerHTML = newContent;
                      const textContent = tempDiv.textContent || tempDiv.innerText || '';
                      setContentLength(textContent.length);
                      
                      // 에디터에서 삭제된 이미지 감지 및 썸네일 목록에서 제거 (양방향 동기화)
                      try {
                        const currentImageUrls = (newContent.match(/!\[[^\]]*\]\([^)]+\)/g) || [])
                          .map(match => {
                            const urlMatch = match.match(/\(([^)]+)\)/);
                            return urlMatch ? urlMatch[1] : null;
                          })
                          .filter(Boolean);
                        
                        setUploadedImages(prev => 
                          prev.filter(image => currentImageUrls.includes(image.url))
                        );
                      } catch (error) {
                        console.error('이미지 동기화 중 오류:', error);
                      }
                    }}
                    placeholder={t('home.content_placeholder')}
                    height={window.innerWidth <= 768 ? '350px' : '500px'}
                    handleImageUpload={() => {}}
                  />
                  <div className="text-right text-xs sm:text-sm mt-1">
                    <span className={`mobile-caption ${contentLength > 45000 ? (contentLength > 50000 ? 'text-red-400' : 'text-yellow-400') : 'text-gray-400'}`}>
                      {contentLength}/50,000자
                      {contentLength > 45000 && contentLength <= 50000 && (
                        <span className="text-yellow-400 ml-1">(제한에 근접함)</span>
                      )}
                      {contentLength > 50000 && (
                        <span className="text-red-400 ml-1">(제한 초과)</span>
                      )}
                    </span>
                  </div>
                </div>
              </div>
              <div className="text-xs text-gray-400 mt-2 p-2 sm:p-3 bg-slate-800/30 rounded-lg border border-slate-700/50 mobile-caption">
                🎨 {t('home.editor_info_1')}<br/>
                🖼️ {t('home.editor_info_2')}<br/>
                🛡️ {t('home.editor_info_3')}<br/>
                🎬 {t('home.editor_info_4')}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2 mobile-text">{t('home.category')}</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-3 py-3 sm:px-4 sm:py-3 rounded-xl bg-slate-700/50 text-white border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent mobile-input touch-target mobile-text"
              >
                <option value="DISCUSSION">{t('home.discussion')}</option>
                <option value="IMAGE">{t('home.star_photo')}</option>
                <option value="REVIEW">{t('home.review')}</option>
                <option value="FREE">{t('home.free')}</option>
                {user?.role === 'ADMIN' && (
                  <>
                    <option value="NOTICE">{t('home.notice')}</option>
                    <option value="NEWS">{t('home.space_news')}</option>
                    <option value="STARLIGHT_CINEMA">{t('home.star_cinema')}</option>
                  </>
                )}
              </select>
            </div>

            {error && (
              <div className="p-3 sm:p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm mobile-card-compact mobile-text">
                {error}
              </div>
            )}
            
            <button
              type="submit"
              disabled={isImageValidating || isSubmitting}
              className="w-full mobile-button touch-target touch-feedback bg-gradient-to-r from-purple-600 to-pink-600 active:from-purple-700 active:to-pink-700 mouse:hover:from-purple-700 mouse:hover:to-pink-700 disabled:from-gray-600 disabled:to-gray-700 disabled:cursor-not-allowed text-white font-semibold py-4 rounded-xl transition-all duration-200 transform active:scale-95 mouse:hover:scale-105 disabled:transform-none shadow-lg mouse:hover:shadow-purple-500/25"
            >
              {isSubmitting ? (
                <div className="flex items-center justify-center gap-2">
                  <div className="animate-spin w-4 h-4 sm:w-5 sm:h-5 border-2 border-white border-t-transparent rounded-full"></div>
                  <span className="mobile-text">수정 중...</span>
                </div>
              ) : isImageValidating ? (
                <div className="flex items-center justify-center gap-2">
                  <div className="animate-spin w-4 h-4 sm:w-5 sm:h-5 border-2 border-white border-t-transparent rounded-full"></div>
                  <span className="mobile-text">{t('home.image_validating')}</span>
                </div>
              ) : (
                <span className="mobile-text">✏️ 수정 완료</span>
              )}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}