// 우주 테마 스텔라 아이콘 컬렉션
import type { StellaIcon as StellaIconType } from '../types/stellaIcon';

export const stellaIcons: StellaIconType[] = [
  // COMMON 등급 - 기본적인 우주 요소들 (100 스텔라) - 9개
  {
    id: 1,
    name: "수성",
    description: "태양에 가장 가까운 행성",
    iconUrl: "Mercury",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 2,
    name: "금성",
    description: "아름다운 새벽별",
    iconUrl: "Venus",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 3,
    name: "화성",
    description: "붉은 행성의 신비",
    iconUrl: "Mars",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 4,
    name: "별",
    description: "밤하늘의 반짝이는 별",
    iconUrl: "Star",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 5,
    name: "태양",
    description: "생명을 주는 따뜻한 별",
    iconUrl: "Sun",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 6,
    name: "달",
    description: "지구의 아름다운 위성",
    iconUrl: "Moon",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 7,
    name: "혜성",
    description: "천년에 한 번 나타나는 신비",
    iconUrl: "Comet",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 8,
    name: "소행성",
    description: "작은 우주 암석",
    iconUrl: "Asteroid",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 9,
    name: "로켓",
    description: "우주를 향한 꿈과 모험",
    iconUrl: "Rocket",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },

  // RARE 등급 - 특별한 우주 현상들 (200 스텔라) - 11개
  {
    id: 10,
    name: "지구",
    description: "우리의 푸른 행성",
    iconUrl: "Earth",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 11,
    name: "목성",
    description: "거대한 가스 행성",
    iconUrl: "Jupiter",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 12,
    name: "천왕성",
    description: "옆으로 누운 신비한 행성",
    iconUrl: "Uranus",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 13,
    name: "해왕성",
    description: "푸른 바다의 행성",
    iconUrl: "Neptune",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 14,
    name: "토성",
    description: "우아한 고리를 가진 행성",
    iconUrl: "Saturn",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 15,
    name: "오로라 성운",
    description: "극지방의 신비한 빛",
    iconUrl: "AuroraNebula",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-wave"
  },
  {
    id: 16,
    name: "유성우",
    description: "소원을 들어주는 별똥별",
    iconUrl: "MeteorShower",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-meteor-trail"
  },
  {
    id: 17,
    name: "UFO",
    description: "미확인 비행 물체",
    iconUrl: "UFO",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 18,
    name: "나선 은하",
    description: "아름다운 나선형 은하",
    iconUrl: "GalaxySpiral",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 19,
    name: "별자리",
    description: "밤하늘의 아름다운 별 그림",
    iconUrl: "Constellation",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-twinkle"
  },
  {
    id: 20,
    name: "우주 정거장",
    description: "인류의 우주 진출 기지",
    iconUrl: "SpaceStation",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 21,
    name: "우주 소용돌이",
    description: "시공간을 빨아들이는 우주 소용돌이",
    iconUrl: "CosmicVortex",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-spin"
  },

  // EPIC 등급 - 환상적인 우주의 신비 (350 스텔라) - 15개
  {
    id: 22,
    name: "펄사",
    description: "규칙적으로 신호를 보내는 별",
    iconUrl: "Pulsar",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 23,
    name: "퀘이사",
    description: "우주에서 가장 밝은 천체",
    iconUrl: "Quasar",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-glow"
  },
  {
    id: 24,
    name: "적색거성",
    description: "거대하게 팽창한 늙은 별",
    iconUrl: "RedGiant",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-stellar-pulse"
  },
  {
    id: 25,
    name: "백색왜성",
    description: "별의 마지막 잔해",
    iconUrl: "WhiteDwarf",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-stellar-pulse"
  },
  {
    id: 26,
    name: "중성자별",
    description: "극도로 압축된 별의 잔해",
    iconUrl: "NeutronStar",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-glow"
  },
  {
    id: 27,
    name: "마그네타",
    description: "강력한 자기장을 가진 별",
    iconUrl: "StellarMagnetar",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-glow"
  },
  {
    id: 28,
    name: "광년",
    description: "빛이 1년간 이동하는 거리",
    iconUrl: "StellarLightYear",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-twinkle"
  },
  {
    id: 29,
    name: "안드로메다",
    description: "우리 은하의 이웃 은하",
    iconUrl: "StellarAndromeda",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-stellar-drift"
  },
  {
    id: 30,
    name: "오리온",
    description: "겨울 밤하늘의 대표 별자리",
    iconUrl: "StellarOrion",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-twinkle"
  },
  {
    id: 31,
    name: "태양계",
    description: "우리 태양계 전체",
    iconUrl: "StellarSolarSystem",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 32,
    name: "암흑물질",
    description: "우주의 숨겨진 물질",
    iconUrl: "StellarDarkMatter",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-quantum-flicker"
  },
  {
    id: 33,
    name: "은하수",
    description: "우리 은하계의 전체 모습",
    iconUrl: "StellarMilkyWay",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 34,
    name: "코스모스",
    description: "우주 전체를 아우르는 존재",
    iconUrl: "StellarCosmos",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-stellar-drift"
  },
  {
    id: 35,
    name: "중력파",
    description: "시공간의 파동",
    iconUrl: "GravitationalWave",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-ripple"
  },
  {
    id: 36,
    name: "타임루프",
    description: "시간의 순환과 무한반복",
    iconUrl: "TimeLoop",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-loop"
  },

  // LEGENDARY 등급 - 전설적인 우주의 보물 (500 스텔라) - 7개
  {
    id: 37,
    name: "블랙홀",
    description: "시공간을 지배하는 절대적 존재",
    iconUrl: "BlackHole",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-black-hole-distortion"
  },
  {
    id: 38,
    name: "초신성",
    description: "별의 장엄한 마지막 순간",
    iconUrl: "Supernova",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-ping"
  },
  {
    id: 39,
    name: "다크에너지",
    description: "우주 가속 팽창의 신비로운 에너지",
    iconUrl: "DarkEnergy",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-dark-pulse"
  },
  {
    id: 40,
    name: "양자터널",
    description: "양자역학의 터널링 현상",
    iconUrl: "QuantumTunnel",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-quantum"
  },
  {
    id: 41,
    name: "다중우주",
    description: "무수히 많은 평행우주들",
    iconUrl: "Multiverse",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-multiverse"
  },
  {
    id: 42,
    name: "스트링이론",
    description: "진동하는 끈들로 이루어진 우주 구조",
    iconUrl: "StringTheory",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-string-vibration"
  },
  {
    id: 43,
    name: "무한우주",
    description: "끝없이 확장하는 우주 자체",
    iconUrl: "StellarInfiniteUniverse",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-dimensional-shift"
  },

  // MYTHIC 등급 - 신화적 우주 존재들 (1000 스텔라) - 1개
  {
    id: 44,
    name: "빅뱅",
    description: "우주 탄생의 순간",
    iconUrl: "BigBang",
    grade: "MYTHIC",
    price: 1000,
    available: true,
    animationClass: "animate-expand"
  }
];