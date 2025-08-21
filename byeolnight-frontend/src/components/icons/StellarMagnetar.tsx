import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

const StellarMagnetar = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="magnetarCore" cx="50%" cy="50%" r="40%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="30%" stopColor="#E3F2FD" />
        <stop offset="70%" stopColor="#1976D2" />
        <stop offset="100%" stopColor="#0D47A1" />
      </radialGradient>
      <linearGradient id="magneticField1" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stopColor="#FF1744" />
        <stop offset="50%" stopColor="#FF5722" />
        <stop offset="100%" stopColor="#FF9800" />
      </linearGradient>
      <linearGradient id="magneticField2" x1="0%" y1="100%" x2="100%" y2="0%">
        <stop offset="0%" stopColor="#E91E63" />
        <stop offset="50%" stopColor="#9C27B0" />
        <stop offset="100%" stopColor="#673AB7" />
      </linearGradient>
      <filter id="magnetarGlow">
        <feGaussianBlur stdDeviation="4" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 자기장 라인들 */}
    <g className="animate-pulse" style={{ animationDuration: '3s' }}>
      <path 
        d="M20 30 Q50 10 80 30 Q50 50 20 30" 
        fill="none" 
        stroke="url(#magneticField1)" 
        strokeWidth="3"
        filter="url(#magnetarGlow)"
      />
      <path 
        d="M20 70 Q50 90 80 70 Q50 50 20 70" 
        fill="none" 
        stroke="url(#magneticField1)" 
        strokeWidth="3"
        filter="url(#magnetarGlow)"
      />
      <path 
        d="M30 20 Q10 50 30 80 Q50 50 30 20" 
        fill="none" 
        stroke="url(#magneticField2)" 
        strokeWidth="2.5"
        opacity="0.8"
      />
      <path 
        d="M70 20 Q90 50 70 80 Q50 50 70 20" 
        fill="none" 
        stroke="url(#magneticField2)" 
        strokeWidth="2.5"
        opacity="0.8"
      />
    </g>
    
    {/* 중성자별 본체 */}
    <circle 
      cx="50" 
      cy="50" 
      r="15" 
      fill="url(#magnetarCore)" 
      filter="url(#magnetarGlow)"
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    
    {/* 자기장 극점들 */}
    <g className="animate-spin" style={{ animationDuration: '10s', transformOrigin: '50px 50px' }}>
      <circle cx="50" cy="30" r="2" fill="#FF1744" className="animate-ping" />
      <circle cx="50" cy="70" r="2" fill="#FF1744" className="animate-ping" style={{ animationDelay: '1s' }} />
      <circle cx="30" cy="50" r="1.5" fill="#E91E63" className="animate-ping" style={{ animationDelay: '2s' }} />
      <circle cx="70" cy="50" r="1.5" fill="#E91E63" className="animate-ping" style={{ animationDelay: '3s' }} />
    </g>
    
    {/* X선 방출 */}
    <g className="animate-pulse" style={{ animationDuration: '1.5s' }}>
      <line x1="50" y1="20" x2="50" y2="10" stroke="#FFFFFF" strokeWidth="2" opacity="0.8" />
      <line x1="50" y1="80" x2="50" y2="90" stroke="#FFFFFF" strokeWidth="2" opacity="0.8" />
      <line x1="20" y1="50" x2="10" y2="50" stroke="#FFFFFF" strokeWidth="2" opacity="0.8" />
      <line x1="80" y1="50" x2="90" y2="50" stroke="#FFFFFF" strokeWidth="2" opacity="0.8" />
    </g>
    
    {/* 자기장 강도 표시 */}
    <circle 
      cx="50" 
      cy="50" 
      r="25" 
      fill="none" 
      stroke="#FF5722" 
      strokeWidth="0.5" 
      opacity="0.4"
      className="animate-ping"
      style={{ animationDuration: '4s' }}
    />
    <circle 
      cx="50" 
      cy="50" 
      r="35" 
      fill="none" 
      stroke="#9C27B0" 
      strokeWidth="0.3" 
      opacity="0.3"
      className="animate-ping"
      style={{ animationDuration: '5s', animationDelay: '1s' }}
    />
    
    {/* 자기장 강도 텍스트 */}
    <text x="50" y="95" textAnchor="middle" fill="#FF5722" fontSize="6" opacity="0.7">
      10¹⁵ Gauss
    </text>
  </svg>
);

export default StellarMagnetar;