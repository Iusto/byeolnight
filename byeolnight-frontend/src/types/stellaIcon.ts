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
  name: string;
  description: string;
  iconUrl: string;
  grade: string;
  gradeColor: string;
  type: string;
  animationClass: string;
  purchasePrice: number;
  equipped: boolean;
  purchasedAt: string;
}

export interface EquippedIcon {
  iconId: number;
  icon: StellaIcon;
}