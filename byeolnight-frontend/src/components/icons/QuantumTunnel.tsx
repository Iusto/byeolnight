import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function QuantumTunnel({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        <linearGradient id="tunnelGrad" x1="0%" y1="50%" x2="100%" y2="50%">
          <stop offset="0%" stopColor="#00E676" opacity="0.8" />
          <stop offset="50%" stopColor="#00BCD4" opacity="0.6" />
          <stop offset="100%" stopColor="#3F51B5" opacity="0.8" />
        </linearGradient>
        <radialGradient id="quantumField" cx="50%" cy="50%" r="80%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="70%" stopColor="#E1F5FE" opacity="0.3" />
          <stop offset="100%" stopColor="#0277BD" opacity="0.5" />
        </radialGradient>
        <filter id="quantumGlow">
          <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
          <feMerge> 
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
      </defs>
      
      {/* 양자장 배경 */}
      <circle 
        cx="50" 
        cy="50" 
        r="45" 
        fill="url(#quantumField)" 
        className="animate-pulse"
        style={{ animationDuration: '4s' }}
      />
      
      {/* 터널 입구 */}
      <ellipse cx="20" cy="50" rx="8" ry="15" fill="#00E676" opacity="0.7" className="animate-pulse" />
      <ellipse cx="80" cy="50" rx="8" ry="15" fill="#3F51B5" opacity="0.7" className="animate-pulse" style={{ animationDelay: '2s' }} />
      
      {/* 터널 본체 */}
      <g className="animate-pulse" style={{ animationDuration: '3s' }}>
        <ellipse cx="30" cy="50" rx="6" ry="12" fill="none" stroke="url(#tunnelGrad)" strokeWidth="2" />
        <ellipse cx="40" cy="50" rx="5" ry="10" fill="none" stroke="url(#tunnelGrad)" strokeWidth="1.5" />
        <ellipse cx="50" cy="50" rx="4" ry="8" fill="none" stroke="url(#tunnelGrad)" strokeWidth="1" />
        <ellipse cx="60" cy="50" rx="5" ry="10" fill="none" stroke="url(#tunnelGrad)" strokeWidth="1.5" />
        <ellipse cx="70" cy="50" rx="6" ry="12" fill="none" stroke="url(#tunnelGrad)" strokeWidth="2" />
      </g>
      
      {/* 양자 입자들 */}
      <g className="animate-spin" style={{ animationDuration: '8s', transformOrigin: '50px 50px' }}>
        <circle cx="25" cy="45" r="1" fill="#00E676" className="animate-ping" />
        <circle cx="35" cy="55" r="0.8" fill="#00BCD4" className="animate-ping" style={{ animationDelay: '1s' }} />
        <circle cx="45" cy="45" r="1.2" fill="#2196F3" className="animate-ping" style={{ animationDelay: '2s' }} />
        <circle cx="55" cy="55" r="0.9" fill="#3F51B5" className="animate-ping" style={{ animationDelay: '3s' }} />
        <circle cx="65" cy="45" r="1.1" fill="#673AB7" className="animate-ping" style={{ animationDelay: '4s' }} />
        <circle cx="75" cy="55" r="0.7" fill="#9C27B0" className="animate-ping" style={{ animationDelay: '5s' }} />
      </g>
      
      {/* 확률 파동 */}
      <g className="animate-pulse" style={{ animationDuration: '2s' }}>
        <path 
          d="M15 50 Q25 40 35 50 Q45 60 55 50 Q65 40 75 50 Q85 60 95 50" 
          fill="none" 
          stroke="#00BCD4" 
          strokeWidth="1" 
          opacity="0.6"
          filter="url(#quantumGlow)"
        />
        <path 
          d="M15 50 Q25 60 35 50 Q45 40 55 50 Q65 60 75 50 Q85 40 95 50" 
          fill="none" 
          stroke="#00E676" 
          strokeWidth="0.8" 
          opacity="0.5"
        />
      </g>
      
      {/* 양자 얽힘 효과 */}
      <line x1="20" y1="50" x2="80" y2="50" stroke="#E1F5FE" strokeWidth="0.5" opacity="0.4" className="animate-ping" />
      
      {/* 에너지 방출 */}
      <circle 
        cx="50" 
        cy="50" 
        r="25" 
        fill="none" 
        stroke="#00BCD4" 
        strokeWidth="0.3" 
        opacity="0.4"
        className="animate-ping"
        style={{ animationDuration: '5s' }}
      />
    </svg>
  );
}