import { useTranslation } from 'react-i18next';

interface PointEarningGuideProps {
  className?: string;
}

export default function PointEarningGuide({ className = '' }: PointEarningGuideProps) {
  const { t } = useTranslation();

  return (
    <div className={`bg-[#1f2336] bg-opacity-80 backdrop-blur-md rounded-xl p-4 sm:p-6 mobile-card ${className}`}>
      <h2 className="text-lg sm:text-xl font-bold mb-3 sm:mb-4 text-center bg-gradient-to-r from-yellow-400 to-orange-400 bg-clip-text text-transparent mobile-title">
        ⭐ {t('shop.how_to_earn_points')}
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 sm:gap-4">
        <div className="bg-[#252842] bg-opacity-60 rounded-lg p-3 mobile-card-compact">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-lg">📅</span>
            <span className="font-semibold text-sm sm:text-base mobile-text">{t('shop.daily_attendance')}</span>
          </div>
          <p className="text-yellow-400 font-bold text-xs sm:text-sm mobile-caption">+10 {t('shop.points')}</p>
          <p className="text-gray-400 text-xs mobile-caption">{t('shop.daily_attendance_desc')}</p>
        </div>
        
        <div className="bg-[#252842] bg-opacity-60 rounded-lg p-3 mobile-card-compact">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-lg">✍️</span>
            <span className="font-semibold text-sm sm:text-base mobile-text">{t('shop.post_write')}</span>
          </div>
          <p className="text-yellow-400 font-bold text-xs sm:text-sm mobile-caption">+20 {t('shop.points')}</p>
          <p className="text-gray-400 text-xs mobile-caption">{t('shop.post_write_desc')}</p>
        </div>
        
        <div className="bg-[#252842] bg-opacity-60 rounded-lg p-3 mobile-card-compact">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-lg">💬</span>
            <span className="font-semibold text-sm sm:text-base mobile-text">{t('shop.comment_write')}</span>
          </div>
          <p className="text-yellow-400 font-bold text-xs sm:text-sm mobile-caption">+5 {t('shop.points')}</p>
          <p className="text-gray-400 text-xs mobile-caption">{t('shop.comment_write_desc')}</p>
        </div>
        
        <div className="bg-[#252842] bg-opacity-60 rounded-lg p-3 mobile-card-compact">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-lg">❤️</span>
            <span className="font-semibold text-sm sm:text-base mobile-text">{t('shop.receive_like')}</span>
          </div>
          <p className="text-yellow-400 font-bold text-xs sm:text-sm mobile-caption">+2 {t('shop.points')}</p>
          <p className="text-gray-400 text-xs mobile-caption">{t('shop.receive_like_desc')}</p>
        </div>
        
        <div className="bg-[#252842] bg-opacity-60 rounded-lg p-3 mobile-card-compact">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-lg">👍</span>
            <span className="font-semibold text-sm sm:text-base mobile-text">{t('shop.give_like')}</span>
          </div>
          <p className="text-yellow-400 font-bold text-xs sm:text-sm mobile-caption">+1 {t('shop.points')}</p>
          <p className="text-gray-400 text-xs mobile-caption">{t('shop.give_like_desc')}</p>
        </div>
        
        <div className="bg-[#252842] bg-opacity-60 rounded-lg p-3 mobile-card-compact">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-lg">🚨</span>
            <span className="font-semibold text-sm sm:text-base mobile-text">{t('shop.valid_report')}</span>
          </div>
          <p className="text-yellow-400 font-bold text-xs sm:text-sm mobile-caption">+10 {t('shop.points')}</p>
          <p className="text-gray-400 text-xs mobile-caption">{t('shop.valid_report_desc')}</p>
        </div>
      </div>
      
      <div className="mt-4 p-3 bg-blue-900 bg-opacity-30 rounded-lg border border-blue-500 border-opacity-30">
        <p className="text-blue-300 text-xs sm:text-sm text-center mobile-caption">
          💡 {t('shop.daily_limits_notice')}
        </p>
      </div>
    </div>
  );
}