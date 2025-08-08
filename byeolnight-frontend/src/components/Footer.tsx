import { useTranslation } from 'react-i18next';

export default function Footer() {
  const { t } = useTranslation();
  
  return (
    <footer className="relative bg-gradient-to-r from-slate-900 via-purple-900/50 to-slate-900 text-white mt-12 overflow-hidden">
      {/* 배경 별 효과 */}
      <div className="absolute inset-0 opacity-20">
        <div className="absolute top-4 left-10 w-1 h-1 bg-white rounded-full animate-pulse"></div>
        <div className="absolute top-8 right-20 w-1 h-1 bg-purple-300 rounded-full animate-pulse" style={{animationDelay: '0.5s'}}></div>
        <div className="absolute bottom-6 left-1/4 w-1 h-1 bg-pink-300 rounded-full animate-pulse" style={{animationDelay: '1s'}}></div>
        <div className="absolute bottom-4 right-1/3 w-1 h-1 bg-blue-300 rounded-full animate-pulse" style={{animationDelay: '1.5s'}}></div>
      </div>
      
      <div className="relative max-w-7xl mx-auto px-4 py-8">
        <div className="text-center space-y-4">
          {/* 로고 섹션 */}
          <div className="flex items-center justify-center gap-3 mb-6">
            <span className="text-3xl">🌌</span>
            <h3 className="text-2xl font-bold bg-gradient-to-r from-purple-400 via-pink-400 to-indigo-400 bg-clip-text text-transparent">
              {t('home.bigtitle')}
            </h3>
            <span className="text-3xl">✨</span>
          </div>
          
          {/* 설명 */}
          <p className="text-gray-300 text-lg mb-6">
            {t('home.footer_text')}
          </p>
          
          {/* 구분선 */}
          <div className="w-32 h-px bg-gradient-to-r from-transparent via-purple-500 to-transparent mx-auto mb-6"></div>
          
          {/* 저작권 정보 */}
          <div className="flex flex-col md:flex-row items-center justify-center gap-4 text-sm text-gray-400">
            <p>© 2025 {t('home.bigtitle')}. All rights reserved.</p>
            <div className="hidden md:block w-px h-4 bg-gray-600"></div>
            <p className="flex items-center gap-2">
              <span>Made with</span>
              <span className="text-red-400 animate-pulse">❤️</span>
              <span>for space lovers</span>
            </p>
          </div>
          
          {/* 소셜/링크 섹션 */}
          <div className="flex items-center justify-center gap-6 pt-4">
            <div className="flex items-center gap-2 text-purple-300 hover:text-purple-200 transition-colors cursor-pointer">
              <span>🚀</span>
              <span className="text-sm">Explore Universe</span>
            </div>
            <div className="flex items-center gap-2 text-pink-300 hover:text-pink-200 transition-colors cursor-pointer">
              <span>🌟</span>
              <span className="text-sm">Share Dreams</span>
            </div>
            <div className="flex items-center gap-2 text-blue-300 hover:text-blue-200 transition-colors cursor-pointer">
              <span>🌙</span>
              <span className="text-sm">Night Sky</span>
            </div>
          </div>
        </div>
      </div>
      
      {/* 하단 그라데이션 */}
      <div className="absolute bottom-0 left-0 right-0 h-1 bg-gradient-to-r from-purple-500 via-pink-500 to-indigo-500"></div>
    </footer>
  );
}
