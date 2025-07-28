import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import '@toast-ui/editor/dist/theme/toastui-editor-dark.css';
import '../styles/tui-editor.css';
import { uploadImage } from '../lib/s3Upload';

// 이미지 URL 정규식
const IMAGE_URL_REGEX = /^https?:\/\/.+\.(jpg|jpeg|png|gif|webp)(\?.*)?$/i;

// 이미지 URL 검증 함수
const isValidImageUrl = (url: string): boolean => {
  return IMAGE_URL_REGEX.test(url);
};

// 이미지 업로드 이벤트 처리 여부를 확인하기 위한 전역 플래그
export const isHandlingImageUpload = { current: false };

interface TuiEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  height?: string;
  handleImageUpload?: () => void;
}

const TuiEditor = forwardRef(({ 
  value, 
  onChange, 
  placeholder = "Enter content...", 
  height = "500px",
  handleImageUpload 
}: TuiEditorProps, ref) => {
  const editorRef = useRef<any>(null);

  // 외부에서 ref를 통해 에디터에 접근할 수 있도록 설정
  useImperativeHandle(ref, () => ({
    getInstance: () => editorRef.current?.getInstance(),
    insertContent: (content: string) => {
      const instance = editorRef.current?.getInstance();
      if (instance) {
        instance.insertText(content);
      }
    },
    getMarkdown: () => {
      const instance = editorRef.current?.getInstance();
      return instance ? instance.getMarkdown() : '';
    },
    getHTML: () => {
      const instance = editorRef.current?.getInstance();
      return instance ? instance.getHTML() : '';
    }
  }));

  // 초기 값 설정
  useEffect(() => {
    const instance = editorRef.current?.getInstance();
    if (instance && value !== instance.getMarkdown()) {
      instance.setMarkdown(value);
    }
  }, [value]);

  // 에디터 변경 이벤트 핸들러
  const handleChange = () => {
    const instance = editorRef.current?.getInstance();
    if (instance) {
      // HTML 대신 마크다운 형식으로 저장하여 이미지가 정상적으로 표시되도록 함
      const newContent = instance.getMarkdown();
      onChange(newContent);
    }
  };

  // 이미지 업로드 핸들러 설정
  const onUploadImage = async (blob: Blob, callback: Function) => {
    try {
      // 이미지 업로드 처리 중임을 표시
      isHandlingImageUpload.current = true;
      
      // 검열 중 표시를 위한 이벤트 발생
      const imageValidatingEvent = new CustomEvent('imageValidating', { detail: { validating: true } });
      document.dispatchEvent(imageValidatingEvent);
      
      // 부적절한 이미지 감지 시 파일 업로드 창이 뜨지 않도록 일정 시간 플래그 유지
      const resetTimer = setTimeout(() => {
        isHandlingImageUpload.current = false;
      }, 2000); // 2초 후 플래그 초기화
      
      // 클립보드에서 붙여넣기된 이미지인 경우 직접 처리
      if (blob.type.startsWith('image/')) {
        // Blob을 File로 변환
        const file = new File([blob], `clipboard-image-${Date.now()}.${blob.type.split('/')[1] || 'png'}`, {
          type: blob.type
        });
        
        try {
          // 이미지 업로드 처리 (검열 과정 적용)
          console.log('클립보드 이미지 검열 시작...');
          
          // 직접 서버로 이미지 전송하여 검열 처리
          const formData = new FormData();
          formData.append('file', file);
          formData.append('needsModeration', 'true');
          
          // JWT 토큰 가져오기
          const token = localStorage.getItem('accessToken');
          
          const moderationResponse = await fetch('/api/files/moderate-direct', {
            method: 'POST',
            headers: {
              'Authorization': `Bearer ${token}`
            },
            body: formData
          });
          
          if (!moderationResponse.ok) {
            // alert 제거하고 오류만 발생시킴
            throw new Error('이미지 검열 실패: ' + moderationResponse.statusText);
          }
          
          const moderationResult = await moderationResponse.json();
          console.log('클립보드 이미지 검열 결과:', moderationResult);
          
          // 부적절한 이미지인 경우 예외 발생 - alert 한 번만 표시
          if (moderationResult.data && moderationResult.data.isSafe === false) {
            // alert 한 번만 표시
            alert('부적절한 이미지가 감지되었습니다. 다른 이미지를 사용해주세요.');
            throw new Error('부적절한 이미지가 감지되었습니다. 다른 이미지를 사용해주세요.');
          }
          
          // 검열 통과한 이미지 정보 추출
          const imageData = moderationResult.data;
          if (!imageData || !imageData.url) {
            // alert 제거하고 오류만 발생시킴
            throw new Error('이미지 URL을 받지 못했습니다.');
          }
          
          // URL에 붙어있는 불필요한 텍스트 제거
          let cleanUrl = imageData.url;
          if (cleanUrl.includes('링크그대로만뜨고') || cleanUrl.includes('이미지가 안뜨')) {
            const urlParts = cleanUrl.split('.');
            const extension = urlParts[urlParts.length - 1].toLowerCase();
            if (['jpg', 'jpeg', 'png', 'gif', 'webp'].some(ext => extension.startsWith(ext))) {
              const extensionEndIndex = cleanUrl.lastIndexOf('.' + extension) + extension.length + 1;
              cleanUrl = cleanUrl.substring(0, extensionEndIndex);
              console.log('수정된 URL:', cleanUrl);
            }
          }
          
          // URL 검증
          if (!isValidImageUrl(cleanUrl)) {
            console.error('유효하지 않은 이미지 URL:', cleanUrl);
            throw new Error('유효하지 않은 이미지 URL입니다.');
          }
          
          // 콜백으로 URL 전달 - 이미지 삽입
          callback(cleanUrl, '검열 통과된 이미지');
          console.log('이미지 업로드 및 검열 성공:', cleanUrl);
          return true;
        } catch (error: any) {
          console.error('클립보드 이미지 업로드 및 검열 오류:', error);
          // alert 제거 - 오류만 발생시킴
          throw error; // 오류를 위로 전파하여 상위 핸들러에서 처리하도록 함
        }
      }
      
      // 버튼을 통한 업로드인 경우 커스텀 핸들러 호출
      if (handleImageUpload) {
        handleImageUpload();
      }
    } catch (error: any) {
      console.error('이미지 업로드 오류:', error);
      // alert 제거 - 오류 로그만 출력
    } finally {
      // 이미지 업로드 처리 완료 표시
      // 오류 발생 시에도 플래그를 유지하여 파일 업로드 창이 뜨지 않도록 처리
      // 플래그는 위에서 설정한 타이머에 의해 자동으로 초기화됨
      
      // 검열 완료 표시를 위한 이벤트 발생
      const imageValidatingEvent = new CustomEvent('imageValidating', { detail: { validating: false } });
      document.dispatchEvent(imageValidatingEvent);
    }
    // 실제 업로드는 handleImageUpload에서 처리하므로 여기서는 취소
    // 부적절한 이미지 감지 시 파일 업로드 창이 뜨지 않도록 처리
    if (isHandlingImageUpload.current) {
      return true; // 이미 처리중인 경우 기본 업로드 창 방지
    }
    return false;
  };

  return (
    <Editor
      ref={editorRef}
      initialValue={value || ''}
      placeholder={placeholder}
      previewStyle="vertical"
      height={height}
      initialEditType="wysiwyg"
      useCommandShortcut={true}
      usageStatistics={false}
      onChange={handleChange}
      theme="dark"
      hooks={{
        addImageBlobHook: onUploadImage
      }}
      toolbarItems={[
        ['heading', 'bold', 'italic', 'strike'],
        ['hr', 'quote'],
        ['ul', 'ol', 'task', 'indent', 'outdent'],
        ['table', 'link'],
        ['code', 'codeblock'],
        ['scrollSync'],
      ]}
    />
  );
});

export default TuiEditor;