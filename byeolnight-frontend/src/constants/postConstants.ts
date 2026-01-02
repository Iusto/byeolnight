// 카테고리 라벨
export const CATEGORY_LABELS: Record<string, string> = {
  NEWS: '뉴스',
  DISCUSSION: '토론',
  IMAGE: '사진',
  REVIEW: '후기',
  FREE: '자유',
  NOTICE: '공지',
  STARLIGHT_CINEMA: '별빛 시네마',
};

// 카테고리별 아이콘
export const CATEGORY_ICONS: Record<string, string> = {
  NEWS: '🚀',
  DISCUSSION: '💬',
  IMAGE: '🌌',
  REVIEW: '⭐',
  FREE: '🎈',
  NOTICE: '📢',
  STARLIGHT_CINEMA: '🎬'
};

// 사용자가 글을 작성할 수 있는 카테고리
export const USER_WRITABLE_CATEGORIES = ['DISCUSSION', 'IMAGE', 'REVIEW', 'FREE'];
