import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Moon({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="moonGradient" cx="40%" cy="30%" r="80%">
        <stop offset="0%" stopColor="#FAFAFA" />
        <stop offset="30%" stopColor="#F5F5F5" />
        <stop offset="60%" stopColor="#E0E0E0" />
        <stop offset="100%" stopColor="#BDBDBD" />
      </radialGradient>
      <radialGradient id="moonGlow" cx="50%" cy="50%" r="90%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="70%" stopColor="transparent" />
        <stop offset="100%" stopColor="#F5F5F5" opacity="0.6" />
      </radialGradient>
      <filter id="moonSoftGlow">
        <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 달 글로우 */}
    <circle 
      cx="50" 
      cy="50" 
      r="42" 
      fill="url(#moonGlow)" 
      filter="url(#moonSoftGlow)"
      className="animate-pulse"
      style={{ animationDuration: '6s' }}
    />
    
    {/* 달 본체 */}
    <circle 
      cx="50" 
      cy="50" 
      r="35" 
      fill="url(#moonGradient)"
    />
    
    {/* 크레이터들 */}
    <circle cx="40" cy="35" r="4" fill="#BDBDBD" opacity="0.8" />
    <circle cx="38" cy="33" r="1.5" fill="#9E9E9E" opacity="0.6" />
    
    <circle cx="60" cy="45" r="3" fill="#BDBDBD" opacity="0.7" />
    <circle cx="58" cy="43" r="1" fill="#9E9E9E" opacity="0.5" />
    
    <circle cx="45" cy="60" r="5" fill="#BDBDBD" opacity="0.9" />
    <circle cx="43" cy="58" r="2" fill="#9E9E9E" opacity="0.7" />
    <circle cx="47" cy="62" r="1" fill="#757575" opacity="0.6" />
    
    <circle cx="65" cy="30" r="2.5" fill="#BDBDBD" opacity="0.6" />
    <circle cx="30" cy="55" r="2" fill="#BDBDBD" opacity="0.5" />
    
    {/* 바다 (Mare) */}
    <ellipse cx="35" cy="45" rx="8" ry="6" fill="#9E9E9E" opacity="0.4" />
    <ellipse cx="55" cy="55" rx="6" ry="4" fill="#9E9E9E" opacity="0.3" />
    
    {/* 달빛 반사 */}
    <ellipse cx="45" cy="40" rx="12" ry="8" fill="#FFFFFF" opacity="0.3" />
    
    {/* 신비로운 별빛 효과 */}
    <circle cx="25" cy="40" r="0.5" fill="#FFFFFF" className="animate-ping" />
    <circle cx="75" cy="35" r="0.5" fill="#F5F5F5" className="animate-ping" style={{ animationDelay: '2s' }} />
    <circle cx="70" cy="65" r="0.3" fill="#FFFFFF" className="animate-ping" style={{ animationDelay: '4s' }} />
    <circle cx="30" cy="70" r="0.3" fill="#F5F5F5" className="animate-ping" style={{ animationDelay: '1s' }} />
    
    {/* 달 주변 먼지 */}
    <circle cx="20" cy="50" r="0.8" fill="#E0E0E0" opacity="0.6" />
    <circle cx="80" cy="50" r="0.6" fill="#E0E0E0" opacity="0.5" />
    <circle cx="50" cy="15" r="0.4" fill="#F5F5F5" opacity="0.7" />
    <circle cx="50" cy="85" r="0.4" fill="#F5F5F5" opacity="0.7" />
  </svg>
);
}