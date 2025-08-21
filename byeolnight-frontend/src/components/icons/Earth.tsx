import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Earth({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="earthGradient" cx="50%" cy="30%" r="70%">
        <stop offset="0%" stopColor="#87CEEB" />
        <stop offset="30%" stopColor="#4FC3F7" />
        <stop offset="60%" stopColor="#2196F3" />
        <stop offset="100%" stopColor="#1565C0" />
      </radialGradient>
      <radialGradient id="earthAtmosphere" cx="50%" cy="50%" r="85%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="75%" stopColor="transparent" />
        <stop offset="100%" stopColor="#81D4FA" opacity="0.4" />
      </radialGradient>
      <filter id="earthGlow">
        <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 대기층 글로우 */}
    <circle 
      cx="50" 
      cy="50" 
      r="40" 
      fill="url(#earthAtmosphere)" 
      filter="url(#earthGlow)"
      className="animate-pulse"
      style={{ animationDuration: '4s' }}
    />
    
    {/* 지구 본체 */}
    <circle 
      cx="50" 
      cy="50" 
      r="35" 
      fill="url(#earthGradient)"
      className="animate-bounce"
      style={{ animationDuration: '6s' }}
    />
    
    {/* 대륙들 */}
    <path 
      d="M30 40 Q40 35, 50 40 L55 45 Q50 50, 45 45 Z" 
      fill="#4CAF50" 
      opacity="0.8"
    />
    <path 
      d="M60 30 Q70 25, 75 35 L70 45 Q65 40, 60 35 Z" 
      fill="#388E3C" 
      opacity="0.7"
    />
    <path 
      d="M25 55 Q35 50, 40 60 L35 65 Q30 60, 25 55 Z" 
      fill="#2E7D32" 
      opacity="0.6"
    />
    
    {/* 구름 */}
    <ellipse cx="45" cy="25" rx="8" ry="3" fill="#FFFFFF" opacity="0.6" />
    <ellipse cx="65" cy="50" rx="6" ry="2" fill="#FFFFFF" opacity="0.5" />
    <ellipse cx="35" cy="70" rx="7" ry="2.5" fill="#FFFFFF" opacity="0.4" />
    
    {/* 반짝이는 점들 */}
    <circle cx="40" cy="30" r="0.5" fill="#FFFFFF" className="animate-ping" />
    <circle cx="60" cy="60" r="0.5" fill="#FFFFFF" className="animate-ping" style={{ animationDelay: '1s' }} />
  </svg>
);
}