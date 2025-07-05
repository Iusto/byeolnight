import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

const StellarMilkyWay = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <linearGradient id="galaxyArm1" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#E1F5FE" />
        <stop offset="30%" stopColor="#B3E5FC" />
        <stop offset="60%" stopColor="#4FC3F7" />
        <stop offset="100%" stopColor="#0277BD" />
      </linearGradient>
      <linearGradient id="galaxyArm2" x1="100%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stopColor="#F3E5F5" />
        <stop offset="30%" stopColor="#CE93D8" />
        <stop offset="60%" stopColor="#AB47BC" />
        <stop offset="100%" stopColor="#7B1FA2" />
      </linearGradient>
      <radialGradient id="galaxyCore" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#FFF8E1" />
        <stop offset="30%" stopColor="#FFECB3" />
        <stop offset="70%" stopColor="#FFB74D" />
        <stop offset="100%" stopColor="#E65100" />
      </radialGradient>
      <filter id="galaxyGlow">
        <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 은하 나선팔들 */}
    <g className="animate-spin" style={{ animationDuration: '30s', transformOrigin: '50px 50px' }}>
      <path 
        d="M50 20 Q70 30 80 50 Q70 70 50 80 Q30 70 20 50 Q30 30 50 20" 
        fill="none" 
        stroke="url(#galaxyArm1)" 
        strokeWidth="8"
        filter="url(#galaxyGlow)"
        opacity="0.7"
      />
      <path 
        d="M50 25 Q65 35 75 50 Q65 65 50 75 Q35 65 25 50 Q35 35 50 25" 
        fill="none" 
        stroke="url(#galaxyArm2)" 
        strokeWidth="6"
        opacity="0.6"
      />
    </g>
    
    {/* 반대 방향 나선팔 */}
    <g className="animate-spin" style={{ animationDuration: '25s', animationDirection: 'reverse', transformOrigin: '50px 50px' }}>
      <path 
        d="M20 30 Q40 20 60 30 Q80 40 70 60 Q60 80 40 70 Q20 60 30 40 Q20 30 20 30" 
        fill="none" 
        stroke="url(#galaxyArm1)" 
        strokeWidth="5"
        opacity="0.5"
      />
      <path 
        d="M80 70 Q60 80 40 70 Q20 60 30 40 Q40 20 60 30 Q80 40 70 60 Q80 70 80 70" 
        fill="none" 
        stroke="url(#galaxyArm2)" 
        strokeWidth="4"
        opacity="0.4"
      />
    </g>
    
    {/* 은하 중심 (초대질량 블랙홀) */}
    <circle 
      cx="50" 
      cy="50" 
      r="8" 
      fill="url(#galaxyCore)" 
      filter="url(#galaxyGlow)"
      className="animate-pulse"
      style={{ animationDuration: '3s' }}
    />
    
    {/* 별들 */}
    <g className="animate-pulse" style={{ animationDuration: '4s' }}>
      <circle cx="35" cy="25" r="1" fill="#E1F5FE" />
      <circle cx="65" cy="30" r="0.8" fill="#B3E5FC" />
      <circle cx="75" cy="45" r="1.2" fill="#4FC3F7" />
      <circle cx="70" cy="65" r="0.9" fill="#CE93D8" />
      <circle cx="45" cy="75" r="1.1" fill="#AB47BC" />
      <circle cx="25" cy="70" r="0.7" fill="#7B1FA2" />
      <circle cx="20" cy="45" r="1" fill="#0277BD" />
      <circle cx="30" cy="35" r="0.6" fill="#FFF8E1" />
    </g>
    
    {/* 성간 먼지 구름 */}
    <g className="animate-pulse" style={{ animationDuration: '6s' }} opacity="0.3">
      <ellipse cx="40" cy="35" rx="8" ry="3" fill="#B3E5FC" transform="rotate(45 40 35)" />
      <ellipse cx="60" cy="65" rx="6" ry="2" fill="#CE93D8" transform="rotate(-30 60 65)" />
      <ellipse cx="25" cy="55" rx="5" ry="2" fill="#4FC3F7" transform="rotate(60 25 55)" />
    </g>
    
    {/* 은하 헤일로 */}
    <circle 
      cx="50" 
      cy="50" 
      r="40" 
      fill="none" 
      stroke="#E1F5FE" 
      strokeWidth="0.5" 
      opacity="0.2"
      className="animate-ping"
      style={{ animationDuration: '8s' }}
    />
    
    {/* 태양계 위치 표시 */}
    <g className="animate-ping">
      <circle cx="65" cy="40" r="1.5" fill="#FFEB3B" />
      <text x="68" y="38" fill="#FFEB3B" fontSize="4">☉</text>
    </g>
  </svg>
);

export default StellarMilkyWay;