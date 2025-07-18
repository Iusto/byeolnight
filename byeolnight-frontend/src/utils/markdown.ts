/**
 * 마크다운 텍스트를 파싱하는 유틸리티 함수
 * @param text 파싱할 마크다운 텍스트
 * @returns 파싱된 HTML 또는 원본 텍스트
 */
export function parseMarkdown(text: string): string {
  if (!text) return '';
  
  // HTML 태그가 이미 포함된 경우 (iframe 등) 원본 반환
  if (text.includes('<iframe') || text.includes('<div')) {
    return text;
  }
  
  // 간단한 마크다운 파싱 (실제 프로젝트에서는 더 복잡할 수 있음)
  // 헤더
  text = text.replace(/^# (.*?)$/gm, '<h1>$1</h1>');
  text = text.replace(/^## (.*?)$/gm, '<h2>$1</h2>');
  text = text.replace(/^### (.*?)$/gm, '<h3>$1</h3>');
  
  // 굵은 글씨
  text = text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  
  // 기울임 글씨
  text = text.replace(/\*(.*?)\*/g, '<em>$1</em>');
  
  // 링크
  text = text.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');
  
  // 줄바꿈
  text = text.replace(/\n/g, '<br>');
  
  return text;
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