import { useRef, useEffect, forwardRef, useImperativeHandle, useState } from 'react';
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';
import '../styles/quill-editor.css';

interface QuillEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  handleImageUpload?: () => void;
}

const QuillEditor = forwardRef(({ value, onChange, placeholder = "내용을 입력하세요...", handleImageUpload }: QuillEditorProps, ref) => {
  const editorRef = useRef<any>(null);
  const [isReady, setIsReady] = useState(false);
  const [key, setKey] = useState(Date.now()); // 에디터 강제 리렌더링을 위한 키

  // 외부에서 ref를 통해 에디터에 접근할 수 있도록 설정
  useImperativeHandle(ref, () => ({
    getEditor: () => editorRef.current?.getEditor(),
    focus: () => editorRef.current?.focus(),
    resetEditor: () => setKey(Date.now()) // 에디터 리셋 함수 추가
  }));

  // 컴포넌트가 마운트된 후 에디터 초기화
  useEffect(() => {
    // 에디터 초기화 지연 처리로 DOMNodeInserted 경고 방지
    const timer = setTimeout(() => {
      setIsReady(true);
      if (editorRef.current) {
        console.log('Quill 에디터 초기화 완료');
        
        try {
          // MutationObserver를 사용하여 DOM 변경 감지 (DOMNodeInserted 대체)
          const editor = editorRef.current.getEditor();
          if (editor && editor.container) {
            const targetNode = editor.container;
            const observer = new MutationObserver(() => {
              // DOM 변경 감지 시 필요한 작업
            });
            
            observer.observe(targetNode, { childList: true, subtree: true });
            return () => observer.disconnect();
          }
        } catch (error) {
          console.error('에디터 초기화 중 오류:', error);
        }
      }
    }, 200); // 지연 시간 증가
    
    return () => clearTimeout(timer);
  }, [key]); // key가 변경될 때마다 에디터 재초기화

  // 값이 변경될 때 에디터 참조 유지 확인
  useEffect(() => {
    if (editorRef.current && value !== undefined) {
      console.log('에디터 값 업데이트 확인');
    }
  }, [value]);

  // 에디터 모듈 설정
  const modules = {
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
          try {
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
          } catch (error) {
            console.error('비디오 삽입 중 오류:', error);
            alert('비디오 삽입 중 오류가 발생했습니다. 다시 시도해주세요.');
          }
        }
      }
    },
    clipboard: {
      matchVisual: false
    }
  };

  // 에디터 포맷 설정
  const formats = [
    'header', 'bold', 'italic', 'underline', 'strike',
    'color', 'background', 'list', 'bullet', 'align',
    'link', 'image', 'video', 'iframe'
  ];

  if (!isReady) {
    return (
      <div className="quill-editor-loading" style={{
        height: '300px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#1f2336',
        borderRadius: '0.75rem',
        border: '1px solid #4a5568'
      }}>
        <div style={{ color: '#a0aec0' }}>에디터 로딩 중...</div>
      </div>
    );
  }

  return (
    <ReactQuill
      key={key} // 강제 리렌더링을 위한 키 추가
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
      modules={modules}
      formats={formats}
      placeholder={placeholder}
      preserveWhitespace={true}
    />
  );
});

export default QuillEditor;