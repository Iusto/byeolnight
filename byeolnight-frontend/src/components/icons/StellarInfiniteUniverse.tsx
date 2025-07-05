import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

const StellarInfiniteUniverse = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="universeCore" cx="50%" cy="50%" r="30%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="50%" stopColor="#E1F5FE" />
        <stop offset="100%" stopColor="#0277BD" />
      </radialGradient>
      <linearGradient id="infinityGrad1" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#FF6B6B" />
        <stop offset="25%" stopColor="#4ECDC4" />
        <stop offset="50%" stopColor="#45B7D1" />
        <stop offset="75%" stopColor="#96CEB4" />
        <stop offset="100%" stopColor="#FFEAA7" />
      </linearGradient>
      <linearGradient id="infinityGrad2" x1="100%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stopColor="#DDA0DD" />
        <stop offset="25%" stopColor="#98D8C8" />
        <stop offset="50%" stopColor="#F7DC6F" />
        <stop offset="75%" stopColor="#BB8FCE" />
        <stop offset="100%" stopColor="#85C1E9" />
      </linearGradient>
      <filter id="infiniteGlow">
        <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 무한대 기호 (∞) - 메인 */}
    <g className="animate-spin" style={{ animationDuration: '20s', transformOrigin: '50px 50px' }}>
      <path 
        d="M25 50 Q35 30 50 50 Q65 70 75 50 Q65 30 50 50 Q35 70 25 50" 
        fill="none" 
        stroke="url(#infinityGrad1)" 
        strokeWidth="6"
        filter="url(#infiniteGlow)"
      />
      <path 
        d="M27 50 Q37 32 50 50 Q63 68 73 50 Q63 32 50 50 Q37 68 27 50" 
        fill="none" 
        stroke="url(#infinityGrad2)" 
        strokeWidth="4"
        opacity="0.8"
      />
    </g>
    
    {/* 다중 차원 무한대들 */}
    <g className="animate-spin" style={{ animationDuration: '15s', animationDirection: 'reverse', transformOrigin: '50px 50px' }}>
      <path 
        d="M30 50 Q40 35 50 50 Q60 65 70 50 Q60 35 50 50 Q40 65 30 50" 
        fill="none" 
        stroke="url(#infinityGrad2)" 
        strokeWidth="3"
        opacity="0.6"
      />
      <path 
        d="M35 50 Q42 40 50 50 Q58 60 65 50 Q58 40 50 50 Q42 60 35 50" 
        fill="none" 
        stroke="url(#infinityGrad1)" 
        strokeWidth="2"
        opacity="0.5"
      />
    </g>
    
    {/* 우주 확장 표현 */}
    <g className="animate-pulse" style={{ animationDuration: '8s' }}>
      <circle cx="50" cy="50" r="40" fill="none" stroke="url(#infinityGrad1)" strokeWidth="1" opacity="0.3" />
      <circle cx="50" cy="50" r="35" fill="none" stroke="url(#infinityGrad2)" strokeWidth="0.8" opacity="0.4" />
      <circle cx="50" cy="50" r="30" fill="none" stroke="url(#infinityGrad1)" strokeWidth="0.6" opacity="0.5" />
    </g>
    
    {/* 평행우주들 */}
    <g className="animate-pulse" style={{ animationDuration: '6s' }}>
      <circle cx="20" cy="20" r="3" fill="#FF6B6B" opacity="0.7" />
      <circle cx="80" cy="20" r="2.5" fill="#4ECDC4" opacity="0.6" />
      <circle cx="80" cy="80" r="3.5" fill="#45B7D1" opacity="0.8" />
      <circle cx="20" cy="80" r="2.8" fill="#96CEB4" opacity="0.5" />
      <circle cx="15" cy="50" r="2" fill="#FFEAA7" opacity="0.6" />
      <circle cx="85" cy="50" r="2.2" fill="#DDA0DD" opacity="0.7" />
    </g>
    
    {/* 시공간 곡률 */}
    <g className="animate-spin" style={{ animationDuration: '25s', transformOrigin: '50px 50px' }}>
      <ellipse cx="50" cy="50" rx="45" ry="15" fill="none" stroke="#E1F5FE" strokeWidth="0.5" opacity="0.3" transform="rotate(0 50 50)" />
      <ellipse cx="50" cy="50" rx="45" ry="15" fill="none" stroke="#B3E5FC" strokeWidth="0.4" opacity="0.3" transform="rotate(60 50 50)" />
      <ellipse cx="50" cy="50" rx="45" ry="15" fill="none" stroke="#81D4FA" strokeWidth="0.3" opacity="0.3" transform="rotate(120 50 50)" />
    </g>
    
    {/* 양자 거품들 */}
    <g className="animate-ping">
      <circle cx="35" cy="25" r="0.8" fill="#FF6B6B" />
      <circle cx="65" cy="30" r="0.6" fill="#4ECDC4" />
      <circle cx="70" cy="65" r="1" fill="#45B7D1" />
      <circle cx="30" cy="70" r="0.7" fill="#96CEB4" />
      <circle cx="25" cy="45" r="0.5" fill="#FFEAA7" />
      <circle cx="75" cy="55" r="0.9" fill="#DDA0DD" />
    </g>
    
    {/* 중심 특이점 */}
    <circle 
      cx="50" 
      cy="50" 
      r="4" 
      fill="url(#universeCore)" 
      filter="url(#infiniteGlow)"
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    
    {/* 무한대 기호 텍스트 */}
    <text x="50" y="55" textAnchor="middle" fill="#FFFFFF" fontSize="12" opacity="0.9">
      ∞
    </text>
    
    {/* 우주 상수 */}
    <text x="50" y="10" textAnchor="middle" fill="#4ECDC4" fontSize="5" opacity="0.8">
      Λ = 1.1 × 10⁻⁵² m⁻²
    </text>
    
    {/* 다중우주 이론 */}
    <text x="50" y="95" textAnchor="middle" fill="#FF6B6B" fontSize="5" opacity="0.7">
      Infinite Multiverse
    </text>
    
    {/* 에너지 파동 */}
    <g className="animate-pulse" style={{ animationDuration: '4s' }} opacity="0.4">
      <path d="M10 50 Q30 30 50 50 Q70 70 90 50" stroke="url(#infinityGrad1)" strokeWidth="1" fill="none" />
      <path d="M10 45 Q30 65 50 45 Q70 25 90 45" stroke="url(#infinityGrad2)" strokeWidth="1" fill="none" />
      <path d="M10 55 Q30 35 50 55 Q70 75 90 55" stroke="url(#infinityGrad1)" strokeWidth="1" fill="none" />
    </g>
  </svg>
);

export default StellarInfiniteUniverse;