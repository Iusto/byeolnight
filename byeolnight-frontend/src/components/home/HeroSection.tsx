import { useTranslation } from 'react-i18next';
import { useAuth } from '../../contexts/AuthContext';

export default function HeroSection() {
  const { user } = useAuth();
  const { t } = useTranslation();

  return (
    <div className="relative overflow-hidden bg-gradient-to-r from-purple-600/20 via-indigo-600/20 to-pink-600/20">
      {/* 글로우 오브 */}
      <div className="absolute top-5 right-5 sm:top-10 sm:right-10 w-16 h-16 sm:w-32 sm:h-32 bg-gradient-to-br from-purple-500/30 to-pink-500/30 rounded-full blur-2xl animate-pulse"></div>
      <div className="absolute bottom-5 left-5 sm:bottom-10 sm:left-10 w-12 h-12 sm:w-24 sm:h-24 bg-gradient-to-br from-blue-500/20 to-cyan-500/20 rounded-full blur-xl animate-pulse" style={{ animationDelay: '1s' }}></div>
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-72 h-72 sm:w-[28rem] sm:h-[28rem] bg-gradient-to-br from-indigo-500/10 via-purple-500/10 to-transparent rounded-full blur-3xl animate-nebula-flow"></div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8 sm:py-20 text-center relative">
        <div className="mb-2 sm:mb-8">
          <div className="inline-block animate-float">
            <span className="text-3xl sm:text-6xl md:text-8xl drop-shadow-[0_0_25px_rgba(168,85,247,0.6)]">🌌</span>
          </div>
        </div>

        <h1 className="text-3xl sm:text-5xl md:text-7xl font-extrabold mb-3 sm:mb-6 animate-fade-in px-2 tracking-tight">
          <span className="bg-gradient-to-r from-purple-300 via-pink-200 to-blue-300 bg-clip-text text-transparent animate-aurora-dance drop-shadow-[0_0_30px_rgba(139,92,246,0.35)]">
            {t('home.bigtitle')}
          </span>
        </h1>

        <p className="text-sm sm:text-xl md:text-2xl text-slate-200/90 mobile-text mb-4 sm:mb-8 max-w-5xl mx-auto animate-fade-in-delay px-4">
          {t('home.subtitle')}
        </p>

        <div className="inline-flex items-center gap-2 sm:gap-3 bg-gradient-to-r from-white/10 to-white/5 backdrop-blur-md px-4 py-2 sm:px-8 sm:py-4 rounded-full border border-white/20 shadow-2xl hover:shadow-glow transition-all duration-300 hover:scale-105 hover:border-purple-400/40 touch-target">
          <span className="text-lg sm:text-2xl animate-bounce">{user ? '👋' : '✨'}</span>
          <span className="text-white font-medium text-sm sm:text-base mobile-text">
            {user ? `${user.nickname}${t('home.welcome')}` : t('home.login_prompt')}
          </span>
        </div>
      </div>
    </div>
  );
}
