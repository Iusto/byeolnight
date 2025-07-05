import { useRef, useState } from 'react';
import axios from '../lib/axios';

interface SimpleRichEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

export default function SimpleRichEditor({ value, onChange, placeholder }: SimpleRichEditorProps) {
  const editorRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 10 * 1024 * 1024) {
      alert('파일 크기는 10MB 이하만 업로드 가능합니다.');
      return;
    }

    if (!file.type.startsWith('image/')) {
      alert('이미지 파일만 업로드 가능합니다.');
      return;
    }

    setUploading(true);
    try {
      const presignedRes = await axios.post('/member/files/presigned-url', {
        filename: file.name
      });

      const { url: presignedUrl } = presignedRes.data;

      await fetch(presignedUrl, {
        method: 'PUT',
        body: file,
        headers: {
          'Content-Type': file.type,
        },
      });

      const imageUrl = presignedUrl.split('?')[0];
      
      // 이미지를 에디터에 삽입
      document.execCommand('insertHTML', false, `<img src="${imageUrl}" alt="${file.name}" style="max-width: 100%; height: auto; margin: 10px 0;" />`);
      
      // 내용 업데이트
      if (editorRef.current) {
        onChange(editorRef.current.innerHTML);
      }

    } catch (error) {
      console.error('이미지 업로드 실패:', error);
      alert('이미지 업로드에 실패했습니다.');
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const execCommand = (command: string, value?: string) => {
    document.execCommand(command, false, value);
    if (editorRef.current) {
      onChange(editorRef.current.innerHTML);
    }
  };

  const handleInput = () => {
    if (editorRef.current) {
      onChange(editorRef.current.innerHTML);
    }
  };

  return (
    <div className="simple-rich-editor border border-gray-600 rounded-lg overflow-hidden">
      {/* 툴바 */}
      <div className="flex flex-wrap gap-1 p-2 bg-[#1f2336] border-b border-gray-600">
        <button
          type="button"
          onClick={() => execCommand('bold')}
          className="px-3 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="굵게"
        >
          <strong>B</strong>
        </button>
        <button
          type="button"
          onClick={() => execCommand('italic')}
          className="px-3 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="기울임"
        >
          <em>I</em>
        </button>
        <button
          type="button"
          onClick={() => execCommand('underline')}
          className="px-3 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="밑줄"
        >
          <u>U</u>
        </button>
        <div className="w-px bg-gray-600 mx-1"></div>
        <button
          type="button"
          onClick={() => execCommand('formatBlock', 'h2')}
          className="px-3 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="제목"
        >
          H2
        </button>
        <button
          type="button"
          onClick={() => execCommand('formatBlock', 'h3')}
          className="px-3 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="부제목"
        >
          H3
        </button>
        <div className="w-px bg-gray-600 mx-1"></div>
        <button
          type="button"
          onClick={() => execCommand('insertUnorderedList')}
          className="px-3 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="목록"
        >
          •
        </button>
        <button
          type="button"
          onClick={() => execCommand('insertOrderedList')}
          className="px-3 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="번호 목록"
        >
          1.
        </button>
        <div className="w-px bg-gray-600 mx-1"></div>
        <button
          type="button"
          onClick={() => {
            const url = prompt('링크 URL을 입력하세요:');
            if (url) execCommand('createLink', url);
          }}
          className="px-3 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="링크"
        >
          🔗
        </button>
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
          className="px-3 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition disabled:opacity-50"
          title="이미지 업로드"
        >
          {uploading ? '⏳' : '🖼️'}
        </button>
        <div className="w-px bg-gray-600 mx-1"></div>
        <button
          type="button"
          onClick={() => execCommand('removeFormat')}
          className="px-3 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="서식 제거"
        >
          🧹
        </button>
      </div>

      {/* 에디터 영역 */}
      <div
        ref={editorRef}
        contentEditable
        onInput={handleInput}
        dangerouslySetInnerHTML={{ __html: value }}
        className="min-h-[200px] p-4 bg-[#2a2e45] text-white focus:outline-none"
        style={{
          wordBreak: 'break-word',
          lineHeight: '1.6'
        }}
        data-placeholder={placeholder}
      />

      {/* 숨겨진 파일 입력 */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        onChange={handleImageUpload}
        className="hidden"
      />

      <style jsx>{`
        [contenteditable]:empty:before {
          content: attr(data-placeholder);
          color: #a0aec0;
          pointer-events: none;
        }
        
        [contenteditable] h2 {
          font-size: 1.5em;
          font-weight: bold;
          margin: 0.5em 0;
        }
        
        [contenteditable] h3 {
          font-size: 1.25em;
          font-weight: bold;
          margin: 0.5em 0;
        }
        
        [contenteditable] ul, [contenteditable] ol {
          margin: 0.5em 0;
          padding-left: 2em;
        }
        
        [contenteditable] a {
          color: #9f7aea;
          text-decoration: underline;
        }
        
        [contenteditable] img {
          max-width: 100%;
          height: auto;
          margin: 10px 0;
          border-radius: 8px;
        }
      `}</style>
    </div>
  );
}