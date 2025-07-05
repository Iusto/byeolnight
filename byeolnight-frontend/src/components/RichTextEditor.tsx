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
    input.click();

    input.onchange = async () => {
      const file = input.files?.[0];
      if (!file) return;

      // 파일 크기 체크 (10MB)
      if (file.size > 10 * 1024 * 1024) {
        alert('파일 크기는 10MB 이하만 업로드 가능합니다.');
        return;
      }

      try {
        // S3 Presigned URL 요청
        const presignedRes = await axios.post('/member/files/presigned-url', {
          filename: file.name
        });

        const { url: presignedUrl } = presignedRes.data;

        // S3에 파일 업로드
        await fetch(presignedUrl, {
          method: 'PUT',
          body: file,
          headers: {
            'Content-Type': file.type,
          },
        });

        // 업로드된 이미지 URL 생성
        const imageUrl = presignedUrl.split('?')[0];

        // Quill 에디터에 이미지 삽입
        const quill = quillRef.current?.getEditor();
        if (quill) {
          const range = quill.getSelection();
          quill.insertEmbed(range?.index || 0, 'image', imageUrl);
        }

      } catch (error) {
        console.error('이미지 업로드 실패:', error);
        alert('이미지 업로드에 실패했습니다.');
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