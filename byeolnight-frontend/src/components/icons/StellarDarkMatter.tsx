import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

const StellarDarkMatter = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      {/* 암흑물질 핵심 - 절대적 어둠 */}
      <radialGradient id="darkMatterCore" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#000000" />
        <stop offset="25%" stopColor="#0A0A0A" />
        <stop offset="50%" stopColor="#1A0033" />
        <stop offset="75%" stopColor="#2D1B69" />
        <stop offset="100%" stopColor="#4A148C" />
      </radialGradient>
      
      {/* 암흑물질 헤일로 - 다층 구조 */}
      <radialGradient id="darkMatterHalo1" cx="50%" cy="50%" r="95%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="40%" stopColor="#2D1B69" opacity="0.4" />
        <stop offset="70%" stopColor="#4A148C" opacity="0.6" />
        <stop offset="90%" stopColor="#6A1B9A" opacity="0.8" />
        <stop offset="100%" stopColor="#000000" opacity="0.9" />
      </radialGradient>
      
      <radialGradient id="darkMatterHalo2" cx="50%" cy="50%" r="85%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="50%" stopColor="#4A148C" opacity="0.3" />
        <stop offset="80%" stopColor="#6A1B9A" opacity="0.5" />
        <stop offset="100%" stopColor="#8E24AA" opacity="0.7" />
      </radialGradient>
      
      <radialGradient id="darkMatterHalo3" cx="50%" cy="50%" r="75%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="60%" stopColor="#6A1B9A" opacity="0.4" />
        <stop offset="90%" stopColor="#8E24AA" opacity="0.6" />
        <stop offset="100%" stopColor="#AB47BC" opacity="0.5" />
      </radialGradient>
      
      {/* 중력 렌즈 효과 */}
      <radialGradient id="gravitationalLens" cx="50%" cy="50%" r="80%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="60%" stopColor="#8E24AA" opacity="0.3" />
        <stop offset="85%" stopColor="#7B1FA2" opacity="0.5" />
        <stop offset="100%" stopColor="#4A148C" opacity="0.7" />
      </radialGradient>
      
      {/* 초강력 암흑 글로우 */}
      <filter id="darkGlow">
        <feGaussianBlur stdDeviation="8" result="coloredBlur"/>
        <feGaussianBlur stdDeviation="16" result="bigBlur"/>
        <feGaussianBlur stdDeviation="24" result="massiveBlur"/>
        <feMerge> 
          <feMergeNode in="massiveBlur"/>
          <feMergeNode in="bigBlur"/>
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
      
      {/* 보이지 않는 물질 효과 */}
      <filter id="invisibleMatter">
        <feTurbulence baseFrequency="0.05" numOctaves="4" result="noise"/>
        <feDisplacementMap in="SourceGraphic" in2="noise" scale="4"/>
        <feGaussianBlur stdDeviation="2" result="blur"/>
      </filter>
      
      {/* 중력 왜곡 효과 */}
      <filter id="gravitationalWarp">
        <feTurbulence baseFrequency="0.02" numOctaves="5" result="warp"/>
        <feDisplacementMap in="SourceGraphic" in2="warp" scale="6"/>
      </filter>
      
      {/* 암흑 에너지 패턴 */}
      <pattern id="darkPattern" x="0" y="0" width="12" height="12" patternUnits="userSpaceOnUse">
        <circle cx="6" cy="6" r="2" fill="#2D1B69" opacity="0.4"/>
        <circle cx="6" cy="6" r="1" fill="#4A148C" opacity="0.6"/>
        <circle cx="6" cy="6" r="0.5" fill="#6A1B9A" opacity="0.8"/>
      </pattern>
      
      {/* WIMP 입자 패턴 */}
      <pattern id="wimpPattern" x="0" y="0" width="8" height="8" patternUnits="userSpaceOnUse">
        <circle cx="4" cy="4" r="0.8" fill="#8E24AA" opacity="0.5"/>
        <circle cx="4" cy="4" r="0.4" fill="#AB47BC" opacity="0.7"/>
      </pattern>
    </defs>
    
    {/* 우주 배경 - 암흑물질이 지배하는 공간 */}
    <rect x="0" y="0" width="100" height="100" fill="#000000" />
    
    {/* 암흑물질 헤일로 - 다층 구조 */}
    <circle 
      cx="50" 
      cy="50" 
      r="49" 
      fill="url(#darkMatterHalo1)" 
      className="animate-pulse"
      style={{ animationDuration: '12s' }}
    />
    
    <circle 
      cx="50" 
      cy="50" 
      r="45" 
      fill="url(#darkMatterHalo2)" 
      className="animate-pulse"
      style={{ animationDuration: '10s', animationDelay: '2s' }}
    />
    
    <circle 
      cx="50" 
      cy="50" 
      r="40" 
      fill="url(#darkMatterHalo3)" 
      className="animate-pulse"
      style={{ animationDuration: '8s', animationDelay: '4s' }}
    />
    
    {/* 중력 렌즈 효과 */}
    <circle 
      cx="50" 
      cy="50" 
      r="42" 
      fill="url(#gravitationalLens)" 
      className="animate-pulse"
      style={{ animationDuration: '9s', animationDelay: '1s' }}
      filter="url(#gravitationalWarp)"
    />
    
    {/* 암흑물질 필라멘트 구조 - 우주 거대구조 */}
    <g className="animate-pulse" style={{ animationDuration: '15s' }} opacity="0.6">
      <path d="M5 5 Q25 25 50 15 Q75 5 95 25 Q85 50 95 75 Q75 95 50 85 Q25 95 5 75 Q15 50 5 25 Q25 15 5 5" 
            fill="none" stroke="#2D1B69" strokeWidth="2.5" strokeDasharray="8,4" filter="url(#invisibleMatter)" />
      <path d="M10 10 Q30 30 50 20 Q70 10 90 30 Q80 50 90 70 Q70 90 50 80 Q30 90 10 70 Q20 50 10 30 Q30 20 10 10" 
            fill="none" stroke="#4A148C" strokeWidth="2" strokeDasharray="6,3" opacity="0.8" />
      <path d="M15 15 Q35 35 50 25 Q65 15 85 35 Q75 50 85 65 Q65 85 50 75 Q35 85 15 65 Q25 50 15 35 Q35 25 15 15" 
            fill="none" stroke="#6A1B9A" strokeWidth="1.5" strokeDasharray="4,2" opacity="0.7" />
    </g>
    
    {/* 추가 필라멘트 - 다른 방향 */}
    <g className="animate-pulse" style={{ animationDuration: '18s', animationDelay: '3s' }} opacity="0.5">
      <path d="M5 95 Q25 75 50 85 Q75 95 95 75 Q85 50 95 25 Q75 5 50 15 Q25 5 5 25 Q15 50 5 75 Q25 85 5 95" 
            fill="none" stroke="#8E24AA" strokeWidth="2" strokeDasharray="10,5" filter="url(#invisibleMatter)" />
      <path d="M10 90 Q30 70 50 80 Q70 90 90 70 Q80 50 90 30 Q70 10 50 20 Q30 10 10 30 Q20 50 10 70 Q30 80 10 90" 
            fill="none" stroke="#AB47BC" strokeWidth="1.8" strokeDasharray="8,4" opacity="0.8" />
    </g>
    
    {/* 보이지 않는 물질의 윤곽들 - 더 복잡한 구조 */}
    <g className="animate-pulse" style={{ animationDuration: '8s' }} opacity="0.7">
      <path 
        d="M12 20 Q28 8 50 20 Q72 32 88 20 Q92 50 88 80 Q72 92 50 80 Q28 68 12 80 Q8 50 12 20" 
        fill="none" 
        stroke="#4A148C" 
        strokeWidth="2.5" 
        strokeDasharray="8,6"
        filter="url(#invisibleMatter)"
      />
      <path 
        d="M18 25 Q32 13 50 25 Q68 37 82 25 Q86 50 82 75 Q68 87 50 75 Q32 63 18 75 Q14 50 18 25" 
        fill="none" 
        stroke="#6A1B9A" 
        strokeWidth="2.2" 
        strokeDasharray="7,5"
        opacity="0.9"
      />
      <path 
        d="M22 30 Q36 18 50 30 Q64 42 78 30 Q82 50 78 70 Q64 82 50 70 Q36 58 22 70 Q18 50 22 30" 
        fill="none" 
        stroke="#8E24AA" 
        strokeWidth="2" 
        strokeDasharray="6,4"
        opacity="0.8"
      />
      <path 
        d="M26 35 Q40 23 50 35 Q60 47 74 35 Q78 50 74 65 Q60 77 50 65 Q40 53 26 65 Q22 50 26 35" 
        fill="none" 
        stroke="#AB47BC" 
        strokeWidth="1.8" 
        strokeDasharray="5,3"
        opacity="0.7"
      />
      <path 
        d="M30 40 Q42 28 50 40 Q58 52 70 40 Q74 50 70 60 Q58 72 50 60 Q42 48 30 60 Q26 50 30 40" 
        fill="none" 
        stroke="#CE93D8" 
        strokeWidth="1.5" 
        strokeDasharray="4,2"
        opacity="0.6"
      />
    </g>
    
    {/* 추가 암흑물질 구조 - 타원형 */}
    <g className="animate-pulse" style={{ animationDuration: '11s', animationDelay: '2s' }} opacity="0.6">
      <ellipse cx="50" cy="50" rx="38" ry="22" fill="none" stroke="#2D1B69" strokeWidth="2" strokeDasharray="12,8" />
      <ellipse cx="50" cy="50" rx="22" ry="38" fill="none" stroke="#4A148C" strokeWidth="1.8" strokeDasharray="10,6" />
      <ellipse cx="50" cy="50" rx="32" ry="18" fill="none" stroke="#6A1B9A" strokeWidth="1.5" strokeDasharray="8,4" />
      <ellipse cx="50" cy="50" rx="18" ry="32" fill="none" stroke="#8E24AA" strokeWidth="1.2" strokeDasharray="6,3" />
    </g>
    
    {/* 중심 암흑 코어 - 절대적 어둠 */}
    <circle 
      cx="50" 
      cy="50" 
      r="22" 
      fill="url(#darkMatterCore)" 
      filter="url(#darkGlow)"
      className="animate-pulse"
      style={{ animationDuration: '6s' }}
    />
    
    {/* 내부 암흑 코어 */}
    <circle 
      cx="50" 
      cy="50" 
      r="16" 
      fill="#000000" 
      className="animate-pulse"
      style={{ animationDuration: '4s' }}
    />
    
    {/* 최중심 특이점 */}
    <circle 
      cx="50" 
      cy="50" 
      r="8" 
      fill="#000000" 
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    
    {/* 중력 렌즈 효과로 왜곡된 은하들 - 더 많고 화려하게 */}
    <g className="animate-spin" style={{ animationDuration: '30s', transformOrigin: '50px 50px' }}>
      <ellipse cx="15" cy="10" rx="5" ry="2" fill="#4A148C" opacity="0.9" className="animate-pulse" transform="rotate(30 15 10)" filter="url(#gravitationalWarp)" />
      <ellipse cx="85" cy="15" rx="4.5" ry="1.8" fill="#6A1B9A" opacity="0.8" className="animate-pulse" transform="rotate(-45 85 15)" style={{ animationDelay: '2s' }} />
      <ellipse cx="90" cy="85" rx="5.5" ry="2.2" fill="#8E24AA" opacity="0.9" className="animate-pulse" transform="rotate(60 90 85)" style={{ animationDelay: '4s' }} />
      <ellipse cx="10" cy="90" rx="4.8" ry="1.9" fill="#AB47BC" opacity="0.7" className="animate-pulse" transform="rotate(-30 10 90)" style={{ animationDelay: '6s' }} />
      <ellipse cx="20" cy="50" rx="4" ry="1.6" fill="#2D1B69" opacity="0.6" className="animate-pulse" transform="rotate(90 20 50)" style={{ animationDelay: '8s' }} />
      <ellipse cx="80" cy="55" rx="4.2" ry="1.7" fill="#4A148C" opacity="0.8" className="animate-pulse" transform="rotate(-60 80 55)" style={{ animationDelay: '10s' }} />
      <ellipse cx="50" cy="15" rx="3.8" ry="1.5" fill="#6A1B9A" opacity="0.7" className="animate-pulse" transform="rotate(45 50 15)" style={{ animationDelay: '12s' }} />
      <ellipse cx="50" cy="85" rx="4.4" ry="1.8" fill="#8E24AA" opacity="0.8" className="animate-pulse" transform="rotate(-45 50 85)" style={{ animationDelay: '14s' }} />
    </g>
    
    {/* 반대 방향 은하 회전 */}
    <g className="animate-spin" style={{ animationDuration: '40s', transformOrigin: '50px 50px', animationDirection: 'reverse' }}>
      <ellipse cx="25" cy="25" rx="3.5" ry="1.4" fill="#6A1B9A" opacity="0.7" className="animate-pulse" transform="rotate(60 25 25)" />
      <ellipse cx="75" cy="25" rx="3.8" ry="1.5" fill="#8E24AA" opacity="0.8" className="animate-pulse" transform="rotate(-30 75 25)" style={{ animationDelay: '3s' }} />
      <ellipse cx="75" cy="75" rx="4.2" ry="1.7" fill="#AB47BC" opacity="0.9" className="animate-pulse" transform="rotate(45 75 75)" style={{ animationDelay: '6s' }} />
      <ellipse cx="25" cy="75" rx="3.6" ry="1.4" fill="#CE93D8" opacity="0.6" className="animate-pulse" transform="rotate(-60 25 75)" style={{ animationDelay: '9s' }} />
      <ellipse cx="35" cy="15" rx="3" ry="1.2" fill="#E1BEE7" opacity="0.5" className="animate-pulse" transform="rotate(30 35 15)" style={{ animationDelay: '12s' }} />
      <ellipse cx="65" cy="85" rx="3.4" ry="1.3" fill="#F3E5F5" opacity="0.7" className="animate-pulse" transform="rotate(-45 65 85)" style={{ animationDelay: '15s' }} />
    </g>
    
    {/* 암흑물질 입자들 (WIMP, 액시온, 스테릴 중성미자) - 더 많고 다양하게 */}
    <g className="animate-pulse" style={{ animationDuration: '5s' }}>
      <circle cx="25" cy="20" r="1.2" fill="#4A148C" className="animate-ping" filter="url(#invisibleMatter)" />
      <circle cx="75" cy="25" r="1" fill="#6A1B9A" className="animate-ping" style={{ animationDelay: '0.5s' }} />
      <circle cx="80" cy="75" r="1.4" fill="#8E24AA" className="animate-ping" style={{ animationDelay: '1s' }} filter="url(#invisibleMatter)" />
      <circle cx="20" cy="80" r="1.1" fill="#AB47BC" className="animate-ping" style={{ animationDelay: '1.5s' }} />
      <circle cx="8" cy="45" r="0.9" fill="#2D1B69" className="animate-ping" style={{ animationDelay: '2s' }} />
      <circle cx="92" cy="40" r="1.3" fill="#4A148C" className="animate-ping" style={{ animationDelay: '2.5s' }} filter="url(#invisibleMatter)" />
      <circle cx="45" cy="8" r="0.8" fill="#6A1B9A" className="animate-ping" style={{ animationDelay: '3s' }} />
      <circle cx="55" cy="92" r="1" fill="#8E24AA" className="animate-ping" style={{ animationDelay: '3.5s' }} />
      <circle cx="15" cy="60" r="0.7" fill="#AB47BC" className="animate-ping" style={{ animationDelay: '4s' }} />
      <circle cx="85" cy="35" r="1.1" fill="#CE93D8" className="animate-ping" style={{ animationDelay: '4.5s' }} />
    </g>
    
    {/* 추가 암흑물질 입자 - 다른 패턴 */}
    <g className="animate-pulse" style={{ animationDuration: '7s', animationDelay: '1s' }}>
      <circle cx="35" cy="15" r="0.6" fill="#2D1B69" className="animate-ping" />
      <circle cx="65" cy="20" r="0.8" fill="#4A148C" className="animate-ping" style={{ animationDelay: '1s' }} />
      <circle cx="85" cy="65" r="0.7" fill="#6A1B9A" className="animate-ping" style={{ animationDelay: '2s' }} />
      <circle cx="15" cy="70" r="0.9" fill="#8E24AA" className="animate-ping" style={{ animationDelay: '3s' }} />
      <circle cx="30" cy="85" r="0.5" fill="#AB47BC" className="animate-ping" style={{ animationDelay: '4s' }} />
      <circle cx="70" cy="10" r="0.8" fill="#CE93D8" className="animate-ping" style={{ animationDelay: '5s' }} />
      <circle cx="10" cy="30" r="0.6" fill="#E1BEE7" className="animate-ping" style={{ animationDelay: '6s' }} />
      <circle cx="90" cy="70" r="0.7" fill="#F3E5F5" className="animate-ping" style={{ animationDelay: '0.5s' }} />
    </g>
    
    {/* 중력적 영향 표시 - 다중 레이어 */}
    <g className="animate-ping" style={{ animationDuration: '8s' }}>
      <circle cx="50" cy="50" r="25" fill="none" stroke="#2D1B69" strokeWidth="1" opacity="0.5" />
      <circle cx="50" cy="50" r="30" fill="none" stroke="#4A148C" strokeWidth="0.8" opacity="0.4" />
      <circle cx="50" cy="50" r="35" fill="none" stroke="#6A1B9A" strokeWidth="0.6" opacity="0.3" />
      <circle cx="50" cy="50" r="40" fill="none" stroke="#8E24AA" strokeWidth="0.4" opacity="0.2" />
      <circle cx="50" cy="50" r="45" fill="none" stroke="#AB47BC" strokeWidth="0.2" opacity="0.1" />
    </g>
    
    {/* 추가 중력 효과 */}
    <g className="animate-ping" style={{ animationDuration: '10s', animationDelay: '2s' }}>
      <circle cx="50" cy="50" r="28" fill="none" stroke="#CE93D8" strokeWidth="0.7" opacity="0.4" />
      <circle cx="50" cy="50" r="33" fill="none" stroke="#E1BEE7" strokeWidth="0.5" opacity="0.3" />
      <circle cx="50" cy="50" r="38" fill="none" stroke="#F3E5F5" strokeWidth="0.3" opacity="0.2" />
      <circle cx="50" cy="50" r="43" fill="none" stroke="#FCE4EC" strokeWidth="0.2" opacity="0.1" />
    </g>
    
    {/* 암흑물질 비율 */}
    <text x="50" y="6" textAnchor="middle" fill="#8E24AA" fontSize="4" opacity="0.9" fontWeight="bold">
      Dark Matter
    </text>
    <text x="50" y="12" textAnchor="middle" fill="#6A1B9A" fontSize="3.5" opacity="0.8">
      ~27% of Universe
    </text>
    
    {/* 질량 표시 */}
    <text x="50" y="94" textAnchor="middle" fill="#AB47BC" fontSize="3.5" opacity="0.9">
      5× Ordinary Matter
    </text>
    
    {/* 상호작용 표시 */}
    <text x="5" y="20" fill="#2D1B69" fontSize="2.5" opacity="0.8">
      Weakly
    </text>
    <text x="5" y="25" fill="#4A148C" fontSize="2.5" opacity="0.8">
      Interacting
    </text>
    
    {/* 검출 불가 표시 */}
    <text x="95" y="20" fill="#6A1B9A" fontSize="2.5" opacity="0.7" textAnchor="end">
      Invisible
    </text>
    <text x="95" y="25" fill="#8E24AA" fontSize="2.5" opacity="0.7" textAnchor="end">
      to Light
    </text>
    
    {/* 후보 입자들 */}
    <text x="5" y="80" fill="#AB47BC" fontSize="2.2" opacity="0.7">
      WIMPs
    </text>
    <text x="5" y="85" fill="#CE93D8" fontSize="2.2" opacity="0.7">
      Axions
    </text>
    <text x="5" y="90" fill="#E1BEE7" fontSize="2.2" opacity="0.7">
      Sterile ν
    </text>
    
    {/* 구조 형성 */}
    <text x="95" y="80" fill="#8E24AA" fontSize="2.2" opacity="0.6" textAnchor="end">
      Structure
    </text>
    <text x="95" y="85" fill="#AB47BC" fontSize="2.2" opacity="0.6" textAnchor="end">
      Formation
    </text>
    
    {/* 은하 회전 곡선 */}
    <text x="50" y="88" textAnchor="middle" fill="#6A1B9A" fontSize="2.8" opacity="0.8">
      Galaxy Rotation Curves
    </text>
  </svg>
);

export default StellarDarkMatter;