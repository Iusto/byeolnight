import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function UFO({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="ufoBody" cx="50%" cy="40%" r="60%">
        <stop offset="0%" stopColor="#E0E0E0" />
        <stop offset="50%" stopColor="#BDBDBD" />
        <stop offset="100%" stopColor="#757575" />
      </radialGradient>
      <radialGradient id="ufoDome" cx="50%" cy="30%" r="50%">
        <stop offset="0%" stopColor="#81D4FA" opacity="0.8" />
        <stop offset="70%" stopColor="#2196F3" opacity="0.6" />
        <stop offset="100%" stopColor="#1565C0" opacity="0.4" />
      </radialGradient>
      <linearGradient id="beamGradient" x1="50%" y1="0%" x2="50%" y2="100%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="30%" stopColor="#00E5FF" opacity="0.3" />
        <stop offset="70%" stopColor="#81D4FA" opacity="0.6" />
        <stop offset="100%" stopColor="#FFFFFF" opacity="0.8" />
      </linearGradient>
      <filter id="ufoGlow">
        <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 견인 광선 */}
    <polygon 
      points="35,70 65,70 55,95 45,95" 
      fill="url(#beamGradient)" 
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    
    {/* UFO 본체 */}
    <ellipse 
      cx="50" 
      cy="45" 
      rx="30" 
      ry="8" 
      fill="url(#ufoBody)"
      filter="url(#ufoGlow)"
      className="animate-bounce"
      style={{ animationDuration: '4s' }}
    />
    
    {/* UFO 돔 */}
    <ellipse 
      cx="50" 
      cy="38" 
      rx="20" 
      ry="12" 
      fill="url(#ufoDome)"
      className="animate-pulse"
      style={{ animationDuration: '3s' }}
    />
    
    {/* 조명 */}
    <circle cx="35" cy="45" r="2" fill="#FFEB3B" className="animate-ping" />
    <circle cx="50" cy="45" r="2" fill="#FF5722" className="animate-ping" style={{ animationDelay: '0.5s' }} />
    <circle cx="65" cy="45" r="2" fill="#4CAF50" className="animate-ping" style={{ animationDelay: '1s' }} />
    
    {/* 안테나 */}
    <line x1="50" y1="26" x2="50" y2="20" stroke="#757575" strokeWidth="1" />
    <circle cx="50" cy="18" r="2" fill="#FF5722" className="animate-pulse" />
    
    {/* 광선 입자들 */}
    <circle cx="45" cy="80" r="0.5" fill="#00E5FF" className="animate-ping" />
    <circle cx="55" cy="85" r="0.3" fill="#81D4FA" className="animate-ping" style={{ animationDelay: '1s' }} />
    <circle cx="50" cy="75" r="0.4" fill="#FFFFFF" className="animate-ping" style={{ animationDelay: '0.5s' }} />
    
    {/* 에너지 파동 */}
    <circle 
      cx="50" 
      cy="45" 
      r="35" 
      fill="none" 
      stroke="#00E5FF" 
      strokeWidth="0.5" 
      opacity="0.3"
      className="animate-ping"
      style={{ animationDuration: '3s' }}
    />
  </svg>
);
}