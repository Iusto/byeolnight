import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

const CosmicVortex = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="vortexCore" cx="50%" cy="50%" r="30%">
        <stop offset="0%" stopColor="#000000" />
        <stop offset="100%" stopColor="#1A237E" />
      </radialGradient>
      <linearGradient id="vortexSpiral1" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#3F51B5" />
        <stop offset="50%" stopColor="#9C27B0" />
        <stop offset="100%" stopColor="#E91E63" />
      </linearGradient>
      <linearGradient id="vortexSpiral2" x1="100%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stopColor="#FF5722" />
        <stop offset="50%" stopColor="#FF9800" />
        <stop offset="100%" stopColor="#FFC107" />
      </linearGradient>
      <filter id="vortexGlow">
        <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 소용돌이 나선들 */}
    <g className="animate-spin" style={{ animationDuration: '8s', transformOrigin: '50px 50px' }}>
      <path 
        d="M50 15 Q70 25 75 50 Q70 75 50 85 Q30 75 25 50 Q30 25 50 15" 
        fill="none" 
        stroke="url(#vortexSpiral1)" 
        strokeWidth="4"
        filter="url(#vortexGlow)"
      />
      <path 
        d="M50 20 Q65 30 70 50 Q65 70 50 80 Q35 70 30 50 Q35 30 50 20" 
        fill="none" 
        stroke="url(#vortexSpiral2)" 
        strokeWidth="3"
        opacity="0.8"
      />
    </g>
    
    {/* 반대 방향 소용돌이 */}
    <g className="animate-spin" style={{ animationDuration: '6s', animationDirection: 'reverse', transformOrigin: '50px 50px' }}>
      <path 
        d="M15 35 Q35 15 65 35 Q85 65 65 85 Q35 85 15 65 Q15 35 15 35" 
        fill="none" 
        stroke="url(#vortexSpiral1)" 
        strokeWidth="2"
        opacity="0.6"
      />
      <path 
        d="M85 65 Q65 85 35 65 Q15 35 35 15 Q65 15 85 35 Q85 65 85 65" 
        fill="none" 
        stroke="url(#vortexSpiral2)" 
        strokeWidth="2"
        opacity="0.5"
      />
    </g>
    
    {/* 중심 소용돌이 코어 */}
    <circle 
      cx="50" 
      cy="50" 
      r="10" 
      fill="url(#vortexCore)" 
      filter="url(#vortexGlow)"
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    
    {/* 에너지 입자들 */}
    <g className="animate-spin" style={{ animationDuration: '12s', transformOrigin: '50px 50px' }}>
      <circle cx="35" cy="20" r="1" fill="#3F51B5" className="animate-ping" />
      <circle cx="80" cy="35" r="0.8" fill="#9C27B0" className="animate-ping" style={{ animationDelay: '1s' }} />
      <circle cx="80" cy="65" r="1.2" fill="#E91E63" className="animate-ping" style={{ animationDelay: '2s' }} />
      <circle cx="65" cy="80" r="0.9" fill="#FF5722" className="animate-ping" style={{ animationDelay: '3s' }} />
      <circle cx="35" cy="80" r="1.1" fill="#FF9800" className="animate-ping" style={{ animationDelay: '4s' }} />
      <circle cx="20" cy="65" r="0.7" fill="#FFC107" className="animate-ping" style={{ animationDelay: '5s' }} />
      <circle cx="20" cy="35" r="1" fill="#3F51B5" className="animate-ping" style={{ animationDelay: '6s' }} />
    </g>
    
    {/* 시공간 왜곡 효과 */}
    <circle 
      cx="50" 
      cy="50" 
      r="25" 
      fill="none" 
      stroke="#3F51B5" 
      strokeWidth="0.5" 
      opacity="0.4"
      className="animate-ping"
      style={{ animationDuration: '5s' }}
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
      style={{ animationDuration: '6s', animationDelay: '1s' }}
    />
  </svg>
);

export default CosmicVortex;