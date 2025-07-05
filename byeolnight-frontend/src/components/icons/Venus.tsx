import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Venus({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="venusCore" cx="45%" cy="35%" r="80%">
        <stop offset="0%" stopColor="#FFF8E1" />
        <stop offset="30%" stopColor="#FFECB3" />
        <stop offset="60%" stopColor="#FFB74D" />
        <stop offset="100%" stopColor="#E65100" />
      </radialGradient>
      <radialGradient id="venusAtmo" cx="50%" cy="50%" r="90%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="60%" stopColor="#FFF3E0" opacity="0.2" />
        <stop offset="100%" stopColor="#FFCC02" opacity="0.4" />
      </radialGradient>
      <linearGradient id="venusStorm" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stopColor="#FFE0B2" opacity="0.8" />
        <stop offset="50%" stopColor="#FFF3E0" opacity="0.6" />
        <stop offset="100%" stopColor="#FFE0B2" opacity="0.8" />
      </linearGradient>
      <filter id="venusGlow">
        <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 대기 글로우 */}
    <circle 
      cx="50" 
      cy="50" 
      r="42" 
      fill="url(#venusAtmo)" 
      className="animate-pulse"
      style={{ animationDuration: '4s' }}
    />
    
    {/* 금성 본체 */}
    <circle 
      cx="50" 
      cy="50" 
      r="35" 
      fill="url(#venusCore)"
      filter="url(#venusGlow)"
    />
    
    {/* 회전하는 구름 층 */}
    <g className="animate-spin" style={{ animationDuration: '15s', transformOrigin: '50px 50px' }}>
      <path 
        d="M18 42 Q35 35, 52 42 Q70 48, 82 42" 
        stroke="url(#venusStorm)" 
        strokeWidth="3" 
        fill="none" 
        opacity="0.7"
      />
      <path 
        d="M20 58 Q40 52, 60 58 Q75 63, 80 58" 
        stroke="url(#venusStorm)" 
        strokeWidth="2.5" 
        fill="none" 
        opacity="0.6"
      />
    </g>
    
    {/* 산성 구름 */}
    <ellipse cx="35" cy="30" rx="8" ry="3" fill="#FFF3E0" opacity="0.5" className="animate-pulse" />
    <ellipse cx="65" cy="65" rx="6" ry="2" fill="#FFE0B2" opacity="0.6" className="animate-pulse" style={{ animationDelay: '1s' }} />
    
    {/* 대기 파동 */}
    <circle 
      cx="50" 
      cy="50" 
      r="38" 
      fill="none" 
      stroke="#FFCC02" 
      strokeWidth="0.5" 
      opacity="0.4"
      className="animate-ping"
      style={{ animationDuration: '5s' }}
    />
    
    {/* 온실효과 빛 */}
    <circle cx="45" cy="40" r="1" fill="#FFF8E1" className="animate-ping" />
    <circle cx="60" cy="50" r="0.8" fill="#FFF8E1" className="animate-ping" style={{ animationDelay: '2s' }} />
  </svg>
  );
}