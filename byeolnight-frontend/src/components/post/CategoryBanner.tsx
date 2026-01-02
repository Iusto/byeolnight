import { useTranslation } from 'react-i18next';

interface CategoryBannerProps {
  category: string;
}

export default function CategoryBanner({ category }: CategoryBannerProps) {
  const { t } = useTranslation();

  const banners: Record<string, JSX.Element> = {
    NEWS: (
      <div className="mb-4 sm:mb-6 p-3 sm:p-4 bg-blue-900/30 rounded-lg border border-blue-600/30">
        <div className="flex items-center gap-2 sm:gap-3 mb-1 sm:mb-2">
          <span className="text-lg sm:text-2xl">🤖</span>
          <h3 className="text-sm sm:text-lg font-semibold text-blue-200">{t('home.auto_news_update')}</h3>
        </div>
        <p className="text-blue-200 text-xs sm:text-sm leading-relaxed">
          {t('home.news_bot_desc')}
          <br />
          <strong>[{t('home.target_sources')}]</strong><br />
          {t('home.newsdata_api')}<br />
          {t('home.keywords')}
          <br />
          {t('home.news_registration')}
        </p>
      </div>
    ),
    IMAGE: (
      <div className="mb-4 sm:mb-6 p-3 sm:p-4 bg-purple-900/30 rounded-lg border border-purple-600/30">
        <div className="flex items-center gap-2 sm:gap-3 mb-1 sm:mb-2">
          <span className="text-lg sm:text-2xl">🌌</span>
          <h3 className="text-sm sm:text-lg font-semibold text-purple-200">{t('home.star_photo')} {t('home.board_usage_guide')}</h3>
        </div>
        <p className="text-purple-200 text-xs sm:text-sm leading-relaxed">
          {t('home.image_board_desc')}
          <br />
          <strong>📷 {t('home.image_recommended')}</strong>
        </p>
      </div>
    ),
    REVIEW: (
      <div className="mb-4 sm:mb-6 p-3 sm:p-4 bg-yellow-900/30 rounded-lg border border-yellow-600/30">
        <div className="flex items-center gap-2 sm:gap-3 mb-1 sm:mb-2">
          <span className="text-lg sm:text-2xl">⭐</span>
          <h3 className="text-sm sm:text-lg font-semibold text-yellow-200">{t('home.review')} {t('home.board_usage_guide')}</h3>
        </div>
        <p className="text-yellow-200 text-xs sm:text-sm leading-relaxed">
          {t('home.review_board_desc')}
          <br />
          <strong>🎆 {t('home.review_recommended')}</strong>
        </p>
      </div>
    ),
    FREE: (
      <div className="mb-4 sm:mb-6 p-3 sm:p-4 bg-green-900/30 rounded-lg border border-green-600/30">
        <div className="flex items-center gap-2 sm:gap-3 mb-1 sm:mb-2">
          <span className="text-lg sm:text-2xl">🎈</span>
          <h3 className="text-sm sm:text-lg font-semibold text-green-200">{t('home.free')} {t('home.board_usage_guide')}</h3>
        </div>
        <p className="text-green-200 text-xs sm:text-sm leading-relaxed">
          {t('home.free_board_desc')}
          <br />
          <strong>🚀 {t('home.recommended_content')}</strong>
        </p>
      </div>
    ),
    NOTICE: (
      <div className="mb-4 sm:mb-6 p-3 sm:p-4 bg-red-900/30 rounded-lg border border-red-600/30">
        <div className="flex items-center gap-2 sm:gap-3 mb-1 sm:mb-2">
          <span className="text-lg sm:text-2xl">📢</span>
          <h3 className="text-sm sm:text-lg font-semibold text-red-200">{t('home.notice_board')} {t('home.board_usage_guide')}</h3>
        </div>
        <p className="text-red-200 text-xs sm:text-sm leading-relaxed">
          {t('home.notice_board_desc')}
          <br />
          <strong>⚠️ {t('home.notice_warning')}</strong>
        </p>
      </div>
    ),
    STARLIGHT_CINEMA: (
      <div className="mb-4 sm:mb-6 p-3 sm:p-4 bg-gradient-to-r from-purple-900/30 to-pink-900/30 rounded-lg border border-purple-600/30">
        <div className="flex items-center gap-2 sm:gap-3 mb-1 sm:mb-2">
          <span className="text-lg sm:text-2xl">🤖</span>
          <h3 className="text-sm sm:text-lg font-semibold text-purple-200">{t('home.ai_cinema_curation')}</h3>
        </div>
        <p className="text-purple-200 text-xs sm:text-sm leading-relaxed">
          {t('home.cinema_bot_desc')}
          <br />
          <strong>🎬 {t('home.curation_content')}</strong>
          <br />
          <strong>🤖 {t('home.ai_summary')}</strong>
          <br />
          <strong>⚠️ {t('home.cinema_warning')}</strong>
        </p>
      </div>
    ),
  };

  return banners[category] || null;
}
