import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Uranus({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="uranusGradient" cx="50%" cy="30%" r="70%">
        <stop offset="0%" stopColor="#E1F5FE" />
        <stop offset="30%" stopColor="#81D4FA" />
        <stop offset="60%" stopColor="#29B6F6" />
        <stop offset="100%" stopColor="#0277BD" />
      </radialGradient>
      <radialGradient id="uranusGlow" cx="50%" cy="50%" r="85%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="75%" stopColor="transparent" />
        <stop offset="100%" stopColor="#81D4FA" opacity="0.4" />
      </radialGradient>
      <linearGradient id="uranusRing" x1="0%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="40%" stopColor="#B3E5FC" opacity="0.6" />
        <stop offset="60%" stopColor="#B3E5FC" opacity="0.6" />
        <stop offset="100%" stopColor="transparent" />
      </linearGradient>
    </defs>
    
    {/* 천왕성 글로우 */}
    <circle 
      cx="50" 
      cy="50" 
      r="40" 
      fill="url(#uranusGlow)" 
      className="animate-pulse"
      style={{ animationDuration: '5s' }}
    />
    
    {/* 수직 고리들 (천왕성의 특징) */}
    <ellipse 
      cx="50" 
      cy="50" 
      rx="8" 
      ry="42" 
      fill="none" 
      stroke="url(#uranusRing)" 
      strokeWidth="2"
      opacity="0.7"
      className="animate-pulse"
      style={{ animationDuration: '3s' }}
    />
    <ellipse 
      cx="50" 
      cy="50" 
      rx="12" 
      ry="45" 
      fill="none" 
      stroke="#B3E5FC" 
      strokeWidth="1"
      opacity="0.5"
      className="animate-pulse"
      style={{ animationDuration: '4s', animationDelay: '1s' }}
    />
    
    {/* 천왕성 본체 */}
    <circle 
      cx="50" 
      cy="50" 
      r="28" 
      fill="url(#uranusGradient)"
      className="animate-bounce"
      style={{ animationDuration: '7s' }}
    />
    
    {/* 대기 패턴 */}
    <ellipse cx="50" cy="40" rx="25" ry="3" fill="#81D4FA" opacity="0.4" />
    <ellipse cx="50" cy="50" rx="24" ry="2" fill="#29B6F6" opacity="0.3" />
    <ellipse cx="50" cy="60" rx="23" ry="2.5" fill="#0277BD" opacity="0.5" />
    
    {/* 얼음 결정 효과 */}
    <polygon 
      points="35,35 37,32 39,35 37,38" 
      fill="#FFFFFF" 
      opacity="0.8"
      className="animate-ping"
      style={{ animationDelay: '2s' }}
    />
    <polygon 
      points="65,60 67,57 69,60 67,63" 
      fill="#E1F5FE" 
      opacity="0.7"
      className="animate-ping"
      style={{ animationDelay: '1s' }}
    />
    
    {/* 차가운 빛 효과 */}
    <circle cx="40" cy="45" r="0.5" fill="#FFFFFF" className="animate-ping" />
    <circle cx="60" cy="55" r="0.5" fill="#E1F5FE" className="animate-ping" style={{ animationDelay: '2.5s' }} />
  </svg>
);
}