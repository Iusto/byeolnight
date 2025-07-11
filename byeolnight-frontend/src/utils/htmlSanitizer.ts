// HTML 콘텐츠 보안 정책: 허용 태그만 렌더링
export const sanitizeHtml = (html: string): string => {
  // 허용된 태그만 유지하고 나머지는 제거
  const allowedTags = ['img', 'iframe'];
  const dangerousTags = ['script', 'style', 'object', 'embed', 'form', 'input'];
  
  let sanitized = html;
  
  // 위험한 태그 완전 제거
  dangerousTags.forEach(tag => {
    const regex = new RegExp(`<${tag}[^>]*>.*?<\/${tag}>`, 'gis');
    sanitized = sanitized.replace(regex, '');
    const selfClosingRegex = new RegExp(`<${tag}[^>]*\/?>`, 'gis');
    sanitized = sanitized.replace(selfClosingRegex, '');
  });
  
  // img 태그 검증 및 정리
  sanitized = sanitized.replace(/<img([^>]*)>/gi, (match, attrs) => {
    // src 속성만 허용하고 나머지 위험한 속성 제거
    const srcMatch = attrs.match(/src\s*=\s*["']([^"']+)["']/i);
    const altMatch = attrs.match(/alt\s*=\s*["']([^"']+)["']/i);
    const styleMatch = attrs.match(/style\s*=\s*["']([^"']*max-width[^"']*)["']/i);
    
    if (srcMatch) {
      const src = srcMatch[1];
      const alt = altMatch ? altMatch[1] : '';
      const style = styleMatch ? styleMatch[1] : 'max-width: 100%; height: auto;';
      
      // 안전한 URL인지 확인 (http/https만 허용)
      if (src.startsWith('http://') || src.startsWith('https://')) {
        return `<img src="${src}" alt="${alt}" style="${style}" />`;
      }
    }
    return '';
  });
  
  // iframe 태그 검증 (YouTube, Vimeo만 허용)
  sanitized = sanitized.replace(/<iframe([^>]*)>.*?<\/iframe>/gi, (match, attrs) => {
    const srcMatch = attrs.match(/src\s*=\s*["']([^"']+)["']/i);
    const titleMatch = attrs.match(/title\s*=\s*["']([^"']+)["']/i);
    const styleMatch = attrs.match(/style\s*=\s*["']([^"']+)["']/i);
    const allowMatch = attrs.match(/allow\s*=\s*["']([^"']+)["']/i);
    
    if (srcMatch) {
      const src = srcMatch[1];
      
      // YouTube 또는 Vimeo 임베드만 허용
      if (src.includes('youtube.com/embed/') || src.includes('youtu.be/') || src.includes('vimeo.com/video/')) {
        // YouTube URL 정규화
        let embedUrl = src;
        if (src.includes('youtu.be/')) {
          const videoId = src.split('youtu.be/')[1].split('?')[0];
          embedUrl = `https://www.youtube.com/embed/${videoId}`;
        }
        
        const title = titleMatch ? titleMatch[1] : 'YouTube 영상';
        const style = styleMatch ? styleMatch[1] : 'width: 100%; height: 500px;';
        const allowAttrs = allowMatch ? allowMatch[1] : 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share';
        
        return `<iframe src="${embedUrl}" title="${title}" style="${style}" frameborder="0" allowfullscreen allow="${allowAttrs}"></iframe>`;
      }
    }
    return '';
  });
  
  return sanitized;
};