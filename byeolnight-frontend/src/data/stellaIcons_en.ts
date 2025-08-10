// 우주 테마 스텔라 아이콘 컬렉션
import type { StellaIcon as StellaIconType } from '../types/stellaIcon';

export const stellaIcons: StellaIconType[] = [
  // COMMON 등급 - 기본적인 우주 요소들 (100 스텔라) - 9개
  {
    id: 1,
    name: "Mercury",
    description: "The closest planet to the Sun",
    iconUrl: "Mercury",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 2,
    name: "Venus",
    description: "Beautiful morning star",
    iconUrl: "Venus",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 3,
    name: "Mars",
    description: "Mystery of the red planet",
    iconUrl: "Mars",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 4,
    name: "Star",
    description: "Sparkling star in the night sky",
    iconUrl: "Star",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 5,
    name: "Sun",
    description: "The warm star that gives life",
    iconUrl: "Sun",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 6,
    name: "Moon",
    description: "Earth's beautiful satellite",
    iconUrl: "Moon",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 7,
    name: "Comet",
    description: "A mystery that appears once in a millennium",
    iconUrl: "Comet",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 8,
    name: "Asteroid",
    description: "Small space rock",
    iconUrl: "Asteroid",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 9,
    name: "Rocket",
    description: "Dream and adventure toward space",
    iconUrl: "Rocket",
    grade: "COMMON",
    price: 100,
    available: true,
    animationClass: "animate-pulse"
  },

  // RARE Grade - Special space phenomena (200 Stella) - 11 items
  {
    id: 10,
    name: "Earth",
    description: "Our blue planet",
    iconUrl: "Earth",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 11,
    name: "Jupiter",
    description: "Giant gas planet",
    iconUrl: "Jupiter",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 12,
    name: "Uranus",
    description: "Mysterious planet lying on its side",
    iconUrl: "Uranus",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 13,
    name: "Neptune",
    description: "Blue ocean planet",
    iconUrl: "Neptune",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 14,
    name: "Saturn",
    description: "Planet with elegant rings",
    iconUrl: "Saturn",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-pulse"
  },
  {
    id: 15,
    name: "Aurora Nebula",
    description: "Mysterious light of the polar regions",
    iconUrl: "AuroraNebula",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-wave"
  },
  {
    id: 16,
    name: "Meteor Shower",
    description: "Shooting stars that grant wishes",
    iconUrl: "MeteorShower",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-meteor-trail"
  },
  {
    id: 17,
    name: "UFO",
    description: "Unidentified flying object",
    iconUrl: "UFO",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-bounce"
  },
  {
    id: 18,
    name: "Spiral Galaxy",
    description: "Beautiful spiral galaxy",
    iconUrl: "GalaxySpiral",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 19,
    name: "Constellation",
    description: "Beautiful star patterns in the night sky",
    iconUrl: "Constellation",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-twinkle"
  },
  {
    id: 20,
    name: "Space Station",
    description: "Humanity's space outpost",
    iconUrl: "SpaceStation",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 21,
    name: "Cosmic Vortex",
    description: "Space vortex that swallows spacetime",
    iconUrl: "CosmicVortex",
    grade: "RARE",
    price: 200,
    available: true,
    animationClass: "animate-spin"
  },

  // EPIC Grade - Fantastic mysteries of space (350 Stella) - 15 items
  {
    id: 22,
    name: "Pulsar",
    description: "Star that sends regular signals",
    iconUrl: "Pulsar",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 23,
    name: "Quasar",
    description: "Brightest object in the universe",
    iconUrl: "Quasar",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-glow"
  },
  {
    id: 24,
    name: "Red Giant",
    description: "Massively expanded old star",
    iconUrl: "RedGiant",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-stellar-pulse"
  },
  {
    id: 25,
    name: "White Dwarf",
    description: "Final remnant of a star",
    iconUrl: "WhiteDwarf",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-stellar-pulse"
  },
  {
    id: 26,
    name: "Neutron Star",
    description: "Extremely compressed stellar remnant",
    iconUrl: "NeutronStar",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-glow"
  },
  {
    id: 27,
    name: "Magnetar",
    description: "Star with powerful magnetic field",
    iconUrl: "StellarMagnetar",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-glow"
  },
  {
    id: 28,
    name: "Light Year",
    description: "Distance light travels in one year",
    iconUrl: "StellarLightYear",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-twinkle"
  },
  {
    id: 29,
    name: "Andromeda",
    description: "Neighboring galaxy to our Milky Way",
    iconUrl: "StellarAndromeda",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-stellar-drift"
  },
  {
    id: 30,
    name: "Orion",
    description: "Representative constellation of winter night sky",
    iconUrl: "StellarOrion",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-twinkle"
  },
  {
    id: 31,
    name: "Solar System",
    description: "Our entire solar system",
    iconUrl: "StellarSolarSystem",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 32,
    name: "Dark Matter",
    description: "Hidden matter of the universe",
    iconUrl: "StellarDarkMatter",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-quantum-flicker"
  },
  {
    id: 33,
    name: "Milky Way",
    description: "Full view of our galaxy",
    iconUrl: "StellarMilkyWay",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-spin"
  },
  {
    id: 34,
    name: "Cosmos",
    description: "Being that encompasses the entire universe",
    iconUrl: "StellarCosmos",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-stellar-drift"
  },
  {
    id: 35,
    name: "Gravitational Wave",
    description: "Ripples in spacetime",
    iconUrl: "GravitationalWave",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-ripple"
  },
  {
    id: 36,
    name: "Time Loop",
    description: "Circulation and infinite repetition of time",
    iconUrl: "TimeLoop",
    grade: "EPIC",
    price: 350,
    available: true,
    animationClass: "animate-loop"
  },

  // LEGENDARY Grade - Legendary treasures of space (500 Stella) - 7 items
  {
    id: 37,
    name: "Black Hole",
    description: "Absolute being that dominates spacetime",
    iconUrl: "BlackHole",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-black-hole-distortion"
  },
  {
    id: 38,
    name: "Supernova",
    description: "Magnificent final moment of a star",
    iconUrl: "Supernova",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-ping"
  },
  {
    id: 39,
    name: "Dark Energy",
    description: "Mysterious energy of cosmic acceleration",
    iconUrl: "DarkEnergy",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-dark-pulse"
  },
  {
    id: 40,
    name: "Quantum Tunnel",
    description: "Quantum mechanical tunneling phenomenon",
    iconUrl: "QuantumTunnel",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-quantum"
  },
  {
    id: 41,
    name: "Multiverse",
    description: "Countless parallel universes",
    iconUrl: "Multiverse",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-multiverse"
  },
  {
    id: 42,
    name: "String Theory",
    description: "Cosmic structure made of vibrating strings",
    iconUrl: "StringTheory",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-string-vibration"
  },
  {
    id: 43,
    name: "Infinite Universe",
    description: "The endlessly expanding universe itself",
    iconUrl: "StellarInfiniteUniverse",
    grade: "LEGENDARY",
    price: 500,
    available: true,
    animationClass: "animate-dimensional-shift"
  },

  // MYTHIC Grade - Mythical cosmic beings (1000 Stella) - 1 item
  {
    id: 44,
    name: "Big Bang",
    description: "The moment of universe's birth",
    iconUrl: "BigBang",
    grade: "MYTHIC",
    price: 1000,
    available: true,
    animationClass: "animate-expand"
  }
];
