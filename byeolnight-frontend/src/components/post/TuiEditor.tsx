// TuiEditor.tsx
// Toast UI Editor 커스텀 래퍼 컴포넌트 - 다국어 placeholder 반영 및 이미지 검열 업로드 처리

import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import '@toast-ui/editor/dist/theme/toastui-editor-dark.css';
import '../../styles/tui-editor.css';
import { uploadImage } from '../../lib/s3Upload';
import { useTranslation } from 'react-i18next';
import { getErrorMessage } from '../../types/api';

// 이미지 URL 정규식
const IMAGE_URL_REGEX = /^https?:\/\/.+\.(jpg|jpeg|png|gif|webp|svg|bmp)(\?.*)?$/i;

const isValidImageUrl = (url: string): boolean => {
  return IMAGE_URL_REGEX.test(url);
};

export const isHandlingImageUpload = { current: false };

// 부적절한 이미지 관련 에러 메시지 감지
const isInappropriateImageError = (message: string): boolean => {
  return message.includes('부적절한') ||
         message.includes('검열') ||
         message.includes('inappropriate') ||
         message.includes('isSafe');
};



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
  const { t } = useTranslation();
  const editorRef = useRef<any>(null);
  const previousMarkdownRef = useRef<string>('');
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
    // 전역 에디터 참조 설정 (커스텀 버튼에서 사용)
    (window as any).currentEditor = instance;
  }, [value]);

  // 이미지 선택 상태에서 삭제 시 확인 다이얼로그
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key !== 'Delete' && e.key !== 'Backspace') return;

      const editorEl = editorRef.current?.getRootElement();
      if (!editorEl) return;

      // 선택된 이미지 확인 (Toast UI Editor는 ProseMirror 기반)
      const selectedImage = editorEl.querySelector('.toastui-editor-contents img.ProseMirror-selectednode');
      if (!selectedImage) return;

      // 이미지가 선택된 상태에서 삭제 시도
      e.preventDefault();
      e.stopPropagation();

      const confirmed = window.confirm('이미지를 삭제하시겠습니까?');
      if (confirmed) {
        selectedImage.remove();
        const instance = editorRef.current?.getInstance();
        if (instance) {
          onChange(instance.getMarkdown());
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown, true);
    return () => document.removeEventListener('keydown', handleKeyDown, true);
  }, [onChange]);

  // 마크다운 변경 시 외부 콜백 호출
  const handleChange = () => {
    const instance = editorRef.current?.getInstance();
    if (instance) {
      onChange(instance.getMarkdown());
    }
  };

  // 이미지 업로드 훅
  const onUploadImage = (blob: Blob, callback: (url: string, altText: string) => void) => {
    // 업로드 전 에디터 상태 저장
    const instance = editorRef.current?.getInstance();
    const prevMarkdown = instance?.getMarkdown() || '';
    previousMarkdownRef.current = prevMarkdown;

    handleImageUploadAsync(blob, callback);
    return false; // 기본 동작(파일명 삽입) 방지
  };

  const handleImageUploadAsync = async (blob: Blob, callback: (url: string, altText: string) => void) => {
    try {
      isHandlingImageUpload.current = true;
      document.dispatchEvent(new CustomEvent('imageValidating', { detail: { validating: true } }));

      if (blob.size > 10 * 1024 * 1024) {
        alert('파일 크기는 10MB를 초과할 수 없습니다.');
        return;
      }

      if (!blob.type.startsWith('image/')) {
        alert('이미지 파일만 업로드 가능합니다.');
        return;
      }

      const file = new File([blob], `clipboard-image-${Date.now()}.${blob.type.split('/')[1] || 'png'}`, {
        type: blob.type
      });

      const imageData = await uploadImage(file);

      if (imageData?.url && isValidImageUrl(imageData.url)) {
        callback(imageData.url, imageData.originalName || '업로드된 이미지');
      } else {
        throw new Error('이미지 업로드 실패');
      }
    } catch (error: unknown) {
      const errorMessage = getErrorMessage(error);

      // 에러 시 에디터에 잘못 삽입된 내용 복원
      const instance = editorRef.current?.getInstance();
      if (instance && previousMarkdownRef.current) {
        const currentMarkdown = instance.getMarkdown();
        if (currentMarkdown !== previousMarkdownRef.current) {
          instance.setMarkdown(previousMarkdownRef.current);
          onChange(previousMarkdownRef.current);
        }
      }

      // 에러 메시지 표시
      alert(errorMessage || '이미지 업로드에 실패했습니다.');
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
        ['ul', 'ol', 'task'],
        ['table'],
        ['scrollSync'],
      ]}
    />
  );
});

export default TuiEditor;