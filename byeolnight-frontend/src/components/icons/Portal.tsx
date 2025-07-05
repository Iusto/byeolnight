import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export default function Portal({ className = "", size = 100 }: IconProps) {
  return (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="portalCenter" cx="50%" cy="50%" r="30%">
        <stop offset="0%" stopColor="#000000" />
        <stop offset="100%" stopColor="#1A1A1A" />
      </radialGradient>
      <radialGradient id="portalRing1" cx="50%" cy="50%" r="60%">
        <stop offset="40%" stopColor="transparent" />
        <stop offset="50%" stopColor="#9C27B0" opacity="0.8" />
        <stop offset="60%" stopColor="transparent" />
      </radialGradient>
      <radialGradient id="portalRing2" cx="50%" cy="50%" r="80%">
        <stop offset="60%" stopColor="transparent" />
        <stop offset="70%" stopColor="#00E5FF" opacity="0.6" />
        <stop offset="80%" stopColor="transparent" />
      </radialGradient>
    </defs>
    
    {/* 포털 외곽 링 */}
    <circle cx="50" cy="50" r="40" fill="url(#portalRing2)" className="animate-spin" style={{ animationDuration: '8s' }} />
    <circle cx="50" cy="50" r="30" fill="url(#portalRing1)" className="animate-spin" style={{ animationDuration: '6s', animationDirection: 'reverse' }} />
    
    {/* 포털 중심 */}
    <circle cx="50" cy="50" r="15" fill="url(#portalCenter)" />
    
    {/* 에너지 파동 */}
    <circle cx="50" cy="50" r="25" fill="none" stroke="#9C27B0" strokeWidth="1" opacity="0.6" className="animate-ping" />
    <circle cx="50" cy="50" r="35" fill="none" stroke="#00E5FF" strokeWidth="0.8" opacity="0.4" className="animate-ping" style={{ animationDelay: '1s' }} />
    
    {/* 차원 입자들 */}
    <circle cx="35" cy="35" r="0.8" fill="#9C27B0" className="animate-ping" />
    <circle cx="65" cy="35" r="0.6" fill="#00E5FF" className="animate-ping" style={{ animationDelay: '1.5s' }} />
    <circle cx="35" cy="65" r="0.7" fill="#9C27B0" className="animate-ping" style={{ animationDelay: '2s' }} />
    <circle cx="65" cy="65" r="0.5" fill="#00E5FF" className="animate-ping" style={{ animationDelay: '0.5s' }} />
  </svg>
);
}