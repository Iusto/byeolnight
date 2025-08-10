import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

const StellarAndromeda = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <linearGradient id="andromedaCore" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#FFF8E1" />
        <stop offset="30%" stopColor="#FFECB3" />
        <stop offset="70%" stopColor="#FFB74D" />
        <stop offset="100%" stopColor="#E65100" />
      </linearGradient>
      <linearGradient id="andromedaArm1" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stopColor="#E1F5FE" />
        <stop offset="50%" stopColor="#4FC3F7" />
        <stop offset="100%" stopColor="#0277BD" />
      </linearGradient>
      <linearGradient id="andromedaArm2" x1="100%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stopColor="#F3E5F5" />
        <stop offset="50%" stopColor="#CE93D8" />
        <stop offset="100%" stopColor="#7B1FA2" />
      </linearGradient>
      <filter id="andromedaGlow">
        <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 안드로메다 은하 나선팔들 */}
    <g className="animate-spin" style={{ animationDuration: '40s', transformOrigin: '50px 50px' }}>
      <ellipse 
        cx="50" cy="50" rx="40" ry="15" 
        fill="none" 
        stroke="url(#andromedaArm1)" 
        strokeWidth="6"
        filter="url(#andromedaGlow)"
        transform="rotate(30 50 50)"
        opacity="0.8"
      />
      <ellipse 
        cx="50" cy="50" rx="35" ry="12" 
        fill="none" 
        stroke="url(#andromedaArm2)" 
        strokeWidth="5"
        transform="rotate(30 50 50)"
        opacity="0.7"
      />
      <ellipse 
        cx="50" cy="50" rx="30" ry="10" 
        fill="none" 
        stroke="url(#andromedaArm1)" 
        strokeWidth="4"
        transform="rotate(30 50 50)"
        opacity="0.6"
      />
    </g>
    
    {/* 반대 방향 나선팔 */}
    <g className="animate-spin" style={{ animationDuration: '35s', animationDirection: 'reverse', transformOrigin: '50px 50px' }}>
      <ellipse 
        cx="50" cy="50" rx="38" ry="13" 
        fill="none" 
        stroke="url(#andromedaArm2)" 
        strokeWidth="4"
        transform="rotate(-45 50 50)"
        opacity="0.5"
      />
      <ellipse 
        cx="50" cy="50" rx="32" ry="9" 
        fill="none" 
        stroke="url(#andromedaArm1)" 
        strokeWidth="3"
        transform="rotate(-45 50 50)"
        opacity="0.4"
      />
    </g>
    
    {/* 은하 중심 (초대질량 블랙홀) */}
    <ellipse 
      cx="50" 
      cy="50" 
      rx="8" 
      ry="3" 
      fill="url(#andromedaCore)" 
      filter="url(#andromedaGlow)"
      className="animate-pulse"
      style={{ animationDuration: '3s' }}
      transform="rotate(30 50 50)"
    />
    
    {/* 별 형성 지역들 */}
    <g className="animate-pulse" style={{ animationDuration: '5s' }}>
      <ellipse cx="30" cy="40" rx="3" ry="1" fill="#4FC3F7" transform="rotate(30 30 40)" opacity="0.8" />
      <ellipse cx="70" cy="35" rx="2.5" ry="0.8" fill="#CE93D8" transform="rotate(30 70 35)" opacity="0.7" />
      <ellipse cx="65" cy="65" rx="3.5" ry="1.2" fill="#4FC3F7" transform="rotate(30 65 65)" opacity="0.9" />
      <ellipse cx="35" cy="70" rx="2" ry="0.6" fill="#CE93D8" transform="rotate(30 35 70)" opacity="0.6" />
    </g>
    
    {/* 개별 별들 */}
    <g className="animate-pulse" style={{ animationDuration: '4s' }}>
      <circle cx="25" cy="35" r="0.8" fill="#E1F5FE" />
      <circle cx="75" cy="30" r="1" fill="#F3E5F5" />
      <circle cx="80" cy="60" r="0.6" fill="#4FC3F7" />
      <circle cx="20" cy="65" r="0.9" fill="#CE93D8" />
      <circle cx="60" cy="25" r="0.7" fill="#E1F5FE" />
      <circle cx="40" cy="75" r="1.1" fill="#F3E5F5" />
    </g>
    
    {/* 은하 헤일로 */}
    <ellipse 
      cx="50" 
      cy="50" 
      rx="45" 
      ry="20" 
      fill="none" 
      stroke="#E1F5FE" 
      strokeWidth="0.5" 
      opacity="0.2"
      className="animate-pulse"
      style={{ animationDuration: '8s' }}
      transform="rotate(30 50 50)"
    />
    
    {/* 거리 표시 */}
    <text x="50" y="10" textAnchor="middle" fill="#4FC3F7" fontSize="6" opacity="0.7">
      M31 - 2.5M ly
    </text>
    
    {/* 충돌 예정 표시 */}
    <g className="animate-pulse" style={{ animationDuration: '6s' }} opacity="0.5">
      <path d="M85 15 L90 10 M85 15 L80 10 M85 15 L85 5" stroke="#FF5722" strokeWidth="1" />
      <text x="85" y="25" textAnchor="middle" fill="#FF5722" fontSize="4">4.5B years</text>
    </g>
  </svg>
);

export default StellarAndromeda;