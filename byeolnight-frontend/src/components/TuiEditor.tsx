import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import '@toast-ui/editor/dist/theme/toastui-editor-dark.css';
import '../styles/tui-editor.css';
import { uploadImage } from '../lib/s3Upload';

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
  placeholder = "내용을 입력하세요...", 
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
      const newContent = instance.getHTML();
      onChange(newContent);
    }
  };

  // 이미지 업로드 핸들러 설정
  const onUploadImage = async (blob: Blob, callback: Function) => {
    try {
      // 이미지 업로드 처리 중임을 표시
      isHandlingImageUpload.current = true;
      
      // 클립보드에서 붙여넣기된 이미지인 경우 직접 처리
      if (blob.type.startsWith('image/')) {
        // Blob을 File로 변환
        const file = new File([blob], `clipboard-image-${Date.now()}.${blob.type.split('/')[1] || 'png'}`, {
          type: blob.type
        });
        
        try {
          // 이미지 업로드 처리
          const imageData = await uploadImage(file, false);
          if (imageData && imageData.url) {
            // 콜백으로 URL 전달 - 마크다운 형식으로 삽입
            callback(imageData.url, '클립보드 이미지');
            return true;
          }
        } catch (error) {
          console.error('클립보드 이미지 업로드 오류:', error);
        }
      }
      
      // 버튼을 통한 업로드인 경우 커스텀 핸들러 호출
      if (handleImageUpload) {
        handleImageUpload();
      }
    } catch (error) {
      console.error('이미지 업로드 오류:', error);
    } finally {
      // 이미지 업로드 처리 완료 표시
      setTimeout(() => {
        isHandlingImageUpload.current = false;
      }, 100); // 약간의 지연을 주어 이벤트 처리 순서 문제 방지
    }
    // 실제 업로드는 handleImageUpload에서 처리하므로 여기서는 취소
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