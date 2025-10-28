// 포맷팅 유틸리티 함수

export const formatDate = (dateStr: string): string => {
  if (!dateStr) return '';
  try {
    return new Date(dateStr).toLocaleString();
  } catch {
    return dateStr;
  }
};

export const extractFirstImage = (content: string): string | null => {
  if (!content) return null;
  const imgMatches = [
    content.match(/<img[^>]+src=["']([^"']+)["'][^>]*>/i),
    content.match(/!\[.*?\]\(([^)]+)\)/),
    content.match(/https?:\/\/[^\s]+\.(jpg|jpeg|png|gif|webp)/i)
  ];
  
  for (const match of imgMatches) {
    if (match?.[1] && !match[1].includes('placeholder')) {
      return match[1].trim();
    }
  }
  return null;
};

export const formatTime = (seconds: number): string => {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
};
