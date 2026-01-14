/**
 * 대표 인증서 정보
 */
export interface RepresentativeCertificate {
  icon: string;
  name: string;
}

/**
 * 사용자 정보 (로그인 후 /auth/me 응답)
 */
export interface User {
  id: number;
  email: string;
  nickname: string;
  role: 'USER' | 'ADMIN' | 'MODERATOR' | string;
  points: number;
  equippedIconId?: number;
  equippedIconName?: string;
  socialProvider?: string;
  representativeCertificate?: RepresentativeCertificate;
}
