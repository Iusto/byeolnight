import React from 'react';
import type { StellaIcon as StellaIconType } from '../types/stellaIcon';
import type { IconRegistry } from '../../types/icons';
import * as StellarIcons from '../icons';

interface StellaIconProps {
  icon: StellaIconType;
  size?: 'small' | 'medium' | 'large';
  selected?: boolean;
  equipped?: boolean;
  owned?: boolean;
  onClick?: () => void;
  showPrice?: boolean;
  showName?: boolean;
}

// 등급별 차별화된 스타일 정의
const gradeStyles = {
  COMMON: {
    border: 'border-slate-400',
    bg: 'bg-gradient-to-br from-slate-800/50 to-gray-700/30',
    text: 'text-slate-200',
    glow: 'shadow-lg shadow-slate-400/30',
    ring: 'ring-slate-400/50',
    particle: 'bg-slate-300',
    animation: ''
  },
  RARE: {
    border: 'border-cyan-400',
    bg: 'bg-gradient-to-br from-cyan-900/60 to-blue-800/40',
    text: 'text-cyan-200',
    glow: 'shadow-xl shadow-cyan-400/50 hover:shadow-cyan-300/70',
    ring: 'ring-cyan-400/60',
    particle: 'bg-cyan-300',
    animation: 'hover:animate-pulse'
  },
  EPIC: {
    border: 'border-purple-400',
    bg: 'bg-gradient-to-br from-purple-900/70 to-violet-800/50',
    text: 'text-purple-200',
    glow: 'shadow-xl shadow-purple-400/60 hover:shadow-purple-300/80',
    ring: 'ring-purple-400/70',
    particle: 'bg-purple-300',
    animation: 'animate-pulse hover:scale-110'
  },
  LEGENDARY: {
    border: 'border-yellow-400',
    bg: 'bg-gradient-to-br from-yellow-900/80 to-orange-800/60',
    text: 'text-yellow-200',
    glow: 'shadow-2xl shadow-yellow-400/70 hover:shadow-yellow-300/90',
    ring: 'ring-yellow-400/80',
    particle: 'bg-yellow-300',
    animation: 'animate-pulse hover:animate-ping'
  },
  MYTHIC: {
    border: 'border-pink-400',
    bg: 'bg-gradient-to-br from-pink-900/90 to-purple-800/70',
    text: 'text-pink-200',
    glow: 'shadow-2xl shadow-pink-400/80 hover:shadow-pink-300/100',
    ring: 'ring-pink-400/90',
    particle: 'bg-pink-300',
    animation: 'animate-pulse hover:animate-bounce'
  }
};

const sizeClasses = {
  small: 'w-10 h-10 text-xl',
  medium: 'w-20 h-20 text-4xl',
  large: 'w-28 h-28 text-6xl'
};

// 우주 아이콘 렌더링 함수
const renderStellarIcon = (icon: StellaIconType, size: 'small' | 'medium' | 'large') => {
  const iconSize = size === 'small' ? 40 : size === 'medium' ? 80 : 112;
  const iconClass = "relative z-10 filter drop-shadow-lg group-hover:drop-shadow-2xl transition-all duration-500 group-hover:scale-110";
  
  // iconUrl을 기반으로 동적으로 컴포넌트 선택
  const IconComponent = (StellarIcons as IconRegistry)[icon.iconUrl];
  
  if (IconComponent) {
    return <IconComponent className={iconClass} size={iconSize} />;
  }
  
  // 기본 아이콘 (fallback)
  return <StellarIcons.StellarStar className={iconClass} size={iconSize} />;
};

