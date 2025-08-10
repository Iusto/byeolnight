import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Sun({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="sunCore" cx="50%" cy="50%" r="60%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="30%" stopColor="#FFEB3B" />
        <stop offset="70%" stopColor="#FF9800" />
        <stop offset="100%" stopColor="#FF5722" />
      </radialGradient>
      <radialGradient id="sunCorona" cx="50%" cy="50%" r="80%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="60%" stopColor="transparent" />
        <stop offset="100%" stopColor="#FF9800" opacity="0.4" />
      </radialGradient>
    </defs>
    
    {/* 코로나 */}
    <circle 
      cx="50" 
      cy="50" 
      r="45" 
      fill="url(#sunCorona)"
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    
    {/* 태양광선 */}
    <g className="animate-spin" style={{ animationDuration: '20s', transformOrigin: '50px 50px' }}>
      <line x1="50" y1="5" x2="50" y2="15" stroke="#FFEB3B" strokeWidth="3" />
      <line x1="50" y1="85" x2="50" y2="95" stroke="#FFEB3B" strokeWidth="3" />
      <line x1="5" y1="50" x2="15" y2="50" stroke="#FFEB3B" strokeWidth="3" />
      <line x1="85" y1="50" x2="95" y2="50" stroke="#FFEB3B" strokeWidth="3" />
      
      <line x1="21.5" y1="21.5" x2="28.5" y2="28.5" stroke="#FF9800" strokeWidth="2" />
      <line x1="78.5" y1="78.5" x2="71.5" y2="71.5" stroke="#FF9800" strokeWidth="2" />
      <line x1="78.5" y1="21.5" x2="71.5" y2="28.5" stroke="#FF9800" strokeWidth="2" />
      <line x1="21.5" y1="78.5" x2="28.5" y2="71.5" stroke="#FF9800" strokeWidth="2" />
    </g>
    
    {/* 태양 본체 */}
    <circle 
      cx="50" 
      cy="50" 
      r="25" 
      fill="url(#sunCore)"
    />
    
    {/* 흑점들 */}
    <circle cx="45" cy="45" r="2" fill="#FF5722" opacity="0.7" />
    <circle cx="58" cy="52" r="1.5" fill="#FF5722" opacity="0.6" />
    <circle cx="42" cy="58" r="1" fill="#FF5722" opacity="0.5" />
    
    {/* 플레어 */}
    <ellipse cx="35" cy="35" rx="3" ry="1" fill="#FFFFFF" opacity="0.8" />
    <ellipse cx="65" cy="40" rx="2" ry="1" fill="#FFFFFF" opacity="0.7" />
  </svg>
);
}