import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Rocket({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <linearGradient id="rocketBody" x1="50%" y1="0%" x2="50%" y2="100%">
        <stop offset="0%" stopColor="#E0E0E0" />
        <stop offset="50%" stopColor="#BDBDBD" />
        <stop offset="100%" stopColor="#757575" />
      </linearGradient>
      <radialGradient id="rocketWindow" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#81D4FA" />
        <stop offset="100%" stopColor="#1976D2" />
      </radialGradient>
      <linearGradient id="flame" x1="50%" y1="0%" x2="50%" y2="100%">
        <stop offset="0%" stopColor="#FF5722" />
        <stop offset="50%" stopColor="#FF9800" />
        <stop offset="100%" stopColor="#FFEB3B" />
      </linearGradient>
    </defs>
    
    {/* 로켓 화염 */}
    <ellipse 
      cx="50" 
      cy="85" 
      rx="8" 
      ry="15" 
      fill="url(#flame)"
      className="animate-pulse"
      style={{ animationDuration: '0.5s' }}
    />
    <ellipse 
      cx="45" 
      cy="88" 
      rx="4" 
      ry="10" 
      fill="#FFEB3B"
      className="animate-pulse"
      style={{ animationDuration: '0.3s' }}
    />
    <ellipse 
      cx="55" 
      cy="88" 
      rx="4" 
      ry="10" 
      fill="#FFEB3B"
      className="animate-pulse"
      style={{ animationDuration: '0.4s' }}
    />
    
    {/* 로켓 본체 */}
    <ellipse cx="50" cy="45" rx="12" ry="35" fill="url(#rocketBody)" />
    
    {/* 로켓 노즈콘 */}
    <path d="M38 20 L50 10 L62 20 Z" fill="#FF5722" />
    
    {/* 창문 */}
    <circle cx="50" cy="30" r="6" fill="url(#rocketWindow)" />
    <circle cx="50" cy="30" r="4" fill="#FFFFFF" opacity="0.3" />
    
    {/* 날개 */}
    <path d="M38 60 L30 75 L38 70 Z" fill="#757575" />
    <path d="M62 60 L70 75 L62 70 Z" fill="#757575" />
    
    {/* 로켓 디테일 */}
    <rect x="47" y="40" width="6" height="20" fill="#757575" />
    <circle cx="50" cy="50" r="2" fill="#FF5722" />
    
    {/* 추진 노즐 */}
    <rect x="45" y="70" width="10" height="8" fill="#424242" />
    
    {/* 연기 입자들 */}
    <circle cx="45" cy="95" r="1" fill="#BDBDBD" opacity="0.6" className="animate-ping" />
    <circle cx="55" cy="92" r="0.8" fill="#BDBDBD" opacity="0.5" className="animate-ping" style={{ animationDelay: '0.5s' }} />
    <circle cx="50" cy="98" r="0.6" fill="#BDBDBD" opacity="0.7" className="animate-ping" style={{ animationDelay: '1s' }} />
  </svg>
);
}