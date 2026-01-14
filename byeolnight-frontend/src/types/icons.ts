import type { ComponentType, SVGProps } from 'react';

/**
 * SVG 아이콘 컴포넌트 타입
 */
export type IconComponent = ComponentType<SVGProps<SVGSVGElement> & { size?: number }>;

/**
 * 아이콘 레지스트리 타입
 */
export interface IconRegistry {
  [key: string]: IconComponent | undefined;
}
