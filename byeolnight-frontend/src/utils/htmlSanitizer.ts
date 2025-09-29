import DOMPurify from 'dompurify';

/**
 * DOMPurify를 사용하여 HTML 콘텐츠를 안전하게 정화하는 함수
 * XSS 공격을 방지하기 위해 허용된 태그와 속성만 남기고 제거합니다.
 * 
 * @param html 정화할 HTML 문자열
 * @returns 정화된 HTML 문자열
 */
export function sanitizeHtml(html: string): string {
  if (!html) return '';
  
  // DOMPurify 설정: 엄격한 보안 정책
  const config = {
    ALLOWED_TAGS: [
      'p', 'div', 'span', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'strong', 'em', 'u', 's', 'blockquote', 'pre', 'code',
      'ul', 'ol', 'li', 'br', 'hr',
      'a', 'img', 'iframe', 'video', 'audio', 'source',
      'table', 'thead', 'tbody', 'tr', 'th', 'td'
    ],
    ALLOWED_ATTR: [
      'class', 'id', 'style', 'href', 'target', 'rel',
      'src', 'alt', 'width', 'height', 'frameborder', 'allowfullscreen',
      'controls', 'type'
    ],
    ALLOW_DATA_ATTR: false,
    FORBID_TAGS: ['script', 'object', 'embed', 'form', 'input'],
    FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover'],
    KEEP_CONTENT: true
  };
  
  return DOMPurify.sanitize(html, config);
}