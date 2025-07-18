import { useRef, useEffect, forwardRef, useImperativeHandle } from 'react';
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';

interface QuillEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  handleImageUpload?: () => void;
}

const QuillEditor = forwardRef(({ value, onChange, placeholder = "내용을 입력하세요...", handleImageUpload }: QuillEditorProps, ref) => {
  const editorRef = useRef<any>(null);

  // 외부에서 ref를 통해 에디터에 접근할 수 있도록 설정
  useImperativeHandle(ref, () => ({
    getEditor: () => editorRef.current?.getEditor(),
    focus: () => editorRef.current?.focus()
  }));

  // 컴포넌트가 마운트된 후 에디터 초기화
  useEffect(() => {
    // 에디터가 로드된 후 필요한 초기화 작업
    if (editorRef.current) {
      console.log('Quill 에디터 초기화 완료');
    }
  }, []);

  return (
    <ReactQuill
      ref={editorRef}
      value={value}
      onChange={onChange}
      theme="snow"
      className="quill-editor"
      style={{ 
        flex: '1',
        display: 'flex',
        flexDirection: 'column',
        width: '100%',
        border: 'none'
      }}
      modules={{
        toolbar: {
          container: [
            [{ 'header': [1, 2, 3, false] }],
            ['bold', 'italic', 'underline', 'strike'],
            [{ 'color': [] }, { 'background': [] }],
            [{ 'list': 'ordered'}, { 'list': 'bullet' }],
            [{ 'align': [] }],
            ['link', 'image', 'video'],
            ['clean']
          ],
          handlers: {
            // 이미지 버튼 클릭 시 사용자 정의 함수 실행
            image: handleImageUpload,
            // 비디오 버튼 클릭 시 기본 동작 유지
            video: function() {
              const range = this.quill.getSelection();
              const url = prompt('YouTube 임베드 URL을 입력하세요 (예: https://www.youtube.com/embed/VIDEO_ID)');
              if (url) {
                // YouTube 임베드 URL 형식 검증
                if (url.includes('youtube.com/embed/')) {
                  this.quill.insertEmbed(range.index, 'video', url);
                } else {
                  alert('올바른 YouTube 임베드 URL을 입력해주세요 (https://www.youtube.com/embed/VIDEO_ID 형식)');
                }
              }
            }
          }
        },
        clipboard: {
          matchVisual: false
        }
      }}
      formats={[
        'header', 'bold', 'italic', 'underline', 'strike',
        'color', 'background', 'list', 'bullet', 'align',
        'link', 'image', 'video', 'iframe'
      ]}
      placeholder={placeholder}
    />
  );
});

export default QuillEditor;