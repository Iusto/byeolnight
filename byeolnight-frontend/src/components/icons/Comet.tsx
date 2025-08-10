import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Comet({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="cometCore" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="30%" stopColor="#FFEB3B" />
        <stop offset="70%" stopColor="#FF9800" />
        <stop offset="100%" stopColor="#FF5722" />
      </radialGradient>
      <linearGradient id="cometTail1" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#FFEB3B" opacity="0.8" />
        <stop offset="50%" stopColor="#FF9800" opacity="0.6" />
        <stop offset="100%" stopColor="transparent" />
      </linearGradient>
      <linearGradient id="cometTail2" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#81D4FA" opacity="0.7" />
        <stop offset="50%" stopColor="#2196F3" opacity="0.5" />
        <stop offset="100%" stopColor="transparent" />
      </linearGradient>
      <filter id="cometGlow">
        <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 혜성 꼬리 (가스) */}
    <path 
      d="M25 25 Q40 35, 55 45 Q70 55, 85 75" 
      stroke="url(#cometTail2)" 
      strokeWidth="8" 
      fill="none" 
      filter="url(#cometGlow)"
      className="animate-pulse"
      style={{ animationDuration: '3s' }}
    />
    <path 
      d="M20 30 Q35 40, 50 50 Q65 60, 80 80" 
      stroke="url(#cometTail2)" 
      strokeWidth="5" 
      fill="none" 
      opacity="0.7"
      className="animate-pulse"
      style={{ animationDuration: '4s', animationDelay: '1s' }}
    />
    
    {/* 혜성 꼬리 (먼지) */}
    <path 
      d="M30 20 Q45 30, 60 40 Q75 50, 90 70" 
      stroke="url(#cometTail1)" 
      strokeWidth="6" 
      fill="none" 
      filter="url(#cometGlow)"
      className="animate-pulse"
      style={{ animationDuration: '2.5s' }}
    />
    <path 
      d="M25 25 Q40 35, 55 45 Q70 55, 85 75" 
      stroke="url(#cometTail1)" 
      strokeWidth="3" 
      fill="none" 
      opacity="0.8"
      className="animate-pulse"
      style={{ animationDuration: '3.5s', animationDelay: '0.5s' }}
    />
    
    {/* 혜성 핵 */}
    <circle 
      cx="25" 
      cy="25" 
      r="8" 
      fill="url(#cometCore)"
      filter="url(#cometGlow)"
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    
    {/* 코마 (대기) */}
    <circle 
      cx="25" 
      cy="25" 
      r="12" 
      fill="#FFEB3B" 
      opacity="0.3"
      className="animate-pulse"
      style={{ animationDuration: '4s' }}
    />
    
    {/* 꼬리 입자들 */}
    <circle cx="35" cy="35" r="1" fill="#FFEB3B" className="animate-ping" />
    <circle cx="45" cy="45" r="0.8" fill="#FF9800" className="animate-ping" style={{ animationDelay: '0.5s' }} />
    <circle cx="55" cy="55" r="1.2" fill="#81D4FA" className="animate-ping" style={{ animationDelay: '1s' }} />
    <circle cx="65" cy="65" r="0.6" fill="#2196F3" className="animate-ping" style={{ animationDelay: '1.5s' }} />
    <circle cx="75" cy="75" r="0.9" fill="#FFEB3B" className="animate-ping" style={{ animationDelay: '2s' }} />
    
    {/* 얼음 결정들 */}
    <polygon points="40,30 42,28 44,30 42,32" fill="#81D4FA" opacity="0.8" className="animate-twinkle" />
    <polygon points="50,40 52,38 54,40 52,42" fill="#E1F5FE" opacity="0.7" className="animate-twinkle" style={{ animationDelay: '1s' }} />
    <polygon points="60,50 62,48 64,50 62,52" fill="#81D4FA" opacity="0.9" className="animate-twinkle" style={{ animationDelay: '2s' }} />
    
    {/* 태양풍 효과 */}
    <line x1="15" y1="15" x2="20" y2="20" stroke="#FFFFFF" strokeWidth="0.5" opacity="0.6" />
    <line x1="10" y1="20" x2="15" y2="25" stroke="#FFFFFF" strokeWidth="0.3" opacity="0.5" />
    <line x1="20" y1="10" x2="25" y2="15" stroke="#FFFFFF" strokeWidth="0.4" opacity="0.7" />
  </svg>
);
}