interface StellaIconProps {
  iconUrl: string;
  animationClass?: string;
  grade: string;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const gradeColors: Record<string, string> = {
  COMMON: '#9CA3AF',
  RARE: '#3B82F6', 
  LEGENDARY: '#F59E0B',
  EVENT: '#EF4444'
};

export default function StellaIcon({ 
  iconUrl, 
  animationClass = '', 
  grade, 
  size = 'md',
  className = ''
}: StellaIconProps) {
  const sizeClasses = {
    sm: 'w-6 h-6',
    md: 'w-8 h-8', 
    lg: 'w-12 h-12'
  };

  const gradeColor = gradeColors[grade] || gradeColors.COMMON;

  return (
    <div 
      className={`${sizeClasses[size]} ${animationClass} ${className} flex items-center justify-center`}
      style={{ 
        filter: `drop-shadow(0 0 4px ${gradeColor}40)`,
        borderRadius: '50%'
      }}
    >
      <img 
        src={iconUrl} 
        alt="스텔라 아이콘"
        className="w-full h-full object-contain"
        style={{ filter: `brightness(1.1) saturate(1.2)` }}
      />
    </div>
  );
}