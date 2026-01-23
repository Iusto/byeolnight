// TuiEditor.tsx
// Toast UI Editor 커스텀 래퍼 컴포넌트 - 다국어 placeholder 반영 및 이미지 검열 업로드 처리

import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef } from 'react';
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

    // 전역 에디터 참조 설정 (커스텀 버튼에서 사용)
    (window as any).currentEditor = instance;
  }, [value]);

  // 이미지 삭제 시 확인 다이얼로그
  useEffect(() => {
    const editorEl = editorRef.current?.getRootElement();
    if (!editorEl) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      // Delete 또는 Backspace 키
      if (e.key === 'Delete' || e.key === 'Backspace') {
        const selection = window.getSelection();
        if (!selection || selection.rangeCount === 0) return;

        // 선택된 요소 또는 커서 위치의 요소 확인
        const range = selection.getRangeAt(0);
        let targetNode: Node | null = range.startContainer;

        // 이미지가 선택되었는지 확인
        let imageToDelete: HTMLImageElement | null = null;

        // 직접 이미지를 선택한 경우
        if (targetNode.nodeName === 'IMG') {
          imageToDelete = targetNode as HTMLImageElement;
        }
        // 부모 요소에서 이미지 확인
        else if (targetNode.parentElement) {
          const parent = targetNode.parentElement;
          // 이미지 바로 앞/뒤에 커서가 있는 경우
          const img = parent.querySelector('img');
          if (img && range.collapsed) {
            // Backspace: 커서 바로 앞에 이미지가 있는지 확인
            if (e.key === 'Backspace') {
              const prevSibling = range.startContainer.previousSibling ||
                (range.startOffset > 0 ? null : targetNode.parentElement?.previousSibling);
              if (prevSibling?.nodeName === 'IMG') {
                imageToDelete = prevSibling as HTMLImageElement;
              }
            }
            // Delete: 커서 바로 뒤에 이미지가 있는지 확인
            else if (e.key === 'Delete') {
              const nextSibling = range.startContainer.nextSibling ||
                targetNode.parentElement?.nextSibling;
              if (nextSibling?.nodeName === 'IMG') {
                imageToDelete = nextSibling as HTMLImageElement;
              }
            }
          }
        }

        // 선택 영역에 이미지가 포함된 경우
        if (!imageToDelete && !range.collapsed) {
          const fragment = range.cloneContents();
          const imgInSelection = fragment.querySelector('img');
          if (imgInSelection) {
            imageToDelete = editorEl.querySelector(`img[src="${imgInSelection.src}"]`);
          }
        }

        // 이미지 삭제 확인
        if (imageToDelete) {
          e.preventDefault();
          e.stopPropagation();

          const confirmed = window.confirm('이미지를 삭제하시겠습니까?');
          if (confirmed) {
            // 확인 시 이미지 삭제
            imageToDelete.remove();
            // 에디터 내용 동기화
            const instance = editorRef.current?.getInstance();
            if (instance) {
              onChange(instance.getMarkdown());
            }
          }
        }
      }
    };

    // WYSIWYG 에디터 영역에 이벤트 리스너 추가
    const wwEditor = editorEl.querySelector('.toastui-editor-ww-container');
    if (wwEditor) {
      wwEditor.addEventListener('keydown', handleKeyDown as EventListener, true);
    }

    return () => {
      if (wwEditor) {
        wwEditor.removeEventListener('keydown', handleKeyDown as EventListener, true);
      }
    };
  }, [onChange]);

  // 마크다운 변경 시 외부 콜백 호출
  const handleChange = () => {
    const instance = editorRef.current?.getInstance();
    if (instance) {
      const newContent = instance.getMarkdown();
      onChange(newContent);
    }
  };

  // Toast UI Editor의 addImageBlobHook은 동기적으로 반환값을 확인함
  // async 함수는 Promise를 반환하므로 truthy로 처리되어 기본 동작이 수행됨
  // 따라서 동기적으로 false를 반환하고, 비동기 처리는 별도로 수행
  const onUploadImage = (blob: Blob, callback: (url: string, altText: string) => void) => {
    // 비동기 업로드 처리
    handleImageUploadAsync(blob, callback);
    // 동기적으로 false 반환하여 기본 동작(파일명 삽입) 방지
    return false;
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
        // 업로드 성공 시에만 callback 호출하여 이미지 삽입
        callback(imageData.url, imageData.originalName || '업로드된 이미지');
      } else {
        throw new Error('이미지 업로드 실패');
      }
    } catch (error: unknown) {
      console.error('이미지 업로드 오류:', error);
      const errorMessage = getErrorMessage(error);

      // 부적절한 이미지 에러인 경우 명확한 경고 메시지 표시
      if (isInappropriateImageError(errorMessage)) {
        alert('⚠️ 부적절한 이미지가 감지되었습니다.\n\n이 이미지는 커뮤니티 가이드라인에 위배되어 업로드할 수 없습니다.\n다른 이미지를 사용해주세요.');
      } else {
        alert(errorMessage);
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