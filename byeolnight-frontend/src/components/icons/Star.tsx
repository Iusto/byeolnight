import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Star({ className = "", size = 100 }: IconProps) {
  const uniqueId = `star-${Math.random().toString(36).substr(2, 9)}`;
  
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none" style={{ display: 'block' }}>
    <defs>
      <radialGradient id={`starCore-${uniqueId}`} cx="50%" cy="50%" r="40%">
        <stop offset="0%" stopColor="#FFFFFF" />
        <stop offset="50%" stopColor="#FFEB3B" />
        <stop offset="100%" stopColor="#FF9800" />
      </radialGradient>
      <radialGradient id={`starGlow-${uniqueId}`} cx="50%" cy="50%" r="80%">
        <stop offset="0%" stopColor="transparent" />
        <stop offset="60%" stopColor="transparent" />
        <stop offset="100%" stopColor="#FFEB3B" opacity="0.3" />
      </radialGradient>
    </defs>
    
    {/* 별 글로우 */}
    <circle 
      cx="50" 
      cy="50" 
      r="40" 
      fill={`url(#starGlow-${uniqueId})`}
      className="animate-pulse"
      style={{ animationDuration: '3s' }}
    />
    
    {/* 별 모양 */}
    <polygon 
      points="50,15 55,35 75,35 60,50 65,70 50,60 35,70 40,50 25,35 45,35" 
      fill={`url(#starCore-${uniqueId})`}
    />
    
    {/* 중심 빛 */}
    <circle cx="50" cy="50" r="8" fill="#FFFFFF" opacity="0.8" />
    
    {/* 십자 광선 */}
    <line x1="50" y1="20" x2="50" y2="30" stroke="#FFFFFF" strokeWidth="2" opacity="0.6" />
    <line x1="50" y1="70" x2="50" y2="80" stroke="#FFFFFF" strokeWidth="2" opacity="0.6" />
    <line x1="20" y1="50" x2="30" y2="50" stroke="#FFFFFF" strokeWidth="2" opacity="0.6" />
    <line x1="70" y1="50" x2="80" y2="50" stroke="#FFFFFF" strokeWidth="2" opacity="0.6" />
  </svg>
  );
}