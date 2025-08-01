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
  },

  // RARE等級 - 特別な宇宙現象 (200ステラ) - 11個
  {
    id: 10,
    name: "地球",
    description: "私たちの青い惑星",
    iconUrl: "Earth",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 11,
    name: "木星",
    description: "巨大なガス惑星",
    iconUrl: "Jupiter",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 12,
    name: "天王星",
    description: "横に倒れた神秘的な惑星",
    iconUrl: "Uranus",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 13,
    name: "海王星",
    description: "青い海の惑星",
    iconUrl: "Neptune",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 14,
    name: "土星",
    description: "優雅な環を持つ惑星",
    iconUrl: "Saturn",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 15,
    name: "オーロラ星雲",
    description: "極地方の神秘的な光",
    iconUrl: "AuroraNebula",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-wave"
  },
  {
    id: 16,
    name: "流星群",
    description: "願いを叶える流れ星",
    iconUrl: "MeteorShower",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-meteor-trail"
  },
  {
    id: 17,
    name: "UFO",
    description: "未確認飛行物体",
    iconUrl: "UFO",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 18,
    name: "渦巻銀河",
    description: "美しい渦巻き型銀河",
    iconUrl: "GalaxySpiral",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 19,
    name: "星座",
    description: "夜空の美しい星の絵",
    iconUrl: "Constellation",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-twinkle"
  },
  {
    id: 20,
    name: "宇宙ステーション",
    description: "人類の宇宙進出基地",
    iconUrl: "SpaceStation",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 21,
    name: "宇宙の渦",
    description: "時空を吸い込む宇宙の渦",
    iconUrl: "CosmicVortex",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-spin"
  },

  // EPIC等級 - 幻想的な宇宙の神秘 (350ステラ) - 15個
  {
    id: 22,
    name: "パルサー",
    description: "規則的に信号を送る星",
    iconUrl: "Pulsar",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 23,
    name: "クエーサー",
    description: "宇宙で最も明るい天体",
    iconUrl: "Quasar",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-glow"
  },
  {
    id: 24,
    name: "赤色巨星",
    description: "巨大に膨張した老いた星",
    iconUrl: "RedGiant",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-stellar-pulse"
  },
  {
    id: 25,
    name: "白色矮星",
    description: "星の最後の残骸",
    iconUrl: "WhiteDwarf",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-stellar-pulse"
  },
  {
    id: 26,
    name: "中性子星",
    description: "極度に圧縮された星の残骸",
    iconUrl: "NeutronStar",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-glow"
  },
  {
    id: 27,
    name: "マグネター",
    description: "強力な磁場を持つ星",
    iconUrl: "StellarMagnetar",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-glow"
  },
  {
    id: 28,
    name: "光年",
    description: "光が1年間移動する距離",
    iconUrl: "StellarLightYear",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-twinkle"
  },
  {
    id: 29,
    name: "アンドロメダ",
    description: "我々の銀河の隣の銀河",
    iconUrl: "StellarAndromeda",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-stellar-drift"
  },
  {
    id: 30,
    name: "オリオン",
    description: "冬の夜空の代表的な星座",
    iconUrl: "StellarOrion",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-twinkle"
  },
  {
    id: 31,
    name: "太陽系",
    description: "私たちの太陽系全体",
    iconUrl: "StellarSolarSystem",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 32,
    name: "暗黒物質",
    description: "宇宙の隠された物質",
    iconUrl: "StellarDarkMatter",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-quantum-flicker"
  },
  {
    id: 33,
    name: "天の川",
    description: "我々の銀河系の全体の姿",
    iconUrl: "StellarMilkyWay",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 34,
    name: "コスモス",
    description: "宇宙全体を包括する存在",
    iconUrl: "StellarCosmos",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-stellar-drift"
  },
  {
    id: 35,
    name: "重力波",
    description: "時空の波動",
    iconUrl: "GravitationalWave",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-ripple"
  },
  {
    id: 36,
    name: "タイムループ",
    description: "時間の循環と無限反復",
    iconUrl: "TimeLoop",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-loop"
  },

  // LEGENDARY等級 - 伝説的な宇宙の宝物 (500ステラ) - 7個
  {
    id: 37,
    name: "ブラックホール",
    description: "時空を支配する絶対的存在",
    iconUrl: "BlackHole",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-black-hole-distortion"
  },
  {
    id: 38,
    name: "超新星",
    description: "星の壮大な最後の瞬間",
    iconUrl: "Supernova",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-ping"
  },
  {
    id: 39,
    name: "ダークエネルギー",
    description: "宇宙加速膨張の神秘的エネルギー",
    iconUrl: "DarkEnergy",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-dark-pulse"
  },
  {
    id: 40,
    name: "量子トンネル",
    description: "量子力学のトンネリング現象",
    iconUrl: "QuantumTunnel",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-quantum"
  },
  {
    id: 41,
    name: "多元宇宙",
    description: "無数の平行宇宙たち",
    iconUrl: "Multiverse",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-multiverse"
  },
  {
    id: 42,
    name: "ストリング理論",
    description: "振動する弦でできた宇宙構造",
    iconUrl: "StringTheory",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-string-vibration"
  },
  {
    id: 43,
    name: "無限宇宙",
    description: "果てしなく拡張する宇宙自体",
    iconUrl: "StellarInfiniteUniverse",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-dimensional-shift"
  },

  // MYTHIC等級 - 神話的宇宙存在 (1000ステラ) - 1個
  {
    id: 44,
    name: "ビッグバン",
    description: "宇宙誕生の瞬間",
    iconUrl: "BigBang",
    grade: "MYTHIC",
    price: 1000,
    available: true,
    animationClass: "animate-expand"
  }
];
