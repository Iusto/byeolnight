import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import '@toast-ui/editor/dist/theme/toastui-editor-dark.css';
import '../styles/tui-editor.css';

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
    if (handleImageUpload) {
      // 이미지 업로드 버튼 클릭 시 커스텀 핸들러 호출
      handleImageUpload();
      // 실제 업로드는 handleImageUpload에서 처리하므로 여기서는 취소
      return false;
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