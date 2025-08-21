import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function TimeLoop({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        <linearGradient id="timeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#E1BEE7" />
          <stop offset="50%" stopColor="#9C27B0" />
          <stop offset="100%" stopColor="#4A148C" />
        </linearGradient>
        <radialGradient id="timeCore" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#F3E5F5" />
          <stop offset="50%" stopColor="#CE93D8" />
          <stop offset="100%" stopColor="#7B1FA2" />
        </radialGradient>
        <filter id="timeGlow">
          <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
          <feMerge> 
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
      </defs>
      
      {/* 시간 루프 궤도 */}
      <g className="animate-spin" style={{ animationDuration: '12s', transformOrigin: '50px 50px' }}>
        <path 
          d="M50 15 A35 35 0 1 1 49 15" 
          fill="none" 
          stroke="url(#timeGrad)" 
          strokeWidth="3"
          filter="url(#timeGlow)"
        />
        <path 
          d="M50 25 A25 25 0 1 1 49 25" 
          fill="none" 
          stroke="#CE93D8" 
          strokeWidth="2"
          opacity="0.7"
        />
      </g>
      
      {/* 역방향 루프 */}
      <g className="animate-spin" style={{ animationDuration: '8s', animationDirection: 'reverse', transformOrigin: '50px 50px' }}>
        <path 
          d="M50 20 A30 30 0 1 0 49 20" 
          fill="none" 
          stroke="#E1BEE7" 
          strokeWidth="1.5"
          opacity="0.6"
        />
      </g>
      
      {/* 중심 시간 코어 */}
      <circle 
        cx="50" 
        cy="50" 
        r="12" 
        fill="url(#timeCore)" 
        filter="url(#timeGlow)"
        className="animate-pulse"
        style={{ animationDuration: '2s' }}
      />
      
      {/* 시계 바늘들 */}
      <g className="animate-spin" style={{ animationDuration: '4s', transformOrigin: '50px 50px' }}>
        <line x1="50" y1="50" x2="50" y2="42" stroke="#4A148C" strokeWidth="2" />
        <line x1="50" y1="50" x2="56" y2="50" stroke="#7B1FA2" strokeWidth="1.5" />
      </g>
      
      {/* 시간 입자들 */}
      <g className="animate-spin" style={{ animationDuration: '15s', transformOrigin: '50px 50px' }}>
        <circle cx="35" cy="25" r="1" fill="#9C27B0" className="animate-pulse" />
        <circle cx="65" cy="25" r="0.8" fill="#CE93D8" className="animate-pulse" style={{ animationDelay: '1s' }} />
        <circle cx="75" cy="50" r="1.2" fill="#7B1FA2" className="animate-pulse" style={{ animationDelay: '2s' }} />
        <circle cx="65" cy="75" r="0.9" fill="#9C27B0" className="animate-pulse" style={{ animationDelay: '3s' }} />
        <circle cx="35" cy="75" r="1.1" fill="#CE93D8" className="animate-pulse" style={{ animationDelay: '4s' }} />
        <circle cx="25" cy="50" r="0.7" fill="#E1BEE7" className="animate-pulse" style={{ animationDelay: '5s' }} />
      </g>
      
      {/* 시간 왜곡 효과 */}
      <circle 
        cx="50" 
        cy="50" 
        r="40" 
        fill="none" 
        stroke="#E1BEE7" 
        strokeWidth="0.5" 
        opacity="0.3"
        className="animate-ping"
        style={{ animationDuration: '6s' }}
      />
      
      {/* 무한 기호 */}
      <path 
        d="M40 50 Q45 45 50 50 Q55 55 60 50 Q55 45 50 50 Q45 55 40 50" 
        fill="none" 
        stroke="#F3E5F5" 
        strokeWidth="1" 
        opacity="0.8"
        className="animate-pulse"
      />
    </svg>
  );
}