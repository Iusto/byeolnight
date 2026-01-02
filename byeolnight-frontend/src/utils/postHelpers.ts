// 카테고리 라벨 함수 (다국어 지원)
export const getCategoryLabel = (category: string, t: any): string => {
  const labels: Record<string, string> = {
    NEWS: t('home.space_news'),
    DISCUSSION: t('home.discussion'),
    IMAGE: t('home.star_photo'),
    REVIEW: t('home.review'),
    FREE: t('home.free'),
    NOTICE: t('home.notice'),
    STARLIGHT_CINEMA: t('home.star_cinema'),
  };
  return labels[category] || category;
};

// 카테고리 설명 함수 (다국어 지원)
export const getCategoryDescription = (category: string, t: any): string => {
  const descriptions: Record<string, string> = {
    NEWS: t('home.news_auto_desc'),
    DISCUSSION: t('home.discussion_auto_desc'),
    IMAGE: t('home.share_beautiful_space_photos'),
    REVIEW: t('home.share_space_experiences'),
    FREE: t('home.share_free_space_stories'),
    NOTICE: t('home.check_important_notices'),
    STARLIGHT_CINEMA: t('home.cinema_auto_desc')
  };
  return descriptions[category] || '';
};

// 이미지 추출 함수
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
