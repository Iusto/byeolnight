import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function BlackHole({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        {/* 사건의 지평선 - 절대적 어둠 */}
        <radialGradient id="eventHorizon" cx="50%" cy="50%" r="60%">
          <stop offset="0%" stopColor="#000000" />
          <stop offset="80%" stopColor="#000000" />
          <stop offset="95%" stopColor="#0A0A0A" />
          <stop offset="100%" stopColor="#1A1A1A" />
        </radialGradient>
        
        {/* 강착원반 - 극고온 */}
        <linearGradient id="accretionHot" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="15%" stopColor="#E3F2FD" />
          <stop offset="30%" stopColor="#FFEB3B" />
          <stop offset="50%" stopColor="#FF9800" />
          <stop offset="70%" stopColor="#FF5722" />
          <stop offset="85%" stopColor="#E91E63" />
          <stop offset="100%" stopColor="#9C27B0" />
        </linearGradient>
        
        {/* 강착원반 - 중온 */}
        <linearGradient id="accretionMedium" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#E91E63" />
          <stop offset="25%" stopColor="#9C27B0" />
          <stop offset="50%" stopColor="#673AB7" />
          <stop offset="75%" stopColor="#3F51B5" />
          <stop offset="100%" stopColor="#2196F3" />
        </linearGradient>
        
        {/* 강착원반 - 저온 */}
        <linearGradient id="accretionCool" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#2196F3" />
          <stop offset="30%" stopColor="#1976D2" />
          <stop offset="60%" stopColor="#1565C0" />
          <stop offset="100%" stopColor="#0D47A1" />
        </linearGradient>
        
        {/* 초강력 블랙홀 글로우 */}
        <filter id="blackHoleGlow">
          <feGaussianBlur stdDeviation="6" result="coloredBlur"/>
          <feGaussianBlur stdDeviation="12" result="bigBlur"/>
          <feGaussianBlur stdDeviation="20" result="massiveBlur"/>
          <feMerge> 
            <feMergeNode in="massiveBlur"/>
            <feMergeNode in="bigBlur"/>
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
        
        {/* 중력 렌즈 왜곡 효과 */}
        <filter id="gravitationalLensing">
          <feTurbulence baseFrequency="0.02" numOctaves="4" result="noise"/>
          <feDisplacementMap in="SourceGraphic" in2="noise" scale="4"/>
          <feGaussianBlur stdDeviation="1" result="blur"/>
        </filter>
        
        {/* 시공간 왜곡 */}
        <filter id="spacetimeWarp">
          <feTurbulence baseFrequency="0.01" numOctaves="3" result="warp"/>
          <feDisplacementMap in="SourceGraphic" in2="warp" scale="6"/>
        </filter>
        
        {/* 사건의 지평선 왜곡 */}
        <filter id="horizonWarp">
          <feTurbulence baseFrequency="0.05" numOctaves="2" result="distort"/>
          <feDisplacementMap in="SourceGraphic" in2="distort" scale="3"/>
        </filter>
      </defs>
      
      {/* 중력 렌즈 효과로 왜곡된 배경 은하들 */}
      <g className="animate-pulse" style={{ animationDuration: '10s' }}>
        <ellipse cx="15" cy="12" rx="5" ry="1.5" fill="#E3F2FD" filter="url(#gravitationalLensing)" opacity="0.9" transform="rotate(30 15 12)" />
        <ellipse cx="85" cy="10" rx="4.5" ry="1.2" fill="#BBDEFB" filter="url(#gravitationalLensing)" opacity="0.8" transform="rotate(-45 85 10)" />
        <ellipse cx="90" cy="85" rx="6" ry="1.8" fill="#90CAF9" filter="url(#gravitationalLensing)" opacity="0.9" transform="rotate(60 90 85)" />
        <ellipse cx="10" cy="88" rx="5.2" ry="1.4" fill="#64B5F6" filter="url(#gravitationalLensing)" opacity="0.7" transform="rotate(-30 10 88)" />
        <ellipse cx="8" cy="45" rx="4" ry="1" fill="#42A5F5" filter="url(#gravitationalLensing)" opacity="0.8" transform="rotate(90 8 45)" />
        <ellipse cx="92" cy="50" rx="4.8" ry="1.3" fill="#2196F3" filter="url(#gravitationalLensing)" opacity="0.8" transform="rotate(-60 92 50)" />
        <ellipse cx="50" cy="8" rx="3.5" ry="0.9" fill="#1976D2" filter="url(#gravitationalLensing)" opacity="0.6" transform="rotate(45 50 8)" />
        <ellipse cx="50" cy="92" rx="4.2" ry="1.1" fill="#1565C0" filter="url(#gravitationalLensing)" opacity="0.7" transform="rotate(-45 50 92)" />
      </g>
      
      {/* 아인슈타인 링 효과 - 더 화려하게 */}
      <g className="animate-pulse" style={{ animationDuration: '8s' }} opacity="0.6">
        <circle cx="50" cy="50" r="35" fill="none" stroke="#E3F2FD" strokeWidth="1.5" strokeDasharray="4,3" filter="url(#spacetimeWarp)" />
        <circle cx="50" cy="50" r="32" fill="none" stroke="#BBDEFB" strokeWidth="1.2" strokeDasharray="3,2" filter="url(#spacetimeWarp)" />
        <circle cx="50" cy="50" r="29" fill="none" stroke="#90CAF9" strokeWidth="1" strokeDasharray="2,1" filter="url(#spacetimeWarp)" />
        <circle cx="50" cy="50" r="26" fill="none" stroke="#64B5F6" strokeWidth="0.8" strokeDasharray="1,1" filter="url(#spacetimeWarp)" />
      </g>
      
      {/* 강착원반 - 최외곽 (저온 영역) */}
      <g className="animate-spin" style={{ animationDuration: '15s', transformOrigin: '50px 50px' }}>
        <ellipse 
          cx="50" cy="50" rx="42" ry="12" 
          fill="none" 
          stroke="url(#accretionCool)" 
          strokeWidth="5"
          filter="url(#blackHoleGlow)"
          opacity="0.8"
        />
        <ellipse 
          cx="50" cy="50" rx="38" ry="10" 
          fill="none" 
          stroke="#1976D2" 
          strokeWidth="4"
          opacity="0.9"
        />
        <ellipse 
          cx="50" cy="50" rx="34" ry="8" 
          fill="none" 
          stroke="#1565C0" 
          strokeWidth="3"
          opacity="0.7"
        />
      </g>
      
      {/* 강착원반 - 중간 (중온 영역) */}
      <g className="animate-spin" style={{ animationDuration: '8s', transformOrigin: '50px 50px' }}>
        <ellipse 
          cx="50" cy="50" rx="28" ry="6" 
          fill="none" 
          stroke="url(#accretionMedium)" 
          strokeWidth="4"
          filter="url(#blackHoleGlow)"
          opacity="0.9"
        />
        <ellipse 
          cx="50" cy="50" rx="25" ry="5" 
          fill="none" 
          stroke="#9C27B0" 
          strokeWidth="3.5"
          opacity="0.8"
        />
        <ellipse 
          cx="50" cy="50" rx="22" ry="4" 
          fill="none" 
          stroke="#673AB7" 
          strokeWidth="3"
          opacity="0.9"
        />
      </g>
      
      {/* 강착원반 - 내부 (극고온 영역) */}
      <g className="animate-spin" style={{ animationDuration: '3s', transformOrigin: '50px 50px' }}>
        <ellipse 
          cx="50" cy="50" rx="18" ry="3" 
          fill="none" 
          stroke="url(#accretionHot)" 
          strokeWidth="3"
          filter="url(#blackHoleGlow)"
          opacity="0.95"
        />
        <ellipse 
          cx="50" cy="50" rx="15" ry="2.5" 
          fill="none" 
          stroke="#FFFFFF" 
          strokeWidth="2.5"
          opacity="0.9"
        />
        <ellipse 
          cx="50" cy="50" rx="12" ry="2" 
          fill="none" 
          stroke="#E3F2FD" 
          strokeWidth="2"
          opacity="0.8"
        />
      </g>
      
      {/* 사건의 지평선 - 왜곡된 중심 구멍 */}
      <circle 
        cx="50" 
        cy="50" 
        r="10" 
        fill="url(#eventHorizon)" 
        filter="url(#horizonWarp)"
        className="animate-pulse"
        style={{ animationDuration: '2s' }}
      />
      
      {/* 내부 절대 어둠 */}
      <circle 
        cx="50" 
        cy="50" 
        r="7" 
        fill="#000000" 
        filter="url(#horizonWarp)"
        className="animate-pulse"
        style={{ animationDuration: '1.5s' }}
      />
      
      {/* 최중심 특이점 */}
      <circle 
        cx="50" 
        cy="50" 
        r="1" 
        fill="#000000" 
        className="animate-pulse"
        style={{ animationDuration: '0.5s' }}
      />
      
      {/* 강착물질 가열 및 X선 방출 - 더 화려하게 */}
      <g className="animate-spin" style={{ animationDuration: '1.5s', transformOrigin: '50px 50px' }}>
        <circle cx="28" cy="50" r="1.5" fill="#FFFFFF" className="animate-ping" filter="url(#blackHoleGlow)" />
        <circle cx="72" cy="50" r="1.3" fill="#FFEB3B" className="animate-ping" style={{ animationDelay: '0.1s' }} filter="url(#blackHoleGlow)" />
        <circle cx="50" cy="38" r="1.4" fill="#FF9800" className="animate-ping" style={{ animationDelay: '0.2s' }} filter="url(#blackHoleGlow)" />
        <circle cx="50" cy="62" r="1.6" fill="#FF5722" className="animate-ping" style={{ animationDelay: '0.3s' }} filter="url(#blackHoleGlow)" />
        <circle cx="40" cy="40" r="1.2" fill="#E91E63" className="animate-ping" style={{ animationDelay: '0.4s' }} filter="url(#blackHoleGlow)" />
        <circle cx="60" cy="60" r="1.4" fill="#9C27B0" className="animate-ping" style={{ animationDelay: '0.5s' }} filter="url(#blackHoleGlow)" />
        <circle cx="60" cy="40" r="1.1" fill="#673AB7" className="animate-ping" style={{ animationDelay: '0.6s' }} filter="url(#blackHoleGlow)" />
        <circle cx="40" cy="60" r="1.3" fill="#3F51B5" className="animate-ping" style={{ animationDelay: '0.7s' }} filter="url(#blackHoleGlow)" />
      </g>
      
      {/* 제트 분출 - 극지방 */}
      <g className="animate-pulse" style={{ animationDuration: '2s' }}>
        <rect x="48" y="2" width="4" height="20" fill="url(#accretionHot)" opacity="0.9" filter="url(#blackHoleGlow)" />
        <rect x="48" y="78" width="4" height="20" fill="url(#accretionHot)" opacity="0.9" filter="url(#blackHoleGlow)" />
        <rect x="47" y="0" width="6" height="12" fill="#FFFFFF" opacity="0.7" />
        <rect x="47" y="88" width="6" height="12" fill="#FFFFFF" opacity="0.7" />
      </g>
      
      {/* 호킹 복사 - 더 많은 입자 */}
      <g className="animate-ping" style={{ animationDuration: '3s' }}>
        <circle cx="35" cy="35" r="0.6" fill="#E3F2FD" />
        <circle cx="65" cy="35" r="0.5" fill="#BBDEFB" />
        <circle cx="65" cy="65" r="0.7" fill="#90CAF9" />
        <circle cx="35" cy="65" r="0.6" fill="#64B5F6" />
        <circle cx="30" cy="50" r="0.4" fill="#42A5F5" />
        <circle cx="70" cy="50" r="0.5" fill="#2196F3" />
        <circle cx="50" cy="30" r="0.6" fill="#1976D2" />
        <circle cx="50" cy="70" r="0.4" fill="#1565C0" />
        <circle cx="25" cy="25" r="0.3" fill="#0D47A1" />
        <circle cx="75" cy="75" r="0.5" fill="#001970" />
      </g>
      
      {/* 시공간 곡률 표시 - 더 복잡하게 */}
      <g className="animate-pulse" style={{ animationDuration: '12s' }} opacity="0.3">
        <circle cx="50" cy="50" r="47" fill="none" stroke="#E3F2FD" strokeWidth="0.8" strokeDasharray="6,4" filter="url(#spacetimeWarp)" />
        <circle cx="50" cy="50" r="43" fill="none" stroke="#BBDEFB" strokeWidth="0.6" strokeDasharray="5,3" filter="url(#spacetimeWarp)" />
        <circle cx="50" cy="50" r="39" fill="none" stroke="#90CAF9" strokeWidth="0.5" strokeDasharray="4,2" filter="url(#spacetimeWarp)" />
        <circle cx="50" cy="50" r="35" fill="none" stroke="#64B5F6" strokeWidth="0.4" strokeDasharray="3,1" filter="url(#spacetimeWarp)" />
      </g>
      
      {/* 중력파 방출 */}
      <g className="animate-ping" style={{ animationDuration: '6s' }} opacity="0.4">
        <circle cx="50" cy="50" r="20" fill="none" stroke="#FFFFFF" strokeWidth="0.3" />
        <circle cx="50" cy="50" r="30" fill="none" stroke="#E3F2FD" strokeWidth="0.2" />
        <circle cx="50" cy="50" r="40" fill="none" stroke="#BBDEFB" strokeWidth="0.1" />
      </g>
      
      {/* 슈바르츠실트 반지름 */}
      <text x="50" y="8" textAnchor="middle" fill="#E3F2FD" fontSize="3.5" opacity="0.9">
        Rs = 2GM/c²
      </text>
      
      {/* 사건의 지평선 표시 */}
      <text x="50" y="15" textAnchor="middle" fill="#BBDEFB" fontSize="3" opacity="0.8">
        Event Horizon
      </text>
      
      {/* 질량 정보 */}
      <text x="50" y="92" textAnchor="middle" fill="#FF9800" fontSize="3.5" opacity="0.8">
        M ≈ 10⁶⁻¹⁰ M☉
      </text>
      
      {/* 온도 정보 */}
      <text x="50" y="98" textAnchor="middle" fill="#FF5722" fontSize="3" opacity="0.7">
        T ~ 10⁷⁻⁸ K
      </text>
      
      {/* 호킹 온도 */}
      <text x="8" y="50" fill="#90CAF9" fontSize="2.5" opacity="0.6">
        TH ~ 10⁻⁷ K
      </text>
      
      {/* 각운동량 */}
      <text x="92" y="50" fill="#64B5F6" fontSize="2.5" opacity="0.6" textAnchor="end">
        J/M²
      </text>
    </svg>
  );
}