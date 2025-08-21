import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

const StellarLightYear = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      {/* 빛의 스펙트럼 그라디언트 */}
      <linearGradient id="lightSpectrum" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="15%" stopColor="#E1F5FE" />
        <stop offset="30%" stopColor="#81D4FA" />
        <stop offset="45%" stopColor="#2196F3" />
        <stop offset="60%" stopColor="#1976D2" />
        <stop offset="75%" stopColor="#0D47A1" />
        <stop offset="100%" stopColor="#001970" />
      </linearGradient>
      
      {/* 광원 그라디언트 */}
      <radialGradient id="lightSource" cx="50%" cy="50%" r="60%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="30%" stopColor="#E3F2FD" />
        <stop offset="60%" stopColor="#BBDEFB" />
        <stop offset="100%" stopColor="#2196F3" />
      </radialGradient>
      
      {/* 광속 파동 그라디언트 */}
      <radialGradient id="lightWave" cx="50%" cy="50%" r="80%">
        <stop offset="0%" stopColor="#FFFFFF" opacity="0.9" />
        <stop offset="40%" stopColor="#E1F5FE" opacity="0.7" />
        <stop offset="70%" stopColor="#81D4FA" opacity="0.5" />
        <stop offset="100%" stopColor="#2196F3" opacity="0.3" />
      </radialGradient>
      
      {/* 강력한 빛 글로우 */}
      <filter id="lightGlow">
        <feGaussianBlur stdDeviation="6" result="coloredBlur"/>
        <feGaussianBlur stdDeviation="12" result="bigBlur"/>
        <feMerge> 
          <feMergeNode in="bigBlur"/>
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
      
      {/* 광속 효과 */}
      <filter id="speedOfLight">
        <feGaussianBlur stdDeviation="3" result="blur"/>
        <feOffset in="blur" dx="2" dy="0" result="offset"/>
      </filter>
      
      {/* 프리즘 효과 */}
      <filter id="prismEffect">
        <feTurbulence baseFrequency="0.1" numOctaves="2" result="noise"/>
        <feDisplacementMap in="SourceGraphic" in2="noise" scale="1"/>
      </filter>
    </defs>
    
    {/* 우주 배경 - 별들 */}
    <g className="animate-pulse" style={{ animationDuration: '8s' }}>
      <circle cx="10" cy="15" r="0.5" fill="#FFFFFF" opacity="0.8" />
      <circle cx="90" cy="20" r="0.4" fill="#E1F5FE" opacity="0.7" />
      <circle cx="15" cy="85" r="0.6" fill="#BBDEFB" opacity="0.9" />
      <circle cx="85" cy="80" r="0.3" fill="#81D4FA" opacity="0.6" />
      <circle cx="25" cy="25" r="0.4" fill="#2196F3" opacity="0.8" />
      <circle cx="75" cy="75" r="0.5" fill="#1976D2" opacity="0.7" />
    </g>
    
    {/* 빛의 궤적 - 더 화려하게 */}
    <g className="animate-pulse" style={{ animationDuration: '2s' }}>
      <path 
        d="M5 50 Q25 20 50 50 Q75 80 95 50" 
        fill="none" 
        stroke="url(#lightSpectrum)" 
        strokeWidth="6"
        filter="url(#lightGlow)"
      />
      <path 
        d="M5 45 Q25 15 50 45 Q75 75 95 45" 
        fill="none" 
        stroke="#E3F2FD" 
        strokeWidth="4"
        opacity="0.8"
      />
      <path 
        d="M5 55 Q25 25 50 55 Q75 85 95 55" 
        fill="none" 
        stroke="#BBDEFB" 
        strokeWidth="4"
        opacity="0.6"
      />
      <path 
        d="M5 40 Q25 10 50 40 Q75 70 95 40" 
        fill="none" 
        stroke="#81D4FA" 
        strokeWidth="3"
        opacity="0.7"
      />
      <path 
        d="M5 60 Q25 30 50 60 Q75 90 95 60" 
        fill="none" 
        stroke="#2196F3" 
        strokeWidth="3"
        opacity="0.5"
      />
    </g>
    
    {/* 광속 입자들 - 광자 */}
    <g className="animate-spin" style={{ animationDuration: '6s', transformOrigin: '50px 50px' }}>
      <circle cx="15" cy="50" r="1.5" fill="url(#lightSource)" className="animate-ping" filter="url(#speedOfLight)" />
      <circle cx="30" cy="35" r="1.2" fill="#FFFFFF" className="animate-ping" style={{ animationDelay: '0.5s' }} />
      <circle cx="50" cy="50" r="2" fill="url(#lightSource)" className="animate-ping" style={{ animationDelay: '1s' }} filter="url(#lightGlow)" />
      <circle cx="70" cy="65" r="1.3" fill="#E3F2FD" className="animate-ping" style={{ animationDelay: '1.5s' }} />
      <circle cx="85" cy="50" r="1.8" fill="url(#lightSource)" className="animate-ping" style={{ animationDelay: '2s' }} />
    </g>
    
    {/* 반대 방향 광자들 */}
    <g className="animate-spin" style={{ animationDuration: '8s', transformOrigin: '50px 50px', animationDirection: 'reverse' }}>
      <circle cx="25" cy="40" r="1" fill="#81D4FA" className="animate-ping" />
      <circle cx="45" cy="60" r="0.8" fill="#2196F3" className="animate-ping" style={{ animationDelay: '1s' }} />
      <circle cx="65" cy="40" r="1.1" fill="#1976D2" className="animate-ping" style={{ animationDelay: '2s' }} />
      <circle cx="75" cy="55" r="0.9" fill="#0D47A1" className="animate-ping" style={{ animationDelay: '3s' }} />
    </g>
    
    {/* 빛의 파동 - 전자기파 */}
    <g className="animate-ping" style={{ animationDuration: '3s' }}>
      <circle 
        cx="50" 
        cy="50" 
        r="20" 
        fill="none" 
        stroke="url(#lightWave)" 
        strokeWidth="2" 
        opacity="0.7"
      />
      <circle 
        cx="50" 
        cy="50" 
        r="30" 
        fill="none" 
        stroke="#E1F5FE" 
        strokeWidth="1.5" 
        opacity="0.6"
      />
      <circle 
        cx="50" 
        cy="50" 
        r="40" 
        fill="none" 
        stroke="#BBDEFB" 
        strokeWidth="1" 
        opacity="0.5"
      />
      <circle 
        cx="50" 
        cy="50" 
        r="47" 
        fill="none" 
        stroke="#81D4FA" 
        strokeWidth="0.5" 
        opacity="0.4"
      />
    </g>
    
    {/* 추가 파동 */}
    <g className="animate-ping" style={{ animationDuration: '4s', animationDelay: '1s' }}>
      <circle cx="50" cy="50" r="25" fill="none" stroke="#2196F3" strokeWidth="1.2" opacity="0.5" />
      <circle cx="50" cy="50" r="35" fill="none" stroke="#1976D2" strokeWidth="0.8" opacity="0.4" />
      <circle cx="50" cy="50" r="43" fill="none" stroke="#0D47A1" strokeWidth="0.4" opacity="0.3" />
    </g>
    
    {/* 거리 측정선 - 더 정확하게 */}
    <g className="animate-pulse" style={{ animationDuration: '5s' }} opacity="0.8">
      <line x1="10" y1="15" x2="90" y2="15" stroke="#2196F3" strokeWidth="1.5" strokeDasharray="3,2" />
      <line x1="10" y1="85" x2="90" y2="85" stroke="#2196F3" strokeWidth="1.5" strokeDasharray="3,2" />
      <line x1="10" y1="15" x2="10" y2="20" stroke="#2196F3" strokeWidth="1.5" />
      <line x1="90" y1="15" x2="90" y2="20" stroke="#2196F3" strokeWidth="1.5" />
      <line x1="10" y1="85" x2="10" y2="80" stroke="#2196F3" strokeWidth="1.5" />
      <line x1="90" y1="85" x2="90" y2="80" stroke="#2196F3" strokeWidth="1.5" />
    </g>
    
    {/* 프리즘 분광 효과 */}
    <g className="animate-pulse" style={{ animationDuration: '3s' }} opacity="0.6">
      <rect x="20" y="25" width="2" height="8" fill="#FF0000" filter="url(#prismEffect)" />
      <rect x="23" y="25" width="2" height="8" fill="#FF8000" filter="url(#prismEffect)" />
      <rect x="26" y="25" width="2" height="8" fill="#FFFF00" filter="url(#prismEffect)" />
      <rect x="29" y="25" width="2" height="8" fill="#00FF00" filter="url(#prismEffect)" />
      <rect x="32" y="25" width="2" height="8" fill="#0080FF" filter="url(#prismEffect)" />
      <rect x="35" y="25" width="2" height="8" fill="#4000FF" filter="url(#prismEffect)" />
      <rect x="38" y="25" width="2" height="8" fill="#8000FF" filter="url(#prismEffect)" />
    </g>
    
    {/* 광속 상수 */}
    <text x="50" y="10" textAnchor="middle" fill="#FFFFFF" fontSize="4" opacity="0.9" fontWeight="bold">
      c = 299,792,458 m/s
    </text>
    
    {/* 1 광년 표시 */}
    <text x="50" y="22" textAnchor="middle" fill="#E1F5FE" fontSize="5" opacity="0.9" fontWeight="bold">
      1 Light Year
    </text>
    
    {/* 정확한 거리 */}
    <text x="50" y="92" textAnchor="middle" fill="#2196F3" fontSize="3.5" opacity="0.8">
      9.461 × 10¹⁵ meters
    </text>
    
    {/* 킬로미터 단위 */}
    <text x="50" y="98" textAnchor="middle" fill="#81D4FA" fontSize="3" opacity="0.7">
      9.461 trillion km
    </text>
    
    {/* 시간 표시 */}
    <text x="8" y="50" fill="#BBDEFB" fontSize="2.8" opacity="0.8">
      t = 1 year
    </text>
    
    {/* 천문단위 비교 */}
    <text x="92" y="50" fill="#64B5F6" fontSize="2.5" opacity="0.7" textAnchor="end">
      ≈ 63,241 AU
    </text>
    
    {/* 파섹 변환 */}
    <text x="8" y="95" fill="#42A5F5" fontSize="2.5" opacity="0.6">
      ≈ 0.307 pc
    </text>
  </svg>
);

export default StellarLightYear;