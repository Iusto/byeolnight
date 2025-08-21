// TuiEditor.tsx
// Toast UI Editor 커스텀 래퍼 컴포넌트 - 다국어 placeholder 반영 및 이미지 검열 업로드 처리

import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import '@toast-ui/editor/dist/theme/toastui-editor-dark.css';
import '../styles/tui-editor.css';
import { uploadImage } from '../lib/s3Upload';
import { useTranslation } from 'react-i18next';

// 이미지 URL 정규식 (업로드 후 유효성 검사용)
const IMAGE_URL_REGEX = /^https?:\/\/.+\.(jpg|jpeg|png|gif|webp)(\?.*)?$/i;

// 이미지 URL 유효성 검사 함수
const isValidImageUrl = (url: string): boolean => {
  return IMAGE_URL_REGEX.test(url);
};

// 이미지 업로드 처리 여부를 전역 플래그로 표시
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
  placeholder,
  height = "500px",
  handleImageUpload 
}: TuiEditorProps, ref) => {
  const { t, i18n } = useTranslation();
  const editorRef = useRef<any>(null);
  const effectivePlaceholder = placeholder ?? t('home.content_placeholder');

  // 외부에서 ref로 에디터 인스턴스 접근 허용
  useImperativeHandle(ref, () => ({
    getInstance: () => editorRef.current?.getInstance(),
    insertContent: (content: string) => {
      const instance = editorRef.current?.getInstance();
      if (instance) instance.insertText(content);
    },
    getMarkdown: () => editorRef.current?.getInstance()?.getMarkdown() || '',
    getHTML: () => editorRef.current?.getInstance()?.getHTML() || ''
  }));

  // 초기 마크다운 값 설정
  useEffect(() => {
    const instance = editorRef.current?.getInstance();
    if (instance && value !== instance.getMarkdown()) {
      instance.setMarkdown(value);
    }
  }, [value]);

  // 마크다운 변경 시 외부 콜백 호출
  const handleChange = () => {
    const instance = editorRef.current?.getInstance();
    if (instance) {
      const newContent = instance.getMarkdown();
      onChange(newContent);
    }
  };

  // 이미지 업로드 처리 함수 - 간소화 및 안정성 개선
  const onUploadImage = async (blob: Blob, callback: Function) => {
    try {
      isHandlingImageUpload.current = true;
      document.dispatchEvent(new CustomEvent('imageValidating', { detail: { validating: true } }));

      // 파일 크기 체크 (10MB 제한)
      if (blob.size > 10 * 1024 * 1024) {
        alert('파일 크기는 10MB를 초과할 수 없습니다.');
        return false;
      }

      // 이미지 타입 체크
      if (!blob.type.startsWith('image/')) {
        alert('이미지 파일만 업로드 가능합니다.');
        return false;
      }

      const file = new File([blob], `clipboard-image-${Date.now()}.${blob.type.split('/')[1] || 'png'}`, {
        type: blob.type
      });

      console.log('TUI Editor 이미지 업로드 시작:', {
        fileName: file.name,
        fileSize: file.size,
        fileType: file.type,
        userAgent: navigator.userAgent
      });

      // 통합된 uploadImage 함수 사용
      const imageData = await uploadImage(file);
      
      if (imageData && imageData.url && isValidImageUrl(imageData.url)) {
        console.log('TUI Editor 이미지 업로드 성공:', imageData.url);
        callback(imageData.url, imageData.originalName || '업로드된 이미지');
        return true;
      } else {
        throw new Error('이미지 업로드 실패: 유효하지 않은 응답');
      }
    } catch (error: any) {
      console.error('TUI Editor 이미지 업로드 오류:', {
        message: error.message,
        name: error.name,
        stack: error.stack,
        response: error.response?.data
      });
      
      // 사용자 친화적인 오류 메시지 표시
      let userMessage = error.message || '이미지 업로드에 실패했습니다.';
      
      // 특정 오류에 대한 추가 안내
      if (error.message?.includes('네트워크')) {
        userMessage += '\n\n💡 해결 방법:\n• 인터넷 연결을 확인해주세요\n• 다른 브라우저를 시도해보세요\n• 시크릿 모드를 사용해보세요';
      } else if (error.message?.includes('브라우저 보안')) {
        userMessage += '\n\n💡 해결 방법:\n• 다른 브라우저를 사용해보세요\n• 시크릿/프라이빗 모드를 시도해보세요\n• 브라우저 확장 프로그램을 비활성화해보세요';
      } else if (error.message?.includes('시간 초과')) {
        userMessage += '\n\n💡 해결 방법:\n• 이미지 크기를 줄여보세요\n• 잠시 후 다시 시도해주세요\n• 네트워크 연결을 확인해주세요';
      }
      
      alert(userMessage);
      return false;
    } finally {
      isHandlingImageUpload.current = false;
      document.dispatchEvent(new CustomEvent('imageValidating', { detail: { validating: false } }));
    }
  };

  return (
    <Editor
      ref={editorRef}
      initialValue={value || ''}
      placeholder={effectivePlaceholder}
      previewStyle="vertical"
      height={height}
      initialEditType="wysiwyg"
      useCommandShortcut={true}
      usageStatistics={false}
      onChange={handleChange}
      theme="dark"
      hooks={{ addImageBlobHook: onUploadImage }}
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