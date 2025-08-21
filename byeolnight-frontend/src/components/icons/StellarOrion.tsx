import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

const StellarOrion = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="betelgeuse" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="30%" stopColor="#FFEB3B" />
        <stop offset="70%" stopColor="#FF9800" />
        <stop offset="100%" stopColor="#FF5722" />
      </radialGradient>
      <radialGradient id="rigel" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="50%" stopColor="#E3F2FD" />
        <stop offset="100%" stopColor="#1976D2" />
      </radialGradient>
      <radialGradient id="orionNebula" cx="50%" cy="50%" r="80%">
        <stop offset="0%" stopColor="#E91E63" opacity="0.8" />
        <stop offset="50%" stopColor="#9C27B0" opacity="0.6" />
        <stop offset="100%" stopColor="#673AB7" opacity="0.3" />
      </radialGradient>
      <filter id="starGlow">
        <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 오리온 대성운 (M42) */}
    <ellipse 
      cx="50" 
      cy="70" 
      rx="12" 
      ry="6" 
      fill="url(#orionNebula)" 
      className="animate-pulse"
      style={{ animationDuration: '6s' }}
    />
    
    {/* 베텔게우스 (적색초거성) - 왼쪽 어깨 */}
    <circle 
      cx="25" 
      cy="25" 
      r="3" 
      fill="url(#betelgeuse)" 
      filter="url(#starGlow)"
      className="animate-pulse"
      style={{ animationDuration: '4s' }}
    />
    
    {/* 리겔 (청색초거성) - 오른쪽 발 */}
    <circle 
      cx="75" 
      cy="80" 
      r="2.5" 
      fill="url(#rigel)" 
      filter="url(#starGlow)"
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    
    {/* 허리띠 삼성 (알니탁, 알닐람, 민타카) */}
    <circle cx="40" cy="45" r="2" fill="#E3F2FD" className="animate-pulse" />
    <circle cx="50" cy="45" r="2.2" fill="#FFFFFF" className="animate-pulse" style={{ animationDelay: '0.5s' }} />
    <circle cx="60" cy="45" r="1.8" fill="#E3F2FD" className="animate-pulse" style={{ animationDelay: '1s' }} />
    
    {/* 기타 주요 별들 */}
    <circle cx="75" cy="25" r="1.8" fill="#4FC3F7" className="animate-pulse" style={{ animationDelay: '1.5s' }} />
    <circle cx="25" cy="80" r="1.6" fill="#81C784" className="animate-pulse" style={{ animationDelay: '2s' }} />
    <circle cx="35" cy="35" r="1.4" fill="#FFB74D" className="animate-pulse" style={{ animationDelay: '2.5s' }} />
    <circle cx="65" cy="35" r="1.5" fill="#F48FB1" className="animate-pulse" style={{ animationDelay: '3s' }} />
    
    {/* 별자리 연결선 - 미니멀하게 */}
    <g stroke="#64FFDA" strokeWidth="0.8" opacity="0.4" className="animate-pulse" style={{ animationDuration: '8s' }}>
      <line x1="25" y1="25" x2="35" y2="35" />
      <line x1="35" y1="35" x2="50" y2="45" />
      <line x1="50" y1="45" x2="65" y2="35" />
      <line x1="65" y1="35" x2="75" y2="25" />
      <line x1="40" y1="45" x2="25" y2="80" />
      <line x1="60" y1="45" x2="75" y2="80" />
      <line x1="50" y1="45" x2="50" y2="65" />
    </g>
    
    {/* 오리온 대성운 내부 별들 */}
    <g className="animate-pulse" style={{ animationDuration: '5s' }}>
      <circle cx="47" cy="68" r="0.8" fill="#E91E63" />
      <circle cx="53" cy="70" r="1" fill="#9C27B0" />
      <circle cx="50" cy="72" r="0.6" fill="#673AB7" />
      <circle cx="49" cy="66" r="0.7" fill="#E91E63" />
    </g>
    
    {/* 말머리 성운 (어둠) */}
    <ellipse 
      cx="35" 
      cy="55" 
      rx="2" 
      ry="1" 
      fill="#1A1A1A" 
      opacity="0.8"
      className="animate-pulse"
      style={{ animationDuration: '10s' }}
    />
    
    {/* 배경 별들 - 최소화 */}
    <g className="animate-pulse" style={{ animationDuration: '12s' }} opacity="0.3">
      <circle cx="15" cy="40" r="0.3" fill="#B0BEC5" />
      <circle cx="85" cy="50" r="0.4" fill="#B0BEC5" />
      <circle cx="20" cy="70" r="0.2" fill="#B0BEC5" />
      <circle cx="80" cy="40" r="0.3" fill="#B0BEC5" />
    </g>
  </svg>
);

export default StellarOrion;