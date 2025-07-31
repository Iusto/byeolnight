// 우주 테마 스텔라 아이콘 컬렉션
import type { StellaIcon as StellaIconType } from '../types/stellaIcon';

export const stellaIcons: StellaIconType[] = [
  // COMMON 등급 - 기본적인 우주 요소들 (100 스텔라) - 9개
  {
    id: 1,
    name: "水星",
    description: "太陽に最も近い惑星",
    iconUrl: "Mercury",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 2,
    name: "金星",
    description: "美しい明けの明星",
    iconUrl: "Venus",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 3,
    name: "火星",
    description: "赤い惑星の神秘",
    iconUrl: "Mars",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 4,
    name: "星",
    description: "夜空に輝く星",
    iconUrl: "Star",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 5,
    name: "太陽",
    description: "命を与える暖かい星",
    iconUrl: "Sun",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 6,
    name: "月",
    description: "地球の美しい衛星",
    iconUrl: "Moon",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 7,
    name: "彗星",
    description: "千年に一度現れる神秘",
    iconUrl: "Comet",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 8,
    name: "小惑星",
    description: "小さな宇宙の岩",
    iconUrl: "Asteroid",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 9,
    name: "ロケット",
    description: "宇宙への夢と冒険",
    iconUrl: "Rocket",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  }
];
