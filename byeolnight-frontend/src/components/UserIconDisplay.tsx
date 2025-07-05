import React from 'react';
import * as Icons from './icons';

interface UserIconDisplayProps {
  iconName?: string;
  size?: 'small' | 'medium' | 'large';
  className?: string;
}

export default function UserIconDisplay({ iconName, size = 'small', className = '' }: UserIconDisplayProps) {
  if (!iconName) return null;

  const IconComponent = (Icons as any)[iconName];
  if (!IconComponent) return null;

  const sizeClasses = {
    small: 'w-5 h-5',
    medium: 'w-6 h-6', 
    large: 'w-8 h-8'
  };

  return (
    <div className={`inline-flex items-center justify-center ${sizeClasses[size]} ${className}`}>
      <IconComponent className="w-full h-full" />
    </div>
  );
}