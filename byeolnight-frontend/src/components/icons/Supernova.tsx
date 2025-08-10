import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Supernova({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        {/* 중성자별 잔해 */}
        <radialGradient id="neutronStar" cx="50%" cy="50%" r="30%">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="40%" stopColor="#E3F2FD" />
          <stop offset="80%" stopColor="#2196F3" />
          <stop offset="100%" stopColor="#1565C0" />
        </radialGradient>
        
        {/* 폭발 핵심 */}
        <radialGradient id="explosionCore" cx="50%" cy="50%" r="40%">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="20%" stopColor="#FFEB3B" />
          <stop offset="50%" stopColor="#FF9800" />
          <stop offset="80%" stopColor="#FF5722" />
          <stop offset="100%" stopColor="#E91E63" />
        </radialGradient>
        
        {/* 첫 번째 충격파 */}
        <radialGradient id="shockWave1" cx="50%" cy="50%" r="70%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="30%" stopColor="#FF5722" opacity="0.9" />
          <stop offset="60%" stopColor="#E91E63" opacity="0.7" />
          <stop offset="90%" stopColor="#9C27B0" opacity="0.5" />
          <stop offset="100%" stopColor="#673AB7" opacity="0.3" />
        </radialGradient>
        
        {/* 두 번째 충격파 */}
        <radialGradient id="shockWave2" cx="50%" cy="50%" r="85%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="40%" stopColor="#9C27B0" opacity="0.8" />
          <stop offset="70%" stopColor="#673AB7" opacity="0.6" />
          <stop offset="90%" stopColor="#3F51B5" opacity="0.4" />
          <stop offset="100%" stopColor="#1976D2" opacity="0.2" />
        </radialGradient>
        
        {/* 세 번째 충격파 */}
        <radialGradient id="shockWave3" cx="50%" cy="50%" r="95%">
          <stop offset="0%" stopColor="transparent" />
          <stop offset="50%" stopColor="#3F51B5" opacity="0.7" />
          <stop offset="80%" stopColor="#1976D2" opacity="0.5" />
          <stop offset="100%" stopColor="#0D47A1" opacity="0.3" />
        </radialGradient>
        
        {/* 초강력 폭발 글로우 */}
        <filter id="supernovaGlow">
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
        
        {/* 폭발 깜빡임 효과 */}
        <filter id="explosionFlicker">
          <feTurbulence baseFrequency="0.9" numOctaves="4" result="noise" seed="2"/>
          <feDisplacementMap in="SourceGraphic" in2="noise" scale="3"/>
        </filter>
        
        {/* 충격파 왜곡 */}
        <filter id="shockWave">
          <feTurbulence baseFrequency="0.3" numOctaves="3" result="wave"/>
          <feDisplacementMap in="SourceGraphic" in2="wave" scale="4"/>
        </filter>
        
        {/* 중원소 분산 효과 */}
        <filter id="elementalDispersion">
          <feTurbulence baseFrequency="0.5" numOctaves="2" result="disperse"/>
          <feDisplacementMap in="SourceGraphic" in2="disperse" scale="2"/>
        </filter>
      </defs>
      
      {/* 최외곽 충격파 - 성간매질과의 충돌 */}
      <g className="animate-ping" style={{ animationDuration: '4s' }}>
        <circle cx="50" cy="50" r="49" fill="none" stroke="#0D47A1" strokeWidth="6" opacity="0.4" filter="url(#shockWave)" />
        <circle cx="50" cy="50" r="47" fill="none" stroke="#1976D2" strokeWidth="5" opacity="0.5" />
        <circle cx="50" cy="50" r="45" fill="none" stroke="#3F51B5" strokeWidth="4" opacity="0.6" />
        <circle cx="50" cy="50" r="43" fill="none" stroke="#673AB7" strokeWidth="3" opacity="0.7" />
      </g>
      
      {/* 세 번째 충격파 껍질 */}
      <circle 
        cx="50" 
        cy="50" 
        r="46" 
        fill="url(#shockWave3)" 
        className="animate-pulse"
        style={{ animationDuration: '3s' }}
        filter="url(#shockWave)"
      />
      
      {/* 두 번째 충격파 껍질 */}
      <circle 
        cx="50" 
        cy="50" 
        r="40" 
        fill="url(#shockWave2)" 
        className="animate-pulse"
        style={{ animationDuration: '2s', animationDelay: '0.3s' }}
        filter="url(#shockWave)"
      />
      
      {/* 첫 번째 충격파 껍질 */}
      <circle 
        cx="50" 
        cy="50" 
        r="32" 
        fill="url(#shockWave1)" 
        className="animate-pulse"
        style={{ animationDuration: '1.5s', animationDelay: '0.6s' }}
        filter="url(#shockWave)"
      />
      
      {/* 폭발 핵심 */}
      <circle 
        cx="50" 
        cy="50" 
        r="20" 
        fill="url(#explosionCore)" 
        className="animate-pulse"
        style={{ animationDuration: '1s', animationDelay: '0.8s' }}
        filter="url(#supernovaGlow)"
      />
      
      {/* 방사형 폭발 광선들 - 더 많고 화려하게 */}
      <g className="animate-pulse" style={{ animationDuration: '1.2s' }}>
        {/* 주요 8방향 광선 */}
        <path d="M50 15 L50 2" stroke="#FFFFFF" strokeWidth="8" filter="url(#supernovaGlow)" />
        <path d="M50 85 L50 98" stroke="#FFFFFF" strokeWidth="8" filter="url(#supernovaGlow)" />
        <path d="M15 50 L2 50" stroke="#FFFFFF" strokeWidth="8" filter="url(#supernovaGlow)" />
        <path d="M85 50 L98 50" stroke="#FFFFFF" strokeWidth="8" filter="url(#supernovaGlow)" />
        
        <path d="M25 25 L10 10" stroke="#FFEB3B" strokeWidth="7" filter="url(#supernovaGlow)" />
        <path d="M75 75 L90 90" stroke="#FFEB3B" strokeWidth="7" filter="url(#supernovaGlow)" />
        <path d="M75 25 L90 10" stroke="#FFEB3B" strokeWidth="7" filter="url(#supernovaGlow)" />
        <path d="M25 75 L10 90" stroke="#FFEB3B" strokeWidth="7" filter="url(#supernovaGlow)" />
        
        {/* 보조 광선들 */}
        <path d="M50 25 L50 5" stroke="#FF9800" strokeWidth="6" filter="url(#supernovaGlow)" />
        <path d="M50 75 L50 95" stroke="#FF9800" strokeWidth="6" filter="url(#supernovaGlow)" />
        <path d="M25 50 L5 50" stroke="#FF9800" strokeWidth="6" filter="url(#supernovaGlow)" />
        <path d="M75 50 L95 50" stroke="#FF9800" strokeWidth="6" filter="url(#supernovaGlow)" />
        
        {/* 추가 대각선 광선 */}
        <path d="M35 15 L25 5" stroke="#FF5722" strokeWidth="5" filter="url(#supernovaGlow)" />
        <path d="M65 15 L75 5" stroke="#FF5722" strokeWidth="5" filter="url(#supernovaGlow)" />
        <path d="M35 85 L25 95" stroke="#FF5722" strokeWidth="5" filter="url(#supernovaGlow)" />
        <path d="M65 85 L75 95" stroke="#FF5722" strokeWidth="5" filter="url(#supernovaGlow)" />
        <path d="M15 35 L5 25" stroke="#FF5722" strokeWidth="5" filter="url(#supernovaGlow)" />
        <path d="M15 65 L5 75" stroke="#FF5722" strokeWidth="5" filter="url(#supernovaGlow)" />
        <path d="M85 35 L95 25" stroke="#FF5722" strokeWidth="5" filter="url(#supernovaGlow)" />
        <path d="M85 65 L95 75" stroke="#FF5722" strokeWidth="5" filter="url(#supernovaGlow)" />
      </g>
      
      {/* 중성자별 잔해 - 초고속 회전 */}
      <circle 
        cx="50" 
        cy="50" 
        r="4" 
        fill="url(#neutronStar)" 
        filter="url(#supernovaGlow)"
        className="animate-spin"
        style={{ animationDuration: '0.03s' }}
      />
      
      {/* 중원소 방출 (Fe, Ni, Co, Si 등) - 매우 화려하게 */}
      <g className="animate-spin" style={{ animationDuration: '5s', transformOrigin: '50px 50px' }}>
        {/* 철 (Fe) */}
        <circle cx="18" cy="12" r="2.5" fill="#FF5722" className="animate-ping" filter="url(#elementalDispersion)" />
        <circle cx="82" cy="15" r="2.2" fill="#FF5722" className="animate-ping" style={{ animationDelay: '0.5s' }} />
        <circle cx="88" cy="82" r="2.8" fill="#FF5722" className="animate-ping" style={{ animationDelay: '1s' }} />
        <circle cx="12" cy="88" r="2.4" fill="#FF5722" className="animate-ping" style={{ animationDelay: '1.5s' }} />
        
        {/* 니켈 (Ni) */}
        <circle cx="25" cy="20" r="2.1" fill="#FF9800" className="animate-ping" style={{ animationDelay: '2s' }} filter="url(#elementalDispersion)" />
        <circle cx="75" cy="25" r="2.3" fill="#FF9800" className="animate-ping" style={{ animationDelay: '2.5s' }} />
        <circle cx="80" cy="75" r="2" fill="#FF9800" className="animate-ping" style={{ animationDelay: '3s' }} />
        <circle cx="20" cy="80" r="2.2" fill="#FF9800" className="animate-ping" style={{ animationDelay: '3.5s' }} />
        
        {/* 코발트 (Co) */}
        <circle cx="15" cy="35" r="1.9" fill="#FFEB3B" className="animate-ping" style={{ animationDelay: '4s' }} filter="url(#elementalDispersion)" />
        <circle cx="85" cy="30" r="2.1" fill="#FFEB3B" className="animate-ping" style={{ animationDelay: '4.5s' }} />
        <circle cx="70" cy="85" r="1.8" fill="#FFEB3B" className="animate-ping" style={{ animationDelay: '0.2s' }} />
        <circle cx="30" cy="15" r="2" fill="#FFEB3B" className="animate-ping" style={{ animationDelay: '0.7s' }} />
        
        {/* 실리콘 (Si) */}
        <circle cx="8" cy="50" r="1.7" fill="#E91E63" className="animate-ping" style={{ animationDelay: '1.2s' }} />
        <circle cx="92" cy="45" r="2.2" fill="#E91E63" className="animate-ping" style={{ animationDelay: '1.7s' }} />
        <circle cx="50" cy="8" r="1.9" fill="#E91E63" className="animate-ping" style={{ animationDelay: '2.2s' }} />
        <circle cx="50" cy="92" r="2.1" fill="#E91E63" className="animate-ping" style={{ animationDelay: '2.7s' }} />
        
        {/* 마그네슘 (Mg) */}
        <circle cx="35" cy="8" r="1.6" fill="#9C27B0" className="animate-ping" style={{ animationDelay: '3.2s' }} />
        <circle cx="65" cy="92" r="1.8" fill="#9C27B0" className="animate-ping" style={{ animationDelay: '3.7s' }} />
        <circle cx="8" cy="65" r="1.5" fill="#9C27B0" className="animate-ping" style={{ animationDelay: '4.2s' }} />
        <circle cx="92" cy="35" r="1.7" fill="#9C27B0" className="animate-ping" style={{ animationDelay: '4.7s' }} />
      </g>
      
      {/* 반대 방향 중원소 회전 */}
      <g className="animate-spin" style={{ animationDuration: '6s', transformOrigin: '50px 50px', animationDirection: 'reverse' }}>
        <circle cx="22" cy="35" r="1.4" fill="#673AB7" className="animate-ping" filter="url(#elementalDispersion)" />
        <circle cx="78" cy="65" r="1.6" fill="#3F51B5" className="animate-ping" style={{ animationDelay: '1s' }} />
        <circle cx="35" cy="78" r="1.3" fill="#2196F3" className="animate-ping" style={{ animationDelay: '2s' }} />
        <circle cx="65" cy="22" r="1.5" fill="#1976D2" className="animate-ping" style={{ animationDelay: '3s' }} />
      </g>
      
      {/* 감마선 방출 - 동심원 */}
      <g className="animate-pulse" style={{ animationDuration: '0.4s' }}>
        <circle cx="50" cy="50" r="10" fill="none" stroke="#FFFFFF" strokeWidth="3" opacity="0.9" />
        <circle cx="50" cy="50" r="14" fill="none" stroke="#E3F2FD" strokeWidth="2.5" opacity="0.8" />
        <circle cx="50" cy="50" r="18" fill="none" stroke="#BBDEFB" strokeWidth="2" opacity="0.7" />
        <circle cx="50" cy="50" r="22" fill="none" stroke="#90CAF9" strokeWidth="1.5" opacity="0.6" />
        <circle cx="50" cy="50" r="26" fill="none" stroke="#64B5F6" strokeWidth="1.2" opacity="0.5" />
        <circle cx="50" cy="50" r="30" fill="none" stroke="#42A5F5" strokeWidth="1" opacity="0.4" />
      </g>
      
      {/* 초신성 잔해 성운 형성 - 더 화려하게 */}
      <g className="animate-pulse" style={{ animationDuration: '8s' }} opacity="0.6">
        <ellipse cx="20" cy="15" rx="10" ry="5" fill="#FF5722" transform="rotate(45 20 15)" filter="url(#explosionFlicker)" />
        <ellipse cx="80" cy="85" rx="9" ry="4.5" fill="#9C27B0" transform="rotate(-30 80 85)" filter="url(#explosionFlicker)" />
        <ellipse cx="85" cy="15" rx="8" ry="4" fill="#FF9800" transform="rotate(60 85 15)" filter="url(#explosionFlicker)" />
        <ellipse cx="15" cy="85" rx="11" ry="5.5" fill="#673AB7" transform="rotate(-45 15 85)" filter="url(#explosionFlicker)" />
        <ellipse cx="80" cy="20" rx="7" ry="3.5" fill="#E91E63" transform="rotate(30 80 20)" filter="url(#explosionFlicker)" />
        <ellipse cx="20" cy="80" rx="9" ry="4.5" fill="#3F51B5" transform="rotate(-60 20 80)" filter="url(#explosionFlicker)" />
        <ellipse cx="50" cy="12" rx="6" ry="3" fill="#FFEB3B" transform="rotate(90 50 12)" filter="url(#explosionFlicker)" />
        <ellipse cx="50" cy="88" rx="8" ry="4" fill="#2196F3" transform="rotate(0 50 88)" filter="url(#explosionFlicker)" />
      </g>
      
      {/* 우주선 생성 지역 - 고에너지 입자 */}
      <g className="animate-pulse" style={{ animationDuration: '2.5s' }} opacity="0.8">
        <circle cx="3" cy="8" r="0.8" fill="#00E5FF" className="animate-ping" />
        <circle cx="97" cy="12" r="0.7" fill="#18FFFF" className="animate-ping" style={{ animationDelay: '0.3s' }} />
        <circle cx="92" cy="92" r="1" fill="#64FFDA" className="animate-ping" style={{ animationDelay: '0.6s' }} />
        <circle cx="8" cy="97" r="0.9" fill="#00BCD4" className="animate-ping" style={{ animationDelay: '0.9s' }} />
        <circle cx="12" cy="3" r="0.6" fill="#B2EBF2" className="animate-ping" style={{ animationDelay: '1.2s' }} />
        <circle cx="88" cy="3" r="0.8" fill="#4DD0E1" className="animate-ping" style={{ animationDelay: '1.5s' }} />
        <circle cx="3" cy="88" r="0.7" fill="#26C6DA" className="animate-ping" style={{ animationDelay: '1.8s' }} />
        <circle cx="97" cy="88" r="0.9" fill="#00ACC1" className="animate-ping" style={{ animationDelay: '2.1s' }} />
      </g>
      
      {/* 중성미자 방출 표시 */}
      <g className="animate-ping" style={{ animationDuration: '1s' }} opacity="0.3">
        <circle cx="50" cy="50" r="35" fill="none" stroke="#E3F2FD" strokeWidth="0.5" />
        <circle cx="50" cy="50" r="40" fill="none" stroke="#BBDEFB" strokeWidth="0.4" />
        <circle cx="50" cy="50" r="45" fill="none" stroke="#90CAF9" strokeWidth="0.3" />
      </g>
      
      {/* 광도 표시 */}
      <text x="50" y="6" textAnchor="middle" fill="#FFFFFF" fontSize="3.5" opacity="0.9" fontWeight="bold">
        Peak: -19.3 mag
      </text>
      
      {/* 폭발 에너지 */}
      <text x="50" y="12" textAnchor="middle" fill="#FFEB3B" fontSize="3" opacity="0.8">
        E ~ 10⁴⁴ J
      </text>
      
      {/* 타입 분류 */}
      <text x="50" y="94" textAnchor="middle" fill="#FF9800" fontSize="3.5" opacity="0.8">
        Type Ia/II
      </text>
      
      {/* 확장 속도 */}
      <text x="8" y="50" fill="#E91E63" fontSize="2.5" opacity="0.7">
        v ~ 10⁴ km/s
      </text>
      
      {/* 온도 */}
      <text x="92" y="50" fill="#9C27B0" fontSize="2.5" opacity="0.6" textAnchor="end">
        T ~ 10⁹ K
      </text>
    </svg>
  );
}