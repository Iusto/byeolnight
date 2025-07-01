import { useRef, useState } from 'react';
import axios from '../lib/axios';

interface SimpleTextEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

export default function SimpleTextEditor({ value, onChange, placeholder }: SimpleTextEditorProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // 파일 크기 체크 (10MB)
    if (file.size > 10 * 1024 * 1024) {
      alert('파일 크기는 10MB 이하만 업로드 가능합니다.');
      return;
    }

    // 이미지 형식 체크
    if (!file.type.startsWith('image/')) {
      alert('이미지 파일만 업로드 가능합니다.');
      return;
    }

    setUploading(true);
    try {
      // S3 Presigned URL 요청
      const presignedRes = await axios.post('/member/files/presigned-url', {
        filename: file.name
      });

      const { url: presignedUrl, s3Key } = presignedRes.data;

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

      // 텍스트 영역에 이미지 마크다운 삽입
      const textarea = textareaRef.current;
      if (textarea) {
        const start = textarea.selectionStart;
        const end = textarea.selectionEnd;
        const imageMarkdown = `\n![${file.name}](${imageUrl})\n`;
        
        const newValue = value.substring(0, start) + imageMarkdown + value.substring(end);
        onChange(newValue);
        
        // 커서 위치 조정
        setTimeout(() => {
          textarea.focus();
          textarea.setSelectionRange(start + imageMarkdown.length, start + imageMarkdown.length);
        }, 0);
      }

    } catch (error) {
      console.error('이미지 업로드 실패:', error);
      alert('이미지 업로드에 실패했습니다.');
    } finally {
      setUploading(false);
      // 파일 입력 초기화
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const insertText = (before: string, after: string = '') => {
    const textarea = textareaRef.current;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selectedText = value.substring(start, end);
    
    const newText = before + selectedText + after;
    const newValue = value.substring(0, start) + newText + value.substring(end);
    
    onChange(newValue);
    
    setTimeout(() => {
      textarea.focus();
      if (selectedText) {
        textarea.setSelectionRange(start, start + newText.length);
      } else {
        textarea.setSelectionRange(start + before.length, start + before.length);
      }
    }, 0);
  };

  return (
    <div className="simple-text-editor">
      {/* 툴바 */}
      <div className="flex flex-wrap gap-2 p-3 bg-[#1f2336] border border-gray-600 rounded-t-lg">
        <button
          type="button"
          onClick={() => insertText('**', '**')}
          className="px-2 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="굵게"
        >
          <strong>B</strong>
        </button>
        <button
          type="button"
          onClick={() => insertText('*', '*')}
          className="px-2 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="기울임"
        >
          <em>I</em>
        </button>
        <button
          type="button"
          onClick={() => insertText('# ', '')}
          className="px-2 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="제목"
        >
          H1
        </button>
        <button
          type="button"
          onClick={() => insertText('## ', '')}
          className="px-2 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="부제목"
        >
          H2
        </button>
        <button
          type="button"
          onClick={() => insertText('- ', '')}
          className="px-2 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="목록"
        >
          •
        </button>
        <button
          type="button"
          onClick={() => insertText('[링크텍스트](', ')')}
          className="px-2 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition"
          title="링크"
        >
          🔗
        </button>
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
          className="px-2 py-1 text-sm bg-[#2a2e45] hover:bg-[#323654] text-white rounded transition disabled:opacity-50"
          title="이미지 업로드"
        >
          {uploading ? '⏳' : '🖼️'}
        </button>
      </div>

      {/* 텍스트 영역 */}
      <textarea
        ref={textareaRef}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full h-64 p-4 bg-[#2a2e45] text-white border border-gray-600 border-t-0 rounded-b-lg resize-none focus:outline-none focus:ring-2 focus:ring-purple-500"
        style={{ fontFamily: 'monospace' }}
      />

      {/* 숨겨진 파일 입력 */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        onChange={handleImageUpload}
        className="hidden"
      />

      {/* 마크다운 안내 */}
      <div className="mt-2 text-xs text-gray-400">
        💡 마크다운 지원: **굵게**, *기울임*, # 제목, ## 부제목, - 목록, [링크](URL)
      </div>
    </div>
  );
}