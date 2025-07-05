import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function DarkEnergy({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        {/* 다크에너지 필드 - 더 복잡한 그라디언트 */}
        <radialGradient id="darkEnergyField" cx="50%" cy="50%" r="90%">
          <stop offset="0%" stopColor="#000000" />
          <stop offset="15%" stopColor="#1A0033" />
          <stop offset="35%" stopColor="#4A148C" />
          <stop offset="55%" stopColor="#7B1FA2" />
          <stop offset="75%" stopColor="#9C27B0" />
          <stop offset="90%" stopColor="#4A148C" />
          <stop offset="100%" stopColor="#000000" />
        </radialGradient>
        
        {/* 팽창력 그라디언트 1 */}
        <linearGradient id="expansionForce1" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#9C27B0" />
          <stop offset="25%" stopColor="#E91E63" />
          <stop offset="50%" stopColor="#FF5722" />
          <stop offset="75%" stopColor="#FF9800" />
          <stop offset="100%" stopColor="#FFEB3B" />
        </linearGradient>
        
        {/* 팽창력 그라디언트 2 */}
        <linearGradient id="expansionForce2" x1="100%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#673AB7" />
          <stop offset="25%" stopColor="#3F51B5" />
          <stop offset="50%" stopColor="#2196F3" />
          <stop offset="75%" stopColor="#00BCD4" />
          <stop offset="100%" stopColor="#4CAF50" />
        </linearGradient>
        
        {/* 팽창력 그라디언트 3 */}
        <radialGradient id="expansionForce3" cx="50%" cy="50%" r="80%">
          <stop offset="0%" stopColor="#E91E63" />
          <stop offset="30%" stopColor="#9C27B0" />
          <stop offset="60%" stopColor="#673AB7" />
          <stop offset="100%" stopColor="#3F51B5" />
        </radialGradient>
        
        {/* 우주 상수 그라디언트 */}
        <radialGradient id="cosmologicalConstant" cx="50%" cy="50%" r="60%">
          <stop offset="0%" stopColor="#FFFFFF" opacity="0.9" />
          <stop offset="40%" stopColor="#E91E63" opacity="0.7" />
          <stop offset="80%" stopColor="#9C27B0" opacity="0.5" />
          <stop offset="100%" stopColor="#4A148C" opacity="0.3" />
        </radialGradient>
        
        {/* 강력한 다크에너지 글로우 */}
        <filter id="darkEnergyGlow">
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
        
        {/* 에너지 깜빡임 효과 */}
        <filter id="energyFlicker">
          <feTurbulence baseFrequency="0.8" numOctaves="4" result="noise" seed="3"/>
          <feDisplacementMap in="SourceGraphic" in2="noise" scale="3"/>
        </filter>
        
        {/* 팽창 왜곡 효과 */}
        <filter id="expansionWarp">
          <feTurbulence baseFrequency="0.02" numOctaves="3" result="warp"/>
          <feDisplacementMap in="SourceGraphic" in2="warp" scale="5"/>
        </filter>
        
        {/* 진공 에너지 패턴 */}
        <pattern id="vacuumEnergy" x="0" y="0" width="8" height="8" patternUnits="userSpaceOnUse">
          <circle cx="4" cy="4" r="1.5" fill="#9C27B0" opacity="0.4"/>
          <circle cx="4" cy="4" r="0.8" fill="#E91E63" opacity="0.6"/>
        </pattern>
      </defs>
      
      {/* 우주 배경 - 다크에너지 필드 */}
      <rect x="0" y="0" width="100" height="100" fill="url(#darkEnergyField)" />
      
      {/* 진공 에너지 패턴 */}
      <rect x="0" y="0" width="100" height="100" fill="url(#vacuumEnergy)" opacity="0.3" />
      
      {/* 팽창하는 우주 격자 - 더 화려하게 */}
      <g className="animate-pulse" style={{ animationDuration: '4s' }}>
        <g stroke="#9C27B0" strokeWidth="1.2" opacity="0.7" filter="url(#expansionWarp)">
          <line x1="10" y1="0" x2="10" y2="100" />
          <line x1="20" y1="0" x2="20" y2="100" />
          <line x1="30" y1="0" x2="30" y2="100" />
          <line x1="40" y1="0" x2="40" y2="100" />
          <line x1="50" y1="0" x2="50" y2="100" />
          <line x1="60" y1="0" x2="60" y2="100" />
          <line x1="70" y1="0" x2="70" y2="100" />
          <line x1="80" y1="0" x2="80" y2="100" />
          <line x1="90" y1="0" x2="90" y2="100" />
          
          <line x1="0" y1="10" x2="100" y2="10" />
          <line x1="0" y1="20" x2="100" y2="20" />
          <line x1="0" y1="30" x2="100" y2="30" />
          <line x1="0" y1="40" x2="100" y2="40" />
          <line x1="0" y1="50" x2="100" y2="50" />
          <line x1="0" y1="60" x2="100" y2="60" />
          <line x1="0" y1="70" x2="100" y2="70" />
          <line x1="0" y1="80" x2="100" y2="80" />
          <line x1="0" y1="90" x2="100" y2="90" />
        </g>
      </g>
      
      {/* 추가 격자 - 대각선 */}
      <g className="animate-pulse" style={{ animationDuration: '5s', animationDelay: '1s' }} opacity="0.5">
        <g stroke="#E91E63" strokeWidth="0.8">
          <line x1="0" y1="0" x2="100" y2="100" />
          <line x1="0" y1="100" x2="100" y2="0" />
          <line x1="0" y1="25" x2="75" y2="100" />
          <line x1="25" y1="0" x2="100" y2="75" />
          <line x1="0" y1="75" x2="25" y2="100" />
          <line x1="75" y1="0" x2="100" y2="25" />
        </g>
      </g>
      
      {/* 가속 팽창 에너지 파동들 - 더 많은 레이어 */}
      <g className="animate-pulse" style={{ animationDuration: '2.5s' }}>
        <circle cx="50" cy="50" r="12" fill="none" stroke="url(#expansionForce1)" strokeWidth="4" opacity="0.9" filter="url(#darkEnergyGlow)" />
        <circle cx="50" cy="50" r="18" fill="none" stroke="url(#expansionForce2)" strokeWidth="3.5" opacity="0.8" filter="url(#darkEnergyGlow)" />
        <circle cx="50" cy="50" r="25" fill="none" stroke="url(#expansionForce3)" strokeWidth="3" opacity="0.7" filter="url(#darkEnergyGlow)" />
        <circle cx="50" cy="50" r="32" fill="none" stroke="url(#expansionForce1)" strokeWidth="2.5" opacity="0.6" filter="url(#darkEnergyGlow)" />
        <circle cx="50" cy="50" r="38" fill="none" stroke="url(#expansionForce2)" strokeWidth="2" opacity="0.5" filter="url(#darkEnergyGlow)" />
        <circle cx="50" cy="50" r="44" fill="none" stroke="url(#expansionForce3)" strokeWidth="1.5" opacity="0.4" filter="url(#darkEnergyGlow)" />
      </g>
      
      {/* 추가 에너지 파동 */}
      <g className="animate-pulse" style={{ animationDuration: '3s', animationDelay: '0.8s' }}>
        <circle cx="50" cy="50" r="15" fill="none" stroke="#FF5722" strokeWidth="2.5" opacity="0.7" />
        <circle cx="50" cy="50" r="22" fill="none" stroke="#FF9800" strokeWidth="2" opacity="0.6" />
        <circle cx="50" cy="50" r="29" fill="none" stroke="#FFEB3B" strokeWidth="1.8" opacity="0.5" />
        <circle cx="50" cy="50" r="35" fill="none" stroke="#4CAF50" strokeWidth="1.5" opacity="0.4" />
        <circle cx="50" cy="50" r="41" fill="none" stroke="#00BCD4" strokeWidth="1.2" opacity="0.3" />
      </g>
      
      {/* 팽창 방향 표시 - 더 화려한 에너지 흐름 */}
      <g className="animate-pulse" style={{ animationDuration: '3s' }}>
        {/* 상하좌우 방향 */}
        <path d="M50 30 Q45 20 40 15 Q42 13 45 15 Q50 20 50 30" fill="url(#expansionForce1)" opacity="0.8" filter="url(#energyFlicker)" />
        <path d="M50 70 Q55 80 60 85 Q58 87 55 85 Q50 80 50 70" fill="url(#expansionForce1)" opacity="0.8" filter="url(#energyFlicker)" />
        <path d="M30 50 Q20 45 15 40 Q13 42 15 45 Q20 50 30 50" fill="url(#expansionForce2)" opacity="0.8" filter="url(#energyFlicker)" />
        <path d="M70 50 Q80 55 85 60 Q87 58 85 55 Q80 50 70 50" fill="url(#expansionForce2)" opacity="0.8" filter="url(#energyFlicker)" />
        
        {/* 대각선 방향 */}
        <path d="M35 35 Q25 25 18 18 Q20 16 25 21 Q35 31 35 35" fill="url(#expansionForce3)" opacity="0.7" filter="url(#energyFlicker)" />
        <path d="M65 65 Q75 75 82 82 Q80 84 75 79 Q65 69 65 65" fill="url(#expansionForce3)" opacity="0.7" filter="url(#energyFlicker)" />
        <path d="M65 35 Q75 25 82 18 Q84 20 79 25 Q69 35 65 35" fill="url(#expansionForce1)" opacity="0.7" filter="url(#energyFlicker)" />
        <path d="M35 65 Q25 75 18 82 Q16 80 21 75 Q31 65 35 65" fill="url(#expansionForce2)" opacity="0.7" filter="url(#energyFlicker)" />
        
        {/* 추가 중간 방향들 */}
        <path d="M50 20 Q48 12 45 8 Q47 6 50 10 Q52 15 50 20" fill="url(#expansionForce1)" opacity="0.6" />
        <path d="M50 80 Q52 88 55 92 Q53 94 50 90 Q48 85 50 80" fill="url(#expansionForce1)" opacity="0.6" />
        <path d="M20 50 Q12 48 8 45 Q6 47 10 50 Q15 52 20 50" fill="url(#expansionForce2)" opacity="0.6" />
        <path d="M80 50 Q88 52 92 55 Q94 53 90 50 Q85 48 80 50" fill="url(#expansionForce2)" opacity="0.6" />
      </g>
      
      {/* 은하들이 멀어지는 모습 - 더 많고 화려하게 */}
      <g className="animate-pulse" style={{ animationDuration: '6s' }}>
        <ellipse cx="12" cy="12" rx="5" ry="2.5" fill="url(#expansionForce1)" opacity="0.9" transform="rotate(45 12 12)" filter="url(#energyFlicker)" />
        <ellipse cx="88" cy="12" rx="4.5" ry="2.2" fill="url(#expansionForce2)" opacity="0.8" transform="rotate(-30 88 12)" filter="url(#energyFlicker)" />
        <ellipse cx="12" cy="88" rx="5.2" ry="2.6" fill="url(#expansionForce3)" opacity="0.9" transform="rotate(60 12 88)" filter="url(#energyFlicker)" />
        <ellipse cx="88" cy="88" rx="4.8" ry="2.4" fill="url(#expansionForce1)" opacity="0.7" transform="rotate(-45 88 88)" filter="url(#energyFlicker)" />
        <ellipse cx="25" cy="25" rx="3.5" ry="1.8" fill="url(#expansionForce2)" opacity="0.8" transform="rotate(30 25 25)" />
        <ellipse cx="75" cy="25" rx="4" ry="2" fill="url(#expansionForce3)" opacity="0.7" transform="rotate(-60 75 25)" />
        <ellipse cx="25" cy="75" rx="3.8" ry="1.9" fill="url(#expansionForce1)" opacity="0.8" transform="rotate(45 25 75)" />
        <ellipse cx="75" cy="75" rx="4.2" ry="2.1" fill="url(#expansionForce2)" opacity="0.6" transform="rotate(-30 75 75)" />
      </g>
      
      {/* 중심 다크에너지 소스 - 더 화려하게 */}
      <circle 
        cx="50" 
        cy="50" 
        r="8" 
        fill="url(#cosmologicalConstant)" 
        stroke="url(#expansionForce1)" 
        strokeWidth="4"
        filter="url(#darkEnergyGlow)"
        className="animate-pulse"
        style={{ animationDuration: '1.8s' }}
      />
      <circle 
        cx="50" 
        cy="50" 
        r="5" 
        fill="url(#expansionForce3)" 
        className="animate-pulse"
        style={{ animationDuration: '1.2s' }}
      />
      <circle 
        cx="50" 
        cy="50" 
        r="2" 
        fill="#FFFFFF" 
        className="animate-pulse"
        style={{ animationDuration: '0.8s' }}
      />
      
      {/* 다크에너지 입자들 - 더 많고 화려하게 */}
      <g className="animate-spin" style={{ animationDuration: '10s', transformOrigin: '50px 50px' }}>
        <circle cx="25" cy="20" r="1.2" fill="#9C27B0" className="animate-ping" filter="url(#energyFlicker)" />
        <circle cx="75" cy="25" r="1" fill="#E91E63" className="animate-ping" style={{ animationDelay: '1s' }} filter="url(#energyFlicker)" />
        <circle cx="80" cy="75" r="1.4" fill="#673AB7" className="animate-ping" style={{ animationDelay: '2s' }} filter="url(#energyFlicker)" />
        <circle cx="20" cy="80" r="1.1" fill="#3F51B5" className="animate-ping" style={{ animationDelay: '3s' }} filter="url(#energyFlicker)" />
        <circle cx="15" cy="45" r="0.9" fill="#2196F3" className="animate-ping" style={{ animationDelay: '4s' }} filter="url(#energyFlicker)" />
        <circle cx="85" cy="40" r="1.3" fill="#FF5722" className="animate-ping" style={{ animationDelay: '5s' }} filter="url(#energyFlicker)" />
        <circle cx="45" cy="15" r="0.8" fill="#FF9800" className="animate-ping" style={{ animationDelay: '6s' }} filter="url(#energyFlicker)" />
        <circle cx="55" cy="85" r="1.1" fill="#FFEB3B" className="animate-ping" style={{ animationDelay: '7s' }} filter="url(#energyFlicker)" />
        <circle cx="35" cy="10" r="0.7" fill="#4CAF50" className="animate-ping" style={{ animationDelay: '8s' }} filter="url(#energyFlicker)" />
        <circle cx="65" cy="90" r="1" fill="#00BCD4" className="animate-ping" style={{ animationDelay: '9s' }} filter="url(#energyFlicker)" />
      </g>
      
      {/* 반대 방향 입자 회전 */}
      <g className="animate-spin" style={{ animationDuration: '12s', transformOrigin: '50px 50px', animationDirection: 'reverse' }}>
        <circle cx="30" cy="30" r="0.8" fill="#7B1FA2" className="animate-ping" />
        <circle cx="70" cy="30" r="0.9" fill="#8E24AA" className="animate-ping" style={{ animationDelay: '2s' }} />
        <circle cx="70" cy="70" r="0.7" fill="#AB47BC" className="animate-ping" style={{ animationDelay: '4s' }} />
        <circle cx="30" cy="70" r="1" fill="#CE93D8" className="animate-ping" style={{ animationDelay: '6s' }} />
        <circle cx="10" cy="50" r="0.6" fill="#E1BEE7" className="animate-ping" style={{ animationDelay: '8s' }} />
        <circle cx="90" cy="50" r="0.8" fill="#F3E5F5" className="animate-ping" style={{ animationDelay: '10s' }} />
      </g>
      
      {/* 보이지 않는 에너지 파동 - 더 복잡하게 */}
      <g className="animate-ping" style={{ animationDuration: '4s' }}>
        <circle cx="50" cy="50" r="20" fill="none" stroke="#9C27B0" strokeWidth="1" opacity="0.6" />
        <circle cx="50" cy="50" r="28" fill="none" stroke="#E91E63" strokeWidth="0.8" opacity="0.5" />
        <circle cx="50" cy="50" r="36" fill="none" stroke="#FF5722" strokeWidth="0.6" opacity="0.4" />
        <circle cx="50" cy="50" r="43" fill="none" stroke="#FF9800" strokeWidth="0.4" opacity="0.3" />
        <circle cx="50" cy="50" r="48" fill="none" stroke="#FFEB3B" strokeWidth="0.2" opacity="0.2" />
      </g>
      
      {/* 추가 에너지 파동 */}
      <g className="animate-ping" style={{ animationDuration: '5s', animationDelay: '1.5s' }}>
        <circle cx="50" cy="50" r="24" fill="none" stroke="#673AB7" strokeWidth="0.9" opacity="0.5" />
        <circle cx="50" cy="50" r="31" fill="none" stroke="#3F51B5" strokeWidth="0.7" opacity="0.4" />
        <circle cx="50" cy="50" r="39" fill="none" stroke="#2196F3" strokeWidth="0.5" opacity="0.3" />
        <circle cx="50" cy="50" r="45" fill="none" stroke="#00BCD4" strokeWidth="0.3" opacity="0.2" />
      </g>
      
      {/* 우주 상수 표시 */}
      <text x="50" y="8" textAnchor="middle" fill="#E91E63" fontSize="4" opacity="0.9" fontWeight="bold">
        Dark Energy
      </text>
      <text x="50" y="15" textAnchor="middle" fill="#9C27B0" fontSize="3.5" opacity="0.8">
        Λ ≈ 10⁻⁵² m⁻²
      </text>
      
      {/* 암흑 에너지 비율 */}
      <text x="50" y="92" textAnchor="middle" fill="#FF5722" fontSize="4" opacity="0.9" fontWeight="bold">
        ~68% of Universe
      </text>
      
      {/* 허블 상수 */}
      <text x="50" y="98" textAnchor="middle" fill="#FF9800" fontSize="3" opacity="0.8">
        H₀ = 67.4 km/s/Mpc
      </text>
      
      {/* 상태 방정식 */}
      <text x="8" y="50" fill="#673AB7" fontSize="2.8" opacity="0.8">
        w ≈ -1
      </text>
      
      {/* 밀도 매개변수 */}
      <text x="92" y="50" fill="#3F51B5" fontSize="2.8" opacity="0.7" textAnchor="end">
        ΩΛ ≈ 0.68
      </text>
      
      {/* 가속 팽창 */}
      <text x="8" y="25" fill="#E91E63" fontSize="2.5" opacity="0.7">
        Accelerating
      </text>
      
      {/* 진공 에너지 */}
      <text x="92" y="25" fill="#9C27B0" fontSize="2.5" opacity="0.6" textAnchor="end">
        Vacuum Energy
      </text>
    </svg>
  );
}