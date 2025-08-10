import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Saturn({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        <radialGradient id="saturnBody" cx="45%" cy="35%" r="70%">
          <stop offset="0%" stopColor="#FFF8DC" />
          <stop offset="30%" stopColor="#F4E4BC" />
          <stop offset="70%" stopColor="#D2B48C" />
          <stop offset="100%" stopColor="#A0937D" />
        </radialGradient>
        <linearGradient id="ringGrad1" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#F4E4BC" opacity="0.9" />
          <stop offset="50%" stopColor="#E6D3A3" opacity="0.7" />
          <stop offset="100%" stopColor="#C8B99C" opacity="0.5" />
        </linearGradient>
        <linearGradient id="ringGrad2" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#E6D3A3" opacity="0.6" />
          <stop offset="50%" stopColor="#C8B99C" opacity="0.4" />
          <stop offset="100%" stopColor="#A0937D" opacity="0.3" />
        </linearGradient>
        <linearGradient id="ringGrad3" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#DDD6C1" opacity="0.8" />
          <stop offset="50%" stopColor="#C8B99C" opacity="0.6" />
          <stop offset="100%" stopColor="#B8A082" opacity="0.4" />
        </linearGradient>
        <filter id="saturnGlow">
          <feGaussianBlur stdDeviation="4" result="coloredBlur"/>
          <feGaussianBlur stdDeviation="8" result="bigBlur"/>
          <feMerge> 
            <feMergeNode in="bigBlur"/>
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
        <filter id="ringShimmer">
          <feGaussianBlur stdDeviation="1" result="blur"/>
          <feOffset in="blur" dx="1" dy="0" result="offset"/>
        </filter>
      </defs>
      
      {/* 토성의 고리들 (뒤쪽) - 더 화려하게 */}
      <g className="animate-pulse" style={{ animationDuration: '5s' }}>
        <ellipse cx="50" cy="50" rx="47" ry="12" fill="url(#ringGrad1)" opacity="0.3" />
        <ellipse cx="50" cy="50" rx="43" ry="10" fill="url(#ringGrad2)" opacity="0.4" />
        <ellipse cx="50" cy="50" rx="39" ry="8" fill="url(#ringGrad3)" opacity="0.5" />
        <ellipse cx="50" cy="50" rx="35" ry="7" fill="url(#ringGrad1)" opacity="0.6" />
        <ellipse cx="50" cy="50" rx="31" ry="6" fill="url(#ringGrad2)" opacity="0.4" />
        <ellipse cx="50" cy="50" rx="27" ry="5" fill="url(#ringGrad3)" opacity="0.3" />
      </g>
      
      {/* 토성 본체 - 회전 애니메이션 추가 */}
      <circle 
        cx="50" 
        cy="50" 
        r="22" 
        fill="url(#saturnBody)" 
        filter="url(#saturnGlow)"
        className="animate-pulse"
        style={{ animationDuration: '3s' }}
      />
      
      {/* 토성의 대기 띠들 - 회전 효과 */}
      <g className="animate-pulse" style={{ animationDuration: '4s' }}>
        <ellipse cx="50" cy="40" rx="20" ry="2.5" fill="#D2B48C" opacity="0.5" />
        <ellipse cx="50" cy="45" rx="19" ry="2" fill="#C8B99C" opacity="0.6" />
        <ellipse cx="50" cy="50" rx="18" ry="1.8" fill="#B8A082" opacity="0.4" />
        <ellipse cx="50" cy="55" rx="17" ry="2" fill="#A0937D" opacity="0.5" />
        <ellipse cx="50" cy="60" rx="16" ry="2.2" fill="#8B7D6B" opacity="0.4" />
      </g>
      
      {/* 토성의 고리들 (앞쪽 - 더 선명하게) */}
      <g className="animate-pulse" style={{ animationDuration: '7s', animationDelay: '1s' }}>
        <ellipse cx="50" cy="50" rx="47" ry="12" fill="none" stroke="url(#ringGrad1)" strokeWidth="2.5" opacity="0.8" filter="url(#ringShimmer)" />
        <ellipse cx="50" cy="50" rx="43" ry="10" fill="none" stroke="#F4E4BC" strokeWidth="2" opacity="0.9" />
        <ellipse cx="50" cy="50" rx="39" ry="8" fill="none" stroke="url(#ringGrad2)" strokeWidth="1.8" opacity="0.7" />
        <ellipse cx="50" cy="50" rx="35" ry="7" fill="none" stroke="#E6D3A3" strokeWidth="1.5" opacity="0.8" />
        <ellipse cx="50" cy="50" rx="31" ry="6" fill="none" stroke="url(#ringGrad3)" strokeWidth="1.2" opacity="0.6" />
        <ellipse cx="50" cy="50" rx="27" ry="5" fill="none" stroke="#DDD6C1" strokeWidth="1" opacity="0.7" />
      </g>
      
      {/* 카시니 간극 표현 */}
      <ellipse cx="50" cy="50" rx="37" ry="7.5" fill="none" stroke="#000000" strokeWidth="0.8" opacity="0.3" />
      
      {/* 고리의 그림자 효과 */}
      <ellipse cx="50" cy="54" rx="18" ry="5" fill="#8B7D6B" opacity="0.3" />
      <ellipse cx="50" cy="56" rx="15" ry="3" fill="#6D5F4D" opacity="0.2" />
      
      {/* 고리 입자들 - 더 많고 화려하게 */}
      <g className="animate-spin" style={{ animationDuration: '20s', transformOrigin: '50px 50px' }}>
        <circle cx="22" cy="50" r="0.8" fill="#F4E4BC" className="animate-ping" />
        <circle cx="78" cy="50" r="0.6" fill="#E6D3A3" className="animate-ping" style={{ animationDelay: '1s' }} />
        <circle cx="30" cy="47" r="0.7" fill="#C8B99C" className="animate-ping" style={{ animationDelay: '2s' }} />
        <circle cx="70" cy="53" r="0.9" fill="#D2B48C" className="animate-ping" style={{ animationDelay: '3s' }} />
        <circle cx="25" cy="52" r="0.5" fill="#DDD6C1" className="animate-ping" style={{ animationDelay: '4s' }} />
        <circle cx="75" cy="48" r="0.7" fill="#B8A082" className="animate-ping" style={{ animationDelay: '5s' }} />
      </g>
      
      {/* 반대 방향 고리 입자들 */}
      <g className="animate-spin" style={{ animationDuration: '15s', transformOrigin: '50px 50px', animationDirection: 'reverse' }}>
        <circle cx="35" cy="45" r="0.4" fill="#F4E4BC" className="animate-ping" />
        <circle cx="65" cy="55" r="0.6" fill="#E6D3A3" className="animate-ping" style={{ animationDelay: '2s' }} />
        <circle cx="28" cy="54" r="0.5" fill="#C8B99C" className="animate-ping" style={{ animationDelay: '4s' }} />
        <circle cx="72" cy="46" r="0.3" fill="#D2B48C" className="animate-ping" style={{ animationDelay: '6s' }} />
      </g>
      
      {/* 토성의 위성들 (타이탄, 엔셀라두스 등) */}
      <g className="animate-pulse" style={{ animationDuration: '6s' }}>
        <circle cx="15" cy="35" r="1.2" fill="#C8B99C" opacity="0.8" />
        <circle cx="85" cy="65" r="0.9" fill="#E6D3A3" opacity="0.7" />
        <circle cx="20" cy="70" r="0.7" fill="#F4E4BC" opacity="0.6" />
        <circle cx="80" cy="30" r="1" fill="#DDD6C1" opacity="0.8" />
      </g>
      
      {/* 토성의 자기장 효과 */}
      <g className="animate-pulse" style={{ animationDuration: '8s' }} opacity="0.3">
        <ellipse cx="50" cy="50" rx="25" ry="35" fill="none" stroke="#F4E4BC" strokeWidth="0.5" strokeDasharray="3,3" />
        <ellipse cx="50" cy="50" rx="30" ry="40" fill="none" stroke="#E6D3A3" strokeWidth="0.3" strokeDasharray="2,2" />
      </g>
      
      {/* 고리 시스템 정보 */}
      <text x="50" y="10" textAnchor="middle" fill="#F4E4BC" fontSize="3.5" opacity="0.8">
        Ring System
      </text>
      
      {/* 자전 주기 */}
      <text x="50" y="95" textAnchor="middle" fill="#D2B48C" fontSize="3" opacity="0.7">
        10h 33m
      </text>
    </svg>
  );
}