import ReactMarkdown from 'react-markdown';
import rehypeRaw from 'rehype-raw';
import rehypeSanitize from 'rehype-sanitize';
import { sanitizeHtml } from '../utils/htmlSanitizer';

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
  
  if (!content) return null;
  
  if (isPreview) {
    // 미리보기/에디터에서만 DOMPurify 후 dangerouslySetInnerHTML 사용
    const sanitizedContent = sanitizeHtml(content);
    return (
      <div 
        className={`prose prose-lg max-w-none dark:prose-invert youtube-content post-content ${className}`}
        style={{ 
          whiteSpace: 'pre-wrap', 
          wordWrap: 'break-word', 
          lineHeight: '1.7',
          ...style 
        }}
        dangerouslySetInnerHTML={{ __html: sanitizedContent }}
      />
    );
  }
  
  // 렌더링: react-markdown + rehype-sanitize
  return (
    <ReactMarkdown
      className={`prose prose-lg max-w-none dark:prose-invert youtube-content post-content ${className}`}
      rehypePlugins={[
        rehypeRaw, // 제한적 raw HTML 허용
        [rehypeSanitize, {
          // 엄격한 sanitize 정책
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
      {content}
    </ReactMarkdown>
  );
}