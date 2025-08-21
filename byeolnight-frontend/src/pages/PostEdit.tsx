import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import TuiEditor, { isHandlingImageUpload } from '../components/TuiEditor';
import { sanitizeHtml } from '../utils/htmlSanitizer';

import { uploadImage } from '../lib/s3Upload';

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
  
  // 클립보드 이미지 업로드 함수 (검열 과정 추가)
  const uploadClipboardImage = async (file: File) => {
    setIsImageValidating(true);
    try {
      console.log('클립보드 이미지 업로드 및 검열 시작...');
      
      // 파일 크기 체크 (10MB 제한으로 변경)
      if (file.size > 10 * 1024 * 1024) {
        throw new Error('이미지 크기는 10MB를 초과할 수 없습니다. 이미지를 압축하거나 크기를 줄여주세요.');
      }
      
      // 통합된 s3Upload 유틸리티 사용 (검열 과정 포함)
      const imageData = await uploadImage(file);
      console.log('이미지 업로드 완료:', imageData?.url ? '성공' : '실패');
      
      if (!imageData || !imageData.url) {
        throw new Error('이미지 URL을 받지 못했습니다.');
      }
      
      // 검열 통과한 이미지만 목록에 추가 (중복 방지)
      setUploadedImages(prev => {
        const exists = prev.some(img => img.url === imageData.url);
        return exists ? prev : [...prev, imageData];
      });
      console.log('검열 통과된 클립보드 이미지 추가');
      
      return imageData.url;
    } catch (error: any) {
      console.error('클립보드 이미지 업로드 오류:', error);
      const errorMsg = error.message || '이미지 검열 실패: 부적절한 이미지가 감지되었습니다.';
      
      // 오류 메시지 표시 (alert 대신 ValidationAlert만 사용)
      setValidationAlert({
        message: errorMsg,
        type: 'error'
      });
      throw error;
    } finally {
      setIsImageValidating(false);
    }
  };
  
  // 클립보드 붙여넣기 이벤트 핸들러
  const handlePaste = async (event: ClipboardEvent) => {
    // TUI Editor가 자체적으로 클립보드 이벤트를 처리하도록 허용
    if (document.activeElement?.closest('.toastui-editor-ww-container')) {
      return;
    }
    
    // TUI Editor에서 이미지 업로드를 처리 중이면 중복 처리 방지
    if (isHandlingImageUpload.current) {
      return;
    }
    
    try {
      const items = event.clipboardData?.items;
      if (!items) return;
      
      // 모바일 환경 감지
      const isMobileDevice = isMobile();
      
      // 모바일에서 클립보드 접근 제한 있을 수 있음
      if (isMobileDevice && items.length === 0) return;
      
      for (let i = 0; i < items.length; i++) {
        const item = items[i];
        
        // 이미지 파일인지 확인
        if (item.type.indexOf('image') !== -1) {
          event.preventDefault();
          const file = item.getAsFile();
          if (!file) continue;
          
          // 모바일에서는 클립보드 붙여넣기 제한
          if (isMobileDevice) {
            // alert 제거하고 ValidationAlert로 대체
            setValidationAlert({
              message: '모바일에서는 이미지 붙여넣기가 제한될 수 있습니다. 이미지 버튼을 사용해주세요.',
              type: 'warning'
            });
            return;
          }
          
          try {
            // 이미지 업로드 및 검열 시작
            const imageData = await uploadClipboardImage(file);
            if (!imageData || !imageData.url) throw new Error('이미지 URL을 받지 못했습니다.');
            
            // URL 검증
            if (!isValidImageUrl(imageData.url)) {
              throw new Error('유효하지 않은 이미지 URL입니다.');
            }
            
            // 검열 통과한 이미지만 에디터에 삽입
            insertImageToEditor(imageData.url, '클립보드 이미지');
          } catch (error: any) {
            console.error('클립보드 이미지 업로드 실패:', error);
            // 파일 입력 초기화 (동일한 파일 재선택 가능하도록)
            if (fileInputRef.current) {
              fileInputRef.current.value = '';
            }
            // ValidationAlert로 표시하고 alert 제거
            setValidationAlert({
              message: error.message || '이미지 검열 실패: 부적절한 이미지가 감지되었습니다.',
              type: 'error'
            });
          }
          break;
        }
      }
    } catch (error) {
      console.error('클립보드 처리 중 오류:', error);
      setIsImageValidating(false);
    }
  };
  
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
  
  // 모바일 환경 감지 함수
  const isMobile = () => {
    return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
  };

  // 파일 선택 입력 요소를 참조하기 위한 ref 추가
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  const handleImageUpload = () => {
    // 기존 ref를 통해 파일 선택 대화상자 열기
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };
  
  // 파일 선택 시 호출되는 함수
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) {
      return;
    }
    
    console.log('파일 선택됨:', file.name, file.type);
      
    // 파일 형식 검사
    const validImageTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
    if (!validImageTypes.includes(file.type)) {
      setValidationAlert({
        message: '지원되는 이미지 형식이 아닙니다. (jpg, png, gif, webp만 허용)',
        type: 'error'
      });
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      return;
    }
      
    setIsImageValidating(true);
    try {
      console.log('이미지 업로드 및 검열 시작...');
      
      // 파일 크기 체크 (10MB 제한으로 변경)
      if (file.size > 10 * 1024 * 1024) {
        throw new Error('이미지 크기는 10MB를 초과할 수 없습니다. 이미지를 압축하거나 크기를 줄여주세요.');
      }
      
      // 통합된 s3Upload 유틸리티 사용 (검열 과정 포함)
      const imageData = await uploadImage(file);
      console.log('이미지 업로드 완료:', imageData?.url ? '성공' : '실패');
      
      if (!imageData || !imageData.url) {
        throw new Error('이미지 URL을 받지 못했습니다.');
      }
      
      // URL 검증
      if (!isValidImageUrl(imageData.url)) {
        throw new Error('유효하지 않은 이미지 URL입니다.');
      }
      
      // 검열 통과한 이미지만 목록에 추가 (중복 방지)
      setUploadedImages(prev => {
        const exists = prev.some(img => img.url === imageData.url);
        return exists ? prev : [...prev, imageData];
      });
      console.log('검열 통과된 이미지 목록 업데이트');

      // 이미지를 에디터에 삽입
      insertImageToEditor(imageData, imageData.originalName || '검열 통과된 이미지');
      
    } catch (error: any) {
      console.error('이미지 업로드 오류:', error);
      const errorMsg = error.message || '이미지 업로드에 실패했습니다.';
      
      // 오류 메시지 표시 - alert 제거하고 ValidationAlert만 사용
      setValidationAlert({
        message: errorMsg,
        type: 'error'
      });
    } finally {
      setIsImageValidating(false);
      // 파일 입력 초기화 (동일한 파일 재선택 가능하도록)
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };
  
  const removeImage = (index: number) => {
    const imageToRemove = uploadedImages[index];
    if (!imageToRemove) return;
    
    // 에디터에서 해당 이미지 제거
    try {
      if (editorRef.current?.getInstance) {
        const instance = editorRef.current.getInstance();
        if (instance) {
          const currentContent = instance.getMarkdown();
          // URL을 정규식에서 안전하게 사용하기 위해 이스케이프
          const escapedUrl = imageToRemove.url.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
          // 마크다운 이미지 패턴 제거
          const imgRegex = new RegExp(`!\\[[^\\]]*\\]\\(${escapedUrl}\\)`, 'gi');
          const newContent = currentContent.replace(imgRegex, '').replace(/\n\n+/g, '\n\n');
          
          // 에디터와 상태 모두 업데이트
          instance.setMarkdown(newContent);
          setContent(newContent);
        }
      }
    } catch (error) {
      console.error('에디터에서 이미지 제거 중 오류:', error);
    }
    
    // 썸네일 목록에서 이미지 제거
    setUploadedImages(prev => prev.filter((_, i) => i !== index));
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
  
  // 컴포넌트 마운트 시 이벤트 리스너 등록
  useEffect(() => {
    document.addEventListener('paste', handlePaste);
    
    // TUI Editor에서 발생하는 이미지 검열 이벤트 수신
    const handleImageValidating = (e: CustomEvent) => {
      const { validating } = e.detail;
      setIsImageValidating(validating);
    };
    
    document.addEventListener('imageValidating', handleImageValidating as EventListener);
    
    return () => {
      document.removeEventListener('paste', handlePaste);
      document.removeEventListener('imageValidating', handleImageValidating as EventListener);
    };
  }, []);

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
      {/* 파일 선택 입력 요소 - 화면에 보이지 않지만 React에서 관리 */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        onChange={handleFileChange}
        style={{ display: 'none' }}
      />
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
                <div className="flex gap-2 w-full sm:w-auto">
                  <button
                    type="button"
                    onClick={handleImageUpload}
                    disabled={isImageValidating}
                    className="mobile-button touch-target touch-feedback flex items-center justify-center gap-2 px-3 py-2 sm:px-4 sm:py-2 bg-blue-600/80 active:bg-blue-600 mouse:hover:bg-blue-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white rounded-xl text-xs sm:text-sm font-medium transition-all duration-200 shadow-lg mouse:hover:shadow-blue-500/25 transform active:scale-95 mouse:hover:scale-105 disabled:transform-none flex-1 sm:flex-none"
                  >
                    {isImageValidating ? (
                      <>
                        <div className="animate-spin w-3 h-3 sm:w-4 sm:h-4 border-2 border-white border-t-transparent rounded-full"></div>
                        <span className="mobile-caption">{t('home.validating')}</span>
                      </>
                    ) : (
                      <>
                        <span className="text-sm sm:text-base">🖼️</span>
                        <span className="mobile-caption">{t('home.add_image')}</span>
                      </>
                    )}
                  </button>
                </div>
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
                    handleImageUpload={handleImageUpload}
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
          
          {/* 이미지 검열 중 알림 - 모바일 최적화 */}
          {isImageValidating && (
            <div className="p-3 sm:p-4 bg-blue-500/10 border border-blue-500/20 rounded-xl text-blue-400 text-sm flex items-center gap-2 sm:gap-3 mobile-card-compact">
              <div className="animate-spin w-4 h-4 sm:w-5 sm:h-5 border-2 border-blue-400 border-t-transparent rounded-full flex-shrink-0"></div>
              <div>
                <div className="font-medium mobile-text">🛡️ 이미지 검열 중...</div>
                <div className="text-xs text-blue-300 mt-1 mobile-caption">안전한 콘텐츠를 위해 이미지를 검사하고 있습니다. 잠시만 기다려주세요.</div>
              </div>
            </div>
          )}
          
          {/* 검열 결과 알림 - 모바일 최적화 */}
          {validationAlert && (
            <div className={`p-3 sm:p-4 ${validationAlert.type === 'error' ? 'bg-red-500/10 border-red-500/20 text-red-400' : validationAlert.type === 'warning' ? 'bg-yellow-500/10 border-yellow-500/20 text-yellow-400' : 'bg-green-500/10 border-green-500/20 text-green-400'} border rounded-xl text-sm flex items-start gap-2 sm:gap-3 animate-fadeIn mobile-card-compact`}>
              <div className={`${validationAlert.type === 'error' ? 'text-red-400' : validationAlert.type === 'warning' ? 'text-yellow-400' : 'text-green-400'} text-lg sm:text-xl flex-shrink-0`}>
                {validationAlert.type === 'error' ? '⚠️' : validationAlert.type === 'warning' ? '⚠️' : '✅'}
              </div>
              <div className="flex-1">
                <div className="font-medium mobile-text">{validationAlert.message}</div>
                {validationAlert.type === 'error' && (
                  <div className="text-xs mt-1 mobile-caption">
                    이미지가 자동으로 삭제되었습니다. 다른 이미지를 사용해주세요.
                  </div>
                )}
              </div>
              <button 
                onClick={() => setValidationAlert(null)} 
                className="text-base sm:text-lg hover:text-white transition-colors touch-target flex-shrink-0"
              >
                ×
              </button>
            </div>
          )}

          {/* 업로드된 이미지 미리보기 - 모바일 최적화 */}
          {uploadedImages.length > 0 && (
            <div className="space-y-2">
              <h3 className="text-sm font-medium text-gray-300 flex flex-col sm:flex-row items-start sm:items-center gap-2 mobile-text">
                <span>업로드된 이미지:</span>
                <span className="text-xs bg-green-600/20 text-green-400 px-2 py-1 rounded-full border border-green-500/30 mobile-caption">
                  ✓ 안전한 이미지만 표시됨
                </span>
              </h3>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 sm:gap-4 mobile-grid-2">
                {uploadedImages.map((image, index) => (
                  <div key={index} className="relative group">
                    <img
                      src={image.url}
                      alt={image.originalName}
                      className="w-full h-20 sm:h-24 object-cover rounded-lg shadow-md mobile-thumbnail"
                      onError={(e) => {
                        console.error('이미지 로드 실패:', image.url);
                        e.currentTarget.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEyIDJMMTMuMDkgOC4yNkwyMCA5TDEzLjA5IDE1Ljc0TDEyIDIyTDEwLjkxIDE1Ljc0TDQgOUwxMC45MSA4LjI2TDEyIDJaIiBmaWxsPSIjOTk5Ii8+Cjwvc3ZnPgo=';
                      }}
                    />
                    <button
                      type="button"
                      onClick={() => removeImage(index)}
                      className="absolute top-1 right-1 bg-red-600 hover:bg-red-700 text-white rounded-full w-6 h-6 sm:w-7 sm:h-7 flex items-center justify-center text-xs opacity-0 group-hover:opacity-100 transition-opacity touch-target"
                    >
                      ×
                    </button>
                    <div className="absolute bottom-0 left-0 right-0 bg-black/50 text-white text-xs p-1 rounded-b-lg truncate mobile-caption">
                      {image.originalName}
                    </div>
                    <div className="absolute top-1 left-1 bg-green-600/80 text-white text-xs px-1 py-0.5 rounded flex items-center gap-1 mobile-caption">
                      ✓ 안전한 이미지
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
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