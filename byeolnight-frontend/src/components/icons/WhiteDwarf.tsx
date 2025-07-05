import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function WhiteDwarf({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        {/* 백색왜성 핵심 - 다이아몬드 같은 결정 구조 */}
        <radialGradient id="whiteDwarfCore" cx="50%" cy="50%" r="60%">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="30%" stopColor="#F8F9FA" />
          <stop offset="60%" stopColor="#E3F2FD" />
          <stop offset="100%" stopColor="#BBDEFB" />
        </radialGradient>
        
        {/* 다이아몬드 결정화 그라디언트 */}
        <radialGradient id="diamondCore" cx="50%" cy="50%" r="40%">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="50%" stopColor="#E1F5FE" />
          <stop offset="100%" stopColor="#B3E5FC" />
        </radialGradient>
        
        {/* 백색왜성 외부 글로우 */}
        <radialGradient id="whiteDwarfGlow" cx="50%" cy="50%" r="90%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="50%" stopColor="transparent" />
          <stop offset="80%" stopColor="#FFFFFF" opacity="0.6" />
          <stop offset="100%" stopColor="#E3F2FD" opacity="0.8" />
        </radialGradient>
        
        {/* 강력한 다이아몬드 글로우 */}
        <filter id="diamondGlow">
          <feGaussianBlur stdDeviation="4" result="coloredBlur"/>
          <feGaussianBlur stdDeviation="8" result="bigBlur"/>
          <feMerge> 
            <feMergeNode in="bigBlur"/>
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
        
        {/* 결정 반사 효과 */}
        <filter id="crystalline">
          <feGaussianBlur stdDeviation="1" result="blur"/>
          <feOffset in="blur" dx="1" dy="1" result="offset"/>
          <feFlood floodColor="#FFFFFF" floodOpacity="0.8"/>
          <feComposite in2="offset" operator="in"/>
        </filter>
      </defs>
      
      {/* 백색왜성 외부 글로우 - 우주의 보석 */}
      <circle 
        cx="50" 
        cy="50" 
        r="45" 
        fill="url(#whiteDwarfGlow)" 
        className="animate-pulse" 
        style={{ animationDuration: '4s' }} 
      />
      
      {/* 중간 글로우 레이어 */}
      <circle 
        cx="50" 
        cy="50" 
        r="35" 
        fill="url(#whiteDwarfGlow)" 
        className="animate-pulse" 
        style={{ animationDuration: '3s', animationDelay: '0.5s' }} 
        opacity="0.7"
      />
      
      {/* 백색왜성 본체 - 다이아몬드 코어 */}
      <circle 
        cx="50" 
        cy="50" 
        r="20" 
        fill="url(#whiteDwarfCore)" 
        filter="url(#diamondGlow)"
        className="animate-pulse"
        style={{ animationDuration: '2s' }}
      />
      
      {/* 내부 다이아몬드 결정 */}
      <circle 
        cx="50" 
        cy="50" 
        r="12" 
        fill="url(#diamondCore)" 
        className="animate-pulse"
        style={{ animationDuration: '1.5s' }}
      />
      
      {/* 다이아몬드 결정화 패턴 - 8방향 대칭 */}
      <g className="animate-pulse" style={{ animationDuration: '3s' }}>
        {/* 주요 결정면 */}
        <polygon points="50,35 60,45 50,55 40,45" fill="#FFFFFF" opacity="0.9" filter="url(#crystalline)" />
        <polygon points="50,45 60,55 50,65 40,55" fill="#F8F9FA" opacity="0.8" />
        <polygon points="35,50 45,40 55,50 45,60" fill="#E3F2FD" opacity="0.7" />
        <polygon points="45,50 55,40 65,50 55,60" fill="#BBDEFB" opacity="0.6" />
        
        {/* 보조 결정면 */}
        <polygon points="45,40 50,35 55,40 50,45" fill="#FFFFFF" opacity="0.8" />
        <polygon points="45,60 50,55 55,60 50,65" fill="#FFFFFF" opacity="0.8" />
        <polygon points="40,45 35,50 40,55 45,50" fill="#F8F9FA" opacity="0.7" />
        <polygon points="60,45 65,50 60,55 55,50" fill="#F8F9FA" opacity="0.7" />
      </g>
      
      {/* 결정 격자 구조 */}
      <g className="animate-pulse" style={{ animationDuration: '4s', animationDelay: '1s' }} opacity="0.6">
        <line x1="35" y1="35" x2="65" y2="65" stroke="#FFFFFF" strokeWidth="0.5" />
        <line x1="65" y1="35" x2="35" y2="65" stroke="#FFFFFF" strokeWidth="0.5" />
        <line x1="50" y1="30" x2="50" y2="70" stroke="#E3F2FD" strokeWidth="0.8" />
        <line x1="30" y1="50" x2="70" y2="50" stroke="#E3F2FD" strokeWidth="0.8" />
      </g>
      
      {/* 고온 복사 - 다중 레이어 */}
      <g className="animate-pulse" style={{ animationDuration: '2.5s' }}>
        <circle cx="50" cy="50" r="25" fill="none" stroke="#FFFFFF" strokeWidth="1" opacity="0.7" />
        <circle cx="50" cy="50" r="30" fill="none" stroke="#E3F2FD" strokeWidth="0.8" opacity="0.6" />
        <circle cx="50" cy="50" r="35" fill="none" stroke="#BBDEFB" strokeWidth="0.6" opacity="0.5" />
      </g>
      
      {/* 추가 복사 링 */}
      <g className="animate-pulse" style={{ animationDuration: '3.5s', animationDelay: '1s' }}>
        <circle cx="50" cy="50" r="28" fill="none" stroke="#F8F9FA" strokeWidth="0.5" opacity="0.4" />
        <circle cx="50" cy="50" r="33" fill="none" stroke="#E1F5FE" strokeWidth="0.4" opacity="0.3" />
        <circle cx="50" cy="50" r="38" fill="none" stroke="#B3E5FC" strokeWidth="0.3" opacity="0.2" />
      </g>
      
      {/* 중력장 효과 - 시공간 왜곡 */}
      <g className="animate-ping" style={{ animationDuration: '4s' }}>
        <circle cx="25" cy="25" r="1.2" fill="#FFFFFF" />
        <circle cx="75" cy="25" r="1" fill="#E3F2FD" />
        <circle cx="75" cy="75" r="1.3" fill="#FFFFFF" />
        <circle cx="25" cy="75" r="0.9" fill="#E3F2FD" />
      </g>
      
      {/* 추가 중력 효과 */}
      <g className="animate-ping" style={{ animationDuration: '5s', animationDelay: '1s' }}>
        <circle cx="15" cy="50" r="0.8" fill="#F8F9FA" />
        <circle cx="85" cy="50" r="0.7" fill="#E1F5FE" />
        <circle cx="50" cy="15" r="0.9" fill="#B3E5FC" />
        <circle cx="50" cy="85" r="0.6" fill="#BBDEFB" />
      </g>
      
      {/* 다이아몬드 반짝임 효과 */}
      <g className="animate-ping" style={{ animationDuration: '1.5s' }}>
        <circle cx="45" cy="40" r="0.5" fill="#FFFFFF" />
        <circle cx="55" cy="45" r="0.4" fill="#FFFFFF" />
        <circle cx="55" cy="55" r="0.6" fill="#FFFFFF" />
        <circle cx="45" cy="60" r="0.3" fill="#FFFFFF" />
      </g>
      
      {/* 별의 마지막 빛 - 중심 하이라이트 */}
      <circle 
        cx="50" 
        cy="50" 
        r="3" 
        fill="#FFFFFF" 
        filter="url(#diamondGlow)"
        className="animate-pulse"
        style={{ animationDuration: '1s' }}
      />
      
      {/* 온도 표시 */}
      <text x="50" y="10" textAnchor="middle" fill="#FFFFFF" fontSize="3.5" opacity="0.8">
        T ~ 5,000K
      </text>
      
      {/* 밀도 표시 */}
      <text x="50" y="95" textAnchor="middle" fill="#E3F2FD" fontSize="3" opacity="0.7">
        ρ ~ 10⁶ g/cm³
      </text>
    </svg>
  );
}