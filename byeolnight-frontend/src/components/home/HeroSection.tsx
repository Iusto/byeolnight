import { useTranslation } from 'react-i18next';
import { useAuth } from '../../contexts/AuthContext';

export default function HeroSection() {
  const { user } = useAuth();
  const { t } = useTranslation();

  return (
    <div className="relative overflow-hidden bg-gradient-to-r from-purple-600/20 via-indigo-600/20 to-pink-600/20">
      <div className="absolute top-5 right-5 sm:top-10 sm:right-10 w-16 h-16 sm:w-32 sm:h-32 bg-gradient-to-br from-purple-500/30 to-pink-500/30 rounded-full blur-xl animate-pulse"></div>
      <div className="absolute bottom-5 left-5 sm:bottom-10 sm:left-10 w-12 h-12 sm:w-24 sm:h-24 bg-gradient-to-br from-blue-500/20 to-cyan-500/20 rounded-full blur-lg animate-pulse" style={{animationDelay: '1s'}}></div>
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8 sm:py-20 text-center relative">
        <div className="mb-2 sm:mb-8">
          <div className="inline-block animate-float">
            <span className="text-2xl sm:text-6xl md:text-8xl drop-shadow-2xl">🌌</span>
          </div>
        </div>
        <h1 className="text-2xl sm:text-5xl md:text-7xl font-bold mb-2 sm:mb-6 animate-fade-in px-2">
          <span className="text-white mobile-text">
            {t('home.bigtitle')}
          </span>
        </h1>
        <p className="text-sm sm:text-xl md:text-2xl text-white mobile-text mb-3 sm:mb-8 max-w-5xl mx-auto animate-fade-in-delay px-4">
          {t('home.subtitle')}
        </p>
        <div className="inline-flex items-center gap-2 sm:gap-3 bg-gradient-to-r from-white/10 to-white/5 backdrop-blur-md px-4 py-2 sm:px-8 sm:py-4 rounded-full border border-white/20 shadow-2xl hover:shadow-purple-500/25 transition-all duration-300 hover:scale-105 touch-target">
          <span className="text-lg sm:text-2xl animate-bounce">{user ? '👋' : '✨'}</span>
          <span className="text-white font-medium text-sm sm:text-base mobile-text">
            {user ? `${user.nickname}${t('home.welcome')}` : t('home.login_prompt')}
          </span>
        </div>
      </div>
    </div>
  );
}
