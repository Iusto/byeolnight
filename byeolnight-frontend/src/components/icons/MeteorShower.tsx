import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function MeteorShower({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <linearGradient id="meteorTrail1" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="30%" stopColor="#FFEB3B" opacity="0.8" />
        <stop offset="70%" stopColor="#FF9800" opacity="0.9" />
        <stop offset="100%" stopColor="#FF5722" />
      </linearGradient>
      <linearGradient id="meteorTrail2" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="40%" stopColor="#00E5FF" opacity="0.7" />
        <stop offset="80%" stopColor="#2196F3" opacity="0.8" />
        <stop offset="100%" stopColor="#1565C0" />
      </linearGradient>
      <linearGradient id="meteorTrail3" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="35%" stopColor="#E91E63" opacity="0.6" />
        <stop offset="75%" stopColor="#9C27B0" opacity="0.7" />
        <stop offset="100%" stopColor="#673AB7" />
      </linearGradient>
      <radialGradient id="meteorHead" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="50%" stopColor="#FFEB3B" />
        <stop offset="100%" stopColor="#FF9800" />
      </radialGradient>
      <filter id="meteorGlow">
        <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 주요 유성 */}
    <g className="animate-meteor-trail">
      <line 
        x1="10" y1="90" x2="40" y2="60" 
        stroke="url(#meteorTrail1)" 
        strokeWidth="4" 
        filter="url(#meteorGlow)"
      />
      <circle 
        cx="40" 
        cy="60" 
        r="3" 
        fill="url(#meteorHead)"
        filter="url(#meteorGlow)"
      />
    </g>
    
    {/* 두 번째 유성 */}
    <g className="animate-meteor-trail" style={{ animationDelay: '1s' }}>
      <line 
        x1="20" y1="80" x2="50" y2="50" 
        stroke="url(#meteorTrail2)" 
        strokeWidth="3" 
        filter="url(#meteorGlow)"
      />
      <circle 
        cx="50" 
        cy="50" 
        r="2.5" 
        fill="url(#meteorHead)"
        filter="url(#meteorGlow)"
      />
    </g>
    
    {/* 세 번째 유성 */}
    <g className="animate-meteor-trail" style={{ animationDelay: '2s' }}>
      <line 
        x1="30" y1="70" x2="60" y2="40" 
        stroke="url(#meteorTrail3)" 
        strokeWidth="2.5" 
        filter="url(#meteorGlow)"
      />
      <circle 
        cx="60" 
        cy="40" 
        r="2" 
        fill="url(#meteorHead)"
        filter="url(#meteorGlow)"
      />
    </g>
    
    {/* 작은 유성들 */}
    <g className="animate-meteor-trail" style={{ animationDelay: '0.5s' }}>
      <line 
        x1="5" y1="95" x2="25" y2="75" 
        stroke="#FFEB3B" 
        strokeWidth="2" 
        opacity="0.8"
      />
      <circle cx="25" cy="75" r="1.5" fill="#FFFFFF" />
    </g>
    
    <g className="animate-meteor-trail" style={{ animationDelay: '1.5s' }}>
      <line 
        x1="15" y1="85" x2="35" y2="65" 
        stroke="#00E5FF" 
        strokeWidth="1.5" 
        opacity="0.7"
      />
      <circle cx="35" cy="65" r="1" fill="#81D4FA" />
    </g>
    
    <g className="animate-meteor-trail" style={{ animationDelay: '2.5s' }}>
      <line 
        x1="25" y1="75" x2="45" y2="55" 
        stroke="#E91E63" 
        strokeWidth="1.8" 
        opacity="0.6"
      />
      <circle cx="45" cy="55" r="1.2" fill="#F8BBD9" />
    </g>
    
    {/* 복사점 (Radiant) */}
    <g className="animate-pulse" style={{ animationDuration: '4s' }}>
      <circle cx="75" cy="25" r="8" fill="none" stroke="#FFFFFF" strokeWidth="0.5" opacity="0.4" />
      <circle cx="75" cy="25" r="6" fill="none" stroke="#FFFFFF" strokeWidth="0.3" opacity="0.6" />
      <circle cx="75" cy="25" r="4" fill="none" stroke="#FFFFFF" strokeWidth="0.2" opacity="0.8" />
      <circle cx="75" cy="25" r="1" fill="#FFFFFF" opacity="0.9" />
    </g>
    
    {/* 대기 진입 효과 */}
    <ellipse 
      cx="35" 
      cy="65" 
      rx="3" 
      ry="1" 
      fill="#FF9800" 
      opacity="0.5"
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    <ellipse 
      cx="50" 
      cy="50" 
      rx="2.5" 
      ry="0.8" 
      fill="#2196F3" 
      opacity="0.6"
      className="animate-pulse"
      style={{ animationDuration: '2.5s', animationDelay: '1s' }}
    />
    
    {/* 별들 (배경) */}
    <circle cx="80" cy="15" r="0.5" fill="#FFFFFF" className="animate-twinkle" />
    <circle cx="90" cy="30" r="0.4" fill="#FFFFFF" className="animate-twinkle" style={{ animationDelay: '1s' }} />
    <circle cx="85" cy="45" r="0.3" fill="#FFFFFF" className="animate-twinkle" style={{ animationDelay: '2s' }} />
    <circle cx="95" cy="60" r="0.4" fill="#FFFFFF" className="animate-twinkle" style={{ animationDelay: '0.5s' }} />
    <circle cx="70" cy="10" r="0.3" fill="#FFFFFF" className="animate-twinkle" style={{ animationDelay: '1.5s' }} />
    
    {/* 유성 파편들 */}
    <circle cx="30" cy="70" r="0.3" fill="#FF9800" className="animate-ping" />
    <circle cx="45" cy="55" r="0.2" fill="#FFEB3B" className="animate-ping" style={{ animationDelay: '0.8s' }} />
    <circle cx="55" cy="45" r="0.4" fill="#00E5FF" className="animate-ping" style={{ animationDelay: '1.2s' }} />
    <circle cx="38" cy="62" r="0.2" fill="#E91E63" className="animate-ping" style={{ animationDelay: '1.8s' }} />
  </svg>
);
}