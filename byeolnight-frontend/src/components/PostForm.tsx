import { useState, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';
import TuiEditor from './TuiEditor';
import ImageUploader from './ImageUploader';
import MarkdownRenderer from './MarkdownRenderer';

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

interface PostFormProps {
  initialTitle?: string;
  initialContent?: string;
  initialCategory?: string;
  initialImages?: FileDto[];
  isEdit?: boolean;
  isFixedCategory?: boolean;
  fixedCategory?: string;
  onSubmit: (data: {
    title: string;
    content: string;
    category: string;
    images: FileDto[];
  }) => Promise<void>;
  submitButtonText: string;
  isSubmitting: boolean;
  error: string;
}

export default function PostForm({
  initialTitle = '',
  initialContent = '',
  initialCategory = 'DISCUSSION',
  initialImages = [],
  isEdit = false,
  isFixedCategory = false,
  fixedCategory,
  onSubmit,
  submitButtonText,
  isSubmitting,
  error
}: PostFormProps) {
  const { user } = useAuth();
  const { t } = useTranslation();
  const [title, setTitle] = useState(initialTitle);
  const [content, setContent] = useState(initialContent);
  const [contentLength, setContentLength] = useState(0);
  const [category, setCategory] = useState(initialCategory);
  const [uploadedImages, setUploadedImages] = useState<FileDto[]>(initialImages);
  const [isImageValidating, setIsImageValidating] = useState(false);
  const [validationAlert, setValidationAlert] = useState<{message: string, type: 'success' | 'error' | 'warning', imageUrl?: string} | null>(null);
  const editorRef = useRef<any>(null);

  // 초기값 설정
  useEffect(() => {
    if (fixedCategory && isFixedCategory) {
      setCategory(fixedCategory);
    }
  }, [fixedCategory, isFixedCategory]);

  // 컨텐츠 길이 계산
  useEffect(() => {
    if (content) {
      const tempDiv = document.createElement('div');
      tempDiv.innerHTML = content;
      const textContent = tempDiv.textContent || tempDiv.innerText || '';
      setContentLength(textContent.length);
    }
  }, [content]);

  // 에디터에 이미지 삽입 함수
  const insertImageToEditor = (imageData: FileDto | string, altText: string) => {
    const imageUrl = typeof imageData === 'string' ? imageData : imageData.url;
    
    if (!isValidImageUrl(imageUrl)) {
      setValidationAlert({
        message: '유효하지 않은 이미지 URL입니다.',
        type: 'error'
      });
      return;
    }
    
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

  const removeImage = (index: number) => {
    const imageToRemove = uploadedImages[index];
    if (!imageToRemove) return;
    
    try {
      if (editorRef.current?.getInstance) {
        const instance = editorRef.current.getInstance();
        if (instance) {
          const currentContent = instance.getMarkdown();
          const escapedUrl = imageToRemove.url.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
          const imgRegex = new RegExp(`!\\[[^\\]]*\\]\\(${escapedUrl}\\)`, 'gi');
          const newContent = currentContent.replace(imgRegex, '').replace(/\n\n+/g, '\n\n');
          
          instance.setMarkdown(newContent);
          setContent(newContent);
        }
      }
    } catch (error) {
      console.error('에디터에서 이미지 제거 중 오류:', error);
    }
    
    setUploadedImages(prev => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (isSubmitting) return;
    
    // 길이 검증
    if (title.length > 100) {
      setValidationAlert({
        message: '제목은 100자를 초과할 수 없습니다.',
        type: 'error'
      });
      return;
    }
    
    const maxContentLength = isEdit ? 50000 : 10000;
    if (content.length > maxContentLength) {
      setValidationAlert({
        message: `내용은 ${maxContentLength.toLocaleString()}자를 초과할 수 없습니다.`,
        type: 'error'
      });
      return;
    }

    await onSubmit({
      title,
      content,
      category,
      images: uploadedImages
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 sm:space-y-6">
      <div>
        <label className="block text-sm font-medium text-gray-300 mobile-text-secondary mb-2">{t('post.title')}</label>
        <div className="relative">
          <input
            type="text"
            placeholder={t('home.title_placeholder')}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={100}
            required
            className="w-full mobile-input px-3 py-3 sm:px-4 sm:py-3 rounded-xl bg-slate-700/50 text-white mobile-text border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent placeholder-gray-400 transition-all duration-200 text-base touch-target"
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
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = newContent;
                const textContent = tempDiv.textContent || tempDiv.innerText || '';
                setContentLength(textContent.length);
                
                // 에디터에서 삭제된 이미지 감지 및 썸네일 목록에서 제거
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
              <span className={`mobile-caption ${contentLength > (isEdit ? 45000 : 9000) ? (contentLength > (isEdit ? 50000 : 10000) ? 'text-red-400' : 'text-yellow-400') : 'text-gray-400'}`}>
                {contentLength}/{isEdit ? '50,000' : '10,000'}자
                {contentLength > (isEdit ? 45000 : 9000) && contentLength <= (isEdit ? 50000 : 10000) && (
                  <span className="text-yellow-400 ml-1">(제한에 근접함)</span>
                )}
                {contentLength > (isEdit ? 50000 : 10000) && (
                  <span className="text-red-400 ml-1">(제한 초과)</span>
                )}
              </span>
            </div>
          </div>
        </div>
        
        {/* Markdown 미리보기 */}
        {content && (
          <div className="mt-4 p-4 bg-slate-800/30 rounded-xl border border-slate-700/50">
            <h3 className="text-sm font-medium text-gray-300 mb-3">📝 미리보기:</h3>
            <MarkdownRenderer content={content} isPreview={true} />
          </div>
        )}
        
        <div className="text-xs text-gray-400 mt-2 p-2 sm:p-3 bg-slate-800/30 rounded-lg border border-slate-700/50 mobile-caption">
          🎨 {t('home.editor_info_1')}<br/>
          🖼️ {t('home.editor_info_2')}<br/>
          🛡️ {t('home.editor_info_3')}<br/>
          🎬 {t('home.editor_info_4')}
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2 mobile-text">{t('home.category')}</label>
        {isFixedCategory ? (
          <div className="w-full px-3 py-3 sm:px-4 sm:py-3 rounded-xl bg-slate-700/50 text-gray-300 border border-slate-600/50 mobile-text">
            {{
              DISCUSSION: t('home.discussion'),
              IMAGE: t('home.star_photo'), 
              REVIEW: t('home.review'),
              FREE: t('home.free'),
              NOTICE: t('home.notice'),
              NEWS: t('home.space_news'),
              STARLIGHT_CINEMA: t('home.star_cinema')
            }[category]} ({t('home.fixed')})
          </div>
        ) : (
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="w-full px-3 py-3 sm:px-4 sm:py-3 rounded-xl bg-slate-700/50 text-white border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent mobile-input touch-target mobile-text"
          >
            <option value="DISCUSSION">{t('home.discussion')}</option>
            <option value="IMAGE">{t('home.star_photo')}</option>
            <option value="REVIEW">{t('home.review')}</option>
            <option value="FREE">{t('home.free')}</option>
            <option value="STARLIGHT_CINEMA">{t('home.star_cinema')}</option>
            {user?.role === 'ADMIN' && (
              <>
                <option value="NEWS">{t('home.space_news')}</option>
                {!isEdit && <option value="NOTICE">{t('home.notice')}</option>}
              </>
            )}
          </select>
        )}
      </div>

      {(error || validationAlert) && (
        <div className="p-3 sm:p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm mobile-card-compact mobile-text">
          {error || validationAlert?.message}
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
            <span className="mobile-text">{isEdit ? '수정 중...' : t('home.submitting')}</span>
          </div>
        ) : isImageValidating ? (
          <div className="flex items-center justify-center gap-2">
            <div className="animate-spin w-4 h-4 sm:w-5 sm:h-5 border-2 border-white border-t-transparent rounded-full"></div>
            <span className="mobile-text">{t('home.image_validating')}</span>
          </div>
        ) : (
          <span className="mobile-text">{submitButtonText}</span>
        )}
      </button>

      {/* 검열 완료된 이미지 목록 */}
      {uploadedImages.length > 0 && (
        <div className="space-y-3 mt-6">
          <h3 className="text-sm font-medium text-gray-300 flex flex-col sm:flex-row items-start sm:items-center gap-2 mobile-text">
            <span>✅ 검열 완료된 이미지:</span>
            <span className="text-xs bg-green-600/20 text-green-400 px-2 py-1 rounded-full border border-green-500/30 mobile-caption">
              안전한 이미지만 표시됨
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
                  className="absolute -top-2 -right-2 bg-red-600 hover:bg-red-700 text-white rounded-full w-7 h-7 flex items-center justify-center text-sm font-bold shadow-lg z-20 transition-all duration-200 transform hover:scale-110"
                >
                  ×
                </button>
                <div className="absolute bottom-0 left-0 right-0 bg-black/70 text-white text-xs p-1 rounded-b-lg truncate mobile-caption">
                  {image.originalName}
                </div>
                <div className="absolute top-1 left-1 bg-green-600/90 text-white text-xs px-2 py-1 rounded flex items-center gap-1 mobile-caption shadow-sm z-10">
                  ✓ 검열완료
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </form>
  );
}