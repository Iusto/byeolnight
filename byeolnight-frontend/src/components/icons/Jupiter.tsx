import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Jupiter({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="jupiterGradient" cx="50%" cy="30%" r="80%">
        <stop offset="0%" stopColor="#FFF8E1" />
        <stop offset="20%" stopColor="#FFECB3" />
        <stop offset="40%" stopColor="#FFD54F" />
        <stop offset="60%" stopColor="#FF8F00" />
        <stop offset="80%" stopColor="#E65100" />
        <stop offset="100%" stopColor="#BF360C" />
      </radialGradient>
      <radialGradient id="jupiterGlow" cx="50%" cy="50%" r="90%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="80%" stopColor="transparent" />
        <stop offset="100%" stopColor="#FFD54F" opacity="0.3" />
      </radialGradient>
      <radialGradient id="redSpot" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#FF5722" />
        <stop offset="100%" stopColor="#D32F2F" />
      </radialGradient>
    </defs>
    
    {/* 목성 글로우 */}
    <circle 
      cx="50" 
      cy="50" 
      r="42" 
      fill="url(#jupiterGlow)" 
      className="animate-pulse"
      style={{ animationDuration: '3s' }}
    />
    
    {/* 목성 본체 */}
    <circle 
      cx="50" 
      cy="50" 
      r="38" 
      fill="url(#jupiterGradient)"
      className="animate-bounce"
      style={{ animationDuration: '8s' }}
    />
    
    {/* 대기 띠들 */}
    <ellipse cx="50" cy="35" rx="35" ry="3" fill="#E65100" opacity="0.6" />
    <ellipse cx="50" cy="45" rx="36" ry="2" fill="#FF8F00" opacity="0.5" />
    <ellipse cx="50" cy="55" rx="35" ry="2.5" fill="#FFD54F" opacity="0.4" />
    <ellipse cx="50" cy="65" rx="33" ry="2" fill="#E65100" opacity="0.6" />
    
    {/* 대적점 */}
    <ellipse 
      cx="65" 
      cy="50" 
      rx="6" 
      ry="4" 
      fill="url(#redSpot)"
      className="animate-pulse"
      style={{ animationDuration: '2s' }}
    />
    
    {/* 소용돌이 효과 */}
    <circle cx="30" cy="40" r="2" fill="#FFECB3" opacity="0.7" />
    <circle cx="70" cy="60" r="1.5" fill="#FFECB3" opacity="0.6" />
    <circle cx="40" cy="65" r="1" fill="#FFF8E1" opacity="0.8" />
    
    {/* 반짝이는 점들 */}
    <circle cx="25" cy="50" r="0.5" fill="#FFFFFF" className="animate-ping" />
    <circle cx="75" cy="45" r="0.5" fill="#FFFFFF" className="animate-ping" style={{ animationDelay: '1.5s' }} />
  </svg>
);
}