import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function AuroraNebula({ className = "", size = 100 }: IconProps) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
      <defs>
        <linearGradient id="auroraGradient1" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#00FF87" />
          <stop offset="25%" stopColor="#60EFFF" />
          <stop offset="50%" stopColor="#FF6B9D" />
          <stop offset="75%" stopColor="#C471ED" />
          <stop offset="100%" stopColor="#F64F59" />
        </linearGradient>
        <linearGradient id="auroraGradient2" x1="100%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#667eea" />
          <stop offset="50%" stopColor="#764ba2" />
          <stop offset="100%" stopColor="#f093fb" />
        </linearGradient>
        <filter id="auroraGlow">
          <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
          <feMerge> 
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
      </defs>
      
      {/* 오로라 파동들 */}
      <path 
        d="M10 30 Q30 20 50 35 Q70 50 90 40" 
        stroke="url(#auroraGradient1)" 
        strokeWidth="8" 
        fill="none"
        filter="url(#auroraGlow)"
        className="animate-pulse"
        opacity="0.8"
      />
      <path 
        d="M15 50 Q35 40 55 55 Q75 70 95 60" 
        stroke="url(#auroraGradient2)" 
        strokeWidth="6" 
        fill="none"
        filter="url(#auroraGlow)"
        className="animate-pulse"
        style={{ animationDelay: '1s' }}
        opacity="0.7"
      />
      <path 
        d="M5 70 Q25 60 45 75 Q65 90 85 80" 
        stroke="url(#auroraGradient1)" 
        strokeWidth="4" 
        fill="none"
        filter="url(#auroraGlow)"
        className="animate-pulse"
        style={{ animationDelay: '2s' }}
        opacity="0.6"
      />
      
      {/* 반짝이는 별들 */}
      <circle cx="20" cy="25" r="1" fill="#FFFFFF" className="animate-ping" />
      <circle cx="80" cy="35" r="0.8" fill="#FFFFFF" className="animate-ping" style={{ animationDelay: '0.5s' }} />
      <circle cx="30" cy="80" r="1.2" fill="#FFFFFF" className="animate-ping" style={{ animationDelay: '1.5s' }} />
      <circle cx="70" cy="75" r="0.6" fill="#FFFFFF" className="animate-ping" style={{ animationDelay: '2.5s' }} />
    </svg>
  );
}