import { useMemo } from 'react';

interface StarfieldBackgroundProps {
  /** 별 개수 (기본 80) */
  density?: number;
  /** 성운 글로우 표시 여부 (기본 true) */
  nebula?: boolean;
  className?: string;
}

interface Star {
  left: number;
  top: number;
  size: number;
  delay: number;
  duration: number;
  opacity: number;
}

/**
 * 우주 테마 배경 효과.
 * - 다층 별(크기·속도 랜덤) + 트윙클
 * - 성운 글로우 블롭(느린 드리프트)
 *
 * pointer-events-none 으로 상호작용을 방해하지 않으며,
 * 부모에 깔리도록 absolute inset-0 으로 채운다.
 * 별 좌표는 useMemo 로 한 번만 생성해 리렌더 시 흔들리지 않게 한다.
 */
export default function StarfieldBackground({
  density = 80,
  nebula = true,
  className = '',
}: StarfieldBackgroundProps) {
  const stars = useMemo<Star[]>(
    () =>
      Array.from({ length: density }, () => ({
        left: Math.random() * 100,
        top: Math.random() * 100,
        size: Math.random() < 0.8 ? 1 : Math.random() < 0.5 ? 2 : 3,
        delay: Math.random() * 4,
        duration: 2 + Math.random() * 3,
        opacity: 0.4 + Math.random() * 0.6,
      })),
    [density]
  );

  return (
    <div className={`absolute inset-0 overflow-hidden pointer-events-none ${className}`} aria-hidden="true">
      {/* 성운 글로우 */}
      {nebula && (
        <>
          <div className="absolute -top-20 -left-20 w-72 h-72 bg-purple-600/20 rounded-full blur-3xl animate-nebula-flow" />
          <div
            className="absolute top-1/3 -right-24 w-80 h-80 bg-blue-600/15 rounded-full blur-3xl animate-nebula-flow"
            style={{ animationDelay: '4s' }}
          />
          <div
            className="absolute -bottom-24 left-1/4 w-72 h-72 bg-pink-600/15 rounded-full blur-3xl animate-nebula-flow"
            style={{ animationDelay: '8s' }}
          />
        </>
      )}

      {/* 별 */}
      {stars.map((star, i) => (
        <div
          key={i}
          className="absolute rounded-full bg-white animate-twinkle"
          style={{
            left: `${star.left}%`,
            top: `${star.top}%`,
            width: `${star.size}px`,
            height: `${star.size}px`,
            opacity: star.opacity,
            animationDelay: `${star.delay}s`,
            animationDuration: `${star.duration}s`,
            boxShadow: star.size >= 2 ? '0 0 4px rgba(255,255,255,0.8)' : undefined,
          }}
        />
      ))}
    </div>
  );
}
