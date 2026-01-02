import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export default function CategoryGrid() {
  const { t } = useTranslation();

  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 xl:grid-cols-7 gap-3 sm:gap-4">
      <Link to="/posts?category=NEWS&sort=recent#posts-section" className="group">
        <div className="relative p-3 sm:p-4 bg-gradient-to-br from-blue-600/70 to-cyan-600/70 hover:from-blue-600/90 hover:to-cyan-600/90 active:from-blue-700/90 active:to-cyan-700/90 rounded-xl border border-blue-500/50 hover:border-blue-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-blue-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
          <div className="absolute top-1 right-1">
            <span className="bg-gradient-to-r from-blue-500 to-cyan-500 text-white text-xs px-1.5 py-0.5 rounded-full font-bold shadow-md">🤖</span>
          </div>
          <div className="w-8 h-8 mx-auto bg-gradient-to-br from-blue-500/30 to-cyan-500/30 rounded-full flex items-center justify-center mb-2">
            <div className="text-xl">🚀</div>
          </div>
          <div className="text-sm font-bold text-blue-100 mb-1 leading-tight">{t('home.space_news')}</div>
          <div className="text-xs text-blue-300 bg-blue-500/20 rounded-full py-1 px-2 inline-block">{t('home.news_auto')}</div>
        </div>
      </Link>

      <Link to="/posts?category=DISCUSSION&sort=recent#posts-section" className="group">
        <div className="relative p-3 sm:p-4 bg-gradient-to-br from-green-600/70 to-emerald-600/70 hover:from-green-600/90 hover:to-emerald-600/90 active:from-green-700/90 active:to-emerald-700/90 rounded-xl border border-green-500/50 hover:border-green-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-green-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
          <div className="absolute top-1 right-1">
            <span className="bg-gradient-to-r from-green-500 to-emerald-500 text-white text-xs px-1.5 py-0.5 rounded-full font-bold shadow-md">🤖</span>
          </div>
          <div className="w-8 h-8 mx-auto bg-gradient-to-br from-green-500/30 to-emerald-500/30 rounded-full flex items-center justify-center mb-2">
            <div className="text-xl">💬</div>
          </div>
          <div className="text-sm font-bold text-green-100 mb-1 leading-tight">{t('home.discussion')}</div>
          <div className="text-xs text-green-300 bg-green-500/20 rounded-full py-1 px-2 inline-block">{t('home.discussion_auto')}</div>
        </div>
      </Link>

      <Link to="/posts?category=IMAGE&sort=recent#posts-section" className="group">
        <div className="relative p-3 sm:p-4 bg-gradient-to-br from-purple-600/70 to-indigo-600/70 hover:from-purple-600/90 hover:to-indigo-600/90 active:from-purple-700/90 active:to-indigo-700/90 rounded-xl border border-purple-500/50 hover:border-purple-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-purple-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
          <div className="w-8 h-8 mx-auto bg-gradient-to-br from-purple-500/30 to-indigo-500/30 rounded-full flex items-center justify-center mb-2">
            <div className="text-xl">🌌</div>
          </div>
          <div className="text-sm font-bold text-purple-100 mb-1 leading-tight">{t('home.star_photo')}</div>
          <div className="text-xs text-purple-300 bg-purple-500/20 rounded-full py-1 px-2 inline-block">{t('home.gallery')}</div>
        </div>
      </Link>

      <Link to="/posts?category=REVIEW&sort=recent#posts-section" className="group">
        <div className="relative p-3 sm:p-4 bg-gradient-to-br from-yellow-600/70 to-orange-600/70 hover:from-yellow-600/90 hover:to-orange-600/90 active:from-yellow-700/90 active:to-orange-700/90 rounded-xl border border-yellow-500/50 hover:border-yellow-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-yellow-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
          <div className="w-8 h-8 mx-auto bg-gradient-to-br from-yellow-500/30 to-orange-500/30 rounded-full flex items-center justify-center mb-2">
            <div className="text-xl">⭐</div>
          </div>
          <div className="text-sm font-bold text-yellow-100 mb-1 leading-tight">{t('home.review')}</div>
          <div className="text-xs text-yellow-300 bg-yellow-500/20 rounded-full py-1 px-2 inline-block">{t('home.review_share')}</div>
        </div>
      </Link>

      <Link to="/posts?category=FREE&sort=recent#posts-section" className="group">
        <div className="relative p-3 sm:p-4 bg-gradient-to-br from-pink-600/70 to-rose-600/70 hover:from-pink-600/90 hover:to-rose-600/90 active:from-pink-700/90 active:to-rose-700/90 rounded-xl border border-pink-500/50 hover:border-pink-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-pink-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
          <div className="w-8 h-8 mx-auto bg-gradient-to-br from-pink-500/30 to-rose-500/30 rounded-full flex items-center justify-center mb-2">
            <div className="text-xl">🎈</div>
          </div>
          <div className="text-sm font-bold text-pink-100 mb-1 leading-tight">{t('home.free')}</div>
          <div className="text-xs text-pink-300 bg-pink-500/20 rounded-full py-1 px-2 inline-block">{t('home.free_chat')}</div>
        </div>
      </Link>

      <Link to="/posts?category=NOTICE&sort=recent#posts-section" className="group">
        <div className="relative p-3 sm:p-4 bg-gradient-to-br from-red-600/70 to-orange-600/70 hover:from-red-600/90 hover:to-orange-600/90 active:from-red-700/90 active:to-orange-700/90 rounded-xl border border-red-500/50 hover:border-red-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-red-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
          <div className="w-8 h-8 mx-auto bg-gradient-to-br from-red-500/30 to-orange-500/30 rounded-full flex items-center justify-center mb-2">
            <div className="text-xl">📢</div>
          </div>
          <div className="text-sm font-bold text-red-100 mb-1 leading-tight">{t('home.notice')}</div>
          <div className="text-xs text-red-300 bg-red-500/20 rounded-full py-1 px-2 inline-block">{t('home.important_notice')}</div>
        </div>
      </Link>

      <Link to="/posts?category=STARLIGHT_CINEMA&sort=recent#posts-section" className="group">
        <div className="relative p-3 sm:p-4 bg-gradient-to-br from-purple-600/70 to-pink-600/70 hover:from-purple-600/90 hover:to-pink-600/90 active:from-purple-700/90 active:to-pink-700/90 rounded-xl border border-purple-500/50 hover:border-purple-400/70 transition-all duration-300 text-center transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-purple-500/30 overflow-hidden min-h-[90px] sm:min-h-[100px] flex flex-col justify-center">
          <div className="absolute top-1 right-1">
            <span className="bg-gradient-to-r from-purple-500 to-pink-500 text-white text-xs px-1.5 py-0.5 rounded-full font-bold shadow-md">🤖</span>
          </div>
          <div className="w-8 h-8 mx-auto bg-gradient-to-br from-purple-500/30 to-pink-500/30 rounded-full flex items-center justify-center mb-2">
            <div className="text-xl">🎬</div>
          </div>
          <div className="text-sm font-bold text-purple-100 mb-1 leading-tight">{t('home.star_cinema')}</div>
          <div className="text-xs text-purple-300 bg-purple-500/20 rounded-full py-1 px-2 inline-block">{t('home.video_curation')}</div>
        </div>
      </Link>
    </div>
  );
}
