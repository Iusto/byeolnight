import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function RedGiant({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        <radialGradient id="redGiantCore" cx="40%" cy="30%" r="80%">
          <stop offset="0%" stopColor="#FFEB3B" />
          <stop offset="20%" stopColor="#FF9800" />
          <stop offset="50%" stopColor="#FF5722" />
          <stop offset="80%" stopColor="#D32F2F" />
          <stop offset="100%" stopColor="#B71C1C" />
        </radialGradient>
        <radialGradient id="redGiantFlare1" cx="50%" cy="50%" r="60%">
          <stop offset="0%" stopColor="#FFEB3B" opacity="0.8" />
          <stop offset="50%" stopColor="#FF9800" opacity="0.6" />
          <stop offset="100%" stopColor="#FF5722" opacity="0.3" />
        </radialGradient>
        <radialGradient id="redGiantFlare2" cx="50%" cy="50%" r="80%">
          <stop offset="0%" stopColor="#FF5722" opacity="0.6" />
          <stop offset="50%" stopColor="#D32F2F" opacity="0.4" />
          <stop offset="100%" stopColor="#B71C1C" opacity="0.2" />
        </radialGradient>
        <filter id="redGiantGlow">
          <feGaussianBlur stdDeviation="4" result="coloredBlur"/>
          <feMerge> 
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
        <filter id="flameFlicker">
          <feTurbulence baseFrequency="0.9" numOctaves="4" result="noise" seed="1"/>
          <feDisplacementMap in="SourceGraphic" in2="noise" scale="3"/>
        </filter>
      </defs>
      
      {/* 외부 플라즈마 층 */}
      <circle 
        cx="50" 
        cy="50" 
        r="45" 
        fill="url(#redGiantFlare2)" 
        className="animate-pulse"
        style={{ animationDuration: '3s' }}
      />
      
      {/* 중간 대류층 */}
      <circle 
        cx="50" 
        cy="50" 
        r="35" 
        fill="url(#redGiantFlare1)" 
        className="animate-pulse"
        style={{ animationDuration: '2s', animationDelay: '0.5s' }}
      />
      
      {/* 적색거성 본체 */}
      <circle 
        cx="50" 
        cy="50" 
        r="25" 
        fill="url(#redGiantCore)" 
        filter="url(#redGiantGlow)"
        className="animate-pulse"
        style={{ animationDuration: '1.5s' }}
      />
      
      {/* 표면 대류 셀들 (이글거리는 효과) */}
      <g className="animate-pulse" style={{ animationDuration: '1s' }}>
        <circle cx="40" cy="35" r="3" fill="#FFEB3B" opacity="0.8" filter="url(#flameFlicker)" />
        <circle cx="60" cy="40" r="2.5" fill="#FF9800" opacity="0.7" filter="url(#flameFlicker)" />
        <circle cx="45" cy="60" r="3.5" fill="#FF5722" opacity="0.9" filter="url(#flameFlicker)" />
        <circle cx="65" cy="65" r="2" fill="#FFEB3B" opacity="0.6" filter="url(#flameFlicker)" />
        <circle cx="35" cy="55" r="2.8" fill="#FF9800" opacity="0.8" filter="url(#flameFlicker)" />
      </g>
      
      {/* 태양풍과 질량 손실 */}
      <g className="animate-pulse" style={{ animationDuration: '2.5s' }} opacity="0.6">
        <path d="M50 25 Q45 15 40 10" stroke="#FF9800" strokeWidth="2" fill="none" />
        <path d="M50 25 Q55 15 60 10" stroke="#FF5722" strokeWidth="2" fill="none" />
        <path d="M75 50 Q85 45 90 40" stroke="#FF9800" strokeWidth="2" fill="none" />
        <path d="M75 50 Q85 55 90 60" stroke="#FF5722" strokeWidth="2" fill="none" />
        <path d="M50 75 Q45 85 40 90" stroke="#FF9800" strokeWidth="2" fill="none" />
        <path d="M50 75 Q55 85 60 90" stroke="#FF5722" strokeWidth="2" fill="none" />
        <path d="M25 50 Q15 45 10 40" stroke="#FF9800" strokeWidth="2" fill="none" />
        <path d="M25 50 Q15 55 10 60" stroke="#FF5722" strokeWidth="2" fill="none" />
      </g>
      
      {/* 코로나 방출 */}
      <g className="animate-ping">
        <circle cx="30" cy="20" r="1" fill="#FFEB3B" />
        <circle cx="70" cy="25" r="0.8" fill="#FF9800" />
        <circle cx="80" cy="70" r="1.2" fill="#FF5722" />
        <circle cx="20" cy="75" r="0.9" fill="#FFEB3B" />
        <circle cx="25" cy="30" r="0.7" fill="#FF9800" />
        <circle cx="75" cy="80" r="1.1" fill="#FF5722" />
      </g>
      
      {/* 헬륨 플래시 효과 */}
      <circle 
        cx="50" 
        cy="50" 
        r="30" 
        fill="none" 
        stroke="#FFEB3B" 
        strokeWidth="1" 
        opacity="0.4"
        className="animate-ping"
        style={{ animationDuration: '4s' }}
      />
      <circle 
        cx="50" 
        cy="50" 
        r="40" 
        fill="none" 
        stroke="#FF5722" 
        strokeWidth="0.5" 
        opacity="0.3"
        className="animate-ping"
        style={{ animationDuration: '5s', animationDelay: '1s' }}
      />
      
      {/* 별의 분류 표시 */}
      <text x="50" y="95" textAnchor="middle" fill="#FF5722" fontSize="6" opacity="0.8">
        Class M Giant
      </text>
    </svg>
  );
}