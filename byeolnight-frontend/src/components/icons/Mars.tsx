import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Mars({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="marsGradient" cx="50%" cy="30%" r="70%">
        <stop offset="0%" stopColor="#FFAB91" />
        <stop offset="30%" stopColor="#FF7043" />
        <stop offset="60%" stopColor="#D84315" />
        <stop offset="100%" stopColor="#BF360C" />
      </radialGradient>
      <radialGradient id="marsAtmosphere" cx="50%" cy="50%" r="80%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="70%" stopColor="transparent" />
        <stop offset="100%" stopColor="#FFAB91" opacity="0.2" />
      </radialGradient>
    </defs>
    
    {/* 얇은 대기층 */}
    <circle 
      cx="50" 
      cy="50" 
      r="37" 
      fill="url(#marsAtmosphere)" 
      className="animate-pulse"
      style={{ animationDuration: '5s' }}
    />
    
    {/* 화성 본체 */}
    <circle 
      cx="50" 
      cy="50" 
      r="34" 
      fill="url(#marsGradient)"
    />
    
    {/* 극지방 얼음 */}
    <ellipse cx="50" cy="20" rx="8" ry="4" fill="#FFFFFF" opacity="0.7" />
    <ellipse cx="50" cy="80" rx="6" ry="3" fill="#FFFFFF" opacity="0.6" />
    
    {/* 협곡과 크레이터 */}
    <path 
      d="M25 45 Q50 40, 75 50" 
      stroke="#BF360C" 
      strokeWidth="2" 
      fill="none" 
      opacity="0.6"
    />
    <circle cx="35" cy="35" r="3" fill="#BF360C" opacity="0.5" />
    <circle cx="65" cy="60" r="2.5" fill="#BF360C" opacity="0.6" />
    <circle cx="45" cy="65" r="2" fill="#BF360C" opacity="0.4" />
    
    {/* 먼지 폭풍 효과 */}
    <path 
      d="M20 55 Q40 50, 60 55 Q70 60, 80 55" 
      stroke="#FF7043" 
      strokeWidth="1" 
      fill="none" 
      opacity="0.3"
    />
  </svg>
);
}