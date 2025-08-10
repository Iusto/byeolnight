import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Wormhole({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="wormholeCenter" cx="50%" cy="50%" r="20%">
        <stop offset="0%" stopColor="#000000" />
        <stop offset="100%" stopColor="#1A1A1A" />
      </radialGradient>
      <radialGradient id="wormholeTunnel" cx="50%" cy="50%" r="80%">
        <stop offset="20%" stopColor="transparent" />
        <stop offset="30%" stopColor="#9C27B0" opacity="0.3" />
        <stop offset="40%" stopColor="#673AB7" opacity="0.5" />
        <stop offset="50%" stopColor="#3F51B5" opacity="0.7" />
        <stop offset="60%" stopColor="#2196F3" opacity="0.6" />
        <stop offset="70%" stopColor="#00BCD4" opacity="0.4" />
        <stop offset="80%" stopColor="#4CAF50" opacity="0.3" />
        <stop offset="100%" stopColor="transparent" />
      </radialGradient>
      <linearGradient id="spacetimeGrid" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#00E5FF" opacity="0.6" />
        <stop offset="50%" stopColor="#9C27B0" opacity="0.8" />
        <stop offset="100%" stopColor="#FF6B35" opacity="0.6" />
      </linearGradient>
      <filter id="wormholeGlow">
        <feGaussianBlur stdDeviation="5" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 시공간 왜곡 격자 */}
    <g className="animate-spin" style={{ animationDuration: '20s', transformOrigin: '50px 50px' }}>
      <circle cx="50" cy="50" r="40" fill="none" stroke="url(#spacetimeGrid)" strokeWidth="0.5" opacity="0.4" />
      <circle cx="50" cy="50" r="35" fill="none" stroke="url(#spacetimeGrid)" strokeWidth="0.8" opacity="0.5" />
      <circle cx="50" cy="50" r="30" fill="none" stroke="url(#spacetimeGrid)" strokeWidth="1" opacity="0.6" />
      <circle cx="50" cy="50" r="25" fill="none" stroke="url(#spacetimeGrid)" strokeWidth="1.2" opacity="0.7" />
    </g>
    
    {/* 웜홀 터널 */}
    <circle 
      cx="50" 
      cy="50" 
      r="40" 
      fill="url(#wormholeTunnel)"
      filter="url(#wormholeGlow)"
      className="animate-pulse"
      style={{ animationDuration: '4s' }}
    />
    
    {/* 차원 고리들 */}
    <g className="animate-spin" style={{ animationDuration: '8s', animationDirection: 'reverse', transformOrigin: '50px 50px' }}>
      <ellipse cx="50" cy="50" rx="38" ry="8" fill="none" stroke="#9C27B0" strokeWidth="1.5" opacity="0.8" />
      <ellipse cx="50" cy="50" rx="32" ry="6" fill="none" stroke="#673AB7" strokeWidth="1.2" opacity="0.9" />
      <ellipse cx="50" cy="50" rx="26" ry="4" fill="none" stroke="#3F51B5" strokeWidth="1" opacity="1" />
    </g>
    
    {/* 웜홀 중심 */}
    <circle 
      cx="50" 
      cy="50" 
      r="8" 
      fill="url(#wormholeCenter)"
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    
    {/* 차원 간 에너지 흐름 */}
    <g className="animate-spin" style={{ animationDuration: '6s', transformOrigin: '50px 50px' }}>
      <path 
        d="M50 20 Q60 30, 50 40 Q40 30, 50 20" 
        fill="#00E5FF" 
        opacity="0.6"
        className="animate-pulse"
        style={{ animationDuration: '3s' }}
      />
      <path 
        d="M50 60 Q60 70, 50 80 Q40 70, 50 60" 
        fill="#00E5FF" 
        opacity="0.6"
        className="animate-pulse"
        style={{ animationDuration: '3s', animationDelay: '1.5s' }}
      />
      <path 
        d="M20 50 Q30 40, 40 50 Q30 60, 20 50" 
        fill="#9C27B0" 
        opacity="0.7"
        className="animate-pulse"
        style={{ animationDuration: '3s', animationDelay: '0.5s' }}
      />
      <path 
        d="M60 50 Q70 40, 80 50 Q70 60, 60 50" 
        fill="#9C27B0" 
        opacity="0.7"
        className="animate-pulse"
        style={{ animationDuration: '3s', animationDelay: '2s' }}
      />
    </g>
    
    {/* 양자 요동 */}
    <circle cx="30" cy="30" r="0.8" fill="#00E5FF" className="animate-ping" />
    <circle cx="70" cy="30" r="0.6" fill="#9C27B0" className="animate-ping" style={{ animationDelay: '1s' }} />
    <circle cx="30" cy="70" r="0.7" fill="#FF6B35" className="animate-ping" style={{ animationDelay: '2s' }} />
    <circle cx="70" cy="70" r="0.5" fill="#00E5FF" className="animate-ping" style={{ animationDelay: '0.5s' }} />
    
    {/* 시공간 특이점들 */}
    <circle cx="35" cy="35" r="1" fill="#FFFFFF" opacity="0.9" className="animate-ping" style={{ animationDelay: '3s' }} />
    <circle cx="65" cy="35" r="0.8" fill="#FFFFFF" opacity="0.8" className="animate-ping" style={{ animationDelay: '1.5s' }} />
    <circle cx="35" cy="65" r="0.9" fill="#FFFFFF" opacity="0.7" className="animate-ping" style={{ animationDelay: '2.5s' }} />
    <circle cx="65" cy="65" r="0.6" fill="#FFFFFF" opacity="0.9" className="animate-ping" style={{ animationDelay: '0.8s' }} />
    
    {/* 차원 경계면 */}
    <circle 
      cx="50" 
      cy="50" 
      r="20" 
      fill="none" 
      stroke="#FFFFFF" 
      strokeWidth="0.3" 
      opacity="0.5"
      className="animate-ping"
      style={{ animationDuration: '5s' }}
    />
  </svg>
);
}