// 마크다운을 HTML로 변환하는 통합 유틸리티
export function parseMarkdown(text: string): string {
  if (!text) return '';
  
  // 이미 HTML 태그가 있으면 그대로 반환 (ReactQuill에서 생성된 HTML)
  if (text.includes('<iframe') || text.includes('<div') || text.includes('<img')) {
    return text;
  }
  
  let html = text;
  
  // 1. 수평선 (---)
  html = html.replace(/^---$/gm, '<hr style="border: none; border-top: 2px solid #8b5cf6; margin: 20px 0;">');
  
  // 2. 헤더 (# ## ###)
  html = html.replace(/^### (.+)$/gm, '<h3 style="color: #a855f7; font-size: 1.25rem; font-weight: bold; margin: 16px 0 8px 0;">$1</h3>');
  html = html.replace(/^## (.+)$/gm, '<h2 style="color: #9333ea; font-size: 1.5rem; font-weight: bold; margin: 20px 0 12px 0;">$1</h2>');
  html = html.replace(/^# (.+)$/gm, '<h1 style="color: #7c3aed; font-size: 1.875rem; font-weight: bold; margin: 24px 0 16px 0;">$1</h1>');
  
  // 3. 굵은 글씨 (**text**)
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong style="color: #fbbf24; font-weight: bold;">$1</strong>');
  
  // 4. 기울임 (*text*)
  html = html.replace(/\*([^*]+)\*/g, '<em style="color: #a78bfa; font-style: italic;">$1</em>');
  
  // 5. 링크 [text](url)
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" style="color: #60a5fa; text-decoration: underline;" target="_blank">$1</a>');
  
  // 6. 이미지: ![alt](url)
  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" style="max-width: 100%; height: auto; margin: 16px 0; border-radius: 8px;" />');
  
  // 7. 리스트 (- item)
  html = html.replace(/^- (.+)$/gm, '<li style="color: #e5e7eb; margin: 4px 0; padding-left: 8px;">$1</li>');
  
  // 8. 이모지가 포함된 리스트 특별 처리
  html = html.replace(/^(🔥|🌠|🗞|💬|🎬|🪐|🧑‍🚀|🛠|📲|🌌|🔭|🌐|💌) (.+)$/gm, 
    '<div style="color: #e5e7eb; margin: 8px 0; padding: 8px 12px; background: rgba(139, 92, 246, 0.1); border-left: 3px solid #8b5cf6; border-radius: 4px;"><span style="margin-right: 8px;">$1</span>$2</div>');
  
  // 9. 줄바꿈 처리
  html = html.replace(/\n\n/g, '</p><p style="margin: 12px 0; line-height: 1.6;">');
  html = html.replace(/\n/g, '<br>');
  
  // 10. 전체를 p 태그로 감싸기
  if (!html.startsWith('<')) {
    html = '<p style="margin: 12px 0; line-height: 1.6;">' + html + '</p>';
  }
  
  // 11. 빈 p 태그 제거
  html = html.replace(/<p[^>]*><\/p>/g, '');
  
  return html;
}

// ReactQuill에서 사용할 마크다운 변환 함수
export function convertMarkdownInQuill(content: string): string {
  // 마크다운 패턴이 있는지 확인
  const hasMarkdown = /^(#{1,3}\s|---$|\*\*.*\*\*|\*[^*]+\*|^-\s|\[.*\]\(.*\))/m.test(content);
  
  if (hasMarkdown) {
    return parseMarkdown(content);
  }
  
  return content;
}

/**
 * 마크다운 텍스트에서 첫 번째 문단만 추출하는 함수
 * @param text 마크다운 텍스트
 * @param maxLength 최대 길이 (기본값: 100)
 * @returns 추출된 첫 번째 문단
 */
export function extractFirstParagraph(text: string, maxLength: number = 100): string {
  if (!text) return '';
  
  // 첫 번째 줄바꿈이나 빈 줄까지의 텍스트 추출
  const firstParagraph = text.split(/\n\s*\n/)[0].trim();
  
  // 최대 길이로 제한
  if (firstParagraph.length > maxLength) {
    return firstParagraph.substring(0, maxLength) + '...';
  }
  
  return firstParagraph;
}