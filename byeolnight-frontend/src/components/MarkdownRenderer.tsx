import { useMemo } from 'react';
import ReactMarkdown from 'react-markdown';
import rehypeRaw from 'rehype-raw';
import rehypeSanitize from 'rehype-sanitize';
import YouTubeEmbed from './YouTubeEmbed';

interface MarkdownRendererProps {
  content: string;
  className?: string;
  style?: React.CSSProperties;
  isPreview?: boolean; // 미리보기/에디터용인지 구분
}

export default function MarkdownRenderer({ 
  content, 
  className = '', 
  style,
  isPreview = false 
}: MarkdownRendererProps) {
  
  // 메모이제이션된 HTML 엔티티 디코딩
  const decodedContent = useMemo(() => {
    if (!content) return '';
    
    // HTML 엔티티만 디코딩 (마크다운 문법 유지)
    const textarea = document.createElement('textarea');
    textarea.innerHTML = content;
    return textarea.value;
  }, [content]);
  
  if (!decodedContent) return null;
  
  // YouTube URL을 감지하고 컴포넌트로 변환
  const processYouTubeUrls = (text: string) => {
    const youtubeRegex = /(https?:\/\/(www\.)?(youtube\.com\/watch\?v=|youtu\.be\/)([^\s&]+))/g;
    const parts = text.split(youtubeRegex);
    
    return parts.map((part, index) => {
      if (part && part.match(youtubeRegex)) {
        return <YouTubeEmbed key={index} url={part} />;
      }
      return part;
    });
  };
  
  // YouTube URL이 포함된 경우 특별 처리
  const hasYouTube = /https?:\/\/(www\.)?(youtube\.com\/watch\?v=|youtu\.be\/)/.test(decodedContent);
  
  if (hasYouTube) {
    return (
      <div className={`post-content ${className}`} style={style}>
        {processYouTubeUrls(decodedContent)}
      </div>
    );
  }
  
  // 일반 마크다운 렌더링
  return (
    <ReactMarkdown
      className={`post-content ${className}`}
      rehypePlugins={[
        rehypeRaw,
        [rehypeSanitize, {
          tagNames: ['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'strong', 'em', 'u', 's', 'blockquote', 'pre', 'code', 'ul', 'ol', 'li', 'br', 'hr', 'a', 'img', 'iframe'],
          attributes: {
            '*': ['className', 'style'],
            'a': ['href', 'target', 'rel'],
            'img': ['src', 'alt', 'width', 'height'],
            'iframe': ['src', 'width', 'height', 'frameBorder', 'allowFullScreen']
          }
        }]
      ]}
      components={{
        // 제목 컴포넌트들
        h1: ({ children, ...props }) => (
          <h1 {...props} style={{ fontSize: '2rem', fontWeight: 'bold', margin: '1.5rem 0 1rem 0', color: '#e2e8f0' }}>
            {children}
          </h1>
        ),
        h2: ({ children, ...props }) => (
          <h2 {...props} style={{ fontSize: '1.75rem', fontWeight: 'bold', margin: '1.25rem 0 0.75rem 0', color: '#e2e8f0' }}>
            {children}
          </h2>
        ),
        h3: ({ children, ...props }) => (
          <h3 {...props} style={{ fontSize: '1.5rem', fontWeight: 'bold', margin: '1rem 0 0.5rem 0', color: '#e2e8f0' }}>
            {children}
          </h3>
        ),
        h4: ({ children, ...props }) => (
          <h4 {...props} style={{ fontSize: '1.25rem', fontWeight: 'bold', margin: '0.75rem 0 0.5rem 0', color: '#e2e8f0' }}>
            {children}
          </h4>
        ),
        h5: ({ children, ...props }) => (
          <h5 {...props} style={{ fontSize: '1.125rem', fontWeight: 'bold', margin: '0.75rem 0 0.5rem 0', color: '#e2e8f0' }}>
            {children}
          </h5>
        ),
        h6: ({ children, ...props }) => (
          <h6 {...props} style={{ fontSize: '1rem', fontWeight: 'bold', margin: '0.5rem 0 0.25rem 0', color: '#e2e8f0' }}>
            {children}
          </h6>
        ),
        // 텍스트 스타일
        p: ({ children, ...props }) => (
          <p {...props} style={{ fontSize: '1rem', lineHeight: '1.7', margin: '0.4rem 0', color: '#cbd5e1' }}>
            {children}
          </p>
        ),
        strong: ({ children, ...props }) => (
          <strong {...props} style={{ fontWeight: 'bold', color: '#f1f5f9' }}>
            {children}
          </strong>
        ),
        em: ({ children, ...props }) => (
          <em {...props} style={{ fontStyle: 'italic', color: '#a78bfa' }}>
            {children}
          </em>
        ),
        // 인용구
        blockquote: ({ children, ...props }) => (
          <blockquote {...props} style={{
            borderLeft: '4px solid #8b5cf6',
            padding: '1rem 1.5rem',
            margin: '1.5rem 0',
            fontStyle: 'italic',
            background: 'rgba(139, 92, 246, 0.1)',
            borderRadius: '0 8px 8px 0',
            color: '#c4b5fd'
          }}>
            {children}
          </blockquote>
        ),
        // 코드
        code: ({ children, ...props }) => (
          <code {...props} style={{
            background: 'rgba(139, 92, 246, 0.2)',
            color: '#e879f9',
            padding: '0.2rem 0.4rem',
            borderRadius: '4px',
            fontFamily: 'Courier New, Consolas, monospace',
            fontSize: '0.9em'
          }}>
            {children}
          </code>
        ),
        // 이미지 컴포넌트 커스터마이징
        img: ({ src, alt, ...props }) => (
          <img 
            src={src} 
            alt={alt} 
            {...props}
            style={{
              maxWidth: '100%',
              height: 'auto',
              margin: '16px 0',
              borderRadius: '8px',
              display: 'block',
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.3)',
              cursor: 'pointer'
            }}
            onClick={() => src && window.open(src, '_blank')}
            loading="lazy"
          />
        ),
        // 링크 컴포넌트 커스터마이징
        a: ({ href, children, ...props }) => (
          <a 
            href={href} 
            {...props}
            target="_blank" 
            rel="noopener noreferrer"
            style={{
              color: '#a78bfa',
              textDecoration: 'underline',
              transition: 'color 0.2s ease'
            }}
          >
            {children}
          </a>
        )
      }}
      style={{ 
        whiteSpace: 'pre-wrap', 
        wordWrap: 'break-word', 
        lineHeight: '1.7',
        ...style 
      }}
    >
      {decodedContent}
    </ReactMarkdown>
  );
}