import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

const StellarSolarSystem = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      {/* 태양 그라디언트 */}
      <radialGradient id="sunGradient" cx="50%" cy="50%" r="60%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="30%" stopColor="#FFEB3B" />
        <stop offset="60%" stopColor="#FF9800" />
        <stop offset="100%" stopColor="#FF5722" />
      </radialGradient>
      
      {/* 행성별 그라디언트 */}
      <radialGradient id="mercuryGrad" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#BCAAA4" />
        <stop offset="100%" stopColor="#8D6E63" />
      </radialGradient>
      
      <radialGradient id="venusGrad" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#FFEB3B" />
        <stop offset="100%" stopColor="#FFC107" />
      </radialGradient>
      
      <radialGradient id="earthGrad" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#81D4FA" />
        <stop offset="50%" stopColor="#2196F3" />
        <stop offset="100%" stopColor="#1976D2" />
      </radialGradient>
      
      <radialGradient id="marsGrad" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#FF8A65" />
        <stop offset="100%" stopColor="#F44336" />
      </radialGradient>
      
      <radialGradient id="jupiterGrad" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#FFCC02" />
        <stop offset="50%" stopColor="#FF9800" />
        <stop offset="100%" stopColor="#E65100" />
      </radialGradient>
      
      <radialGradient id="saturnGrad" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#FFF8DC" />
        <stop offset="100%" stopColor="#F4E4BC" />
      </radialGradient>
      
      <radialGradient id="uranusGrad" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#81D4FA" />
        <stop offset="100%" stopColor="#4FC3F7" />
      </radialGradient>
      
      <radialGradient id="neptuneGrad" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#5C6BC0" />
        <stop offset="100%" stopColor="#3F51B5" />
      </radialGradient>
      
      {/* 태양풍 그라디언트 */}
      <radialGradient id="solarWind" cx="50%" cy="50%" r="80%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="60%" stopColor="#FFEB3B" opacity="0.3" />
        <stop offset="100%" stopColor="#FF9800" opacity="0.5" />
      </radialGradient>
      
      {/* 강력한 태양계 글로우 */}
      <filter id="solarGlow">
        <feGaussianBlur stdDeviation="6" result="coloredBlur"/>
        <feGaussianBlur stdDeviation="12" result="bigBlur"/>
        <feMerge> 
          <feMergeNode in="bigBlur"/>
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
      
      {/* 궤도 글로우 */}
      <filter id="orbitGlow">
        <feGaussianBlur stdDeviation="2" result="blur"/>
        <feOffset in="blur" dx="0" dy="0" result="offset"/>
      </filter>
      
      {/* 행성 대기 효과 */}
      <filter id="atmosphere">
        <feGaussianBlur stdDeviation="1" result="blur"/>
        <feOffset in="blur" dx="0.5" dy="0.5" result="offset"/>
      </filter>
    </defs>
    
    {/* 태양풍과 헬리오스피어 */}
    <circle 
      cx="50" 
      cy="50" 
      r="48" 
      fill="url(#solarWind)"
      className="animate-pulse"
      style={{ animationDuration: '8s' }}
    />
    
    {/* 태양 - 핵융합 반응 */}
    <circle 
      cx="50" 
      cy="50" 
      r="8" 
      fill="url(#sunGradient)" 
      filter="url(#solarGlow)"
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    
    {/* 태양 코로나 */}
    <circle 
      cx="50" 
      cy="50" 
      r="12" 
      fill="none"
      stroke="#FFEB3B"
      strokeWidth="0.5"
      opacity="0.6"
      className="animate-pulse"
      style={{ animationDuration: '3s' }}
    />
    
    {/* 궤도들 - 더 화려하게 */}
    <g fill="none" strokeWidth="0.8" opacity="0.4" filter="url(#orbitGlow)">
      <circle cx="50" cy="50" r="14" stroke="#8D6E63" strokeDasharray="2,1" />
      <circle cx="50" cy="50" r="17" stroke="#FFC107" strokeDasharray="3,1" />
      <circle cx="50" cy="50" r="21" stroke="#2196F3" strokeDasharray="4,1" />
      <circle cx="50" cy="50" r="25" stroke="#F44336" strokeDasharray="3,2" />
      <circle cx="50" cy="50" r="31" stroke="#FF9800" strokeDasharray="5,2" />
      <circle cx="50" cy="50" r="36" stroke="#F4E4BC" strokeDasharray="4,3" />
      <circle cx="50" cy="50" r="40" stroke="#4FC3F7" strokeDasharray="3,3" />
      <circle cx="50" cy="50" r="44" stroke="#3F51B5" strokeDasharray="2,4" />
    </g>
    
    {/* 수성 - 88일 공전 */}
    <g className="animate-spin" style={{ animationDuration: '4s', transformOrigin: '50px 50px' }}>
      <circle cx="64" cy="50" r="1.2" fill="url(#mercuryGrad)" filter="url(#atmosphere)" />
      <circle cx="64" cy="50" r="0.8" fill="#BCAAA4" opacity="0.8" />
    </g>
    
    {/* 금성 - 225일 공전 */}
    <g className="animate-spin" style={{ animationDuration: '6s', transformOrigin: '50px 50px' }}>
      <circle cx="67" cy="50" r="1.4" fill="url(#venusGrad)" filter="url(#atmosphere)" />
      <circle cx="67" cy="50" r="1" fill="#FFEB3B" opacity="0.9" />
      {/* 금성 대기 */}
      <circle cx="67" cy="50" r="1.8" fill="none" stroke="#FFEB3B" strokeWidth="0.3" opacity="0.5" />
    </g>
    
    {/* 지구 + 달 - 365일 공전 */}
    <g className="animate-spin" style={{ animationDuration: '8s', transformOrigin: '50px 50px' }}>
      <circle cx="71" cy="50" r="1.6" fill="url(#earthGrad)" filter="url(#atmosphere)" />
      <circle cx="71" cy="50" r="1.2" fill="#4CAF50" opacity="0.6" />
      {/* 지구 대기 */}
      <circle cx="71" cy="50" r="2" fill="none" stroke="#81D4FA" strokeWidth="0.4" opacity="0.6" />
      {/* 달 - 27일 공전 */}
      <g className="animate-spin" style={{ animationDuration: '1s', transformOrigin: '71px 50px' }}>
        <circle cx="74" cy="50" r="0.5" fill="#9E9E9E" />
      </g>
    </g>
    
    {/* 화성 - 687일 공전 */}
    <g className="animate-spin" style={{ animationDuration: '12s', transformOrigin: '50px 50px' }}>
      <circle cx="75" cy="50" r="1.3" fill="url(#marsGrad)" filter="url(#atmosphere)" />
      <circle cx="75" cy="50" r="0.9" fill="#FF5722" opacity="0.8" />
      {/* 화성 극관 */}
      <circle cx="75" cy="49" r="0.3" fill="#FFFFFF" opacity="0.7" />
      <circle cx="75" cy="51" r="0.3" fill="#FFFFFF" opacity="0.7" />
    </g>
    
    {/* 소행성대 */}
    <g className="animate-spin" style={{ animationDuration: '16s', transformOrigin: '50px 50px' }}>
      <circle cx="78" cy="50" r="0.4" fill="#795548" className="animate-ping" />
      <circle cx="79" cy="52" r="0.3" fill="#8D6E63" className="animate-ping" style={{ animationDelay: '1s' }} />
      <circle cx="77" cy="48" r="0.35" fill="#A1887F" className="animate-ping" style={{ animationDelay: '2s' }} />
      <circle cx="80" cy="49" r="0.25" fill="#6D4C41" className="animate-ping" style={{ animationDelay: '3s' }} />
      <circle cx="76" cy="51" r="0.3" fill="#5D4037" className="animate-ping" style={{ animationDelay: '4s' }} />
    </g>
    
    {/* 목성 - 12년 공전 */}
    <g className="animate-spin" style={{ animationDuration: '20s', transformOrigin: '50px 50px' }}>
      <circle cx="81" cy="50" r="3" fill="url(#jupiterGrad)" filter="url(#atmosphere)" />
      {/* 목성 대적점 */}
      <circle cx="82" cy="50" r="0.8" fill="#F44336" opacity="0.8" />
      {/* 목성 띠들 */}
      <ellipse cx="81" cy="49" rx="2.8" ry="0.4" fill="#E65100" opacity="0.6" />
      <ellipse cx="81" cy="51" rx="2.8" ry="0.4" fill="#FF8F00" opacity="0.6" />
      {/* 갈릴레이 위성들 */}
      <g className="animate-spin" style={{ animationDuration: '2s', transformOrigin: '81px 50px' }}>
        <circle cx="84" cy="50" r="0.3" fill="#FFEB3B" />
        <circle cx="78" cy="50" r="0.25" fill="#FFC107" />
        <circle cx="81" cy="47" r="0.35" fill="#FF9800" />
        <circle cx="81" cy="53" r="0.4" fill="#FF8F00" />
      </g>
    </g>
    
    {/* 토성 - 29년 공전 */}
    <g className="animate-spin" style={{ animationDuration: '24s', transformOrigin: '50px 50px' }}>
      <circle cx="86" cy="50" r="2.5" fill="url(#saturnGrad)" filter="url(#atmosphere)" />
      {/* 토성 고리 시스템 */}
      <ellipse cx="86" cy="50" rx="4" ry="1" fill="none" stroke="#F4E4BC" strokeWidth="0.8" opacity="0.8" />
      <ellipse cx="86" cy="50" rx="3.5" ry="0.8" fill="none" stroke="#E6D3A3" strokeWidth="0.6" opacity="0.9" />
      <ellipse cx="86" cy="50" rx="3" ry="0.6" fill="none" stroke="#D2B48C" strokeWidth="0.4" opacity="0.7" />
      {/* 타이탄 */}
      <g className="animate-spin" style={{ animationDuration: '3s', transformOrigin: '86px 50px' }}>
        <circle cx="89" cy="50" r="0.4" fill="#C8B99C" />
      </g>
    </g>
    
    {/* 천왕성 - 84년 공전 */}
    <g className="animate-spin" style={{ animationDuration: '28s', transformOrigin: '50px 50px' }}>
      <circle cx="90" cy="50" r="1.8" fill="url(#uranusGrad)" filter="url(#atmosphere)" />
      {/* 천왕성 고리 (수직) */}
      <ellipse cx="90" cy="50" rx="0.4" ry="2.5" fill="none" stroke="#4FC3F7" strokeWidth="0.3" opacity="0.6" />
      <ellipse cx="90" cy="50" rx="0.6" ry="2.8" fill="none" stroke="#29B6F6" strokeWidth="0.2" opacity="0.5" />
    </g>
    
    {/* 해왕성 - 165년 공전 */}
    <g className="animate-spin" style={{ animationDuration: '32s', transformOrigin: '50px 50px' }}>
      <circle cx="94" cy="50" r="1.6" fill="url(#neptuneGrad)" filter="url(#atmosphere)" />
      {/* 해왕성 대암점 */}
      <circle cx="94" cy="49" r="0.4" fill="#1A237E" opacity="0.8" />
      {/* 트리톤 */}
      <g className="animate-spin" style={{ animationDuration: '1.5s', transformOrigin: '94px 50px', animationDirection: 'reverse' }}>
        <circle cx="92" cy="50" r="0.3" fill="#B39DDB" />
      </g>
    </g>
    
    {/* 태양풍 - 방사형 */}
    <g className="animate-pulse" style={{ animationDuration: '4s' }} opacity="0.6">
      <path d="M50 42 Q45 30 40 25" stroke="#FFEB3B" strokeWidth="1.5" fill="none" />
      <path d="M50 58 Q55 70 60 75" stroke="#FFEB3B" strokeWidth="1.5" fill="none" />
      <path d="M42 50 Q30 45 25 40" stroke="#FF9800" strokeWidth="1.2" fill="none" />
      <path d="M58 50 Q70 55 75 60" stroke="#FF9800" strokeWidth="1.2" fill="none" />
      <path d="M46 46 Q35 35 28 28" stroke="#FFC107" strokeWidth="1" fill="none" />
      <path d="M54 54 Q65 65 72 72" stroke="#FFC107" strokeWidth="1" fill="none" />
      <path d="M54 46 Q65 35 72 28" stroke="#FFEB3B" strokeWidth="1" fill="none" />
      <path d="M46 54 Q35 65 28 72" stroke="#FF9800" strokeWidth="1" fill="none" />
    </g>
    
    {/* 헬리오스피어 경계 */}
    <circle 
      cx="50" 
      cy="50" 
      r="47" 
      fill="none" 
      stroke="#FFEB3B" 
      strokeWidth="0.5" 
      opacity="0.4"
      strokeDasharray="6,4"
      className="animate-pulse"
      style={{ animationDuration: '10s' }}
    />
    
    {/* 혜성 궤도 - 할리 혜성 */}
    <g className="animate-spin" style={{ animationDuration: '60s', transformOrigin: '50px 50px' }}>
      <ellipse cx="50" cy="50" rx="42" ry="18" fill="none" stroke="#81C784" strokeWidth="0.6" opacity="0.5" strokeDasharray="3,2" />
      <circle cx="92" cy="50" r="1" fill="#81C784" />
      <path d="M92 50 Q87 48 82 50 Q85 52 88 50" stroke="#81C784" strokeWidth="0.8" fill="none" opacity="0.8" />
    </g>
    
    {/* 카이퍼 벨트 */}
    <g className="animate-pulse" style={{ animationDuration: '15s' }} opacity="0.4">
      <circle cx="96" cy="48" r="0.2" fill="#607D8B" />
      <circle cx="97" cy="52" r="0.15" fill="#78909C" />
      <circle cx="95" cy="50" r="0.18" fill="#90A4AE" />
      <circle cx="98" cy="50" r="0.12" fill="#B0BEC5" />
    </g>
    
    {/* 오르트 구름 힌트 */}
    <g className="animate-pulse" style={{ animationDuration: '20s' }} opacity="0.2">
      <circle cx="5" cy="20" r="0.1" fill="#CFD8DC" />
      <circle cx="95" cy="15" r="0.1" fill="#ECEFF1" />
      <circle cx="10" cy="80" r="0.1" fill="#CFD8DC" />
      <circle cx="90" cy="85" r="0.1" fill="#ECEFF1" />
    </g>
    
    {/* 태양계 정보 */}
    <text x="50" y="8" textAnchor="middle" fill="#FFEB3B" fontSize="4" opacity="0.9" fontWeight="bold">
      SOLAR SYSTEM
    </text>
    
    {/* 나이 */}
    <text x="50" y="15" textAnchor="middle" fill="#FF9800" fontSize="3" opacity="0.8">
      4.6 Billion Years
    </text>
    
    {/* 규모 표시 */}
    <text x="50" y="92" textAnchor="middle" fill="#81C784" fontSize="3.5" opacity="0.8">
      ~100 AU diameter
    </text>
    
    {/* 행성 개수 */}
    <text x="8" y="50" fill="#2196F3" fontSize="2.8" opacity="0.7">
      8 Planets
    </text>
    
    {/* 위성 개수 */}
    <text x="92" y="50" fill="#9E9E9E" fontSize="2.5" opacity="0.6" textAnchor="end">
      200+ Moons
    </text>
    
    {/* 생명체 존재 */}
    <text x="8" y="25" fill="#4CAF50" fontSize="2.5" opacity="0.8">
      Life on Earth
    </text>
    
    {/* 골디락스 존 */}
    <text x="92" y="25" fill="#8BC34A" fontSize="2.2" opacity="0.7" textAnchor="end">
      Habitable Zone
    </text>
  </svg>
);

export default StellarSolarSystem;