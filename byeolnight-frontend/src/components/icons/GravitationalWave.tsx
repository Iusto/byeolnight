import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

const GravitationalWave = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <linearGradient id="waveGrad1" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stopColor="#00E5FF" />
        <stop offset="50%" stopColor="#18FFFF" />
        <stop offset="100%" stopColor="#64FFDA" />
      </linearGradient>
      <linearGradient id="waveGrad2" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stopColor="#3F51B5" />
        <stop offset="50%" stopColor="#2196F3" />
        <stop offset="100%" stopColor="#00BCD4" />
      </linearGradient>
      <radialGradient id="sourceGrad" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="50%" stopColor="#E3F2FD" />
        <stop offset="100%" stopColor="#1976D2" />
      </radialGradient>
      <filter id="waveGlow">
        <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 시공간 격자 배경 */}
    <g stroke="#1976D2" strokeWidth="0.3" opacity="0.2">
      <defs>
        <pattern id="grid" width="10" height="10" patternUnits="userSpaceOnUse">
          <path d="M 10 0 L 0 0 0 10" fill="none" stroke="#1976D2" strokeWidth="0.3"/>
        </pattern>
      </defs>
      <rect width="100" height="100" fill="url(#grid)" />
    </g>
    
    {/* 중력파 파동들 */}
    <g className="animate-pulse" style={{ animationDuration: '2s' }}>
      <path 
        d="M5 50 Q20 30 35 50 Q50 70 65 50 Q80 30 95 50" 
        fill="none" 
        stroke="url(#waveGrad1)" 
        strokeWidth="4"
        filter="url(#waveGlow)"
      />
      <path 
        d="M5 45 Q20 25 35 45 Q50 65 65 45 Q80 25 95 45" 
        fill="none" 
        stroke="url(#waveGrad2)" 
        strokeWidth="3"
        opacity="0.8"
      />
      <path 
        d="M5 55 Q20 35 35 55 Q50 75 65 55 Q80 35 95 55" 
        fill="none" 
        stroke="url(#waveGrad2)" 
        strokeWidth="3"
        opacity="0.8"
      />
    </g>
    
    {/* 두 번째 파동 (위상차) */}
    <g className="animate-pulse" style={{ animationDuration: '2s', animationDelay: '1s' }}>
      <path 
        d="M5 40 Q20 60 35 40 Q50 20 65 40 Q80 60 95 40" 
        fill="none" 
        stroke="url(#waveGrad1)" 
        strokeWidth="2.5"
        opacity="0.6"
      />
      <path 
        d="M5 60 Q20 40 35 60 Q50 80 65 60 Q80 40 95 60" 
        fill="none" 
        stroke="url(#waveGrad1)" 
        strokeWidth="2.5"
        opacity="0.6"
      />
    </g>
    
    {/* 파동 소스 (충돌하는 중성자별들) */}
    <g className="animate-spin" style={{ animationDuration: '8s', transformOrigin: '50px 50px' }}>
      <circle cx="45" cy="50" r="3" fill="url(#sourceGrad)" filter="url(#waveGlow)" />
      <circle cx="55" cy="50" r="3" fill="url(#sourceGrad)" filter="url(#waveGlow)" />
      <ellipse cx="50" cy="50" rx="8" ry="2" fill="#00E5FF" opacity="0.3" />
    </g>
    
    {/* LIGO 검출기 표현 */}
    <g className="animate-pulse" style={{ animationDuration: '3s' }} opacity="0.7">
      <rect x="10" y="48" width="15" height="4" fill="#2196F3" />
      <rect x="75" y="48" width="15" height="4" fill="#2196F3" />
      <text x="17" y="45" fill="#2196F3" fontSize="3">LIGO</text>
      <text x="82" y="45" fill="#2196F3" fontSize="3">LIGO</text>
    </g>
    
    {/* 시공간 왜곡 표시 */}
    <g className="animate-ping">
      <circle cx="50" cy="50" r="20" fill="none" stroke="#00E5FF" strokeWidth="0.5" opacity="0.4" />
      <circle cx="50" cy="50" r="30" fill="none" stroke="#18FFFF" strokeWidth="0.3" opacity="0.3" />
      <circle cx="50" cy="50" r="40" fill="none" stroke="#64FFDA" strokeWidth="0.2" opacity="0.2" />
    </g>
    
    {/* 파동 입자들 */}
    <g className="animate-pulse" style={{ animationDuration: '1.5s' }}>
      <circle cx="15" cy="35" r="0.5" fill="#00E5FF" />
      <circle cx="85" cy="65" r="0.5" fill="#18FFFF" />
      <circle cx="25" cy="65" r="0.3" fill="#64FFDA" />
      <circle cx="75" cy="35" r="0.4" fill="#00BCD4" />
    </g>
    
    {/* 주파수 표시 */}
    <text x="50" y="10" textAnchor="middle" fill="#00E5FF" fontSize="6" opacity="0.8">
      f ~ 100-1000 Hz
    </text>
    
    {/* 진폭 표시 */}
    <text x="50" y="95" textAnchor="middle" fill="#18FFFF" fontSize="5" opacity="0.7">
      h ~ 10⁻²¹
    </text>
  </svg>
);

export default GravitationalWave;