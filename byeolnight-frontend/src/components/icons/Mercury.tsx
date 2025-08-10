import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Mercury({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="mercuryCore" cx="40%" cy="30%" r="70%">
        <stop offset="0%" stopColor="#FFD54F" />
        <stop offset="30%" stopColor="#FF8A65" />
        <stop offset="60%" stopColor="#8D6E63" />
        <stop offset="100%" stopColor="#3E2723" />
      </radialGradient>
      <radialGradient id="mercuryHeat" cx="60%" cy="40%" r="50%">
        <stop offset="0%" stopColor="#FF6F00" opacity="0.8" />
        <stop offset="50%" stopColor="#FF8F00" opacity="0.4" />
        <stop offset="100%" stopColor="transparent" />
      </radialGradient>
      <filter id="mercuryGlow">
        <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 태양열 오라 */}
    <circle 
      cx="50" 
      cy="50" 
      r="42" 
      fill="url(#mercuryHeat)" 
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    
    {/* 수성 본체 */}
    <circle 
      cx="50" 
      cy="50" 
      r="35" 
      fill="url(#mercuryCore)" 
      filter="url(#mercuryGlow)"
    />
    
    {/* 대형 크레이터 */}
    <ellipse cx="38" cy="32" rx="6" ry="4" fill="#3E2723" opacity="0.7" />
    <ellipse cx="62" cy="58" rx="5" ry="3" fill="#3E2723" opacity="0.6" />
    <ellipse cx="45" cy="65" rx="4" ry="2" fill="#3E2723" opacity="0.8" />
    
    {/* 작은 크레이터들 */}
    <circle cx="30" cy="45" r="2" fill="#5D4037" opacity="0.5" />
    <circle cx="70" cy="35" r="1.5" fill="#5D4037" opacity="0.6" />
    <circle cx="55" cy="25" r="1" fill="#5D4037" opacity="0.4" />
    <circle cx="25" cy="60" r="1.5" fill="#5D4037" opacity="0.7" />
    
    {/* 열기 파동 */}
    <circle 
      cx="50" 
      cy="50" 
      r="38" 
      fill="none" 
      stroke="#FF8F00" 
      strokeWidth="0.5" 
      opacity="0.3"
      className="animate-ping"
      style={{ animationDuration: '3s' }}
    />
    
    {/* 표면 균열 */}
    <path d="M35 40 Q45 35 55 40" stroke="#5D4037" strokeWidth="0.8" opacity="0.6" fill="none" />
    <path d="M40 55 Q50 60 60 55" stroke="#5D4037" strokeWidth="0.6" opacity="0.5" fill="none" />
    </svg>
  );
}