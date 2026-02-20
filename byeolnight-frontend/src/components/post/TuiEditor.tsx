// TuiEditor.tsx
// Toast UI Editor 커스텀 래퍼 컴포넌트 - 다국어 placeholder 반영 및 이미지 검열 업로드 처리

import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import '@toast-ui/editor/dist/theme/toastui-editor-dark.css';
import '../../styles/tui-editor.css';
import { uploadImage } from '../../lib/s3Upload';
import type { UploadedImageResponse } from '../../lib/s3Upload';
import { useTranslation } from 'react-i18next';
import { isValidImageUrl } from '../../utils/imageUtils';

export const isHandlingImageUpload = { current: false };

interface TuiEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  height?: string;
  handleImageUpload?: () => void;
  onImageUploaded?: (imageData: UploadedImageResponse) => void;
}

const TuiEditor = forwardRef(({
  value,
  onChange,
  placeholder,
  height = "500px",
  handleImageUpload,
  onImageUploaded
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
  }, [value]);

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
        onImageUploaded?.(imageData);
      } else {
        throw new Error('이미지 업로드 실패');
      }
    } catch {
      // 에러 시 에디터에 잘못 삽입된 내용 복원
      const instance = editorRef.current?.getInstance();
      if (instance && previousMarkdownRef.current) {
        const currentMarkdown = instance.getMarkdown();
        if (currentMarkdown !== previousMarkdownRef.current) {
          instance.setMarkdown(previousMarkdownRef.current);
          onChange(previousMarkdownRef.current);
        }
      }
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