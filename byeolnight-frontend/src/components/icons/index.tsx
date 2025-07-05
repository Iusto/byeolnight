// 기존 우주 아이콘들
export { default as Asteroid } from './Asteroid';
export { default as AuroraNebula } from './AuroraNebula';
export { default as Mercury } from './Mercury';
export { default as Venus } from './Venus';
export { default as Earth } from './Earth';
export { default as Mars } from './Mars';
export { default as Jupiter } from './Jupiter';
export { default as Saturn } from './Saturn';
export { default as Uranus } from './Uranus';
export { default as Neptune } from './Neptune';
export { default as Moon } from './Moon';
export { default as BlackHole } from './BlackHole';
export { default as Supernova } from './Supernova';
export { default as Wormhole } from './Wormhole';
export { default as GalaxySpiral } from './GalaxySpiral';
export { default as Pulsar } from './Pulsar';
export { default as Quasar } from './Quasar';
export { default as MeteorShower } from './MeteorShower';
export { default as UFO } from './UFO';
export { default as Comet } from './Comet';
export { default as SpaceStation } from './SpaceStation';
export { default as Constellation } from './Constellation';
export { default as BigBang } from './BigBang';
export { default as Star } from './Star';
export { default as Sun } from './Sun';
export { default as Rocket } from './Rocket';
export { default as NeutronStar } from './NeutronStar';
export { default as WhiteDwarf } from './WhiteDwarf';
export { default as RedGiant } from './RedGiant';
export { default as Portal } from './Portal';

// 새로 추가된 스텔라 아이콘들
export { default as StellarMagnetar } from './StellarMagnetar';
export { default as StellarLightYear } from './StellarLightYear';
export { default as StellarAndromeda } from './StellarAndromeda';
export { default as StellarOrion } from './StellarOrion';
export { default as StellarSolarSystem } from './StellarSolarSystem';
export { default as StellarDarkMatter } from './StellarDarkMatter';
export { default as StellarMilkyWay } from './StellarMilkyWay';
export { default as StellarCosmos } from './StellarCosmos';
export { default as StellarInfiniteUniverse } from './StellarInfiniteUniverse';

// 새로운 고등급 아이콘들
export { default as DarkEnergy } from './DarkEnergy';
export { default as GravitationalWave } from './GravitationalWave';
export { default as TimeLoop } from './TimeLoop';
export { default as QuantumTunnel } from './QuantumTunnel';
export { default as Multiverse } from './Multiverse';
export { default as StringTheory } from './StringTheory';
export { default as CosmicVortex } from './CosmicVortex';

// 기존 아이콘들 (호환성을 위해 유지)
import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

export const StellarStar = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="starGradient" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#534c99" />
        <stop offset="50%" stopColor="#1a5a9c" />
        <stop offset="100%" stopColor="#7fdf36" />
      </radialGradient>
    </defs>
    <circle cx="50" cy="50" r="40" fill="url(#starGradient)" className="animate-pulse" />
  </svg>
);

export const StellarMoon = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="moonGradient" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#64047e" />
        <stop offset="50%" stopColor="#91df74" />
        <stop offset="100%" stopColor="#3ade6b" />
      </radialGradient>
    </defs>
    <circle cx="50" cy="50" r="40" fill="url(#moonGradient)" className="animate-pulse" />
  </svg>
);

export const StellarSun = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="sunGradient" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#1a6c7a" />
        <stop offset="50%" stopColor="#03ce27" />
        <stop offset="100%" stopColor="#eee4e4" />
      </radialGradient>
    </defs>
    <circle cx="50" cy="50" r="40" fill="url(#sunGradient)" className="animate-pulse" />
  </svg>
);

export const StellarGalaxy = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="galaxyGradient" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#5fff81" />
        <stop offset="50%" stopColor="#238ee0" />
        <stop offset="100%" stopColor="#ddecbd" />
      </radialGradient>
    </defs>
    <circle cx="50" cy="50" r="40" fill="url(#galaxyGradient)" className="animate-pulse" />
  </svg>
);

export const StellarRocket = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="rocketGradient" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#fc4833" />
        <stop offset="50%" stopColor="#30bd24" />
        <stop offset="100%" stopColor="#f571ca" />
      </radialGradient>
    </defs>
    <circle cx="50" cy="50" r="40" fill="url(#rocketGradient)" className="animate-pulse" />
  </svg>
);

export const StellarComet = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="cometGradient" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#180e4b" />
        <stop offset="50%" stopColor="#86a7c4" />
        <stop offset="100%" stopColor="#a13698" />
      </radialGradient>
    </defs>
    <circle cx="50" cy="50" r="40" fill="url(#cometGradient)" className="animate-pulse" />
  </svg>
);

export const StellarSaturn = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="saturnGradient" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#9f7c76" />
        <stop offset="50%" stopColor="#458ab2" />
        <stop offset="100%" stopColor="#2aa05e" />
      </radialGradient>
    </defs>
    <circle cx="50" cy="50" r="40" fill="url(#saturnGradient)" className="animate-pulse" />
  </svg>
);

export const StellarBlackHole = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="blackholeGradient" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#687ad1" />
        <stop offset="50%" stopColor="#12a375" />
        <stop offset="100%" stopColor="#b300d1" />
      </radialGradient>
    </defs>
    <circle cx="50" cy="50" r="40" fill="url(#blackholeGradient)" className="animate-pulse" />
  </svg>
);

export const StellarPortal = ({ className = "", size = 100 }: IconProps) => (
  <svg className={className} width={size} height={size} viewBox="0 0 100 100" fill="none">
    <defs>
      <radialGradient id="portalGradient" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#3eef3e" />
        <stop offset="50%" stopColor="#fe5ff1" />
        <stop offset="100%" stopColor="#58ac3f" />
      </radialGradient>
    </defs>
    <circle cx="50" cy="50" r="40" fill="url(#portalGradient)" className="animate-pulse" />
  </svg>
);