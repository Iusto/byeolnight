import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function SpaceStation({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <linearGradient id="stationBody" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#E0E0E0" />
        <stop offset="50%" stopColor="#BDBDBD" />
        <stop offset="100%" stopColor="#757575" />
      </linearGradient>
      <radialGradient id="solarPanel" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#1565C0" />
        <stop offset="50%" stopColor="#1976D2" />
        <stop offset="100%" stopColor="#0D47A1" />
      </radialGradient>
      <radialGradient id="stationGlow" cx="50%" cy="50%" r="80%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="70%" stopColor="transparent" />
        <stop offset="100%" stopColor="#81D4FA" opacity="0.4" />
      </radialGradient>
      <filter id="techGlow">
        <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 우주 정거장 글로우 */}
    <circle 
      cx="50" 
      cy="50" 
      r="45" 
      fill="url(#stationGlow)"
      className="animate-pulse"
      style={{ animationDuration: '4s' }}
    />
    
    {/* 태양 전지판 (왼쪽) */}
    <rect 
      x="10" y="35" width="25" height="30" 
      fill="url(#solarPanel)" 
      filter="url(#techGlow)"
      className="animate-pulse"
      style={{ animationDuration: '3s' }}
    />
    <g className="animate-pulse" style={{ animationDuration: '2s' }}>
      <line x1="12" y1="40" x2="33" y2="40" stroke="#81D4FA" strokeWidth="0.5" />
      <line x1="12" y1="45" x2="33" y2="45" stroke="#81D4FA" strokeWidth="0.5" />
      <line x1="12" y1="50" x2="33" y2="50" stroke="#81D4FA" strokeWidth="0.5" />
      <line x1="12" y1="55" x2="33" y2="55" stroke="#81D4FA" strokeWidth="0.5" />
      <line x1="12" y1="60" x2="33" y2="60" stroke="#81D4FA" strokeWidth="0.5" />
    </g>
    
    {/* 태양 전지판 (오른쪽) */}
    <rect 
      x="65" y="35" width="25" height="30" 
      fill="url(#solarPanel)" 
      filter="url(#techGlow)"
      className="animate-pulse"
      style={{ animationDuration: '3s', animationDelay: '1s' }}
    />
    <g className="animate-pulse" style={{ animationDuration: '2s', animationDelay: '1s' }}>
      <line x1="67" y1="40" x2="88" y2="40" stroke="#81D4FA" strokeWidth="0.5" />
      <line x1="67" y1="45" x2="88" y2="45" stroke="#81D4FA" strokeWidth="0.5" />
      <line x1="67" y1="50" x2="88" y2="50" stroke="#81D4FA" strokeWidth="0.5" />
      <line x1="67" y1="55" x2="88" y2="55" stroke="#81D4FA" strokeWidth="0.5" />
      <line x1="67" y1="60" x2="88" y2="60" stroke="#81D4FA" strokeWidth="0.5" />
    </g>
    
    {/* 연결 구조물 */}
    <rect x="35" y="48" width="30" height="4" fill="url(#stationBody)" />
    
    {/* 중앙 모듈 */}
    <circle 
      cx="50" 
      cy="50" 
      r="15" 
      fill="url(#stationBody)"
      filter="url(#techGlow)"
      className="animate-spin"
      style={{ animationDuration: '20s' }}
    />
    
    {/* 도킹 포트들 */}
    <circle cx="50" cy="30" r="3" fill="#757575" />
    <circle cx="50" cy="70" r="3" fill="#757575" />
    <circle cx="30" cy="50" r="3" fill="#757575" />
    <circle cx="70" cy="50" r="3" fill="#757575" />
    
    {/* 안테나 */}
    <line x1="50" y1="35" x2="50" y2="25" stroke="#757575" strokeWidth="1" />
    <line x1="50" y1="65" x2="50" y2="75" stroke="#757575" strokeWidth="1" />
    <circle cx="50" cy="23" r="1.5" fill="#FF5722" className="animate-ping" />
    <circle cx="50" cy="77" r="1.5" fill="#4CAF50" className="animate-ping" style={{ animationDelay: '1s' }} />
    
    {/* 조명 */}
    <circle cx="45" cy="45" r="1" fill="#FFEB3B" className="animate-ping" />
    <circle cx="55" cy="45" r="1" fill="#00E5FF" className="animate-ping" style={{ animationDelay: '0.5s' }} />
    <circle cx="45" cy="55" r="1" fill="#4CAF50" className="animate-ping" style={{ animationDelay: '1s' }} />
    <circle cx="55" cy="55" r="1" fill="#FF5722" className="animate-ping" style={{ animationDelay: '1.5s' }} />
    
    {/* 통신 신호 */}
    <circle 
      cx="50" 
      cy="50" 
      r="25" 
      fill="none" 
      stroke="#00E5FF" 
      strokeWidth="0.5" 
      opacity="0.4"
      className="animate-ping"
      style={{ animationDuration: '3s' }}
    />
    <circle 
      cx="50" 
      cy="50" 
      r="35" 
      fill="none" 
      stroke="#00E5FF" 
      strokeWidth="0.3" 
      opacity="0.3"
      className="animate-ping"
      style={{ animationDuration: '4s', animationDelay: '1s' }}
    />
    
    {/* 우주 쓰레기 */}
    <circle cx="20" cy="20" r="0.5" fill="#757575" className="animate-pulse" />
    <circle cx="80" cy="80" r="0.3" fill="#BDBDBD" className="animate-pulse" style={{ animationDelay: '2s' }} />
  </svg>
);
}