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

  // 이미지 업로드 처리 함수
  const onUploadImage = async (blob: Blob, callback: Function) => {
    try {
      isHandlingImageUpload.current = true;

      document.dispatchEvent(new CustomEvent('imageValidating', { detail: { validating: true } }));
      const resetTimer = setTimeout(() => {
        isHandlingImageUpload.current = false;
      }, 2000);

      if (blob.type.startsWith('image/')) {
        const file = new File([blob], `clipboard-image-${Date.now()}.${blob.type.split('/')[1] || 'png'}`, {
          type: blob.type
        });

        try {
          const formData = new FormData();
          formData.append('file', file);
          formData.append('needsModeration', 'true');
          const token = localStorage.getItem('accessToken');

          const moderationResponse = await fetch('/api/files/moderate-direct', {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
            body: formData
          });

          if (!moderationResponse.ok) {
            throw new Error('이미지 검열 실패: ' + moderationResponse.statusText);
          }

          const moderationResult = await moderationResponse.json();
          if (moderationResult.data?.isSafe === false) {
            alert('부적절한 이미지가 감지되었습니다. 다른 이미지를 사용해주세요.');
            throw new Error('부적절한 이미지 감지');
          }

          let cleanUrl = moderationResult.data.url;
          const extension = cleanUrl.split('.').pop()?.toLowerCase();
          if (['jpg', 'jpeg', 'png', 'gif', 'webp'].some(ext => extension?.startsWith(ext))) {
            const endIdx = cleanUrl.lastIndexOf('.' + extension) + extension.length + 1;
            cleanUrl = cleanUrl.substring(0, endIdx);
          }

          if (!isValidImageUrl(cleanUrl)) throw new Error('유효하지 않은 이미지 URL');

          callback(cleanUrl, '검열 통과된 이미지');
          return true;
        } catch (error) {
          console.error('이미지 업로드 검열 오류:', error);
          throw error;
        }
      }

      if (handleImageUpload) handleImageUpload();
    } catch (error) {
      console.error('이미지 업로드 오류:', error);
    } finally {
      document.dispatchEvent(new CustomEvent('imageValidating', { detail: { validating: false } }));
    }

    if (isHandlingImageUpload.current) return true;
    return false;
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