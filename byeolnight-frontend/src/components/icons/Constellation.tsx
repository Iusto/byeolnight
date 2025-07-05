import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Constellation({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="starGlow" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="50%" stopColor="#FFEB3B" />
        <stop offset="100%" stopColor="#FF9800" />
      </radialGradient>
      <linearGradient id="connectionGlow" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#64FFDA" />
        <stop offset="50%" stopColor="#18FFFF" />
        <stop offset="100%" stopColor="#00BCD4" />
      </linearGradient>
      <filter id="constellationGlow">
        <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 별자리 연결선들 - 빛나는 효과 */}
    <g className="animate-pulse" style={{ animationDuration: '4s' }}>
      <line x1="20" y1="25" x2="35" y2="20" stroke="url(#connectionGlow)" strokeWidth="2" opacity="0.8" />
      <line x1="35" y1="20" x2="50" y2="30" stroke="url(#connectionGlow)" strokeWidth="2" opacity="0.8" />
      <line x1="50" y1="30" x2="65" y2="25" stroke="url(#connectionGlow)" strokeWidth="2" opacity="0.8" />
      <line x1="65" y1="25" x2="80" y2="35" stroke="url(#connectionGlow)" strokeWidth="2" opacity="0.8" />
      <line x1="50" y1="30" x2="45" y2="50" stroke="url(#connectionGlow)" strokeWidth="2" opacity="0.8" />
      <line x1="45" y1="50" x2="30" y2="60" stroke="url(#connectionGlow)" strokeWidth="2" opacity="0.8" />
      <line x1="45" y1="50" x2="60" y2="55" stroke="url(#connectionGlow)" strokeWidth="2" opacity="0.8" />
      <line x1="60" y1="55" x2="75" y2="70" stroke="url(#connectionGlow)" strokeWidth="2" opacity="0.8" />
      <line x1="30" y1="60" x2="25" y2="80" stroke="url(#connectionGlow)" strokeWidth="2" opacity="0.8" />
    </g>
    
    {/* 주요 별들 (밝은 별) */}
    <circle 
      cx="20" cy="25" r="3" 
      fill="url(#starGlow)" 
      filter="url(#constellationGlow)"
      className="animate-ping"
    />
    <circle 
      cx="50" cy="30" r="4" 
      fill="url(#starGlow)" 
      filter="url(#constellationGlow)"
      className="animate-ping"
      style={{ animationDelay: '0.5s' }}
    />
    <circle 
      cx="80" cy="35" r="3.5" 
      fill="url(#starGlow)" 
      filter="url(#constellationGlow)"
      className="animate-ping"
      style={{ animationDelay: '1s' }}
    />
    <circle 
      cx="45" cy="50" r="3" 
      fill="url(#starGlow)" 
      filter="url(#constellationGlow)"
      className="animate-ping"
      style={{ animationDelay: '1.5s' }}
    />
    <circle 
      cx="75" cy="70" r="2.5" 
      fill="url(#starGlow)" 
      filter="url(#constellationGlow)"
      className="animate-ping"
      style={{ animationDelay: '2s' }}
    />
    
    {/* 보조 별들 (중간 밝기) */}
    <circle cx="35" cy="20" r="2" fill="#64FFDA" className="animate-ping" style={{ animationDelay: '0.3s' }} />
    <circle cx="65" cy="25" r="2.5" fill="#18FFFF" className="animate-ping" style={{ animationDelay: '0.8s' }} />
    <circle cx="30" cy="60" r="2" fill="#00BCD4" className="animate-ping" style={{ animationDelay: '1.3s' }} />
    <circle cx="60" cy="55" r="2" fill="#64FFDA" className="animate-ping" style={{ animationDelay: '1.8s' }} />
    <circle cx="25" cy="80" r="1.5" fill="#18FFFF" className="animate-ping" style={{ animationDelay: '2.3s' }} />
    
    {/* 배경 별들 (어두운 별) */}
    <circle cx="15" cy="40" r="1" fill="#B0BEC5" className="animate-pulse" style={{ animationDelay: '0.2s' }} />
    <circle cx="85" cy="20" r="0.8" fill="#B0BEC5" className="animate-pulse" style={{ animationDelay: '0.7s' }} />
    <circle cx="70" cy="45" r="1" fill="#B0BEC5" className="animate-pulse" style={{ animationDelay: '1.2s' }} />
    <circle cx="90" cy="60" r="0.6" fill="#B0BEC5" className="animate-pulse" style={{ animationDelay: '1.7s' }} />
    <circle cx="10" cy="70" r="0.8" fill="#B0BEC5" className="animate-pulse" style={{ animationDelay: '2.2s' }} />
    
    {/* 별빛 확산 효과 */}
    <g className="animate-pulse" style={{ animationDuration: '6s' }} opacity="0.3">
      <circle cx="20" cy="25" r="8" fill="none" stroke="#64FFDA" strokeWidth="0.5" />
      <circle cx="50" cy="30" r="10" fill="none" stroke="#FFEB3B" strokeWidth="0.5" />
      <circle cx="80" cy="35" r="9" fill="none" stroke="#18FFFF" strokeWidth="0.5" />
      <circle cx="45" cy="50" r="8" fill="none" stroke="#00BCD4" strokeWidth="0.5" />
      <circle cx="75" cy="70" r="7" fill="none" stroke="#64FFDA" strokeWidth="0.5" />
    </g>
    
    {/* 별자리 이름 */}
    <text x="50" y="90" textAnchor="middle" fill="#64FFDA" fontSize="8" opacity="0.7" className="animate-pulse">
      ★ CONSTELLATION ★
    </text>
  </svg>
);
}