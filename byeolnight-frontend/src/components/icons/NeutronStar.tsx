import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function NeutronStar({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        {/* 중성자별 핵심 - 극도로 압축된 물질 */}
        <radialGradient id="neutronCore" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="20%" stopColor="#E1F5FE" />
          <stop offset="50%" stopColor="#00E5FF" />
          <stop offset="80%" stopColor="#2196F3" />
          <stop offset="100%" stopColor="#1565C0" />
        </radialGradient>
        
        {/* 자기권 그라디언트 */}
        <radialGradient id="magnetosphere" cx="50%" cy="50%" r="90%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="40%" stopColor="transparent" />
          <stop offset="70%" stopColor="#00E5FF" opacity="0.4" />
          <stop offset="90%" stopColor="#2196F3" opacity="0.6" />
          <stop offset="100%" stopColor="#1565C0" opacity="0.8" />
        </radialGradient>
        
        {/* 펄사 빔 그라디언트 */}
        <linearGradient id="pulsarBeam" x1="50%" y1="0%" x2="50%" y2="100%">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="30%" stopColor="#00E5FF" />
          <stop offset="70%" stopColor="#2196F3" />
          <stop offset="100%" stopColor="transparent" />
        </linearGradient>
        
        {/* 초강력 글로우 */}
        <filter id="neutronGlow">
          <feGaussianBlur stdDeviation="5" result="coloredBlur"/>
          <feGaussianBlur stdDeviation="10" result="bigBlur"/>
          <feMerge> 
            <feMergeNode in="bigBlur"/>
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
        
        {/* 자기장 왜곡 효과 */}
        <filter id="magneticField">
          <feTurbulence baseFrequency="0.05" numOctaves="2" result="noise"/>
          <feDisplacementMap in="SourceGraphic" in2="noise" scale="1"/>
        </filter>
        
        {/* 회전 블러 효과 */}
        <filter id="rotationBlur">
          <feGaussianBlur stdDeviation="2" result="blur"/>
          <feOffset in="blur" dx="0.5" dy="0" result="offset"/>
        </filter>
      </defs>
      
      {/* 자기권 - 거대한 자기장 */}
      <circle 
        cx="50" 
        cy="50" 
        r="48" 
        fill="url(#magnetosphere)" 
        className="animate-pulse" 
        style={{ animationDuration: '3s' }} 
      />
      
      {/* 자기장 라인들 - 복잡한 구조 */}
      <g className="animate-pulse" style={{ animationDuration: '2s' }}>
        <ellipse cx="50" cy="50" rx="35" ry="18" fill="none" stroke="#00E5FF" strokeWidth="1.5" opacity="0.8" />
        <ellipse cx="50" cy="50" rx="40" ry="22" fill="none" stroke="#81D4FA" strokeWidth="1.2" opacity="0.7" />
        <ellipse cx="50" cy="50" rx="30" ry="15" fill="none" stroke="#00E5FF" strokeWidth="1.8" opacity="0.9" />
        <ellipse cx="50" cy="50" rx="25" ry="12" fill="none" stroke="#FFFFFF" strokeWidth="2" opacity="0.6" />
      </g>
      
      {/* 추가 자기장 라인 - 다른 각도 */}
      <g className="animate-pulse" style={{ animationDuration: '2.5s', animationDelay: '0.5s' }}>
        <ellipse cx="50" cy="50" rx="18" ry="35" fill="none" stroke="#2196F3" strokeWidth="1.3" opacity="0.7" />
        <ellipse cx="50" cy="50" rx="22" ry="40" fill="none" stroke="#42A5F5" strokeWidth="1" opacity="0.6" />
        <ellipse cx="50" cy="50" rx="15" ry="30" fill="none" stroke="#1E88E5" strokeWidth="1.5" opacity="0.8" />
      </g>
      
      {/* 중성자별 본체 - 초고속 회전 */}
      <circle 
        cx="50" 
        cy="50" 
        r="15" 
        fill="url(#neutronCore)" 
        filter="url(#neutronGlow)"
        className="animate-pulse"
        style={{ animationDuration: '0.5s' }}
      />
      
      {/* 회전 효과를 위한 추가 레이어 */}
      <circle 
        cx="50" 
        cy="50" 
        r="12" 
        fill="url(#neutronCore)" 
        filter="url(#rotationBlur)"
        className="animate-spin"
        style={{ animationDuration: '0.1s' }}
        opacity="0.8"
      />
      
      {/* 극지방 핫스팟 - 자기극 */}
      <g className="animate-pulse" style={{ animationDuration: '0.3s' }}>
        <ellipse cx="50" cy="35" rx="4" ry="3" fill="#FFFFFF" opacity="0.9" />
        <ellipse cx="50" cy="65" rx="4" ry="3" fill="#FFFFFF" opacity="0.9" />
        <ellipse cx="50" cy="33" rx="2" ry="1.5" fill="#00E5FF" />
        <ellipse cx="50" cy="67" rx="2" ry="1.5" fill="#00E5FF" />
      </g>
      
      {/* 펄사 빔 - 회전하는 등대 */}
      <g className="animate-spin" style={{ animationDuration: '0.2s', transformOrigin: '50px 50px' }}>
        <rect x="48" y="10" width="4" height="30" fill="url(#pulsarBeam)" opacity="0.8" />
        <rect x="48" y="60" width="4" height="30" fill="url(#pulsarBeam)" opacity="0.8" />
      </g>
      
      {/* 추가 펄사 빔 - 90도 회전 */}
      <g className="animate-spin" style={{ animationDuration: '0.2s', transformOrigin: '50px 50px', animationDelay: '0.1s' }}>
        <rect x="10" y="48" width="30" height="4" fill="url(#pulsarBeam)" opacity="0.6" />
        <rect x="60" y="48" width="30" height="4" fill="url(#pulsarBeam)" opacity="0.6" />
      </g>
      
      {/* X선 방출 - 다중 레이어 */}
      <g className="animate-ping" style={{ animationDuration: '1s' }}>
        <circle cx="50" cy="50" r="20" fill="none" stroke="#FFFFFF" strokeWidth="1" opacity="0.6" />
        <circle cx="50" cy="50" r="25" fill="none" stroke="#00E5FF" strokeWidth="0.8" opacity="0.5" />
        <circle cx="50" cy="50" r="30" fill="none" stroke="#2196F3" strokeWidth="0.6" opacity="0.4" />
        <circle cx="50" cy="50" r="35" fill="none" stroke="#1565C0" strokeWidth="0.4" opacity="0.3" />
      </g>
      
      {/* 감마선 방출 */}
      <g className="animate-ping" style={{ animationDuration: '0.8s', animationDelay: '0.3s' }}>
        <circle cx="50" cy="50" r="18" fill="none" stroke="#FFFFFF" strokeWidth="1.5" opacity="0.7" />
        <circle cx="50" cy="50" r="23" fill="none" stroke="#E1F5FE" strokeWidth="1.2" opacity="0.6" />
        <circle cx="50" cy="50" r="28" fill="none" stroke="#B3E5FC" strokeWidth="0.9" opacity="0.5" />
      </g>
      
      {/* 고에너지 입자들 - 초고속 회전 */}
      <g className="animate-spin" style={{ animationDuration: '0.05s', transformOrigin: '50px 50px' }}>
        <circle cx="30" cy="30" r="1" fill="#FFFFFF" className="animate-ping" />
        <circle cx="70" cy="30" r="0.8" fill="#00E5FF" className="animate-ping" />
        <circle cx="70" cy="70" r="1.2" fill="#FFFFFF" className="animate-ping" />
        <circle cx="30" cy="70" r="0.9" fill="#00E5FF" className="animate-ping" />
      </g>
      
      {/* 반대 방향 입자 회전 */}
      <g className="animate-spin" style={{ animationDuration: '0.07s', transformOrigin: '50px 50px', animationDirection: 'reverse' }}>
        <circle cx="20" cy="50" r="0.7" fill="#2196F3" className="animate-ping" />
        <circle cx="80" cy="50" r="0.6" fill="#1E88E5" className="animate-ping" />
        <circle cx="50" cy="20" r="0.8" fill="#1976D2" className="animate-ping" />
        <circle cx="50" cy="80" r="0.5" fill="#1565C0" className="animate-ping" />
      </g>
      
      {/* 중력파 방출 */}
      <g className="animate-pulse" style={{ animationDuration: '4s' }} opacity="0.4">
        <circle cx="50" cy="50" r="42" fill="none" stroke="#FFFFFF" strokeWidth="0.3" />
        <circle cx="50" cy="50" r="45" fill="none" stroke="#E1F5FE" strokeWidth="0.2" />
        <circle cx="50" cy="50" r="47" fill="none" stroke="#B3E5FC" strokeWidth="0.1" />
      </g>
      
      {/* 물질 제트 - 극지방에서 분출 */}
      <g className="animate-pulse" style={{ animationDuration: '1.5s' }}>
        <rect x="49" y="5" width="2" height="15" fill="#00E5FF" opacity="0.8" />
        <rect x="49" y="80" width="2" height="15" fill="#00E5FF" opacity="0.8" />
        <rect x="48.5" y="3" width="3" height="8" fill="#FFFFFF" opacity="0.6" />
        <rect x="48.5" y="89" width="3" height="8" fill="#FFFFFF" opacity="0.6" />
      </g>
      
      {/* 회전 주기 표시 */}
      <text x="50" y="10" textAnchor="middle" fill="#FFFFFF" fontSize="3" opacity="0.8">
        P ~ 1ms
      </text>
      
      {/* 자기장 강도 */}
      <text x="10" y="15" fill="#00E5FF" fontSize="2.5" opacity="0.7">
        B ~ 10¹⁵ G
      </text>
      
      {/* 밀도 표시 */}
      <text x="50" y="95" textAnchor="middle" fill="#2196F3" fontSize="3" opacity="0.7">
        ρ ~ 10¹⁵ g/cm³
      </text>
      
      {/* 중성자별 질량 */}
      <text x="90" y="90" fill="#1565C0" fontSize="2.5" opacity="0.6">
        M ~ 1.4M☉
      </text>
    </svg>
  );
}