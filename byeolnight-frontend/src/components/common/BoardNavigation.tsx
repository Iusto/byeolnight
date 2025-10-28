import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { BOARD_CONFIGS } from '../../constants/categories';

export default function BoardNavigation() {
  const { t } = useTranslation();

  return (
    <div className="mb-6 sm:mb-12">
      <div className="text-center mb-4 sm:mb-8">
        <div className="inline-block mb-2 sm:mb-4">
          <div className="relative">
            <span className="text-2xl sm:text-4xl animate-spin-slow">🚀</span>
            <div className="absolute -inset-2 bg-gradient-to-r from-purple-600 to-pink-600 rounded-full blur opacity-20 animate-pulse"></div>
          </div>
        </div>
        <h2 className="text-lg sm:text-3xl font-bold mb-1 sm:mb-3">
          <span className="text-white mobile-text">
            {t('home.explore_boards')}
          </span>
        </h2>
        <p className="text-white mobile-text-secondary text-xs sm:text-sm">{t('home.explore_subtitle')}</p>
      </div>
      <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-2 sm:gap-4">
        {BOARD_CONFIGS.map((board, index) => (
          <Link 
            key={board.key} 
            to={board.path || `/posts?category=${board.key}&sort=recent#posts-section`} 
            className="group"
          >
            <div 
              className={`relative p-3 sm:p-4 bg-gradient-to-br ${board.bgClass} rounded-xl border ${board.borderClass} ${board.hoverBgClass} ${board.hoverBorderClass} transition-all duration-300 text-center transform active:scale-95 hover:scale-105 hover:-translate-y-1 shadow-lg ${board.shadowClass} backdrop-blur-sm min-h-[70px] sm:min-h-[90px] flex flex-col justify-center`}
              style={{animationDelay: `${index * 0.1}s`}}
            >
              {board.hasAI && (
                <div className="absolute -top-2 -right-2">
                  <div className="relative">
                    <span className={`bg-gradient-to-r ${board.aiClass} text-white text-xs px-2 py-1 rounded-full font-bold shadow-lg animate-pulse`}>
                      🤖 AI
                    </span>
                    <div className="absolute inset-0 bg-gradient-to-r from-white/20 to-white/10 rounded-full blur opacity-50 animate-ping"></div>
                  </div>
                </div>
              )}
              <div className="text-base sm:text-2xl mb-1 sm:mb-2 group-hover:animate-bounce group-hover:scale-110 transition-transform duration-300">
                {board.icon}
              </div>
              <div className="text-xs sm:text-sm font-bold text-white mobile-text group-hover:text-white transition-colors leading-tight" style={{textShadow: '0 2px 4px rgba(0,0,0,0.9), 0 1px 2px rgba(0,0,0,0.8)', color: '#ffffff', filter: 'brightness(1.1)'}}>
                {board.key === 'NEWS' ? t('home.space_news') : 
                 board.key === 'IMAGE' ? t('home.star_photo') :
                 board.key === 'STARLIGHT_CINEMA' ? t('home.star_cinema') :
                 t(`home.${board.key.toLowerCase()}`)}
              </div>
              <div className="absolute inset-0 bg-gradient-to-br from-white/0 to-white/0 group-hover:from-white/5 group-hover:to-white/10 rounded-xl transition-all duration-300"></div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
