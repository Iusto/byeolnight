/**
 * HTML 콘텐츠를 안전하게 정화하는 함수
 * XSS 공격을 방지하기 위해 허용된 태그와 속성만 남기고 제거합니다.
 * 
 * @param html 정화할 HTML 문자열
 * @returns 정화된 HTML 문자열
 */
export function sanitizeHtml(html: string): string {
  if (!html) return '';
  
  // 이미 안전한 HTML이라고 판단되는 경우 (ReactQuill에서 생성된 HTML)
  if (html.includes('quill-editor') || html.includes('ql-editor')) {
    return html;
  }
  
  // DOMParser를 사용하여 HTML 파싱
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, 'text/html');
  
  // 허용된 태그 목록
  const allowedTags = [
    'p', 'div', 'span', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'strong', 'em', 'u', 's', 'blockquote', 'pre', 'code',
    'ul', 'ol', 'li', 'br', 'hr',
    'a', 'img', 'iframe', 'video', 'audio', 'source',
    'table', 'thead', 'tbody', 'tr', 'th', 'td'
  ];
  
  // 허용된 속성 목록
  const allowedAttributes = {
    'all': ['class', 'id', 'style'],
    'a': ['href', 'target', 'rel'],
    'img': ['src', 'alt', 'width', 'height', 'style'],
    'iframe': ['src', 'width', 'height', 'frameborder', 'allowfullscreen'],
    'video': ['src', 'controls', 'width', 'height'],
    'audio': ['src', 'controls'],
    'source': ['src', 'type']
  };
  
  // 위험한 스크립트 및 이벤트 핸들러 속성 제거
  const dangerousAttributes = [
    'onclick', 'ondblclick', 'onmousedown', 'onmouseup', 'onmouseover', 'onmousemove', 'onmouseout',
    'onkeydown', 'onkeypress', 'onkeyup', 'onload', 'onunload', 'onabort', 'onerror', 'onresize',
    'onscroll', 'onfocus', 'onblur', 'onchange', 'onsubmit', 'onreset', 'javascript:'
  ];
  
  // 모든 요소 순회
  const sanitizeNode = (node: Element) => {
    // 태그 이름이 허용 목록에 없으면 내용만 유지
    if (!allowedTags.includes(node.tagName.toLowerCase())) {
      const content = node.textContent;
      const span = document.createElement('span');
      span.textContent = content;
      node.parentNode?.replaceChild(span, node);
      return;
    }
    
    // 속성 정화
    Array.from(node.attributes).forEach(attr => {
      const attrName = attr.name.toLowerCase();
      
      // 위험한 속성 제거
      if (dangerousAttributes.some(dangerous => attrName.includes(dangerous))) {
        node.removeAttribute(attr.name);
        return;
      }
      
      // 태그별 허용 속성 확인
      const tagAllowedAttrs = allowedAttributes[node.tagName.toLowerCase() as keyof typeof allowedAttributes] || [];
      const globalAllowedAttrs = allowedAttributes['all'];
      
      if (!tagAllowedAttrs.includes(attrName) && !globalAllowedAttrs.includes(attrName)) {
        node.removeAttribute(attr.name);
      }
      
      // href 속성 정화 (javascript: 프로토콜 제거)
      if (attrName === 'href' && attr.value.toLowerCase().startsWith('javascript:')) {
        node.setAttribute('href', '#');
      }
      
      // src 속성 정화 (javascript: 프로토콜 제거)
      if (attrName === 'src' && attr.value.toLowerCase().startsWith('javascript:')) {
        node.removeAttribute('src');
      }
      
      // style 속성 정화 (expression, behavior 등 제거)
      if (attrName === 'style') {
        const dangerousStyles = ['expression', 'behavior', 'javascript', 'vbscript'];
        if (dangerousStyles.some(style => attr.value.toLowerCase().includes(style))) {
          node.removeAttribute('style');
        }
      }
    });
    
    // 자식 요소 정화
    Array.from(node.children).forEach(child => {
      sanitizeNode(child as Element);
    });
  };
  
  // body의 모든 자식 요소 정화
  Array.from(doc.body.children).forEach(child => {
    sanitizeNode(child as Element);
  });
  
  // 정화된 HTML 반환
  return doc.body.innerHTML;
}