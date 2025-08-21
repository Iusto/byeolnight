import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Asteroid({ className = "", size = 100 }: IconProps) {
  const uniqueId = `asteroid-${Math.random().toString(36).substr(2, 9)}`;
  
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none" style={{ display: 'block' }}>
      <defs>
        <radialGradient id={`asteroidGradient-${uniqueId}`} cx="40%" cy="30%" r="80%">
          <stop offset="0%" stopColor="#8D6E63" />
          <stop offset="50%" stopColor="#5D4037" />
          <stop offset="100%" stopColor="#3E2723" />
        </radialGradient>
        <filter id={`asteroidGlow-${uniqueId}`}>
          <feGaussianBlur stdDeviation="1" result="coloredBlur"/>
          <feMerge> 
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
      </defs>
      
      {/* 소행성 본체 */}
      <path 
        d="M25 45 Q30 20 50 25 Q70 30 75 50 Q70 70 50 75 Q30 70 25 45 Z" 
        fill={`url(#asteroidGradient-${uniqueId})`}
        filter={`url(#asteroidGlow-${uniqueId})`}
      />
      
      {/* 크레이터들 */}
      <circle cx="40" cy="35" r="3" fill="#3E2723" opacity="0.8" />
      <circle cx="60" cy="50" r="2" fill="#3E2723" opacity="0.6" />
      <circle cx="45" cy="60" r="1.5" fill="#3E2723" opacity="0.7" />
      <circle cx="55" cy="40" r="1" fill="#3E2723" opacity="0.5" />
      
      {/* 표면 질감 */}
      <circle cx="35" cy="50" r="0.5" fill="#6D4C41" opacity="0.8" />
      <circle cx="65" cy="45" r="0.8" fill="#6D4C41" opacity="0.6" />
      <circle cx="50" cy="35" r="0.6" fill="#6D4C41" opacity="0.7" />
    </svg>
  );
}