import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function GalaxySpiral({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        {/* 은하 중심 - 초대질량 블랙홀 */}
        <radialGradient id="galaxyCenter" cx="50%" cy="50%" r="40%">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="30%" stopColor="#FFEB3B" />
          <stop offset="60%" stopColor="#FF9800" />
          <stop offset="100%" stopColor="#FF5722" />
        </radialGradient>
        
        {/* 나선팔 그라디언트 - 더 화려하게 */}
        <radialGradient id="spiralArm1" cx="50%" cy="50%" r="90%">
          <stop offset="20%" stopColor="transparent" />
          <stop offset="40%" stopColor="#E1F5FE" opacity="0.8" />
          <stop offset="60%" stopColor="#81D4FA" opacity="0.7" />
          <stop offset="80%" stopColor="#2196F3" opacity="0.6" />
          <stop offset="100%" stopColor="#1565C0" opacity="0.4" />
        </radialGradient>
        
        <radialGradient id="spiralArm2" cx="50%" cy="50%" r="90%">
          <stop offset="20%" stopColor="transparent" />
          <stop offset="40%" stopColor="#F3E5F5" opacity="0.7" />
          <stop offset="60%" stopColor="#CE93D8" opacity="0.6" />
          <stop offset="80%" stopColor="#9C27B0" opacity="0.5" />
          <stop offset="100%" stopColor="#4A148C" opacity="0.3" />
        </radialGradient>
        
        {/* 은하 헤일로 */}
        <radialGradient id="galaxyHalo" cx="50%" cy="50%" r="95%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="70%" stopColor="transparent" />
          <stop offset="90%" stopColor="#E3F2FD" opacity="0.3" />
          <stop offset="100%" stopColor="#BBDEFB" opacity="0.2" />
        </radialGradient>
        
        {/* 강력한 글로우 효과 */}
        <filter id="galaxyGlow">
          <feGaussianBlur stdDeviation="5" result="coloredBlur"/>
          <feGaussianBlur stdDeviation="10" result="bigBlur"/>
          <feMerge> 
            <feMergeNode in="bigBlur"/>
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
        
        {/* 별 형성 지역 효과 */}
        <filter id="starFormation">
          <feGaussianBlur stdDeviation="2" result="blur"/>
          <feOffset in="blur" dx="1" dy="1" result="offset"/>
        </filter>
      </defs>
      
      {/* 은하 헤일로 - 암흑물질 */}
      <circle 
        cx="50" 
        cy="50" 
        r="48" 
        fill="url(#galaxyHalo)" 
        className="animate-pulse"
        style={{ animationDuration: '12s' }}
      />
      
      {/* 나선팔 - 4개의 주요 팔 */}
      <g className="animate-spin" style={{ animationDuration: '40s', transformOrigin: '50px 50px' }}>
        {/* 첫 번째 나선팔 */}
        <path 
          d="M50 50 Q75 25, 90 50 Q75 75, 50 50" 
          fill="url(#spiralArm1)" 
          filter="url(#galaxyGlow)"
        />
        <path 
          d="M50 50 Q70 30, 85 50 Q70 70, 50 50" 
          fill="url(#spiralArm2)" 
          opacity="0.8"
        />
        
        {/* 두 번째 나선팔 */}
        <path 
          d="M50 50 Q25 75, 10 50 Q25 25, 50 50" 
          fill="url(#spiralArm1)" 
          filter="url(#galaxyGlow)"
        />
        <path 
          d="M50 50 Q30 70, 15 50 Q30 30, 50 50" 
          fill="url(#spiralArm2)" 
          opacity="0.8"
        />
        
        {/* 세 번째 나선팔 */}
        <path 
          d="M50 50 Q25 25, 50 10 Q75 25, 50 50" 
          fill="url(#spiralArm1)" 
          opacity="0.7"
        />
        
        {/* 네 번째 나선팔 */}
        <path 
          d="M50 50 Q75 75, 50 90 Q25 75, 50 50" 
          fill="url(#spiralArm1)" 
          opacity="0.7"
        />
      </g>
      
      {/* 반대 방향 나선팔 - 더 세밀한 구조 */}
      <g className="animate-spin" style={{ animationDuration: '50s', transformOrigin: '50px 50px', animationDirection: 'reverse' }}>
        <path 
          d="M50 50 Q65 35, 75 50 Q65 65, 50 50" 
          fill="url(#spiralArm2)" 
          opacity="0.6"
        />
        <path 
          d="M50 50 Q35 65, 25 50 Q35 35, 50 50" 
          fill="url(#spiralArm2)" 
          opacity="0.6"
        />
      </g>
      
      {/* 은하 중심 - 초대질량 블랙홀과 활성핵 */}
      <circle 
        cx="50" 
        cy="50" 
        r="12" 
        fill="url(#galaxyCenter)" 
        filter="url(#galaxyGlow)"
        className="animate-pulse"
        style={{ animationDuration: '2s' }}
      />
      
      {/* 중심 블랙홀 */}
      <circle 
        cx="50" 
        cy="50" 
        r="4" 
        fill="#000000" 
        className="animate-pulse"
        style={{ animationDuration: '1s' }}
      />
      
      {/* 별 형성 지역들 - HII 영역 */}
      <g className="animate-pulse" style={{ animationDuration: '4s' }}>
        <circle cx="30" cy="30" r="2.5" fill="#FF69B4" opacity="0.8" filter="url(#starFormation)" />
        <circle cx="70" cy="30" r="2" fill="#FF1493" opacity="0.7" filter="url(#starFormation)" />
        <circle cx="70" cy="70" r="2.8" fill="#FF69B4" opacity="0.9" filter="url(#starFormation)" />
        <circle cx="30" cy="70" r="2.2" fill="#FF1493" opacity="0.6" filter="url(#starFormation)" />
        <circle cx="25" cy="50" r="1.8" fill="#FF69B4" opacity="0.7" filter="url(#starFormation)" />
        <circle cx="75" cy="50" r="2.3" fill="#FF1493" opacity="0.8" filter="url(#starFormation)" />
      </g>
      
      {/* 별들 - 다양한 종류와 크기 */}
      <g className="animate-pulse" style={{ animationDuration: '3s' }}>
        {/* O형 별 (파란색 거성) */}
        <circle cx="35" cy="25" r="1.2" fill="#87CEEB" className="animate-ping" />
        <circle cx="65" cy="35" r="1" fill="#4169E1" className="animate-ping" style={{ animationDelay: '0.5s' }} />
        <circle cx="75" cy="65" r="1.3" fill="#0000FF" className="animate-ping" style={{ animationDelay: '1s' }} />
        
        {/* G형 별 (태양형) */}
        <circle cx="40" cy="60" r="0.9" fill="#FFFF00" className="animate-ping" style={{ animationDelay: '1.5s' }} />
        <circle cx="60" cy="25" r="0.8" fill="#FFA500" className="animate-ping" style={{ animationDelay: '2s' }} />
        
        {/* M형 별 (적색왜성) */}
        <circle cx="25" cy="65" r="0.6" fill="#FF4500" className="animate-ping" style={{ animationDelay: '2.5s' }} />
        <circle cx="75" cy="35" r="0.7" fill="#FF6347" className="animate-ping" style={{ animationDelay: '0.3s' }} />
      </g>
      
      {/* 추가 별들 - 더 많은 별 */}
      <g className="animate-pulse" style={{ animationDuration: '5s', animationDelay: '1s' }}>
        <circle cx="20" cy="40" r="0.5" fill="#FFFFFF" className="animate-ping" />
        <circle cx="80" cy="60" r="0.6" fill="#E1F5FE" className="animate-ping" style={{ animationDelay: '1s' }} />
        <circle cx="45" cy="20" r="0.4" fill="#BBDEFB" className="animate-ping" style={{ animationDelay: '2s' }} />
        <circle cx="55" cy="80" r="0.7" fill="#90CAF9" className="animate-ping" style={{ animationDelay: '3s' }} />
        <circle cx="15" cy="25" r="0.3" fill="#64B5F6" className="animate-ping" style={{ animationDelay: '4s' }} />
        <circle cx="85" cy="75" r="0.5" fill="#42A5F5" className="animate-ping" style={{ animationDelay: '0.8s' }} />
      </g>
      
      {/* 구상성단들 */}
      <g className="animate-pulse" style={{ animationDuration: '8s' }}>
        <circle cx="15" cy="15" r="1.5" fill="#FFD700" opacity="0.7" />
        <circle cx="85" cy="15" r="1.3" fill="#FFA500" opacity="0.6" />
        <circle cx="15" cy="85" r="1.4" fill="#FF8C00" opacity="0.8" />
        <circle cx="85" cy="85" r="1.2" fill="#FFD700" opacity="0.5" />
      </g>
      
      {/* 은하 회전 속도 표시 */}
      <g className="animate-pulse" style={{ animationDuration: '6s' }} opacity="0.4">
        <circle cx="50" cy="50" r="20" fill="none" stroke="#81D4FA" strokeWidth="0.5" strokeDasharray="2,2" />
        <circle cx="50" cy="50" r="30" fill="none" stroke="#2196F3" strokeWidth="0.4" strokeDasharray="3,3" />
        <circle cx="50" cy="50" r="40" fill="none" stroke="#1565C0" strokeWidth="0.3" strokeDasharray="4,4" />
      </g>
      
      {/* 은하 분류 */}
      <text x="50" y="8" textAnchor="middle" fill="#81D4FA" fontSize="4" opacity="0.9">
        Spiral Galaxy
      </text>
      
      {/* 허블 분류 */}
      <text x="50" y="95" textAnchor="middle" fill="#2196F3" fontSize="3.5" opacity="0.8">
        Type: Sb/Sc
      </text>
      
      {/* 은하 질량 */}
      <text x="8" y="50" fill="#1565C0" fontSize="2.5" opacity="0.7">
        M ~ 10¹² M☉
      </text>
      
      {/* 별 개수 */}
      <text x="92" y="50" fill="#42A5F5" fontSize="2.5" opacity="0.6" textAnchor="end">
        ~10¹¹ stars
      </text>
    </svg>
  );
}