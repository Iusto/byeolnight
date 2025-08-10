import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function BigBang({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        {/* 빅뱅 폭발 중심 - 더 화려하게 */}
        <radialGradient id="bigBangCore" cx="50%" cy="50%" r="40%">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="15%" stopColor="#FFD700" />
          <stop offset="30%" stopColor="#FF6B35" />
          <stop offset="45%" stopColor="#FF1744" />
          <stop offset="60%" stopColor="#E91E63" />
          <stop offset="75%" stopColor="#9C27B0" />
          <stop offset="90%" stopColor="#673AB7" />
          <stop offset="100%" stopColor="#3F51B5" />
        </radialGradient>
        
        {/* 폭발 파동 1 - 더 강렬하게 */}
        <radialGradient id="explosion1" cx="50%" cy="50%" r="70%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="20%" stopColor="#FF1744" opacity="0.9" />
          <stop offset="40%" stopColor="#E91E63" opacity="0.8" />
          <stop offset="60%" stopColor="#9C27B0" opacity="0.7" />
          <stop offset="80%" stopColor="#673AB7" opacity="0.6" />
          <stop offset="100%" stopColor="#3F51B5" opacity="0.4" />
        </radialGradient>
        
        {/* 폭발 파동 2 */}
        <radialGradient id="explosion2" cx="50%" cy="50%" r="85%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="30%" stopColor="#9C27B0" opacity="0.8" />
          <stop offset="50%" stopColor="#673AB7" opacity="0.7" />
          <stop offset="70%" stopColor="#3F51B5" opacity="0.6" />
          <stop offset="90%" stopColor="#2196F3" opacity="0.4" />
          <stop offset="100%" stopColor="#00BCD4" opacity="0.2" />
        </radialGradient>
        
        {/* 폭발 파동 3 */}
        <radialGradient id="explosion3" cx="50%" cy="50%" r="98%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="40%" stopColor="#3F51B5" opacity="0.7" />
          <stop offset="60%" stopColor="#2196F3" opacity="0.6" />
          <stop offset="80%" stopColor="#00BCD4" opacity="0.4" />
          <stop offset="95%" stopColor="#4CAF50" opacity="0.3" />
          <stop offset="100%" stopColor="#8BC34A" opacity="0.1" />
        </radialGradient>
        
        {/* 초강력 빅뱅 글로우 - 5단계 */}
        <filter id="bigBangGlow">
          <feGaussianBlur stdDeviation="6" result="blur1"/>
          <feGaussianBlur stdDeviation="12" result="blur2"/>
          <feGaussianBlur stdDeviation="18" result="blur3"/>
          <feGaussianBlur stdDeviation="24" result="blur4"/>
          <feGaussianBlur stdDeviation="30" result="blur5"/>
          <feMerge> 
            <feMergeNode in="blur5"/>
            <feMergeNode in="blur4"/>
            <feMergeNode in="blur3"/>
            <feMergeNode in="blur2"/>
            <feMergeNode in="blur1"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
        
        {/* 폭발 효과 - 더 강렬하게 */}
        <filter id="explosionEffect">
          <feTurbulence baseFrequency="0.5" numOctaves="6" result="explosion"/>
          <feDisplacementMap in="SourceGraphic" in2="explosion" scale="8"/>
        </filter>
        
        {/* 에너지 왜곡 */}
        <filter id="energyWarp">
          <feTurbulence baseFrequency="0.2" numOctaves="4" result="warp"/>
          <feDisplacementMap in="SourceGraphic" in2="warp" scale="6"/>
        </filter>
        
        {/* 빛의 산란 */}
        <filter id="lightScatter">
          <feGaussianBlur stdDeviation="4" result="scatter"/>
          <feOffset in="scatter" dx="2" dy="2" result="offset"/>
        </filter>
      </defs>
      
      {/* 우주 배경 - 무의 공허 */}
      <rect x="0" y="0" width="100" height="100" fill="#000000" />
      
      {/* 최외곽 폭발 파동 - 더 크게 */}
      <circle 
        cx="50" 
        cy="50" 
        r="49" 
        fill="url(#explosion3)" 
        className="animate-ping"
        style={{ animationDuration: '5s' }}
        filter="url(#energyWarp)"
      />
      
      {/* 중간 폭발 파동 */}
      <circle 
        cx="50" 
        cy="50" 
        r="44" 
        fill="url(#explosion2)" 
        className="animate-ping"
        style={{ animationDuration: '4s', animationDelay: '0.3s' }}
        filter="url(#energyWarp)"
      />
      
      {/* 내부 폭발 파동 */}
      <circle 
        cx="50" 
        cy="50" 
        r="38" 
        fill="url(#explosion1)" 
        className="animate-ping"
        style={{ animationDuration: '3s', animationDelay: '0.6s' }}
        filter="url(#explosionEffect)"
      />
      
      {/* 빅뱅 핵심 - 더 크고 화려하게 */}
      <circle 
        cx="50" 
        cy="50" 
        r="30" 
        fill="url(#bigBangCore)" 
        filter="url(#bigBangGlow)"
        className="animate-pulse"
        style={{ animationDuration: '0.8s' }}
      />
      
      {/* 내부 핵심 */}
      <circle 
        cx="50" 
        cy="50" 
        r="20" 
        fill="url(#bigBangCore)" 
        filter="url(#bigBangGlow)"
        className="animate-pulse"
        style={{ animationDuration: '0.5s' }}
      />
      
      {/* 방사형 폭발 광선들 - 더 많고 화려하게 */}
      <g className="animate-pulse" style={{ animationDuration: '1.2s' }}>
        {/* 주요 8방향 - 더 굵게 */}
        <path d="M50 15 L50 2" stroke="#FFFFFF" strokeWidth="12" filter="url(#bigBangGlow)" />
        <path d="M50 85 L50 98" stroke="#FFFFFF" strokeWidth="12" filter="url(#bigBangGlow)" />
        <path d="M15 50 L2 50" stroke="#FFFFFF" strokeWidth="12" filter="url(#bigBangGlow)" />
        <path d="M85 50 L98 50" stroke="#FFFFFF" strokeWidth="12" filter="url(#bigBangGlow)" />
        
        <path d="M26 26 L10 10" stroke="#FFD700" strokeWidth="10" filter="url(#bigBangGlow)" />
        <path d="M74 74 L90 90" stroke="#FFD700" strokeWidth="10" filter="url(#bigBangGlow)" />
        <path d="M74 26 L90 10" stroke="#FFD700" strokeWidth="10" filter="url(#bigBangGlow)" />
        <path d="M26 74 L10 90" stroke="#FFD700" strokeWidth="10" filter="url(#bigBangGlow)" />
        
        {/* 추가 16방향 광선 */}
        <path d="M50 25 L50 8" stroke="#FF6B35" strokeWidth="8" filter="url(#bigBangGlow)" />
        <path d="M50 75 L50 92" stroke="#FF6B35" strokeWidth="8" filter="url(#bigBangGlow)" />
        <path d="M25 50 L8 50" stroke="#FF6B35" strokeWidth="8" filter="url(#bigBangGlow)" />
        <path d="M75 50 L92 50" stroke="#FF6B35" strokeWidth="8" filter="url(#bigBangGlow)" />
        
        <path d="M35 20 L25 5" stroke="#FF1744" strokeWidth="7" filter="url(#bigBangGlow)" />
        <path d="M65 20 L75 5" stroke="#FF1744" strokeWidth="7" filter="url(#bigBangGlow)" />
        <path d="M35 80 L25 95" stroke="#FF1744" strokeWidth="7" filter="url(#bigBangGlow)" />
        <path d="M65 80 L75 95" stroke="#FF1744" strokeWidth="7" filter="url(#bigBangGlow)" />
        <path d="M20 35 L5 25" stroke="#FF1744" strokeWidth="7" filter="url(#bigBangGlow)" />
        <path d="M20 65 L5 75" stroke="#FF1744" strokeWidth="7" filter="url(#bigBangGlow)" />
        <path d="M80 35 L95 25" stroke="#FF1744" strokeWidth="7" filter="url(#bigBangGlow)" />
        <path d="M80 65 L95 75" stroke="#FF1744" strokeWidth="7" filter="url(#bigBangGlow)" />
      </g>
      
      {/* 회전하는 에너지 나선 - 더 복잡하게 */}
      <g className="animate-spin" style={{ animationDuration: '2.5s', transformOrigin: '50px 50px' }}>
        <path d="M50 50 Q75 25, 90 50 Q75 75, 50 50" fill="none" stroke="#FF1744" strokeWidth="6" opacity="0.9" filter="url(#explosionEffect)" />
        <path d="M50 50 Q25 75, 10 50 Q25 25, 50 50" fill="none" stroke="#E91E63" strokeWidth="6" opacity="0.9" filter="url(#explosionEffect)" />
        <path d="M50 50 Q75 75, 50 90 Q25 75, 50 50" fill="none" stroke="#9C27B0" strokeWidth="5" opacity="0.8" />
        <path d="M50 50 Q25 25, 50 10 Q75 25, 50 50" fill="none" stroke="#673AB7" strokeWidth="5" opacity="0.8" />
      </g>
      
      {/* 반대 방향 나선 - 더 많이 */}
      <g className="animate-spin" style={{ animationDuration: '3.5s', transformOrigin: '50px 50px', animationDirection: 'reverse' }}>
        <path d="M50 50 Q25 25, 10 50 Q25 75, 50 50" fill="none" stroke="#3F51B5" strokeWidth="4" opacity="0.8" />
        <path d="M50 50 Q75 75, 90 50 Q75 25, 50 50" fill="none" stroke="#2196F3" strokeWidth="4" opacity="0.8" />
        <path d="M50 50 Q25 50, 10 25 Q50 25, 50 50" fill="none" stroke="#00BCD4" strokeWidth="3" opacity="0.7" />
        <path d="M50 50 Q75 50, 90 75 Q50 75, 50 50" fill="none" stroke="#4CAF50" strokeWidth="3" opacity="0.7" />
      </g>
      
      {/* 입자 생성 - 더 많고 화려하게 */}
      <g className="animate-spin" style={{ animationDuration: '1.8s', transformOrigin: '50px 50px' }}>
        <circle cx="30" cy="30" r="2.5" fill="#FFFFFF" className="animate-ping" filter="url(#explosionEffect)" />
        <circle cx="70" cy="30" r="2.2" fill="#FFD700" className="animate-ping" style={{ animationDelay: '0.2s' }} />
        <circle cx="70" cy="70" r="2.8" fill="#FF6B35" className="animate-ping" style={{ animationDelay: '0.4s' }} />
        <circle cx="30" cy="70" r="2.4" fill="#FF1744" className="animate-ping" style={{ animationDelay: '0.6s' }} />
        <circle cx="20" cy="50" r="2.1" fill="#E91E63" className="animate-ping" style={{ animationDelay: '0.8s' }} />
        <circle cx="80" cy="50" r="2.6" fill="#9C27B0" className="animate-ping" style={{ animationDelay: '1s' }} />
        <circle cx="50" cy="20" r="2.3" fill="#673AB7" className="animate-ping" style={{ animationDelay: '1.2s' }} />
        <circle cx="50" cy="80" r="2" fill="#3F51B5" className="animate-ping" style={{ animationDelay: '1.4s' }} />
      </g>
      
      {/* 반대 방향 입자 */}
      <g className="animate-spin" style={{ animationDuration: '2.2s', transformOrigin: '50px 50px', animationDirection: 'reverse' }}>
        <circle cx="35" cy="35" r="1.8" fill="#2196F3" className="animate-ping" filter="url(#lightScatter)" />
        <circle cx="65" cy="35" r="1.6" fill="#00BCD4" className="animate-ping" style={{ animationDelay: '0.3s' }} />
        <circle cx="65" cy="65" r="2" fill="#4CAF50" className="animate-ping" style={{ animationDelay: '0.6s' }} />
        <circle cx="35" cy="65" r="1.7" fill="#8BC34A" className="animate-ping" style={{ animationDelay: '0.9s' }} />
        <circle cx="25" cy="40" r="1.5" fill="#CDDC39" className="animate-ping" style={{ animationDelay: '1.2s' }} />
        <circle cx="75" cy="60" r="1.9" fill="#FFEB3B" className="animate-ping" style={{ animationDelay: '1.5s' }} />
      </g>
      
      {/* 시공간 파동 - 더 많은 레이어 */}
      <g className="animate-ping" style={{ animationDuration: '1.8s' }}>
        <circle cx="50" cy="50" r="12" fill="none" stroke="#FFFFFF" strokeWidth="4" opacity="0.9" />
        <circle cx="50" cy="50" r="16" fill="none" stroke="#FFD700" strokeWidth="3.5" opacity="0.8" />
        <circle cx="50" cy="50" r="20" fill="none" stroke="#FF6B35" strokeWidth="3" opacity="0.7" />
        <circle cx="50" cy="50" r="24" fill="none" stroke="#FF1744" strokeWidth="2.5" opacity="0.6" />
        <circle cx="50" cy="50" r="28" fill="none" stroke="#E91E63" strokeWidth="2" opacity="0.5" />
        <circle cx="50" cy="50" r="32" fill="none" stroke="#9C27B0" strokeWidth="1.8" opacity="0.4" />
        <circle cx="50" cy="50" r="36" fill="none" stroke="#673AB7" strokeWidth="1.5" opacity="0.3" />
        <circle cx="50" cy="50" r="40" fill="none" stroke="#3F51B5" strokeWidth="1.2" opacity="0.2" />
        <circle cx="50" cy="50" r="44" fill="none" stroke="#2196F3" strokeWidth="1" opacity="0.1" />
      </g>
      
      {/* 추가 시공간 파동 */}
      <g className="animate-ping" style={{ animationDuration: '2.5s', animationDelay: '0.5s' }}>
        <circle cx="50" cy="50" r="14" fill="none" stroke="#00BCD4" strokeWidth="2.5" opacity="0.7" />
        <circle cx="50" cy="50" r="18" fill="none" stroke="#4CAF50" strokeWidth="2" opacity="0.6" />
        <circle cx="50" cy="50" r="22" fill="none" stroke="#8BC34A" strokeWidth="1.8" opacity="0.5" />
        <circle cx="50" cy="50" r="26" fill="none" stroke="#CDDC39" strokeWidth="1.5" opacity="0.4" />
        <circle cx="50" cy="50" r="30" fill="none" stroke="#FFEB3B" strokeWidth="1.2" opacity="0.3" />
        <circle cx="50" cy="50" r="34" fill="none" stroke="#FFC107" strokeWidth="1" opacity="0.2" />
        <circle cx="50" cy="50" r="38" fill="none" stroke="#FF9800" strokeWidth="0.8" opacity="0.1" />
      </g>
      
      {/* 우주 창조의 순간 - 중심 특이점 */}
      <circle 
        cx="50" 
        cy="50" 
        r="5" 
        fill="#FFFFFF" 
        filter="url(#bigBangGlow)"
        className="animate-pulse"
        style={{ animationDuration: '0.3s' }}
      />
      
      {/* 최중심 빛 */}
      <circle 
        cx="50" 
        cy="50" 
        r="2" 
        fill="#FFFFFF" 
        filter="url(#bigBangGlow)"
        className="animate-pulse"
        style={{ animationDuration: '0.1s' }}
      />
      
      {/* 화려한 텍스트 */}
      <text x="50" y="8" textAnchor="middle" fill="#FFFFFF" fontSize="6" opacity="0.9" fontWeight="bold" filter="url(#lightScatter)">
        BIG BANG
      </text>
      
      <text x="50" y="16" textAnchor="middle" fill="#FFD700" fontSize="4" opacity="0.8" filter="url(#lightScatter)">
        우주 창조의 순간
      </text>
      
      <text x="50" y="94" textAnchor="middle" fill="#FF6B35" fontSize="4.5" opacity="0.9" fontWeight="bold">
        138억 년 전
      </text>
      
      <text x="8" y="50" fill="#FF1744" fontSize="3.5" opacity="0.9" fontWeight="bold">
        T → ∞
      </text>
      
      <text x="92" y="50" fill="#E91E63" fontSize="3.5" opacity="0.9" fontWeight="bold" textAnchor="end">
        ρ → ∞
      </text>
    </svg>
  );
}