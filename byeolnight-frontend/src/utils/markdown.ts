// 간단한 마크다운 파서
export function parseMarkdown(text: string): string {
  if (!text) return '';
  
  // HTML 태그가 이미 있는 경우 그대로 반환 (iframe 등)
  if (text.includes('<iframe') || text.includes('<div')) {
    return text;
  }
  
  let html = text
    // 이미지: ![alt](url)
    .replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" class="max-w-full h-auto my-4 rounded-lg" />')
    // 링크: [text](url)
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer" class="text-blue-400 hover:text-blue-300 underline">$1</a>')
    // 굵게: **text**
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    // 기울임: *text*
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    // 제목: # text
    .replace(/^# (.+)$/gm, '<h1 class="text-2xl font-bold my-4">$1</h1>')
    // 부제목: ## text
    .replace(/^## (.+)$/gm, '<h2 class="text-xl font-semibold my-3">$1</h2>')
    // 목록: - text
    .replace(/^- (.+)$/gm, '<li class="ml-4">• $1</li>')
    // 줄바꿈
    .replace(/\n/g, '<br />');
  
  return html;
}