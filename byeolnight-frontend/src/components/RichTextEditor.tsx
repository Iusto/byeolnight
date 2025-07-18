import { useRef, useMemo } from 'react';
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';
import axios from '../lib/axios';

interface RichTextEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

export default function RichTextEditor({ value, onChange, placeholder }: RichTextEditorProps) {
  const quillRef = useRef<ReactQuill>(null);

  const imageHandler = async () => {
    const input = document.createElement('input');
    input.setAttribute('type', 'file');
    input.setAttribute('accept', 'image/*');
    
    // 모바일에서 갤러리 접근을 위해 capture 속성 제거
    input.removeAttribute('capture');
    
    // 실제 DOM에 추가하여 모바일에서도 작동하도록 함
    document.body.appendChild(input);
    input.style.display = 'none';
    input.click();

    input.onchange = async () => {
      const file = input.files?.[0];
      if (!file) {
        document.body.removeChild(input);
        return;
      }

      // 파일 크기 체크 (5MB)
      if (file.size > 5 * 1024 * 1024) {
        alert('파일 크기는 5MB 이하만 업로드 가능합니다.');
        document.body.removeChild(input);
        return;
      }

      try {
        // 이미지 검열을 위해 /files/upload-image 엔드포인트 사용
        const formData = new FormData();
        formData.append('file', file);
        
        // 검열 API 호출
        const response = await axios.post('/files/upload-image', formData);
        
        if (!response.data || !response.data.data) {
          throw new Error('이미지 업로드 응답이 올바르지 않습니다.');
        }
        
        const imageData = response.data.data;
        const imageUrl = imageData.url;

        // Quill 에디터에 이미지 삽입
        const quill = quillRef.current?.getEditor();
        if (quill) {
          const range = quill.getSelection();
          quill.insertEmbed(range?.index || 0, 'image', imageUrl);
        }

      } catch (error: any) {
        console.error('이미지 업로드 실패:', error);
        const errorMsg = error.response?.data?.message || '이미지 업로드에 실패했습니다.';
        alert(errorMsg);
      } finally {
        // DOM에서 제거
        document.body.removeChild(input);
      }
    };
  };

  const modules = useMemo(() => ({
    toolbar: {
      container: [
        [{ 'header': [1, 2, 3, false] }],
        ['bold', 'italic', 'underline', 'strike'],
        [{ 'color': [] }, { 'background': [] }],
        [{ 'list': 'ordered'}, { 'list': 'bullet' }],
        [{ 'align': [] }],
        ['link', 'image'],
        ['clean']
      ],
      handlers: {
        image: imageHandler
      }
    },
    clipboard: {
      matchVisual: false,
    }
  }), []);

  const formats = [
    'header',
    'bold', 'italic', 'underline', 'strike',
    'color', 'background',
    'list', 'bullet',
    'align',
    'link', 'image'
  ];

  return (
    <div className="rich-text-editor">
      <style jsx global>{`
        .ql-toolbar {
          background: #1f2336 !important;
          border: 1px solid #4a5568 !important;
          border-bottom: none !important;
          border-radius: 8px 8px 0 0 !important;
        }
        
        .ql-toolbar .ql-stroke {
          fill: none !important;
          stroke: #e2e8f0 !important;
        }
        
        .ql-toolbar .ql-fill {
          fill: #e2e8f0 !important;
          stroke: none !important;
        }
        
        .ql-toolbar .ql-picker-label {
          color: #e2e8f0 !important;
        }
        
        .ql-toolbar button:hover {
          background: #2a2e45 !important;
        }
        
        .ql-toolbar button.ql-active {
          background: #553c9a !important;
        }
        
        .ql-container {
          background: #2a2e45 !important;
          border: 1px solid #4a5568 !important;
          border-radius: 0 0 8px 8px !important;
          color: #e2e8f0 !important;
        }
        
        .ql-editor {
          color: #e2e8f0 !important;
          min-height: 200px !important;
        }
        
        .ql-editor.ql-blank::before {
          color: #a0aec0 !important;
          font-style: normal !important;
        }
        
        .ql-snow .ql-picker-options {
          background: #1f2336 !important;
          border: 1px solid #4a5568 !important;
        }
        
        .ql-snow .ql-picker-item:hover {
          background: #2a2e45 !important;
          color: #e2e8f0 !important;
        }
      `}</style>
      
      <ReactQuill
        ref={quillRef}
        theme="snow"
        value={value}
        onChange={onChange}
        modules={modules}
        formats={formats}
        placeholder={placeholder}
      />
    </div>
  );
}