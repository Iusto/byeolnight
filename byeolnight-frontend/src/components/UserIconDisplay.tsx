import React from 'react';
import * as Icons from './icons';

interface UserIconDisplayProps {
  iconName?: string;
  size?: 'small' | 'medium' | 'large';
  className?: string;
}

// 기본 소행성 아이콘 (사용자가 아이콘을 장착하지 않았을 때만 사용)
const DefaultAsteroid = ({ className }: { className?: string }) => {
  const AsteroidIcon = (Icons as any)['Asteroid'];
  
  if (AsteroidIcon) {
    return <AsteroidIcon className={className} />;
  }
  
  return (
    <svg className={className} fill="currentColor" viewBox="0 0 24 24">
      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.94-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/>
    </svg>
  );
};

export default function UserIconDisplay({ iconName, size = 'small', className = '' }: UserIconDisplayProps) {
  // iconName이 없으면 기본 소행성 아이콘 표시 (임시적)
  // 실제로는 모든 사용자가 소행성 아이콘을 보유하고 장착해야 함
  if (!iconName) {
    return (
      <div className={`icon-container ${getSizeClass(size)} rounded-lg`}>
        <DefaultAsteroid className={`w-full h-full text-gray-400 ${className}`} />
      </div>
    );
  }

  const IconComponent = (Icons as any)[iconName];
  
  if (!IconComponent) {
    console.warn(`아이콘을 찾을 수 없습니다: ${iconName}`);
    // 아이콘을 찾을 수 없으면 기본 소행성 아이콘 표시
    return (
      <div className={`icon-container ${getSizeClass(size)} rounded-lg`}>
        <DefaultAsteroid className={`w-full h-full text-gray-400 ${className}`} />
      </div>
    );
  }

  return (
    <div className={`icon-container ${getSizeClass(size)} rounded-lg ${className}`}>
      <IconComponent className="w-full h-full" style={{ display: 'block' }} />
    </div>
  );
}

function getSizeClass(size: 'small' | 'medium' | 'large') {
  const sizeClasses = {
    small: 'w-7 h-7',
    medium: 'w-12 h-12',
    large: 'w-16 h-16'
  };
  return sizeClasses[size];
}