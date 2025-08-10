import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Quasar({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="quasarCore" cx="50%" cy="50%" r="30%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="40%" stopColor="#FFEB3B" />
        <stop offset="80%" stopColor="#FF9800" />
        <stop offset="100%" stopColor="#FF5722" />
      </radialGradient>
      <radialGradient id="accretionDisk" cx="50%" cy="50%" r="70%">
        <stop offset="30%" stopColor="transparent" />
        <stop offset="40%" stopColor="#FF6B35" opacity="0.8" />
        <stop offset="50%" stopColor="#F7931E" opacity="0.9" />
        <stop offset="60%" stopColor="#FFD23F" opacity="0.7" />
        <stop offset="70%" stopColor="#FF6B35" opacity="0.6" />
        <stop offset="100%" stopColor="transparent" />
      </radialGradient>
      <linearGradient id="relativistic-jet" x1="0%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stopColor="#00E5FF" opacity="0.9" />
        <stop offset="30%" stopColor="#2196F3" opacity="0.8" />
        <stop offset="70%" stopColor="#9C27B0" opacity="0.7" />
        <stop offset="100%" stopColor="#E91E63" opacity="0.6" />
      </linearGradient>
      <filter id="quasarGlow">
        <feGaussianBlur stdDeviation="6" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
      <filter id="intense-glow">
        <feGaussianBlur stdDeviation="8" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 상대론적 제트 (위쪽) */}
    <rect 
      x="47" y="5" width="6" height="35" 
      fill="url(#relativistic-jet)" 
      filter="url(#intense-glow)"
      className="animate-pulse"
      style={{ animationDuration: '1.5s' }}
    />
    <rect 
      x="48" y="5" width="4" height="35" 
      fill="#FFFFFF" 
      opacity="0.8"
      className="animate-pulse"
      style={{ animationDuration: '1s' }}
    />
    
    {/* 상대론적 제트 (아래쪽) */}
    <rect 
      x="47" y="60" width="6" height="35" 
      fill="url(#relativistic-jet)" 
      filter="url(#intense-glow)"
      className="animate-pulse"
      style={{ animationDuration: '1.5s', animationDelay: '0.5s' }}
    />
    <rect 
      x="48" y="60" width="4" height="35" 
      fill="#FFFFFF" 
      opacity="0.8"
      className="animate-pulse"
      style={{ animationDuration: '1s', animationDelay: '0.5s' }}
    />
    
    {/* 강착원반 */}
    <ellipse 
      cx="50" 
      cy="50" 
      rx="35" 
      ry="8" 
      fill="url(#accretionDisk)"
      filter="url(#quasarGlow)"
      className="animate-spin"
      style={{ animationDuration: '4s' }}
    />
    <ellipse 
      cx="50" 
      cy="50" 
      rx="30" 
      ry="6" 
      fill="none"
      stroke="#FFD23F"
      strokeWidth="1"
      opacity="0.8"
      className="animate-spin"
      style={{ animationDuration: '3s', animationDirection: 'reverse' }}
    />
    
    {/* 퀘이사 중심 (초대질량 블랙홀) */}
    <circle 
      cx="50" 
      cy="50" 
      r="12" 
      fill="url(#quasarCore)"
      filter="url(#intense-glow)"
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    
    {/* 브로드 라인 영역 */}
    <circle 
      cx="50" 
      cy="50" 
      r="18" 
      fill="none" 
      stroke="#FFEB3B" 
      strokeWidth="2" 
      opacity="0.6"
      className="animate-pulse"
      style={{ animationDuration: '3s' }}
    />
    
    {/* 고에너지 방사선 */}
    <g className="animate-spin" style={{ animationDuration: '8s', transformOrigin: '50px 50px' }}>
      <line x1="50" y1="20" x2="50" y2="30" stroke="#00E5FF" strokeWidth="2" opacity="0.8" />
      <line x1="50" y1="70" x2="50" y2="80" stroke="#00E5FF" strokeWidth="2" opacity="0.8" />
      <line x1="20" y1="50" x2="30" y2="50" stroke="#9C27B0" strokeWidth="1.5" opacity="0.7" />
      <line x1="70" y1="50" x2="80" y2="50" stroke="#9C27B0" strokeWidth="1.5" opacity="0.7" />
      
      <line x1="29.3" y1="29.3" x2="35.4" y2="35.4" stroke="#E91E63" strokeWidth="1" opacity="0.6" />
      <line x1="70.7" y1="70.7" x2="64.6" y2="64.6" stroke="#E91E63" strokeWidth="1" opacity="0.6" />
      <line x1="70.7" y1="29.3" x2="64.6" y2="35.4" stroke="#E91E63" strokeWidth="1" opacity="0.6" />
      <line x1="29.3" y1="70.7" x2="35.4" y2="64.6" stroke="#E91E63" strokeWidth="1" opacity="0.6" />
    </g>
    
    {/* 적색편이 효과 */}
    <circle 
      cx="50" 
      cy="50" 
      r="40" 
      fill="none" 
      stroke="#FF5722" 
      strokeWidth="0.5" 
      opacity="0.4"
      className="animate-ping"
      style={{ animationDuration: '6s' }}
    />
    <circle 
      cx="50" 
      cy="50" 
      r="45" 
      fill="none" 
      stroke="#FF5722" 
      strokeWidth="0.3" 
      opacity="0.3"
      className="animate-ping"
      style={{ animationDuration: '8s', animationDelay: '2s' }}
    />
    
    {/* 중력 렌즈 효과 */}
    <circle 
      cx="50" 
      cy="50" 
      r="25" 
      fill="none" 
      stroke="#FFFFFF" 
      strokeWidth="0.5" 
      opacity="0.3"
      className="animate-pulse"
      style={{ animationDuration: '5s' }}
    />
    
    {/* 고에너지 입자들 */}
    <circle cx="25" cy="25" r="1" fill="#00E5FF" className="animate-ping" />
    <circle cx="75" cy="25" r="0.8" fill="#FFEB3B" className="animate-ping" style={{ animationDelay: '1s' }} />
    <circle cx="25" cy="75" r="0.9" fill="#9C27B0" className="animate-ping" style={{ animationDelay: '2s' }} />
    <circle cx="75" cy="75" r="0.7" fill="#E91E63" className="animate-ping" style={{ animationDelay: '0.5s' }} />
    
    {/* 우주론적 거리 표시 */}
    <g opacity="0.4" className="animate-pulse" style={{ animationDuration: '10s' }}>
      <circle cx="15" cy="15" r="0.5" fill="#FFFFFF" />
      <circle cx="85" cy="15" r="0.4" fill="#FFFFFF" />
      <circle cx="15" cy="85" r="0.6" fill="#FFFFFF" />
      <circle cx="85" cy="85" r="0.3" fill="#FFFFFF" />
    </g>
  </svg>
);
}