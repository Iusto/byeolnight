import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Multiverse({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        <radialGradient id="universe1" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#FF6B6B" />
          <stop offset="100%" stopColor="#FF8E53" />
        </radialGradient>
        <radialGradient id="universe2" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#4ECDC4" />
          <stop offset="100%" stopColor="#44A08D" />
        </radialGradient>
        <radialGradient id="universe3" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#A8E6CF" />
          <stop offset="100%" stopColor="#88D8A3" />
        </radialGradient>
        <radialGradient id="universe4" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#FFD93D" />
          <stop offset="100%" stopColor="#FF6B6B" />
        </radialGradient>
        <radialGradient id="universe5" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#6C5CE7" />
          <stop offset="100%" stopColor="#A29BFE" />
        </radialGradient>
        <filter id="multiverseGlow">
          <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
          <feMerge> 
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
      </defs>
      
      {/* 중앙 우주 */}
      <circle 
        cx="50" 
        cy="50" 
        r="15" 
        fill="url(#universe1)" 
        filter="url(#multiverseGlow)"
        className="animate-pulse"
        style={{ animationDuration: '3s' }}
      />
      
      {/* 주변 우주들 */}
      <g className="animate-spin" style={{ animationDuration: '20s', transformOrigin: '50px 50px' }}>
        <circle 
          cx="30" 
          cy="30" 
          r="12" 
          fill="url(#universe2)" 
          filter="url(#multiverseGlow)"
          className="animate-pulse"
          style={{ animationDuration: '4s', animationDelay: '1s' }}
        />
        <circle 
          cx="70" 
          cy="30" 
          r="10" 
          fill="url(#universe3)" 
          filter="url(#multiverseGlow)"
          className="animate-pulse"
          style={{ animationDuration: '3.5s', animationDelay: '2s' }}
        />
        <circle 
          cx="70" 
          cy="70" 
          r="13" 
          fill="url(#universe4)" 
          filter="url(#multiverseGlow)"
          className="animate-pulse"
          style={{ animationDuration: '4.5s', animationDelay: '0.5s' }}
        />
        <circle 
          cx="30" 
          cy="70" 
          r="11" 
          fill="url(#universe5)" 
          filter="url(#multiverseGlow)"
          className="animate-pulse"
          style={{ animationDuration: '3.8s', animationDelay: '1.5s' }}
        />
      </g>
      
      {/* 차원 연결선들 */}
      <g className="animate-pulse" style={{ animationDuration: '5s' }} opacity="0.4">
        <line x1="50" y1="50" x2="30" y2="30" stroke="#FFFFFF" strokeWidth="1" />
        <line x1="50" y1="50" x2="70" y2="30" stroke="#FFFFFF" strokeWidth="1" />
        <line x1="50" y1="50" x2="70" y2="70" stroke="#FFFFFF" strokeWidth="1" />
        <line x1="50" y1="50" x2="30" y2="70" stroke="#FFFFFF" strokeWidth="1" />
      </g>
      
      {/* 작은 우주 버블들 */}
      <g className="animate-spin" style={{ animationDuration: '15s', animationDirection: 'reverse', transformOrigin: '50px 50px' }}>
        <circle cx="20" cy="50" r="3" fill="#FF6B6B" opacity="0.7" className="animate-ping" />
        <circle cx="80" cy="50" r="2.5" fill="#4ECDC4" opacity="0.6" className="animate-ping" style={{ animationDelay: '2s' }} />
        <circle cx="50" cy="20" r="2" fill="#A8E6CF" opacity="0.8" className="animate-ping" style={{ animationDelay: '4s' }} />
        <circle cx="50" cy="80" r="3.5" fill="#FFD93D" opacity="0.5" className="animate-ping" style={{ animationDelay: '1s' }} />
      </g>
      
      {/* 차원 경계 */}
      <circle 
        cx="50" 
        cy="50" 
        r="40" 
        fill="none" 
        stroke="#FFFFFF" 
        strokeWidth="0.5" 
        opacity="0.3"
        strokeDasharray="5,5"
        className="animate-spin"
        style={{ animationDuration: '30s' }}
      />
      
      {/* 양자 거품들 */}
      <circle cx="15" cy="25" r="1" fill="#FF8E53" opacity="0.6" className="animate-ping" />
      <circle cx="85" cy="25" r="0.8" fill="#44A08D" opacity="0.7" className="animate-ping" style={{ animationDelay: '3s' }} />
      <circle cx="85" cy="75" r="1.2" fill="#88D8A3" opacity="0.5" className="animate-ping" style={{ animationDelay: '1s' }} />
      <circle cx="15" cy="75" r="0.9" fill="#A29BFE" opacity="0.8" className="animate-ping" style={{ animationDelay: '4s' }} />
      
      {/* 중심 에너지 */}
      <circle 
        cx="50" 
        cy="50" 
        r="5" 
        fill="#FFFFFF" 
        opacity="0.8"
        className="animate-pulse"
        style={{ animationDuration: '1s' }}
      />
    </svg>
  );
}