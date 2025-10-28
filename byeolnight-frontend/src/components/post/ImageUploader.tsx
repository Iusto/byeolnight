import { useState, useRef, useEffect } from 'react';
import { uploadImage } from '../lib/s3Upload';
import { isHandlingImageUpload } from './TuiEditor';

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

interface ValidationAlert {
  message: string;
  type: 'success' | 'error' | 'warning';
  imageUrl?: string;
}

interface ImageUploaderProps {
  uploadedImages: FileDto[];
  setUploadedImages: React.Dispatch<React.SetStateAction<FileDto[]>>;
  onImageInsert: (imageData: FileDto | string, altText: string) => void;
  isImageValidating: boolean;
  setIsImageValidating: React.Dispatch<React.SetStateAction<boolean>>;
  validationAlert: ValidationAlert | null;
  setValidationAlert: React.Dispatch<React.SetStateAction<ValidationAlert | null>>;
}

export default function ImageUploader({
  uploadedImages,
  setUploadedImages,
  onImageInsert,
  isImageValidating,
  setIsImageValidating,
  validationAlert,
  setValidationAlert
}: ImageUploaderProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 모바일 환경 감지 함수
  const isMobile = () => {
    return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
  };

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
        if (!exists) {
          console.log('검열 통과된 클립보드 이미지 추가:', imageData.originalName);
          return [...prev, imageData];
        }
        return prev;
      });
      
      return imageData;
    } catch (error: any) {
      console.error('클립보드 이미지 업로드 오류:', {
        message: error.message,
        name: error.name,
        stack: error.stack,
        response: error.response?.data
      });
      
      let errorMsg = error.message || '이미지 검열 실패: 부적절한 이미지가 감지되었습니다.';
      let alertType: 'error' | 'warning' = 'error';
      
      // 네트워크 오류에 대한 추가 안내
      if (error.message?.includes('네트워크') || error.message?.includes('브라우저 보안')) {
        alertType = 'warning';
        errorMsg += '\n\n💡 해결 방법: 다른 브라우저를 사용하거나 시크릿 모드를 시도해보세요.';
      }
      
      // 오류 메시지 표시 (alert 대신 ValidationAlert만 사용)
      setValidationAlert({
        message: errorMsg,
        type: alertType
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
            
            // 검열 통과한 이미지를 에디터에 삽입
            onImageInsert(imageData, '클립보드 이미지');
            
            // 성공 알림 표시
            setValidationAlert({
              message: '클립보드 이미지가 성공적으로 업로드되었습니다.',
              type: 'success'
            });
            
            // 3초 후 알림 자동 제거
            setTimeout(() => {
              setValidationAlert(null);
            }, 3000);
          } catch (error: any) {
            console.error('클립보드 이미지 업로드 실패:', {
              message: error.message,
              name: error.name,
              stack: error.stack,
              response: error.response?.data
            });
            
            // 파일 입력 초기화 (동일한 파일 재선택 가능하도록)
            if (fileInputRef.current) {
              fileInputRef.current.value = '';
            }
            
            let errorMsg = error.message || '이미지 검열 실패: 부적절한 이미지가 감지되었습니다.';
            let alertType: 'error' | 'warning' = 'error';
            
            // 네트워크 오류에 대한 추가 안내
            if (error.message?.includes('네트워크') || error.message?.includes('브라우저 보안')) {
              alertType = 'warning';
              errorMsg += '\n\n💡 해결 방법: 다른 브라우저를 사용하거나 시크릿 모드를 시도해보세요.';
            }
            
            // ValidationAlert로 표시하고 alert 제거
            setValidationAlert({
              message: errorMsg,
              type: alertType
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

  const handleImageUpload = () => {
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
      onImageInsert(imageData, imageData.originalName || '검열 통과된 이미지');
      
      // 성공 알림 표시
      setValidationAlert({
        message: '이미지가 성공적으로 업로드되었습니다.',
        type: 'success'
      });
      
      // 3초 후 알림 자동 제거
      setTimeout(() => {
        setValidationAlert(null);
      }, 3000);
      
    } catch (error: any) {
      console.error('이미지 업로드 오류:', {
        message: error.message,
        name: error.name,
        stack: error.stack,
        response: error.response?.data
      });
      
      let errorMsg = error.message || '이미지 업로드에 실패했습니다.';
      let alertType: 'error' | 'warning' = 'error';
      
      // 네트워크 오류에 대한 추가 안내
      if (error.message?.includes('네트워크') || error.message?.includes('브라우저 보안')) {
        alertType = 'warning';
        errorMsg += '\n\n💡 해결 방법: 다른 브라우저를 사용하거나 시크릿 모드를 시도해보세요.';
      }
      
      // 오류 메시지 표시 - alert 제거하고 ValidationAlert만 사용
      setValidationAlert({
        message: errorMsg,
        type: alertType
      });
    } finally {
      setIsImageValidating(false);
      // 파일 입력 초기화 (동일한 파일 재선택 가능하도록)
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };



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

  return (
    <div className="space-y-4">
      {/* 파일 선택 입력 요소 - 화면에 보이지 않지만 React에서 관리 */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        onChange={handleFileChange}
        style={{ display: 'none' }}
      />

      {/* 이미지 업로드 버튼 영역 */}
      <div className="flex gap-2 w-full sm:w-auto">
        <button
          type="button"
          onClick={handleImageUpload}
          disabled={isImageValidating}
          className="mobile-button touch-target touch-feedback flex items-center justify-center gap-2 px-3 py-2 sm:px-4 sm:py-3 bg-blue-600/80 active:bg-blue-600 mouse:hover:bg-blue-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white rounded-xl text-xs sm:text-sm font-medium transition-all duration-200 shadow-lg mouse:hover:shadow-blue-500/25 transform active:scale-95 mouse:hover:scale-105 disabled:transform-none flex-1 sm:flex-none min-w-[120px]"
        >
          {isImageValidating ? (
            <>
              <div className="animate-spin w-3 h-3 sm:w-4 sm:h-4 border-2 border-white border-t-transparent rounded-full"></div>
              <span className="mobile-caption">검열 중</span>
            </>
          ) : (
            <>
              <span className="text-sm sm:text-base">🖼️</span>
              <span className="mobile-caption">이미지 추가</span>
            </>
          )}
        </button>
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


    </div>
  );
}