import { Link } from 'react-router-dom';

interface SectionProps {
  title: string;
  icon: string;
  link: string;
  bgColor: string;
  borderColor: string;
  children: React.ReactNode;
}

export default function Section({ title, icon, link, bgColor, borderColor, children }: SectionProps) {
  return (
    <div className={`${bgColor.replace('/30', '/50').replace('/20', '/40')} ${borderColor.replace('/20', '/40')} backdrop-blur-md rounded-2xl p-4 sm:p-6 border shadow-lg hover:shadow-xl transition-all duration-300 relative overflow-hidden group`}>
      <div className="absolute inset-0 bg-gradient-to-br from-white/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>
      
      <div className="flex justify-between items-center mb-3 sm:mb-6 relative z-10">
        <div className="flex items-center gap-2 sm:gap-4">
          <div className="relative">
            <div className="w-6 h-6 sm:w-12 sm:h-12 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-sm sm:text-2xl shadow-lg hover:shadow-purple-500/50 transition-all duration-300 hover:scale-110">
              {icon}
            </div>
            <div className="absolute inset-0 bg-gradient-to-r from-purple-400 to-pink-400 rounded-full blur opacity-30 animate-pulse"></div>
          </div>
          <h3 className="text-base sm:text-2xl font-bold text-white" style={{textShadow: '0 2px 4px rgba(0,0,0,0.9)', filter: 'brightness(1.2)'}}>
            {title}
          </h3>
        </div>
        <Link 
          to={`${link}#posts-section`} 
          className="group/btn flex items-center gap-1 sm:gap-2 px-3 py-2 sm:px-6 sm:py-3 bg-gradient-to-r from-purple-600 via-purple-700 to-pink-600 hover:from-purple-700 hover:via-purple-800 hover:to-pink-700 text-white rounded-full text-xs sm:text-sm font-medium transition-all duration-300 shadow-lg hover:shadow-purple-500/50 hover:scale-105 backdrop-blur-sm border border-purple-400/30"
        >
          <span className="hidden sm:inline">더보기</span>
          <span className="sm:hidden text-xs">더보기</span>
          <span className="group-hover/btn:translate-x-1 group-hover/btn:scale-110 transition-all duration-200 text-xs sm:text-sm">🚀</span>
        </Link>
      </div>
      <div className="relative z-10">
        {children}
      </div>
    </div>
  );
}
