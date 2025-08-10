import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function StringTheory({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        <linearGradient id="string1" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#FF9A9E" />
          <stop offset="50%" stopColor="#FECFEF" />
          <stop offset="100%" stopColor="#FECFEF" />
        </linearGradient>
        <linearGradient id="string2" x1="0%" y1="100%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#A8EDEA" />
          <stop offset="100%" stopColor="#FED6E3" />
        </linearGradient>
        <linearGradient id="string3" x1="0%" y1="50%" x2="100%" y2="50%">
          <stop offset="0%" stopColor="#D299C2" />
          <stop offset="50%" stopColor="#FEF9D7" />
          <stop offset="100%" stopColor="#DAE2F8" />
        </linearGradient>
        <radialGradient id="dimension" cx="50%" cy="50%" r="80%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="70%" stopColor="#E8F5E8" opacity="0.3" />
          <stop offset="100%" stopColor="#C8E6C9" opacity="0.5" />
        </radialGradient>
        <filter id="stringGlow">
          <feGaussianBlur stdDeviation="1.5" result="coloredBlur"/>
          <feMerge> 
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
      </defs>
      
      {/* 차원 배경 */}
      <circle 
        cx="50" 
        cy="50" 
        r="45" 
        fill="url(#dimension)" 
        className="animate-pulse"
        style={{ animationDuration: '6s' }}
      />
      
      {/* 진동하는 스트링들 */}
      <g className="animate-pulse" style={{ animationDuration: '2s' }}>
        <path 
          d="M10 30 Q25 20 40 30 Q55 40 70 30 Q85 20 100 30" 
          fill="none" 
          stroke="url(#string1)" 
          strokeWidth="2"
          filter="url(#stringGlow)"
        />
        <path 
          d="M10 50 Q25 60 40 50 Q55 40 70 50 Q85 60 100 50" 
          fill="none" 
          stroke="url(#string2)" 
          strokeWidth="2"
          filter="url(#stringGlow)"
        />
        <path 
          d="M10 70 Q25 80 40 70 Q55 60 70 70 Q85 80 100 70" 
          fill="none" 
          stroke="url(#string3)" 
          strokeWidth="2"
          filter="url(#stringGlow)"
        />
      </g>
      
      {/* 수직 스트링들 */}
      <g className="animate-pulse" style={{ animationDuration: '2.5s', animationDelay: '1s' }}>
        <path 
          d="M30 10 Q20 25 30 40 Q40 55 30 70 Q20 85 30 100" 
          fill="none" 
          stroke="url(#string1)" 
          strokeWidth="1.5"
          opacity="0.8"
        />
        <path 
          d="M50 10 Q60 25 50 40 Q40 55 50 70 Q60 85 50 100" 
          fill="none" 
          stroke="url(#string2)" 
          strokeWidth="1.5"
          opacity="0.8"
        />
        <path 
          d="M70 10 Q80 25 70 40 Q60 55 70 70 Q80 85 70 100" 
          fill="none" 
          stroke="url(#string3)" 
          strokeWidth="1.5"
          opacity="0.8"
        />
      </g>
      
      {/* 교차점들 (입자들) */}
      <g className="animate-spin" style={{ animationDuration: '10s', transformOrigin: '50px 50px' }}>
        <circle cx="30" cy="30" r="2" fill="#FF9A9E" className="animate-ping" />
        <circle cx="50" cy="30" r="1.5" fill="#A8EDEA" className="animate-ping" style={{ animationDelay: '1s' }} />
        <circle cx="70" cy="30" r="2.2" fill="#D299C2" className="animate-ping" style={{ animationDelay: '2s' }} />
        <circle cx="30" cy="50" r="1.8" fill="#FECFEF" className="animate-ping" style={{ animationDelay: '3s' }} />
        <circle cx="50" cy="50" r="2.5" fill="#FED6E3" className="animate-ping" style={{ animationDelay: '0.5s' }} />
        <circle cx="70" cy="50" r="1.6" fill="#FEF9D7" className="animate-ping" style={{ animationDelay: '1.5s' }} />
        <circle cx="30" cy="70" r="2.1" fill="#DAE2F8" className="animate-ping" style={{ animationDelay: '2.5s' }} />
        <circle cx="50" cy="70" r="1.7" fill="#C8E6C9" className="animate-ping" style={{ animationDelay: '3.5s' }} />
        <circle cx="70" cy="70" r="1.9" fill="#E8F5E8" className="animate-ping" style={{ animationDelay: '4s' }} />
      </g>
      
      {/* 고차원 진동 */}
      <g className="animate-pulse" style={{ animationDuration: '3s' }} opacity="0.6">
        <path 
          d="M20 20 Q35 35 50 20 Q65 35 80 20" 
          fill="none" 
          stroke="#FF9A9E" 
          strokeWidth="0.8"
        />
        <path 
          d="M20 80 Q35 65 50 80 Q65 65 80 80" 
          fill="none" 
          stroke="#A8EDEA" 
          strokeWidth="0.8"
        />
        <path 
          d="M20 50 Q35 35 50 50 Q65 65 80 50" 
          fill="none" 
          stroke="#D299C2" 
          strokeWidth="0.8"
        />
      </g>
      
      {/* 양자 거품들 */}
      <circle cx="15" cy="15" r="0.8" fill="#FF9A9E" opacity="0.7" className="animate-ping" />
      <circle cx="85" cy="15" r="0.6" fill="#A8EDEA" opacity="0.8" className="animate-ping" style={{ animationDelay: '2s' }} />
      <circle cx="85" cy="85" r="1" fill="#D299C2" opacity="0.6" className="animate-ping" style={{ animationDelay: '4s' }} />
      <circle cx="15" cy="85" r="0.7" fill="#FECFEF" opacity="0.9" className="animate-ping" style={{ animationDelay: '1s' }} />
      
      {/* 차원 경계 */}
      <circle 
        cx="50" 
        cy="50" 
        r="35" 
        fill="none" 
        stroke="#E8F5E8" 
        strokeWidth="0.5" 
        opacity="0.4"
        className="animate-ping"
        style={{ animationDuration: '8s' }}
      />
    </svg>
  );
}