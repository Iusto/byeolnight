// 카테고리 관련 상수 통합
import type { TranslationFunction } from '../types/i18n';

export const CATEGORY_ICONS: Record<string, string> = {
  NEWS: '🚀',
  DISCUSSION: '💬',
  IMAGE: '🌌',
  REVIEW: '⭐',
  FREE: '🎈',
  NOTICE: '📢',
  STARLIGHT_CINEMA: '🎬'
};

export const CATEGORY_LABELS: Record<string, string> = {
  NEWS: '뉴스',
  DISCUSSION: '토론',
  IMAGE: '사진',
  REVIEW: '후기',
  FREE: '자유',
  NOTICE: '공지',
  STARLIGHT_CINEMA: '별빛 시네마',
};

export const USER_WRITABLE_CATEGORIES = ['DISCUSSION', 'IMAGE', 'REVIEW', 'FREE'];

export const BOARD_CONFIGS = [
  { key: 'NEWS', icon: '🚀', bgClass: 'from-blue-500/60 to-blue-600/60', hoverBgClass: 'hover:from-blue-600/40 hover:to-blue-600/40', borderClass: 'border-blue-400/60', hoverBorderClass: 'hover:border-blue-400/50', shadowClass: 'hover:shadow-blue-500/25', textClass: 'text-blue-100', hasAI: true, aiClass: 'from-blue-500 to-blue-600' },
  { key: 'DISCUSSION', icon: '💬', bgClass: 'from-green-500/60 to-green-600/60', hoverBgClass: 'hover:from-green-600/40 hover:to-green-600/40', borderClass: 'border-green-400/60', hoverBorderClass: 'hover:border-green-400/50', shadowClass: 'hover:shadow-green-500/25', textClass: 'text-green-100', hasAI: true, aiClass: 'from-green-500 to-green-600' },
  { key: 'IMAGE', icon: '🌌', bgClass: 'from-purple-500/60 to-purple-600/60', hoverBgClass: 'hover:from-purple-600/40 hover:to-purple-600/40', borderClass: 'border-purple-400/60', hoverBorderClass: 'hover:border-purple-400/50', shadowClass: 'hover:shadow-purple-500/25', textClass: 'text-purple-100' },
  { key: 'REVIEW', icon: '⭐', bgClass: 'from-yellow-500/60 to-yellow-600/60', hoverBgClass: 'hover:from-yellow-600/40 hover:to-yellow-600/40', borderClass: 'border-yellow-400/60', hoverBorderClass: 'hover:border-yellow-400/50', shadowClass: 'hover:shadow-yellow-500/25', textClass: 'text-yellow-100' },
  { key: 'FREE', icon: '🎈', bgClass: 'from-pink-500/60 to-pink-600/60', hoverBgClass: 'hover:from-pink-600/40 hover:to-pink-600/40', borderClass: 'border-pink-400/60', hoverBorderClass: 'hover:border-pink-400/50', shadowClass: 'hover:shadow-pink-500/25', textClass: 'text-pink-100' },
  { key: 'NOTICE', icon: '📢', bgClass: 'from-red-500/60 to-red-600/60', hoverBgClass: 'hover:from-red-600/40 hover:to-red-600/40', borderClass: 'border-red-400/60', hoverBorderClass: 'hover:border-red-400/50', shadowClass: 'hover:shadow-red-500/25', textClass: 'text-red-100' },
  { key: 'STARLIGHT_CINEMA', icon: '🎬', bgClass: 'from-purple-500/60 to-purple-600/60', hoverBgClass: 'hover:from-purple-600/40 hover:to-purple-600/40', borderClass: 'border-purple-400/60', hoverBorderClass: 'hover:border-purple-400/50', shadowClass: 'hover:shadow-purple-500/25', textClass: 'text-purple-100', hasAI: true, aiClass: 'from-purple-500 to-purple-600' },
  { key: 'SUGGESTIONS', icon: '💡', bgClass: 'from-orange-500/60 to-orange-600/60', hoverBgClass: 'hover:from-orange-600/40 hover:to-orange-600/40', borderClass: 'border-orange-400/60', hoverBorderClass: 'hover:border-orange-400/50', shadowClass: 'hover:shadow-orange-500/25', textClass: 'text-orange-100', path: '/suggestions' }
];

export const getCategoryLabel = (category: string, t: TranslationFunction): string => {
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

export const getCategoryDescription = (category: string, t: TranslationFunction): string => {
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
