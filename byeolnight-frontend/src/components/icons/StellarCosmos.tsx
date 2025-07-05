import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

const StellarCosmos = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="cosmicBg" cx="50%" cy="50%" r="80%">
        <stop offset="0%" stopColor="#000051" />
        <stop offset="30%" stopColor="#1A237E" />
        <stop offset="60%" stopColor="#3F51B5" />
        <stop offset="100%" stopColor="#000000" />
      </radialGradient>
      <linearGradient id="galaxyCluster1" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#E1F5FE" />
        <stop offset="50%" stopColor="#4FC3F7" />
        <stop offset="100%" stopColor="#0277BD" />
      </linearGradient>
      <linearGradient id="galaxyCluster2" x1="100%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stopColor="#F3E5F5" />
        <stop offset="50%" stopColor="#CE93D8" />
        <stop offset="100%" stopColor="#7B1FA2" />
      </linearGradient>
      <filter id="cosmicGlow">
        <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 우주 배경 */}
    <rect 
      x="0" y="0" width="100" height="100" 
      fill="url(#cosmicBg)" 
      className="animate-pulse"
      style={{ animationDuration: '10s' }}
    />
    
    {/* 은하단들 */}
    <g className="animate-pulse" style={{ animationDuration: '8s' }}>
      <ellipse cx="25" cy="25" rx="8" ry="4" fill="url(#galaxyCluster1)" opacity="0.7" transform="rotate(45 25 25)" />
      <ellipse cx="75" cy="25" rx="6" ry="3" fill="url(#galaxyCluster2)" opacity="0.6" transform="rotate(-30 75 25)" />
      <ellipse cx="20" cy="70" rx="7" ry="3.5" fill="url(#galaxyCluster1)" opacity="0.8" transform="rotate(60 20 70)" />
      <ellipse cx="80" cy="75" rx="9" ry="4.5" fill="url(#galaxyCluster2)" opacity="0.5" transform="rotate(-45 80 75)" />
    </g>
    
    {/* 개별 은하들 */}
    <g className="animate-pulse" style={{ animationDuration: '6s' }}>
      <ellipse cx="15" cy="15" rx="2" ry="1" fill="#E1F5FE" transform="rotate(30 15 15)" />
      <ellipse cx="85" cy="15" rx="1.5" ry="0.8" fill="#F3E5F5" transform="rotate(-60 85 15)" />
      <ellipse cx="10" cy="50" rx="1.8" ry="0.9" fill="#4FC3F7" transform="rotate(45 10 50)" />
      <ellipse cx="90" cy="45" rx="2.2" ry="1.1" fill="#CE93D8" transform="rotate(-30 90 45)" />
      <ellipse cx="15" cy="85" rx="1.6" ry="0.7" fill="#0277BD" transform="rotate(60 15 85)" />
      <ellipse cx="85" cy="85" rx="1.9" ry="1" fill="#7B1FA2" transform="rotate(-45 85 85)" />
    </g>
    
    {/* 우주의 거대 구조 (필라멘트) */}
    <g className="animate-pulse" style={{ animationDuration: '12s' }} opacity="0.3">
      <path d="M10 20 Q30 40 50 30 Q70 20 90 40" stroke="#4FC3F7" strokeWidth="1" fill="none" />
      <path d="M20 80 Q40 60 60 70 Q80 80 90 60" stroke="#CE93D8" strokeWidth="1" fill="none" />
      <path d="M20 10 Q50 50 80 20" stroke="#E1F5FE" strokeWidth="0.8" fill="none" />
      <path d="M10 60 Q50 30 90 70" stroke="#F3E5F5" strokeWidth="0.8" fill="none" />
    </g>
    
    {/* 암흑물질 헤일로 */}
    <g className="animate-pulse" style={{ animationDuration: '15s' }} opacity="0.2">
      <circle cx="25" cy="25" r="15" fill="none" stroke="#1A237E" strokeWidth="0.5" />
      <circle cx="75" cy="25" r="12" fill="none" stroke="#1A237E" strokeWidth="0.5" />
      <circle cx="20" cy="70" r="13" fill="none" stroke="#1A237E" strokeWidth="0.5" />
      <circle cx="80" cy="75" r="16" fill="none" stroke="#1A237E" strokeWidth="0.5" />
    </g>
    
    {/* 퀘이사들 (먼 거리의 밝은 천체) */}
    <g className="animate-ping">
      <circle cx="30" cy="35" r="0.8" fill="#FFEB3B" />
      <circle cx="70" cy="40" r="0.6" fill="#FF9800" />
      <circle cx="40" cy="65" r="0.9" fill="#FFEB3B" />
      <circle cx="60" cy="20" r="0.7" fill="#FF9800" />
    </g>
    
    {/* 감마선 폭발 */}
    <g className="animate-pulse" style={{ animationDuration: '2s' }}>
      <circle cx="50" cy="50" r="1.5" fill="#FFFFFF" filter="url(#cosmicGlow)" />
      <path d="M50 45 L50 35 M50 55 L50 65 M45 50 L35 50 M55 50 L65 50" stroke="#FFFFFF" strokeWidth="1" />
    </g>
    
    {/* 우주 마이크로파 배경복사 */}
    <g className="animate-pulse" style={{ animationDuration: '20s' }} opacity="0.1">
      <rect x="0" y="0" width="100" height="100" fill="url(#cosmicBg)" />
    </g>
    
    {/* 허블 상수 표시 */}
    <text x="50" y="10" textAnchor="middle" fill="#4FC3F7" fontSize="5" opacity="0.8">
      H₀ = 70 km/s/Mpc
    </text>
    
    {/* 우주 나이 */}
    <text x="50" y="95" textAnchor="middle" fill="#CE93D8" fontSize="5" opacity="0.8">
      Age: 13.8 Billion Years
    </text>
    
    {/* 관측 가능한 우주 경계 */}
    <circle 
      cx="50" 
      cy="50" 
      r="48" 
      fill="none" 
      stroke="#FFFFFF" 
      strokeWidth="0.3" 
      opacity="0.2"
      className="animate-pulse"
      style={{ animationDuration: '25s' }}
      strokeDasharray="2,2"
    />
  </svg>
);

export default StellarCosmos;