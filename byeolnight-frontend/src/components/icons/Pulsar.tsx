import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Pulsar({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        {/* 펄사 중심 - 극도로 압축된 중성자별 */}
        <radialGradient id="pulsarCore" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="25%" stopColor="#E1F5FE" />
          <stop offset="50%" stopColor="#00E5FF" />
          <stop offset="75%" stopColor="#2196F3" />
          <stop offset="100%" stopColor="#1565C0" />
        </radialGradient>
        
        {/* 전파 빔 그라디언트 */}
        <linearGradient id="radioBeam" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="10%" stopColor="#00E5FF" opacity="0.3" />
          <stop offset="30%" stopColor="#FFFFFF" opacity="0.9" />
          <stop offset="50%" stopColor="#00E5FF" opacity="1" />
          <stop offset="70%" stopColor="#FFFFFF" opacity="0.9" />
          <stop offset="90%" stopColor="#00E5FF" opacity="0.3" />
          <stop offset="100%" stopColor="transparent" />
        </linearGradient>
        
        {/* X선 빔 그라디언트 */}
        <linearGradient id="xrayBeam" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="20%" stopColor="#E91E63" opacity="0.7" />
          <stop offset="50%" stopColor="#FFFFFF" opacity="1" />
          <stop offset="80%" stopColor="#E91E63" opacity="0.7" />
          <stop offset="100%" stopColor="transparent" />
        </linearGradient>
        
        {/* 감마선 빔 그라디언트 */}
        <linearGradient id="gammaBeam" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="25%" stopColor="#9C27B0" opacity="0.8" />
          <stop offset="50%" stopColor="#FFFFFF" opacity="1" />
          <stop offset="75%" stopColor="#9C27B0" opacity="0.8" />
          <stop offset="100%" stopColor="transparent" />
        </linearGradient>
        
        {/* 자기권 그라디언트 */}
        <radialGradient id="magnetosphere" cx="50%" cy="50%" r="90%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="50%" stopColor="transparent" />
          <stop offset="80%" stopColor="#E1F5FE" opacity="0.4" />
          <stop offset="100%" stopColor="#81D4FA" opacity="0.6" />
        </radialGradient>
        
        {/* 초강력 펄사 글로우 */}
        <filter id="pulsarGlow">
          <feGaussianBlur stdDeviation="8" result="coloredBlur"/>
          <feGaussianBlur stdDeviation="16" result="bigBlur"/>
          <feMerge> 
            <feMergeNode in="bigBlur"/>
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
        
        {/* 전파 간섭 효과 */}
        <filter id="radioInterference">
          <feTurbulence baseFrequency="0.8" numOctaves="3" result="noise"/>
          <feDisplacementMap in="SourceGraphic" in2="noise" scale="2"/>
        </filter>
        
        {/* 펄스 깜빡임 효과 */}
        <filter id="pulseFlicker">
          <feTurbulence baseFrequency="1.2" numOctaves="2" result="flicker"/>
          <feDisplacementMap in="SourceGraphic" in2="flicker" scale="1"/>
        </filter>
      </defs>
      
      {/* 자기권 - 거대한 자기장 */}
      <circle 
        cx="50" 
        cy="50" 
        r="47" 
        fill="url(#magnetosphere)"
        className="animate-pulse"
        style={{ animationDuration: '4s' }}
      />
      
      {/* 전파 망원경 신호 패턴 */}
      <g className="animate-pulse" style={{ animationDuration: '1.337s' }}>
        <circle cx="50" cy="50" r="35" fill="none" stroke="#00E5FF" strokeWidth="0.8" opacity="0.6" strokeDasharray="4,2" />
        <circle cx="50" cy="50" r="30" fill="none" stroke="#81D4FA" strokeWidth="0.6" opacity="0.5" strokeDasharray="3,1" />
        <circle cx="50" cy="50" r="25" fill="none" stroke="#B3E5FC" strokeWidth="0.4" opacity="0.4" strokeDasharray="2,1" />
      </g>
      
      {/* 주요 전파 빔 - 정확한 펄스 주기 */}
      <g className="animate-spin" style={{ animationDuration: '1.337s', transformOrigin: '50px 50px' }}>
        <rect 
          x="5" y="47" width="90" height="6" 
          fill="url(#radioBeam)" 
          filter="url(#pulsarGlow)"
          opacity="0.95"
        />
        <rect 
          x="47" y="5" width="6" height="90" 
          fill="url(#radioBeam)" 
          filter="url(#pulsarGlow)"
          opacity="0.95"
        />
      </g>
      
      {/* X선 빔 - 다른 주기 */}
      <g className="animate-spin" style={{ animationDuration: '0.668s', transformOrigin: '50px 50px' }}>
        <rect 
          x="10" y="48" width="80" height="4" 
          fill="url(#xrayBeam)" 
          filter="url(#pulsarGlow)"
          opacity="0.8"
        />
        <rect 
          x="48" y="10" width="4" height="80" 
          fill="url(#xrayBeam)" 
          filter="url(#pulsarGlow)"
          opacity="0.8"
        />
      </g>
      
      {/* 감마선 빔 - 또 다른 주기 */}
      <g className="animate-spin" style={{ animationDuration: '2.674s', transformOrigin: '50px 50px' }}>
        <line 
          x1="15" y1="15" x2="85" y2="85" 
          stroke="url(#gammaBeam)" 
          strokeWidth="5" 
          opacity="0.7"
          filter="url(#pulsarGlow)"
        />
        <line 
          x1="85" y1="15" x2="15" y2="85" 
          stroke="url(#gammaBeam)" 
          strokeWidth="5" 
          opacity="0.7"
          filter="url(#pulsarGlow)"
        />
      </g>
      
      {/* 보조 전파 빔들 - 더 복잡한 패턴 */}
      <g className="animate-spin" style={{ animationDuration: '0.445s', animationDirection: 'reverse', transformOrigin: '50px 50px' }}>
        <rect x="20" y="49" width="60" height="2" fill="#00E5FF" opacity="0.6" filter="url(#radioInterference)" />
        <rect x="49" y="20" width="2" height="60" fill="#00E5FF" opacity="0.6" filter="url(#radioInterference)" />
      </g>
      
      {/* 펄사 중심 - 극도로 압축된 중성자별 */}
      <circle 
        cx="50" 
        cy="50" 
        r="10" 
        fill="url(#pulsarCore)"
        filter="url(#pulsarGlow)"
        className="animate-pulse"
        style={{ animationDuration: '0.337s' }}
      />
      
      {/* 내부 핵심 */}
      <circle 
        cx="50" 
        cy="50" 
        r="6" 
        fill="#FFFFFF"
        className="animate-pulse"
        style={{ animationDuration: '0.168s' }}
      />
      
      {/* 자기장 라인들 - 복잡한 구조 */}
      <g className="animate-pulse" style={{ animationDuration: '3s' }}>
        <ellipse cx="50" cy="50" rx="28" ry="16" fill="none" stroke="#81D4FA" strokeWidth="1.2" opacity="0.6" />
        <ellipse cx="50" cy="50" rx="32" ry="19" fill="none" stroke="#B3E5FC" strokeWidth="1" opacity="0.5" />
        <ellipse cx="50" cy="50" rx="36" ry="22" fill="none" stroke="#E1F5FE" strokeWidth="0.8" opacity="0.4" />
        <ellipse cx="50" cy="50" rx="16" ry="28" fill="none" stroke="#00E5FF" strokeWidth="1" opacity="0.7" />
        <ellipse cx="50" cy="50" rx="19" ry="32" fill="none" stroke="#81D4FA" strokeWidth="0.8" opacity="0.6" />
      </g>
      
      {/* 전파 펄스 - 정확한 주기성 */}
      <g className="animate-ping" style={{ animationDuration: '1.337s' }}>
        <circle cx="50" cy="50" r="18" fill="none" stroke="#00E5FF" strokeWidth="2" opacity="0.9" />
        <circle cx="50" cy="50" r="22" fill="none" stroke="#81D4FA" strokeWidth="1.5" opacity="0.7" />
        <circle cx="50" cy="50" r="26" fill="none" stroke="#B3E5FC" strokeWidth="1.2" opacity="0.5" />
        <circle cx="50" cy="50" r="30" fill="none" stroke="#E1F5FE" strokeWidth="1" opacity="0.3" />
      </g>
      
      {/* 추가 펄스 링 */}
      <g className="animate-ping" style={{ animationDuration: '0.668s', animationDelay: '0.2s' }}>
        <circle cx="50" cy="50" r="15" fill="none" stroke="#FFFFFF" strokeWidth="1.5" opacity="0.8" />
        <circle cx="50" cy="50" r="20" fill="none" stroke="#E1F5FE" strokeWidth="1" opacity="0.6" />
        <circle cx="50" cy="50" r="24" fill="none" stroke="#B3E5FC" strokeWidth="0.8" opacity="0.4" />
      </g>
      
      {/* 고에너지 입자들 - 빠른 회전 */}
      <g className="animate-spin" style={{ animationDuration: '0.1s', transformOrigin: '50px 50px' }}>
        <circle cx="25" cy="25" r="1.2" fill="#FFFFFF" className="animate-ping" filter="url(#pulseFlicker)" />
        <circle cx="75" cy="25" r="1" fill="#00E5FF" className="animate-ping" />
        <circle cx="75" cy="75" r="1.3" fill="#FFFFFF" className="animate-ping" filter="url(#pulseFlicker)" />
        <circle cx="25" cy="75" r="0.9" fill="#00E5FF" className="animate-ping" />
      </g>
      
      {/* 반대 방향 입자 */}
      <g className="animate-spin" style={{ animationDuration: '0.15s', animationDirection: 'reverse', transformOrigin: '50px 50px' }}>
        <circle cx="15" cy="50" r="0.8" fill="#81D4FA" className="animate-ping" />
        <circle cx="85" cy="50" r="0.7" fill="#B3E5FC" className="animate-ping" />
        <circle cx="50" cy="15" r="0.9" fill="#E1F5FE" className="animate-ping" />
        <circle cx="50" cy="85" r="0.6" fill="#00E5FF" className="animate-ping" />
      </g>
      
      {/* 전파 간섭 패턴 */}
      <g className="animate-pulse" style={{ animationDuration: '2s' }} opacity="0.4">
        <path d="M20 20 Q35 35 50 20 Q65 5 80 20" fill="none" stroke="#00E5FF" strokeWidth="0.5" />
        <path d="M20 80 Q35 65 50 80 Q65 95 80 80" fill="none" stroke="#81D4FA" strokeWidth="0.4" />
        <path d="M20 50 Q35 35 50 50 Q65 65 80 50" fill="none" stroke="#B3E5FC" strokeWidth="0.3" />
      </g>
      
      {/* 시공간 왜곡 효과 */}
      <g className="animate-pulse" style={{ animationDuration: '8s' }} opacity="0.3">
        <circle cx="50" cy="50" r="40" fill="none" stroke="#FFFFFF" strokeWidth="0.3" strokeDasharray="8,4" />
        <circle cx="50" cy="50" r="44" fill="none" stroke="#E1F5FE" strokeWidth="0.2" strokeDasharray="6,3" />
      </g>
      
      {/* 펄스 주기 표시 */}
      <text x="50" y="8" textAnchor="middle" fill="#00E5FF" fontSize="3.5" opacity="0.9">
        P = 1.337 s
      </text>
      
      {/* 자기장 강도 */}
      <text x="8" y="15" fill="#81D4FA" fontSize="2.5" opacity="0.8">
        B ~ 10¹² G
      </text>
      
      {/* 전파 주파수 */}
      <text x="50" y="95" textAnchor="middle" fill="#B3E5FC" fontSize="3" opacity="0.7">
        f ~ 1.4 GHz
      </text>
      
      {/* 회전 속도 */}
      <text x="92" y="50" fill="#E1F5FE" fontSize="2.5" opacity="0.6" textAnchor="end">
        Ω ~ 0.75 Hz
      </text>
    </svg>
  );
}