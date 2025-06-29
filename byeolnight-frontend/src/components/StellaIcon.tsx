import React from 'react';

interface StellaIconProps {
  iconUrl?: string;
  animationClass?: string;
  grade?: string;
  size?: 'sm' | 'md' | 'lg';
  showTooltip?: boolean;
  tooltipText?: string;
}

const StellaIcon: React.FC<StellaIconProps> = ({
  iconUrl,
  animationClass = '',
  grade = 'COMMON',
  size = 'md',
  showTooltip = false,
  tooltipText = ''
}) => {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8'
  };

  const gradeEffects = {
    COMMON: '',
    RARE: 'drop-shadow-[0_0_4px_rgba(59,130,246,0.5)]',
    LEGENDARY: 'drop-shadow-[0_0_6px_rgba(245,158,11,0.7)]',
    EVENT: 'drop-shadow-[0_0_8px_rgba(239,68,68,0.8)]'
  };

  if (!iconUrl) return null;

  return (
    <div className="relative inline-block group">
      <img
        src={iconUrl}
        alt="스텔라 아이콘"
        className={`
          ${sizeClasses[size]} 
          ${animationClass} 
          ${gradeEffects[grade as keyof typeof gradeEffects]}
          object-contain
        `}
      />
      
      {showTooltip && tooltipText && (
        <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-2 py-1 bg-gray-900 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity duration-200 whitespace-nowrap z-10">
          {tooltipText}
          <div className="absolute top-full left-1/2 transform -translate-x-1/2 border-4 border-transparent border-t-gray-900"></div>
        </div>
      )}
    </div>
  );
};

export default StellaIcon;