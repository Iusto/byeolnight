export interface StellaIcon {
  id: number;
  name: string;
  description: string;
  iconUrl: string;
  grade: 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY' | 'MYTHIC';
  price: number;
  available: boolean;
  animationClass: string;
}

export interface UserIcon {
  id: number;
  iconId: number;
  icon: StellaIcon;
  purchasedAt: string;
}

export interface EquippedIcon {
  iconId: number;
  icon: StellaIcon;
}