export default function StellaIcon({
  icon,
  size = 'medium',
  selected = false,
  equipped = false,
  owned = false,
  onClick,
  showPrice = false,
  showName = false
}: StellaIconProps) {
  const gradeStyle = gradeStyles[icon.grade];
  const sizeClass = sizeClasses[size];

  return (
    <div className="flex flex-col items-center space-y-2">
      {/* 우주 아이콘 컨테이너 */}
      <div
        className={`
          relative flex items-center justify-center rounded-2xl border-2 cursor-pointer
          transition-all duration-500 hover:scale-105
          backdrop-blur-md overflow-hidden group
          ${sizeClass}
          ${gradeStyle.border}
          ${gradeStyle.bg}
          ${gradeStyle.glow}
          ${gradeStyle.animation}
          ${selected ? `ring-4 ring-white ${gradeStyle.ring} animate-pulse` : ''}
          ${equipped ? 'ring-4 ring-emerald-400 ring-opacity-90 animate-pulse' : ''}
          ${onClick ? 'hover:brightness-110' : ''}
          transform-gpu
        `}
        onClick={onClick}
        title={`${icon.name} - ${icon.description}`}
      >
        {/* 우주 배경 효과 */}
        <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-1500 ease-out"></div>
        
        {/* 등급별 파티클 효과 */}
        {(icon.grade === 'RARE' || icon.grade === 'EPIC' || icon.grade === 'LEGENDARY' || icon.grade === 'MYTHIC') && (
          <>
            <div className={`absolute top-1 right-2 w-0.5 h-0.5 ${gradeStyle.particle} rounded-full animate-ping opacity-70`}></div>
            <div className={`absolute bottom-2 left-1 w-0.5 h-0.5 ${gradeStyle.particle} rounded-full animate-ping opacity-50`} style={{animationDelay: '0.5s'}}></div>
          </>
        )}
        {(icon.grade === 'LEGENDARY' || icon.grade === 'MYTHIC') && (
          <>
            <div className={`absolute top-3 left-3 w-0.5 h-0.5 ${gradeStyle.particle} rounded-full animate-ping opacity-60`} style={{animationDelay: '1s'}}></div>
            <div className={`absolute bottom-1 right-1 w-0.5 h-0.5 ${gradeStyle.particle} rounded-full animate-ping opacity-40`} style={{animationDelay: '1.5s'}}></div>
          </>
        )}
        
        {/* 성운 효과 */}
        {(icon.grade === 'EPIC' || icon.grade === 'LEGENDARY' || icon.grade === 'MYTHIC') && (
          <div className="absolute inset-0 rounded-2xl bg-gradient-to-br from-transparent via-white/10 to-transparent animate-pulse opacity-30"></div>
        )}
        
        {/* 은하수 효과 (LEGENDARY, MYTHIC) */}
        {(icon.grade === 'LEGENDARY' || icon.grade === 'MYTHIC') && (
          <div className="absolute inset-0 rounded-2xl">
            <div className="absolute top-0 left-0 w-full h-0.5 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-pulse"></div>
            <div className="absolute bottom-0 right-0 w-full h-0.5 bg-gradient-to-l from-transparent via-white/30 to-transparent animate-pulse" style={{animationDelay: '1s'}}></div>
          </div>
        )}
        
        {/* 블랙홀 효과 (MYTHIC) */}
        {icon.grade === 'MYTHIC' && (
          <div className="absolute -inset-1 rounded-2xl bg-gradient-to-r from-pink-500/10 via-purple-500/20 to-violet-500/10 animate-spin" style={{animationDuration: '10s'}}></div>
        )}
        {/* 최고급 우주 SVG 아이콘 */}
        {renderStellarIcon(icon, size)}

        {/* 우주 장착됨 표시 */}
        {equipped && (
          <div className="absolute -top-2 -right-2 bg-gradient-to-r from-emerald-500 to-green-400 text-white text-xs px-2 py-1 rounded-full shadow-lg shadow-emerald-400/50 animate-pulse border border-emerald-300">
            ✨ 장착
          </div>
        )}

        {/* 우주 보유중 표시 */}
        {owned && !equipped && (
          <div className="absolute -top-2 -right-2 bg-gradient-to-r from-blue-500 to-cyan-400 text-white text-xs px-2 py-1 rounded-full shadow-lg shadow-blue-400/50 animate-pulse border border-blue-300">
            🌟 보유
          </div>
        )}

        {/* 등급 뱃지 */}
        <div className={`absolute -bottom-2 -left-2 px-2 py-1 rounded-full text-xs font-bold ${gradeStyle.text} bg-gradient-to-r from-black/70 to-gray-900/70 backdrop-blur-sm border ${gradeStyle.border} shadow-md ${icon.grade === 'MYTHIC' ? 'animate-pulse' : icon.grade === 'LEGENDARY' ? 'animate-pulse' : ''}`}>
          {icon.grade === 'MYTHIC' ? '🌌' : icon.grade === 'LEGENDARY' ? '🌟' : icon.grade === 'EPIC' ? '🔮' : icon.grade === 'RARE' ? '✨' : '⭐'} {icon.grade}
        </div>
      </div>

      {/* 우주 아이콘 이름 */}
      {showName && (
        <div className="text-center mt-2">
          <p className={`text-sm font-bold ${gradeStyle.text} drop-shadow-lg`}>{icon.name}</p>
        </div>
      )}

      {/* 우주 가격 표시 */}
      {showPrice && (
        <div className="text-center mt-2">
          <div className="inline-flex items-center gap-1 bg-gradient-to-r from-yellow-900/50 to-orange-900/50 px-3 py-1 rounded-full border border-yellow-400/30 backdrop-blur-sm">
            <span className="text-yellow-300 text-sm">⭐</span>
            <span className="text-yellow-100 font-medium text-sm">{icon.price.toLocaleString()}</span>
          </div>
        </div>
      )}
    </div>
  );
}