import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Neptune({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="neptuneGradient" cx="50%" cy="30%" r="70%">
        <stop offset="0%" stopColor="#E3F2FD" />
        <stop offset="30%" stopColor="#2196F3" />
        <stop offset="60%" stopColor="#1565C0" />
        <stop offset="100%" stopColor="#0D47A1" />
      </radialGradient>
      <radialGradient id="neptuneGlow" cx="50%" cy="50%" r="85%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="75%" stopColor="transparent" />
        <stop offset="100%" stopColor="#2196F3" opacity="0.5" />
      </radialGradient>
      <radialGradient id="storm" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#1976D2" />
        <stop offset="100%" stopColor="#0D47A1" />
      </radialGradient>
    </defs>
    
    {/* 해왕성 글로우 */}
    <circle 
      cx="50" 
      cy="50" 
      r="40" 
      fill="url(#neptuneGlow)" 
      className="animate-pulse"
      style={{ animationDuration: '4s' }}
    />
    
    {/* 해왕성 본체 */}
    <circle 
      cx="50" 
      cy="50" 
      r="32" 
      fill="url(#neptuneGradient)"
      className="animate-bounce"
      style={{ animationDuration: '6s' }}
    />
    
    {/* 대기 띠들 */}
    <ellipse cx="50" cy="38" rx="30" ry="2.5" fill="#1976D2" opacity="0.6" />
    <ellipse cx="50" cy="48" rx="29" ry="2" fill="#1565C0" opacity="0.5" />
    <ellipse cx="50" cy="58" rx="28" ry="2.5" fill="#0D47A1" opacity="0.7" />
    <ellipse cx="50" cy="68" rx="26" ry="2" fill="#1976D2" opacity="0.4" />
    
    {/* 대흑점 (Great Dark Spot) */}
    <ellipse 
      cx="60" 
      cy="45" 
      rx="5" 
      ry="8" 
      fill="url(#storm)"
      className="animate-pulse"
      style={{ animationDuration: '3s' }}
    />
    
    {/* 바람 패턴 */}
    <path 
      d="M20 40 Q35 35, 50 40 Q65 45, 80 40" 
      stroke="#64B5F6" 
      strokeWidth="1" 
      fill="none" 
      opacity="0.6"
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    <path 
      d="M25 55 Q40 50, 55 55 Q70 60, 75 55" 
      stroke="#42A5F5" 
      strokeWidth="1" 
      fill="none" 
      opacity="0.5"
      className="animate-pulse"
      style={{ animationDuration: '2.5s', animationDelay: '1s' }}
    />
    
    {/* 얼음 입자들 */}
    <circle cx="35" cy="30" r="1" fill="#E3F2FD" opacity="0.8" />
    <circle cx="70" cy="65" r="0.8" fill="#BBDEFB" opacity="0.7" />
    <circle cx="30" cy="70" r="0.6" fill="#E3F2FD" opacity="0.9" />
    
    {/* 신비로운 빛 효과 */}
    <circle cx="40" cy="40" r="0.5" fill="#FFFFFF" className="animate-ping" />
    <circle cx="65" cy="55" r="0.5" fill="#E3F2FD" className="animate-ping" style={{ animationDelay: '1.5s' }} />
    <circle cx="45" cy="65" r="0.3" fill="#FFFFFF" className="animate-ping" style={{ animationDelay: '2.5s' }} />
  </svg>
);
}